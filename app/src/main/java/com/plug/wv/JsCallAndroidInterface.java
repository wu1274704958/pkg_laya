package com.plug.wv;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;


import com.just.agentweb.AgentWeb;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by 李明明
 * Date is 2019/5/7
 **/
public class JsCallAndroidInterface {
    private Handler deliver = new Handler(Looper.getMainLooper());
    private final FullScreenDialog fullScreenDialog;
    private AgentWeb agent;
    private Context context;
    FullScreenDialog.OnWVCb cb;


    public JsCallAndroidInterface(FullScreenDialog dialog,AgentWeb agent, Context context, FullScreenDialog.OnWVCb cb) {
        this.agent = agent;
        this.context = context;
        fullScreenDialog = dialog;
        this.cb = cb;
    }

    @JavascriptInterface
    public void BackToAndroid(final String data) {
        Log.e(FullScreenDialog.TAG, "BackToAndroid" + data);
        deliver.post(new Runnable() {
            @Override
            public void run() {
                cb.onJsCall("BackToAndroid",data);
            }
        });

    }

}