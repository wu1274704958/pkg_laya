package com.huolong.hf;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.webkit.ValueCallback;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.huolong.hf.utils.BatteryReceiver;
import com.huolong.hf.utils.NetMonitor;
import com.huolong.hf.utils.Utils;
import com.just.agentwebX5.AgentWebX5;
import com.plug.oaid.Oaid;
import com.plug.reg.Reg;
import com.plug.wv.WebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import one.huolong.online.R;


public class ExternCall {
    private static final String TAG = "ExternCall";
    AgentWebX5 web;
    SparseArray<ValueCallback<JSONObject>> callbacks;
    SparseBooleanArray destroy_map;
    Activity activity;
    public static final int WSendMessageToGame = 1;
    public static final int WSendMessageToGame_Nodel = 2;

    Handler my_handler = new Handler( new Handler.Callback()
    {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if(msg.what == 1)
            {
                sendMessageToGame(callbacks,msg.arg1, (String) msg.obj);
            }else if(msg.what == 2)
            {
                sendMessageToGame_Nodel(callbacks,msg.arg1, (String) msg.obj);
            }
            return true;
        }
    });

    public static final int NetworkStatus = 11;
    public static final int BatteryStatus = 12;
    public static final int ShowEditDialog = 13;
    public static final int CMD_QUICK_REG_NOTIF = 47;
    public static final int CMD_QUICK_ACTION = 48;
    public static final int RegResume = 14;
    public static final int PkgInfo = 15;
    public static final int ALive = 16;
    public static final int GetConfig = 19;

    public ExternCall(AgentWebX5 web,Activity activity) {
        this.web = web;
        this.activity = activity;
        this.callbacks = new SparseArray<ValueCallback<JSONObject>>();
        destroy_map = new SparseBooleanArray();
    }

    static class MyCB implements ValueCallback<JSONObject> {
        AgentWebX5 web;

        public MyCB(AgentWebX5 web) {
            this.web = web;
        }

        @Override
        public void onReceiveValue(JSONObject jsonObject) {
            try {
                Log.e(TAG,"onReceiveValue" + jsonObject.toString());
                //web.getJsEntraceAccess().quickCallJs("extern_back", jsonObject.toString());
                JSBridge.call(jsonObject);
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
            rm(cmdid);
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
            if(destroy_map.get(cmdid,false))
                rm(cmdid);
        }
    }

    private EditDialog editDialog;

    public void call(int cmd,int id,JSONObject body,boolean is_destroy) throws JSONException {
        Log.e(TAG,"call " + cmd + " id " + id + " is_destroy " + is_destroy );
        MyCB cb = new MyCB(web);
        add(id,cb,is_destroy);

        switch (cmd)
        {
            case 300:
                Log.e(body.getString("tag"),body.getString("body"));
                break;
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
                Toast.makeText(activity,body.getString("msg"),body.getInt("dur")).show();
                break;
            case BatteryStatus:
                IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                BroadcastReceiver receiver = new BatteryReceiver(id,my_handler);
                activity.registerReceiver(receiver, filter);
                break;
            case NetworkStatus:
                new NetMonitor(id,my_handler,activity).monitor();
                break;
            case CMD_QUICK_REG_NOTIF:
                Logw.e("CMD_QUICK_REG_NOTIF -----------------");
                QuickSdk.NotifGameCmdId = id;
                QuickSdk.init(activity,my_handler);
                QuickSdk.notifGame(QuickSdk.NOTIF_INIT,QuickSdk.STATE_SUCCESS,new JSONObject());
                break;
            case CMD_QUICK_ACTION:
                Logw.e("action " + body.toString());
                int func = 0;
                JSONArray arr = null;
                try {
                    func = body.getInt("func");
                    if(body.has("args"))
                        arr = body.getJSONArray("args");
                }catch (JSONException e)
                {
                    //messageCallback("args error");
                    Logw.e("args error" + e.getMessage());
                    return;
                }
                Logw.e(func + "----------" + arr);
                QuickSdk.gameCall(func,arr,cb);
                break;
            case ShowEditDialog:
                if(editDialog == null)
                    editDialog = new EditDialog(activity,my_handler, R.style.dialog_input);
                Log.e(TAG,body.toString());
                String res = "";

                if(body.has("res"))
                    res = body.getString("res");
                Log.e(TAG,"res = " + res);
                JSONObject oth = null;
                if(body.has("oth"))
                    oth = body.getJSONObject("oth");
                editDialog.go(id,res,oth);
                break;
            case RegResume:
                reg_resume_cmdid = id;
                break;
            case PkgInfo:
                {
                    JSONObject o = new JSONObject();
                    o.put("ver", Utils.getAppVersionName(activity));
                    o.put("model", Build.MODEL);
                    o.put("channel", "AL");
                    sendMessageToGame(callbacks, id, o.toString());
                    break;
                }
            case ALive:
                {
                    GameAlive = true;
                    break;
                }
            case GetConfig:
                {
                    final int local_id = id;
                    int r = LocalCacheMgr.loadConfig(activity, new LocalCacheMgr.OnLoadConfig() {
                        @Override
                        public void onResult(String s) {
                            JSONObject object = new JSONObject();
                            try {
                                if (s != null) {
                                    object.put("ret", 0);
                                    object.put("val", s);
                                    sendMessageToGame(callbacks, local_id, object.toString());
                                } else {
                                    object.put("ret", -3);
                                    sendMessageToGame(callbacks, local_id, object.toString());
                                }
                            }catch (Exception e)
                            {
                                Logw.e("load config return data err = "+e.getMessage());
                            }
                        }
                    });
                    if(r != 0)
                    {
                        JSONObject object = new JSONObject();
                        object.put("ret",r);
                        sendMessageToGame(callbacks,id,object.toString());
                    }
                    break;
                }
        }
    }


    private void add(int id,MyCB cb,boolean is_destroy) {
        if(callbacks.get(id) == null)
            callbacks.put(id,cb);
        if(destroy_map.get(id,true))
            destroy_map.put(id,is_destroy);
    }
    private void rm(int id) {
        if(callbacks.get(id) != null)
            callbacks.remove(id);
        if(destroy_map.indexOfKey(id) != -1)
            destroy_map.delete(id);
    }

    private int reg_resume_cmdid = -1;
    private long pause_time_point = 0;
    private boolean GameAlive = true;
    public void onResume()
    {
        GameAlive = false;
        if(reg_resume_cmdid > -1) {
            JSONObject o = new JSONObject();
            try {
                o.put("act","onResume");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sendMessageToGame_Nodel(callbacks, reg_resume_cmdid, o.toString());
        }

    }
    public void onPause()
    {
        if(reg_resume_cmdid > -1) {
            JSONObject o = new JSONObject();
            try {
                o.put("act","onPause");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sendMessageToGame_Nodel(callbacks, reg_resume_cmdid, o.toString());
        }

        pause_time_point = System.currentTimeMillis();
    }
}
