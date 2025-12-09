package com.example.kesongproject;

import android.os.Handler;
import android.os.Looper;
import androidx.viewpager2.widget.ViewPager2;

/**
 * 专门负责自动轮播的控制器
 * 职责：控制 ViewPager2 自动翻页，处理手势打断
 */
public class CarouselController {

    private final ViewPager2 viewPager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable carouselRunnable;

    private boolean isUserTouched = false; // 用户是否手动打断过
    private static final int INTERVAL = 1500; // 轮播间隔 3秒

    public CarouselController(ViewPager2 viewPager) {
        this.viewPager = viewPager;
        initScrollListener();
    }

    // 初始化监听用户手势
    private void initScrollListener() {
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                // 如果用户正在拖拽 (STATE_DRAGGING = 1)
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    isUserTouched = true; // 标记打断
                    stop(); // 立即停止
                }
            }
        });
    }

    /**
     * 尝试开始轮播
     * (内部会检查各种条件：是否静音、是否单图、是否被打断)
     */
    public void start() {
        // 1. 基础检查：如果是静音，或者用户手动拖过，或者只有1张图，就不播
        if (MusicStatusManager.isMuted() || isUserTouched) {
            return;
        }
        if (viewPager.getAdapter() == null || viewPager.getAdapter().getItemCount() <= 1) {
            return;
        }

        // 2. 移除旧任务，防止叠加
        stop();

        // 3. 定义任务
        carouselRunnable = new Runnable() {
            @Override
            public void run() {
                int current = viewPager.getCurrentItem();
                int total = viewPager.getAdapter().getItemCount();
                int next = (current + 1) % total; // 取模运算实现循环：到了最后一张变0

                // 平滑滚动
                viewPager.setCurrentItem(next, true);

                // 预约下一次
                handler.postDelayed(this, INTERVAL);
            }
        };

        // 4. 发车！
        handler.postDelayed(carouselRunnable, INTERVAL);
    }

    /**
     * 停止轮播
     */
    public void stop() {
        if (carouselRunnable != null) {
            handler.removeCallbacks(carouselRunnable);
        }
    }

    /**
     * 重置用户打断状态 (用于点击“取消静音”时强制恢复轮播)
     */
    public void resetUserTouch() {
        this.isUserTouched = false;
    }

    // --- 生命周期代理 ---

    public void onResume() {
        start(); // 回到前台尝试开始
    }

    public void onPause() {
        stop(); // 退后台必须停
    }

    public void onDestroy() {
        stop(); // 销毁防内存泄漏
    }
}