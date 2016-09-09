package com.watch.guoneng.util;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by zsg on 2016/8/30.
 */
public class TimeUtil {

    public static String getTimetoString(String timestr) {
        long time = Long.parseLong(timestr);
        if (time < 60)
            return time + "秒";
        if (time < 60 * 60) {
            if (time % 60 == 0)
                return time / 60 + "分钟";
            return time / 60 + "分钟" + time % 60 + "秒";
        }
        if (time < 60 * 60 * 24) {
            int hour = (int) (time / 3600);
            int min = (int) (time % 3600 / 60);
            if (min > 0)
                return hour + "小时" + min + "分钟";
            return hour + "小时";
        }

        String temp="";
        if(time % (60 * 60 * 24) / (3600)==0){
            if(time % (60 * 60 * 24)%3600/60!=0)
                temp=time % (60 * 60 * 24)%3600/60+"分钟";
        }else{
            temp=time % (60 * 60 * 24) / (3600) + "小时";
        }
        return time / (60 * 60 * 24) + "天" + temp;

    }


    /**
     * 得到与当前时间的相对时间  若为负数则加一天
     *
     * @return
     */
    public static long getRelativeTime(String setTimer) {
        Date nowTime = new Date(System.currentTimeMillis());

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        //Date setDate=format.parse("18:00");
        String tempTimer = format.format(nowTime).substring(0,10)+" "+setTimer;
        Log.d("testTimeUtil", nowTime.getTime() + "");
        try {
            long delay=(format.parse(tempTimer).getTime()-nowTime.getTime())/1000;
            if(delay<=0)
                delay=24*60*60+delay;
            return delay;
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return 0;

    }
}
