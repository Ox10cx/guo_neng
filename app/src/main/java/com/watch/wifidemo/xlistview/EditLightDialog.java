package com.watch.wifidemo.xlistview;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.widget.Button;

import com.watch.wifidemo.R;


public class EditLightDialog extends Dialog {

    public Button delete_light;
    public Button edit_light;

    public EditLightDialog(Context context) {
        this(context, R.style.CustomProgressDialog);
    }

    public EditLightDialog(Context context, int theme) {
        super(context, theme);
        this.setContentView(R.layout.dialog_edit_light);
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
