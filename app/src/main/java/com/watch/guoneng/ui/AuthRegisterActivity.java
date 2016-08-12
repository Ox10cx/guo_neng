package com.watch.guoneng.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.watch.guoneng.R;
import com.watch.guoneng.app.MyApplication;
import com.watch.guoneng.dao.UserDao;
import com.watch.guoneng.model.User;
import com.watch.guoneng.tool.BaseTools;
import com.watch.guoneng.tool.GetCodeCountTimer;
import com.watch.guoneng.tool.Lg;
import com.watch.guoneng.util.HttpUtil;
import com.watch.guoneng.util.JsonUtil;
import com.watch.guoneng.util.PreferenceUtil;
import com.watch.guoneng.util.ThreadPoolManager;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

public class AuthRegisterActivity extends BaseActivity {
    private final static String TAG = "AuthRegisterActivity";


    private Button registerbtn;
    private EditText phoneedit;
    private EditText codeedit;
    private EditText nameedit;
    private EditText passwordedit;
    private EditText passwordagainedit;
    private RadioGroup sexgroup;
    private Button getcodebtn;
    //    private TextView protocoltv;
    private String phone;
    private String code = "";
    private String name;
    private String password;
    private String password1;
    private String sex;
    private final int regist_what = 0;
    private final int getcode_what = 1;
    private final int login_what = 2;


    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            closeLoadingDialog();
            String result = msg.obj.toString();
            if ("".equals(result.trim()) || result.matches("Connection to .* refused") || result.matches("Connect to.*timed out")) {
                showComReminderDialog();
                return;
            }
            switch (msg.what) {
                case regist_what:
                    try {
                        JSONObject json = new JSONObject(result);
                        if (JsonUtil.getStr(json, JsonUtil.STATUS) != null && !"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                            BaseTools.showToastByLanguage(AuthRegisterActivity.this, json);
                            getcodebtn.setEnabled(true);
                            getcodebtn.setText(R.string.register_button_code);
                        } else {
                            showShortToast(getString(R.string.resgister_ok));
                            //去登录
                            toLogin();
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case getcode_what:
                    try {
                        JSONObject json = new JSONObject(result);
                        if (!JsonUtil.getStr(json, JsonUtil.STATUS).equals("ok")) {
                            registerbtn.setEnabled(false);
                            BaseTools.showToastByLanguage(AuthRegisterActivity.this, json);
                            getcodebtn.setEnabled(true);
                            getcodebtn.setText(R.string.register_button_code);
                        } else {
                            code = String.valueOf(JsonUtil.getInt(json, JsonUtil.CODE));
                            registerbtn.setEnabled(true);
                            showShortToast(getString(R.string.code_has_send));
                            GetCodeCountTimer getCodeCountTimer = new GetCodeCountTimer(90 * 1000,
                                    1000, getcodebtn, AuthRegisterActivity.this);
                            getCodeCountTimer.start();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case login_what:
                    try {
                        JSONObject json = new JSONObject(result);
                        closeLoadingDialog();
                        if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                            BaseTools.showToastByLanguage(AuthRegisterActivity.this, json);
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
                            new UserDao(AuthRegisterActivity.this).insert(user);
//                            showLongToast(getString(R.string.login_success));
                            PreferenceUtil.getInstance(AuthRegisterActivity.this).setUid(user.getId());
                            PreferenceUtil.getInstance(AuthRegisterActivity.this).getString(PreferenceUtil.PHONE, user.getPhone());
                            PreferenceUtil.getInstance(AuthRegisterActivity.this).setToken(user.getToken());
                            MyApplication.getInstance().mToken = user.getToken();    // update the token info.
                            startActivity(new Intent(AuthRegisterActivity.this, MainActivity.class));
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                default:
                    break;
            }
        }
    };

    /**
     * 登录
     */
    private void toLogin() {
        ThreadPoolManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                String result = HttpUtil.post(HttpUtil.URL_LOGIN,
                        new BasicNameValuePair(JsonUtil.PHONE, phone),
                        new BasicNameValuePair(JsonUtil.PASSWORD,
                                password));
                Lg.i(TAG, "url_login" + result);
                Message msg = new Message();
                msg.obj = result;
                msg.what = login_what;
                mHandler.sendMessage(msg);
            }
        });
        showLoadingDialog();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Lg.i(TAG, "onCreate");
        setContentView(R.layout.activity_auth_register);
        initView();
    }

