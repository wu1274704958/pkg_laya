package com.huolong.hf.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.huolong.hf.ExternCall;

import org.json.JSONException;
import org.json.JSONObject;

import static android.content.Context.WIFI_SERVICE;


/**
 * Created by yons on 17/3/8.
 */

public class NetMonitor {
    boolean isFirst = true;
    //network state
    public static final int NETWORK_UNKNOWN = -1;
    public static final int NETWORK_UNAVAILABLE = 0;
    public static final int NETWORK_MOBILE = 1;
    public static final int NETWORK_WIFI = 2;
    private static final int MOBILE_NETWORK_2G = 3;
    private static final int MOBILE_NETWORK_3G = 1;
    private static final int MOBILE_NETWORK_4G = 0;
    private static final int MOBILE_NETWORK_UNKNOWN = -1;
    private final int cmdid;
    private Handler handler;
    private Context context;

    public NetMonitor(int cmdid,Handler handler,Context context)
    {
        this.cmdid = cmdid;
        this.handler = handler;
        this.context = context;
    }

    public static int getNetworkType(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = mTelephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return MOBILE_NETWORK_2G;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
//                 From this link https://goo.gl/R2HOjR ..NETWORK_TYPE_EVDO_0 & NETWORK_TYPE_EVDO_A
//                 EV-DO is an evolution of the CDMA2000 (IS-2000) standard that supports high data rates.
//                 Where CDMA2000 https://goo.gl/1y10WI .CDMA2000 is a family of 3G[1] mobile technology standards for sending voice,
//                 data, and signaling data between mobile phones and cell sites.
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                //For 3g HSDPA , HSPAP(HSPA+) are main  networktype which are under 3g Network
                //But from other constants also it will 3g like HSPA,HSDPA etc which are in 3g case.
                //Some cases are added after  testing(real) in device with 3g enable data
                //and speed also matters to decide 3g network type
                //http://goo.gl/bhtVT
                return MOBILE_NETWORK_3G;
            case TelephonyManager.NETWORK_TYPE_LTE:
                //No specification for the 4g but from wiki
                //I found(LTE (Long-Term Evolution, commonly marketed as 4G LTE))
                //https://goo.gl/9t7yrR
                return MOBILE_NETWORK_4G;
            default:
                return MOBILE_NETWORK_UNKNOWN;
        }
    }

    static class Status{
        int type;
        int lvl = 100;

        public Status(int type) {
            this.type = type;
        }

        public Status(int type, int lvl) {
            this.type = type;
            this.lvl = lvl;
        }
    }

    public Status getNetworkStatus() {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            return new Status(NETWORK_UNAVAILABLE);
        }
        int type = activeNetwork.getType();
        if (type == ConnectivityManager.TYPE_MOBILE) {
            return new Status(NETWORK_MOBILE);
        } else if (type == ConnectivityManager.TYPE_WIFI) {
            WifiManager wifi_service = (WifiManager) context.getSystemService(WIFI_SERVICE);
            if(wifi_service != null)
            {
                WifiInfo wifiInfo = wifi_service.getConnectionInfo();
                return new Status(NETWORK_WIFI,Math.abs(wifiInfo.getRssi()));
            }else
                return new Status(NETWORK_WIFI);
        }
        return new Status(NETWORK_UNKNOWN);
    }

    public void monitor() {
        BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.w("NetMonitor", "Network Type Changed");
                sendNetworkStatus();
            }
        };

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(networkStateReceiver, filter);

        sendNetworkStatus();

    }

    private void sendNetworkStatus() {
        JSONObject data = new JSONObject();
        try {
            Status st = getNetworkStatus();
            data.put("status", st.type);
            data.put("level",st.lvl);
            data.put("accessType", getNetworkType(context));

            Message m = new Message();
            m.arg1 = cmdid;
            m.what = ExternCall.WSendMessageToGame_Nodel;
            m.obj = data.toString();
            handler.sendMessage(m);


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
