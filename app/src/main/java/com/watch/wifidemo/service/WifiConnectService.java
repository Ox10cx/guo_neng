package com.watch.wifidemo.service;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.watch.wifidemo.app.MyApplication;
import com.watch.wifidemo.ui.ICallback;
import com.watch.wifidemo.ui.IService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 16-3-15.
 */
public class WifiConnectService extends Service {
    private static final String  TAG = "hjq";
    private Handler myHandler;
    private static final String HOST = "112.74.23.39";
    private static final int PORT = 9899;

    private static final String CLIENT = "@QC";
    private static final String SEP = "&";
    private static final String END = "#";
    private static final String LOGIN_CMD = "010";
    private static final String LOGIN_RSP_CMD = "011";
    private static final String HEART_CMD = "014";
    private static final String HEART_RSP_CMD = "015";
    private static final String NOTIFY_CMD = "016";
    private static final String NOTIFY_RSP = "017";

    private static final String SWITCH_CMD = "018";
    private static final String SWITCH_RSP = "019";

    private static final String GET_STATUS_CMD = "020";
    private static final String GET_STATUS_RSP = "021";

    public static final String PING_CMD = "022";
    public static final String PING_RSP = "023";

    private static final String ON = "1";
    private static final String OFF = "0";

    final static String IMEI = "123456789012345";


    private String mToken = null;

    Map<String, Socket> mSocketMap = new HashMap<String, Socket>();

    Handler mHandler = new Handler();

    // 命令发送队列
    List<String> mCmdList = new LinkedList<String>();
    final Object mLock = new Object();
    final int mInterval = 20;       // 15s 内响应命令

    private RemoteCallbackList<ICallback> mCallbacks = new RemoteCallbackList<ICallback>();
    private HandlerThread mHandlerThread;

    public class LocalBinder extends Binder {
        public WifiConnectService getService() {
            return WifiConnectService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");

        mHandlerThread = new HandlerThread("LongConnThread");
        mHandlerThread.start();
        myHandler = new Handler(mHandlerThread.getLooper());

        return mBinder;
    }

    /* cmd, imei */
    String[] getCommand(String s) {
        Pattern p = Pattern.compile(CLIENT + SEP + "(\\d+)" + SEP + "([A-Za-z0-9]+)" + SEP + "(\\d+)"  + SEP + "(\\d+)#");
        Matcher m = p.matcher(s);

        if (m.find()) {
            return new String[] { m.group(1), m.group(3)};
        }

        return null;
    }

    void sendCmdTimeout(String[] cmds) {
        if (cmds == null) {
            return;
        }

        String cmd = cmds[0];
        String imei = cmds[1];

//        if (LOGIN_CMD.equals(cmd)) {
//
//        } else if (SWITCH_CMD.equals(cmd)) {
//
//        } else if (GET_STATUS_CMD.equals(cmd)) {
//
//        }
        Log.e(TAG, "cmd '" + cmd + "', imei = " + imei + " timeout");
        synchronized (mCallbacks) {
            int n = mCallbacks.beginBroadcast();
            try {
                int i;
                for (i = 0; i < n; i++) {
                    mCallbacks.getBroadcastItem(i).onCmdTimeout(cmd, imei);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "remote call exception", e);
            }
            mCallbacks.finishBroadcast();
        }
    }

    /*
    * cmdPack 指令队列中的指令和参数
    * cmd, imei 收到的返回指令和参数
     */
    void doWithCmdList(String[] cmdPack, String cmd, String imei) {
        if (cmdPack != null && cmdPack[0].equals(cmd) && cmdPack[1].equals(imei)) {
            mHandler.removeCallbacks(mTimeoutProc);
            synchronized (mLock) {
                mCmdList.remove(0);
                sendNextPack();
            }
        }
    }

