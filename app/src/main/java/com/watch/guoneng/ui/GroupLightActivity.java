package com.watch.guoneng.ui;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.watch.guoneng.BuildConfig;
import com.watch.guoneng.R;
import com.watch.guoneng.adapter.ConLightAdapter;
import com.watch.guoneng.app.MyApplication;
import com.watch.guoneng.model.Light;
import com.watch.guoneng.model.WifiDevice;
import com.watch.guoneng.service.WifiConnectService;
import com.watch.guoneng.service.WifiHttpConnectService;
import com.watch.guoneng.tool.Lg;
import com.watch.guoneng.tool.NetStatuCheck;
import com.watch.guoneng.util.DialogUtil;
import com.watch.guoneng.util.HttpUtil;
import com.watch.guoneng.util.JsonUtil;
import com.watch.guoneng.util.ThreadPoolManager;
import com.watch.guoneng.xlistview.EditDeviceDialog;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Administrator on 16-3-7.
 */
public class GroupLightActivity extends BaseActivity implements View.OnClickListener,
        ExpandableListView.OnChildClickListener, ExpandableListView.OnGroupClickListener
        , ExpandableListView.OnItemLongClickListener {
    private static final String TAG = "GroupLightActivity";
    private ExpandableListView expandableListView;
    private LinkedList<String> fatherList;
    private List<LinkedList<Light>> childList;
    private WifiDevice mDevice;
    private ConLightAdapter adapter;
    private byte[] lightStatuList;
    private static final int LIGHT_LIST = 1;
    private static final int MSG_GETLIST = 2;
    private static final int MSG_ADDLIGHT = 3;
    private static final int MSG_DELETELIGHT = 4;
    private static final int MSG_EDITLIGHT = 5;
    private boolean isGetLightStatus = false;
    private boolean isGetLightList = false;
    private ImageView led_switch;
    private TextView socket_status;
    private boolean is_led_on;
    private Light addLight;
    private int addGroupIndex;
    private String imei = "";
    private int childPosition;
    private int groupPosition;
    private HashMap<Integer, Integer> lightIndex = new HashMap<>();
    private String result = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grouplight);
        expandableListView = (ExpandableListView) findViewById(R.id.scrolllistview);
        expandableListView.setOnGroupClickListener(this);
        expandableListView.setOnChildClickListener(this);
        expandableListView.setOnItemLongClickListener(this);
        mDevice = (WifiDevice) (getIntent().getSerializableExtra("device"));
        imei = mDevice.getAddress();
        led_switch = (ImageView) findViewById(R.id.led_switch);
        led_switch.setOnClickListener(this);
        socket_status = (TextView) findViewById(R.id.socket_status);
        //获取灯泡列表
        initData();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ThreadPoolManager.getInstance().addTask(new Runnable() {
                    @Override
                    public void run() {
                        String result = HttpUtil.post(HttpUtil.URL_GETLIGHTLIST, new BasicNameValuePair("imei", mDevice.getAddress()));
                        Lg.i(TAG, "result = " + result);
                        Message msg = new Message();
                        msg.obj = result;
                        msg.what = MSG_GETLIST;
                        mHandler.sendMessage(msg);
                    }
                });
            }
        }, 500);

        showLoadingDialog();

        Intent intent;
        if (BuildConfig.USE_LONG_CONNECTION.equals("1")) {
            intent = new Intent(this, WifiConnectService.class);
        } else {
            intent = new Intent(this, WifiHttpConnectService.class);
        }

        bindService(intent, mConnection, BIND_AUTO_CREATE);
        adapter = new ConLightAdapter(fatherList, childList, GroupLightActivity.this, expandableListView);
        expandableListView.setAdapter(adapter);
        //设置每一组默认展开
        for (int i = 0; i < adapter.getGroupCount(); i++) {
            expandableListView.expandGroup(i);
        }
    }

    public void initData() {
        fatherList = new LinkedList<String>();
        fatherList.add(getResources().getString(R.string.use_light));

//        fatherList.add(getResources().getString(R.string.useless_light));
//        fatherList.add(getResources().getString(R.string.other_light));

        childList = new LinkedList<LinkedList<Light>>();
    }


    @Override
    protected void onResume() {
        super.onResume();
//        if (is_led_on) {
//            led_switch.setImageResource(R.drawable.tbtn_on);
//        } else {
//            led_switch.setImageResource(R.drawable.tbtn_off);
//        }
    }

    @Override
    public boolean onChildClick(ExpandableListView expandableListView, View view, int gid, int cid, long l) {
        Light light = childList.get(gid).get(cid);
        if (light.getLightStatu() == 3) {
            Intent intent = new Intent(this, ControlLightActivity.class);
            intent.putExtra("light", light);
            intent.putExtra("device", mDevice);
            intent.putExtra("index", Integer.valueOf(light.getId()));
            startActivity(intent);
        } else {
            showShortToast(getResources().getString(R.string.light_offline_remind));
        }
        return false;
    }


    private ICallback.Stub mCallback = new ICallback.Stub() {
        @Override
        public void onConnect(String address) throws RemoteException {
            Lg.i(TAG, TAG + "onConnect");
        }

        @Override
        public void onDisconnect(String address) throws RemoteException {
            Lg.i(TAG, TAG + "onDisconnect");
            closeLoadingDialog();
            //断网重连
            if (NetStatuCheck.checkGPRSState(GroupLightActivity.this).equals("unavailable") &&
                    MyApplication.getInstance().isSocketConnectBreak) {
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        if (getTopActivity() != null) {
                            if (getTopActivity().contains("GroupLightActivity")) {
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
        public void onNotify(String imei, final int ret) throws RemoteException {
            Lg.i(TAG, "onNotify_type" + ret);
            MyApplication.getInstance().mService.getLightStatus(mDevice.getAddress());
//            mHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    closeLoadingDialog();
//                    showLongToast("onNotify->ret:" + ret);
//                    if (ret == 1) {
//                        setSocketSwitchOn();
//                    } else {
//                        setSocketSwitchOff();
//                    }
//                }
//            });
        }

        @Override
        public void onSwitchRsp(String imei, final boolean ret) throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    closeLoadingDialog();
                }
            });
        }

        @Override
        public void onGetStatusRsp(String imei, final int ret) throws RemoteException {
            Lg.i(TAG, "onGetStatusRsp:" + ret);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    closeLoadingDialog();
                    if (ret == 1) {
                        setSocketSwitchOn();
                    } else {
                        setSocketSwitchOff();
                    }
                }
            });
        }

        @Override
        public void onCmdTimeout(String cmd, String imei) throws RemoteException {
            Lg.i(TAG, "onCmdTimeout");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    closeLoadingDialog();
                    showShortToast(getResources().getString(R.string.connect_time_out));
                }
            });

        }

        @Override
        public void onHttpTimeout(String cmd, String imei) throws RemoteException {

        }

        @Override
        public void onPingRsp(String imei, int ret) throws RemoteException {

        }

        @Override
        public void onGetLightList(String imei, final byte[] list) throws RemoteException {
            Log.i(TAG, "onGetLightList");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (list != null) {
//                        for (int i = 0; i < list.length; i++) {
//                            Lg.i(TAG, "onGetLightList--->" + i + "  值：" + list[i]);
//                        }
                        Message msg = new Message();
                        msg.obj = list;
                        msg.what = LIGHT_LIST;
                        mHandler.sendMessage(msg);
                    } else {
                        showShortToast(getString(R.string.device_link_no_light));
                    }
                }
            });

        }

        @Override
        public void onSetBrightChromeRsp(String imei, final int ret) throws RemoteException {
        }

        @Override
        public void onGetBrightChromeRsp(String imei, int index, final int bright, final int chrome) throws RemoteException {
        }

        @Override
        public void onPairLightRsp(final String imei, int ret) throws RemoteException {
            final int res = ret;
            Lg.i(TAG, "onPairLightRsp:" + ret);
            mHandler.post(new Runnable() {
                              @Override
                              public void run() {
                                  closeLoadingDialog();
                                  if (res != 0) {
                                      showShortToast(getResources().getString(R.string.add_light_error));
                                  } else {
                                      if (addLight != null) {
                                          if (childList.size() == 0) {
                                              LinkedList<Light> temlist = new LinkedList<Light>();
                                              childList.add(temlist);
                                          }

                                          //主线程太耗时
                                          //                            for (int i = 0; i < childList.get(addGroupIndex).size(); i++) {
                                          //                                if (Integer.valueOf(addLight.getId()).equals(childList.get(addGroupIndex).get(i).getId())) {
                                          //                                    childList.get(addGroupIndex).remove(i);
                                          //                                    childList.get(addGroupIndex).add(i, addLight);
                                          //                                    break;
                                          //                                }
                                          //                                if (i == childList.get(addGroupIndex).size() - 1) {
                                          //                                    childList.get(addGroupIndex).add(addLight);
                                          //                                }
                                          //                            }

                                          if (lightIndex.containsKey(Integer.valueOf(addLight.getId()))) {
                                              Lg.i(TAG, "删除灯：" + lightIndex.get(Integer.valueOf(addLight.getId())));
                                              childList.get(addGroupIndex).remove((lightIndex.get(Integer.valueOf(addLight.getId()))).intValue());
//                                              for (int i = 0; i < childList.get(addGroupIndex).size(); i++) {
//                                                  Lg.i(TAG, "i:" + childList.get(addGroupIndex).get(i).getId());
//                                              }
                                              childList.get(addGroupIndex).add((lightIndex.get(Integer.valueOf(addLight.getId()))).intValue(), addLight);
//                                              for (int i = 0; i < childList.get(addGroupIndex).size(); i++) {
//                                                  Lg.i(TAG, "i:" + childList.get(addGroupIndex).get(i).getId());
//                                              }
//                                              adapter.notifyDataSetChanged();
                                              ThreadPoolManager.getInstance().addTask(new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      String result = HttpUtil.post(HttpUtil.URL_UPDATELIGHT, new BasicNameValuePair("imei", imei),
                                                              new BasicNameValuePair("index", addLight.getId()),
                                                              new BasicNameValuePair("name", addLight.getName()));
                                                      Lg.i(TAG, "result = " + result);
                                                      Message msg = new Message();
                                                      msg.obj = result;
                                                      msg.what = MSG_ADDLIGHT;
                                                      mHandler.sendMessage(msg);
                                                  }
                                              });
                                          } else {
                                              lightIndex.put(Integer.valueOf(addLight.getId()), childList.get(addGroupIndex).size());
                                              childList.get(addGroupIndex).add(addLight);
                                              //保存灯泡数据到后台
                                              ThreadPoolManager.getInstance().addTask(new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      String result = HttpUtil.post(HttpUtil.URL_ADDLIGHT, new BasicNameValuePair("imei", imei),
                                                              new BasicNameValuePair("index", addLight.getId()),
                                                              new BasicNameValuePair("name", addLight.getName()));
                                                      Lg.i(TAG, "result = " + result);
                                                      Message msg = new Message();
                                                      msg.obj = result;
                                                      msg.what = MSG_ADDLIGHT;
                                                      mHandler.sendMessage(msg);
                                                  }
                                              });
                                          }

                                      }
                                  }
                              }
                          }
            );
        }
    };

    private void setSocketSwitchOff() {
        is_led_on = false;
        led_switch.setImageResource(R.drawable.socket_status_off);
        socket_status.setTextColor(getResources().getColor(R.color.TextColorBlack));
        socket_status.setText(getString(R.string.socket_status_off));
    }

    private void setSocketSwitchOn() {
        is_led_on = true;
        led_switch.setImageResource(R.drawable.socket_status_on);
        socket_status.setTextColor(getResources().getColor(R.color.switch_on));
        socket_status.setText(getString(R.string.socket_status_on));
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            MyApplication.getInstance().mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyApplication.getInstance().mService = IService.Stub.asInterface(service);
            try {
                Lg.i(TAG, " MyApplication.getInstance().mService:" + MyApplication.getInstance().mService);
                MyApplication.getInstance().mService.registerCallback(mCallback);
//                MyApplication.getInstance().mService.getLightList(mDevice.getAddress());
                MyApplication.getInstance().mService.getLightStatus(mDevice.getAddress());
            } catch (RemoteException e) {
                Lg.i(TAG, "" + e);
            }
        }
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            JSONObject json = null;
//            synchronized (msg) {
            switch (msg.what) {
                case LIGHT_LIST:
                    Lg.i(TAG, "LIGHT_LIST");
                    isGetLightList = true;
                    lightStatuList = (byte[]) msg.obj;
                    if (isGetLightList && isGetLightStatus) {
                        setListData(result);
                    }
                    break;
                case MSG_GETLIST:
                    isGetLightStatus = true;
                    result = (String) msg.obj;
//                    Lg.i(TAG, "MSG_GETLIST_result:" + result);
                    if (result.matches("Connection to .* refused") || result.matches("Connect to.*timed out")) {
                        showLongToast(getString(R.string.network_error));
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Lg.i(TAG, "MSG_GETLIST");
                                DialogUtil.showDialog(GroupLightActivity.this, getString(R.string.str_network_error),
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
                    } else {
                        if (isGetLightList && isGetLightStatus) {
                            setListData(result);
                        }
                    }
                    break;
                case MSG_ADDLIGHT:
                    result = (String) msg.obj;
                    try {
                        json = new JSONObject(result);
                        if (JsonUtil.getInt(json, JsonUtil.CODE) != 1) {
                            showLongToast(getResources().getString(R.string.add_light_error));
                        } else {
                            adapter.notifyDataSetChanged();
                            showShortToast(getResources().getString(R.string.add_light_ok));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case MSG_DELETELIGHT:
                    result = (String) msg.obj;
                    try {
                        json = new JSONObject(result);
                        if (JsonUtil.getInt(json, JsonUtil.CODE) != 1) {
                            showLongToast(getResources().getString(R.string.delete_light_error));
                        } else {
                            childList.get(groupPosition).remove(childPosition);
                            adapter.notifyDataSetChanged();
                            showShortToast(getResources().getString(R.string.delete_light_ok));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case MSG_EDITLIGHT:
                    result = (String) msg.getData().get("result");
                    try {
                        json = new JSONObject(result);
                        if (JsonUtil.getInt(json, JsonUtil.CODE) != 1) {
                            showLongToast(getResources().getString(R.string.edit_light_error));
                        } else {
                            childList.get(groupPosition).get(childPosition).setName((String) msg.getData().get("light_name"));
                            adapter.notifyDataSetChanged();
                            showShortToast(getResources().getString(R.string.edit_light_ok));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
//        }
    };

    /**
     * 设置listView的数据
     *
     * @param result
     */
    public void setListData(String result) {
//        Lg.i(TAG, "setListData");
//        Lg.i(TAG, "result:" + result);
        isGetLightStatus = false;
        isGetLightList = false;
        if (result != null && (result.trim()).length() != 0) {
            Lg.i(TAG, "result != null");
            try {
                JSONObject json = new JSONObject(result);
                if (JsonUtil.getInt(json, JsonUtil.CODE) != 1) {
                    showLongToast(JsonUtil.getStr(json, JsonUtil.ERRORCN));
                } else {
                    JSONArray lightsList = json.getJSONArray("lights");
                    LinkedList<Light> temlist = new LinkedList<>();
                    int index = 1;
                    String name = "";
                    Lg.i(TAG, "lightStatuList.length-->>" + lightStatuList.length);
                    for (int i = 0; i < lightsList.length(); i++) {
                        JSONObject ob = lightsList.getJSONObject(i);
                        if (ob.has("index")) {
                            index = ob.getInt("index");
                        }
                        if (ob.has("name")) {
                            name = ob.getString("name");
                        } else {
                            name = "unkown";
                        }
                        Light light = new Light();
                        light.setId("" + index);
                        Lg.i(TAG, "light_index-->>" + index);
                        if (index <= lightStatuList.length) {
                            light.setLightStatu(lightStatuList[index - 1]);
                            light.setName(name);
                        }
                        //在线或者离线状态
                        if (light.getLightStatu() != 0) {
                            lightIndex.put(index, temlist.size());
                            Lg.i(TAG, "lightIndex.put(index, temlist.size()->>" + index + "  " + temlist.size());
                            temlist.add(light);
                        }
                    }
                    childList.add(temlist);
                }
                adapter.notifyDataSetChanged();
            } catch (JSONException e) {
                Lg.i(TAG, "" + e.toString());
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void onDestroy() {
        Lg.i(TAG, "onDestroy");
        if (mConnection != null) {
            try {
//                Lg.i(TAG, "onDestroy->>unregisterCallback");
                MyApplication.getInstance().mService.unregisterCallback(mCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(mConnection);
//        Lg.i(TAG, "onDestroy->>unbindService");
        super.onDestroy();
    }

    @Override
    public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
        adapter.notifyDataSetChanged();
        return false;
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.led_switch:
                showLoadingDialog(getResources().getString(R.string.cmd_sending));
                try {
                    if (is_led_on) {
                        setSocketSwitchOff();
                        MyApplication.getInstance().mService.enableLight(mDevice.getAddress(), false);
                    } else {
                        setSocketSwitchOn();
                        MyApplication.getInstance().mService.enableLight(mDevice.getAddress(), true);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int flatPos, long id) {
        if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            long packedPos = ((ExpandableListView) adapterView).getExpandableListPosition(flatPos);
            groupPosition = ExpandableListView.getPackedPositionGroup(packedPos);
            childPosition = ExpandableListView.getPackedPositionChild(packedPos);
            Lg.i(TAG, "onItemLongClick->>" + childPosition);
            //添加删除和修改
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
                            String result = HttpUtil.post(HttpUtil.URL_DELETELight, new BasicNameValuePair("imei", imei),
                                    new BasicNameValuePair("index", (childList.get(groupPosition)).get(childPosition).getId()));
                            Lg.i(TAG, "result = " + result);
                            Message msg = new Message();
                            msg.obj = result;
                            msg.what = MSG_DELETELIGHT;
                            mHandler.sendMessage(msg);
                        }
                    });
                }
            });

            dialog.edit_light.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    dialog.cancel();
                    Intent intent = new Intent(GroupLightActivity.this, AddLightActivity.class);
                    intent.putExtra("type", "editlight");
                    intent.putExtra("name", childList.get(groupPosition).get(childPosition).getName());
                    startActivityForResult(intent, 212);
                }
            });
            return true;
        }
        return false;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            final String light_name = data.getStringExtra("light_name");
            if (resultCode == 123) {
                addGroupIndex = data.getIntExtra("group_index", 0);
                if (light_name != null && light_name.length() != 0) {
                    try {
                        Lg.i(TAG, "onActivityResult");
                        //待测试
//                        if (childList.size() == 0) {
//                             MyApplication.getInstance().mService.pairLight(mDevice.getAddress(), 1);
//                        } else {
                        Lg.i(TAG, "pair_index:" + Integer.valueOf(data.getStringExtra("light_no")));
                        MyApplication.getInstance().mService.pairLight(mDevice.getAddress(), Integer.valueOf(data.getStringExtra("light_no")));
//                        }
                        showLoadingDialog(getResources().getString(R.string.cmd_sending));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    Light light = new Light();
                    light.setName(light_name);
                    light.setId(data.getStringExtra("light_no"));
                    light.setLightStatu((byte) 3);
                    addLight = light;
                }
            } else if (resultCode == 222) {
                ThreadPoolManager.getInstance().addTask(new Runnable() {
                    @Override
                    public void run() {
                        String result = HttpUtil.post(HttpUtil.URL_UPDATELIGHT, new BasicNameValuePair("imei", imei),
                                new BasicNameValuePair("index", (childList.get(groupPosition)).get(childPosition).getId()),
                                new BasicNameValuePair("name", light_name));
                        Lg.i(TAG, "result = " + result);
                        Message msg = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putString("result", result);
                        bundle.putString("light_name", light_name);
                        msg.what = MSG_EDITLIGHT;
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                    }
                });
            }
        }
    }

}
