package com.watch.guoneng.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.watch.guoneng.R;
import com.watch.guoneng.tool.BaseTools;
import com.watch.guoneng.tool.Lg;
import com.watch.guoneng.util.HttpUtil;
import com.watch.guoneng.util.JsonUtil;
import com.watch.guoneng.util.ThreadPoolManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class FindPasswordActivity extends BaseActivity {
    private EditText phone;

    private EditText password;
    private EditText repassword;
    private EditText codeedit;
    private Button getcode;
    private Button save;

    private Timer mTimer;
    private String phonestr = "";
    private String codestr = "";
    private String inputcode = "";
    private String passwordstr = "";
    private String repasswordstr = "";
    private final int save_what = 0;
    private final int getcode_what = 1;
    private final String TAG = FindPasswordActivity.class.getName();

    private Handler timerHandler = new Handler() {
        public void handleMessage(Message msg) {
            int num = msg.what;
            getcode.setText(num + getString(R.string.second));
            if (num == -1) {
                mTimer.cancel();
                getcode.setEnabled(true);
                getcode.setText(R.string.str_get_pwd);
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
                case save_what: {
                    try {
                        JSONObject json = new JSONObject(result);
                        closeLoadingDialog();
                        if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                            BaseTools.showToastByLanguage(FindPasswordActivity.this, json);
                        } else {
                            BaseTools.showToastByLanguage(FindPasswordActivity.this, json);
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
                            BaseTools.showToastByLanguage(FindPasswordActivity.this, json);
                            save.setEnabled(false);
                            getcode.setEnabled(true);
                            getcode.setText(R.string.register_button_code);
                        } else {
                            codestr = String.valueOf(JsonUtil.getInt(json, JsonUtil.CODE));
                            save.setEnabled(true);
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
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_findpassword);
        initView();
    }

    private void initView() {
        phone = (EditText) findViewById(R.id.phone);
        password = (EditText) findViewById(R.id.password);
        repassword = (EditText) findViewById(R.id.repassword);
        codeedit = (EditText) findViewById(R.id.register_code);
        getcode = (Button) findViewById(R.id.getcode);
        save = (Button) findViewById(R.id.save);
        save.setOnClickListener(this);
        save.setEnabled(false);
        findViewById(R.id.back).setOnClickListener(this);
        getcode.setOnClickListener(this);
        phone.setText(getLocalPhoneNumber());
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.back: {
                onBackPressed();
                break;
            }
            case R.id.getcode: {
                phonestr = phone.getText().toString().trim();
                if (BaseTools.isPhoneNumber(phonestr)) {
                    getcode.setText(getString(R.string.is_sending));
                    getcode.setEnabled(false);
                    ThreadPoolManager.getInstance().addTask(new Runnable() {
                        @Override
                        public void run() {
                            String result = HttpUtil.get(HttpUtil.URL_CHECKMOBILE
                                    + "?mobile=" + phonestr + "&type=2");
                            Lg.i(TAG, "result：" + result);
                            Message msg = new Message();
                            msg.what = getcode_what;
                            msg.obj = result;
                            mHandler.sendMessage(msg);
                        }
                    });
                } else {
                    showShortToast(getString(R.string.phone_format_not_match));
                }
                break;
            }

            case R.id.save: {
                if (checkdata()) {
                    ThreadPoolManager.getInstance().addTask(new Runnable() {
                        @Override
                        public void run() {
                            String result = HttpUtil.get(HttpUtil.URL_CHANGPASSWORD
                                    + "?mobile=" + phonestr + "&checknum=" + inputcode + "&password=" + passwordstr);
                            Lg.i(TAG, result);
                            Message msg = new Message();
                            msg.what = save_what;
                            msg.obj = result;
                            mHandler.sendMessage(msg);
                        }
                    });
                    showLoadingDialog();
                }
                break;
            }
            default:
                break;
        }
    }

    private boolean checkdata() {
        phonestr = phone.getText().toString().trim();
        passwordstr = password.getText().toString().trim();
        repasswordstr = repassword.getText().toString().trim();
        inputcode = codeedit.getText().toString().trim();
        if (phonestr.length() == 14) {
            phonestr = phonestr.substring(3);
        } else if (phonestr.length() != 11) {
            showShortToast(getString(R.string.phone_format_not_match));
            return false;
        }
        if (passwordstr.equals("")) {
            showShortToast(getString(R.string.pwd_not_empty));
            return false;
        } else if (repasswordstr.equals("")) {
            showShortToast(getString(R.string.ensure_pwd_not_empty));
            return false;
        } else if (!passwordstr.equals(repasswordstr)) {
            showShortToast(getString(R.string.pwd_ensurepwd_not_match));
            return false;
        } else if (passwordstr.length() < 6 || passwordstr.length() > 16) {
            showShortToast(getString(R.string.pwd_length_not_right));
            return false;
        } else if (!codestr.equals(inputcode)) {
            showShortToast(getString(R.string.code_not_right));
            return false;
        }
        return true;
    }

    private String getLocalPhoneNumber() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String phoneId = tm.getLine1Number();
        return phoneId;
    }
}
