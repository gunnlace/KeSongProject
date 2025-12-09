package com.example.kesongproject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class DragFloatLayout extends FrameLayout {

    private Activity activity;
    private View dragView; // 要拖动的视图 (通常是图片容器)
    private View backgroundView; // 要变透明的背景 (通常是详情页的白色底)

    private float downX, downY;
    private boolean isDragging = false;
    private final int DRAG_THRESHOLD = 40; // 拖动阈值

    public DragFloatLayout(Context context) { super(context); }
    public DragFloatLayout(Context context, AttributeSet attrs) { super(context, attrs); }

    // 初始化：把 Activity 传进来，方便我们 finish 它
    public void attachToActivity(Activity activity, View dragView, View backgroundView) {
        this.activity = activity;
        this.dragView = dragView;
        this.backgroundView = backgroundView;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // 简单的拦截逻辑：如果是大幅度向下或向右滑，就拦截
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                isDragging = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = ev.getY() - downY;
                float dx = ev.getX() - downX;
                // 如果向下滑动超过阈值，认为是在做“拖拽返回”
                if (dy > DRAG_THRESHOLD && Math.abs(dy) > Math.abs(dx)) {
                    isDragging = true;
                    return true; // 拦截事件，不让子 View (如 ViewPager) 处理了
                }
                break;
        }
        return isDragging;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isDragging) return super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float translationY = event.getY() - downY;
                float translationX = event.getX() - downX;

                // 1. 只有向下滑才动
                if (translationY > 0) {
                    // 移动 View
                    dragView.setTranslationX(translationX);
                    dragView.setTranslationY(translationY);

                    // 2. 缩放 (滑得越远越小，最小 0.5)
                    float percent = Math.min(1, translationY / 800f); // 假设滑800px到底
                    float scale = 1 - percent * 0.5f;
                    dragView.setScaleX(scale);
                    dragView.setScaleY(scale);

                    // 3. 背景变透明 (蒙层由亮变暗淡)
                    // 255 是不透明，0 是全透明
                    int alpha = (int) (255 * (1 - percent));
                    if (backgroundView != null && backgroundView.getBackground() != null) {
                        // 这里假设背景是纯白，实际上可能是 XML 背景
                        backgroundView.getBackground().mutate().setAlpha(alpha);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                float currentY = event.getY() - downY;

                // 4. 判断是“回弹”还是“关闭”
                if (currentY > 300) {
                    // 滑动距离够大 -> 触发系统转场退出
                    if (activity != null) {
                        activity.finishAfterTransition(); // 这一句会触发“缩小回列表”的动画
                    }
                } else {
                    // 滑动距离不够 -> 弹回去
                    dragView.animate().translationX(0).translationY(0).scaleX(1).scaleY(1).setDuration(200).start();
                    if (backgroundView != null && backgroundView.getBackground() != null) backgroundView.getBackground().mutate().setAlpha(255);
                }
                break;
        }
        return true;
    }
}