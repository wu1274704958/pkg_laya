package com.huolong.hf;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.util.Log;
import android.webkit.ValueCallback;
import android.content.res.Configuration;

import android.content.Context;


import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

public class QuickSdk{
    public static boolean RequestQuit = false;
    public static final String Product_Code = "55611690941333531013279334936021";
    public static final String Product_Key = "59365275";
    private static boolean InitSuccess = false;
    public static final int OVERLAY_PERMISSION_REQ_CODE = 0xff;

    public static int NotifGameCmdId = Integer.MIN_VALUE;
    public static final int NOTIF_INIT              = 0;
    public static final int NOTIF_LOGIN             = 1;
    public static final int NOTIF_LOGOUT            = 2;
    public static final int NOTIF_SwitchAccount     = 3;
    public static final int NOTIF_PAY               = 4;
    public static final int NOTIF_EXIT              = 5;
    public static final int NOTIF_SetGameRoleInfo   = 26;

    public static final int NOTIF_CONNECT_BILLING   = 6;

    public static final int STATE_SUCCESS           = 0;
    public static final int STATE_FAILED            = 1;
    public static final int STATE_CANCEL            = 2;

    public static final int FUNC_LOGIN              = 0;
    public static final int FUNC_SetGameRoleInfo    = 1;
    public static final int FUNC_PAY                = 2;
    public static final int FUNC_LOGOUT             = 3;
    public static final int FUNC_EXIT               = 4;
    public static final int FUNC_REGISTER           = 5;
    public static final int FUNC_BACK_PRESSED       = 27;

    private static Activity activity;
    private static Handler handler;
    public static void init(Activity activity_,Handler handler_)
    {
        activity = activity_;
        handler = handler_;
    }

    static class Order{
       public String cpOrderID;
       public String amount;
       public String count;
       public String goodsName;
       public String goodsID;

        public Order() {
            super();
        }

        public String getCpOrderID() {
            return cpOrderID;
        }

        public void setCpOrderID(String cpOrderID) {
            this.cpOrderID = cpOrderID;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getCount() {
            return count;
        }

        public void setCount(String count) {
            this.count = count;
        }

        public String getGoodsName() {
            return goodsName;
        }

        public void setGoodsName(String goodsName) {
            this.goodsName = goodsName;
        }

        public String getGoodsID() {
            return goodsID;
        }

        public void setGoodsID(String goodsID) {
            this.goodsID = goodsID;
        }
    }

    static class ServerInfo{
       public String serverID;
       public String serverName;
       public String gameRoleName;
       public String gameRoleID;
       public String gameRoleLevel;
       public String vipLevel;

        public ServerInfo() {
            super();
        }

        public String getServerID() {
            return serverID;
        }

        public void setServerID(String serverID) {
            this.serverID = serverID;
        }

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public String getGameRoleName() {
            return gameRoleName;
        }

        public void setGameRoleName(String gameRoleName) {
            this.gameRoleName = gameRoleName;
        }

        public String getGameRoleID() {
            return gameRoleID;
        }

        public void setGameRoleID(String gameRoleID) {
            this.gameRoleID = gameRoleID;
        }

        public String getGameRoleLevel() {
            return gameRoleLevel;
        }

        public void setGameRoleLevel(String gameRoleLevel) {
            this.gameRoleLevel = gameRoleLevel;
        }

        public String getVipLevel() {
            return vipLevel;
        }

        public void setVipLevel(String vipLevel) {
            this.vipLevel = vipLevel;
        }
    }

    static class RoleInfo{
        public String serverName;
        public String gameRoleName;
        public String gameRoleID;
        public String gameRoleLevel;
        public String vipLevel;

        public RoleInfo() {
            super();
        }

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public String getGameRoleName() {
            return gameRoleName;
        }

        public void setGameRoleName(String gameRoleName) {
            this.gameRoleName = gameRoleName;
        }

        public String getGameRoleID() {
            return gameRoleID;
        }

        public void setGameRoleID(String gameRoleID) {
            this.gameRoleID = gameRoleID;
        }

        public String getGameRoleLevel() {
            return gameRoleLevel;
        }

        public void setGameRoleLevel(String gameRoleLevel) {
            this.gameRoleLevel = gameRoleLevel;
        }

        public String getVipLevel() {
            return vipLevel;
        }

        public void setVipLevel(String vipLevel) {
            this.vipLevel = vipLevel;
        }
    }

    static class SetGameRoleInfoEx{
        public boolean is_enter = false;
        public String ServerName = null;
        public String FamilyName = null;

        public int getMoney() {
            return money;
        }

        public void setMoney(int money) {
            this.money = money;
        }

        public int money = 0;

        public String getFamilyName() {
            return FamilyName;
        }

        public void setFamilyName(String familyName) {
            FamilyName = familyName;
        }

        public boolean getis_enter() {
            return is_enter;
        }

        public void setis_enter(boolean is_enter) {
            this.is_enter = is_enter;
        }

