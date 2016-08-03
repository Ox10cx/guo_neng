package com.watch.wifidemo.ui;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.watch.wifidemo.R;

public class AddLightActivity extends BaseActivity {
    private String light_name;
    private String light_no;
    private ImageView iv_back;
    private TextView ok_tv;
    private EditText et_light_name;
    private EditText et_light_no;
    private TextView head_title;
    private RelativeLayout edit_light_layout;
    private String type = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addlight);
        type = getIntent().getStringExtra("type");
        initViews();
        if ("addlight".equals(type)) {
            head_title.setText(getString(R.string.add_light));
//            et_light_no.setText(getIntent().getIntExtra("default_no", 1) + "");
            edit_light_layout.setVisibility(View.VISIBLE);
        } else {
            head_title.setText(getString(R.string.edit_light));
            et_light_name.setText(getIntent().getStringExtra("name"));
            edit_light_layout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initViews() {
        head_title = (TextView) findViewById(R.id.head_title);
        edit_light_layout = (RelativeLayout) findViewById(R.id.edit_light_layout);
        iv_back = (ImageView) findViewById(R.id.iv_back);
        iv_back.setOnClickListener(this);
        ok_tv = (TextView) findViewById(R.id.ok_tv);
        ok_tv.setOnClickListener(this);
        et_light_name = (EditText) findViewById(R.id.et_light_name);
        et_light_no = (EditText) findViewById(R.id.et_light_no);
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
                if ("addlight".equals(type)) {
                    light_no = et_light_no.getText().toString().trim();
                    if (light_no.length() == 0) {
                        setHintText(et_light_no, getResources().getString(
                                R.string.light_no_empty_remind));
                        return;
                    } else if (Integer.valueOf(light_no) < 1 || Integer.valueOf(light_no) > 255) {
                        setHintText(et_light_no, getResources().getString(
                                R.string.light_no_format_remind));
                        return;
                    }
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
        if ("addlight".equals(type)) {
            intent.putExtra("light_no", light_no);
            intent.putExtra("group_index", getIntent().getIntExtra("group_index", 0));
            setResult(123, intent);
        } else {
            setResult(222, intent);
        }
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
