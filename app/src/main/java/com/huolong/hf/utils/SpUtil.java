package com.huolong.hf.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;


/**
 * Created by yons on 17/3/8.
 */

public class SpUtil {
    //============== sp utils ===========================
    public static SharedPreferences sp;

    public static void init(Context context)
    {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void save(String key, int value) {
        sp.edit().putInt(key, value).apply();
    }

    public static void save(String key, String value) {
        sp.edit().putString(key, value).commit();
    }

    public static String get(String key) {
        return sp.getString(key, "");
    }

    public static int getInt(String key) {
        return sp.getInt(key, -1);
    }

    public static void save(String key, Set<String> values) {
        sp.edit().putStringSet(key, values).apply();
    }

    public static Set<String> getSet(String key) {
        return sp.getStringSet(key, new HashSet<String>());
    }

    public static void remove(String key) {
        sp.edit().remove(key).apply();
    }

}