    private void initView() {
        phoneedit = (EditText) findViewById(R.id.register_phone);
        codeedit = (EditText) findViewById(R.id.register_code);
        nameedit = (EditText) findViewById(R.id.register_name);
        passwordedit = (EditText) findViewById(R.id.register_password);
        passwordagainedit = (EditText) findViewById(R.id.register_password_again);
        sexgroup = (RadioGroup) findViewById(R.id.register_sex);
        registerbtn = (Button) findViewById(R.id.registerbtn);
        getcodebtn = (Button) findViewById(R.id.register_getcode);
//        protocoltv = (TextView) findViewById(R.id.register_textview_protocol);
        findViewById(R.id.back).setOnClickListener(this);
        registerbtn.setOnClickListener(this);
        getcodebtn.setOnClickListener(this);
//        protocoltv.setOnClickListener(this);
        phoneedit.setText(getLocalPhoneNumber());
        registerbtn.setEnabled(false);
    }

    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back: {
                onBackPressed();
                break;
            }

            case R.id.registerbtn: {
                if (checkdata()) {
                    showLoadingDialog();
                    ThreadPoolManager.getInstance().addTask(new Runnable() {
                        @Override
                        public void run() {
                            String result = HttpUtil.post(HttpUtil.URL_REGISTER,
                                    new BasicNameValuePair(JsonUtil.NAME, name),
                                    new BasicNameValuePair(JsonUtil.PHONE, phone),
                                    new BasicNameValuePair(JsonUtil.SEX, sex),
                                    new BasicNameValuePair(JsonUtil.PASSWORD, password),
                                    new BasicNameValuePair(JsonUtil.REPASSWORD, password1),
                                    new BasicNameValuePair("checknum", code));
                            Message msg = new Message();
                            msg.what = regist_what;
                            msg.obj = result;
                            mHandler.sendMessage(msg);
                        }
                    });
                }
                break;
            }

            case R.id.register_getcode: {
                phone = phoneedit.getText().toString().trim();
                if (!BaseTools.isPhoneNumber(phone)) {
                    showShortToast(getString(R.string.phone_format_not_match));
                    return;
                }
                getcodebtn.setText(getString(R.string.is_sending));
//                getcodebtn.setEnabled(false);
                ThreadPoolManager.getInstance().addTask(new Runnable() {
                    @Override
                    public void run() {
                        String result = HttpUtil.get(HttpUtil.URL_CHECKMOBILE
                                + "?mobile=" + phone + "&type=1");
                        Lg.i(TAG, result);
                        Message msg = new Message();
                        msg.what = getcode_what;
                        msg.obj = result;
                        mHandler.sendMessage(msg);
                    }
                });
                break;
            }

//            case R.id.register_textview_protocol: {
//                Intent mIntent2 = new Intent(this, StaticPageActivity.class);
//                mIntent2.putExtra(JsonUtil.TITLE, getString(R.string.register_textview_protocol));
//                startActivity(mIntent2);
//                break;
//            }

            default:
                break;
        }
    }

    private String getLocalPhoneNumber() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String phoneId = tm.getLine1Number();
        return phoneId;
    }

    private boolean checkdata() {
        phone = phoneedit.getText().toString().trim();
        // +86
        if (phone.length() == 14) {
            phone = phone.substring(3);
        }

        String inputcode = codeedit.getText().toString().trim();
        name = nameedit.getText().toString().trim();
        password = passwordedit.getText().toString().trim();
        Lg.i(TAG, "password:" + password);
        password1 = passwordagainedit.getText().toString().trim();
        int checkid = sexgroup.getCheckedRadioButtonId();
        sex = sexgroup.getCheckedRadioButtonId() == R.id.radioMale ? "1" : "0";

        if (phone.equals("")) {
            showShortToast(getString(R.string.phone_not_empty));
            return false;
        } else if (inputcode.equals("")) {
            showShortToast(getString(R.string.code_not_empty));
            return false;
        } else if (name.equals("")) {
            showShortToast(getString(R.string.name_not_empty));
            return false;
        } else if (password.equals("")) {
            showShortToast(getString(R.string.pwd_not_empty));
            return false;
        } else if (password1.equals("")) {
            showShortToast(getString(R.string.ensure_pwd_not_empty));
            return false;
        } else if (phone.length() != 11) {
            showShortToast(getString(R.string.phone_format_not_match));
            return false;
        } else if (!password.equals(password1)) {
            showShortToast(getString(R.string.pwd_ensurepwd_not_match));
            return false;
        } else if (password.length() < 6 || password.length() > 16) {
            showShortToast(getString(R.string.pwd_length_not_right));
            return false;
        } else if (!code.equals(inputcode)) {
            showShortToast(getString(R.string.code_not_right));
            return false;
        }
        return true;
    }

}
