/*****************************************************************************
 * VlcCrashHandler.java
 * ****************************************************************************
 * Copyright © 2012 VLC authors and VideoLAN
 * <p/>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package com.watch.wifidemo.tool;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.widget.Toast;

import com.watch.wifidemo.R;
import com.watch.wifidemo.app.MyApplication;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;

public class AppCrashHandler implements UncaughtExceptionHandler {

    private static final String TAG = "AppCrashHandler";

    private static AppCrashHandler INSTANCE = new AppCrashHandler();
    private Context mContext;
    private Map<String, String> infos = new HashMap<String, String>();

    public AppCrashHandler() {
    }

    public static AppCrashHandler getInstance() {
        return INSTANCE;
    }

    public void init(Context context) {

        mContext = context;
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, mContext.getResources().getString(R.string.crash_reminder), Toast.LENGTH_LONG)
                        .show();
                Looper.loop();
            }
        }).start();

        try {
            Thread.currentThread();
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (MyApplication.getInstance().activityList != null
                && !MyApplication.getInstance().activityList.isEmpty()) {
            for (int i = 0; i < MyApplication.getInstance().activityList.size(); i++) {
                if (MyApplication.getInstance().activityList.get(i) != null) {
                    MyApplication.getInstance().activityList.get(i).finish();
                }
            }
            MyApplication.getInstance().activityList.isEmpty();
            MyApplication.getInstance().activityList = null;
        }
        restartApp();
    }

    private void restartApp() {
        Intent launchIntent = mContext.getPackageManager()
                .getLaunchIntentForPackage("com.watch.wifidemo");
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        mContext.startActivity(launchIntent);
        android.os.Process.killProcess(android.os.Process.myPid()); // 结束进程之前可以把你程序的注销或者退出代码放在这段代码之前
    }
}
