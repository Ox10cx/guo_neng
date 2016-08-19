package com.watch.guoneng.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.watch.guoneng.R;
import com.watch.guoneng.model.WifiDevice;
import com.watch.guoneng.service.WifiConnectService;

/**
 * Created by Administrator on 16-5-7.
 */
public class LightSettingActivity extends BaseActivity {
    WifiDevice mDevice;
    private IService mService;
    Handler mHandler = new Handler();
    SeekBar mBrightness;
    SeekBar mChrome;
    private String TAG = "hjq";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_light_setting);

        Intent i = getIntent();
        Bundle b = i.getExtras();

        mDevice = (WifiDevice) b.getSerializable("device");
        Log.d("hjq", "mDevice = " + mDevice);

        ImageView ivBack = (ImageView) findViewById(R.id.iv_back);
        ivBack.setOnClickListener(this);

        mBrightness = (SeekBar) findViewById(R.id.seekBarBrightness);
        mBrightness.setMax(255);
        mBrightness.setProgress(0);
        mBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Log.d("hjq", "value in seekbar = " + i);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int bright = mBrightness.getProgress();
                int chrome = mChrome.getProgress();

                try {
                    mService.setBrightChrome(mDevice.getAddress(), 0, bright, chrome);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        mChrome = (SeekBar) findViewById(R.id.seekBarChrome);
        mChrome.setMax(255);
        mChrome.setProgress(0);
        mChrome.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Log.d("hjq", "value in seekbar = " + i);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int bright = mBrightness.getProgress();
                int chrome = mChrome.getProgress();

                try {
                    mService.setBrightChrome(mDevice.getAddress(), 0, bright, chrome);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        Intent intent = new Intent(this, WifiConnectService.class);
        getApplicationContext().bindService(intent, mConnection, BIND_AUTO_CREATE);
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

        }

        @Override
        public void onGetStatusRsp(String imei, final int ret) throws RemoteException {

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
            int i;

            for (i = 0; i < list.length; i++) {
                Log.e("hjq", "list[" + i + "] = " + list[i]);
            }
        }

        @Override
        public void onSetBrightChromeRsp(String imei, final int ret) throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(LightSettingActivity.this, ret == 1 ? R.string.str_success : R.string.str_fail, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onGetBrightChromeRsp(String imei, int index, final int bright, final int chrome) throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBrightness.setProgress(bright);
                    mChrome.setProgress(chrome);
                }
            });
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
                mService.getBrightChrome(mDevice.getAddress(), 0);
                mService.getLightList(mDevice.getAddress());
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
    };

}
