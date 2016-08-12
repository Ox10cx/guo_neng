package com.watch.guoneng.ui;


import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.watch.guoneng.BuildConfig;
import com.watch.guoneng.R;
import com.watch.guoneng.adapter.DeviceListAdapter;
import com.watch.guoneng.app.MyApplication;
import com.watch.guoneng.dao.UserDao;
import com.watch.guoneng.dao.WifiDeviceDao;
import com.watch.guoneng.model.User;
import com.watch.guoneng.model.WifiDevice;
import com.watch.guoneng.service.WifiConnectService;
import com.watch.guoneng.service.WifiHttpConnectService;
import com.watch.guoneng.tool.BaseTools;
import com.watch.guoneng.tool.Lg;
import com.watch.guoneng.tool.NetStatuCheck;
import com.watch.guoneng.tool.NetworkChangeReceiver;
import com.watch.guoneng.util.HttpUtil;
import com.watch.guoneng.util.ImageLoaderUtil;
import com.watch.guoneng.util.JsonUtil;
import com.watch.guoneng.util.PreferenceUtil;
import com.watch.guoneng.util.ThreadPoolManager;
import com.watch.guoneng.xlistview.EditDeviceDialog;

import net.simonvt.menudrawer.MenuDrawer;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Administrator on 16-3-7.
 */
