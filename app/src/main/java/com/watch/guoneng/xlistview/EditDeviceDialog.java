package com.watch.guoneng.xlistview;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.widget.Button;

import com.watch.guoneng.R;


public class EditDeviceDialog extends Dialog {

    public Button delete_light;
    public Button edit_light;

    public EditDeviceDialog(Context context) {
        this(context, R.style.CustomProgressDialog);
    }

    public EditDeviceDialog(Context context, int theme) {
        super(context, theme);
        this.setContentView(R.layout.dialog_edit_device);
        this.getWindow().getAttributes().gravity = Gravity.CENTER;
        delete_light = (Button) this.findViewById(R.id.delete_light);
        edit_light = (Button) this.findViewById(R.id.edit_light);
    }

    /**
     * 按返回键，对话框没有反应，必须点取消
     */
    @Override
    public void onBackPressed() {
        cancel();
    }
}
