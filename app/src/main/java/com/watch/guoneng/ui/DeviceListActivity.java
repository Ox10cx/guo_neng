package com.watch.guoneng.ui;


import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
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
import com.watch.guoneng.util.HttpUtil;
import com.watch.guoneng.util.ImageLoaderUtil;
import com.watch.guoneng.util.JsonUtil;
import com.watch.guoneng.util.PreferenceUtil;
import com.watch.guoneng.util.ThreadPoolManager;
import com.watch.guoneng.xlistview.Menu;
import com.watch.guoneng.xlistview.MenuItem;
import com.watch.guoneng.xlistview.SlideAndDragListView;

import net.simonvt.menudrawer.MenuDrawer;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Administrator on 16-3-7.
 */
public class DeviceListActivity extends BaseActivity implements View.OnClickListener,
        AdapterView.OnItemClickListener, DeviceListAdapter.OnItemClickCallback, SlideAndDragListView.OnListItemLongClickListener,
        SlideAndDragListView.OnDragListener, SlideAndDragListView.OnSlideListener,
        SlideAndDragListView.OnListItemClickListener, SlideAndDragListView.OnMenuItemClickListener,
        SlideAndDragListView.OnItemDeleteListener, BaseTools.OnEditUserInfoListener {
    private static final String TAG = "DeviceListActivity";
    private static final int CHANGE_BLE_DEVICE_SETTING = 1;
    private static final int MSG_GETLIST = 1;
    private static final int MSG_GETWIFIDEVICE = 2;
    private static final int MSG_UNLINKDEVICE = 3;
    private static final int MSG_UPDATELOGINSTATUS = 4;

    private SlideAndDragListView mDeviceList;
    private DeviceListAdapter mDeviceListAdapter;
    boolean mScanningStopped;
    ArrayList<WifiDevice> mListData;


    private Menu mMenu;
    WifiDeviceDao mDeviceDao;

    private ImageView left_menu;
    private ImageView add_menu;
    private MenuDrawer menuDrawer;
    private ImageView iv_photo;
    private TextView tv_name;
    private UserDao mUserDao;
    private User mUser;
    private String userid;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            String result = msg.obj.toString();
            closeLoadingDialog();
            if (result.matches("Connection to .* refused") || result.matches("Connect to.*timed out")) {
                showComReminderDialog();
                return;
            }
            switch (msg.what) {
                case MSG_GETLIST: {
                    try {
                        JSONObject json = new JSONObject(result);
                        if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                            BaseTools.showToastByLanguage(DeviceListActivity.this,json);
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
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    break;
                }

                case MSG_GETWIFIDEVICE: {
                    JSONObject json;
                    try {
                        json = new JSONObject(result);
                        if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                            BaseTools.showToastByLanguage(DeviceListActivity.this,json);
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
                            Log.e("hjq", "d = " + d);

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

                    break;
                }

                case MSG_UNLINKDEVICE: {
                    JSONObject json;
                    int postion = msg.arg1;
                    try {
                        json = new JSONObject(result);
                        if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                            BaseTools.showToastByLanguage(DeviceListActivity.this,json);
                        } else {
                            mDeviceListAdapter.updateDataSet(postion - mDeviceList.getHeaderViewsCount());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    break;
                }

                case MSG_UPDATELOGINSTATUS: {
                    JSONObject json;
                    try {
                        json = new JSONObject(result);
                        if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                            BaseTools.showToastByLanguage(DeviceListActivity.this,json);
                        } else {
                            BaseTools.showToastByLanguage(DeviceListActivity.this,json);
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

    //    boolean MyApplication.getInstance().longConnected;
    private int LINK_DEVICE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_devicelist);
        //左滑
        menuDrawer = MenuDrawer.attach(this);
        menuDrawer.setContentView(R.layout.activity_devicelist);
        menuDrawer.setMenuView(R.layout.left_menu);
//        menuDrawer.setSlideDrawable(R.drawable.ic_drawer);
//        menuDrawer.setDrawerIndicatorEnabled(true);
//        menuDrawer.peekDrawer(1000, 0);

        initView();
        initMenu();
        initUiAndListener();
        fillListData();

        Intent i;
        if (BuildConfig.USE_LONG_CONNECTION.equals("1")) {
            i = new Intent(this, WifiConnectService.class);
        } else {
            i = new Intent(this, WifiHttpConnectService.class);
        }
        getApplicationContext().bindService(i, mConnection, BIND_AUTO_CREATE);

        showLoadingDialog(getResources().getString(R.string.waiting));
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ThreadPoolManager.getInstance().addTask(new Runnable() {
                    @Override
                    public void run() {
                        String result = HttpUtil.post(HttpUtil.URL_GETWIFIDEVICELIST, new BasicNameValuePair("name", "qin"));
                        Log.e(TAG, "result = " + result);

                        Message msg = new Message();
                        msg.obj = result;
                        msg.what = MSG_GETLIST;
                        mHandler.sendMessage(msg);
                    }
                });
            }
        }, 500);
    }

    private void initView() {
        iv_photo = (ImageView) findViewById(R.id.iv_photo);
        iv_photo.setOnClickListener(this);
        tv_name = (TextView) findViewById(R.id.tv_name);
        mUserDao = new UserDao(this);
        mUser = mUserDao.queryById(PreferenceUtil.getInstance(this).getUid());
        if (mUser != null) {
            tv_name.setText(mUser.getName());
        }
        left_menu = (ImageView) findViewById(R.id.left_menu);
        left_menu.setOnClickListener(this);
        add_menu = (ImageView) findViewById(R.id.add_menu);
        add_menu.setOnClickListener(this);
        mDeviceDao = new WifiDeviceDao(this);
        mDeviceList = (SlideAndDragListView) findViewById(R.id.devicelist);
        mDeviceList.setOnItemClickListener(DeviceListActivity.this);
        new BaseTools().setEditUserInfoListener(this);
        updataUserInfo();
    }

    public void initMenu() {
        mMenu = new Menu(new ColorDrawable(Color.WHITE), true);
        mMenu.addItem(new MenuItem.Builder().setWidth((int) getResources().getDimension(R.dimen.slv_item_bg_btn_width) * 2)
                .setBackground(new ColorDrawable(Color.RED))
                .setText(getString(R.string.system_delete))
                .setDirection(MenuItem.DIRECTION_RIGHT)
                .setTextColor(Color.BLACK)
                .setTextSize((int) getResources().getDimension(R.dimen.txt_size))
                .build());
    }

    public void initUiAndListener() {
        mDeviceList.setMenu(mMenu);
        mDeviceList.setOnListItemLongClickListener(this);
        mDeviceList.setOnDragListener(this, mListData);
        mDeviceList.setOnListItemClickListener(this);
        mDeviceList.setOnSlideListener(this);
        mDeviceList.setOnMenuItemClickListener(this);
        mDeviceList.setOnItemDeleteListener(this);
    }


    private void fillListData() {
        mListData = new ArrayList<WifiDevice>(10);
        mDeviceListAdapter = new DeviceListAdapter(this,
                mListData,
                DeviceListActivity.this);
        mDeviceList.setAdapter(mDeviceListAdapter);
    }


    @Override
    protected void onDestroy() {
        if (mConnection != null) {
            try {
                MyApplication.getInstance().mService.unregisterCallback(mCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        getApplicationContext().unbindService(mConnection);
        super.onDestroy();
    }


    void updateWifiDeviceStatus(final String imei) {
        ThreadPoolManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                String result = HttpUtil.post(HttpUtil.URL_GETWIFIDEVICE, new BasicNameValuePair("imei", imei));
                Log.e(TAG, "result = " + result);

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
                Log.e(TAG, "result = " + result);

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
                    mDeviceListAdapter.notifyDataSetChanged();
                    closeLoadingDialog();
                    closeComReminderDialog();
                    pingWifiDevice();
                }
            });
        }

        @Override
        public void onDisconnect(String address) throws RemoteException {
            Lg.i(TAG, TAG + "onDisconnect");
            MyApplication.getInstance().longConnected = false;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDeviceListAdapter.notifyDataSetChanged();
                    closeLoadingDialog();
                    if (MyApplication.getInstance().isSocketConnectBreak) {
                        //长连接意外断开重连接
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
                                if (getTopActivity().contains("MainActivity")) {
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
            Log.d("hjq", "onRead called");
            return false;
        }


        @Override
        public boolean onWrite(final String address, byte[] val) throws RemoteException {
            Log.d("hjq", "onWrite called");
            return true;
        }

        @Override
        public void onNotify(final String imei, int type) throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateWifiDeviceStatus(imei);
                }
            });
        }

        @Override
        public void onSwitchRsp(String imei, boolean ret) throws RemoteException {

        }

        @Override
        public void onGetStatusRsp(String imei, int ret) throws RemoteException {

        }

        @Override
        public void onCmdTimeout(String cmd, final String imei) throws RemoteException {
            Lg.i(TAG, "onCmdTimeout");
            if (cmd.equals(WifiConnectService.PING_CMD)) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setDeviceStatus(imei, WifiDevice.LOGOUT_STATUS);
                        mDeviceListAdapter.notifyDataSetChanged();
                        updateWifiDeviceLoginStatus(imei, WifiDevice.LOGOUT_STATUS);
                    }
                });
            }
        }

        @Override
        public void onHttpTimeout(String cmd, String imei) throws RemoteException {

        }

        @Override
        public void onPingRsp(final String imei, final int ret) throws RemoteException {
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
            Log.d(TAG, "onServiceDisconnected");
            MyApplication.getInstance().mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            MyApplication.getInstance().mService = IService.Stub.asInterface(service);
            Log.i(TAG, "MyApplication.getInstance().mService:" + MyApplication.getInstance().mService);
            try {
                MyApplication.getInstance().mService.registerCallback(mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }

            if (BuildConfig.USE_LONG_CONNECTION.equals("1")) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
//                    connectSocketDialog();
                        connectLongSocket();
                    }
                });
            }
        }
    };

    void pingWifiDevice() {
        int i;

        for (i = 0; i < mListData.size(); i++) {
            WifiDevice d = mListData.get(i);
            if (d.getStatus() == WifiDevice.LOGIN_STATUS) {
                try {
                    MyApplication.getInstance().mService.ping(d.getAddress(), 1);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Log.d("hjq", "xxx id = " + id);
    }

    @Override
    public void onButtonClick(View view, int position) {
        Button v = (Button) view;
        WifiDevice device = mListData.get(position);
        int linkstatus = device.getLinkStatus();

        Log.d("hjq", "linkstatus = " + linkstatus);

        if (linkstatus == WifiDevice.CONNECTED) {
            try {
                MyApplication.getInstance().mService.disconnect(device.getAddress());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            try {
                MyApplication.getInstance().mService.connect(device.getAddress());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mDeviceListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRightArrowClick(int position) {
        WifiDevice d = mListData.get(position);
        if (!BuildConfig.USE_LONG_CONNECTION.equals("1"))  {
            Intent intent = new Intent(this, GroupLightActivity.class);
            intent.putExtra("device", d);
            startActivity(intent);
        } else {
            if (d.getStatus() == WifiDevice.LOGIN_STATUS) {
                if (MyApplication.getInstance().longConnected) {
                    Intent intent = new Intent(this, GroupLightActivity.class);
                    intent.putExtra("device", d);
                    startActivity(intent);
                } else {
                    showShortToast(getString(R.string.service_long_socket_breaked));
//                connectSocketDialog();
                }

            } else {
                showShortToast(getString(R.string.wifi_device_offline));
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
        if (resultCode == RESULT_OK) {
            if (requestCode == LINK_DEVICE) {
                Bundle b = data.getExtras();
                int changed = b.getInt("ret", 0);
                Lg.i("hjq", "changed = " + changed);
                if (changed == 1) {
                    mListData.clear();
                    ThreadPoolManager.getInstance().addTask(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            String result = HttpUtil.post(HttpUtil.URL_GETWIFIDEVICELIST, new BasicNameValuePair("name", "qin"));
                            Log.e(TAG, "result = " + result);

                            Message msg = new Message();
                            msg.obj = result;
                            msg.what = MSG_GETLIST;
                            mHandler.sendMessage(msg);
                        }
                    });
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onListItemLongClick(View view, int position) {
        // Toast.makeText(DeviceListActivity.this, "onItemLongClick   position--->" + position, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onListItemLongClick   " + position);
    }

    @Override
    public void onDragViewStart(int position) {
        // Toast.makeText(DeviceListActivity.this, "onDragViewStart   position--->" + position, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onDragViewStart   " + position);
    }

    @Override
    public void onDragViewMoving(int position) {
//        Toast.makeText(DemoActivity.this, "onDragViewMoving   position--->" + position, Toast.LENGTH_SHORT).show();
        Log.i("yuyidong", "onDragViewMoving   " + position);
    }

    @Override
    public void onDragViewDown(int position) {
        //Toast.makeText(DeviceListActivity.this, "onDragViewDown   position--->" + position, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onDragViewDown   " + position);
    }

    @Override
    public void onListItemClick(View v, int position) {
        // Toast.makeText(DeviceListActivity.this, "onItemClick   position--->" + position, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onListItemClick   " + position);
        if (position < 0) {
            return;
        }
    }

    @Override
    public void onSlideOpen(View view, View parentView, int position, int direction) {
        //   Toast.makeText(DeviceListActivity.this, "onSlideOpen   position--->" + position + "  direction--->" + direction, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onSlideOpen   " + position);
    }

    @Override
    public void onSlideClose(View view, View parentView, int position, int direction) {
        //     Toast.makeText(DeviceListActivity.this, "onSlideClose   position--->" + position + "  direction--->" + direction, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onSlideClose   " + position);
    }

    @Override
    public int onMenuItemClick(View v, int itemPosition, int buttonPosition, int direction) {
        Log.i(TAG, "onMenuItemClick   " + itemPosition + "   " + buttonPosition + "   " + direction);
        switch (direction) {
            case MenuItem.DIRECTION_LEFT:
                switch (buttonPosition) {
                    case 0:
                        return Menu.ITEM_NOTHING;
                    case 1:
                        return Menu.ITEM_SCROLL_BACK;
                }
                break;

            case MenuItem.DIRECTION_RIGHT:
                switch (buttonPosition) {
                    case 0: {
                        final WifiDevice d = mListData.get(itemPosition);
//                        try {
//                            MyApplication.getInstance().mService.disconnect(d.getAddress());
//                        } catch (RemoteException e) {
//                            e.printStackTrace();
//                        }
//                        mDeviceDao.deleteById(d.getAddress());
//                        mDeviceListAdapter.updateDataSet(itemPosition - mDeviceList.getHeaderViewsCount());
                        final int postion = itemPosition;
                        AlertDialog.Builder builder = new AlertDialog.Builder(DeviceListActivity.this);
                        builder.setMessage(R.string.str_unlink_device);
                        builder.setTitle(R.string.str_prompt);
                        builder.setPositiveButton(R.string.system_sure, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ThreadPoolManager.getInstance().addTask(new Runnable() {
                                    @Override
                                    public void run() {
                                        String result = HttpUtil.post(HttpUtil.URL_UNLINKWIFIDEVICE, new BasicNameValuePair("imei", d.getAddress()));
                                        Log.e(TAG, "result = " + result);

                                        Message msg = new Message();
                                        msg.obj = result;
                                        msg.what = MSG_UNLINKDEVICE;
                                        msg.arg1 = postion;
                                        mHandler.sendMessage(msg);
                                    }
                                });
                                dialog.dismiss();
                            }
                        });

                        builder.setNegativeButton(R.string.system_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                        builder.create().show();

                        return Menu.ITEM_SCROLL_BACK;
                    }

                    case 1: {
                        return Menu.ITEM_DELETE_FROM_BOTTOM_TO_TOP;
                    }
                }
        }

        return Menu.ITEM_NOTHING;
    }

    @Override
    public void onItemDelete(View view, int position) {

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
        tv_name.setText(mUser.getName());
        if (mUser.getImage() == null || "".equals(mUser.getImage())) {
            iv_photo.setImageResource(R.drawable.photo);
        } else {
            ImageLoaderUtil.displayImage(HttpUtil.SERVER + mUser.getImage_thumb(), iv_photo, this);
        }
    }
}
