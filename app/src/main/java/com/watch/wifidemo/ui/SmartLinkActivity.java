package com.watch.wifidemo.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.watch.wifidemo.R;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Administrator on 16-3-7.
 */
public class SmartLinkActivity extends BaseActivity {
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
    String retryNumber[] = {"10", "10",  "5"};
    String magicNumber = "iot";
    String rc4Key = "Key";
    String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    int[] stable = new int[256];
    int[] tempPacket = new int[256];
    int[] tempSeq = new int[256];
    int[] sonkey = new int[256];
    String PASS;
    ServerSocket serv;

    private TextView ssidView;
    private TextView bssidView;
    private TextView passView;
    private TextView ipMacView;
    private EditText ssidEdit;
    private EditText bssidEdit;
    private EditText passEdit;
    private Button broadCastButton;
    private ListView listview;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> items;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.frag_smartlink);
        ssidView = (TextView) findViewById(R.id.ssidView);
        bssidView = (TextView) findViewById(R.id.bssidView);
        passView = (TextView) findViewById(R.id.passView);
        ssidEdit = (EditText) findViewById(R.id.ssidText);
        bssidEdit = (EditText) findViewById(R.id.bssidText);
        passEdit = (EditText) findViewById(R.id.passText);
        broadCastButton = (Button) findViewById(R.id.button);
        ipMacView = (TextView) findViewById(R.id.ipMac);
        listview = (ListView) findViewById(R.id.listView);

        ssidView.setText(getString(R.string.SSID));
        bssidView.setText(getString(R.string.BSSID));
        passView.setText(getString(R.string.PASS));
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        ssidEdit.setText(whetherToRemoveTheDoubleQuotationMarks(wifiInfo.getSSID()));
        bssidEdit.setText(wifiInfo.getBSSID());
        SharedPreferences preferences = getSharedPreferences("preFile", 0);
        PASS = preferences.getString("pass", "");
        if (!(PASS.equals(""))) {
            passEdit.setText(PASS);
        }
        broadCastButton.setText(getString(R.string.send));
        broadCastButton.setBackgroundColor(Color.GRAY);
        ipMacView.setText(getString(R.string.IPMAC));
        items = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
        listview.setItemsCanFocus(true);
        listview.setAdapter(adapter);

        broadCastButton.setOnClickListener(buttonSmartLink);

        savePhoneIp(wifiInfo.getIpAddress());
        serv = null;
        sendUdpThread = null;
        tcpThread = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

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
        bssidEdit.setText(wifiInfo.getBSSID());
        SharedPreferences preferences = getSharedPreferences("preFile", 0);
        PASS = preferences.getString("pass", "");
        if (!(PASS.equals(""))) {
            passEdit.setText(PASS);
        }
        broadCastButton.setText(getString(R.string.send));
        broadCastButton.setBackgroundColor(Color.GRAY);
        items.clear();
        savePhoneIp(wifiInfo.getIpAddress());
    }

    View.OnClickListener buttonSmartLink = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            if (broadCastButton.getText() == getString(R.string.send)) {
                broadCastButton.setText(getString(R.string.close));
                broadCastButton.setBackgroundColor(Color.RED);
                enableThread();
            } else {
                broadCastButton.setText(getString(R.string.send));
                broadCastButton.setBackgroundColor(Color.GRAY);
                exitThread();
            }
        }
    };
    public class sendUdpThread extends Thread {

        public void run() {
            KSA();
            PRGA();
            while (!exitProcess) {
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
        }

        public void run() {
            try {
                char[] tmpbuffer = new char[1024];
                in = socket.getInputStream();
                out = socket.getOutputStream();
                streamWriter = new DataOutputStream(out);
                streamReader = new InputStreamReader(in, "UTF-8");
                int len = streamReader.read(tmpbuffer, 0, 1024);
                if(len > 0) {
                    char[] buffer = Arrays.copyOf(tmpbuffer, len);
                    String message =  socket.getInetAddress().getHostAddress() + "/" + new String(buffer);

                    updateListViewState(message);
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
        int port=8209;
        Socket s1=null;

        public void run() {
            while (!exitProcess) {
                try {
                    serv=new ServerSocket(port,10);
                }catch (Exception se){
                    System.out.println("Init ServerSocker Error!!");
                }

                try {
                    s1=serv.accept();
                    tcpReceThread mt=new tcpReceThread(s1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                if(serv != null) {
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
        exitProcess = false;
        if(sendUdpThread == null) {
            sendUdpThread = new sendUdpThread();
            sendUdpThread.start();
        }
        if(tcpThread == null) {
            tcpThread = new tcpThread();
            tcpThread.start();
        }
    }

    void exitThread() {
        exitProcess = true;
        if(sendUdpThread != null) {
            sendUdpThread.interrupt();
            sendUdpThread = null;
        }
        if(tcpThread != null) {
            if(serv != null) {
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
            if(exitProcess)
                return;
        }
    }

    public void SendbroadCast() {
        char crcDdata;

        sendTestData();

        for (int z = 0; z < cmdNumber; z++) {
            packetData[z] = new StringBuffer();
            if (z == 0)
                packetData[0].append(magicNumber);
            else if (z ==1)
                packetData[1].append((char) ssidEdit.length()).append((char) passEdit.length()).append(ipData.charAt(0)).append(ipData.charAt(1)).append(ipData.charAt(2)).append(ipData.charAt(3));
            else
                packetData[2].append(ssidEdit.getText()).append(passEdit.getText());
            crcDdata = crc8_msb((char) 0x1D, packetData[z].length(), z);
            packetData[z].append(crcDdata);
            addSeqPacket(z);
            if(exitProcess)
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
                        if(exitProcess)
                            return;
                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if(exitProcess)
                        return;
                }
            }
            if(exitProcess)
                return;
        }
    }

    public void updateListViewState(final String tmp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (adapter.getCount() == 0) {
                    items.add(tmp);
                    listview.setAdapter(adapter);
                    listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            String str = adapter.getItem(position);
                            Toast.makeText(getApplicationContext(), "choose " + str, Toast.LENGTH_SHORT).show();
                            SharedPreferences preferences = getSharedPreferences("ipmacFile", 0);
                            preferences.edit().putString("ipaddress", str).apply();
                        }
                    });
                }
                for (int i = 0; i < adapter.getCount(); i++) {
                    String c = adapter.getItem(i);
                    if (c.matches(tmp)) {
                        adapter.getItem(i);
                    } else {
                        Set<String> hs = new HashSet<String>();
                        items.add(tmp);
                        hs.addAll(items);
                        items.clear();
                        items.addAll(hs);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }
}
