package com.watch.guoneng.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

//import cn.jpush.android.api.JPushInterface;

public class AuthLoginActivity extends BaseActivity implements OnClickListener {
    private final String TAG = "AuthLoginActivity";
    private TextView upatepassbtn, registbtn;
    private EditText phoneedit;
    private EditText passwordedit;
    private Button loginbtn;
    private String phone;
    private String password;
    private final int getmycoin_what = 1;
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            closeLoadingDialog();
            String result = msg.obj.toString();
            if (result.matches("Connection to .* refused") || result.matches("Connect to.*timed out")) {
                showComReminderDialog();
                return;
            }
            switch (msg.what) {
                case 0:
                    try {
                        JSONObject json = new JSONObject(result);
                        if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                            BaseTools.showToastByLanguage(AuthLoginActivity.this, json);
                        } else {
                            JSONObject msgobj = json.getJSONObject("msg");
                            String token = msgobj.getString("token");
                            JSONObject userobj = json.getJSONObject("user");
                            String id = userobj.getString(JsonUtil.ID);
                            String name = userobj.getString(JsonUtil.NAME);
                            String phone = userobj.getString(JsonUtil.PHONE);
                            String sex = userobj.getString(JsonUtil.SEX);
                            //String password = userobj.getString(JsonUtil.PASSWORD);
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
                            new UserDao(AuthLoginActivity.this).insert(user);
                            showLongToast(getString(R.string.login_success));
                            PreferenceUtil.getInstance(AuthLoginActivity.this).setUid(user.getId());
                            PreferenceUtil.getInstance(AuthLoginActivity.this).getString(PreferenceUtil.PHONE, user.getPhone());
                            PreferenceUtil.getInstance(AuthLoginActivity.this).setToken(user.getToken());
                            MyApplication.getInstance().mToken = user.getToken();    // update the token info.
                            startActivity(new Intent(AuthLoginActivity.this, DeviceListActivity.class));
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case getmycoin_what:
                    if (result.trim().equals("null")) {
                        PreferenceUtil.getInstance(AuthLoginActivity.this).setString(PreferenceUtil.SHIBI, "0");
                        finish();
                        return;
                    }
                    try {
                        JSONObject mycoinobj = new JSONObject(result);
                        String shibi = mycoinobj.getString(JsonUtil.SHIBI);
                        PreferenceUtil.getInstance(AuthLoginActivity.this).setString(PreferenceUtil.SHIBI, shibi);
                        finish();
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_login);

        upatepassbtn = (TextView) findViewById(R.id.updatepassbtn);
        registbtn = (TextView) findViewById(R.id.registbtn);
        phoneedit = (EditText) findViewById(R.id.login_phone);
        passwordedit = (EditText) findViewById(R.id.login_password);
        loginbtn = (Button) findViewById(R.id.login_btn);
        findViewById(R.id.back).setOnClickListener(this);
        upatepassbtn.setOnClickListener(this);
        registbtn.setOnClickListener(this);
        loginbtn.setOnClickListener(this);
        if (new UserDao(this).queryAll().size() > 0) {
            User mUser = new UserDao(this).queryAll().get(0);
            phoneedit.setText(mUser.getPhone());
        } else {
            phoneedit.setText(PreferenceUtil.getInstance(this).getString(PreferenceUtil.PHONE, ""));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                MyApplication.getInstance().exit();
                break;

            case R.id.updatepassbtn:
                startActivity(new Intent(this, FindPasswordActivity.class));
                break;

            case R.id.registbtn:
                startActivity(new Intent(this, AuthRegisterActivity.class));
                break;

            case R.id.login_btn:
                if (checkdata()) {
                    ThreadPoolManager.getInstance().addTask(new Runnable() {
                        @Override
                        public void run() {
                            String result = HttpUtil.post(HttpUtil.URL_LOGIN,
                                    new BasicNameValuePair(JsonUtil.PHONE, phone),
                                    new BasicNameValuePair(JsonUtil.PASSWORD,
                                            password));
                            Log.e(TAG, result);

                            Message msg = new Message();
                            msg.obj = result;
                            msg.what = 0;
                            mHandler.sendMessage(msg);
                        }
                    });
                    showLoadingDialog();
                }
                break;

            default:
                break;
        }
    }

    private boolean checkdata() {
        boolean isright = false;
        phone = phoneedit.getText().toString().trim();
        // +86
        if (phone.length() == 14) {
            phone = phone.substring(3);
        }

        password = passwordedit.getText().toString().trim();

        if (phone == null || phone.equals("")) {
            showLongToast(getString(R.string.phone_not_empty));
        } else if (phone.length() != 11) {
            showLongToast(getString(R.string.phone_format_not_match));
        } else if (password == null || password.equals("")) {
            showLongToast(getString(R.string.pwd_not_empty));
        } else {
            isright = true;
        }
        Lg.i(TAG, isright + "");
        return isright;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        MyApplication.getInstance().exit();
    }
}
