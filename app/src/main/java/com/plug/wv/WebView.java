package com.plug.wv;

import android.app.Activity;
import android.util.Log;
import android.util.SparseArray;
import android.webkit.ValueCallback;

import org.json.JSONException;
import org.json.JSONObject;

public class WebView {
    private static final String TAG = "WV";

    public static void sendMessageToGame(SparseArray<ValueCallback<JSONObject>> callbacks, int cmdid, String msg) {
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
        try{
            String vn = body.has("video_name") ? body.getString("video_name") : null;
            FullScreenDialog d = new FullScreenDialog(activity, body.getString("url"), vn, new FullScreenDialog.OnWVCb() {
                @Override
                public void onDismiss() {
                    sendMessageToGame(callbacks,cmdid,"{\"act\": \"dismiss\"}");
                }

                @Override
                public void onProcess(int p) {
                    sendMessageToGame_Nodel(callbacks,cmdid,"{\"act\": \"process\",\n" +
                            "  \"val\" : "+  p  +"}");
                }

                @Override
                public void onJsCall(String m,String s) {
                    sendMessageToGame_Nodel(callbacks,cmdid,"{\"act\": \""+m+"\",\n" +
                            "  \"val\" :\""+  s  +"\"}");
                }
            });
            d.show();

        }catch (Exception e)
        {
            Log.e(TAG,"err = " + e.getMessage());
        }
    }
}
