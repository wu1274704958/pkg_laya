package com.huolong.hf.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class InputMethodUtils {

    //如果输入法在窗口上已经显示，则隐藏，反之则显示
    public static void showOrHide(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    //view为接受软键盘输入的视图，SHOW_FORCED表示强制显示
    public static void showOrHide(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        //  imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);//SHOW_FORCED表示强制显示
        if(imm != null)
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0); //强制隐藏键盘
    }

    //调用隐藏系统默认的输入法
    public static void showOrHide(Context context, Activity activity) {
        ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static void show(Context context, View view) {
        InputMethodManager imm =  ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE));
        if(imm != null)
            imm.showSoftInput(view,InputMethodManager.SHOW_FORCED);
    }

    //获取输入法打开的状态
    public static boolean isShowing(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if(imm != null)
            return imm.isActive();//isOpen若返回true，则表示输入法打开
        else
            return  false;
    }

    public static boolean isShowing(Context context,View v) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if(imm != null)
            return imm.isActive(v);//isOpen若返回true，则表示输入法打开
        else
            return  false;
    }

    public static boolean isFullscreenMode(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if(imm != null)
            return imm.isFullscreenMode();//isOpen若返回true，则表示输入法打开
        else
            return  false;
    }
}
