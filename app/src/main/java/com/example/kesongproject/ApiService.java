package com.example.kesongproject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {

    // 定义一个 GET 请求
    // 完整 URL 是 https://college-training-camp.bytedance.com/feed/
    // Base URL (后面配) 是 https://college-training-camp.bytedance.com/
    // 这里填相对路径: "feed/"
    @GET("feed/")
    Call<FeedResponse> getFeed(
            @Query("count") int count,                 // 参数: 请求多少条
            @Query("accept_video_clip") boolean acceptVideo // 参数: 是否接收视频
    );
}