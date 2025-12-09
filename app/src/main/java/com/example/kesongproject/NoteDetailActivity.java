package com.example.kesongproject;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.List;

public class NoteDetailActivity extends AppCompatActivity {

    private TextView btnFollow; // 新增变量
    private boolean isFollowed = false; // 当前关注状态

    private ImageView ivBottomLike;
    private TextView tvBottomLikeCount;
    private View llBottomLike;
    private FeedResponse.Post post;

    private androidx.viewpager2.widget.ViewPager2 vpImages;
    private android.widget.LinearLayout llIndicators;
    private TextView tvContent;
    private TextView tvDate;
    private MediaPlayer mediaPlayer;
    private ImageView ivMuteSwitch;//静音按钮
    private MusicController musicController;
    private CarouselController carouselController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);
        supportPostponeEnterTransition();

        // 推迟进入动画 (等图片加载好了再说)
        supportPostponeEnterTransition();

        // 接收传递过来的对象
        post = (FeedResponse.Post) getIntent().getSerializableExtra("post_data");

        // 判空处理 (防止跳转逻辑写错了导致崩盘)
        if (post == null) {
            finish(); // 关闭页面
            return;
        }

        // 找控件
        ImageView ivBack = findViewById(R.id.btn_back);
//        ImageView ivImage = findViewById(R.id.iv_detail_image);
        vpImages = findViewById(R.id.vp_detail_images);
        llIndicators = findViewById(R.id.ll_indicator_container);

        TextView tvTitle = findViewById(R.id.tv_detail_title);
        ImageView ivAvatar = findViewById(R.id.iv_user_avatar);
        TextView tvUserName = findViewById(R.id.tv_user_name);
        btnFollow = findViewById(R.id.btn_follow);//关注按钮

        ivBottomLike = findViewById(R.id.iv_bottom_like);
        tvBottomLikeCount = findViewById(R.id.tv_bottom_like_count);
        llBottomLike = findViewById(R.id.ll_bottom_like);

        tvContent = findViewById(R.id.tv_detail_content);
        tvDate = findViewById(R.id.tv_detail_date);

        ivMuteSwitch = findViewById(R.id.iv_mute_switch);

        View llBottomShare = findViewById(R.id.ll_bottom_share);
        llBottomShare.setOnClickListener(v -> {
            // 弹出分享底部栏
            new ShareFragment().show(getSupportFragmentManager(), "ShareFragment");
        });

        tvTitle.setText(post.title);

        // 初始化轮播控制器
        if (post.clips != null && post.clips.size() > 1) {
            // 初始化 Adapter
            DetailImageAdapter imageAdapter = new DetailImageAdapter(post.clips);
            vpImages.setAdapter(imageAdapter);
            carouselController = new CarouselController(vpImages);
        }
        // 初始化音乐逻辑
        if (post.music != null) {
            musicController = new MusicController(this, ivMuteSwitch);
            // 【联动核心】监听静音状态
            musicController.setOnMuteStateChangeListener(isMuted -> {
                if (carouselController == null) return;

                if (isMuted) {
                    // 静音 -> 停止轮播
                    carouselController.stop();
                } else {
                    // 取消静音 -> 1.原谅用户的打断 2.恢复轮播
                    carouselController.resetUserTouch();
                    carouselController.start();
                }
            });

            musicController.init(post.music.url);
        }

        // 使用 LikeController 更新初始 UI
        LikeController.updateLikeUI(ivBottomLike, tvBottomLikeCount, post);

        // 使用 LikeController 处理点击
        findViewById(R.id.ll_bottom_like).setOnClickListener(v -> {
            LikeController.handleLikeClick(this, post, ivBottomLike, tvBottomLikeCount);
        });

        if (post.clips != null && !post.clips.isEmpty()) {
            // === 计算容器高度 (基于首图) ===
            FeedResponse.Clip firstClip = post.clips.get(0);
            float ratio = 1.0f;
            if (firstClip.width > 0 && firstClip.height > 0) {
                ratio = (float) firstClip.width / firstClip.height;
            }
            // 限制比例范围：
            // 3:4 = 0.75, 16:9 = 1.77
            // 如果比 0.75 还瘦，就强行变成 0.75 (两边留黑或者裁切) -> 这里我们选裁切充满
            // 如果比 1.77 还扁，就强行 1.77
            ratio = Math.max(0.75f, Math.min(1.77f, ratio));
            // 动态修改 ConstraintLayout 的 dimensionRatio
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) vpImages.getLayoutParams();
            params.dimensionRatio = String.valueOf(ratio); // 例如 "0.75"
            vpImages.setLayoutParams(params);
            // === 设置适配器 ===
            DetailImageAdapter imageAdapter = new DetailImageAdapter(post.clips);
            vpImages.setAdapter(imageAdapter);
            // === 生成指示器 (只有多图时才显示) ===
            if (post.clips.size() > 1) {
                llIndicators.setVisibility(View.VISIBLE);
                setupIndicators(post.clips.size());

                // 监听滑动，联动指示器
                vpImages.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        updateIndicatorStatus(position);
                    }
                });
            } else {
                llIndicators.setVisibility(View.GONE); // 单图隐藏
            }

            // 给 ViewPager 设置一模一样的 TransitionName
            vpImages.setTransitionName("post_img_" + post.postId);
            // 等 ViewPager 渲染完第一帧，再开始动画
            vpImages.post(() -> {
                supportStartPostponedEnterTransition();
            });
        } else {
            // 如果没图，直接开始，不然页面会卡死在空白
            supportStartPostponedEnterTransition();
        }

        // 2. 处理正文高亮逻辑
        if (post.content != null) {
            // 如果有 hashtag 数据，才处理；否则直接显示纯文本
            if (post.hashtags != null && !post.hashtags.isEmpty()) {
                setSpannableContent(post.content, post.hashtags);
            } else {
                tvContent.setText(post.content);
            }
        }

        //设置日期
        if (post.createTime > 0) {
            String timeStr = TimeUtils.getFriendlyTimeSpanByNow(post.createTime);
            tvDate.setText(timeStr);
        } else {
            tvDate.setVisibility(View.GONE); // 如果没时间，就隐藏
        }

        // 作者名
        if (post.author != null) {
            tvUserName.setText(post.author.nickname);

            // 头像
            if (post.author.avatar != null && !post.author.avatar.isEmpty()) {
                Glide.with(this)
                        .load(post.author.avatar)
                        .circleCrop()
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
            }
        }


        //判断是否关注了作者
        // 获取作者ID (用来做 Key)，假设 post.author.userId 是唯一的
        String authorId = "";
        if (post != null && post.author != null) {
            authorId = post.author.userId;
        }
        // 从本地读取：我之前关注过这个作者吗？
        isFollowed = FollowManager.isFollowed(this, authorId);
        updateFollowUI(); // 刷新一下按钮样子
        //点击事件
        String finalAuthorId = authorId; // Lambda 里需要 final 变量
        btnFollow.setOnClickListener(v -> {
            // 切换状态
            isFollowed = !isFollowed;
            // 保存状态
            FollowManager.setFollowed(this, finalAuthorId, isFollowed);
            // 更新 UI
            updateFollowUI();
            // 提示用户
            String toastMsg = isFollowed ? "已关注" : "已取消关注";
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
        });

        // 点击返回
        ivBack.setOnClickListener(v -> {
            // 手动调用 finish，触发 onBackPressed 逻辑
            getOnBackPressedDispatcher().onBackPressed();
        });
        // 配置拖拽退出
        DragFloatLayout dragLayout = findViewById(R.id.drag_layout);
        View contentRoot = findViewById(R.id.cl_content_root); // 整个白色背景的内容

        // 我们希望拖动整个页面 (contentRoot)
        // 这里的逻辑是：手指在屏幕上滑，contentRoot 跟着动，背景渐渐消失
        dragLayout.attachToActivity(this, contentRoot, contentRoot);
    }

    @Override
    public void finish() {
        // 准备要还回去的数据
        Intent intent = new Intent();
        intent.putExtra("post_data", post); // 把改过的 post 放进去

        // 设置结果为 OK
        setResult(RESULT_OK, intent);
        super.finish(); // 真正的关闭页面
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (musicController != null) musicController.onResume();
        if (carouselController != null) carouselController.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (musicController != null) musicController.onPause();
        if (carouselController != null) carouselController.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (musicController != null) musicController.onDestroy();
        if (carouselController != null) carouselController.onDestroy();
    }

    // 封装 UI 更新方法
    private void updateFollowUI() {
        if (isFollowed) {
            // === 已关注状态 ===
            btnFollow.setText("已关注");
            btnFollow.setTextColor(Color.parseColor("#333333")); // 深灰字
            btnFollow.setBackgroundResource(R.drawable.bg_follow_btn_gray); // 灰框
        } else {
            // === 未关注状态 ===
            btnFollow.setText("关注");
            btnFollow.setTextColor(Color.parseColor("#FF2442")); // 红字
            btnFollow.setBackgroundResource(R.drawable.bg_follow_btn_red); // 红框
        }
    }
    // 初始化指示器小条条
    private void setupIndicators(int count) {
        llIndicators.removeAllViews();
        for (int i = 0; i < count; i++) {
            View view = new View(this);
            // 默认样式：灰色、短条
            android.widget.LinearLayout.LayoutParams params =
                    new android.widget.LinearLayout.LayoutParams(0, 8, 1.0f); // 宽30px 高6px
            params.setMargins(4, 0, 4, 0);
            view.setLayoutParams(params);
            view.setBackgroundColor(0x80FFFFFF); // 半透明白
            llIndicators.addView(view);
        }
        updateIndicatorStatus(0); // 默认点亮第0个
    }

    // 更新指示器高亮状态
    private void updateIndicatorStatus(int selectedPosition) {
        for (int i = 0; i < llIndicators.getChildCount(); i++) {
            View view = llIndicators.getChildAt(i);
            if (i == selectedPosition) {
                view.setBackgroundColor(0xFFFFFFFF); // 选中：纯白
            } else {
                view.setBackgroundColor(0x80FFFFFF); // 未选中：半透明白
            }
        }
    }

    // 设置富文本
    private void setSpannableContent(String content, List<FeedResponse.Hashtag> hashtags) {
        SpannableString spannableString = new SpannableString(content);

        for (FeedResponse.Hashtag tag : hashtags) {
            // 保护机制：防止后端给的下标越界导致 App 崩
            if (tag.start < 0 || tag.end > content.length() || tag.start >= tag.end) {
                continue;
            }

            // 提取话题文字 (比如 "#快来参与")
            String tagName = content.substring(tag.start, tag.end);

            // 创建点击跨度 (ClickableSpan)
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    // 点击跳转到话题页
                    Intent intent = new Intent(NoteDetailActivity.this, HashtagActivity.class);
                    intent.putExtra("tag_name", tagName);
                    startActivity(intent);
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    // 设置样式
                    ds.setColor(Color.parseColor("#3F51B5")); // 深蓝色高亮
                    ds.setUnderlineText(false); // 去掉下划线 (更美观)
                }
            };

            // 将跨度应用到指定范围
            // Spanned.SPAN_EXCLUSIVE_EXCLUSIVE 表示前后输入新字时不继承这个效果
            spannableString.setSpan(clickableSpan, tag.start, tag.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // 设置给 TextView
        tvContent.setText(spannableString);
        tvContent.setMovementMethod(LinkMovementMethod.getInstance());
        // 设置点击后的背景色为透明 (防止点一下变灰块)
        tvContent.setHighlightColor(Color.TRANSPARENT);
    }
}

