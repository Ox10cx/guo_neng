package com.watch.wifidemo.model;

import java.io.Serializable;

/**
 * Created by lenovo001 on 2016/5/10.
 */
public class Light implements Serializable {
    private String id;
    private String name;
    private boolean is_on;
    private String lightness;
    private String color;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean is_on() {
        return is_on;
    }

    public void setIs_on(boolean is_on) {
        this.is_on = is_on;
    }

    public String getLightness() {
        return lightness;
    }

    public void setLightness(String lightness) {
        this.lightness = lightness;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return "Light{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", is_on=" + is_on +
                ", lightness='" + lightness + '\'' +
                ", color='" + color + '\'' +
                '}';
    }
}
