package com.watch.guoneng.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.watch.guoneng.R;
import com.watch.guoneng.app.MyApplication;
import com.watch.guoneng.dao.UserDao;
import com.watch.guoneng.model.User;
import com.watch.guoneng.tool.BaseTools;
import com.watch.guoneng.tool.Lg;
import com.watch.guoneng.util.HttpUtil;
import com.watch.guoneng.util.JsonUtil;
import com.watch.guoneng.util.PreferenceUtil;
import com.watch.guoneng.util.ThreadPoolManager;
import com.watch.guoneng.xlistview.ComReminderDialog;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class FirstActivity extends BaseActivity {
    private String phone;
    private String password;
    private final String TAG = "FirstActivity";
    private final int MSG_LOGIN = 0;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            final String result = msg.obj.toString();
            closeLoadingDialog();
            if (result.matches("Connection to .* refused") || result.matches("Connect to.*timed out")) {
                closeComReminderDialog();
                final ComReminderDialog comReminderDialog = new ComReminderDialog(FirstActivity.this,
                        getResources().getString(R.string.net_has_breaked)
                        , getResources().getString(R.string.cancel), getResources().getString(R.string.ensure));
                comReminderDialog.setCanceledOnTouchOutside(false);
                comReminderDialog.show();
                comReminderDialog.dialog_cancel.setOnClickListener(new View.OnClickListener() {
                                                                       @Override
                                                                       public void onClick(View v) {
                                                                           comReminderDialog.cancel();
                                                                           finish();
                                                                       }
                                                                   }
                );
                comReminderDialog.dialog_submit.setOnClickListener(new View.OnClickListener() {
                                                                       @Override
                                                                       public void onClick(View v) {
                                                                           comReminderDialog.cancel();
                                                                           if (android.os.Build.VERSION.SDK_INT > 13) {
                                                                               startActivity(new Intent(
                                                                                       android.provider.Settings.ACTION_SETTINGS));
                                                                           } else {
                                                                               startActivity(new Intent(
                                                                                       android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                                                                           }
                                                                           finish();
                                                                       }
                                                                   }
                );
                return;
            }

            switch (msg.what) {
                case MSG_LOGIN: {
                    try {
                        JSONObject json = new JSONObject(result);
                        if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                            BaseTools.showToastByLanguage(FirstActivity.this, json);
                            startActivity(new Intent(FirstActivity.this, AuthLoginActivity.class));
                        } else {
                            JSONObject msgobj = json.getJSONObject("msg");
                            String token = msgobj.getString("token");

                            JSONObject userobj = json.getJSONObject("user");
                            String id = userobj.getString(JsonUtil.ID);
                            String name = userobj.getString(JsonUtil.NAME);
                            String phone = userobj.getString(JsonUtil.PHONE);
                            String sex = userobj.getString(JsonUtil.SEX);
                            //   String password = userobj.getString(JsonUtil.PASSWORD);
                            String create_time = userobj.getString(JsonUtil.CREATE_TIME);

                            String image_thumb = null;
                            String image = null;
                            try {
                                image_thumb = userobj.getString(JsonUtil.IMAGE_THUMB);
                                image = userobj.getString(JsonUtil.IMAGE);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            User user = new User(id, name, phone, sex, password, create_time, image_thumb, image, token);
                            new UserDao(FirstActivity.this).insert(user);
//                            showLongToast(getString(R.string.login_success));
                            PreferenceUtil.getInstance(FirstActivity.this).setUid(user.getId());
                            PreferenceUtil.getInstance(FirstActivity.this).getString(PreferenceUtil.PHONE, user.getPhone());
                            PreferenceUtil.getInstance(FirstActivity.this).setToken(user.getToken());
                            MyApplication.getInstance().mToken = user.getToken();
                            startActivity(new Intent(FirstActivity.this, DeviceListActivity.class));
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                }

                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_frist);

        ArrayList<User> list = new UserDao(this).queryAll();
        //test
        for (User ele : list) {
            Lg.i(TAG, ele.getPhone());
        }
        User user = null;

        if (list != null && !list.isEmpty()) {
            user = list.get(0);
        }
        if (user != null) {
            phone = user.getPhone();
            password = user.getPassword();
            Lg.i(TAG, "phone->>" + phone);
            //延时2s后，跳转
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ThreadPoolManager.getInstance().addTask(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "begin post");
                            String result = HttpUtil.post(HttpUtil.URL_LOGIN,
                                    new BasicNameValuePair(JsonUtil.PHONE, phone),
                                    new BasicNameValuePair(JsonUtil.PASSWORD,
                                            password));
                            Lg.i(TAG, "URL_LOGIN--->result:" + result);
                            Message msg = new Message();
                            msg.obj = result;
                            msg.what = MSG_LOGIN;
                            mHandler.sendMessage(msg);
                        }
                    });
                }
            }, 2000);
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "user is null");
                    finish();
                    startActivity(new Intent(FirstActivity.this, AuthLoginActivity.class));
                }
            }, 2000);
        }
    }


}
