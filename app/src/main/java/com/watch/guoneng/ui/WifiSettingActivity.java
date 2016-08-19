package com.watch.guoneng.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.watch.guoneng.R;
import com.watch.guoneng.model.WifiDevice;
import com.watch.guoneng.service.WifiConnectService;
import com.watch.guoneng.tool.Lg;
import com.watch.guoneng.util.SwitchButton;

/**
 * Created by Administrator on 16-3-11.
 */
public class WifiSettingActivity extends BaseActivity {
    private static final String TAG="WifiSettingActivity";
    WifiDevice mDevice;
    SwitchButton lightSwitch;
    private IService mService;
    Handler mHandler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_antilost_setting);

        Intent i = getIntent();
        Bundle b = i.getExtras();

        mDevice  = (WifiDevice) b.getSerializable("device");
        Log.d("hjq", "mDevice = " + mDevice);

        ImageView ivBack = (ImageView) findViewById(R.id.iv_back);
        ivBack.setOnClickListener(this);

        lightSwitch = (SwitchButton) findViewById(R.id.switchLight);
        lightSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d("hjq", "lightswitch is checked = " + isChecked);
                try {
                    mService.enableLight(mDevice.getAddress(), isChecked);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        Intent intent = new Intent(this, WifiConnectService.class);
        getApplicationContext().bindService(intent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0) {
            Log.e("hjq", "onBackPressed");

            goBack();
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    private void goBack() {
        Intent intent = new Intent();
        Bundle b = new Bundle();
        b.putSerializable("device", mDevice);
        intent.putExtras(b);

        //设置返回数据
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
      public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back: {
                goBack();
                break;
            }
        }

        super.onClick(v);
    }


    private ICallback.Stub mCallback = new ICallback.Stub() {

        @Override
        public void onConnect(String address) throws RemoteException {

        }

        @Override
        public void onDisconnect(String address) throws RemoteException {

        }

        @Override
        public boolean onRead(String address, byte[] val) throws RemoteException {
            return false;
        }

        @Override
        public boolean onWrite(String address, byte[] val) throws RemoteException {
            return false;
        }

        @Override
        public void onNotify(String imei, int type) throws RemoteException {

        }

        @Override
        public void onSwitchRsp(String imei, final boolean ret) throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(WifiSettingActivity.this, ret ? R.string.str_success : R.string.str_fail, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onGetStatusRsp(String imei, final int ret) throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    boolean on;
                    if (ret == 1) {
                        on = true;
                        lightSwitch.setChecked(on);
                    } else if (ret == 0) {
                        on = false;
                        lightSwitch.setChecked(on);
                    } else {
                        Toast.makeText(WifiSettingActivity.this,  R.string.str_fail, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        public void onCmdTimeout(String cmd, String imei) throws RemoteException {

        }

        @Override
        public void onHttpTimeout(String cmd, String imei) throws RemoteException {

        }

        @Override
        public void onPingRsp(String imei, int ret) throws RemoteException {

        }

        @Override
        public void onGetLightList(String imei, byte[] list) throws RemoteException {

        }

        @Override
        public void onSetBrightChromeRsp(String imei, int ret) throws RemoteException {
            Lg.i(TAG,"onSetBrightChromeRsp");
        }

        @Override
        public void onGetBrightChromeRsp(String imei, int index, int bright, int chrome) throws RemoteException {

        }

        @Override
        public void onPairLightRsp(String imei, int ret) throws RemoteException {

        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mService = IService.Stub.asInterface(service);
            try {
                mService.registerCallback(mCallback);
                mService.getLightStatus(mDevice.getAddress());
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
    };


}
