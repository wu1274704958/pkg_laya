package com.huolong.hf;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

import layaair.game.browser.ConchJNI;
import layaair.game.conch.LayaConch5;

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
        Logw.e("js bridge gameCall()" + color + " " + (mMainActivity != null) );
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

    public static void call(JSONObject data) {
        String str = String.format("android_call(%s)",data.toString());
        ConchJNI.RunJS(str);
    }

}
