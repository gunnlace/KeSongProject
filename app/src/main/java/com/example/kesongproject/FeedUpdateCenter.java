package com.example.kesongproject;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据更新中转站 (单例)
 * 作用：让详情页的操作能实时通知到列表页
 */
public class FeedUpdateCenter {

    // 1. 定义接口：谁想听消息，谁就实现这个接口
    public interface OnPostUpdateListener {
        void onPostUpdated(FeedResponse.Post newPost);
    }

    // 2. 订阅者列表 (存的是 HomeFragment 这样的监听者)
    private static final List<OnPostUpdateListener> listeners = new ArrayList<>();

    // 3. 注册监听 (HomeFragment 来调)
    public static void register(OnPostUpdateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    // 4. 取消注册 (HomeFragment 销毁时调，防止内存泄漏)
    public static void unregister(OnPostUpdateListener listener) {
        listeners.remove(listener);
    }

    // 5. 发送通知 (NoteDetailActivity 来调)
    public static void notifyPostUpdated(FeedResponse.Post newPost) {
        // 遍历所有监听者，挨个通知
        for (OnPostUpdateListener listener : listeners) {
            listener.onPostUpdated(newPost);
        }
    }
}