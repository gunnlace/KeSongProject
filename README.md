## 项目名称：KeSongProject 可颂客户端

### 1. 实现方案与框架介绍

本章节主要阐述项目的整体架构设计、技术选型以及核心模块的实现原理。

#### 1.1 整体架构
* **开发语言**：Java
* **架构模式**：采用标准的 Android **MVC** (Model-View-Controller) 分层架构，逻辑清晰，职责分离。
    * **Model**: 数据层 (`FeedResponse`, `Post` 等实体类)，负责数据的解析与封装。
    * **View**: UI 层 (`xml` 布局, `Activity`, `Fragment`)，负责界面展示与用户交互。
    * **Controller**: 逻辑控制层 (`NoteAdapter`, `MusicController`, `CarouselController`)，负责业务逻辑处理与状态分发。

#### 1.2 核心技术栈
* **网络请求**: 使用 **Retrofit2** 进行网络请求封装，实现高效的数据获取与 RESTful API 对接。
* **图片加载**: 集成 **Glide** 图片加载库，实现图片的高效缓存、圆角处理 (`RoundedCorners`) 及圆形头像裁剪 (`CircleCrop`)。
* **列表渲染**:
    * 首页使用 **RecyclerView** 配合 **StaggeredGridLayoutManager** 实现高性能的双列瀑布流布局。
    * 详情页使用 **ViewPager2** 实现多图横向轮播体验。
* **下拉刷新**: 使用 **SwipeRefreshLayout** 实现标准的下拉刷新交互。

#### 1.3 关键技术亮点
* **数据双向同步机制**: 采用 `ActivityResultLauncher` 结合接口回调，实现了详情页点赞状态与首页列表的实时无缝同步。
```java
// HomeFragment.java
// 使用ActivityResultLauncher来监听 详情页 的返回结果
detailLauncher = registerForActivityResult(
    new ActivityResultContracts.StartActivityForResult(),
    result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            // 1. 获取回传的最新 Post 对象
            FeedResponse.Post updatedPost = (FeedResponse.Post) result.getData()
                .getSerializableExtra("post_data");
            
            // 2. 遍历查找并更新本地列表
            for (int i = 0; i < postList.size(); i++) {
                if (postList.get(i).postId.equals(updatedPost.postId)) {
                    postList.set(i, updatedPost);
                    // 3. 仅刷新发生变化的 Item，避免全屏闪烁
                    noteAdapter.notifyItemChanged(i);
                    break;
                }
            }
        }
    }
);
```
* **沉浸式转场动画**: 利用 Android 5.0+ **Shared Element Transition** (共享元素转场)，实现了从列表封面到详情页大图的丝滑放大过渡，并配合 `postponeEnterTransition` 解决网络图片加载延迟导致的动画闪烁问题。
```java
//在实现共享元素转场时，考虑到 ViewPager2 加载大图需要时间，我使用了 supportPostponeEnterTransition 暂时挂起动画，直到图片渲染完成后再开始，确保转场丝滑无缝
// NoteDetailActivity.java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // 1. 挂起进场动画
    supportPostponeEnterTransition();

    // ... 省略数据加载逻辑 ...

    if (post.clips != null) {
        // ... 设置 Adapter ...
        
        // 2. 设置共享元素名称 (需与列表页保持一致)
        vpImages.setTransitionName("post_img_" + post.postId);

        // 3. 等待 View 渲染完毕后，开启转场动画
        vpImages.post(() -> {
            supportStartPostponedEnterTransition();
        });
    }
}
```


* **手势交互系统**: 自定义 `DragFloatLayout` 容器，拦截触摸事件，实现了类似小红书/Instagram 的“下拉/右滑”跟随手势退场功能，包含背景渐变透明与视图缩放效果。

```java
//DragFloatLayout.java
@Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // 简单的拦截逻辑：如果是大幅度向下滑，就拦截
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
                    if (backgroundView != null && backgroundView.getBackground() != null){
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
```

* **组件化控制器设计**: 将音乐播放 (`MusicController`) 和自动轮播 (`CarouselController`) 逻辑从 Activity 中剥离，实现了高内聚、低耦合的代码结构，便于维护与复用。

```java
// MusicController.java 通过接口回调与 Activity 进行通信
public class MusicController {
    // 定义状态回调接口
    public interface OnMuteStateChangeListener {
        void onMuteStateChanged(boolean isMuted);
    }

    public void init(String url) {
        // ... 省略初始化代码 ...
        
        ivMuteSwitch.setOnClickListener(v -> {
            boolean newMute = !MusicStatusManager.isMuted();
            // 触发回调通知 Activity
            if (muteListener != null) {
                muteListener.onMuteStateChanged(newMute);
            }
        });
    }
}
```

