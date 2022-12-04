package com.example.iot;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface Api {

    String BASE_URL = "http://abhishekgiri219.pythonanywhere.com";
    @GET("/mlserver")
    public Call<Results> getPredictionByValues(@Query("values") String values);
}

