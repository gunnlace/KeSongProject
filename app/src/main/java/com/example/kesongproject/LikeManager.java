package com.example.kesongproject;

import android.content.Context;
import android.content.SharedPreferences;

public class LikeManager {
    private static final String PREF_NAME = "like_status_pref";

    // 判断某个帖子是否已点赞
    public static boolean isLiked(Context context, String postId) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // key 是 postId, value 是 boolean
        return sp.getBoolean(postId, false);
    }

    // 设置点赞状态 (持久化保存)
    public static void setLiked(Context context, String postId, boolean isLiked) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(postId, isLiked).apply(); // apply() 会异步写入磁盘
    }
}