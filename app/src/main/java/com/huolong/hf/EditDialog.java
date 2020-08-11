package com.huolong.hf;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.huolong.hf.utils.InputMethodUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;


public class EditDialog extends Dialog {
    private static final String TAG = "EditDialog";
    Handler handler;
    Context context;
    EditText ed;
    Button btn;
    int cmdid = -1;
    String res;
    private LinearLayout touch;

    static class Setting{
        public boolean multline = false;
        public boolean is_number = false;
        public boolean password = false;
        public boolean is_phone = false;

        public boolean getIs_phone() {
            return is_phone;
        }

        public Setting setIs_phone(boolean is_phone) {
            this.is_phone = is_phone;
            return this;
        }

        public boolean getMultline() {
            return multline;
        }

        public Setting setMultline(boolean multline) {
            this.multline = multline;
            return this;
        }

        public boolean getIs_number() {
            return is_number;
        }

        public Setting setIs_number(boolean is_number) {
            this.is_number = is_number;
            return this;
        }

        public boolean getPassword() {
            return password;
        }

        public Setting setPassword(boolean password) {
            this.password = password;
            return this;
        }
    }

    public EditDialog(Context context, Handler cb ,int theme) {
        super(context,theme);
        this.context =context;
        this.handler = cb;
    }

    int getId(String n,String ty)
    {
        return context.getResources().getIdentifier(n,ty,context.getPackageName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(getId("edit","layout"));
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        //一定要在setContentView之后调用，否则无效
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int i) {
                setHideVirtualKey(getWindow());
            }
        });

        touch = findViewById(getId("touch_v","id"));
        touch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                send_and_dismiss();
            }
        });

        ed = findViewById(getId("et", "id"));
        btn = findViewById(getId("et_btn","id"));

        ed.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if(i == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_UP)
                {
                    send_and_dismiss();
                    return true;
                }
                return false;
            }
        });
        ed.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if(i == EditorInfo.IME_ACTION_DONE)
                {
                    send_and_dismiss();
                    return true;
                }
                return false;
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                send_and_dismiss();
            }
        });



    }

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
    }

    private void send_and_dismiss() {
        String res = ed.getText().toString();
        Log.e(TAG,"send " + res);

        JSONObject data = new JSONObject();
        try {
            data.put("res",res);
        } catch (JSONException e) {
            Log.e(TAG,"send err" + e.getMessage());
        }

        Message m = new Message();
        m.arg1 = cmdid;
        m.what = ExternCall.WSendMessageToGame;
        m.obj = data.toString();
        handler.sendMessage(m);

        dismiss();
    }

    public void go(int cmdid,String res,JSONObject oth)
    {
        this.cmdid = cmdid;
        if(this.cmdid > 0)
        {
            Setting setting = new Setting();
            if(oth != null) {
                try {
                    setting = QuickSdk.formJson(Setting.class,oth);

                } catch (Exception e) {
                    Log.e(TAG,"go err " + e.getMessage());
                }
            }
            show();
            apply_setting(setting);
            set_res(res);
            ed.requestFocus();
            ed.setImeOptions(EditorInfo.IME_ACTION_DONE );

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Log.e(TAG,"InputMethodUtils.isShowing(context,ed) = " + InputMethodUtils.isShowing(context,ed) );
                    //Log.e(TAG,"InputMethodUtils.isFullscreenMode(context) = " + InputMethodUtils.isFullscreenMode(context));
                    //if( !InputMethodUtils.isShowing(context,ed) || InputMethodUtils.isFullscreenMode(context))
                    {
                        InputMethodUtils.show(context, ed);
                    }
                }
            },310);
        }
    }

    private void apply_setting(Setting setting) {


        ed.setSingleLine(!setting.multline);
        int ty = InputType.TYPE_CLASS_TEXT;
        if(setting.is_number)
            ty |= InputType.TYPE_CLASS_NUMBER;
        if(setting.password)
            ty |= InputType.TYPE_TEXT_VARIATION_PASSWORD;
        if(setting.is_phone)
            ty |= InputType.TYPE_CLASS_PHONE;

        ed.setInputType(ty);
    }

    private void set_res(String res) {
        if(res == null) return;
        if(ed != null)
        {
            ed.setText(res);
            ed.setSelection(res.length());
        }else{
            this.res = res;
        }
    }

    @Override
    public void dismiss() {
        InputMethodUtils.showOrHide(context,ed);
        super.dismiss();
        cmdid = -1;
    }
}
