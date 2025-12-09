package com.example.kesongproject;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "https://college-training-camp.bytedance.com/";
    private static Retrofit retrofit;

    // 获取 Retrofit 实例 (单例模式)
    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create()) // 自动把 JSON 变 Java 对象
                    .build();
        }
        return retrofit;
    }

    // 获取 ApiService 接口实例
    public static ApiService getApiService() {
        return getRetrofitInstance().create(ApiService.class);
    }
}