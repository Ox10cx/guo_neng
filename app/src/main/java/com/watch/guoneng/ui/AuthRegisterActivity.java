package com.watch.guoneng.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.watch.guoneng.R;
import com.watch.guoneng.tool.BaseTools;
import com.watch.guoneng.tool.Lg;
import com.watch.guoneng.util.HttpUtil;
import com.watch.guoneng.util.JsonUtil;
import com.watch.guoneng.util.ThreadPoolManager;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class AuthRegisterActivity extends BaseActivity {
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
    private Timer mTimer;
    private final static String TAG = "AuthRegisterActivity123";

    private Handler timerHandler = new Handler() {
        public void handleMessage(Message msg) {
            int num = msg.what;
            getcodebtn.setText(num
                    + getString(R.string.second));
            if (num == -1) {
                mTimer.cancel();
                getcodebtn.setEnabled(true);
                getcodebtn.setText(R.string.register_button_code);
            }
        }

        ;
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            closeLoadingDialog();
            String result = msg.obj.toString();
            if ("".equals(result.trim())||result.matches("Connection to .* refused") || result.matches("Connect to.*timed out")) {
                showComReminderDialog();
                return;
            }
            switch (msg.what) {
                case regist_what: {
                    try {
                        JSONObject json = new JSONObject(result);
                        if (JsonUtil.getStr(json, JsonUtil.STATUS) != null && !"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                            BaseTools.showToastByLanguage(AuthRegisterActivity.this, json);
                            getcodebtn.setEnabled(true);
                            getcodebtn.setText(R.string.register_button_code);
                        } else {
                            showShortToast(getString(R.string.resgister_ok));
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                }

                case getcode_what: {
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
                            mTimer = new Timer();
                            mTimer.schedule(new TimerTask() {
                                int num = 90;

                                @Override
                                public void run() {
                                    Message msg = new Message();
                                    msg.what = num;
                                    timerHandler.sendMessage(msg);
                                    num--;
                                }
                            }, 0, 1000);
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

        ;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_register);

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
                getcodebtn.setEnabled(false);
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
