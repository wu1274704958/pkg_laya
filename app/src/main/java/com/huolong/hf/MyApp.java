package com.huolong.hf;

import android.app.Application;
import android.util.Log;

import com.tencent.smtt.export.external.TbsCoreSettings;
import com.tencent.smtt.sdk.QbSdk;

import java.util.HashMap;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //HashMap map = new HashMap();
        //map.put(TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER, true);
        //map.put(TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE, true);
        //QbSdk.initTbsSettings(map);

        QbSdk.initX5Environment(getApplicationContext(), new QbSdk.PreInitCallback() {
            @Override
            public void onCoreInitFinished() {
                Log.e("X5Init","onCoreInitFinished");
            }

            @Override
            public void onViewInitFinished(boolean b) {
                Log.e("X5Init","onViewInitFinished " + b);
            }
        });

    }
}
