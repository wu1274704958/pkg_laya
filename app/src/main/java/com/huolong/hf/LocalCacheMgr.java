package com.huolong.hf;

import android.app.Activity;
import android.nfc.Tag;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.RequiresApi;

import com.huolong.hf.utils.Hash;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.WebView;

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
import java.util.HashSet;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class LocalCacheMgr {

    private static class RFile{
        String name;
        String suffix;

        public RFile(String name, String suffix) {
            this.name = name;
            this.suffix = suffix;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public String toString() {
            return String.format("%s.%s",name,suffix);
        }
    }
    private static class State{
        int st = ST_NULL;
        long len = 0;

        public State(int st) {
            this.len = 0;
            this.st = st;
        }

        public State(int st,long len) {
            this.len = len;
            this.st = st;
        }

        public void set_state(int st)
        {
            this.len = 0;
            this.st = st;
        }
        public void set_state(int st,long l)
        {
            this.len = l;
            this.st = st;
        }
    }

    private static final String TAG = "LocalCacheMgr";
    private String head;
    private Activity activity;
    private String localDir;
    private HashMap<String,State> st_map;
    private HashSet<String> ignore_set;
    public static final Integer ST_WAIT = 0;
    public static final Integer ST_IN_PROGRESS = 100;
    public static final Integer ST_SUCCESS = 2;
    public static final Integer ST_FAILED = 3;
    public static final Integer ST_NULL = -1;
    public static final Integer ST_RETRY = 200;

    public static final Integer MAX_RETRY = 10;
    private Handler handler = new Handler(Looper.getMainLooper());

    private boolean use_web_download = false;

    private boolean not_cache_js = false;
    public LocalCacheMgr(String url,Activity activity) {
        this.activity = activity;
        this.head = url.substring(0,url.lastIndexOf("/") + 1);
        localDir = activity.getFilesDir().getAbsolutePath();
        st_map = new HashMap<>();
        Log.e(TAG,head);
        fill_ignore();
    }

    public void fill_ignore()
    {
        ignore_set = new HashSet<>();
        ignore_set.add("assets/config/config.txt");
        ignore_set.add("assets/loading_m/beijing.png");
        ignore_set.add("layares/loading/loadbg4.jpg");
        ignore_set.add("assets/loadbg4.jpg");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public WebResourceResponse shouldInterceptRequest(WebView webView,
                                                      WebResourceRequest request)
    {

        String url = request.getUrl().toString();
        //Log.e(TAG,url);
        if(url.startsWith(head))
        {
            String sub = url.substring(head.length());
            sub = filter_path(sub);
            //Log.e(TAG,sub);
            InputStream is = open(sub);
            if(is != null)
            {

                String mime = request.getRequestHeaders().get("Accept");
                //Log.e(TAG,"using cached file " + sub);
                return new WebResourceResponse(mime,"UTF-8",is);
            }
        }
        if(use_web_download)
            return null;
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
                   // Log.e(TAG, "using cached file " + url + " " + md5);
                    InputStream in = new FileInputStream(f);
                    return new WebResourceResponse(mime, "UTF-8", in);
                }else
                if(get_state(md5) < 0)
                {
                    final PipedOutputStream out = new PipedOutputStream();
                    PipedInputStream in = new PipedInputStream(out);

                    down(md5);
                    //Log.e(TAG, "download file " + url + " " + md5);
                    download(out,url, mime, localDir, md5, downloadListener);

                    return new WebResourceResponse(mime, "UTF-8", in);
                }
            }
        } catch (Exception e) {
            Log.e(TAG,e.getMessage());
        }

        return null;
    }

    private String filter_path(String sub) {
        int e = sub.lastIndexOf('.');
        if(e > 0)
        {
            String suf = null,bs = null;
            try {
                suf = sub.substring(e+1);
                bs = sub.substring(0,e - 11);
            }catch (Exception exx) { }
            if(bs != null && suf != null)
            {
                //Log.e(TAG,bs+"."+suf);
                String rf = new RFile(bs,suf).toString();
                if(ignore_set.contains(rf))
                {
                    Log.e(TAG,"need ignore " + rf);
                    return rf;
                }
            }
        }
        return sub;
    }

    public OnDownloadListener downloadListener = new OnDownloadListener() {
        @Override
        public void onDownloadSuccess(File file, String name) {
            //Log.e(TAG, "download success " + name);
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
                st_map.put(f,new State(ST_WAIT));
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
                st_map.get(f).set_state(ST_IN_PROGRESS + v);
            }
        }
    }
    public void to_success(String f)
    {
        synchronized (this)
        {
            if(st_map.containsKey(f))
            {
                st_map.get(f).set_state(ST_SUCCESS);
            }
        }
    }
    public int get_state(String f)
    {
        synchronized (this)
        {
            if(st_map.containsKey(f))
            {
                return st_map.get(f).st;
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
                st_map.get(f).set_state(ST_RETRY + v);
            }
        }
    }
    public void set_retry(String f,int v,long l)
    {
        synchronized (this)
        {
            if(st_map.containsKey(f))
            {
                st_map.get(f).set_state(ST_RETRY + v,l);
            }
        }
    }
    public int get_retry(String f)
    {
        synchronized (this)
        {
            if(st_map.containsKey(f))
            {
                int st = st_map.get(f).st;
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
    public long get_retry_len(String f)
    {
        synchronized (this)
        {
            if(st_map.containsKey(f))
            {
                int st = st_map.get(f).st;
                if(st >= ST_RETRY)
                    return st_map.get(f).len;
                else
                    return 0;
            }else
            {
                return 0;
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
                if(!retry(e.getMessage(),out,url,mime,destFileDir,destFileName,listener,0,0)) {
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
                final int MaxLen = 1024 * 64;
                byte[] buf = new byte[MaxLen];
                int len = 0;

                if(response.code() != 200)
                {
                    response.close();
                    String reason = "failed code = " + response.code();
                    if(!retry(reason,out,url,mime,destFileDir,destFileName,listener,0,0)) {
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

                long sum = 0;
                long real_sum = get_retry_len(destFileName);
                try {

                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    fos = new FileOutputStream(file);

                    if(real_sum > sum)
                    {
                        long t_len = 0;
                        while(real_sum != sum)
                        {
                            long r_len = real_sum - sum > MaxLen ? MaxLen : real_sum - sum;
                            t_len = is.read(buf,0, (int) r_len);
                            fos.write(buf,0, (int) t_len);
                            sum += t_len;
                        }
                    }

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
                    if(sum < real_sum) sum = real_sum;
                    success = false;
                    if(!(is_retry = retry(e.getMessage(),out,url,mime,destFileDir,destFileName,listener,5,sum)))
                    {
                        out.flush();
                        if(listener!=null) listener.onDownloadFailed(e,destFileName);
                    }
                }finally {

                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                        if(success || !is_retry)
                            out.close();
                        response.close();
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

    private boolean retry(String reason, final OutputStream out, final String url, final String mime, final String destFileDir, final String destFileName,
                          final OnDownloadListener listener, int lazy_ms,long pos)
    {
        int v = get_retry(destFileName);
        if(pos < get_retry_len(destFileName))
            return false;
        if(v >= 0 && v <= MAX_RETRY){
            set_retry(destFileName,v + 1,pos);
            Log.e(TAG,reason + " retry "+ (v+1) + " pos = " + pos  + " url " + url);
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
