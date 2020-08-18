package com.huolong.hf;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.just.agentwebX5.AgentWebX5;
import com.plug.wv.FullScreenDialog;

/**
 * Created by
 * Date is 2019/5/7
 **/
public class JsCallAndroidInterface {
    private Handler deliver = new Handler(Looper.getMainLooper());
    private AgentWebX5 agent;
    private Context context;
    FullScreenDialog.OnWVCb cb;


    public JsCallAndroidInterface(AgentWebX5 agent, Context context, FullScreenDialog.OnWVCb cb) {
        this.agent = agent;
        this.context = context;
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

    @JavascriptInterface
    public void go(final String data) {
        Log.e(FullScreenDialog.TAG, "BackToAndroid" + data);
        deliver.post(new Runnable() {
            @Override
            public void run() {
                cb.onJsCall("go",data);
            }
        });
    }

}