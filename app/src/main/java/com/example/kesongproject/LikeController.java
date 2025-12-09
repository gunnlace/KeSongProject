package com.example.kesongproject;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

public class LikeController {

    // 处理点赞/取消点赞的动作
    // 参数：context, post数据对象, 心形图标, 数字文本
    public static void handleLikeClick(Context context, FeedResponse.Post post, ImageView ivLike, TextView tvLikeCount) {
        // 1. 改变状态
        boolean newState = !post.isLiked;
        post.isLiked = newState;

        // 2. 更新内存数据 (数字)
        if (newState) {
            post.likeCount++;
        } else {
            post.likeCount--;
        }

        // 3. 更新 UI
        updateLikeUI(ivLike, tvLikeCount, post);

        // 4. 持久化到本地
        LikeManager.setLiked(context, post.postId, newState);

//        // 5.发送全局广播
//        FeedUpdateCenter.notifyPostUpdated(post);
    }

    // 专门负责更新 UI 状态 (避免代码重复)
    public static void updateLikeUI(ImageView ivLike, TextView tvLikeCount, FeedResponse.Post post) {
        // 更新数字
        tvLikeCount.setText(String.valueOf(post.likeCount));

        // 更新图标
        if (post.isLiked) {
            // 实心/红色
            ivLike.setImageResource(R.drawable.ic_interaction_like_selected);
        } else {
            // 空心/灰色
            ivLike.setImageResource(R.drawable.ic_interaction_like);
        }
    }
}