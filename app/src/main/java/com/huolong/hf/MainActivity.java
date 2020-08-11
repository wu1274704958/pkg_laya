package com.huolong.hf;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ContentLoadingProgressBar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.inputmethodservice.Keyboard;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.just.agentweb.AgentWeb;
import com.just.agentweb.WebChromeClient;
import com.plug.wv.FullScreenDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

import one.chuanqi.online.BuildConfig;
import one.chuanqi.online.R;

import static android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK;

public class MainActivity extends Activity {

    private Handler handler = new Handler( new Handler.Callback(){
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what)
            {
                case 0:
                    if(sp_tv != null && msg.obj != null) sp_tv.setText(msg.obj.toString());
                    break;
                case 1:
                    hide_splash();
                    break;
                default:

                    break;
            }
            return true;
        }
    });
    AgentWeb mAgentWeb;
    private String url = "http://cqcdn.aolonggame.cn/cqres/web_online/index.php";
    //private String url = "http://47.102.115.132:8081/cqres/web_online/index.php";
    //private String url = "http://10.10.6.67:8900/bin/index.html";
    FrameLayout root;
    public static String TAG = "WV";
    private Boolean has_splash = true;
    private boolean auto_hide_splash = false;
    ExternCall externCall;
    private View splash_view;
    private LocalCacheMgr cacheMgr;
    private ContentLoadingProgressBar pb;
    private Animation scale;
    private AlertDialog exit_dialog;
    private boolean is_hide_splash = true,need_hide_splash = false;
    private TextView sp_tv;
    private int splash_ani_d = 0;
    private long loding_time = 0;
    FullScreenDialog.OnWVCb cb = new FullScreenDialog.OnWVCb() {
        @Override
        public void onDismiss() {

        }

        @Override
        public void onProcess(int p) {
            if(pb!=null)
                pb.setProgress(p);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
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

        scale = AnimationUtils.loadAnimation(this,R.anim.up);

        cacheMgr = new LocalCacheMgr(url,this);
        setHideVirtualKey(getWindow());

        root = findViewById(R.id.root);

        QuickSdk.init(this);

        mAgentWeb = AgentWeb.with(this)//传入Activity
                .setAgentWebParent(root, root.getLayoutParams())//传入AgentWeb 的父控件 ，如果父控件为 RelativeLayout ， 那么第二参数需要传入 RelativeLayout.LayoutParams
                .closeIndicator()
                .setWebChromeClient(mWebChromeClient)
                .setWebViewClient(mWebViewClient)
                .createAgentWeb()//
                .go(url);
        mAgentWeb.getAgentWebSettings().getWebSettings().setUserAgentString("Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1");
        mAgentWeb.getAgentWebSettings().getWebSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mAgentWeb.getAgentWebSettings().getWebSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);


        if(has_splash) {
            is_hide_splash = false;
            splash_view = create_splash();
            pb = splash_view.findViewById(R.id.pb1);
            sp_tv = splash_view.findViewById(R.id.sp_tv2);
            Logw.e("pb == null = " + (pb == null));
            root.addView(splash_view,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            launch_ani();
        }

        Log.e(TAG,"root "+ root.getChildCount() + (root.getChildAt(0) instanceof ImageView));

        externCall = new ExternCall(mAgentWeb,this);
        mAgentWeb.getJsInterfaceHolder().addJavaObject("native_call", new JsCallAndroidInterface(mAgentWeb, this,cb));
        //mAgentWeb.getJsInterfaceHolder().addJavaObject()
    }

    private void launch_ani() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!is_hide_splash)
                {
                    if(need_hide_splash && loding_time >= 2000) {
                        handler.sendEmptyMessage(1);
                        return;
                    }

                    int a = splash_ani_d % 5;
                    StringBuilder sb = new StringBuilder();
                    for(int i = 0;i < 5;++i)
                    {
                        if(a == i) sb.append('▷'); else sb.append('▶');
                    }
                    Message m = new Message();
                    m.what = 0;
                    m.obj = sb.toString();
                    handler.sendMessage(m);

                    ++splash_ani_d;

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    loding_time += 100;
                }
            }
        }).start();
    }

    private void hide_splash()
    {
        if(loding_time < 2000)
        {
            need_hide_splash = true;
            return;
        }
        if(splash_view != null) {
            splash_view.startAnimation(scale);
            new Handler(getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    root.removeView(splash_view);
                    is_hide_splash = true;
                    //root.invalidate();
                }
            },scale.getDuration());

        }

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
                } //root.invalidate();

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
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            //view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
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

    private com.just.agentweb.WebViewClient mWebViewClient = new com.just.agentweb.WebViewClient()
    {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return cacheMgr.shouldInterceptRequest(view,request);
            }
            return null;
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
        //if(root != null)
            //root.invalidate();
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN)
        {
            hook_back();
            return  true;
        }
        return super.onKeyDown(keyCode,event);
    }

    public void hook_back()
    {
        if(exit_dialog == null) {
            TextView tv = new TextView(this);
            tv.setText("确定要退出游戏吗?");
            tv.setTextColor(getResources().getColor( R.color.colorPrimaryDark) );
            tv.setTextSize(16.f);
            tv.setPadding(39,5,5,5);

            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.dialog);
            exit_dialog = builder.setCancelable(true)
                    .setPositiveButton("退出", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            MainActivity.this.finish();
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .setView(tv)
                    //.setMessage("确定要退出游戏吗?")
                    .setTitle("提示")
                    .create();
        }
        exit_dialog.show();
    }


}
