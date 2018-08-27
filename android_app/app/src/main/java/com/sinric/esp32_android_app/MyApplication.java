package com.sinric.esp32_android_app;

import android.app.Application;
import android.content.Context;

/**
 * Created by WGH on 2017/4/10.
 */

public class MyApplication extends Application{

    private static Context context;
    private static int mPId;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        mPId = android.os.Process.myPid();
    }

    public static Context context() {
        return context;
    }

    public static int getmPId() {
        return mPId;
    }
}