    void parseResponse(String s) {
        Pattern p = Pattern.compile(CLIENT + SEP + "(\\d+)" + SEP + "([A-Za-z0-9]+)" + SEP + "(\\d+)"  + SEP + "(\\d+)#");
        Matcher m = p.matcher(s);
        String[] cmdPack = null;

        synchronized (mLock) {
            if (mCmdList.size() > 0) {
                String str = mCmdList.get(0);
                cmdPack = getCommand(str);
            }
        }

        while (m.find()) {
            String cmd = m.group(1);
            String token = m.group(2);
            String imei = m.group(3);
            String value = m.group(4);

            if (token.equals(mToken)) {
                Log.d("hjq", "token match!");
            }

            if (LOGIN_RSP_CMD.equals(cmd)) {
                Log.d("hjq", "get login rsp cmd");
                doWithCmdList(cmdPack, LOGIN_CMD, imei);

                synchronized (mCallbacks) {
                    int n = mCallbacks.beginBroadcast();
                    try {
                        int i;
                        for (i = 0; i < n; i++) {
                            mCallbacks.getBroadcastItem(i).onConnect(imei);
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "remote call exception", e);
                    }
                    mCallbacks.finishBroadcast();
                }
            }

            if (HEART_CMD.equals(cmd)) {
                sendHeartRspCmd();
            }

            if (NOTIFY_CMD.equals(cmd)) {
                Log.d("hjq", "Get notify cmd here: token =" + token + ", imei = " + imei);

                synchronized (mCallbacks) {
                    int n = mCallbacks.beginBroadcast();
                    try {
                        int i;
                        for (i = 0; i < n; i++) {
                            mCallbacks.getBroadcastItem(i).onNotify(imei, Integer.parseInt(value));
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "remote call exception", e);
                    }
                    mCallbacks.finishBroadcast();
                }
            }

            if (SWITCH_RSP.equals(cmd)) {
                Log.d("hjq", "Get switch rsp here: token =" + token + ", imei = " + imei + ", ret = " + value);
                doWithCmdList(cmdPack, SWITCH_CMD, imei);
                synchronized (mCallbacks) {
                    int n = mCallbacks.beginBroadcast();
                    try {
                        int i;
                        boolean ret;

                        if ("1".equals(value)) {
                            ret = true;
                        } else {
                            ret = false;
                        }

                        for (i = 0; i < n; i++) {
                            mCallbacks.getBroadcastItem(i).onSwitchRsp(imei, ret);
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "remote call exception", e);
                    }
                    mCallbacks.finishBroadcast();
                }
            }

            if (PING_RSP.equals(cmd)) {
                Log.d("hjq", "Get ping rsp here: token =" + token + ", imei = " + imei + ", ret = " + value);
                doWithCmdList(cmdPack, PING_CMD, imei);

                synchronized (mCallbacks) {
                    int n = mCallbacks.beginBroadcast();
                    try {
                        int i;
                        int ret = Integer.parseInt(value);

                        for (i = 0; i < n; i++) {
                            mCallbacks.getBroadcastItem(i).onPingRsp(imei, ret);
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "remote call exception", e);
                    }
                    mCallbacks.finishBroadcast();
                }
            }

            if (GET_STATUS_RSP.equals(cmd)) {
                Log.d("hjq", "Get status rsp here: token =" + token + ", imei = " + imei + ", ret = " + value);
                doWithCmdList(cmdPack, GET_STATUS_CMD, imei);
                synchronized (mCallbacks) {
                    int n = mCallbacks.beginBroadcast();
                    try {
                        int i;
                        int ret = Integer.parseInt(value);

                        for (i = 0; i < n; i++) {
                            mCallbacks.getBroadcastItem(i).onGetStatusRsp(imei, ret);
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "remote call exception", e);
                    }
                    mCallbacks.finishBroadcast();
                }
            }
        }
    }

    void connectServer(final String s) {
        InputStream in = null;

        Socket socket = mSocketMap.get(IMEI);

        try {
            if (socket == null) {
                socket = new Socket(HOST, PORT);
                mSocketMap.put(IMEI, socket);
            }

            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    sendCmd(LOGIN_CMD, IMEI, ON);
                }
            });

            in = socket.getInputStream();
            byte[] b = new byte[1024];
            int len;

