package com.watch.guoneng.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.watch.guoneng.R;
import com.watch.guoneng.dao.UserDao;
import com.watch.guoneng.tool.BaseTools;
import com.watch.guoneng.tool.Lg;
import com.watch.guoneng.util.HttpUtil;
import com.watch.guoneng.util.JsonUtil;
import com.watch.guoneng.util.PreferenceUtil;
import com.watch.guoneng.util.ThreadPoolManager;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

public class PersonUpdatePasswordActivity extends BaseActivity {
    private static final String TAG = "PersonUpdatePasswordActivity";
    private EditText passwordEdit;
    private EditText repasswordEdit;
    private EditText old_passwordEdit;
    private Button savebtn;
    private String uid = "";
    private String oldpassword = "";
    private String passwordstr;
    private String repasswordstr;
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            String result = msg.obj.toString();
            if (result.matches("Connection to .* refused") || result.matches("Connect to.*timed out")) {
                showComReminderDialog();
                return;
            }
            try {
                JSONObject json = new JSONObject(result);
                if ("ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                    showLongToast(getString(R.string.person_password_update_ok));
                    new UserDao(PersonUpdatePasswordActivity.this).updatePassWordById(passwordstr, uid);
                    finish();
                } else {
                    BaseTools.showToastByLanguage(PersonUpdatePasswordActivity.this, json);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        ;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_updatepassword);
        old_passwordEdit = (EditText) findViewById(R.id.old_password);
        passwordEdit = (EditText) findViewById(R.id.password);
        repasswordEdit = (EditText) findViewById(R.id.repassword);
        savebtn = (Button) findViewById(R.id.save);
        savebtn.setOnClickListener(this);
        findViewById(R.id.back).setOnClickListener(this);
        uid = PreferenceUtil.getInstance(this).getUid();
        oldpassword = new UserDao(this).queryById(uid).getPassword();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                onBackPressed();
                break;
            case R.id.save:
                passwordstr = passwordEdit.getText().toString().trim();
                repasswordstr = repasswordEdit.getText().toString().trim();
                oldpassword = old_passwordEdit.getText().toString().trim();
                if (oldpassword.length() < 6 || oldpassword.length() > 16) {
                    showLongToast(getString(R.string.pwd_length_not_right));
                    return;
                } else if (passwordstr.length() < 6 || passwordstr.length() > 16) {
                    showShortToast(getString(R.string.pwd_length_not_right));
                    return;
                }
                if (!passwordstr.equals(repasswordstr)) {
                    showLongToast(getString(R.string.pwd_ensurepwd_not_match));
                    return;
                }
                ThreadPoolManager.getInstance().addTask(new Runnable() {
                    @Override
                    public void run() {
                        String result = HttpUtil.post(HttpUtil.URL_RESETPASSWORD,
                                new BasicNameValuePair("oldpass", oldpassword),
                                new BasicNameValuePair("newpass", passwordstr));
                        Lg.i(TAG, "修改密码result:" + result);
                        Message msg = new Message();
                        msg.obj = result;
                        mHandler.sendMessage(msg);
                    }
                });
                break;

            default:
                break;
        }
    }
}
