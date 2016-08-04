package com.watch.guoneng.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.watch.guoneng.R;
import com.watch.guoneng.util.HttpUtil;
import com.watch.guoneng.util.JsonUtil;
import com.watch.guoneng.util.ThreadPoolManager;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by Administrator on 16-4-15.
 */
public class LinkWifiDeviceActivity extends BaseActivity {
    private static final String TAG="LinkWifiDeviceActivity";
    private EditText imeiEdit;
    private EditText idEdit;
    private Button linkBtn;
    private String imei;
    private String id;
    private ImageView iv_back;

    int ret = 0;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            String result = msg.obj.toString();
            closeLoadingDialog();
            Log.e("hjq", result);

            if (result.matches("Connection to .* refused")) {
                showLongToast("network error");
                return;
            }

            switch (msg.what) {
                case 0:
                    try {
                        JSONObject json = new JSONObject(result);
                        if (JsonUtil.getInt(json, JsonUtil.CODE) != 1) {
                            showLongToast(JsonUtil.getStr(json, JsonUtil.ERRORCN));
                        } else {
                            showLongToast(JsonUtil.getStr(json, JsonUtil.MSGCN));
                            ret = 1;
                            goBack();
                        }
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
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
        setContentView(R.layout.activity_link_device);
        linkBtn = (Button) findViewById(R.id.link);
        imeiEdit = (EditText) findViewById(R.id.wifi_imei);
        idEdit = (EditText) findViewById(R.id.wifi_id);
        iv_back= (ImageView) findViewById(R.id.iv_back);
        iv_back.setOnClickListener(this);
        linkBtn.setOnClickListener(this);
    }

    void goBack() {
        Intent intent = new Intent();
        Bundle b = new Bundle();
        b.putInt("ret", ret);
        intent.putExtras(b);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0) {
            // 具体的操作代码
            Log.e("hjq", "onBackPressed");
            goBack();
        }

        return super.dispatchKeyEvent(event);
    }


    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.iv_back:
                Log.i(TAG,"back");
                goBack();
                break;
            case R.id.link:
                if (checkdata()) {
                    ThreadPoolManager.getInstance().addTask(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            String result = HttpUtil.post(HttpUtil.URL_LINKWIFIDEVICE,
                                    new BasicNameValuePair(JsonUtil.IMEI, imei));
                            Log.e("hjq", result);

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
        imei = imeiEdit.getText().toString().trim();
        // +86
        if (imei.length() != 15) {
            showLongToast("WIFI ID 为15位字符");
        } else {
            isright = true;
        }
        Log.i("hjq", isright + "");
        return isright;
    }
}
