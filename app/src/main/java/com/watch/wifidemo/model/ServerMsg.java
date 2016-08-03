package com.watch.wifidemo.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Administrator on 16-7-11.
 */
public class ServerMsg implements Serializable {
    String msgId;
    String imei;
    String cmd;
    long datetime;

    public final static int MS_HTTP_TIMEOUT = 3 * 1000;        // 3s 发送HTTP请求超时时间
    public final static int MS_DEVICE_TIMEOUT = 8 * 1000;       // 6s 设备回应的超时时间

    public final static int FROM_HTTP = 1;
    public final static int FROM_DEVICE = 2;

    public ServerMsg(String msgId, String imei, String cmd) {
        this.msgId = msgId;
        this.imei = imei;
        this.cmd = cmd;
        datetime = System.currentTimeMillis();
    }

    public boolean isTimeout(int from) {
        long current = new Date().getTime();

        long timeout;
        if (from == FROM_HTTP) {
            timeout = MS_HTTP_TIMEOUT;
        } else {
            timeout = MS_DEVICE_TIMEOUT;
        }

        if (current - datetime > timeout) {
            return true;
        } else {
            return false;
        }
    }

    public ServerMsg() {
        datetime = System.currentTimeMillis();
    }

    public String getMsgId() {
        return msgId;
    }

    public String getImei() {
        return imei;
    }

    public String getCmd() {
        return cmd;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServerMsg serverMsg = (ServerMsg) o;

        return !(getMsgId() != null ? !getMsgId().equals(serverMsg.getMsgId()) : serverMsg.getMsgId() != null);

    }

    @Override
    public int hashCode() {
        return getMsgId() != null ? getMsgId().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ServerMsg{" +
                "msgId='" + msgId + '\'' +
                ", imei='" + imei + '\'' +
                ", cmd='" + cmd + '\'' +
                ", datetime='" + datetime + '\'' +
                '}';
    }
}
