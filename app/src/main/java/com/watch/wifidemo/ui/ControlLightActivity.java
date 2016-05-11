package com.watch.wifidemo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.watch.wifidemo.R;
import com.watch.wifidemo.model.Light;

/**
 * Created by Administrator on 16-3-7.
 */
public class ControlLightActivity extends BaseActivity implements View.OnClickListener {
    private Light light;
    private TextView headtitle;
    private TextView oktv;
    private TextView lightness_per;
    private TextView tem_per;
    private SeekBar lightness_sb;
    private SeekBar tem_sb;
    private int lighenessValue;
    private int temValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conlight);
        headtitle = (TextView) findViewById(R.id.head_title);
        oktv = (TextView) findViewById(R.id.ok_tv);
        oktv.setOnClickListener(this);
        Intent intent = getIntent();
        light = (Light) intent.getSerializableExtra("light");
        headtitle.setText("编号为" + light.getId() + "的彩灯");
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.ok_tv:
//                showShortToast("亮度：" + lightnessSlider.getLightness() +
//                        "\n色温：" + colorPickerView.getColorness());
                break;
            default:
                break;
        }
    }
}
