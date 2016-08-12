package com.watch.guoneng.tool;

import android.content.Context;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.widget.TextView;

import com.watch.guoneng.R;

public class GetCodeCountTimer extends CountDownTimer {
    private static final String TAG = "GetCodeCountTimer";
    /**
     * 获取验证码
     */
    private TextView but;
    private Context context;

    public GetCodeCountTimer(long millisInFuture, long countDownInterval,
                             TextView but, Context context) {
        super(millisInFuture, countDownInterval);
        this.but = but;
        this.context = context;
    }

    @Override
    public void onFinish() {
        but.setClickable(true);
        but.setText(context.getString(R.string.register_button_code));
        but.setTextColor(Color.parseColor("#ffffff"));
    }

    @Override
    public void onTick(long millisUntilFinished) {
        but.setClickable(false);
        but.setTextColor(Color.parseColor("#d2d2d2"));
        but.setText(millisUntilFinished / 1000 + context.getString(R.string.second));
    }

}
