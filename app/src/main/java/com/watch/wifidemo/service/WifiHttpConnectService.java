package com.watch.wifidemo.service;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.watch.wifidemo.app.MyApplication;
import com.watch.wifidemo.model.ServerMsg;
import com.watch.wifidemo.tool.Lg;
import com.watch.wifidemo.ui.ICallback;
import com.watch.wifidemo.ui.IService;
import com.watch.wifidemo.util.HttpUtil;
import com.watch.wifidemo.util.JsonUtil;
import com.watch.wifidemo.util.ThreadPoolManager;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 16-7-8.
 */
public class WifiHttpConnectService extends Service {

    private static final int MSG_GETID = 0;
    private static final int MSG_SENDMSG = 1;
    private static final int MSG_GETMESSAGE = 2;

    private static final String TAG = "hjq";
//    private static final String HOST = "112.74.23.39";
//    private static final int PORT = 9899;

    private static final String CLIENT = "QC";
    private static final String SEP = "@";
    private static final String END = "$";
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

    private static final int MSG_PS = 10;

    int msgPageIndex = 0;

    final static String IMEI = "123456789012345";

    private String mToken = null;

    List<String> msgIdList = new ArrayList<>(100);

    // 命令发送队列
    List<ServerMsg> mCmdList = new ArrayList<>();
    List<ServerMsg> mSentList = new ArrayList<>();
    final Object mLock = new Object();
    final int mInterval = 20;       // 15s 内响应命令
    public final static long MS_POLL_INTERVAL = 3000; // 3S

    private RemoteCallbackList<ICallback> mCallbacks = new RemoteCallbackList<ICallback>();
    private HandlerThread myHandlerThread;
    private Handler myHandler;


    public class LocalBinder extends Binder {
        public WifiHttpConnectService getService() {
            return WifiHttpConnectService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Lg.i("hjq", "onBind");

        myHandlerThread = new HandlerThread("PollThread");
        myHandlerThread.start();
        myHandler = new Handler(myHandlerThread.getLooper());
        startPollTimer();

        return mBinder;
    }

    void checkCmdTimeout(List<ServerMsg> list, int from) {
        ServerMsg msg;
        boolean timeout = false;

        synchronized (mLock) {
            if (list.isEmpty()) {
                return;
            }
            msg = list.get(0);
            if (msg.isTimeout(from)) {
                Log.e("hjq", "msg: " + msg +  "is timeout");
                timeout = true;
                list.remove(0);
            }
        }
        if (!timeout) {
            return;
        }

        String[] array = getCommand(msg.getCmd());
        synchronized (mCallbacks) {
            int n = mCallbacks.beginBroadcast();
            try {
                int i;
                for (i = 0; i < n; i++) {
                    if (from == ServerMsg.FROM_HTTP) {
                        mCallbacks.getBroadcastItem(i).onHttpTimeout(array[0], array[1]);
                    } else {
                        mCallbacks.getBroadcastItem(i).onCmdTimeout(array[0], array[1]);
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "remote call exception", e);
            }
            mCallbacks.finishBroadcast();
        }
    }

    void startPollTimer() {
        myHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getHttpMessage(0);

                checkCmdTimeout(mCmdList, ServerMsg.FROM_HTTP);
                checkCmdTimeout(mSentList, ServerMsg.FROM_DEVICE);

                if (!mCmdList.isEmpty() || !mSentList.isEmpty() ) {
                    myHandler.postDelayed(this, MS_POLL_INTERVAL);
                }
            }
        }, MS_POLL_INTERVAL);
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
            Log.e("hjq", "the length is odd, something error + '" + len + "'");
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
            Log.e("hjq", "the length is odd, something error + '" + len + "'");
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
            Log.e("hjq", "the length is odd, something error + '" + len + "'");
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
            Log.e("hjq", "the length is odd, something error + '" + len + "'");
            return 255;
        }
        int ret = hex2bin(s.charAt(length - 2), s.charAt(length - 1)) & 0xff;
        return ret;
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

