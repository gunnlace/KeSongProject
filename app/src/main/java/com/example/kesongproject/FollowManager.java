package com.example.kesongproject;

import android.content.Context;
import android.content.SharedPreferences;

public class FollowManager {
    private static final String PREF_NAME = "follow_status_pref";

    // 检查是否已关注该作者
    public static boolean isFollowed(Context context, String authorId) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(authorId, false);
    }

    // 设置关注状态
    public static void setFollowed(Context context, String authorId, boolean isFollowed) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(authorId, isFollowed).apply();
    }
}