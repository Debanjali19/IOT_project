package com.example.iot;

import com.google.gson.annotations.SerializedName;

public class Results {
    @SerializedName("result")
    private String result;


    public Results(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }
}