//            if (HEART_CMD.equals(cmd)) {
//                sendHeartRspCmd();
//            }

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
                Lg.i("hjq", "Get ping rsp here: token =" + token + ", imei = " + imei + ", ret = " + value);

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

    boolean sendCmd(String cmd, String imei, String value) {
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

        MyEvent event = new MyEvent(imei, sb.toString());
        ensureMsgIdList(event);

        startPollTimer();

        return true;
    }

    @Override
    public void onDestroy() {
        Lg.i(TAG, "onDestroy");

        mCallbacks.kill();
        myHandlerThread.quit();
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
            return true;
        }

        @Override
        public void disconnect(final String addr) throws RemoteException {
            Lg.i(TAG, "disconnect");
        }

        @Override
        public void enableLight(final String addr, final boolean on) throws RemoteException {
            sendCmd(SWITCH_CMD, addr, on ? ON : OFF);
        }

        @Override
        public void getLightStatus(final String addr) throws RemoteException {
            sendCmd(GET_STATUS_CMD, addr, "0");
        }

        @Override
        public void ping(final String addr, final int val) throws RemoteException {
            sendCmd(PING_CMD, addr, Integer.toString(val));
        }

        @Override
        public void getLightList(final String address) throws RemoteException {
            sendCmd(GET_LIGHT_LIST_CMD, address, "0");
        }

        @Override
        public void setBrightChrome(final String address, int index, int bright, int chrome) throws RemoteException {
            if (index < 0 || index > 255) {
                Log.e("hjq", "index parameter error " + index);
                return;
            }
            if (bright < 0 || bright > 255 || chrome < 0 || chrome > 255) {
                Log.e("hjq", "bright or chrome parameter error " + bright + ":" + chrome);
                return;
            }

            StringBuffer sb = new StringBuffer();
            sb.append(bin2hex((byte) index));
            sb.append(bin2hex((byte) bright));
            sb.append(bin2hex((byte) chrome));
            final String s = sb.toString();

            sendCmd(SET_BRIGHT_CHROME_CMD, address, s);
        }

        @Override
        public void getBrightChrome(final String address, int index) throws RemoteException {
            if (index < 0 || index > 255) {
                Log.e("hjq", "index parameter error " + index);
                return;
            }
            StringBuffer sb = new StringBuffer();
            sb.append(bin2hex((byte) index));
            final String s = sb.toString();
            sendCmd(GET_BRIGHT_CHROME_CMD, address, s);
        }

        @Override
        public void pairLight(final String address, int index) throws RemoteException {
            if (index < 0 || index > 255) {
                Log.e("hjq", "index parameter error " + index);
                return;
            }
            StringBuffer sb = new StringBuffer();
            sb.append(bin2hex((byte) index));
            final String s = sb.toString();

            sendCmd(PAIR_LIGHT_CMD, address, s);
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

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            Bundle b = (Bundle) msg.obj;
            String result = b.getString("result");

            if (result.matches("Connection to .* refused") || result.matches("Connect to.*timed out")) {
                return;
            }

            switch (msg.what) {
                case MSG_GETID: {
                    try {
                        JSONObject json = new JSONObject(result);
                        if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                            Log.e("hjq", "get msgid failed!");
                        } else {
                            JSONArray msgIds = json.getJSONArray("id");
                            for (int i = 0; i < msgIds.length(); i++) {
                                msgIdList.add(msgIds.getString(i));
                            }

                            MyEvent cb = (MyEvent) b.getSerializable("callback");
                            if (cb != null) {
                                cb.onGetMsgIds();
                            }
                        }
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                }

                case MSG_SENDMSG: {
                    ServerMsg m = (ServerMsg) b.getSerializable("msg");
                    synchronized (mLock) {
                        mSentList.add(m);
                        mCmdList.remove(m);
                    }

                    break;
                }

                case MSG_GETMESSAGE: {
                    JSONObject json = null;
                    try {
                        json = new JSONObject(result);
                        int pi = b.getInt("pi");

                        JSONArray msgList = json.getJSONArray("list");
                        for (int i = 0; i < msgList.length(); i++) {
                            JSONObject obj = new JSONObject(msgList.getString(i));

                            String msgId = obj.getString("msgId");
                            String cmd = obj.getString("message");

                            ServerMsg m = new ServerMsg(msgId, null, cmd);
                            synchronized (mLock) {
                                if (!mSentList.remove(m)) {
                                    Log.e("hjq", "can not find ele " + m);
                                }
                            }

                            parseResponse(m.getCmd());
                        }

                        // 还有未读消息,再接着读取
                        if (msgList.length() == MSG_PS) {
                            getHttpMessage(++pi);
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

    void getHttpMessage(final int pageIndex) {
        ThreadPoolManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                String result = HttpUtil.post(HttpUtil.URL_GETUNREADMSG,
                        new BasicNameValuePair("pi", Integer.toString(pageIndex)),
                        new BasicNameValuePair("ps", Integer.toString(MSG_PS)));

                Log.e("hjq", "get http message " + result);

                Bundle b = new Bundle();
                b.putInt("pi", pageIndex);
                b.putString("result", result);

                Message msg = new Message();
                msg.obj = b;
                msg.what = MSG_GETMESSAGE;
                mHandler.sendMessage(msg);
            }
        });
    }

    void sendHttpPack(final ServerMsg msg) {
        ThreadPoolManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                Log.e("hjq", "send cmd: " + msg);
                String result = HttpUtil.post(HttpUtil.URL_SENDMSG,
                        new BasicNameValuePair("imei", msg.getImei()),
                        new BasicNameValuePair("message", msg.getCmd()),
                        new BasicNameValuePair("msgId", msg.getMsgId()));

                Log.e("hjq", result);

                Bundle b = new Bundle();
                b.putSerializable("msg", msg);
                b.putString("result", result);

                Message msg = new Message();
                msg.obj = b;
                msg.what = MSG_SENDMSG;
                mHandler.sendMessage(msg);
            }
        });
    }

    void ensureMsgIdList(MyEvent cb) {
        if (msgIdList.size() <= 5) {
            getMsgIds(cb);
        } else {
            cb.onGetMsgIds();
        }
    }

    void getMsgIds(final MyEvent cb) {
        ThreadPoolManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                String result = HttpUtil.post(HttpUtil.URL_GETMSGID,
                        new BasicNameValuePair("nr", Integer.toString(32)));
                Log.e("hjq", result);

                Bundle b = new Bundle();
                b.putSerializable("callback", cb);
                b.putString("result", result);

                Message msg = new Message();
                msg.obj = b;
                msg.what = MSG_GETID;
                mHandler.sendMessage(msg);
            }
        });
    }


    interface Callback {
        void onGetMsgIds();
    }

    class MyEvent implements Callback, Serializable {
        String imei;
        String cmd;

        MyEvent(String imei, String cmd) {
            this.cmd = cmd;
            this.imei = imei;
        }

        @Override
        public void onGetMsgIds() {
            ServerMsg msg = new ServerMsg();
            String id = msgIdList.remove(0);
            msg.setMsgId(id);
            msg.setCmd(cmd);
            msg.setImei(imei);

            mCmdList.add(msg);

            int i;
            for (i = 0; i < mCmdList.size(); i++) {
                sendHttpPack(mCmdList.get(i));
            }
        }
    }
}
