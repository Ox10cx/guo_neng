package com.watch.wifidemo.tool;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.RemoteException;

import com.watch.wifidemo.app.MyApplication;

import java.util.List;


public class NetworkChangeReceiver extends BroadcastReceiver {
    private static String TAG = "NetworkChangeReceiver";

    public NetworkChangeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //应用位于后台，不显示   网络变化的监听只适用于该应用位于前台
        if (!isBackground(context)) {
            Lg.i(TAG, "NetworkChangeReceiver_onReceive");
            State wifiState = null;
            State mobileState = null;
            ConnectivityManager cm = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            wifiState = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
            mobileState = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();

            if (cm != null) {
                if (wifiState != null && mobileState != null
                        && State.CONNECTED != wifiState
                        && State.CONNECTED == mobileState) {
                    // 非WiFi连接成功
                    Lg.i(TAG, "移动网连接成功");
                    restartLongConnnect();
                } else if (wifiState != null && mobileState != null
                        && State.CONNECTED != wifiState
                        && State.CONNECTED != mobileState) {
                    // 手机没有任何的网络
                    Lg.i(TAG, "没有网络");
                } else if (wifiState != null && State.CONNECTED == wifiState) {
                    // 无线网络连接成功
                    if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()) {
                        Lg.i(TAG, "wifi连接成功");
                        restartLongConnnect();
                    }
                }
            }
        }

    }

    private void restartLongConnnect() {
        if (MyApplication.getInstance().isFirstLongCon && !MyApplication.getInstance().longConnected) {
            try {
                if (MyApplication.getInstance().mService != null) {
                    Lg.i(TAG, "重连长服务");
                    MyApplication.getInstance().mService.connect(null);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 判断应用是否属于后台程序
     *
     * @param context
     * @return
     */
    public boolean isBackground(Context context) {
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        for (RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                    // Log.i("后台", appProcess.processName);
                    return true;
                } else {
                    // Log.i("前台", appProcess.processName);
                    return false;
                }
            }
        }
        return false;
    }


}
