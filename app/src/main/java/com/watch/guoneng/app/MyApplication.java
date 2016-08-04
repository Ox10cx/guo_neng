package com.watch.guoneng.app;

import android.app.Activity;
import android.app.Application;

import com.watch.guoneng.BuildConfig;
import com.watch.guoneng.tool.AppCrashHandler;
import com.watch.guoneng.ui.IService;
import com.watch.guoneng.util.ImageLoaderUtil;
import com.watch.guoneng.util.PreferenceUtil;

import java.util.LinkedList;

public class MyApplication extends Application {

    public int time = 0;
    public int type = 1;
    public int islocation = 0;
    public LinkedList<Activity> activityList = new LinkedList<Activity>();
    private static MyApplication instance;
    public static String mToken = "";
    public double latitude;
    public double longitude;
    public boolean isSocketConnectBreak = true;

    public boolean isFirstLongCon = false;
    public IService mService;
    public boolean longConnected;

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        //异常重启
        if (!BuildConfig.debug) {
            AppCrashHandler ch = AppCrashHandler.getInstance();
            ch.init(getApplicationContext());
        }
    }

    public static MyApplication getInstance() {
        if (null == instance) {
            instance = new MyApplication();
        }
        return instance;
    }

    public void addActivity(Activity activity) {
        activityList.add(activity);
    }

    public void removeActivity(Activity activity) {
        activityList.remove(activity);
    }

    public void exit() {
        for (Activity activity : activityList) {
            activity.finish();
        }
        PreferenceUtil.getInstance(this).setString(PreferenceUtil.CITYID, "0");
        System.exit(0);
    }

    @Override
    public void onTerminate() {
        // TODO Auto-generated method stub
        ImageLoaderUtil.stopload(instance);
        super.onTerminate();
    }

}
