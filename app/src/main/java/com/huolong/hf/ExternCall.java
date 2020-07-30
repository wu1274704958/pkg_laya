package com.huolong.hf;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.webkit.ValueCallback;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.huolong.hf.utils.BatteryReceiver;
import com.huolong.hf.utils.NetMonitor;
import com.just.agentweb.AgentWeb;
import com.plug.oaid.Oaid;
import com.plug.reg.Reg;
import com.plug.wv.WebView;

import org.json.JSONException;
import org.json.JSONObject;


public class ExternCall {
    private static final String TAG = "ExternCall";
    AgentWeb web;
    SparseArray<ValueCallback<JSONObject>> callbacks;
    Activity activity;
    public static final int WSendMessageToGame = 1;
    public static final int WSendMessageToGame_Nodel = 2;

    @SuppressLint("HandlerLeak")
    Handler my_handler = new Handler()
    {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.what == 1)
            {
                sendMessageToGame(callbacks,msg.arg1, (String) msg.obj);
            }else if(msg.what == 2)
            {
                sendMessageToGame_Nodel(callbacks,msg.arg1, (String) msg.obj);
            }
        }
    };

    public static final int NetworkStatus = 11;
    public static final int BatteryStatus = 12;

    public ExternCall(AgentWeb web,Activity activity) {
        this.web = web;
        this.activity = activity;
        this.callbacks = new SparseArray<ValueCallback<JSONObject>>();
    }

    static class MyCB implements ValueCallback<JSONObject> {
        AgentWeb web;

        public MyCB(AgentWeb web) {
            this.web = web;
        }

        @Override
        public void onReceiveValue(JSONObject jsonObject) {
            try {
                Log.e(TAG,"onReceiveValue" + jsonObject.toString());
                web.getJsAccessEntrace().quickCallJs("extern_back", jsonObject.toString());
            } catch (Exception e) {
                Log.e(TAG,e.getMessage());
            }
        }
    }

    public void sendMessageToGame(SparseArray<ValueCallback<JSONObject>> callbacks, int cmdid, String msg) {
        Log.d(TAG,"call1111: cmdid " + cmdid + " data " + msg);
        ValueCallback<JSONObject> callback = callbacks.get(cmdid);
        if (callback != null) {
            JSONObject data = new JSONObject();
            try {
                Log.i(TAG, "call: cmdid " + cmdid + " data " + msg);
                data.put("cmdid", cmdid);
                data.put("data", msg);
            } catch (JSONException e) {
                Log.e(TAG,e.getMessage());
            }
            callback.onReceiveValue(data);
            callbacks.remove(cmdid);
        }
    }

    public void sendMessageToGame_Nodel(SparseArray<ValueCallback<JSONObject>> callbacks,int cmdid, String msg) {
        Log.d(TAG,"call1111: cmdid " + cmdid + " data " + msg);
        ValueCallback<JSONObject> callback = callbacks.get(cmdid);
        if (callback != null) {
            JSONObject data = new JSONObject();
            try {
                Log.i(TAG, "call: cmdid " + cmdid + " data " + msg);
                data.put("cmdid", cmdid);
                data.put("data", msg);
            } catch (JSONException e) {
                Log.e(TAG,e.getMessage());
            }
            callback.onReceiveValue(data);
        }
    }

    public void call(int cmd,int id,JSONObject body,boolean is_destroy)
    {
        add(id,new MyCB(web));

        switch (cmd)
        {
            case 210:
                Oaid.go(callbacks,id,body,activity);
                break;
            case 212:
                Reg.go(callbacks,id,body,activity);
                break;
            case 211:
                WebView.go(callbacks,id,body,activity);
                break;
            case 10:
                try {
                    Toast.makeText(activity,body.getString("msg"),body.getInt("dur")).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case BatteryStatus:
                IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                BroadcastReceiver receiver = new BatteryReceiver(id,my_handler);
                activity.registerReceiver(receiver, filter);
                break;
            case NetworkStatus:
                new NetMonitor(id,my_handler,activity).monitor();
                break;
        }

        if(is_destroy)
        {
            rm(id);
        }
    }

    private void add(int id,MyCB cb) {
        if(callbacks.get(id) == null)
            callbacks.put(id,cb);
    }
    private void rm(int id) {
        if(callbacks.get(id) != null)
            callbacks.remove(id);
    }
}
