package com.huolong.hf;

import android.app.Activity;
import android.nfc.Tag;
import android.os.Build;
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

    public LocalCacheMgr(String url,Activity activity) {
        this.activity = activity;
        this.head = url.substring(0,url.lastIndexOf("/") + 1);
        localDir = activity.getFilesDir().getAbsolutePath();
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
        //try {
        //    String mime = request.getRequestHeaders().get("Accept");
        //    String md5 = Hash.md5(url);
        //    File f = new File(localDir + md5);
        //    if(f.exists())
        //    {
        //        Log.e(TAG,"using cached file " + url + " " + md5 );
        //        InputStream in = new FileInputStream(f);
        //        return new WebResourceResponse(mime,"UTF-8",in);
        //    }else{
        //        Log.e(TAG,"download file " + url + " " + md5 );
        //        download(url,mime,localDir,md5,null);
        //    }
        //} catch (Exception e) {
        //    Log.e(TAG,e.getMessage());
        //}
        return null;
    }

    public InputStream open(String s)
    {
        try {
            InputStream is = activity.getAssets().open(s);
            return is;
        } catch (IOException e) {
            return null;
        }
    }

    public void download(final String url,String mime,final String destFileDir, final String destFileName, final OnDownloadListener listener) {

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
                // 下载失败监听回调
                if(listener != null)
                    listener.onDownloadFailed(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                InputStream is = null;
                byte[] buf = new byte[1024 * 64];
                int len = 0;
                FileOutputStream fos = null;

                //储存下载文件的目录
                File dir = new File(destFileDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File file = new File(dir, destFileName);
                if(!file.exists())
                    file.createNewFile();
                try {

                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        //下载中更新进度条
                        if(listener!=null)
                            listener.onDownloading(progress);
                    }
                    fos.flush();
                    //下载完成
                    if(listener!=null)  listener.onDownloadSuccess(file);
                } catch (Exception e) {
                    listener.onDownloadFailed(e);
                }finally {

                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {

                    }
                }
            }
        });
    }

    public interface OnDownloadListener{

        /**
         * 下载成功之后的文件
         */
        void onDownloadSuccess(File file);

        /**
         * 下载进度
         */
        void onDownloading(int progress);

        /**
         * 下载异常信息
         */

        void onDownloadFailed(Exception e);
    }



}
