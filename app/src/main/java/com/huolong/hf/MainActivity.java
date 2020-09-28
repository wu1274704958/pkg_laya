package com.huolong.hf;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ContentLoadingProgressBar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
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
import android.os.Debug;
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
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gzlxq.yhcq.R;
import com.huolong.hf.utils.NewPkgMgr;
import com.huolong.hf.utils.RestartAPPTool;
import com.huolong.hf.utils.Utils;
import com.just.agentwebX5.AgentWebX5;
import com.plug.wv.FullScreenDialog;
import com.tencent.smtt.export.external.interfaces.ConsoleMessage;
import com.tencent.smtt.export.external.interfaces.JsResult;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
                case 2:
                    if(!new_pkg_mgr.is_network_busy()) {
                        is_pop_longtime = true;
                        pop_error_dialog("网络连接超时，请您检查网络状态!");
                    }
                    break;
                case 4:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        update_memory_info();
                    }
                    break;
                default:

                    break;
            }
            return true;
        }
    });
    AgentWebX5 mAgentWeb;
    //private String url = "http://debugtbs.qq.com";
    private String url = "http://cqcdn.aolonggame.cn/cqres/web_online_7r/index.php";
    //private String url = "http://47.102.115.132:8081/cqres/web_online/index.php";
    //private String url = "http://10.10.6.67:8900/bin/index.html";
    FrameLayout root;
    public static String TAG = "WV";
    private Boolean has_splash = true;
    private boolean has_memory_info = false;
    private boolean auto_hide_splash = false;
    ExternCall externCall;
    private View splash_view;
    private LocalCacheMgr cacheMgr;
    private ContentLoadingProgressBar pb;
    private Animation scale;
    private AlertDialog exit_dialog;
    private boolean is_hide_splash = true,need_hide_splash = false,is_pop_longtime = false;
    private TextView sp_tv;
    private int splash_ani_d = 0;
    private long loding_time = 0;
    private boolean software = false;
    private TextView tv_ver;
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
    private AlertDialog err_dialog;
    private TextView err_dialog_tv;
    private NewPkgMgr new_pkg_mgr;
    private Runnable on_resume_task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QuickSdk.onCreate(savedInstanceState,this);
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
        if(!software)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
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

        new_pkg_mgr = new NewPkgMgr(this);
        new_pkg_mgr.load_config();
        go();

        Log.e("X5Init","canLoadX5 " +  QbSdk.canLoadX5(this));

        //mAgentWeb.getJsInterfaceHolder().addJavaObject()
    }

    private void go()
    {
        mAgentWeb = AgentWebX5.with(this)//传入Activity
                .setAgentWebParent(root, root.getLayoutParams())//传入AgentWeb 的父控件 ，如果父控件为 RelativeLayout ， 那么第二参数需要传入 RelativeLayout.LayoutParams
                .useDefaultIndicator()
                .defaultProgressBarColor()
                .setWebChromeClient(mWebChromeClient)
                .setWebViewClient(mWebViewClient)
                .createAgentWeb()//
                .go(url);
        mAgentWeb.getWebSettings().getWebSettings().setUserAgentString("Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1");
        mAgentWeb.getWebSettings().getWebSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        if(!software)mAgentWeb.getWebSettings().getWebSettings().setRenderPriority(com.tencent.smtt.sdk.WebSettings.RenderPriority.HIGH);


        if(has_splash) {
            is_hide_splash = false;
            splash_view = create_splash();
            pb = splash_view.findViewById(R.id.pb1);
            sp_tv = splash_view.findViewById(R.id.sp_tv2);
            tv_ver = splash_view.findViewById(R.id.tv_ver);
            tv_ver.setText("v"+Utils.getAppVersionName(this) + "_x5_"+ (QbSdk.canLoadX5(this) ? "1":"0"));
            Logw.e("pb == null = " + (pb == null));
            root.addView(splash_view,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            launch_ani();
        }

        if(has_memory_info) {
            mem_info = (LinearLayout) create_mem_info();
            root.addView(mem_info,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            launch_update_mem();
        }

        //if( !QbSdk.canLoadX5(this))
        //{
        //    pop_install_x5_dialog();
        //}

        Log.e(TAG,"root "+ root.getChildCount() + (root.getChildAt(0) instanceof ImageView));


        mAgentWeb.getJsInterfaceHolder().addJavaObject("native_call", new JsCallAndroidInterface(mAgentWeb, this,cb));
        externCall = new ExternCall(mAgentWeb,this);
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
                    if(!is_pop_longtime && loding_time >= 40000)
                    {
                        handler.sendEmptyMessage(2);
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
        if(is_hide_splash) return;
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

    private com.tencent.smtt.sdk.WebChromeClient mWebChromeClient=new com.tencent.smtt.sdk.WebChromeClient(){
        @Override
        public void onProgressChanged(WebView view, int newProgress) {

            cb.onProcess(newProgress);
            if (newProgress==100){
                view.setBackgroundColor(Color.WHITE);
                //mAgentWeb.getIndicatorController().offerIndicator();
                if(splash_view != null && auto_hide_splash) {
                   // root.removeView(splash_view);
                    hide_splash();
                } //root.invalidate();

            }else {
                view.setBackgroundColor(Color.BLACK);
            }

        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
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
            if(!software)
                view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            else
                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            view.getSettings().setAppCacheEnabled(true);
            return true;
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if(consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.TIP)
            {
                Log.v("WVCM",consoleMessage.message()+ " " + consoleMessage.sourceId() + "   " +  consoleMessage.lineNumber());
            }else
            if(consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.DEBUG)
            {
                Log.d("WVCM",consoleMessage.message()+ " " + consoleMessage.sourceId() + "   " +  consoleMessage.lineNumber());
            }else
            if(consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.LOG)
            {
                Log.i("WVCM",consoleMessage.message()+ " " + consoleMessage.sourceId() + "   " +  consoleMessage.lineNumber());
            }else
            if(consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.WARNING)
            {
                Log.w("WVCM",consoleMessage.message()+ " " + consoleMessage.sourceId() + "   " +  consoleMessage.lineNumber());
            }else
            if(consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR)
            {
                String s = consoleMessage.message() + " " + consoleMessage.sourceId() +  " " +  consoleMessage.lineNumber();
                upload_err_msg(s);

                if(!is_hide_splash)
                {
                    //pop_error_dialog("游戏加载错误!");
                }
                Log.e("WVCM",s);
            }
            return true;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Log.e(TAG,"=======  " +  url );
            return false;
        }
    };

    private void upload_err_msg(String s) {
        StringBuffer model = new StringBuffer( Build.MODEL);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        model.append(" ");
        model.append(s);

        JSONObject o = new JSONObject();
        try{
            o.put("token","zHMDVSuveFtK.EngLjZvWrpejEhXboRptbnIdCUY1pfLye7VDbVNvxUWXdceT.IXfkSLOfkp4Nl10J.fAii.sJJcZaxS6YUR0-RniOyx094!");
            o.put("appid","1000000010");
            o.put("type",3);
            o.put("text",model.toString());
        }catch (Exception e)
        {
            Log.e("upload_err_msg",e.getMessage());
        }
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://cquc.xianyul.com/api/feedback")
                .post(RequestBody.create(JSON,o.toString()))
                .build();
        okHttpClient.newCall(request).enqueue(new Callback(){

            @Override
            public void onResponse( Call call,  Response response) throws IOException {
                if(response.code() == 200)
                {
                    try {
                        String res = response.body().string();
                        Log.e("upload_err_msg", "upload success " + res);
                    }catch (Exception e){}
                }else {
                    Log.e("upload_err_msg","upload failed code = " + response.code());
                }
                response.close();
            }

            @Override
            public void onFailure( Call call,  IOException e) {
                Log.e("upload_err_msg","upload failed msg = " + e.getMessage());
            }
        });

    }

    private void pop_error_dialog(String s) {

        if(err_dialog == null) {

            err_dialog_tv = new TextView(this);
            err_dialog_tv.setText(s);
            err_dialog_tv.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
            err_dialog_tv.setTextSize(16.f);
            err_dialog_tv.setPadding(39, 5, 5, 5);

            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.dialog);
            err_dialog = builder.setCancelable(true)
                    .setPositiveButton("重试", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            restart(MainActivity.this,MainActivity.class);
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .setView(err_dialog_tv)
                    //.setMessage("确定要退出游戏吗?")
                    .setTitle("提示")
                    .create();
        }else{
            err_dialog_tv.setText(s);
        }
        err_dialog.show();
    }

    private com.tencent.smtt.sdk.WebViewClient mWebViewClient = new com.tencent.smtt.sdk.WebViewClient()
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
        if(mAgentWeb != null)
            mAgentWeb.getWebLifeCycle().onResume();
        QuickSdk.onResume(this);
        externCall.onResume();
        run_on_resume_task();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mAgentWeb != null)
            mAgentWeb.getWebLifeCycle().onPause();
        QuickSdk.onPause(this);
        externCall.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mAgentWeb != null)
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
        //QuickSdk.onBackPress();
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
            if(!QuickSdk.onBackPress(new Runnable() {
                @Override
                public void run() {
                    hook_back();
                }
            })) {
                hook_back();
            }
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
                            QuickSdk.requestExit();
                            System.exit(0);
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

    void restart(Context context,Class clzss)
    {
        RestartAPPTool.restartAPP2(this,1900);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        QuickSdk.activityAttachBaseContext(newBase);
    }

    private LinearLayout mem_info;
    private TextView[] mem_tvs;

    View create_mem_info()
    {
        LinearLayout view = (LinearLayout) LinearLayout.inflate(this,R.layout.memory, null);

        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void update_memory_info()
    {

        if(mem_info != null) {

            if(mem_tvs == null)
            {
                mem_tvs = new TextView[3];
                for(int i = 0;i < 3;++i)
                {
                    mem_tvs[i] = mem_info.findViewById( getResources().getIdentifier("mem_t"+(i + 1),"id",getPackageName()));
                }
            }

            ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();

            mActivityManager.getMemoryInfo(memoryInfo);
            update_memory2();
            mem_tvs[1].setText(String.format("%s", (double) memoryInfo.availMem / 1000000));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mem_tvs[2].setText(String.format("%s", (double) memoryInfo.totalMem / 1000000));
            }
            String src = mem_tvs[2].getText().toString();
            mem_tvs[2].setText(String.format("%s free : %s max : %s",src,
                    Runtime.getRuntime().freeMemory(),
                    Runtime.getRuntime().maxMemory()
            ));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void update_memory2()
    {

        if(mem_info != null) {

            if (mem_tvs == null) {
                mem_tvs = new TextView[3];
                for (int i = 0; i < 3; ++i) {
                    mem_tvs[i] = mem_info.findViewById(getResources().getIdentifier("mem_t" + (i + 1), "id", getPackageName()));
                }
            }
            ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            Debug.MemoryInfo[] memoryInfos = mActivityManager
                    .getProcessMemoryInfo(new int[]{android.os.Process.myPid()});

            String java_mem = memoryInfos[0].getMemoryStat("summary.java-heap");

            String native_mem = memoryInfos[0].getMemoryStat("summary.native-heap");

            String code_mem = memoryInfos[0].getMemoryStat("summary.code");

            String stack_mem = memoryInfos[0].getMemoryStat("summary.stack");

            String graphics_mem = memoryInfos[0].getMemoryStat("summary.graphics");

            String private_other_mem = memoryInfos[0].getMemoryStat("summary.private-other");

            String system_mem = memoryInfos[0].getMemoryStat("summary.system");

            String total_pss_mem = memoryInfos[0].getMemoryStat("summary.total-pss");

            String total_swap_mem = memoryInfos[0].getMemoryStat("summary.total-swap");

            mem_tvs[0].setText(String.format("" +
                            "java_mem:%s\nnative_mem:%s\ncode_mem:%s\nstack_mem:%s\ngraphics_mem:%s\nprivate_other_mem:%s\nsystem_mem:%s\ntotal_pss_mem:%s\ntotal_swap_mem:%s",
                    Float.parseFloat(java_mem) / 1024.f,
                    Float.parseFloat(native_mem) / 1024.f,
                    Float.parseFloat(code_mem) / 1024.f,
                    Float.parseFloat(stack_mem) / 1024.f,
                    Float.parseFloat(graphics_mem) / 1024.f,
                    Float.parseFloat(private_other_mem) / 1024.f,
                    Float.parseFloat(system_mem) / 1024.f,
                    Float.parseFloat(total_pss_mem) / 1024.f,
                    Float.parseFloat(total_swap_mem) / 1024.f)
            );
        }
    }

    void launch_update_mem()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true)
                {
                    handler.sendEmptyMessage(4);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void pop_install_x5_dialog() {
         TextView err_dialog_tv = new TextView(this);
         err_dialog_tv.setText("检测到没有安装腾讯X5内核,是否前往安装?");
         err_dialog_tv.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
         err_dialog_tv.setTextSize(16.f);
         err_dialog_tv.setPadding(39, 5, 5, 5);
         AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.dialog);
         AlertDialog err_dialog = builder.setCancelable(true)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            hide_splash();
                            mAgentWeb.getLoader().loadUrl("http://debugtbs.qq.com");
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .setView(err_dialog_tv)
                    //.setMessage("确定要退出游戏吗?")
                    .setTitle("提示")
                    .create();

        err_dialog.show();
    }

    public void set_on_resume_task( Runnable task)
    {
        on_resume_task = task;
    }

    void run_on_resume_task()
    {
        if(on_resume_task != null)
        {
            on_resume_task.run();
            on_resume_task = null;
        }
    }

    public String getGoUrl()
    {
        return url;
    }

}
