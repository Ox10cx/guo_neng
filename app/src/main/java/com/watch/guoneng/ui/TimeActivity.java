package com.watch.guoneng.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.watch.guoneng.R;
import com.watch.guoneng.adapter.TimeListAdapter;
import com.watch.guoneng.app.MyApplication;
import com.watch.guoneng.model.LightTimer;
import com.watch.guoneng.model.WifiDevice;
import com.watch.guoneng.service.WifiConnectService;
import com.watch.guoneng.tool.BaseTools;
import com.watch.guoneng.tool.Lg;
import com.watch.guoneng.tool.NetStatuCheck;
import com.watch.guoneng.util.HttpUtil;
import com.watch.guoneng.util.JsonUtil;
import com.watch.guoneng.util.ThreadPoolManager;
import com.watch.guoneng.util.TimeUtil;
import com.watch.guoneng.xlistview.EditDeviceDialog;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * 定时功能
 */
public class TimeActivity extends BaseActivity implements AdapterView.OnItemLongClickListener {

    private static final String TAG = "testTimeActivity";
    private static final int MSG_GETTIMELIST = 1;
    private static final int MSG_CREATETIMER = 2;
    private static final int MSG_DELETETIMER = 3;
    private static final int MSG_UPDATETIMER = 4;
    private WifiDevice device;
    private ListView listView;
    private TimeListAdapter adapter;

    //用于修改定时器   修改定时器时需要重新获得列表  以便得到最新的剩余时间
    private boolean isUpdate=false;
    private String updateId="";




    private Handler mHandler = new Handler() {
        @Override
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
                case MSG_GETTIMELIST:
                    handleTimerListRsp(result);
                    break;
                case MSG_CREATETIMER:
                    handleCreatTimerRsp(result);
                    break;
                case MSG_DELETETIMER:
                    handleDeleteTimerRsp(result);
                    break;
                case MSG_UPDATETIMER:
                    handleUpdateTimerRsp(result);
                    break;

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time);
        device = (WifiDevice) getIntent().getSerializableExtra("device");
        //虚拟设备
        //device=new WifiDevice("","","0023456787a7000");
        //device.setStatus(WifiDevice.LOGIN_STATUS);

