package com.watch.guoneng.model;

/**
 * 开关计时
 * Created by zsg on 2016/8/30.
 */
public class LightTimer {
    private String id;
    private String imei;
    private String delay;
    private String action;
    private String name;
    private int repeat;     //重复次数

    public static int NO_REPEAT=0;      //仅一次
    public static int EVERY_DAY=1;      //每天
    public static int EVERY_WEEK=2;     //每周
    public static int EVERY_MOUTH=3;    //每月

    public LightTimer(String id, String imei, String delay, String action, String name, int repeat) {
        this.id = id;
        this.imei = imei;
        this.delay = delay;
        this.action = action;
        this.name = name;
        this.repeat = repeat;
    }

    public LightTimer(){

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getDelay() {
        return delay;
    }

    public void setDelay(String delay) {
        this.delay = delay;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getRepeat() {
        return repeat;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    public String getRepeatToString(){
        switch(repeat){
            case 0:
                return "仅一次";
            case 1:
                return "每天";
            case 2:
                return "每周";
            case 3:
                return "每月";
        }
        return "";
    }

    public static String getRepeatToString(int repeat){
        String repeatstr="";
        switch(repeat){
            case 0:
                repeatstr="none";
                break;
            case 1:
                repeatstr="day";
                break;
            case 2:
                repeatstr="week";
                break;
            case 3:
                repeatstr="month";
                break;
        }
        return repeatstr;
    }


    @Override
    public String toString() {
        return "LightTimer{" +
                "id='" + id + '\'' +
                ", imei='" + imei + '\'' +
                ", delay='" + delay + '\'' +
                ", action='" + action + '\'' +
                ", repeat=" + repeat +
                '}';
    }
}
