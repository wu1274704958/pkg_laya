package com.huolong.hf.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.UiThread;
import androidx.core.content.FileProvider;

import com.huolong.hf.LocalCacheMgr;
import com.huolong.hf.MainActivity;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import one.huolong.online.R;

public class NewPkgMgr {
    private static final String TAG = "NewPkgMgr";

    public static class Info{
        String version;
        String url;
        boolean is_force;

        @Override
        public String toString() {
            return "Info{" +
                    "version='" + version + '\'' +
                    ", url='" + url + '\'' +
                    ", is_force=" + is_force +
                    '}';
        }
    }
    public static class Ver{
        int major = 0;
        float minor = 0.f;

        public Ver(String v) {
            try{

                int dot_idx = v.indexOf('.');
                if(dot_idx > 0)
                {
                    major = Integer.parseInt( v.substring(0,dot_idx) );
                    if(dot_idx < v.length() - 1)
                    minor = Float.parseFloat(v.substring(dot_idx + 1));
                }
            }catch (Exception e)
            {

            }
        }
    }

    Context context;
    public static final int ST_FREE = 0;
    public static final int ST_LoadConfig = 1;
    public static final int ST_Download = 2;
    public static final int ST_Install = 3;
    private int state = ST_FREE;
    private Info cached;
    private static final int Pid = 1300;
    public NewPkgMgr(Context context)
    {
        this.context = context;

        root = new File(context.getFilesDir().getAbsolutePath() + "/apk/");
        if(!root.exists())
            root.mkdirs();
    }
    private Handler handler = new Handler( new Handler.Callback(){
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what)
            {
                case 0:
                    if(msg.arg1 == 1) {
                    temp_dialog.dismiss();
                        on_downloaded(msg.arg1 == 1);
                    }else {
                        on_downloaded(false);
                    }
                    break;
                case 1:
                    dialog_progress.setProgress(msg.arg1);
                    break;
                case 2:
                    temp_dialog.dismiss();
                    Toast.makeText(context,"下载失败,自动重试!",Toast.LENGTH_SHORT).show();
                    Runnable r = (Runnable) msg.obj;
                    if(r != null) r.run();
                    break;
                case 3:
                    progress_tv.setText(msg.obj != null ? msg.obj.toString():"");
                    break;
                case 4:

                    break;
                default:

                    break;
            }
            return true;
        }
    });;

    public void load_config()
    {
        state = ST_LoadConfig;
        Log.e(TAG,"load_config");
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        JSONObject o = new JSONObject();
        try{
            o.put("pid",Pid);
        }catch (Exception e)
        {
            Log.e(TAG,e.getMessage());
        }
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://cquc.xianyul.com/api/ver2")
                .post(RequestBody.create(JSON,o.toString()))
                .build();
        okHttpClient.newCall(request).enqueue(new Callback(){

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.code() == 200)
                {
                    try {
                        String res = response.body().string();
                        JSONObject obj = new JSONObject(res);
                        Info info = new Info();
                        info.version = obj.getString("verNum");
                        info.url = obj.getString("bundleUrl");
                        info.is_force = obj.getInt("forcedUpdate") == 1;
                        Log.e(TAG, "load config success " + info.toString());
                        onload_success(info);
                    }catch (Exception e){}
                }else {
                    state = ST_FREE;
                    Log.e(TAG,"ver failed code = " + response.code());
                }
                response.close();
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG,"ver failed msg = " + e.getMessage());
                state = ST_FREE;
            }
        });
        Log.e(TAG,"load_config e");
    }

    public void onload_success(Info info)
    {
        cached = info;
        Ver self = new Ver(Utils.getAppVersionName(context));
        Ver line = new Ver(info.version);

        if(self.major < line.major ||  self.minor < line.minor)
        {
            update(info.version,info.is_force,info.url);
        }
    }

    private File root;
    private String apk_name = "update_pkg.apk";
    private String conf_name = "conf.txt";

    private String get_curr_download_pkg_ver()
    {
        File f = new File(root.getAbsolutePath(),conf_name);
        if(f.exists())
        {
            Reader is = null;
            try {
                is = new FileReader(f);
                StringBuffer sb = new StringBuffer();
                int len = -1;
                char[] buf = new char[1024];
                while ((len = is.read(buf)) != -1)
                {
                    sb.append(buf,0,len);
                }
                return sb.toString();
            } catch (Exception e) {
                Log.e(TAG,e.getMessage());
            }finally {
                try {
                    if (is != null)
                        is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private void update(String ver,boolean is_force, String url) {
        Log.e(TAG,"update func");
        String last = get_curr_download_pkg_ver();
        File pkg = new File(root,apk_name);
        if(last != null && last.equals(ver) && pkg.exists())
        {
            on_downloaded(is_force);
        }else{
            download(is_force,url,ver);
        }
    }
    private AlertDialog temp_dialog;
    private void download(final boolean is_force, final String url, final String ver) {
        state = ST_Download;
        Log.e(TAG,"download " + url);
        final LocalCacheMgr.OnDownloadListener cb = new LocalCacheMgr.OnDownloadListener() {
            @Override
            public void onDownloadSuccess(File file, String name) {
                save_ver(ver);

                Message m = new Message();
                m.what = 0;
                m.arg1 = is_force ? 1 : 0;
                handler.sendMessage(m);
            }

            @Override
            public void onDownloading(final int progress, String name) {
                if(is_force && dialog_progress != null)
                {
                    Message m = new Message();
                    m.arg1 = progress;
                    m.what = 1;
                    handler.sendMessage(m);
                }
                Log.e(TAG,"onDownloading " + progress);
            }

            @Override
            public void onDownloadFailed(Exception e, String name) {
                if(is_force)
                {
                    Message m = new Message();
                    m.what = 2;
                    m.obj = new Runnable() {
                        @Override
                        public void run() {
                            download(is_force,url,ver);
                        }
                    };
                    handler.sendMessage(m);
                }else
                    state = ST_FREE;
                Log.e(TAG,"download failed " + e.getMessage() + " " + name);
            }
        };
        final OnDownloadProgressListener downloadProgressListener = new OnDownloadProgressListener() {
            @Override
            public void onProgress(long curr, long max, int progress, String name) {
                if(is_force && progress_tv != null)
                {
                    Message m = new Message();
                    m.obj = String.format("%sMB/%sMB", curr / 1048576,max / 1048576);
                    m.what = 3;
                    handler.sendMessage(m);
                }
            }
        };
        Log.e(TAG,"is_force " + is_force);
        if(is_force)
        {
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pop_dialog("新版本可用,是否下载?", false, "下载", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            temp_dialog = pop_progress_dialog();
                            download(url, root.getAbsolutePath(), apk_name,cb,downloadProgressListener);
                        }
                    }, "取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    });
                }
            });
        }else {
            Log.e(TAG,"pop_dialog " + is_force);
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pop_dialog("新版本可用,是否后台下载?", false, "下载", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            download(url, root.getAbsolutePath(), apk_name,cb,null);
                            dialogInterface.dismiss();
                        }
                    }, "取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                }
            });
        }
    }

    private void save_ver(String ver) {
        File f = new File(root.getAbsolutePath(),conf_name);
        if(!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        {
            Writer ou = null;
            try {
                ou = new FileWriter(f);
                ou.write(ver);
                ou.flush();
            } catch (Exception e) {
                Log.e(TAG,e.getMessage());
            }finally {
                try {
                    if (ou != null)
                        ou.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void on_downloaded(final boolean is_force)
    {
        if(is_force)
        {
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    install(true);
                }
            });
        }else{
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pop_dialog("新版本已下载完毕,是否安装?", false, "安装", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            install(false);
                        }
                    }, "取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            state = ST_FREE;
                        }
                    });
                }
            });
        }
    }

    private boolean install(final boolean is_force)
    {
        state = ST_Install;
        File file = new File(root,apk_name);
        if(!file.exists())
            return false;
        Uri uri = null;
        try{

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                boolean hasInstallPermission = context.getPackageManager().canRequestPackageInstalls();
                if (!hasInstallPermission) {
                    pop_dialog("检测到没有安装更新的权限,请在设置中允许" + context.getString(R.string.app_name) + "安装应用!", false,
                            "跳转设置", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    ((MainActivity) context).set_on_resume_task(new Runnable() {
                                        @Override
                                        public void run() {
                                            install(is_force);
                                        }
                                    });
                                    Toast.makeText(context,"请点击允许安装应用",Toast.LENGTH_LONG).show();
                                    startInstallPermissionSettingActivity();
                                    dialogInterface.dismiss();
                                }
                            }, "取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if(is_force)
                                        android.os.Process.killProcess(android.os.Process.myPid());
                                    else
                                        dialogInterface.dismiss();
                                }
                            });

                    return false;
                }
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                //如果SDK版本>=24，即：Build.VERSION.SDK_INT >= 24
                String packageName = context.getApplicationContext().getPackageName();
                String authority = new StringBuilder(packageName).append(".provider").toString();
                uri = FileProvider.getUriForFile(context, authority, file);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
            } else{
                uri = Uri.fromFile(file);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
            }
            context.startActivity(intent);
            return  true;
        }catch (Exception e) {
            Log.e(TAG,e.getMessage());
        }
        return  false;
    }


    private AlertDialog pop_dialog(String tips, boolean cancelable,
                            String positive, DialogInterface.OnClickListener positiveCb,
                            String negative, DialogInterface.OnClickListener negativeCb)
    {

        TextView dialog_content = new TextView(context);
        dialog_content.setTextColor(context.getResources().getColor(R.color.colorPrimaryDark));
        dialog_content.setTextSize(16.f);
        dialog_content.setPadding(39, 5, 5, 5);

        dialog_content.setText(tips);
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.dialog);
        AlertDialog err_dialog = builder.setCancelable(cancelable)
                .setPositiveButton(positive, positiveCb)
                .setNegativeButton(negative, negativeCb)
                .setView(dialog_content)
                .setTitle("提示")
                .create();

        err_dialog.show();
        return err_dialog;
    }


    private void startInstallPermissionSettingActivity() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private ProgressBar dialog_progress;
    private LinearLayout dialog_progress_view;
    private AlertDialog progress_dialog;
    private TextView progress_tv;
    private AlertDialog pop_progress_dialog()
    {
        if(dialog_progress_view == null) {
            dialog_progress_view = (LinearLayout) LinearLayout.inflate(context, R.layout.progress, null);
            dialog_progress = dialog_progress_view.findViewById(R.id.pb_download);
            progress_tv = dialog_progress_view.findViewById(R.id.tv_download);
            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.dialog);
            progress_dialog = builder.setCancelable(false)
                    .setView(dialog_progress_view)
                    .setTitle("新版本下载中...")
                    .create();
        }
        progress_dialog.show();
        return progress_dialog;
    }

    public interface OnDownloadProgressListener{
        void onProgress(long curr,long max,int progress,String name);
    }

    public void download(final String url, final String destFileDir, final String destFileName,
                         final LocalCacheMgr.OnDownloadListener listener,
                         final OnDownloadProgressListener onProgress) {

        Request request = new Request.Builder()
                .addHeader("User-Agent","Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1")
                .addHeader("Accept","image/webp,image/apng,image/*,*/*;q=0.8")
                .url(url)
                .build();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10000, TimeUnit.MILLISECONDS)
                .readTimeout(10000,TimeUnit.MILLISECONDS)
                .build();

        //异步请求
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (listener != null)
                    listener.onDownloadFailed(e, destFileName);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.e(TAG,"onResponse " + url);
                boolean success = false;
                InputStream is = null;
                final int MaxLen = 1024 * 64;
                byte[] buf = new byte[MaxLen];
                int len = 0;

                if(response.code() != 200)
                {
                    response.close();
                    String reason = "failed code = " + response.code();

                    // 下载失败监听回调
                    if (listener != null)
                        listener.onDownloadFailed(new Exception(reason), destFileName);

                    return;
                }

                FileOutputStream fos = null;

                //储存下载文件的目录
                File dir = new File(destFileDir);
                File temp_dir = new File(destFileDir);
                if(!temp_dir.exists())
                    temp_dir.mkdirs();
                if (!dir.exists())
                    dir.mkdirs();
                File file = new File(temp_dir, destFileName);
                if(!file.exists())
                    file.createNewFile();

                long sum = 0;
                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    fos = new FileOutputStream(file);

                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        //下载中更新进度条
                        if(listener!=null)
                            listener.onDownloading(progress,destFileName);
                        if(onProgress != null)
                            onProgress.onProgress(sum,total,progress,destFileName);
                    }
                    fos.flush();
                    success = true;
                    //下载完成

                } catch (Exception e) {
                    success = false;
                    if(listener!=null) listener.onDownloadFailed(e,destFileName);
                }finally {

                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                        response.close();
                        if(success) {
                            if(listener!=null)  listener.onDownloadSuccess(file,destFileName);
                        }else {
                            file.delete();
                        }

                    } catch (IOException e) {

                    }
                }
            }
        });
    }

    public boolean is_network_busy()
    {
        return state == ST_Download;
    }

}
