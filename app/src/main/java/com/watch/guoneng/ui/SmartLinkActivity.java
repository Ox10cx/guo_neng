package com.watch.guoneng.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.watch.guoneng.R;
import com.watch.guoneng.tool.BaseTools;
import com.watch.guoneng.tool.Lg;
import com.watch.guoneng.util.HttpUtil;
import com.watch.guoneng.util.JsonUtil;
import com.watch.guoneng.util.ThreadPoolManager;
import com.watch.guoneng.xlistview.DonutProgress;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Administrator on 16-3-7.
 */
public class SmartLinkActivity extends BaseActivity {
    private static final String TAG = "SmartLinkActivity";
    Thread sendUdpThread;
    Thread tcpThread;
    boolean exitProcess = false;
    InetAddress address;
    Random rand = new Random();
    StringBuffer ipData;
    int cmdNumber = 3;
    StringBuffer[] packetData = new StringBuffer[cmdNumber];
    StringBuffer[] seqData = new StringBuffer[cmdNumber];
    int testDataRetryNum = 150;
    String retryNumber[] = {"10", "10", "5"};
    String magicNumber = "iot";
    String rc4Key = "Key";
    String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    int[] stable = new int[256];
    int[] tempPacket = new int[256];
    int[] tempSeq = new int[256];
    int[] sonkey = new int[256];
    String PASS;
    ServerSocket serv;

    private EditText ssidEdit;
    private EditText passEdit;
    private DonutProgress broadCastButton;
    private ImageView iv_back;
    private Button iv_scanner;
    private ImageView pwd_show_hide;
    private boolean isSendFinished = false;
    private Timer timer;
    private static final int CONFIGUREOK = 10;
    private static final int CONFIGUREFAIL = 11;
    private static final int LINKWIFI = 12;
    private int ret = 0;
    private boolean isHidden = true;

    /**
     * 扫描跳转Activity RequestCode
     */
    public static final int REQUEST_CODE = 111;
//    private String scannerMac = "";

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
//    private GoogleApiClient client;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_device);
        initView();
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            ssidEdit.setText(whetherToRemoveTheDoubleQuotationMarks(wifiInfo.getSSID()));
        }
        SharedPreferences preferences = getSharedPreferences("preFile", 0);
        PASS = preferences.getString("pass", "");
        if (!(PASS.equals(""))) {
            passEdit.setText(PASS);
        }

        savePhoneIp(wifiInfo.getIpAddress());
        serv = null;
        sendUdpThread = null;
        tcpThread = null;

        //test virtual mac

//        ThreadPoolManager.getInstance().addTask(new Runnable() {
//            @Override
//            public void run() {
//                String result = HttpUtil.post(HttpUtil.URL_LINKWIFIDEVICE,
//                        new BasicNameValuePair(JsonUtil.IMEI, "0023456787a7000"));
//                Lg.i(TAG, "HttpUtil.URL_LINKWIFIDEVICE->>>>>" + result);
//                Message msg = new Message();
//                msg.obj = result;
//                msg.arg1 = LINKWIFI;
//                handler.sendMessage(msg);
//            }
//        });

    }

    private void initView() {
        ssidEdit = (EditText) findViewById(R.id.ssidText);
        passEdit = (EditText) findViewById(R.id.passText);
        broadCastButton = (DonutProgress) findViewById(R.id.button);
        broadCastButton.setOnClickListener(this);
        iv_back = (ImageView) findViewById(R.id.iv_back);
        iv_back.setOnClickListener(this);
        iv_scanner = (Button) findViewById(R.id.iv_scanner);
//        iv_scanner.setOnClickListener(this);
        pwd_show_hide = (ImageView) findViewById(R.id.pwd_show_hide);
        pwd_show_hide.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
        savePassData();
        exitAPP();
    }

    void exitAPP() {
        initUI();
        exitThread();
    }

    void initUI() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        ssidEdit.setText(whetherToRemoveTheDoubleQuotationMarks(wifiInfo.getSSID()));
        SharedPreferences preferences = getSharedPreferences("preFile", 0);
        PASS = preferences.getString("pass", "");
        if (!(PASS.equals(""))) {
            passEdit.setText(PASS);
        }
        savePhoneIp(wifiInfo.getIpAddress());
    }

  /*  @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "SmartLink Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.watch.wifidemo.ui/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "SmartLink Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.watch.wifidemo.ui/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }*/