* **视听联动与智能轮播控制**:
  设计了基于 `Handler` 的 `CarouselController` 实现多图自动轮播，并建立了“静音状态”与“轮播状态”的强关联。
  实现了复杂的交互调度逻辑：当用户开启静音时，自动暂停轮播以减少视觉干扰；当用户开启声音时，自动重置用户打断标记并恢复轮播。同时，监听 `ViewPager2` 的手势状态，确保用户在手动拖拽查看图片时自动暂停轮播，松手后根据声音状态智能恢复，极大提升了沉浸式浏览体验。

> 为了提升沉浸感，我将视觉轮播与听觉状态进行了绑定。通过监听 `MusicController` 的静音状态回调，实时控制 `CarouselController` 的启停，并处理了用户手动触摸打断后的逻辑恢复：

```java
// NoteDetailActivity.java (视听联动核心逻辑)
musicController.setOnMuteStateChangeListener(isMuted -> {
    // 判空保护
    if (carouselController != null) {
        if (isMuted) {
            // 场景A：静音 -> 停止视觉轮播 (降低干扰)
            carouselController.stop();
        } else {
            // 场景B：取消静音 -> 恢复视觉轮播
            // 关键点：重置用户的"手动触摸"标记，强制给用户一个重新开始轮播的机会
            carouselController.resetUserTouch();
            carouselController.start();
        }
    }
});
```




### 2. 完成功能列表

本章节详细列出项目已实现的业务功能模块。

#### 2.1 首页
* **双列瀑布流**: 支持不同高度图片的自适应展示，界面错落有致。
* **下拉刷新 & 上滑加载**: 支持手势下拉重置列表，触底自动加载更多数据（分页逻辑）。
* **状态管理**: 实现了加载中 (Loading)、加载失败 (Error)、空数据 (Empty) 的多状态页面切换。
* **点赞交互**: 列表页直接支持点赞/取消点赞，即时更新 UI 及数字。

#### 2.2 详情页
* **多图轮播**: 支持多张图片横向滑动，配备自适应宽度的指示器 (Indicator)。
* **自动播放**: 支持多图场景下的自动轮播，且具备防打断机制（手指按住暂停，松开恢复）。
* **背景音乐**:
    * 支持解析并播放 Feed 携带的背景音乐。
    * 实现全局静音状态记忆（App 生命周期内有效）。
    * 实现静音按钮与自动轮播的联动控制（静音暂停轮播，开启声音恢复轮播）。
    * 生命周期管理：退后台自动暂停，回前台自动恢复。
* **富文本正文**: 支持 Hashtag (#话题) 高亮解析，点击话题可跳转至话题详情页。
* **发布时间格式化**: 根据时间差自动格式化为“刚刚”、“几小时前”、“昨天”、“MM-dd”等友好格式。

#### 2.3 交互与动画
* **共享元素转场**: 点击卡片图片放大进入详情页，返回时缩小归位。
* **拖拽关闭 **: 在详情页通过下滑或右滑手势，可动态缩小页面并关闭，背景随拖拽距离渐变。
* **关注逻辑**: 支持对作者的关注/取消关注，并持久化保存状态。

---

### 3. 个人思考与总结

本章节记录开发过程中的难点攻克、设计考量及对未来的优化展望。

#### 3.1 遇到的挑战与解决方案
* **难点一：列表与详情页的数据一致性**
    * *问题描述*：用户在详情页点赞后返回，首页列表的点赞状态未更新，导致数据“割裂”。
    * *解决方案*：放弃了传统的 `startActivity`，改用 `registerForActivityResult`。在详情页 `finish` 时将最新的 Post 对象回传，首页接收后精准刷新对应 Item，实现了完美的闭环同步。
* **难点二：ViewPager2 与下拉手势的冲突**
    * *问题描述*：在详情页横滑查看图片时，容易误触自定义的下拉关闭手势。
    * *解决方案*：在 `DragFloatLayout` 的 `onInterceptTouchEvent` 中加入方向判断逻辑，只有当垂直滑动距离远大于水平滑动距离时才拦截事件，确保了横滑体验的流畅性。

#### 3.2 架构设计的思考
* 在开发音乐播放和轮播功能时，我最初将其代码直接写在 Activity 中，导致类极其臃肿。后来我引入了 **Controller 模式** (`MusicController`, `CarouselController`)，将业务逻辑抽离。这不仅让 Activity 代码简洁了很多，还使得生命周期管理更加清晰，极大地提高了代码的可读性和可维护性。

#### 3.3 不足与展望
* 目前版本为了稳定性回滚了视频播放功能。未来计划引入 Google 的 **ExoPlayer** 来支持更复杂的视频流媒体播放，并解决 RecyclerView 多布局复用时的视频状态管理问题。
* 计划引入本地数据库 (Room) 来缓存网络数据，实现离线模式下的浏览体验。