            while ((len = in.read(b)) != -1) {
                String line = new String(b, 0, len);
                Log.e("hjq", "line = " + line);
                parseResponse(line);
            }
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mSocketMap.remove(IMEI);
            }

            synchronized (mCallbacks) {
                int n = mCallbacks.beginBroadcast();
                try {
                    int i;
                    for (i = 0; i < n; i++) {
                        mCallbacks.getBroadcastItem(i).onDisconnect(IMEI);
                    }
                } catch (RemoteException re) {
                    Log.e(TAG, "remote call exception", re);
                }
                mCallbacks.finishBroadcast();
            }
        }
    }

    void sendHeartRspCmd() {
        Iterator<String> iterator = mSocketMap.keySet().iterator();
        final String imei =  iterator.next();
        if (imei == null) {
            Log.e("hjq", "no valid imei");
            return;
        }

        myHandler.post(new Runnable() {
            @Override
            public void run() {
                sendCmd(HEART_RSP_CMD, imei, "120");
            }
        });
    }

    void sendNextPack() {
        if (mCmdList.size() > 0) {
            final String pack = mCmdList.get(0);
            if (pack != null) {
                myHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        doSend(pack);
                    }
                });
            }
        }
    }

    Runnable mTimeoutProc = new Runnable() {
        @Override
        public void run() {
            String s;
            synchronized (mLock) {
                s = mCmdList.remove(0);
                sendNextPack();
            }

            Log.e("hjq", "cmd '" + s + "' res timeout");
            String[] cmds = getCommand(s);
            if (cmds != null) {
                sendCmdTimeout(cmds);
            }
        }
    };

    boolean doSend(String pack) {
        Socket socket = mSocketMap.get(IMEI);
        if (socket == null) {
            Log.e("hjq", "no socket for server");
            return false;
        }

        Log.e("hjq", "cmd string = " + pack);
        OutputStream os = null;
        mHandler.postDelayed(mTimeoutProc, mInterval * 1000);

        try {
            os = socket.getOutputStream();
            byte[] strbyte = pack.getBytes("UTF-8");
            os.write(strbyte, 0, strbyte.length);
        } catch (IOException e2) {
            e2.printStackTrace();
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return false;
        }

        return true;
    }

    boolean sendCmd(String cmd, String imei, String value) {
        String pack;

        mToken = MyApplication.getInstance().mToken;

        StringBuilder sb = new StringBuilder(CLIENT);
        sb.append(SEP);
        sb.append(cmd);
        sb.append(SEP);
        sb.append(mToken);
        sb.append(SEP);
        sb.append(imei);
        sb.append(SEP);
        if (value != null) {
            sb.append(value);
        } else {
            sb.append(OFF);
        }
        sb.append(END);

        synchronized (mLock) {
            mCmdList.add(sb.toString());
            if (mCmdList.size() == 1) {
                pack = mCmdList.get(0);
            } else {
                Log.e("hjq", "wait for cmd '" + mCmdList.get(0) + "' response");
                return true;
            }
        }
        return doSend(pack);
    }

    @Override
    public void onDestroy() {

        for (Socket s : mSocketMap.values()) {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        mCallbacks.kill();
        mHandlerThread.quit();
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {

        return super.onUnbind(intent);
    }

    private IService.Stub mBinder = new IService.Stub() {
        @Override
        public boolean initialize() throws RemoteException {
            return false;
        }

        @Override
        public boolean connect(final String addr) throws RemoteException {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connectServer(addr);
                }
            }).start();

            return true;
        }

        @Override
        public void disconnect(final String addr) throws RemoteException {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Socket s = mSocketMap.get(IMEI);
                    mSocketMap.remove(IMEI);
                    if (s != null) {
                        try {
                            s.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }

        @Override
        public void enableLight(final String addr, final boolean on) throws RemoteException {
            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    sendCmd(SWITCH_CMD, addr, on ? ON : OFF);
                }
            });
        }

        @Override
        public void getLightStatus(final String addr) throws RemoteException {
            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    sendCmd(GET_STATUS_CMD, addr, "0");
                }
            });
        }

        @Override
        public void ping(final String addr, final int val) throws RemoteException {
            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    sendCmd(PING_CMD, addr, Integer.toString(val));
                }
            });
        }

        public void unregisterCallback(ICallback cb){
            if (cb != null) {
                mCallbacks.unregister(cb);
            }
        }

        public void registerCallback(ICallback cb){
            if (cb != null) {
                mCallbacks.register(cb);
            }
        }
    };

}