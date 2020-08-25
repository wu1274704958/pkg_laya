package com.plug.wv;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.VideoView;


import com.just.agentwebX5.AgentWebX5;
import com.tencent.smtt.export.external.interfaces.ConsoleMessage;
import com.tencent.smtt.export.external.interfaces.JsResult;
import com.tencent.smtt.sdk.WebChromeClient;

import java.lang.reflect.Method;


/**
 * Created by wws
 * Date is 2019/5/7
 **/
public class FullScreenDialog extends Dialog {
    AgentWebX5 mAgentWeb;
    private Context context;
    private AlertDialog alertDialog;
    private String url;
    VideoView iv;
    FrameLayout root;
    private String video_name;
    public static String TAG = "WV";
    OnWVCb cb;

    public interface OnWVCb{
        void onDismiss();
        void onProcess(int p);
        void onJsCall(String m,String s);
    }


    public FullScreenDialog(Context context,String url,String video_name,OnWVCb cb) {
        super(context);
        this.context = context;
        this.url=url;
        this.video_name = video_name;
        this.cb = cb;
    }

    int getId(String n,String ty)
    {
        return context.getResources().getIdentifier(n,ty,context.getPackageName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //设置window背景，默认的背景会有Padding值，不能全屏。当然不一定要是透明，你可以设置其他背景，替换默认的背景即可。


        //设置一个布局

        setContentView(  getId("activity_framelayout","layout"));
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        //一定要在setContentView之后调用，否则无效
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int i) {
                int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        //布局位于状态栏下方
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        //全屏
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        //隐藏导航栏
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                if (Build.VERSION.SDK_INT >= 19) {
                    uiOptions |= 0x00001000;
                } else {
                    uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                }
                FullScreenDialog.this.getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            }
        });

        root = (FrameLayout) findViewById(getId("root","id"));

        if(video_name != null) {
            iv = new FullScreenVV(context);
            iv.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                    mp.start();
                }
            });
            iv.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.start();
                }
            });
            String uri = "file:///android_asset/" + video_name;
            iv.setVideoURI(Uri.parse(uri));
            iv.requestFocus();

            root.addView(iv, 1, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        mAgentWeb = AgentWebX5.with((Activity) context)//传入Activity
                .setAgentWebParent(root, root.getLayoutParams())//传入AgentWeb 的父控件 ，如果父控件为 RelativeLayout ， 那么第二参数需要传入 RelativeLayout.LayoutParams
                .useDefaultIndicator()// 使用默认进度条
                .defaultProgressBarColor()
                .setWebChromeClient(mWebChromeClient)
                .createAgentWeb()//
                .go(url);



        Log.e(TAG,"root "+ root.getChildCount() + (root.getChildAt(0) instanceof ImageView));

        mAgentWeb.getJsInterfaceHolder().addJavaObject("android", new JsCallAndroidInterface(this,mAgentWeb, (Activity)context,cb));
    }

    private WebChromeClient mWebChromeClient=new WebChromeClient(){
        @Override
        public void onProgressChanged( com.tencent.smtt.sdk.WebView view, int newProgress) {

            cb.onProcess(newProgress);
            //do you work
            if (newProgress==100){
                //WaitDialog.dismiss();
                view.setBackgroundColor(Color.WHITE);
                if(iv != null) {
                    iv.stopPlayback();
                    root.removeView(iv);
                } root.invalidate();

            }else {
                view.setBackgroundColor(Color.BLACK);
            }

        }

        @Override
        public boolean onCreateWindow(com.tencent.smtt.sdk.WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                view.getSettings().setAllowUniversalAccessFromFileURLs(true);
                view.getSettings().setAllowFileAccessFromFileURLs(true);
            }else{
                try {
                    Class<?> clazz = view.getSettings().getClass();
                    Method method = clazz.getMethod("setAllowUniversalAccessFromFileURLs", boolean.class);
                    if (method != null) {
                        method.invoke(view.getSettings(), true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            view.getSettings().setAppCacheEnabled(true);
            return true;
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if(consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.TIP)
            {
                Log.v("WVCM",consoleMessage.message());
            }else
            if(consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.DEBUG)
            {
                Log.d("WVCM",consoleMessage.message());
            }else
            if(consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.LOG)
            {
                Log.i("WVCM",consoleMessage.message());
            }else
            if(consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.WARNING)
            {
                Log.w("WVCM",consoleMessage.message());
            }else
            if(consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR)
            {
                Log.e("WVCM",consoleMessage.message());
            }
             return true;
        }

        @Override
        public boolean onJsAlert(com.tencent.smtt.sdk.WebView view, String url, String message, JsResult result) {
            Log.e(TAG,"=======  " +  url );
            return false;
        }
    };
    @Override
    public void show() {
        super.show();
    }

    public void callJs(String method,String s) {
        Log.e(TAG,".... " + mAgentWeb + " " + method + " " + s);
        mAgentWeb.getJsEntraceAccess().quickCallJs(method,s);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        cb.onDismiss();
    }
}
