package com.plug.reg;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.ParseException;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.webkit.ValueCallback;

import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class Reg {

    private static final String TAG = "REG";

    static int getId(Context context,String n,String ty)
    {
        return context.getResources().getIdentifier(n,ty,context.getPackageName());
    }

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

    static SparseArray<ValueCallback<JSONObject>> CBS = new SparseArray<>();
    static int CMDID = 0;

    public static void go(final SparseArray<ValueCallback<JSONObject>> callbacks,
                          final int cmdid, JSONObject body, final Activity activity)
    {
        CBS = callbacks;
        CMDID = cmdid;

        try{
            int f = body.getInt("func");
            if(f == 1)
            {
                int len = body.getInt("len");
                sendMessageToGame(CBS,CMDID,"{\"ret\":0,\"msg\":\""+rand_str(len)+"\"}");
            }else
            if(f == 2){
                String acc = body.getString("acc");
                String pwd = body.getString("pwd");
                SaveN = 0;
                RequestTimes = 0;
                SaveImage(activity,acc,pwd);
            }

        }catch (Exception e)
        {
            sendMessageToGame(CBS,CMDID,"{\"ret\":-3,\"msg\":\""+e.getMessage()+"\"}");
        }

    }

    public static String rand_str(int l)
    {
        Date da = new Date();
        long ns = da.getTime();
        ArrayList<Integer> al = new ArrayList<>();
        while (ns > 1)
        {
            al.add( (int)(ns % 10) );
            ns /= 10;
        }
        StringBuilder res = new StringBuilder();
        if( l > al.size() ) l = al.size();
        for(int i = 0;i < l;++i)
        {
            res.append((char) (al.get(i) + new Random().nextInt(16) + 97 ) );
        }

        return res.toString();
    }

    static final String[] Permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE};
    static final int MaxRequestTimes = 3;
    static int RequestTimes = 0;
    static int SaveN = 0;

    public static void SaveImage(final Context context, final String acc, final String pwd)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if(has_not_granted(context,Permissions) == null)
            {
                if(SaveN >= 1) return;
                ++SaveN;
                SaveImageReal(context,acc,pwd);
            }else {
                if(RequestTimes >= MaxRequestTimes)
                {
                    sendMessageToGame(CBS,CMDID,"{\"ret\":-1,\"msg\":\"权限请求失败!\"}");
                    return;
                }
                ActivityCompat.requestPermissions((Activity) context, Permissions, 2);
                ++RequestTimes;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        SaveImage(context, acc, pwd);
                    }
                }, 2000);
            }
        }else {
            if(SaveN >= 1) return;
            ++SaveN;
            SaveImageReal(context,acc,pwd);
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
            return (String[]) res.toArray();
    }

    private static Object[] has_not_granted(Context context,String[] permissions)
    {
        ArrayList<String> res = new ArrayList<>();
        for(int i = 0;i < permissions.length;++i)
        {
            int r = PackageManager.PERMISSION_GRANTED;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                r = context.checkSelfPermission(permissions[i]);
            }
            if(r == PackageManager.PERMISSION_DENIED)
            {
                res.add(permissions[i]);
            }
        }
        if(res.isEmpty())
            return null;
        else
            return res.toArray();
    }

    public static void SaveImageReal(Context context,String acc,String pwd)
    {
        Bitmap b = BitmapFactory.decodeResource(context.getResources(), getId(context,"template","drawable"));
        Bitmap c = write_bitmap(b,acc,pwd);
        new Reg.SavePhoto(context, new SavePhoto.Cb() {
            @Override
            public void onError(String msg) {
                sendMessageToGame(CBS,CMDID,"{\"ret\":-2,\"msg\":\""+msg+"\"}");
            }

            @Override
            public void onSuccess() {
                sendMessageToGame(CBS,CMDID,"{\"ret\":0,\"msg\":\"成功!\"}");
            }
        }).saveBitmap(c,null);
    }

    private static Bitmap write_bitmap(Bitmap b, String acc, String pwd) {

        Paint paint = new Paint();
        paint.setTextSize(32);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.WHITE);

        Paint.FontMetricsInt fm = paint.getFontMetricsInt();

        int width = (int)paint.measureText(acc);
        int height = fm.descent - fm.ascent;

        Bitmap bitmap = Bitmap.createBitmap(594, 507, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(b,new Rect(0,0,(int)b.getWidth(),(int)b.getHeight()),
                new Rect(0,0,(int)bitmap.getWidth(),(int)bitmap.getHeight()),paint);
        paint.setColor(Color.BLACK);
        canvas.drawText(acc, 0.218855f * bitmap.getWidth(),0.32996f * bitmap.getHeight()  , paint);
        canvas.drawText(pwd, 0.218855f * bitmap.getWidth(),0.49089f * bitmap.getHeight() , paint);
        canvas.save();


        return  bitmap;
    }

    static public class SavePhoto{
        //存调用该类的活动
        Context context;
        Cb cb;
        public interface Cb{
            void onError(String msg);
            void onSuccess();
        }
        public SavePhoto(Context context,Cb cb) {
            this.context = context;
            this.cb = cb;
        }
        //保存文件的方法：
        public void SaveBitmapFromView(View view) throws ParseException {
            int w = view.getWidth();
            int h = view.getHeight();
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            view.layout(0, 0, w, h);
            view.draw(c);
            // 缩小图片
            Matrix matrix = new Matrix();
            matrix.postScale(0.5f,0.5f); //长和宽放大缩小的比例
            bmp = Bitmap.createBitmap(bmp,0,0,bmp.getWidth(),bmp.getHeight(),matrix,true);
            DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
            saveBitmap(bmp,format.format(new Date()) + ".JPEG");
        }
        /*
         * 保存文件，文件名为当前日期
         */
        public void saveBitmap(Bitmap bitmap, String bitName){
            String fileName ;
            File file ;
            if(bitName == null)
            {
                DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
                bitName = format.format(new Date()) + ".JPEG";
            }
            if(Build.BRAND .equals("Xiaomi") ){ // 小米手机
                fileName = Environment.getExternalStorageDirectory().getPath()+"/DCIM/Camera/"+bitName ;
            }else{ // Meizu 、Oppo

                fileName = Environment.getExternalStorageDirectory().getPath()+"/DCIM/"+bitName ;
            }
            Log.e(TAG,fileName);
            file = new File(fileName);
            if(file.exists()){
                file.delete();
            }
            FileOutputStream out;
            try{
                out = new FileOutputStream(file);
                // 格式为 JPEG，照相机拍出的图片为JPEG格式的，PNG格式的不能显示在相册中
                if(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out))
                {
                    out.flush();
                    out.close();
                    // 插入图库
                    MediaStore.Images.Media.insertImage(context.getContentResolver(), file.getAbsolutePath(), bitName, null);
                }
            }
            catch (FileNotFoundException e)
            {
                Log.e(TAG,e.getMessage());
                cb.onError(e.getMessage());
                return;
            }
            catch (IOException e)
            {
                Log.e(TAG,e.getMessage());
                cb.onError(e.getMessage());
                return;
            }
            // 发送广播，通知刷新图库的显示
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + fileName)));
            cb.onSuccess();
        }
    }


}
