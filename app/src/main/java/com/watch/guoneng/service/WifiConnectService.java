package com.watch.guoneng.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.watch.guoneng.BuildConfig;
import com.watch.guoneng.app.MyApplication;
import com.watch.guoneng.tool.Lg;
import com.watch.guoneng.ui.ICallback;
import com.watch.guoneng.ui.IService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 16-3-15.
 */
public class WifiConnectService extends Service {
    private static final String TAG = "WifiConnectService";
    private Handler myHandler;
    //private static final String HOST = "112.74.23.39";
    private static final String HOST = "120.25.100.110";
    //  debug  ip 120.25.100.110:7777
    private static int PORT = 9899;

    private static final String CLIENT = "QC";
    private static final String SEP = "@";
    private static final String END = "$";
    private static final String LOGIN_CMD = "010";
    private static final String LOGIN_RSP_CMD = "011";
    private static final String HEART_CMD = "014";
    private static final String HEART_RSP_CMD = "015";
    private static final String NOTIFY_CMD = "016";
    private static final String NOTIFY_RSP = "017";

    public static final String SWITCH_CMD = "018";
    private static final String SWITCH_RSP = "019";

    private static final String GET_STATUS_CMD = "020";
    private static final String GET_STATUS_RSP = "021";

    public static final String PING_CMD = "022";
    public static final String PING_RSP = "023";

    public static final String GET_LIGHT_LIST_CMD = "024";
    public static final String GET_LIGHT_LIST_RSP = "025";

    public static final String SET_BRIGHT_CHROME_CMD = "026";
    public static final String SET_BRIGHT_CHROME_RSP = "027";

    public static final String GET_BRIGHT_CHROME_CMD = "028";
    public static final String GET_BRIGHT_CHROME_RSP = "029";

    public static final String PAIR_LIGHT_CMD = "030";
    public static final String PAIR_LIGHT_RSP = "031";

    private static final String ON = "1";
    private static final String OFF = "0";

    private static final String IMEI_PATTERN = "([0-9a-fA-F]+)";

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
        Lg.i(TAG, "onBind");
        mHandlerThread = new HandlerThread("LongConnThread");
        mHandlerThread.start();
        myHandler = new Handler(mHandlerThread.getLooper());

