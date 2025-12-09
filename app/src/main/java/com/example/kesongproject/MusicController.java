package com.example.kesongproject;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.ImageView;

/**
 * 专门负责音乐播放的控制器
 * 把播放逻辑从 Activity 里剥离出来
 */
public class MusicController {

    private MediaPlayer mediaPlayer;
    private boolean isPrepared = false;
    private String musicUrl;
    private ImageView ivMuteSwitch; // 静音按钮
    private Context context;

    // 构造函数
    public MusicController(Context context, ImageView ivMuteSwitch) {
        this.context = context;
        this.ivMuteSwitch = ivMuteSwitch;
    }
    // 定义接口：状态变更监听器
    public interface OnMuteStateChangeListener {
        void onMuteStateChanged(boolean isMuted);
    }
    private OnMuteStateChangeListener muteListener;

    // 2. 允许外部注册监听
    public void setOnMuteStateChangeListener(OnMuteStateChangeListener listener) {
        this.muteListener = listener;
    }

    /**
     * 初始化播放器
     * @param url 音乐链接
     */
    public void init(String url) {
        this.musicUrl = url;

        // 1. 判空
        if (musicUrl == null || musicUrl.isEmpty()) {
            ivMuteSwitch.setVisibility(View.GONE);
            return;
        }

        // 2. 显示按钮并初始化 UI 状态
        ivMuteSwitch.setVisibility(View.VISIBLE);
        updateMuteIconUI(MusicStatusManager.isMuted());

        // 3. 设置点击事件
        ivMuteSwitch.setOnClickListener(v -> {
            boolean currentMute = MusicStatusManager.isMuted();
            boolean newMute = !currentMute;

            // 更新全局状态
            MusicStatusManager.setMuted(newMute);
            // 更新 UI
            updateMuteIconUI(newMute);
            // 应用给播放器
            applyMuteState(newMute);
            // 通知 Activity，准备轮播
            if (muteListener != null) {
                muteListener.onMuteStateChanged(newMute);
            }
        });

        // 4. 准备播放器
        preparePlayer();
    }

    private void preparePlayer() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(musicUrl);
            mediaPlayer.setLooping(true); // 循环播放

            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                mp.start();
                applyMuteState(MusicStatusManager.isMuted());
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                isPrepared = false;
                ivMuteSwitch.setVisibility(View.GONE);
                return true;
            });
        } catch (Exception e) {
            e.printStackTrace();
            ivMuteSwitch.setVisibility(View.GONE);
        }
    }

    // 应用静音
    private void applyMuteState(boolean isMuted) {
        if (mediaPlayer != null && isPrepared) {
            if (isMuted) {
                mediaPlayer.setVolume(0f, 0f);
            } else {
                mediaPlayer.setVolume(1f, 1f);
            }
        }
    }

    // 更新图标
    private void updateMuteIconUI(boolean isMuted) {
        if (isMuted) {
            ivMuteSwitch.setImageResource(R.drawable.ic_volume_off);
        } else {
            ivMuteSwitch.setImageResource(R.drawable.ic_volume_on);
        }
    }

    // ==========================================
    // 生命周期代理方法 (供 Activity 调用)
    // ==========================================

    public void onResume() {
        if (mediaPlayer != null && isPrepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void onPause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void onDestroy() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPrepared = false;
    }
}