        public String getServerName() {
            return ServerName;
        }

        public void setServerName(String serverName) {
            ServerName = serverName;
        }

        @Override
        public String toString() {
            return "SetGameRoleInfoEx{" +
                    "is_enter=" + is_enter +
                    ", ServerName='" + ServerName + '\'' +
                    '}';
        }
    }


    public static void notifGame(int notif_id,int state_id,JSONObject obj)
    {
        Logw.e("notifGame id = " + notif_id +" state = " + state_id + " obj = " + obj.toString());
        if(NotifGameCmdId != Integer.MIN_VALUE)
        {
            JSONObject jsonObject = new JSONObject();
            if(obj == null)
            {
                obj = new JSONObject();
            }
            try {
                jsonObject.put("id",notif_id);
                jsonObject.put("state",state_id);
                jsonObject.put("data",obj.toString());
            }catch (JSONException e)
            {

            }
            Message m = new Message();
            m.arg1 = NotifGameCmdId;
            m.what = ExternCall.WSendMessageToGame_Nodel;
            m.obj = jsonObject.toString();
            handler.sendMessage(m);
        }
    }



    public static JSONObject toJsonObject(String ...strings)
    {
        JSONObject object = new JSONObject();
        try {
            int i = 1;
            for (String s: strings) {
                object.put("key" + i , s);
                ++i;
            }
        }catch (JSONException e) {}
        return object;
    }


    public static void afterApply()
    {
        Logw.e("afterApply()");

    }

    public static void afterSplash(final Activity activity)
    {
        Logw.e("in afterSplash()");

        Logw.e("pre init__ ()");
        QuickSdk.init_(activity);
    }

    public static void gameActivity_onNewIntent(Activity activity,Intent intent)
    {

    }

    public static void init_(Activity activity)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {

        }else {

        }
    }

