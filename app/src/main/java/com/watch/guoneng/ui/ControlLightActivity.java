package com.watch.guoneng.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.watch.guoneng.R;
import com.watch.guoneng.app.MyApplication;
import com.watch.guoneng.model.Light;
import com.watch.guoneng.model.WifiDevice;
import com.watch.guoneng.service.WifiConnectService;
import com.watch.guoneng.tool.Lg;
import com.watch.guoneng.tool.NetStatuCheck;

/**
 * Created by Administrator on 16-3-7.
 */
public class ControlLightActivity extends BaseActivity implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "ControlLightActivity";
    private Light light;
    private TextView headtitle;
    private TextView oktv;
    private TextView lightness_per;
    private TextView tem_per;
    private SeekBar lightness_sb;
    private SeekBar tem_sb;
    private int lighenessValue;
    private int temValue;
    private ImageView light_switch;
    private boolean is_light_on = false;
    WifiDevice mDevice;
    Handler mHandler = new Handler();
    private Intent temIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conlight);
        initViews();
        showLoadingDialog();
        Intent intent = new Intent(this, WifiConnectService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);
    }

    private void initViews() {
        temIntent = getIntent();
        light = (Light) temIntent.getSerializableExtra("light");
        mDevice = (WifiDevice) temIntent.getSerializableExtra("device");
        headtitle = (TextView) findViewById(R.id.head_title);
        oktv = (TextView) findViewById(R.id.ok_tv);
        lightness_per = (TextView) findViewById(R.id.lightness_per);
        tem_per = (TextView) findViewById(R.id.tem_per);
        lightness_sb = (SeekBar) findViewById(R.id.lightness_sb);
        lightness_sb.setOnSeekBarChangeListener(this);
        tem_sb = (SeekBar) findViewById(R.id.tem_sb);
        tem_sb.setOnSeekBarChangeListener(this);
        oktv.setOnClickListener(this);
        light_switch = (ImageView) findViewById(R.id.light_switch);
        light_switch.setOnClickListener(this);
        headtitle.setText(light.getName());
        is_light_on = light.is_on();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!is_light_on) {
            light_switch.setImageResource(R.drawable.tbtn_off);
        } else {
            light_switch.setImageResource(R.drawable.tbtn_on);
        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.ok_tv:
                if (!is_light_on) {
                    showShortToast(getResources().getString(R.string.please_open_switch));
                    return;
                }
                try {
                    showLoadingDialog(getResources().getString(R.string.cmd_sending));
//                    count_cmd_timeout_rsp = 0;
//                    count_set_bc_rsp = 0;
                    MyApplication.getInstance().mService.setBrightChrome(mDevice.getAddress(), temIntent.getIntExtra("index", 1), lighenessValue, temValue);
                    Log.i(TAG, "setBrightChrome:" + mDevice.getAddress() + "  " + getIntent().getIntExtra("index", 1) + " " + lighenessValue + "  " + temValue);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.light_switch:
                if (!is_light_on) {
                    light_switch.setImageResource(R.drawable.tbtn_on);
                    if (lighenessValue == 0) {
                        lighenessValue = 128;
                        lightness_sb.setProgress(lighenessValue);
                    }
                    is_light_on = true;
                    //发送开关请求
                } else {
                    light_switch.setImageResource(R.drawable.tbtn_off);
                    lighenessValue = 0;
                    lightness_sb.setProgress(lighenessValue);
                    try {
//                        count_cmd_timeout_rsp = 0;
//                        count_set_bc_rsp = 0;
                        MyApplication.getInstance().mService.setBrightChrome(mDevice.getAddress(), temIntent.getIntExtra("index", 1), lighenessValue, temValue);
                        showLoadingDialog(getResources().getString(R.string.cmd_sending));
                    } catch (RemoteException e) {
                        closeLoadingDialog();
                        e.printStackTrace();
                    }
                    is_light_on = false;
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        switch (seekBar.getId()) {
            case R.id.lightness_sb:
                lighenessValue = progress;
                if (lighenessValue == 0) {
                    light_switch.setImageResource(R.drawable.tbtn_off);
                    is_light_on = false;
                } else {
                    light_switch.setImageResource(R.drawable.tbtn_on);
                    is_light_on = true;
                }
                lightness_per.setText((int) ((lighenessValue / 255.00) * 100) + "%");
                break;
            case R.id.tem_sb:
                temValue = progress;
                tem_per.setText((int) ((temValue / 255.00) * 100) + "%");
                break;
            default:
                break;
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Lg.i(TAG, "onStartTrackingTouch");
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Lg.i(TAG, "onStopTrackingTouch");
    }


    private ICallback.Stub mCallback = new ICallback.Stub() {
        @Override
        public void onConnect(String address) throws RemoteException {

        }

        @Override
        public void onDisconnect(String address) throws RemoteException {
            Lg.i(TAG, TAG + "onDisconnect");
            closeLoadingDialog();
            if (NetStatuCheck.checkGPRSState(ControlLightActivity.this).equals("unavailable") &&
                    MyApplication.getInstance().isSocketConnectBreak) {
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        if (getTopActivity() != null) {
                            if (getTopActivity().contains("ControlLightActivity")) {
                                showComReminderDialog();
                            }
                        }
                    }
                });

            }
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
        public void onSwitchRsp(String imei, String ret) throws RemoteException {

        }

        @Override
        public void onGetStatusRsp(String imei, final int ret) throws RemoteException {

        }


        @Override
        public void onCmdTimeout(String cmd, String imei) throws RemoteException {
            closeLoadingDialog();
            Lg.i(TAG, "onCmdTimeout");
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
        public void onSetBrightChromeRsp(String imei, final int ret) throws RemoteException {
            Lg.i(TAG, "onSetBrightChromeRsp->>" + "imei:" + imei + "   ret:" + ret);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    closeLoadingDialog();
                    if (ret == 0) {
                        showShortToast(getResources().getString(R.string.str_success));
                    } else {
                        showShortToast(getResources().getString(R.string.str_fail));
                    }
//                    if (ret != 0) {
                        try {
                            MyApplication.getInstance().mService.getBrightChrome(mDevice.getAddress(), temIntent.getIntExtra("index", 1));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
//                    }
                }
            });
        }

        @Override
        public void onGetBrightChromeRsp(String imei, int index, final int bright, final int chrome) throws RemoteException {
            Log.i(TAG, "onGetBrightChromeRsp->>" + "index:" + index + "bright:" + bright + "chrome:" + chrome);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    closeLoadingDialog();
                    if (bright > 0) {
                        is_light_on = true;
                        light_switch.setImageResource(R.drawable.tbtn_on);

                    } else {
                        is_light_on = false;
                        light_switch.setImageResource(R.drawable.tbtn_off);
                    }
                    lightness_sb.setProgress(bright);
                    tem_sb.setProgress(chrome);
                }
            });
        }

        @Override
        public void onPairLightRsp(String imei, int ret) throws RemoteException {

        }

        @Override
        public void onCreateTimerRsp(String imei, int ret) throws RemoteException {

        }

        @Override
        public void onTimerNotify(String imei, int ret) throws RemoteException {

        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            MyApplication.getInstance().mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyApplication.getInstance().mService = IService.Stub.asInterface(service);
            Log.i(TAG, " MyApplication.getInstance().mService:" + MyApplication.getInstance().mService);
            try {
                MyApplication.getInstance().mService.registerCallback(mCallback);
                MyApplication.getInstance().mService.getBrightChrome(mDevice.getAddress(), temIntent.getIntExtra("index", 1));
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
    };

    @Override
    protected void onDestroy() {
        Lg.i(TAG, "onDestroy");
        if (mConnection != null) {
            try {
                Lg.i(TAG, "onDestroy->>unregisterCallback");
                MyApplication.getInstance().mService.unregisterCallback(mCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(mConnection);
        Lg.i(TAG, "onDestroy->>unbindService");
        super.onDestroy();
    }
}
