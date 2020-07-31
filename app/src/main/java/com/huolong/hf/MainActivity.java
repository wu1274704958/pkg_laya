package com.huolong.hf;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.PersistableBundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.just.agentweb.AgentWeb;
import com.just.agentweb.WebChromeClient;
import com.plug.wv.FullScreenDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    AgentWeb mAgentWeb;
    private String url = "http://cqcdn.aolonggame.cn/cqres/web_online/index.html";
    //private String url = "http://10.10.6.67:8900/bin/index.html";
    FrameLayout root;
    public static String TAG = "WV";
    private Boolean has_splash = true;
    private boolean auto_hide_splash = false;
    ExternCall externCall;
    private View splash_view;
    FullScreenDialog.OnWVCb cb = new FullScreenDialog.OnWVCb() {
        @Override
        public void onDismiss() {

        }

        @Override
        public void onProcess(int p) {

        }

        @Override
        public void onJsCall(String m,String s) {
            try {
                JSONObject o = new JSONObject(s);
                int cmd = o.getInt("cmd");
                boolean is_destroy = o.getBoolean("is_destroy");
                int id = o.getInt("id");
                JSONObject body = o.getJSONObject("body");
                if(cmd == 9 && !auto_hide_splash)
                {
                    hide_splash();
                    return;
                }
                externCall.call(cmd,id,body,is_destroy);

            } catch (JSONException e) {
                Log.e(TAG,e.getMessage());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QuickSdk.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFormat(PixelFormat.RGBX_8888);
        //设置window背景，默认的背景会有Padding值，不能全屏。当然不一定要是透明，你可以设置其他背景，替换默认的背景即可。
        WindowManager.LayoutParams lp =getWindow().getAttributes();
        lp.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        getWindow().setAttributes(lp);

        //设置一个布局

        setContentView(  R.layout.activity_framelayout);
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
            public void onSystemUiVisibilityChange(int visibility) {
                setHideVirtualKey(getWindow());
            }
        });

        setHideVirtualKey(getWindow());

        root = findViewById(R.id.root);

        QuickSdk.init_(this);

        mAgentWeb = AgentWeb.with(this)//传入Activity
                .setAgentWebParent(root, root.getLayoutParams())//传入AgentWeb 的父控件 ，如果父控件为 RelativeLayout ， 那么第二参数需要传入 RelativeLayout.LayoutParams
                .closeIndicator()
                .setWebChromeClient(mWebChromeClient)
                .createAgentWeb()//
                .go(url);
        mAgentWeb.getAgentWebSettings().getWebSettings().setUserAgentString("Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1");


        if(has_splash) {
            splash_view = create_splash();
            root.addView(splash_view,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        Log.e(TAG,"root "+ root.getChildCount() + (root.getChildAt(0) instanceof ImageView));

        externCall = new ExternCall(mAgentWeb,this);
        mAgentWeb.getJsInterfaceHolder().addJavaObject("native_call", new JsCallAndroidInterface(mAgentWeb, this,cb));
        //mAgentWeb.getJsInterfaceHolder().addJavaObject()
    }

    private void hide_splash()
    {
        if(splash_view != null) {
            root.removeView(splash_view);
        } root.invalidate();
    }

    private WebChromeClient mWebChromeClient=new WebChromeClient(){
        @Override
        public void onProgressChanged(android.webkit.WebView view, int newProgress) {

            cb.onProcess(newProgress);
            if (newProgress==100){
                view.setBackgroundColor(Color.WHITE);
                mAgentWeb.getIndicatorController().finish();
                if(splash_view != null && auto_hide_splash) {
                    root.removeView(splash_view);
                } root.invalidate();

            }else {
                view.setBackgroundColor(Color.BLACK);
            }

        }

        @Override
        public boolean onCreateWindow(android.webkit.WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
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
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Log.e(TAG,"=======  " +  url );
            return false;
        }
    };

    public void setHideVirtualKey(Window window){
        //保持布局状态
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
                //布局位于状态栏下方
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|
                //全屏
                View.SYSTEM_UI_FLAG_FULLSCREEN|
                //隐藏导航栏
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        if (Build.VERSION.SDK_INT>=19){
            uiOptions |= 0x00001000;
        }else{
            uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        }
        window.getDecorView().setSystemUiVisibility(uiOptions);
        if(root != null)
            root.invalidate();
    }

    private View create_splash()
    {
        LinearLayout view = (LinearLayout) LinearLayout.inflate(this,R.layout.splash, null);
        //ImageView iv = new ImageView(this);
        //iv.setImageResource(R.drawable.loding);

        return view;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAgentWeb.getWebLifeCycle().onResume();
        QuickSdk.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAgentWeb.getWebLifeCycle().onPause();
        QuickSdk.onPause(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAgentWeb.getWebLifeCycle().onDestroy();
        QuickSdk.onDestroy(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        QuickSdk.gameActivity_onNewIntent(this,intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        QuickSdk.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        QuickSdk.onActivityResult(this,requestCode,resultCode,data);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        QuickSdk.onBackPress();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        QuickSdk.onRestart(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        QuickSdk.onStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        QuickSdk.onStop(this);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        QuickSdk.onConfigurationChanged(newConfig);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        QuickSdk.onWindowFocusChanged(hasFocus);
    }


}
