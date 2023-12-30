package com.iot.smart_lighting.Model;

public class LampTimerModel {

    private int id;
    private String time;
    private int status;
    private int lampId;

    public LampTimerModel(int id, String time, int status, int lampId) {
        this.id = id;
        this.time = time;
        this.status = status;
        this.lampId = lampId;
    }

    public int getId() {
        return id;
    }

    public String getTime() {
        return time;
    }

    public int getStatus() {
        return status;
    }

    public int getLampId() {
        return lampId;
    }
}
