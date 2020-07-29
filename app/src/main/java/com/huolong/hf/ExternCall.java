package com.huolong.hf;

import android.app.Activity;
import android.util.Log;
import android.util.SparseArray;
import android.webkit.ValueCallback;

import com.just.agentweb.AgentWeb;
import com.plug.oaid.Oaid;
import com.plug.reg.Reg;

import org.json.JSONException;
import org.json.JSONObject;

public class ExternCall {
    private static final String TAG = "ExternCall";
    AgentWeb web;
    SparseArray<ValueCallback<JSONObject>> callbacks;
    Activity activity;

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
                web.getJsAccessEntrace().quickCallJs("window.extern_back", String.valueOf(jsonObject.getInt("cmdid")),
                        jsonObject.getString("data"));
            } catch (JSONException e) {
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
                Oaid.go(callbacks,cmd,body,activity);
                break;
            case 212:
                Reg.go(callbacks,cmd,body,activity);
                break;
        }

        if(is_destroy)
        {
            rm(id);
        }
    }

    private void add(int id,MyCB cb) {
        if(callbacks.get(id) != null)
            callbacks.put(id,cb);
    }
    private void rm(int id) {
        if(callbacks.get(id) != null)
            callbacks.remove(id);
    }
}
