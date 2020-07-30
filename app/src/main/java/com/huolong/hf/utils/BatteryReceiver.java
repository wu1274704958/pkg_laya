package com.huolong.hf.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.huolong.hf.ExternCall;

import org.json.JSONException;
import org.json.JSONObject;



public class BatteryReceiver extends BroadcastReceiver {
    private int _cmdid;
    private Handler handler;
    public BatteryReceiver(int cmdid,Handler handler) {
        _cmdid = cmdid;
        this.handler = handler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        JSONObject data = new JSONObject();

        try {
            data.put("level", intent.getExtras().getInt("level"));
            Message m = new Message();
            m.arg1 = _cmdid;
            m.what = ExternCall.WSendMessageToGame_Nodel;
            m.obj = data.toString();
            handler.sendMessage(m);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}