//    View.OnClickListener buttonSmartLink = new View.OnClickListener() {
//        @Override
//        public void onClick(View arg0) {
//            if (broadCastButton.getText() == getString(R.string.send)) {
//                broadCastButton.setText(getString(R.string.close));
//                broadCastButton.setBackgroundColor(Color.RED);
//                enableThread();
//            } else {
//                broadCastButton.setText(getString(R.string.send));
//                broadCastButton.setBackgroundColor(Color.GRAY);
//                exitThread();
//            }
//        }
//    };

    public class sendUdpThread extends Thread {

        public void run() {
            Lg.i(TAG, "sendUdpThread->>>run()");
            KSA();
            PRGA();
            while (!exitProcess) {
                Lg.i(TAG, "sendUdpThread->>>run()--->!exitProcess");
                SendbroadCast();
            }
        }
    }

    public class tcpReceThread extends Thread {
        private Socket socket = null;
        private InputStream in;
        private OutputStream out;
        private DataOutputStream streamWriter;
        private InputStreamReader streamReader;

        public tcpReceThread(Socket socket) {
            super("tcpReceThread");
            this.socket = socket;
            start();
            Lg.i(TAG, "tcpReceThread.start()");
        }

        public void run() {
            Lg.i(TAG, "tcpReceThread->>>run()");
            try {
                char[] tmpbuffer = new char[1024];
                in = socket.getInputStream();
                out = socket.getOutputStream();
                streamWriter = new DataOutputStream(out);
                streamReader = new InputStreamReader(in, "UTF-8");
                int len = streamReader.read(tmpbuffer, 0, 1024);
                Lg.i(TAG, "tcpReceThread->>>run()-->>>len----->>" + len);
                if (len > 0) {
                    char[] buffer = Arrays.copyOf(tmpbuffer, len);
//                    String macMessage = socket.getInetAddress().getHostAddress() + "/" + new String(buffer);
                    String macMessage = new String(buffer);
                    Lg.i(TAG, "message->>>" + macMessage);
                    Message message = new Message();
                    message.arg1 = CONFIGUREOK;
                    message.obj = macMessage;
                    handler.sendMessage(message);
                }
                String message = "ok";
                byte[] midbytes = message.getBytes("UTF8");
                streamWriter.write(midbytes, 0, midbytes.length);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                in.close();
                out.close();
                streamWriter.close();
                streamReader.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class tcpThread extends Thread {
        int port = 8209;
        Socket s1 = null;

        public void run() {
            Lg.i(TAG, "tcpThread");
            while (!exitProcess) {
                try {
                    serv = new ServerSocket(port, 10);
                } catch (Exception se) {
                    Lg.i(TAG, "Init ServerSocker Error!!");
                }

                try {
                    s1 = serv.accept();
                    Lg.i(TAG, "new tcpReceThread(s1)");
                    tcpReceThread mt = new tcpReceThread(s1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (serv != null) {
                    serv.close();
                    serv = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String whetherToRemoveTheDoubleQuotationMarks(String ssid) {
        int deviceVersion;
        deviceVersion = Build.VERSION.SDK_INT;
        if (deviceVersion >= 17) {
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
        }
        return ssid;
    }

    void savePhoneIp(int ipAddress) {
        ipData = new StringBuffer();
        ipData.append((char) (ipAddress & 0xff));
        ipData.append((char) (ipAddress >> 8 & 0xff));
        ipData.append((char) (ipAddress >> 16 & 0xff));
        ipData.append((char) (ipAddress >> 24 & 0xff));
    }

    void enableThread() {
        Lg.i(TAG, "enableThread");
        exitProcess = false;
        if (sendUdpThread == null) {
            sendUdpThread = new sendUdpThread();
            sendUdpThread.start();
        }
        if (tcpThread == null) {
            tcpThread = new tcpThread();
            tcpThread.start();
        }
    }

    void exitThread() {
        exitProcess = true;
        if (sendUdpThread != null) {
            sendUdpThread.interrupt();
            sendUdpThread = null;
        }
        if (tcpThread != null) {
            if (serv != null) {
                try {
                    serv.close();
                    serv = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            tcpThread.interrupt();
            tcpThread = null;
        }
    }

    void savePassData() {
        SharedPreferences preferences = getSharedPreferences("preFile", 0);
        preferences.edit().putString("pass", passEdit.getText().toString()).apply();
    }

    void KSA() {
        int i, j = 0, temp;
        for (i = 0; i < 256; i++)
            stable[i] = i;
        for (i = 0; i < 256; i++) {
            j = (j + stable[i] + rc4Key.charAt(i % rc4Key.length())) % 256;
            temp = stable[i];
            stable[i] = stable[j];
            stable[j] = temp;
        }
    }

    void PRGA() {
        int m = 0, i = 0, j = 0, t, l, temp;
        l = 256;
        while (l > 0) {
            i = (i + 1) % 256;
            j = (j + stable[i]) % 256;
            temp = stable[i];
            stable[i] = stable[j];
            stable[j] = temp;
            t = (stable[j] + stable[i]) % 256;
            sonkey[m++] = stable[t];
            l--;
        }
    }

    char crc8_msb(char poly, int size, int cmdNum) {
        char crc = 0x00, tmp;
        int bit;
        int i = 0;
        while (size > 0) {
            crc ^= packetData[cmdNum].charAt(i);
            for (bit = 0; bit < 8; bit++) {
                if ((0x0ff & (crc & 0x80)) != 0x00) {
                    tmp = (char) (0x0ff & (crc << 1));
                    crc = (char) (tmp ^ poly);
                } else {
                    crc <<= 1;
                }
            }
            size--;
            i++;
        }
        return crc;
    }

    void cmdCryption(int cmdUum) {
        int i;
        for (i = 0; i < packetData[cmdUum].length(); i++) {
            tempPacket[i] = packetData[cmdUum].charAt(i) ^ sonkey[i];
            tempSeq[i] = seqData[cmdUum].charAt(i) ^ sonkey[0];
        }
        tempPacket[i] = '\n';
        tempSeq[i] = '\n';
    }

    void addSeqPacket(int cmdNum) {
        int i;
        char value;

        seqData[cmdNum] = new StringBuffer(packetData[cmdNum]);

        for (i = 0; i < seqData[cmdNum].length(); i++) {
            if (cmdNum == 0)
                value = (char) ((0x0ff & (i)));
            else if (cmdNum == 1)
                value = (char) ((0x0ff & (i << 1) | 0x01));
            else
                value = (char) ((0x0ff & (i << 2) | 0x02));
            seqData[cmdNum].setCharAt(i, value);
        }
    }

    void sendTestData() {
        int[] testData = new int[]{1, 2, 3, 4};
        for (int j = 0; j < testDataRetryNum; j++) {
            for (int k = 0; !(k >= testData.length); k++) {
                AtomicReference<StringBuffer> sendTestData = new AtomicReference<StringBuffer>(new StringBuffer());

                for (int v = 0; v < testData[k]; v++) {
                    sendTestData.get().append(AB.charAt(rand.nextInt(AB.length())));
                }

                try {
                    DatagramSocket clientSocket = new DatagramSocket();
                    clientSocket.setBroadcast(true);
                    address = InetAddress.getByName("255.255.255.255");
                    DatagramPacket sendPacketSeqSocket = new DatagramPacket(sendTestData.get().toString().getBytes(), sendTestData.get().length(), address, 8300);
                    clientSocket.send(sendPacketSeqSocket);
                    try {
                        Thread.sleep(10);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    clientSocket.close();
                    if (exitProcess)
                        return;
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (exitProcess)
                return;
        }
    }

    public void SendbroadCast() {
        Lg.i(TAG, "SendbroadCast()");
        char crcDdata;

        sendTestData();

        for (int z = 0; z < cmdNumber; z++) {
            packetData[z] = new StringBuffer();
            if (z == 0)
                packetData[0].append(magicNumber);
            else if (z == 1)
                packetData[1].append((char) ssidEdit.length()).append((char) passEdit.length()).append(ipData.charAt(0)).append(ipData.charAt(1)).append(ipData.charAt(2)).append(ipData.charAt(3));
            else
                packetData[2].append(ssidEdit.getText()).append(passEdit.getText());
            crcDdata = crc8_msb((char) 0x1D, packetData[z].length(), z);
            packetData[z].append(crcDdata);
            addSeqPacket(z);
            if (exitProcess)
                return;
        }

        for (int i = 0; i < cmdNumber; i++) {
            cmdCryption(i);
            for (int j = 0; j < Integer.valueOf(retryNumber[i]); j++) {
                for (int k = 0; k < packetData[i].length(); k++) {
                    AtomicReference<StringBuffer> sendPacketData = new AtomicReference<StringBuffer>(new StringBuffer());
                    AtomicReference<StringBuffer> sendPacketSeq = new AtomicReference<StringBuffer>(new StringBuffer());

                    for (int v = 0; v < tempPacket[k] + 1; v++) {
                        sendPacketData.get().append(AB.charAt(rand.nextInt(AB.length())));
                    }
                    for (int g = 0; g < (tempSeq[k] + 1 + 256); g++) {
                        sendPacketSeq.get().append(AB.charAt(rand.nextInt(AB.length())));
                    }

                    try {
                        DatagramSocket clientSocket = new DatagramSocket();
                        clientSocket.setBroadcast(true);
                        address = InetAddress.getByName("255.255.255.255");
                        DatagramPacket sendPacketSeqSocket = new DatagramPacket(sendPacketSeq.get().toString().getBytes(), sendPacketSeq.get().length(), address, 8300);
                        clientSocket.send(sendPacketSeqSocket);
                        try {
                            Thread.sleep(10);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        DatagramPacket sendPacketDataSocket = new DatagramPacket(sendPacketData.get().toString().getBytes(), sendPacketData.get().length(), address, 8300);
                        clientSocket.send(sendPacketDataSocket);
                        try {
                            Thread.sleep(10);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        clientSocket.close();
                        if (exitProcess)
                            return;
                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (exitProcess)
                        return;
                }
            }
            if (exitProcess)
                return;
        }
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        switch (view.getId()) {
            case R.id.iv_back:
                if (isSendFinished) {
                    sendFinish();
                }
                finish();
                break;
            case R.id.button:
                if (ssidEdit.getText().toString() == null || ssidEdit.getText().toString().trim().length() == 0) {
                    showShortToast(getString(R.string.wifi_id_not_empty));
                    return;
                }
//                if (passEdit.getText().toString() == null || passEdit.getText().toString().trim().length() == 0) {
//                    showShortToast(getString(R.string.wifi_pwd_not_empty));
//                    return;
//                }

//                if (scannerMac == null || scannerMac.trim().length() == 0) {
//                    showShortToast(getString(R.string.scanner_mac));
//                } else {
                if (!isSendFinished) {
                    isSendFinished = true;
                    broadCastButton.setText(getString(R.string.cancel));
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    broadCastButton.setProgress(broadCastButton.getProgress() + 1);
                                    if (broadCastButton.getProgress() > broadCastButton.getMax()) {
                                        Lg.i(TAG, "broadCastButton.getProgress() > broadCastButton.getMax()");
                                        Message message = new Message();
                                        message.arg1 = CONFIGUREFAIL;
                                        handler.sendMessage(message);
                                        sendFinish();
                                    }
                                }
                            });
                        }
                    }, 0, 1000);
                    //有用
                    enableThread();
                } else {
                    sendFinish();
                }
//                }
                break;
            case R.id.pwd_show_hide:
                if (isHidden) {
                    //设置EditText文本为可见的
                    passEdit.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    pwd_show_hide.setBackgroundResource(R.drawable.pwd_show);
                } else {
                    //设置EditText文本为隐藏的
                    passEdit.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    pwd_show_hide.setBackgroundResource(R.drawable.pwd_hide);
                }
                isHidden = !isHidden;
                break;
//            case R.id.iv_scanner:
//                Intent intent = new Intent(SmartLinkActivity.this, CaptureActivity.class);
//                startActivityForResult(intent, REQUEST_CODE);
//                break;
            default:
                break;
        }
    }

    private void sendFinish() {
        isSendFinished = false;
        broadCastButton.setText(getString(R.string.configure));
        broadCastButton.setProgress(0);
        if (timer != null) {
            timer.cancel();
        }
        exitThread();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String mac = (String) msg.obj;
            if (mac != null && mac.length() != 0) {
                if (mac.matches("Connection to .* refused") || mac.matches("Connect to.*timed out")) {
                    showComReminderDialog();
                    return;
                }
                switch (msg.arg1) {
                    case CONFIGUREOK:
                        sendFinish();
//                    showShortToast(getString(R.string.configure_route_ok));
                        //处理mac---前面不足两位，前面加0,后面添加0
                        String str[] = mac.split("\\:");
                        String realMac = "";
                        for (int i = 0; i < str.length; i++) {
                            if (str[i].length() == 1) {
                                str[i] = "0" + str[i];
                            }
                            realMac = realMac + str[i];
                        }
                        Lg.i(TAG, "realMac->>>" + realMac);
                        //确认扫描mac与连接mac是否相等
//                        if (!scannerMac.equalsIgnoreCase(realMac)) {
//                            showShortToast(getString(R.string.mac_not_match));
//                        } else {
                        if (realMac.length() < 15) {
                            for (int j = realMac.length() + 1; j <= 15; j++) {
                                realMac = realMac + "0";
                            }
                        } else if (realMac.length() > 15) {
                            showShortToast(getString(R.string.get_wifi_mac_error));
                            return;
                        }
                        final String stableMac = realMac;
                        Lg.i(TAG, "stableMac>>>" + stableMac);
                        showShortToast(getString(R.string.configure_route_ok));
                        ThreadPoolManager.getInstance().addTask(new Runnable() {
                            @Override
                            public void run() {
                                String result = HttpUtil.post(HttpUtil.URL_LINKWIFIDEVICE,
                                        new BasicNameValuePair(JsonUtil.IMEI, stableMac));
                                Lg.i(TAG, "HttpUtil.URL_LINKWIFIDEVICE->>>>>" + result);
                                Message msg = new Message();
                                msg.obj = result;
                                msg.arg1 = LINKWIFI;
                                handler.sendMessage(msg);
                            }
                        });
//                        }
                        break;
                    case CONFIGUREFAIL:
                        sendFinish();
                        showShortToast(getString(R.string.configure_route_fail));
                        break;
                    case LINKWIFI:
                        JSONObject json = null;
                        try {
                            json = new JSONObject(mac);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (!"ok".equals(JsonUtil.getStr(json, JsonUtil.STATUS))) {
                            BaseTools.showToastByLanguage(SmartLinkActivity.this, json);
                        } else {
                            BaseTools.showToastByLanguage(SmartLinkActivity.this, json);
                            ret = 1;
                        }
                        Intent intent = new Intent();
                        Bundle b = new Bundle();
                        b.putInt("ret", ret);
                        intent.putExtras(b);
                        setResult(RESULT_OK, intent);
                        finish();
                        break;
                    default:
                        break;
                }
            } else {
                showShortToast(getString(R.string.check_net_config));
            }
        }

    };

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        /**
//         * 处理二维码扫描结果
//         */
//        if (requestCode == REQUEST_CODE) {
//            //处理扫描结果（在界面上显示）
//            if (null != data) {
//                Bundle bundle = data.getExtras();
//                if (bundle == null) {
//                    return;
//                }
//                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
//                    scannerMac = bundle.getString(CodeUtils.RESULT_STRING).trim();
//                    Toast.makeText(this, "解析结果:" + scannerMac, Toast.LENGTH_LONG).show();
//                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
//                    Toast.makeText(SmartLinkActivity.this, "解析二维码失败", Toast.LENGTH_LONG).show();
//                }
//            }
//        }
//    }

}