        return mBinder;
    }

    /* cmd, imei */
    protected String[] getCommand(String s) {
        Pattern p = Pattern.compile(CLIENT + SEP + "(\\d+)" + SEP + "([A-Za-z0-9]+)" + SEP + IMEI_PATTERN + SEP + "([0-9A-Fa-f]+)\\$");
        Matcher m = p.matcher(s);

        if (m.find()) {
            return new String[]{m.group(1), m.group(3)};
        }

        return null;
    }

    void sendCmdTimeout(String[] cmds) {
        if (cmds == null) {
            return;
        }

        String cmd = cmds[0];
        String imei = cmds[1];
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

    byte charToBin(char c) {
        byte v;

        if (c >= '0' && c <= '9') {
            v = (byte) (c - (int) '0');
        } else {
            v = (byte) (10 + (int) c - (int) 'a');
        }

        return v;
    }

    int charToInt(char c) {
        int v;

        if (c >= '0' && c <= '9') {
            v = (int) (c - (int) '0');
        } else {
            v = (int) (10 + (int) c - (int) 'a');
        }

        return v;
    }

    /**
     * 16进制的两字符转换为整型
     *
     * @param c1
     * @param c2
     * @return
     */
    byte hex2bin(char c1, char c2) {    //待确认结果
        byte v;
        byte b1;
        byte b2;

        b1 = charToBin(c1);
        b2 = charToBin(c2);

        v = b1;
        v *= 16;
        v += b2;

        return v;
    }


    int hex2int(char c1, char c2) {
        int v;
        int b1;
        int b2;

        b1 = charToBin(c1);
        b2 = charToBin(c2);

        v = b1;
        v *= 16;
        v += b2;

        return v;
    }


    /*
    * 第一个字节为灯的总数
     */
    int[] getLightArray(String s) {
        byte[] v = new byte[256];
        int i;
        int j;

        s = s.toLowerCase();

        int len = s.length();
        if (len % 2 != 0) {
            Lg.i(TAG, "the length is odd, something error + '" + len + "'");
            len--;
        }

        for (i = 0, j = 0; i < len; i += 2) {
            v[j++] = hex2bin(s.charAt(i), s.charAt(i + 1));
        }

        int n = v[0];       // 灯的总数
        Lg.i(TAG, "n数组：" + n);
        int[] ret = new int[n];

        for (i = 1, j = 0; i < v.length && j < n; i++) {
            byte element = v[i];
            Lg.i(TAG, "test");
            for (int k = 7; k >= 0 && j < n; k--) {
                if ((element & (1 << k)) != 0) {
                    ret[j++] = 1;
                } else {
                    ret[j++] = 0;
                }
            }
        }
        Lg.i(TAG, "test1");
        return ret;
    }


    /*
   * 第一个字节为灯的总数
    */
    int[] getLightIntArray(String s) {
        int[] v = new int[256];
        int i;
        int j;

        s = s.toLowerCase();

        int len = s.length();
        if (len % 2 != 0) {
            Lg.i(TAG, "the length is odd, something error + '" + len + "'");
            len--;
        }

        for (i = 0, j = 0; i < len; i += 2) {
            v[j++] = hex2int(s.charAt(i), s.charAt(i + 1));
        }

        int n = v[0];       // 灯的总数
        Lg.i(TAG, "n数组：" + n);
        int[] ret = new int[n];

        for (i = 1, j = 0; i < v.length && j < n; i++) {
            int element = v[i];
            for (int k = 7; k >= 0 && j < n; k--) {
                if ((element & (1 << k)) != 0) {
                    ret[j++] = 1;
                } else {
                    ret[j++] = 0;
                }
            }
        }
        return ret;
    }


    /**
     * 获得灯泡状态列表
     *
     * @param s
     * @return
     */
    byte[] getLightByteArrayPro(String s) {
        if (s != null && s.trim().length() != 0) {
            s = s.toLowerCase();
            int len = s.length();
            int lightLength = 0;
            if (len <= 2) {
                return null;
            } else {
                lightLength = Integer.parseInt(s.substring(0, 2), 16);  //16进制转换为10进制
            }
            byte[] ret = new byte[lightLength];
            Lg.i(TAG, "灯泡个数：" + lightLength);
            //16进制的灯状态
            String lightStatus = s.substring(2, s.length());
//            Lg.i(TAG, "灯泡status：" + lightStatus);
//            Lg.i(TAG, "灯泡status的长度：" + lightStatus.length());
            for (int i = 0, j = 0; i < lightStatus.length() && j < lightLength; i++, j = j + 2) {
                ret[j] = (byte) ((charToBin(lightStatus.charAt(i)) >> 2) & 0x03);
//                Lg.i(TAG, "j-->" + j + "   " + ret[j]);
                if (j + 1 == lightLength) {
                    break;
                }
                ret[j + 1] = (byte) (charToBin(lightStatus.charAt(i)) & 0x03);
//                Lg.i(TAG, "j+1-->" + (j + 1) + "  " + ret[j + 1]);
            }
            return ret;
        } else {
            return null;
        }
    }


    /**
     * 将字符串转换成二进制字符串
     */
    private String StrToBinstr(String str) {
        String result = "";
        for (int i = 0; i < str.length(); i++) {
            //补全字符串
            result += Integer.toBinaryString(Integer.parseInt(str.substring(i, i + 1), 16)) + "";
        }
        return result;
    }


    /**
     * 解析16进制的字符串,长度为6
     *
     * @param s 返回的16进制的字符串
     * @return
     */
    int[] getRetValue(String s) {
        int[] ret = new int[256];
        int i;
        int j;

        s = s.toLowerCase();
        int len = s.length();
        if (len != 6) {
            Lg.i(TAG, "the length is odd, something error + '" + len + "'");
            return null;
        }
        for (i = 0, j = 0; i < len; i += 2) {
            ret[j++] = hex2bin(s.charAt(i), s.charAt(i + 1)) & 0xff;
        }

        return ret;
    }

    /**
     * 解析16进制的字符串,长度为4
     *
     * @param s 返回的16进制的字符串
     * @return
     */
    int getRetValueByLength(String s, int length) {
        s = s.toLowerCase();
        int len = s.length();
        if (len != length) {
            Lg.i(TAG, "the length is odd, something error + '" + len + "'");
            return 255;
        }
        int ret = hex2bin(s.charAt(length - 2), s.charAt(length - 1)) & 0xff;
        return ret;
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

    /**
     * 解析返回结果
     *
     * @param s
     */
    void parseResponse(String s) {
        Pattern p = Pattern.compile(CLIENT + SEP + "(\\d+)" + SEP + "([A-Za-z0-9]+)" + SEP + IMEI_PATTERN + SEP + "([0-9A-Fa-f]+)\\$");
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
                Lg.i("hjq", "token match!");
            }

            if (LOGIN_RSP_CMD.equals(cmd)) {
                Lg.i("hjq", "get login rsp cmd");
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
                Lg.i("hjq", "Get notify cmd here: token =" + token + ", imei = " + imei);

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
                Lg.i("hjq", "Get switch rsp here: token =" + token + ", imei = " + imei + ", ret = " + value);
                doWithCmdList(cmdPack, SWITCH_CMD, imei);
                synchronized (mCallbacks) {
                    int n = mCallbacks.beginBroadcast();
                    try {
                        int i;
//                        boolean ret;
//
//                        if ("1".equals(value)) {
//                            ret = true;
//                        } else {
//                            ret = false;
//                        }

                        for (i = 0; i < n; i++) {
                            mCallbacks.getBroadcastItem(i).onSwitchRsp(imei, value);
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "remote call exception", e);
                    }
                    mCallbacks.finishBroadcast();
                }
            }

            if (PING_RSP.equals(cmd)) {
                Lg.i("hjq", "Get ping rsp here: token =" + token + ", imei = " + imei + ", ret = " + value);
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
                Lg.i("hjq", "Get status rsp here: token =" + token + ", imei = " + imei + ", ret = " + value);
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

            if (GET_LIGHT_LIST_RSP.equals(cmd)) {
                Lg.i("hjq", "Get light list rsp here: token =" + token + ", imei = " + imei + ", ret = " + value);
                doWithCmdList(cmdPack, GET_LIGHT_LIST_CMD, imei);
//                int[] lightArray = getLightIntArray(value);
                byte[] lightArray = getLightByteArrayPro(value);
                synchronized (mCallbacks) {
                    int n = mCallbacks.beginBroadcast();
                    Lg.i("hjq", "n->>>" + n);
                    try {
                        int i;

                        for (i = 0; i < n; i++) {
                            mCallbacks.getBroadcastItem(i).onGetLightList(imei, lightArray);
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "remote call exception", e);
                    }
                    mCallbacks.finishBroadcast();
                }
            }

            if (GET_BRIGHT_CHROME_RSP.equals(cmd)) {
                Lg.i("hjq", "Get bright and chrome rsp here: token =" + token + ", imei = " + imei + ", ret = " + value);
                doWithCmdList(cmdPack, GET_BRIGHT_CHROME_CMD, imei);
                int[] retArray = getRetValue(value);
                synchronized (mCallbacks) {
                    int n = mCallbacks.beginBroadcast();
                    try {
                        int i;
                        Lg.i("hjq", "n->>>" + n);
                        for (i = 0; i < n; i++) {
                            mCallbacks.getBroadcastItem(i).onGetBrightChromeRsp(imei, retArray[0], retArray[1], retArray[2]);
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "remote call exception", e);
                    }
                    mCallbacks.finishBroadcast();
                }
            }

            if (SET_BRIGHT_CHROME_RSP.equals(cmd)) {
                Lg.i("hjq", "Set bright and chrome rsp here: token =" + token + ", imei = " + imei + ", ret = " + value);
                doWithCmdList(cmdPack, SET_BRIGHT_CHROME_CMD, imei);
                synchronized (mCallbacks) {
                    int n = mCallbacks.beginBroadcast();
                    Lg.i("hjq", "n->>>" + n);
                    try {
                        int i;

                        for (i = 0; i < n; i++) {
                            mCallbacks.getBroadcastItem(i).onSetBrightChromeRsp(imei, getRetValueByLength(value, 4));
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "remote call exception", e);
                    }
                    mCallbacks.finishBroadcast();
                }
            }

            if (PAIR_LIGHT_RSP.equals(cmd)) {
                Lg.i("hjq", "pair light device rsp here: token =" + token + ", imei = " + imei + ", ret = " + value);
                doWithCmdList(cmdPack, PAIR_LIGHT_CMD, imei);
                synchronized (mCallbacks) {
                    int n = mCallbacks.beginBroadcast();
                    try {
                        int i;

                        for (i = 0; i < n; i++) {
                            mCallbacks.getBroadcastItem(i).onPairLightRsp(imei, getRetValueByLength(value, 2));
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "remote call exception", e);
                    }
                    mCallbacks.finishBroadcast();
                }
            }
        }
    }

    /**
     * 连接service服务
     *
     * @param s
     */
    void connectServer(final String s) {
        Lg.i(TAG, "connectServer");
        InputStream in = null;

        Socket socket = mSocketMap.get(IMEI);

        try {
            if (socket == null) {
                if (BuildConfig.debug) {
                    PORT = 7777;
                    Lg.i(TAG, "BuildConfig.debug:" + BuildConfig.debug);

                } else {
                    Lg.i(TAG, "BuildConfig.debug:" + BuildConfig.debug);
                }
                socket = new Socket(HOST, PORT);
                mSocketMap.put(IMEI, socket);
            }

            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    sendCmd(LOGIN_CMD, IMEI, ON);
                }
            });

            //获取返回结果
            in = socket.getInputStream();
            byte[] b = new byte[1024];
            int len;

            while ((len = in.read(b)) != -1) {
                String line = new String(b, 0, len);
                Lg.i(TAG, "line = " + line);
                parseResponse(line);
            }
        } catch (Exception e) {
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
        final String imei = iterator.next();
        if (imei == null) {
            Lg.i(TAG, "no valid imei");
            return;
        }

        myHandler.post(new Runnable() {
            @Override
            public void run() {
                sendCmdDirect(HEART_RSP_CMD, imei, "120");
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
                if (mCmdList.size() == 0) {
                    return;
                }

                s = mCmdList.remove(0);
                sendNextPack();
            }

            Lg.i(TAG, "cmd '" + s + "' res timeout");
            String[] cmds = getCommand(s);
            if (cmds != null) {
                sendCmdTimeout(cmds);
            }
        }
    };

    protected boolean doSend(String pack) {
        Socket socket = mSocketMap.get(IMEI);
        if (socket == null) {
            Lg.i(TAG, "no socket for server");
            return false;
        }

        Lg.i(TAG, "cmd string = " + pack);
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

    boolean sendCmdDirect(String cmd, String imei, String value) {
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
        pack = sb.toString();

        return doSend(pack);
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
                Lg.i(TAG, "wait for cmd '" + mCmdList.get(0) + "' response");
                return true;
            }
        }
        return doSend(pack);
    }

    @Override
    public void onDestroy() {
        Lg.i(TAG, "onDestroy");
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
        Lg.i(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    String bin2hex(byte v) {
        StringBuffer sb = new StringBuffer();
        char[] array = "0123456789ABCDEF".toCharArray();

        int x = (v >> 4) & 0xf;
        sb.append(array[x]);
        x = v & 0xf;
        sb.append(array[x]);

        return sb.toString();
    }

    private IService.Stub mBinder = new IService.Stub() {
        @Override
        public boolean initialize() throws RemoteException {
            Lg.i(TAG, "initialize");
            return false;
        }

        @Override
        public boolean connect(final String addr) throws RemoteException {
            Lg.i(TAG, "connect");
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
            Lg.i(TAG, "disconnect");
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

        @Override
        public void getLightList(final String address) throws RemoteException {
            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    sendCmd(GET_LIGHT_LIST_CMD, address, "0");
                }
            });
        }

        @Override
        public void setBrightChrome(final String address, int index, int bright, int chrome) throws RemoteException {
            if (index < 0 || index > 255) {
                Lg.i(TAG, "index parameter error " + index);
                return;
            }
            if (bright < 0 || bright > 255 || chrome < 0 || chrome > 255) {
                Lg.i(TAG, "bright or chrome parameter error " + bright + ":" + chrome);
                return;
            }

            StringBuffer sb = new StringBuffer();
            sb.append(bin2hex((byte) index));
            sb.append(bin2hex((byte) bright));
            sb.append(bin2hex((byte) chrome));
            final String s = sb.toString();

            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    sendCmd(SET_BRIGHT_CHROME_CMD, address, s);
                }
            });
        }

        @Override
        public void getBrightChrome(final String address, int index) throws RemoteException {
            if (index < 0 || index > 255) {
                Lg.i(TAG, "index parameter error " + index);
                return;
            }
            StringBuffer sb = new StringBuffer();
            sb.append(bin2hex((byte) index));
            final String s = sb.toString();

            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    sendCmd(GET_BRIGHT_CHROME_CMD, address, s);
                }
            });
        }

        @Override
        public void pairLight(final String address, int index) throws RemoteException {
            if (index < 0 || index > 255) {
                Lg.i(TAG, "index parameter error " + index);
                return;
            }
            StringBuffer sb = new StringBuffer();
            sb.append(bin2hex((byte) index));
            final String s = sb.toString();

            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    sendCmd(PAIR_LIGHT_CMD, address, s);
                }
            });
        }


        public void unregisterCallback(ICallback cb) {
            if (cb != null) {
                mCallbacks.unregister(cb);
            }
        }

        public void registerCallback(ICallback cb) {
            if (cb != null) {
                mCallbacks.register(cb);
            }
        }
    };

}
