// ICallback.aidl
package com.watch.wifidemo.ui;

// Declare any non-default types here with import statements
interface ICallback {
       void onConnect(String address);
       void onDisconnect(String address);
       boolean onRead(String address, in byte[] val);
       boolean onWrite(String address, out byte[] val);
       void onNotify(String imei, int type);
       void onSwitchRsp(String imei, boolean ret);
       void onGetStatusRsp(String imei, int ret);
       void onCmdTimeout(String cmd, String imei);
       void onPingRsp(String imei, int ret);
}
