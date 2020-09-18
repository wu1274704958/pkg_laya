package com.huolong.hf;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONException;

import java.lang.ref.WeakReference;


public class JSBridge {
    public static Handler m_Handler = new Handler(Looper.getMainLooper());
    public static WeakReference<MainActivity> mMainActivity = null;

    public static void hideSplash() {

    }

    public static void setFontColor(final String color) {

    }

    public static void setTips(final JSONArray tips) {

    }

    public static void bgColor(final String color) {
        Logw.e("bgColor " + color);
    }

    public static void loading(final double percent) {

    }

    public static void showTextInfo(final boolean show) {

    }

    public static void gameCall(final String color) {
        Logw.e(color + " " + (mMainActivity != null) );
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                if(mMainActivity != null )
                {
                    MainActivity mainActivity = null;
                    if((mainActivity = mMainActivity.get())!=null)
                    {
                        mainActivity.cb.onJsCall("gameCall",color);
                    }
                }
            }
        });

    }
}
