// IService.aidl
package com.watch.wifidemo.ui;

// Declare any non-default types here with import statements
import com.watch.wifidemo.ui.ICallback;

interface IService {
    void registerCallback(ICallback cb);
    void unregisterCallback(ICallback cb);

    boolean initialize();
    boolean connect(String addr);
    void disconnect(String addr);

    void enableLight(String addr, boolean on);
    void getLightStatus(String addr);
    void ping(String addr, int val);
}
