package com.watch.guoneng.tool;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;

public class NetStatuCheck {
    public static ConnectivityManager manager;
    private static final String TAG = "NetStatuCheck";

    public static String checkGPRSState(Context context) {
        boolean flag = false;
        manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager.getActiveNetworkInfo() != null) {
            flag = manager.getActiveNetworkInfo().isAvailable();
        }
        if (flag) {
            State wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                    .getState();

            if (wifi == State.CONNECTED || wifi == State.CONNECTING) {
                Lg.i(TAG, TAG + "    wifi");
                return "wifi";
            }
            if (manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) != null) {
                State gprs = manager
                        .getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
                if (gprs == State.CONNECTED || gprs == State.CONNECTING) {
                    Lg.i(TAG, TAG + "    GPRS");
                    return "GPRS";
                }
            }
        } else {
            Lg.i(TAG, TAG + "    unavailable");
            return "unavailable";
        }
        Lg.i(TAG, TAG + "    unavailable");
        return "unavailable";
    }

}