    private static boolean RequestPermissionsSuccess = false;
    public static void gameActivity_onRequestPermissionsResult(final Activity activity, int requestCode, String[] permissions, int[] grantResults)
    {
        if(requestCode == 1) {
            final String [] not_granted = has_not_granted(permissions,grantResults);
            if (not_granted == null) {//申请成功
//                if(Build.VERSION.SDK_INT >= 23)
//                {
//                    if(Settings.canDrawOverlays(activity))
//                    {
                //activity.finish();
                init_(activity);
                RequestPermissionsSuccess = true;
//                    }else{
//                        try{
//                            Intent  intent=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
//                            activity.startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
//                        }catch (Exception e)
//                        {
//                            Logw.e("悬浮窗 "+e.getMessage());
//                        }
//                    }
//                }
            } else {
                //失败  这里逻辑以游戏为准 这里只是模拟申请失败 cp方可改为继续申请权限 或者退出游戏 或者其他逻辑
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage("申请失败,点错了?继续申请?");
                builder.setPositiveButton("再次申请", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(activity, not_granted, 1);
                        dialogInterface.dismiss();
                    }
                });
                builder.setNegativeButton("退出", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        QuickSdk.RequestQuit = true;
                        dialogInterface.dismiss();
                        activity.finish();
                    }
                });
                builder.show();
            }
        }
    }

    private static String[] has_not_granted(String[] permissions, int[] grantResults) {
        ArrayList<String> res = new ArrayList<>();
        for(int i = 0;i < grantResults.length;++i)
        {
            int r = grantResults[i];
            if(r == PackageManager.PERMISSION_DENIED)
            {
                res.add(permissions[i]);
            }
        }
        if(res.isEmpty())
            return null;
        else
            return res.toArray(new String[0]);
    }


    public static void gameCall(int func, JSONArray args, ValueCallback<JSONObject> callback)
    {
        try {
            switch (func) {
                case FUNC_LOGIN: {
                    login(activity);
                    break;
                }
                case FUNC_SetGameRoleInfo: {
                    JSONObject oth = null;
                    try{
                        oth = args.getJSONObject(2);
                    }catch (JSONException e)
                    {
                        oth = new JSONObject();
                    }
                    Logw.e("oth = " + oth.toString());

                    SetGameRoleInfoEx exinfo = formJson(SetGameRoleInfoEx.class,oth);
                    Logw.e("exinfo = " + exinfo.toString());
                    setGameRoleInfo(activity,formJson(RoleInfo.class,args.getJSONObject(0)),args.getBoolean(1),exinfo);
                    break;
                }
                case FUNC_LOGOUT:{
                    logout(activity);
                    break;
                }
                case FUNC_EXIT:{
                    exit(activity);
                    break;
                }
                case FUNC_PAY:{
                    JSONObject oth = null;
                    try{
                        oth = args.getJSONObject(2);
                    }catch (JSONException e)
                    {
                        oth = new JSONObject();
                    }
                    Logw.e("oth = " + oth.toString());

                    SetGameRoleInfoEx exinfo = formJson(SetGameRoleInfoEx.class,oth);
                    pay(activity,
                            formJson(Order.class,args.getJSONObject(0)),
                            formJson(ServerInfo.class,args.getJSONObject(1)),exinfo);
                    break;
                }
                case FUNC_BACK_PRESSED:{
                    onBackPress();
                    break;
                }
            }
        }catch (Exception e)
        {
            Logw.e(e.getMessage());
        }
    }

    public static void login(Activity activity)
    {
        Logw.e("login b");

        Logw.e("login e");
    }

    public static void setGameRoleInfo(Activity activity,RoleInfo roleInfo,Boolean is_create,SetGameRoleInfoEx oths)
    {

    }

    public static void logout(Activity activity)
    {

    }

    public static void exit(Activity activity)
    {

    }

    public static void pay(Activity activity,final Order order, ServerInfo serverInfo,SetGameRoleInfoEx oths)
    {

    }

    public static <T> T formJson(Class<T> tClass,JSONObject object) throws JSONException, InstantiationException, IllegalAccessException, InvocationTargetException {
        T res = null;

        res = tClass.newInstance();

        Method[] methods = tClass.getMethods();
        for ( Method f : methods)
        {
            String temp = f.getName();
            if(temp.startsWith("set"))
            {
                String name = temp.substring(3);
                String args_name = f.getParameterTypes()[0].getName();
                Log.e("===","formJson " + name + "  " +args_name);
                f.setAccessible(true);
                if(args_name.equals("int"))
                {
                    if(object.has(name))
                        f.invoke(res,object.getInt(name));
                    else
                        f.invoke(res,0);
                }else if(args_name.equals("double"))
                {
                    if(object.has(name))
                        f.invoke(res,object.getDouble(name));
                    else
                        f.invoke(res,0.0);
                }else if(args_name.equals("long"))
                {
                    if(object.has(name))
                        f.invoke(res,object.getLong(name));
                    else
                        f.invoke(res,0L);
                }else if(args_name.equals("boolean"))
                {
                    if(object.has(name))
                        f.invoke(res,object.getBoolean(name));
                    else
                        f.invoke(res,false);
                }else if(args_name.equals("java.lang.String"))
                {
                    if(object.has(name))
                        f.invoke(res,object.getString(name));
                    else
                        f.invoke(res,"");
                }
            }
// else if(temp.startsWith("get"))
//            {
//                Log.e("===","formJson get " + f.invoke(res));
//            }
        }
        return res;
    }

    public static <T> JSONObject toJson(Class<T> tClass,T t) throws JSONException, IllegalAccessException, InvocationTargetException {
        JSONObject res = new JSONObject();

        Method[] methods = tClass.getMethods();
        for ( Method f : methods)
        {
            String temp = f.getName();
            if(temp.startsWith("get"))
            {
                String name = temp.substring(3);
                String ret_name = f.getReturnType().getName();
                Log.e("===","to Json " + name + "  " +ret_name);
                f.setAccessible(true);
                if(ret_name.equals("int"))
                {
                    Object o = f.invoke(t);
                    if(o != null)
                        res.put(name,(int)o);
                    else
                        res.put(name,0);
                }else if(ret_name.equals("double"))
                {
                    Object o = f.invoke(t);
                    if(o != null)
                        res.put(name,(double)o);
                    else
                        res.put(name,0.0);
                }else if(ret_name.equals("long"))
                {
                    Object o = f.invoke(t);
                    if(o != null)
                        res.put(name,(long)o);
                    else
                        res.put(name,0L);
                }else if(ret_name.equals("boolean"))
                {
                    Object o = f.invoke(t);
                    if(o != null)
                        res.put(name,(boolean)o);
                    else
                        res.put(name,false);
                }else if(ret_name.equals("java.lang.String"))
                {
                    String o = (String) f.invoke(t);
                    if(o != null)
                        res.put(name,o);
                    else
                        res.put(name,"");
                }
            }
        }
        return res;
    }

    public static void load_qk_check()
    {
//        Logw.e("b load library!!!");
//        System.loadLibrary("qkcheck");
//        Logw.e("e load library!!!");
    }

    private static HashMap<String,String> CustomData;

    static {
        CustomData = new HashMap<>();
    }

    public static void init(final Activity activity) {
        try {


        }catch (Exception e)
        {
            Logw.e("init err = " + e.toString());
        }

    }


    public static void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

    }

    public static void onResume(Activity activity) {
    }

    public static void onStart(Activity activity) {
    }

    public static void onRestart(Activity activity) {
    }

    public static void onPause(Activity activity) {
    }

    public static void onStop(Activity activity) {
    }

    public static void onDestroy(Activity activity) {
    }

    public static void onConfigurationChanged(Configuration config) {

    }

    public static void onBackPress()
    {

    }

    public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {

    }

    public static void activityAttachBaseContext(Context context)
    {

    }

    public static void onCreate(Bundle savedInstanceState) {

    }

    public static void onWindowFocusChanged(boolean hasFocus) {

    }
}
