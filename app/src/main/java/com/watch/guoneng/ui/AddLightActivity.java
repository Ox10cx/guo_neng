package com.watch.guoneng.ui;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.watch.guoneng.R;

public class AddLightActivity extends BaseActivity {
    private String light_name;
    private ImageView iv_back;
    private TextView ok_tv;
    private EditText et_light_name;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addlight);
        initViews();
        et_light_name.setText(getIntent().getStringExtra("name"));
    }


    private void initViews() {
        iv_back = (ImageView) findViewById(R.id.iv_back);
        iv_back.setOnClickListener(this);
        ok_tv = (TextView) findViewById(R.id.ok_tv);
        ok_tv.setOnClickListener(this);
        et_light_name = (EditText) findViewById(R.id.et_light_name);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.iv_back:
                finish();
                break;
            case R.id.ok_tv:
                light_name = et_light_name.getText().toString().trim();
                if (light_name.length() == 0) {
                    setHintText(et_light_name, getResources().getString(
                            R.string.light_name_empty_remind));
                    return;
                } else if (light_name.length() > 20) {
                    setHintText(et_light_name, getResources().getString(
                            R.string.light_name_format_remind));
                    return;
                }
                goBack();
                break;
            default:
                break;
        }
    }

    private void goBack() {
        Intent intent = new Intent();
        intent.putExtra("light_name", light_name);
        setResult(222, intent);
        finish();
    }

    /**
     * et  输入错误提示
     *
     * @param et
     * @param str
     */
    private void setHintText(EditText et, String str) {
        et.setText("");
        et.setHintTextColor(Color.parseColor("#FF4070"));
        et.setHint(str);
    }

}
