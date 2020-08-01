package com.huolong.hf;

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.io.InputStream;


public class LocalCacheMgr {
    private static final String TAG = "LocalCacheMgr";
    private String head;
    private Activity activity;

    public LocalCacheMgr(String url,Activity activity) {
        this.activity = activity;
        this.head = url.substring(0,url.lastIndexOf("/") + 1);
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
                new WebResourceResponse(mime,"UTF-8",is);
            }
        }
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
}
