// ICallback.aidl
package com.watch.guoneng.ui;

// Declare any non-default types here with import statements
interface ICallback {
       void onConnect(String address);
       void onDisconnect(String address);
       boolean onRead(String address, in byte[] val);
       boolean onWrite(String address, out byte[] val);
       void onNotify(String imei, int type);

       //控制开关的返回结果
       void onSwitchRsp(String imei, boolean ret, boolean status); // 返回最新的状态

       //获取led状态
       void onGetStatusRsp(String imei, int ret);
        //wifi模块请求超时
       void onCmdTimeout(String cmd, String imei);

       // HTTP 请求超时
       void onHttpTimeout(String cmd, String imei);
       void onPingRsp(String imei, int ret);

       void onGetLightList(String imei, out byte[] list);
       void onSetBrightChromeRsp(String imei, int ret);
       void onGetBrightChromeRsp(String imei, int index, int bright, int chrome);
       void onPairLightRsp(String imei, int ret);
}
