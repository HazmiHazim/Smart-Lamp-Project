package com.iot.smart_lighting.Model;

public class LampColourModel {

    private int id;
    private String colour;
    private int lampId;

    public LampColourModel(int id, String colour, int lampId) {
        this.id = id;
        this.colour = colour;
        this.lampId = lampId;
    }

    public int getId() {
        return id;
    }

    public String getColour() {
        return colour;
    }

    public int getLampId() {
        return lampId;
    }
}
