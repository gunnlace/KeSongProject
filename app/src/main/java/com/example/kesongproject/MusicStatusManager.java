package com.example.kesongproject;

/**
 * 音乐状态管理器
 * 作用：在 App 运行期间记住用户的静音设置
 */
public class MusicStatusManager {
    // static 变量：App 活着时一直存在，App 杀掉后重置
    // 默认 false (初始为非静音)
    private static boolean isMuted = false;

    public static boolean isMuted() {
        return isMuted;
    }

    public static void setMuted(boolean muted) {
        isMuted = muted;
    }
}