        initView();
        bindService();
        getTimeList();
    }

    private void initView() {
        listView = (ListView) findViewById(R.id.time_list);
        adapter = new TimeListAdapter(this);
        listView.setAdapter(adapter);
        listView.setOnItemLongClickListener(this);

        findViewById(R.id.add_menu).setOnClickListener(this);
    }

    /**
     * 向服务器请求定时器列表
     */
    public void getTimeList() {
        showLoadingDialog("更新数据中");
        //向服务器获取数据
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ThreadPoolManager.getInstance().addTask(new Runnable() {
                    @Override
                    public void run() {
                        String result = HttpUtil.post(HttpUtil.URL_GETTIMERLIST, new BasicNameValuePair("imei", device.getAddress()));
                        Lg.i(TAG, "URL_GETWIFIDEVICELIST----result = " + result);
                        Message msg = new Message();
                        msg.obj = result;
                        msg.what = MSG_GETTIMELIST;
                        mHandler.sendMessage(msg);
                    }
                });
            }
        });


    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        if (view.getId() == R.id.add_menu) {
            showAddTimeDialog();
        }
    }

    //显示添加计时器对话框
    private void showAddTimeDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.add_time_dialog, null);// 得到加载view
        final RadioGroup radioGroup = (RadioGroup) v.findViewById(R.id.rb_group);
        final TimePicker timePicker = (TimePicker) v.findViewById(R.id.timePicker);
        final Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
        final EditText et_name = (EditText) v.findViewById(R.id.et_time_name);

        timePicker.setIs24HourView(true);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(v)
                .setTitle("添加定时器")
                .setCancelable(true)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int hour = timePicker.getCurrentHour();
                        int min = timePicker.getCurrentMinute();
                        int repeat_pos = spinner.getSelectedItemPosition();
                        String name = et_name.getText().toString().trim();


                        if (radioGroup.getCheckedRadioButtonId() == -1) {
                            showShortToast("请选择开启/关闭");
                            canCloseDialog(dialogInterface, false);
                            return;
                        }

                        if(name.length()>15){
                            showShortToast("名称不能超过15个字符");
                            canCloseDialog(dialogInterface, false);
                            return;
                        }


                        String action = "";
                        if (radioGroup.getCheckedRadioButtonId() == R.id.rb_open)
                            action = "on";
                        else
                            action = "off";
                        sendAddTimeRequest(hour, min, action, repeat_pos, name);
                        canCloseDialog(dialogInterface, true);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        canCloseDialog(dialogInterface, true);
                    }
                })
                .show();
    }

    //发送添加定时器请求
    private void sendAddTimeRequest(int hour, int min, final String action, final int repeat, final String name) {
        Log.d(TAG, "添加定时器：" + hour + ":" + min + "  " + action + " " + repeat + " " + name);

        if (device.getStatus() != WifiDevice.LOGIN_STATUS) {
            //非登录状态
            showShortToast(getString(R.string.unconnect_addtime_fail));
            // return;
        }

        //得到相对时间
        final long delay = TimeUtil.getRelativeTime(hour + ":" + min);


        //向服务器获取数据
        showLoadingDialog();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ThreadPoolManager.getInstance().addTask(new Runnable() {
                    @Override
                    public void run() {
                        String result = HttpUtil.post(HttpUtil.URL_CREATETIMER
                                , new BasicNameValuePair("imei", device.getAddress())
                                , new BasicNameValuePair("name", name)
                                , new BasicNameValuePair("repeat", LightTimer.getRepeatToString(repeat))
                                , new BasicNameValuePair("delay_time", delay + "")
                                , new BasicNameValuePair("action", action)
                        );
                        Lg.i(TAG, "URL_CREATETIMER----result = " + result);
                        Message msg = new Message();
                        msg.obj = result;
                        msg.what = MSG_CREATETIMER;
                        mHandler.sendMessage(msg);
                    }
                });
            }
        });

    }

    //发送更新定时器请求
    private void sendUpdateTimeRequest(final String id, int hour, int min, final String action, final int repeat, final String name) {
        Log.d(TAG, "更新定时器：" + hour + ":" + min + "  " + action + " " + repeat + " " + name);

        if (device.getStatus() != WifiDevice.LOGIN_STATUS) {
            //非登录状态
            showShortToast(getString(R.string.unconnect_addtime_fail));
            // return;
        }

        //得到相对时间
        final long delay = TimeUtil.getRelativeTime(hour + ":" + min);
        Log.d(TAG,"更新：delay"+delay);

        //向服务器获取数据
        showLoadingDialog();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ThreadPoolManager.getInstance().addTask(new Runnable() {
                    @Override
                    public void run() {
                        String result = HttpUtil.post(HttpUtil.URL_UPDATETIMER
                                , new BasicNameValuePair("imei", device.getAddress())
                                , new BasicNameValuePair("name", name)
                                , new BasicNameValuePair("repeat", LightTimer.getRepeatToString(repeat))
                                , new BasicNameValuePair("delay_time", delay + "")
                                , new BasicNameValuePair("action", action)
                                , new BasicNameValuePair("id", id)
                        );
                        Lg.i(TAG, "URL_UPDATETIMER----result = " + result);
                        Message msg = new Message();
                        msg.obj = result;
                        msg.what = MSG_UPDATETIMER;
                        mHandler.sendMessage(msg);
                    }
                });
            }
        });
    }

    /**
     * 长按 删除 修改
     *
     * @param adapterView
     * @param view
     * @param i
     * @param l
     * @return
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        final LightTimer lightTimer = (LightTimer) adapter.getItem(i);

        final EditDeviceDialog dialog = new EditDeviceDialog(this);
        dialog.show();
        dialog.setCanceledOnTouchOutside(true);
        dialog.delete_light.setText("删除定时器");
        dialog.delete_light.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (device.getStatus() != WifiDevice.LOGIN_STATUS) {
                    //非登录状态
                    showShortToast(getString(R.string.unconnect_addtime_fail));
                    // return;
                }
                //向服务器请求数据
                showLoadingDialog();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        ThreadPoolManager.getInstance().addTask(new Runnable() {
                            @Override
                            public void run() {
                                String result = HttpUtil.post(HttpUtil.URL_DELETETIMER
                                        , new BasicNameValuePair("imei", device.getAddress())
                                        , new BasicNameValuePair("id", lightTimer.getId())
                                );
                                Lg.i(TAG, "URL_DELETETIMER----result = " + result);
                                Message msg = new Message();
                                msg.obj = result;
                                msg.what = MSG_DELETETIMER;
                                mHandler.sendMessage(msg);
                            }
                        });
                    }
                });
                dialog.cancel();

            }
        });

        dialog.edit_light.setText("修改定时器");
        dialog.edit_light.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //重新获取一次列表 更新下
                isUpdate=true;
                updateId=lightTimer.getId();
                getTimeList();
                dialog.cancel();

            }
        });


        dialog.time_light.setVisibility(View.GONE);
        return true;
    }

    /**
     * 显示更新定时器对话框
     */
    private void showUpdateTimerDialog(final LightTimer lightTimer) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.add_time_dialog, null);// 得到加载view
        final RadioGroup radioGroup = (RadioGroup) v.findViewById(R.id.rb_group);
        final RadioButton open_btn = (RadioButton) v.findViewById(R.id.rb_open);
        final RadioButton close_btn = (RadioButton) v.findViewById(R.id.rb_close);
        final TimePicker timePicker = (TimePicker) v.findViewById(R.id.timePicker);
        final Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
        final EditText et_name = (EditText) v.findViewById(R.id.et_time_name);

        //设置值
        if (!lightTimer.getName().trim().isEmpty())
            et_name.setText(lightTimer.getName());
        if (lightTimer.getAction().equals("on"))
            open_btn.setChecked(true);
        else
            close_btn.setChecked(true);
        spinner.setSelection(lightTimer.getRepeat());
        Date date=new Date();
        date.setSeconds((int) (date.getSeconds()+Long.parseLong(lightTimer.getDelay())));
        Log.d(TAG,"更新原时间："+date.getHours()+" "+date.getMinutes());
        timePicker.setCurrentHour(date.getHours());
        timePicker.setCurrentMinute(date.getMinutes());

        timePicker.setIs24HourView(true);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(v)
                .setTitle("更新定时器")
                .setCancelable(true)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int hour = timePicker.getCurrentHour();
                        int min = timePicker.getCurrentMinute();
                        int repeat_pos = spinner.getSelectedItemPosition();
                        String name = et_name.getText().toString();


                        if (radioGroup.getCheckedRadioButtonId() == -1) {
                            showShortToast("请选择开启/关闭");
                            canCloseDialog(dialogInterface, false);
                            return;
                        }


                        String action = "";
                        if (radioGroup.getCheckedRadioButtonId() == R.id.rb_open)
                            action = "on";
                        else
                            action = "off";
                        sendUpdateTimeRequest(lightTimer.getId(), hour, min, action, repeat_pos, name);
                        canCloseDialog(dialogInterface, true);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        canCloseDialog(dialogInterface, true);
                    }
                })
                .show();
    }


    //  手动关闭对话框
    private void canCloseDialog(DialogInterface dialogInterface, boolean close) {
        try {
            Field field = dialogInterface.getClass().getSuperclass().getDeclaredField("mShowing");
            field.setAccessible(true);
            field.set(dialogInterface, close);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 绑定服务
     */
    private void bindService() {
        Lg.i(TAG, "blindService");
        Intent i = new Intent(this, WifiConnectService.class);
        getApplicationContext().bindService(i, mConnection, BIND_AUTO_CREATE);
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
            if (!MyApplication.getInstance().longConnected)
                showShortToast(getString(R.string.str_disconnected));

        }
    };

    private ICallback.Stub mCallback = new ICallback.Stub() {
        @Override
        public void onConnect(String address) throws RemoteException {
            Lg.i(TAG, "onConnect");
            MyApplication.getInstance().longConnected = true;

        }

        @Override
        public void onDisconnect(String address) throws RemoteException {
            Lg.i(TAG, "onDisconnect");
            MyApplication.getInstance().longConnected = false;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    closeLoadingDialog();
                    showShortToast(getString(R.string.str_disconnect));
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
        public void onNotify(final String imei, final int type) throws RemoteException {

        }

        @Override
        public void onSwitchRsp(final String imei, final String ret) throws RemoteException {

        }

        @Override
        public void onGetStatusRsp(String imei, int ret) throws RemoteException {

        }

        //待优化
        @Override
        public void onCmdTimeout(String cmd, final String imei) throws RemoteException {
            if(cmd.equals(WifiConnectService.HEART_RSP_CMD)){
                return;
            }
            Lg.i(TAG, "onCmdTimeout");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    closeLoadingDialog();
                    showShortToast(getString(R.string.control_timeout));
                }
            });

      /*      if (cmd.equals(WifiConnectService.GET_TIMER_TASK_CMD)) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        closeLoadingDialog();
                        showShortToast("操作失败");
                    }
                });
            }*/

            //有用
//            if (cmd.equals(WifiConnectService.PING_CMD)||cmd.equals(WifiConnectService.SWITCH_CMD)) {
//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        Lg.i(TAG, "onCmdTimeout_LOGOUT_STATUS");
//                        setDeviceStatus(imei, WifiDevice.LOGOUT_STATUS);
//                        mDeviceListAdapter.notifyDataSetChanged();
//                        updateWifiDeviceLoginStatus(imei, WifiDevice.LOGOUT_STATUS);
//                    }
//                });
//            }
        }

        @Override
        public void onHttpTimeout(String cmd, String imei) throws RemoteException {
            Lg.i(TAG, "onHttpTimeout");
        }

        @Override
        public void onPingRsp(final String imei, final int ret) throws RemoteException {
            Lg.i(TAG, "onPingRsp");

        }

        @Override
        public void onGetLightList(String imei, byte[] list) throws RemoteException {

        }

        @Override
        public void onSetBrightChromeRsp(String imei, int ret) throws RemoteException {
            Lg.i(TAG, "onSetBrightChromeRsp");
        }

        @Override
        public void onGetBrightChromeRsp(String imei, int index, int bright, int chrome) throws
                RemoteException {

        }

        @Override
        public void onPairLightRsp(String imei, int ret) throws RemoteException {

        }

        @Override
        public void onCreateTimerRsp(String imei, int ret) throws RemoteException {
            Log.d(TAG, "onCreateTimerRsp: " + ret);
            //添加定时器成功
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    closeLoadingDialog();
                    showShortToast("操作成功");
                    getTimeList();
                }
            });

        }

        @Override
        public void onTimerNotify(String imei, int ret) throws RemoteException {
            Log.d(TAG, "onTimerNotify:"+imei+"  ret:"+ret);
            //收到了长连接的定时器响应后  请求服务器 更新列表
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showLongToast("定时器时间到...");
                    getTimeList();
                }
            },1000);
        }
    };

    /**
     * 处理获取定时器列表 响应
     *
     * @param result
     */
    private void handleTimerListRsp(String result) {
        JSONObject json;

        try {
            json = new JSONObject(result);
            if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                BaseTools.showToastByLanguage(TimeActivity.this, json);
            } else {
                JSONArray timerlist = json.getJSONArray("list");
                ArrayList<LightTimer> lightList = new ArrayList<>();
                for (int i = 0; i < timerlist.length(); i++) {
                    JSONObject ob = timerlist.getJSONObject(i);
                    String id = ob.getString("id");
                    String imei = ob.getString("imei");
                    if (!imei.equals(device.getAddress()))
                        break;

                    long delay = Long.parseLong(ob.getString("action_time"));

                    String repeatstr = ob.getString("repeat");
                    String action = ob.getString("action");
                    String name = ob.getString("name");

                    int repeat = 0;
                    if (repeatstr.equals("none"))
                        repeat = 0;
                    else if (repeatstr.equals("day"))
                        repeat = 1;
                    else if (repeatstr.equals("week"))
                        repeat = 2;
                    else if (repeatstr.equals("month"))
                        repeat = 3;

                    LightTimer timer = new LightTimer(id, imei, String.valueOf(delay), action, name, repeat);
                    lightList.add(timer);
                }

                adapter.update(lightList);
                if(isUpdate){
                    isUpdate=false;
                    boolean isFound=false;
                    for(LightTimer timer:adapter.datas){
                        if(timer.getId().equals(updateId)){
                            //弹出定时器对话框
                            showUpdateTimerDialog(timer);
                            isFound=true;
                            break;
                        }
                    }

                    if(!isFound)
                        showShortToast("该定时器已过期...");

                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理创建定时器响应
     *
     * @param result
     */
    private void handleCreatTimerRsp(String result) {
        JSONObject json;
        try {
            json = new JSONObject(result);
            if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                BaseTools.showToastByLanguage(TimeActivity.this, json);
            } else {
                String time_id = json.getString("id");
                Log.d(TAG, "time_id:" + time_id + "创建成功");
                //showShortToast("创建定时器成功");
                //长连接通知模块获取列表
                try {
                    showLoadingDialog();
                    MyApplication.getInstance().mService.creatrTimer(device.getAddress());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                //getTimeList();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理删除定时器响应
     *
     * @param result
     */
    private void handleDeleteTimerRsp(String result) {
        JSONObject json;
        try {
            json = new JSONObject(result);
            if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                BaseTools.showToastByLanguage(TimeActivity.this, json);
            } else {
                Log.d(TAG, "删除定时器成功");
                showShortToast("删除定时器成功");

                //长连接通知模块获取列表
                try {
                    showLoadingDialog();
                    MyApplication.getInstance().mService.creatrTimer(device.getAddress());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                //getTimeList();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理更新定时器响应
     *
     * @param result
     */
    private void handleUpdateTimerRsp(String result) {
        JSONObject json;
        try {
            json = new JSONObject(result);
            if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                BaseTools.showToastByLanguage(TimeActivity.this, json);
            } else {
                Log.d(TAG, "http更新定时器成功");
                //长连接通知模块获取列表
                try {
                    showLoadingDialog();
                    MyApplication.getInstance().mService.creatrTimer(device.getAddress());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            //getTimeList();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (MyApplication.getInstance().mService != null)
                MyApplication.getInstance().mService.unregisterCallback(mCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void doBack(View v){
        finish();
    }
}
