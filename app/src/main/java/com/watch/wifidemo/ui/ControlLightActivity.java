package com.watch.wifidemo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.watch.wifidemo.R;
import com.watch.wifidemo.model.Light;

/**
 * Created by Administrator on 16-3-7.
 */
public class ControlLightActivity extends BaseActivity implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "ControlLightActivity";
    private Light light;
    private TextView headtitle;
    private TextView oktv;
    private TextView lightness_per;
    private TextView tem_per;
    private SeekBar lightness_sb;
    private SeekBar tem_sb;
    private int lighenessValue;
    private int temValue;
    private ImageView light_switch;
    private boolean is_light_on = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conlight);
        initViews();
    }

    private void initViews() {
        headtitle = (TextView) findViewById(R.id.head_title);
        oktv = (TextView) findViewById(R.id.ok_tv);
        lightness_per = (TextView) findViewById(R.id.lightness_per);
        tem_per = (TextView) findViewById(R.id.tem_per);
        lightness_sb = (SeekBar) findViewById(R.id.lightness_sb);
        lightness_sb.setOnSeekBarChangeListener(this);
        tem_sb = (SeekBar) findViewById(R.id.tem_sb);
        tem_sb.setOnSeekBarChangeListener(this);
        oktv.setOnClickListener(this);
        light_switch = (ImageView) findViewById(R.id.light_switch);
        light_switch.setOnClickListener(this);
        Intent intent = getIntent();
        light = (Light) intent.getSerializableExtra("light");
        headtitle.setText("编号为" + light.getId() + "的彩灯");
        is_light_on = light.is_on();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!is_light_on) {
            light_switch.setImageResource(R.drawable.tbtn_off);
        } else {
            light_switch.setImageResource(R.drawable.tbtn_on);
        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.ok_tv:
                showShortToast("亮度：" + lighenessValue +
                        "\n色温：" + temValue);
                break;
            case R.id.light_switch:
                if (!is_light_on) {
                    light_switch.setImageResource(R.drawable.tbtn_on);
                    is_light_on = true;
                } else {
                    light_switch.setImageResource(R.drawable.tbtn_off);
                    is_light_on = false;
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        Log.i(TAG, "onProgressChanged");
        switch (seekBar.getId()) {
            case R.id.lightness_sb:
                Log.i(TAG, "lightness_sb");
                lighenessValue = progress;
                Log.i(TAG,"lighenessValue:"+lighenessValue);
                lightness_per.setText((int)((lighenessValue / 255.00)*100) + "%");
                Log.i(TAG,"per:"+lighenessValue / 255 + "%");
                break;
            case R.id.tem_sb:
                temValue = progress;
                tem_per.setText((int)((temValue / 255.00)*100) + "%");
                break;
            default:
                break;
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
