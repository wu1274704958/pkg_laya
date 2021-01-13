package com.plug.oaid;

import android.app.Activity;
import android.util.Log;
import android.util.SparseArray;
import android.webkit.ValueCallback;


import com.plug.oaid.oaid_tool.helpers.DevicesIDsHelper;

import org.json.JSONException;
import org.json.JSONObject;

public class Oaid {


    private static final String TAG = "OAID";

    public static void sendMessageToGame(SparseArray<ValueCallback<JSONObject>> callbacks,int cmdid, String msg) {
        Log.d(TAG,"call1111: cmdid " + cmdid + " data " + msg);
        ValueCallback<JSONObject> callback = callbacks.get(cmdid);
        if (callback != null) {
            JSONObject data = new JSONObject();
            try {
                Log.i(TAG, "call: cmdid " + cmdid + " data " + msg);
                data.put("cmdid", cmdid);
                data.put("data", msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            callback.onReceiveValue(data);
            callbacks.remove(cmdid);
        }
    }

    public static void sendMessageToGame_Nodel(SparseArray<ValueCallback<JSONObject>> callbacks,int cmdid, String msg) {
        Log.d(TAG,"call1111: cmdid " + cmdid + " data " + msg);
        ValueCallback<JSONObject> callback = callbacks.get(cmdid);
        if (callback != null) {
            JSONObject data = new JSONObject();
            try {
                Log.i(TAG, "call: cmdid " + cmdid + " data " + msg);
                data.put("cmdid", cmdid);
                data.put("data", msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            callback.onReceiveValue(data);
        }
    }

    public static void go(final SparseArray<ValueCallback<JSONObject>> callbacks,
                          final int cmdid, JSONObject body, final Activity activity)
    {
        JSONObject obj = new JSONObject();
        String duid = DeviceIdUtils.getDeviceId(activity);
        try {
            obj.put("duid", duid == null ? "" : duid);
            obj.put("oaid", "" );
        } catch (JSONException e) {
            Log.e(TAG, "err = " + e.getMessage());
        }
        Log.e(TAG,obj.toString());
        sendMessageToGame(callbacks, cmdid, obj.toString());

    }


}