public class DeviceListActivity extends BaseActivity implements View.OnClickListener, DeviceListAdapter.OnItemImageViewClickCallback
        , BaseTools.OnEditUserInfoListener, ListView.OnItemClickListener, ListView.OnItemLongClickListener {
    private static final String TAG = "DeviceListActivity";
    private static final int MSG_GETLIST = 7;
    private static final int MSG_GETWIFIDEVICE = 2;
    private static final int MSG_UNLINKDEVICE = 3;
    private static final int MSG_UPDATELOGINSTATUS = 4;
    private static final int MSG_EDITDEVICENAME = 5;
    private static final int MSG_DEVICESWITCHSTATUSRSP = 6;

    private ListView mDeviceList;
    private DeviceListAdapter mDeviceListAdapter;
    ArrayList<WifiDevice> mListData;
    WifiDeviceDao mDeviceDao;

    private ImageView left_menu;
    private ImageView add_menu;
    private MenuDrawer menuDrawer;
    private ImageView iv_photo;
    private TextView tv_name;
    private UserDao mUserDao;
    private User mUser;
    private String userid;
    private int LINK_DEVICE = 1;

    /**
     * 修改名称的设备index
     */
    private int index = 0;

    /**
     * 需要修改的设备名称
     */
    private String device_name = "";
    private NetworkChangeReceiver networkChangeReceiver = null;
    private boolean isBlindService = false;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            closeLoadingDialog();
            String result = "";
            if (msg.obj != null) {
                result = msg.obj.toString();
            }
            if (result.matches("Connection to .* refused") || result.matches("Connect to.*timed out")) {
                showComReminderDialog();
                return;
            }
            switch (msg.what) {
                case MSG_GETLIST: {
                    handleDeviceListRsp(result);
                    if (isBlindService) {
                        blindService();
                    }
                    break;
                }

                case MSG_GETWIFIDEVICE: {
                    handleDeviceRsp(result);
                    break;
                }

                case MSG_UNLINKDEVICE: {
                    handleUnlinkDevice(msg, result);
                    break;
                }

                case MSG_UPDATELOGINSTATUS: {
                    handleUpdataLoginStatus(result);
                    break;
                }

                case MSG_EDITDEVICENAME:
                    handleEditDeviceName(result);
                    break;
                case MSG_DEVICESWITCHSTATUSRSP:
                    WifiDevice wifiDevice = mListData.get(msg.arg1);
                    if (msg.arg2 == 1) {
                        wifiDevice.setSwitchStatus(true);
                    } else {
                        wifiDevice.setSwitchStatus(false);
                    }
                    mDeviceListAdapter.notifyDataSetChanged();
                    break;
                default:
                    break;
            }
        }
    };

    private void handleEditDeviceName(String result) {
        JSONObject json;
        try {
            json = new JSONObject(result);
            if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                BaseTools.showToastByLanguage(DeviceListActivity.this, json);
            } else {
                BaseTools.showToastByLanguage(DeviceListActivity.this, json);
                mListData.get(index).setName(device_name);
                mDeviceListAdapter.notifyDataSetChanged();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleUpdataLoginStatus(String result) {
        JSONObject json;
        try {
            json = new JSONObject(result);
            if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                BaseTools.showToastByLanguage(DeviceListActivity.this, json);
            } else {
                BaseTools.showToastByLanguage(DeviceListActivity.this, json);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleUnlinkDevice(Message msg, String result) {
        JSONObject json;
        int postion = msg.arg1;
        try {
            json = new JSONObject(result);
            if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                BaseTools.showToastByLanguage(DeviceListActivity.this, json);
            } else {
                mDeviceListAdapter.updateDataSet(postion - mDeviceList.getHeaderViewsCount());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleDeviceRsp(String result) {
        JSONObject json;
        try {
            json = new JSONObject(result);
            if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                BaseTools.showToastByLanguage(DeviceListActivity.this, json);
            } else {
                JSONObject ob = json.getJSONObject("wifi");
                int i;
                String imei = ob.getString("imei");
                WifiDevice d = null;
                for (i = 0; i < mListData.size(); i++) {
                    if (imei.equals(mListData.get(i).getAddress())) {
                        d = mListData.get(i);
                        break;
                    }
                }
                Lg.i("hjq", "d = " + d);

                if (d != null) {
                    String name;
                    int status = WifiDevice.INACTIVE_STATUS;

                    if (ob.has("name")) {
                        name = ob.getString("name");
                    } else {
                        name = "unkown";
                    }

                    if (ob.has("status")) {
                        status = ob.getInt("status");
                    }

                    d.setName(name);
                    d.setStatus(status);
                    mListData.set(i, d);

                    mDeviceListAdapter.notifyDataSetChanged();
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleDeviceListRsp(String result) {
        JSONObject json;
        try {
            json = new JSONObject(result);
            if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                BaseTools.showToastByLanguage(DeviceListActivity.this, json);
            } else {
                JSONArray wifilist = json.getJSONArray("wifis");
                for (int i = 0; i < wifilist.length(); i++) {
                    JSONObject ob = wifilist.getJSONObject(i);
                    String address = ob.getString("imei");
                    String name;
                    int status = WifiDevice.INACTIVE_STATUS;

                    if (ob.has("name")) {
                        name = ob.getString("name");
                    } else {
                        name = "unkown";
                    }

                    if (ob.has("status")) {
                        status = ob.getInt("status");
                    }

                    WifiDevice d = new WifiDevice(null, name, address);
                    d.setStatus(status);
                    mListData.add(d);
                }

                mDeviceListAdapter.notifyDataSetChanged();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        registerNetChangeBroad();
        fillListData();
        showLoadingDialog(getResources().getString(R.string.waiting));
        isBlindService = true;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ThreadPoolManager.getInstance().addTask(new Runnable() {
                    @Override
                    public void run() {
                        String result = HttpUtil.post(HttpUtil.URL_GETWIFIDEVICELIST, new BasicNameValuePair("name", "qin"));
                        Lg.i(TAG, "URL_GETWIFIDEVICELIST----result = " + result);
                        Message msg = new Message();
                        msg.obj = result;
                        msg.what = MSG_GETLIST;
                        mHandler.sendMessage(msg);
                    }
                });
            }
        });
//        blindService();
    }

    /**
     * 绑定服务
     */
    private void blindService() {
        Lg.i(TAG, "blindService");
        Intent i = null;
        if (BuildConfig.USE_LONG_CONNECTION.equals("1")) {
            i = new Intent(this, WifiConnectService.class);
        } else {
            i = new Intent(this, WifiHttpConnectService.class);
        }
        getApplicationContext().bindService(i, mConnection, BIND_AUTO_CREATE);
    }

    /**
     * 注册网络变化广播
     */
    private void registerNetChangeBroad() {
        networkChangeReceiver = new NetworkChangeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, filter);
    }

    private void initView() {
        //左滑
        menuDrawer = MenuDrawer.attach(this);
        menuDrawer.setContentView(R.layout.activity_devicelist);
        menuDrawer.setMenuView(R.layout.left_menu);

        iv_photo = (ImageView) findViewById(R.id.iv_photo);
        iv_photo.setOnClickListener(this);
        tv_name = (TextView) findViewById(R.id.tv_name);
        mUserDao = new UserDao(this);
        mUser = mUserDao.queryById(PreferenceUtil.getInstance(this).getUid());
        if (mUser != null) {
            tv_name.setText(BaseTools.subStringByBytes(mUser.getName(), 10));
        }
        left_menu = (ImageView) findViewById(R.id.left_menu);
        left_menu.setOnClickListener(this);
        add_menu = (ImageView) findViewById(R.id.add_menu);
        add_menu.setOnClickListener(this);
        mDeviceDao = new WifiDeviceDao(this);
        mDeviceList = (ListView) findViewById(R.id.devicelist);
//        mDeviceList.setOnItemClickListener(this);
        mDeviceList.setOnItemLongClickListener(this);
        new BaseTools().setEditUserInfoListener(this);
        updataUserInfo();
    }


    private void fillListData() {
        mListData = new ArrayList<>(10);
        mDeviceListAdapter = new DeviceListAdapter(this, mListData, this);
        mDeviceList.setAdapter(mDeviceListAdapter);
    }


    @Override
    protected void onDestroy() {
        MyApplication.getInstance().isSocketConnectBreak = false;
        if (mConnection != null) {
            try {
                MyApplication.getInstance().mService.unregisterCallback(mCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        getApplicationContext().unbindService(mConnection);
        unregisterReceiver(networkChangeReceiver);
        super.onDestroy();
    }


    void updateWifiDeviceStatus(final String imei) {
        ThreadPoolManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                String result = HttpUtil.post(HttpUtil.URL_GETWIFIDEVICE, new BasicNameValuePair("imei", imei));
                Lg.i(TAG, "result = " + result);
                Message msg = new Message();
                msg.obj = result;
                msg.what = MSG_GETWIFIDEVICE;
                mHandler.sendMessage(msg);
            }
        });
    }

    void updateWifiDeviceLoginStatus(final String imei, final int status) {
        ThreadPoolManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                String result = HttpUtil.post(HttpUtil.URL_UPDATEWIFILOGINSTATUS,
                        new BasicNameValuePair("imei", imei),
                        new BasicNameValuePair("status", Integer.toString(status)));
                Lg.i(TAG, "result = " + result);

                Message msg = new Message();
                msg.obj = result;
                msg.what = MSG_UPDATELOGINSTATUS;
                mHandler.sendMessage(msg);
            }
        });
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.left_menu:
                if (menuDrawer.isMenuVisible()) {
                    menuDrawer.closeMenu();
                } else {
                    menuDrawer.openMenu();
                }
                break;
            case R.id.add_menu:
                intent = new Intent(DeviceListActivity.this, SmartLinkActivity.class);
                startActivityForResult(intent, LINK_DEVICE);
                break;
            case R.id.iv_photo:
                if (mUser != null) {
                    intent = new Intent(DeviceListActivity.this, PersonInfoActivity.class);
                    startActivity(intent);
                } else {
                    intent = new Intent(DeviceListActivity.this, AuthLoginActivity.class);
                    startActivity(intent);
                }
                break;
            default:
                break;
        }
    }

    private ICallback.Stub mCallback = new ICallback.Stub() {
        @Override
        public void onConnect(String address) throws RemoteException {
            Lg.i(TAG, "onConnect");
            MyApplication.getInstance().longConnected = true;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    pingWifiDevice();
                    closeLoadingDialog();
                    closeComReminderDialog();
                    mDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onDisconnect(String address) throws RemoteException {
            Lg.i(TAG, "onDisconnect");
            MyApplication.getInstance().longConnected = false;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    closeLoadingDialog();
                    mDeviceListAdapter.notifyDataSetChanged();
                    if (MyApplication.getInstance().isSocketConnectBreak) {
                        //有网络自动连接
                        if (!NetStatuCheck.checkGPRSState(DeviceListActivity.this).equals("unavailable")) {
                            try {
                                MyApplication.getInstance().mService.connect(null);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else {  //没有网络
                            Lg.i(TAG, "getTopActivity():" + getTopActivity());
                            if (getTopActivity() != null) {
                                if (getTopActivity().contains("DeviceListActivity")) {
                                    showComReminderDialog();
                                }
                            }
                        }
                    }
                }
            });
        }

        @Override
        public boolean onRead(String address, byte[] val) throws RemoteException {
            Lg.i(TAG, "onRead called");
            return false;
        }


        @Override
        public boolean onWrite(final String address, byte[] val) throws RemoteException {
            Lg.i(TAG, "onWrite called");
            return true;
        }

        @Override
        public void onNotify(final String imei, int type) throws RemoteException {
            Lg.i(TAG, "onNotify-type-->>>>" + type);
//            if (type == 100) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateWifiDeviceStatus(imei);
                }
            });
//            } else if (type == 1) {
            for (int i = 0; i < mListData.size(); i++) {
                if (imei.equalsIgnoreCase(mListData.get(i).getAddress())) {
                    MyApplication.getInstance().mService.getLightStatus(mListData.get(i).getAddress());
                    break;
                }
            }
//            }

        }

        @Override
        public void onSwitchRsp(String imei, boolean ret) throws RemoteException {
            Lg.i(TAG, "onSwitchRsp-ret-->>>>" + ret);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    closeLoadingDialog();
                }
            });
            if (!ret) {
                MyApplication.getInstance().mService.getLightStatus(mListData.get(index).getAddress());
            }
        }

        @Override
        public void onGetStatusRsp(String imei, int ret) throws RemoteException {
            Lg.i(TAG, "onGetStatusRsp->>imei:" + imei + "   ret:" + ret);
            synchronized (this) {
                for (int i = 0; i < mListData.size(); i++) {
                    if (imei.equalsIgnoreCase(mListData.get(i).getAddress())) {
                        Message message = new Message();
                        message.what = MSG_DEVICESWITCHSTATUSRSP;
                        message.arg1 = i;
                        message.arg2 = ret;
                        mHandler.sendMessage(message);
                        break;
                    }
                }
            }
        }

        //待优化
        @Override
        public void onCmdTimeout(String cmd, final String imei) throws RemoteException {
            Lg.i(TAG, "onCmdTimeout");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    closeLoadingDialog();
                }
            });
            if (cmd.equals(WifiConnectService.PING_CMD)) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Lg.i(TAG, "onCmdTimeout_LOGOUT_STATUS");
                        setDeviceStatus(imei, WifiDevice.LOGOUT_STATUS);
                        mDeviceListAdapter.notifyDataSetChanged();
                        updateWifiDeviceLoginStatus(imei, WifiDevice.LOGOUT_STATUS);
                    }
                });
            }
        }

        @Override
        public void onHttpTimeout(String cmd, String imei) throws RemoteException {
            Lg.i(TAG, "onHttpTimeout");
        }

        @Override
        public void onPingRsp(final String imei, final int ret) throws RemoteException {
            Lg.i(TAG, "onPingRsp");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    setDeviceStatus(imei, WifiDevice.LOGIN_STATUS);
                    mDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onGetLightList(String imei, byte[] list) throws RemoteException {

        }

        @Override
        public void onSetBrightChromeRsp(String imei, int ret) throws RemoteException {
            Lg.i(TAG, "onSetBrightChromeRsp");
        }

        @Override
        public void onGetBrightChromeRsp(String imei, int index, int bright, int chrome) throws RemoteException {

        }

        @Override
        public void onPairLightRsp(String imei, int ret) throws RemoteException {

        }
    };

    void setDeviceStatus(String imei, int status) {
        for (int i = 0; i < mListData.size(); i++) {
            WifiDevice d = mListData.get(i);
            if (d.getAddress().equals(imei)) {
                d.setStatus(status);
                return;
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Lg.i(TAG, "onServiceDisconnected");
            MyApplication.getInstance().mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Lg.i(TAG, "onServiceConnected");
            MyApplication.getInstance().mService = IService.Stub.asInterface(service);
            Lg.i(TAG, "MyApplication.getInstance().mService:" + MyApplication.getInstance().mService);
            try {
                MyApplication.getInstance().mService.registerCallback(mCallback);
            } catch (RemoteException e) {
                Lg.i(TAG, "" + e);
            }

            if (BuildConfig.USE_LONG_CONNECTION.equals("1")) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        connectLongSocket();
                    }
                });
            }
        }
    };

    public void pingWifiDevice() {
        for (int i = 0; i < mListData.size(); i++) {
            WifiDevice d = mListData.get(i);
            if (d.getStatus() == WifiDevice.LOGIN_STATUS) {
                try {
                    MyApplication.getInstance().mService.ping(d.getAddress(), 1);

                    //lzg edit
                    MyApplication.getInstance().mService.getLightStatus(d.getAddress());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void connectLongSocket() {
        try {
            if (MyApplication.getInstance().mService != null) {
                MyApplication.getInstance().mService.connect(null);
                MyApplication.getInstance().isFirstLongCon = true;
            } else {
                showShortToast(getString(R.string.link_service_fail));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Lg.i(TAG, "onActivityResult->>requestCode:" + requestCode + "  resultCode:" + resultCode);
        if (resultCode == RESULT_OK) {
            if (requestCode == LINK_DEVICE) {
                Bundle b = data.getExtras();
                int changed = b.getInt("ret", 0);
                Lg.i(TAG, "changed = " + changed);
                if (changed == 1) {
                    mListData.clear();
                    isBlindService = false;
                    ThreadPoolManager.getInstance().addTask(new Runnable() {
                        @Override
                        public void run() {
                            String result = HttpUtil.post(HttpUtil.URL_GETWIFIDEVICELIST, new BasicNameValuePair("name", "qin"));
                            Lg.i(TAG, "URL_GETWIFIDEVICELIST_result = " + result);
                            Message msg = new Message();
                            msg.obj = result;
                            msg.what = MSG_GETLIST;
                            mHandler.sendMessage(msg);
                        }
                    });
                }
            }
        } else if (requestCode == 212 && resultCode == 222) {
            Lg.i(TAG, "edit device name");
            device_name = data.getStringExtra("light_name");
            ThreadPoolManager.getInstance().addTask(new Runnable() {
                @Override
                public void run() {
                    String result = HttpUtil.post(HttpUtil.URL_EDITDEVICENAME,
                            new BasicNameValuePair("imei", mListData.get(index).getAddress()),
                            new BasicNameValuePair("name", device_name));
                    Lg.i(TAG, "URL_EDITDEVICENAME_result = " + result);
                    Message msg = new Message();
                    msg.obj = result;
                    msg.what = MSG_EDITDEVICENAME;
                    mHandler.sendMessage(msg);
                }
            });
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    //按返回键的监听事情
    @Override
    public void onBackPressed() {
        if (menuDrawer.isMenuVisible()) {
            menuDrawer.closeMenu();
            return;
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onEditUserInfo() {
        Lg.i(TAG, "onEditUserInfo()");
        updataUserInfo();
    }

    private void updataUserInfo() {
        userid = PreferenceUtil.getInstance(this).getUid();
        mUser = mUserDao.queryById(userid);
        tv_name.setText(BaseTools.subStringByBytes(mUser.getName(), 10));
        if (mUser.getImage() == null || "".equals(mUser.getImage())) {
            iv_photo.setImageResource(R.drawable.photo);
        } else {
            ImageLoaderUtil.displayImage(HttpUtil.SERVER + mUser.getImage_thumb(), iv_photo, this);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Lg.i(TAG, "onItemClick");
//        if (i < 0) {
//            return;
//        }
//        WifiDevice d = mListData.get(i);
//        if (!BuildConfig.USE_LONG_CONNECTION.equals("1")) {
//            Intent intent = new Intent(this, GroupLightActivity.class);
//            intent.putExtra("device", d);
//            startActivity(intent);
//        } else {
//            if (d.getStatus() == WifiDevice.LOGIN_STATUS) {
//                Lg.i(TAG, "MyApplication.getInstance().longConnected->>>" + MyApplication.getInstance().longConnected);
//                if (MyApplication.getInstance().longConnected) {
//                    Intent intent = new Intent(this, GroupLightActivity.class);
//                    intent.putExtra("device", d);
//                    startActivity(intent);
//                } else {
//                    showShortToast(getString(R.string.service_long_socket_breaked));
//                }
//
//            } else {
//                showShortToast(getString(R.string.wifi_device_offline));
//            }
//        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        Lg.i(TAG, "onItemLongClick");
        //删除
        final WifiDevice d = mListData.get(i);
        index = i;
        final EditDeviceDialog dialog = new EditDeviceDialog(this);
        dialog.show();
        dialog.setCanceledOnTouchOutside(true);
        dialog.delete_light.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                ThreadPoolManager.getInstance().addTask(new Runnable() {
                    @Override
                    public void run() {
                        String result = HttpUtil.post(HttpUtil.URL_UNLINKWIFIDEVICE, new BasicNameValuePair("imei", d.getAddress()));
                        Lg.i(TAG, "URL_UNLINKWIFIDEVICE_result = " + result);
                        Message msg = new Message();
                        msg.obj = result;
                        msg.what = MSG_UNLINKDEVICE;
                        msg.arg1 = index;
                        mHandler.sendMessage(msg);
                    }
                });
            }
        });

        dialog.edit_light.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.cancel();
                Lg.i(TAG, "edit device name");
                Intent intent = new Intent(DeviceListActivity.this, AddLightActivity.class);
                intent.putExtra("name", d.getName());
                startActivityForResult(intent, 212);
            }
        });
        return true;
    }

    @Override
    public void onImageViewClick(int postion) {
        index = postion;
        WifiDevice wifiDevice = mListData.get(postion);
        if (wifiDevice != null) {
            if (MyApplication.getInstance().longConnected) {
                if (wifiDevice.getStatus() == WifiDevice.LOGIN_STATUS) {
                    try {
                        showLoadingDialog(getResources().getString(R.string.cmd_sending));
                        if (wifiDevice.isSwitchStatus()) {
                            MyApplication.getInstance().mService.enableLight(wifiDevice.getAddress(), false);
                            wifiDevice.setSwitchStatus(false);
                        } else {
                            MyApplication.getInstance().mService.enableLight(wifiDevice.getAddress(), true);
                            wifiDevice.setSwitchStatus(true);
                        }
                        mDeviceListAdapter.notifyDataSetChanged();
                    } catch (RemoteException e) {
                        Lg.i(TAG, e.toString());
                    }
                } else {
                    showShortToast(getString(R.string.wifi_device_offline));
                }
            } else {
                showShortToast(getString(R.string.service_long_socket_breaked));
            }
        }

    }
}
