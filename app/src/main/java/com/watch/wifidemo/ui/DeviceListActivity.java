package com.watch.wifidemo.ui;


import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.watch.wifidemo.R;
import com.watch.wifidemo.app.MyApplication;
import com.watch.wifidemo.dao.UserDao;
import com.watch.wifidemo.dao.WifiDeviceDao;
import com.watch.wifidemo.model.User;
import com.watch.wifidemo.model.WifiDevice;
import com.watch.wifidemo.service.WifiConnectService;
import com.watch.wifidemo.util.DialogUtil;
import com.watch.wifidemo.util.HttpUtil;
import com.watch.wifidemo.util.JsonUtil;
import com.watch.wifidemo.util.PreferenceUtil;
import com.watch.wifidemo.util.ThreadPoolManager;
import com.watch.wifidemo.xlistview.ItemMainLayout;
import com.watch.wifidemo.xlistview.Menu;
import com.watch.wifidemo.xlistview.MenuItem;
import com.watch.wifidemo.xlistview.SlideAndDragListView;
import com.watch.wifidemo.adapter.DeviceListAdapter;


import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 16-3-7.
 */
public class DeviceListActivity  extends BaseActivity  implements View.OnClickListener,
        AdapterView.OnItemClickListener, DeviceListAdapter.OnItemClickCallback, SlideAndDragListView.OnListItemLongClickListener,
        SlideAndDragListView.OnDragListener, SlideAndDragListView.OnSlideListener,
        SlideAndDragListView.OnListItemClickListener, SlideAndDragListView.OnMenuItemClickListener,
        SlideAndDragListView.OnItemDeleteListener{
    private static final int CHANGE_BLE_DEVICE_SETTING = 1;
    private static final int MSG_GETLIST = 1;
    private static final int MSG_GETWIFIDEVICE = 2;
    private static final int MSG_UNLINKDEVICE = 3;

    private SlideAndDragListView mDeviceList;
    private DeviceListAdapter mDeviceListAdapter;
    boolean mScanningStopped;
    ArrayList<WifiDevice> mListData;

    SharedPreferences mSharedPreferences;

    private final String TAG = "hjq";
    private Menu mMenu;
    private Handler myHandler;
    HandlerThread mHandlerThread;
    private IService mService;
    WifiDeviceDao mDeviceDao;

    Button linkbtn;
    Button connectbtn;

    boolean mConnected;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            String result = msg.obj.toString();
            closeLoadingDialog();
            Log.e(TAG, result);

            if (result.matches("Connection to .* refused") || result.matches("Connect to.*timed out")) {
                showLongToast(getString(R.string.network_error));
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "postDelayed2");

                        DialogUtil.showDialog(DeviceListActivity.this, getString(R.string.str_network_error),
                                getString(R.string.str_network_prompt),
                                getString(R.string.system_sure),
                                null,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        finish();
                                    }
                                }, null, true);
                    }
                }, 1000);
                return;
            }

            switch (msg.what) {
                case MSG_GETLIST: {
                    try {
                        JSONObject json = new JSONObject(result);
                        if (JsonUtil.getInt(json, JsonUtil.CODE) != 1) {
                            showLongToast(JsonUtil.getStr(json, JsonUtil.ERRORCN));
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
                        if (JsonUtil.getInt(json, JsonUtil.CODE) != 1) {
                            showLongToast(JsonUtil.getStr(json, JsonUtil.ERRORCN));
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
                        if (JsonUtil.getInt(json, JsonUtil.CODE) != 1) {
                            showLongToast(JsonUtil.getStr(json, JsonUtil.ERRORCN));
                        } else {
                            mDeviceListAdapter.updateDataSet(postion - mDeviceList.getHeaderViewsCount());
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
    private int LINK_DEVICE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devicelist);

        linkbtn = (Button) findViewById(R.id.btn_link);
        connectbtn = (Button) findViewById(R.id.btn_connect);

        linkbtn.setOnClickListener(this);
        connectbtn.setOnClickListener(this);

        mDeviceDao = new WifiDeviceDao(this);
        mSharedPreferences = getSharedPreferences("watch_app_preference", 0);

        mDeviceList = (SlideAndDragListView)findViewById(R.id.devicelist);
        mDeviceList.setOnItemClickListener(DeviceListActivity.this);

        initMenu();
        initUiAndListener();
        fillListData();

        Intent i = new Intent(this, WifiConnectService.class);
        getApplicationContext().bindService(i, mConnection, BIND_AUTO_CREATE);

        showLoadingDialog(getResources().getString(R.string.waiting));

        mHandlerThread = new HandlerThread("torchThread");
        mHandlerThread.start();
        myHandler = new Handler(mHandlerThread.getLooper());

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //     showLoadingDialog();
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
        }, 500);
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


    @Override
    protected void onDialogCancel() {
        super.onDialogCancel();

        Log.d("hjq", "mScanningStopped = " + mScanningStopped);
    }


    private void fillListData() {
        mListData = new ArrayList<WifiDevice>(10);

        mDeviceListAdapter = new DeviceListAdapter(this,
                mListData,
                DeviceListActivity.this);
        mDeviceList.setAdapter(mDeviceListAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (mConnection != null) {
            getApplicationContext().unbindService(mConnection);
        }

        mHandlerThread.quit();

        super.onDestroy();
    }


    @Override
    protected void onStop() {
        super.onStop();
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

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.btn_link: {
                Intent i = new Intent(DeviceListActivity.this, LinkWifiDeviceActivity.class);
                startActivityForResult(i, LINK_DEVICE);
                break;
            }

            case R.id.btn_connect: {
                //Toast.makeText(this,  R.string.prompt, Toast.LENGTH_SHORT).show();
                showLoadingDialog();
                if (mConnected) {
                    try {
                        mService.disconnect(null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        mService.connect(null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                break;
            }

            default:
                break;
        }
    }

    private ICallback.Stub mCallback = new ICallback.Stub() {
        @Override
        public void onConnect(String address) throws RemoteException {
            mConnected = true;

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDeviceListAdapter.notifyDataSetChanged();
                    connectbtn.setText(R.string.str_disconnect);
                    closeLoadingDialog();
                    Toast.makeText(DeviceListActivity.this, R.string.str_connect_success, Toast.LENGTH_SHORT).show();
                    pingWifiDevice();
                }
            });
        }

        @Override
        public void onDisconnect(String address) throws RemoteException {
            mConnected = false;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDeviceListAdapter.notifyDataSetChanged();
                    connectbtn.setText(R.string.str_connect);
                    closeLoadingDialog();
                    Toast.makeText(DeviceListActivity.this, R.string.str_disconnected, Toast.LENGTH_SHORT).show();
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
            if (cmd.equals(WifiConnectService.PING_CMD)) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setDeviceStatus(imei, WifiDevice.LOGOUT_STATUS);
                        mDeviceListAdapter.notifyDataSetChanged();
                    }
                });
            }
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
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mService = IService.Stub.asInterface(service);
            try {
                mService.registerCallback(mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(DeviceListActivity.this);
                    builder.setMessage(R.string.str_connect_first);
                    builder.setTitle(R.string.str_connect);
                    builder.setPositiveButton(R.string.system_sure, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                mService.connect(null);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                            dialog.dismiss();
                        }
                    });
                    builder.show();
                }
            });
        }
    };

    void pingWifiDevice() {
        int i;

        for (i = 0; i < mListData.size(); i++) {
            WifiDevice d = mListData.get(i);
            if (d.getStatus() == WifiDevice.LOGIN_STATUS) {
                try {
                    mService.ping(d.getAddress(), 1);
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
        Button v = (Button)view;
        WifiDevice device = mListData.get(position);
        int linkstatus = device.getLinkStatus();

        Log.d("hjq", "linkstatus = " + linkstatus);

        if (linkstatus == WifiDevice.CONNECTED) {
            try {
                mService.disconnect(device.getAddress());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            try {
                mService.connect(device.getAddress());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        mDeviceListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRightArrowClick(int position) {
        WifiDevice d = mListData.get(position);
        if (mConnected) {
            Intent intent = new Intent(DeviceListActivity.this, WifiSettingActivity.class);
            intent.putExtra("device", d);
            startActivity(intent);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(DeviceListActivity.this);
            builder.setMessage(R.string.str_connect_first);
            builder.setTitle(R.string.str_connect);
            builder.setPositiveButton(R.string.system_sure, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        mService.connect(null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    dialog.dismiss();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
             if (requestCode == LINK_DEVICE) {
                Bundle b = data.getExtras();
                int changed = b.getInt("ret", 0);

                Log.d("hjq", "changed = " + changed);
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
//                            mService.disconnect(d.getAddress());
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
}
