package com.huolong.hf;

import android.app.Activity;
import android.nfc.Tag;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.annotation.RequiresApi;

import com.huolong.hf.utils.Hash;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class LocalCacheMgr {
    private static final String TAG = "LocalCacheMgr";
    private String head;
    private Activity activity;
    private String localDir;
    private HashMap<String,Integer> st_map;
    public static final Integer ST_WAIT = 0;
    public static final Integer ST_IN_PROGRESS = 100;
    public static final Integer ST_SUCCESS = 2;
    public static final Integer ST_FAILED = 3;
    public static final Integer ST_NULL = -1;
    public static final Integer ST_RETRY = 200;

    public static final Integer MAX_RETRY = 10;
    private Handler handler = new Handler(Looper.getMainLooper());

    private boolean not_cache_js = true;
    public LocalCacheMgr(String url,Activity activity) {
        this.activity = activity;
        this.head = url.substring(0,url.lastIndexOf("/") + 1);
        localDir = activity.getFilesDir().getAbsolutePath();
        st_map = new HashMap<>();
        Log.e(TAG,head);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request)
    {

        String url = request.getUrl().toString();
        //Log.e(TAG,url);
        if(url.startsWith(head))
        {
            String sub = url.substring(head.length());

            //Log.e(TAG,sub);
            InputStream is = open(sub);
            if(is != null)
            {

                String mime = request.getRequestHeaders().get("Accept");
                Log.e(TAG,"using cached file " + sub);
                return new WebResourceResponse(mime,"UTF-8",is);
            }
        }
        try {
            if(
                    url.endsWith(".png") || ( !not_cache_js && url.endsWith(".js")) || url.endsWith(".mp3") ||
                    url.endsWith(".jpg")  || url.endsWith(".dat") || url.endsWith(".txt") || url.endsWith(".wdp")
            ) {
                String mime = request.getRequestHeaders().get("Accept");
                String md5 = Hash.md5(url);
                File f = new File(localDir +"/"+ md5);
                if(f.exists() || get_state(md5) == ST_SUCCESS )
                {
                    Log.e(TAG, "using cached file " + url + " " + md5);
                    InputStream in = new FileInputStream(f);
                    return new WebResourceResponse(mime, "UTF-8", in);
                }else
                if(get_state(md5) < 0)
                {
                    final PipedOutputStream out = new PipedOutputStream();
                    PipedInputStream in = new PipedInputStream(out);

                    down(md5);
                    Log.e(TAG, "download file " + url + " " + md5);
                    download(out,url, mime, localDir, md5, downloadListener);

                    return new WebResourceResponse(mime, "UTF-8", in);
                }
            }
        } catch (Exception e) {
            Log.e(TAG,e.getMessage());
        }

        return null;
    }

    public OnDownloadListener downloadListener = new OnDownloadListener() {
        @Override
        public void onDownloadSuccess(File file, String name) {
            Log.e(TAG, "download success " + name);
            to_success(name);
        }

        @Override
        public void onDownloading(int progress, String name) {
            to_inprogress(name,progress);
        }

        @Override
        public void onDownloadFailed(Exception e, String name) {
            Log.e(TAG, "download failed " + e.getMessage()  + "\n" + name);
            to_failed(name);
        }
    };

    public InputStream open(String s)
    {
        try {
            InputStream is = activity.getAssets().open(s);
            return is;
        } catch (IOException e) {
            return null;
        }
    }

    public void down(String f)
    {
        synchronized (this)
        {
            if(!st_map.containsKey(f))
            {
                st_map.put(f,ST_WAIT);
            }
        }
    }
    public void to_failed(String f)
    {
        synchronized (this)
        {
            if(st_map.containsKey(f))
            {
                st_map.remove(f);
            }
        }
    }
    public void to_inprogress(String f,int v)
    {
        synchronized (this)
        {
            if(st_map.containsKey(f))
            {
                st_map.put(f,ST_IN_PROGRESS + v);
            }
        }
    }
    public void to_success(String f)
    {
        synchronized (this)
        {
            if(st_map.containsKey(f))
            {
                st_map.put(f,ST_SUCCESS);
            }
        }
    }
    public int get_state(String f)
    {
        synchronized (this)
        {
            if(st_map.containsKey(f))
            {
                return st_map.get(f);
            }else
            {
                return -1;
            }
        }
    }
    public void set_retry(String f,int v)
    {
        synchronized (this)
        {
            if(st_map.containsKey(f))
            {
                st_map.put(f,ST_RETRY + v);
            }
        }
    }
    public int get_retry(String f)
    {
        synchronized (this)
        {
            if(st_map.containsKey(f))
            {
                int st = st_map.get(f);
                if(st >= ST_RETRY)
                    return st - ST_RETRY;
                else
                    return 0;
            }else
            {
                return -1;
            }
        }
    }

    public void download(final OutputStream out, final String url, final String mime, final String destFileDir, final String destFileName, final OnDownloadListener listener) {

        Request request = new Request.Builder()
                .addHeader("User-Agent","Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1")
                .addHeader("Accept",mime)
                .url(url)
                .build();

        OkHttpClient okHttpClient = new OkHttpClient();

        //异步请求
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if(!retry(e.getMessage(),out,url,mime,destFileDir,destFileName,listener,0)) {
                    try {
                        out.flush();
                        out.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    // 下载失败监听回调
                    if (listener != null)
                        listener.onDownloadFailed(e, destFileName);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                boolean success = false;
                boolean is_retry = false;
                InputStream is = null;
                byte[] buf = new byte[1024 * 64];
                int len = 0;

                if(response.code() != 200)
                {
                    response.close();
                    String reason = "failed code = " + response.code();
                    if(!retry(reason,out,url,mime,destFileDir,destFileName,listener,0)) {
                        try {
                            out.flush();
                            out.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        // 下载失败监听回调
                        if (listener != null)
                            listener.onDownloadFailed(new Exception(reason), destFileName);
                    }
                    return;
                }

                FileOutputStream fos = null;

                //储存下载文件的目录
                File dir = new File(destFileDir);
                File temp_dir = new File(destFileDir + "/temp");
                if(!temp_dir.exists())
                    temp_dir.mkdirs();
                if (!dir.exists())
                    dir.mkdirs();
                File file = new File(temp_dir, destFileName);
                if(!file.exists())
                    file.createNewFile();
                try {

                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        out.write(buf,0,len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        //下载中更新进度条
                        if(listener!=null)
                            listener.onDownloading(progress,destFileName);
                    }
                    fos.flush();
                    out.flush();
                    success = true;
                    //下载完成

                } catch (Exception e) {
                    out.flush();
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
                        out.close();
                        if(success) {
                            File real_file = new File(dir,destFileName);
                            file.renameTo(real_file);
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

    private boolean retry(String reason, final OutputStream out, final String url, final String mime, final String destFileDir, final String destFileName, final OnDownloadListener listener, int lazy_ms)
    {
        int v = get_retry(destFileName);
        if(v >= 0 && v <= MAX_RETRY){
            set_retry(destFileName,v + 1);
            Log.e(TAG,reason + " retry "+ (v+1) + " url " + url);
            if(lazy_ms > 0)
            {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        download(out,url,mime,destFileDir,destFileName,listener);
                    }
                },lazy_ms);
            }else
                download(out,url,mime,destFileDir,destFileName,listener);
            return true;
        }
        return false;
    }

    public interface OnDownloadListener{

        /**
         * 下载成功之后的文件
         */
        void onDownloadSuccess(File file,String name);

        /**
         * 下载进度
         */
        void onDownloading(int progress,String name);

        /**
         * 下载异常信息
         */

        void onDownloadFailed(Exception e,String name);
    }



}
