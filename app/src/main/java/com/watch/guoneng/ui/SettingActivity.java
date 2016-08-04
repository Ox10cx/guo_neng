package com.watch.guoneng.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.watch.guoneng.R;
import com.watch.guoneng.util.CommonUtil;
import com.watch.guoneng.util.DialogUtil;
import com.watch.guoneng.util.HttpUtil;
import com.watch.guoneng.util.JsonUtil;
import com.watch.guoneng.util.ThreadPoolManager;
import com.watch.guoneng.util.UpdateManager;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Administrator on 16-3-7.
 */
public class SettingActivity  extends BaseActivity {
    SharedPreferences mSharedPreferences;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            closeLoadingDialog();
            String result = msg.obj.toString();
            try {
                final JSONObject json = new JSONObject(result);
                if (json.getInt(JsonUtil.CODE) == 1) {
                    Log.e("hjq", "msg is = " + json.getString(JsonUtil.MSG));
                    showLongToast(json.getString(JsonUtil.MSG));
                } else {
                    final String path = json.getString(JsonUtil.PATH);
                    final String updatemsg = json.getString(JsonUtil.MSG);

                    DialogUtil.showDialog(SettingActivity.this, "发现新版本！",
                            json.getString(JsonUtil.MSG) + "是否要更新？",
                            getString(R.string.system_sure),
                            getString(R.string.system_cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    // TODO Auto-generated method stub
                                    UpdateManager mUpdateManager = new UpdateManager(
                                            SettingActivity.this,
                                            updatemsg,
                                            HttpUtil.SERVER + path);
                                    mUpdateManager.showDownloadDialog();
                                }
                            }, null, true);
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.setting_activity);

        LinearLayout ll_userinfo = (LinearLayout)findViewById(R.id.ll_userinfo);
        ll_userinfo.setOnClickListener(this);

        Button button = (Button) findViewById(R.id.bt_checkupdate);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadingDialog("正在检查版本..");
                ThreadPoolManager.getInstance().addTask(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        Log.e("hjq", "version=" + CommonUtil.getVersionName(SettingActivity.this));
                        String result = HttpUtil.post(HttpUtil.URL_ANDROIDUPDATE,
                                new BasicNameValuePair(JsonUtil.VERSION, CommonUtil.getVersionName(SettingActivity.this)));
                        Message msg = new Message();
                        msg.obj = result;
                        mHandler.sendMessage(msg);
                    }
                });
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_userinfo: {
                Intent intent = new Intent(SettingActivity.this, PersonInfoActivity.class);
                startActivity(intent);
                break;
            }

            case R.id.iv_back: {
                finish();
                break;
            }

            default:
                break;
        }

        super.onClick(v);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
