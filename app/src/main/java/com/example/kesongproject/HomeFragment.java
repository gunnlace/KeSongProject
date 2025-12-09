package com.example.kesongproject;

//import android.app.ActivityOptions;
import androidx.core.app.ActivityOptionsCompat;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.content.Intent;
import android.app.Activity;


//public class HomeFragment extends Fragment implements FeedUpdateCenter.OnPostUpdateListener {
public class HomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private NoteAdapter noteAdapter;
    private List<FeedResponse.Post> postList = new ArrayList<>();
    // 控件
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyView;
    private ProgressBar bottomLoadingBar;

    private ActivityResultLauncher<Intent> detailLauncher; //点赞数回力镖发射器

    // 状态标记
    private boolean isLoading = false; // 是否正在加载中

    // 标记：是否是第一次强制失败（用于测试空态页）
    private boolean isFirstForceFail = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        // 向中转站注册自己
//        FeedUpdateCenter.register(this);

        // 2. 注册发射器 (必须在 onCreate 里注册)
        detailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 这里是“回力标”飞回来的地方！
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // 拿到详情页传回来的 新post
                        FeedResponse.Post updatedPost = (FeedResponse.Post) result.getData().getSerializableExtra("post_data");

                        if (updatedPost != null) {
                            // 3. 更新列表里的旧数据
                            // 我们需要知道是第几个变了，这里简单点，遍历 ID 找
                            for (int i = 0; i < postList.size(); i++) {
                                if (postList.get(i).postId.equals(updatedPost.postId)) {
                                    // 加个 Log
                                    android.util.Log.d("HomeFragment", "找到目标了！正在更新第 " + i + " 条数据");
                                    // 替换成新的
                                    postList.set(i, updatedPost);
                                    // 局部刷新那个格子 (比 notifyDataSetChanged 高效)
                                    noteAdapter.notifyItemChanged(i);
                                    break;
                                }
                            }
                        }
                    }
                }
        );
    }

//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        // 注销中转站，否则会内存泄漏 (Fragment死了但还被中转站抓着不放)
//        FeedUpdateCenter.unregister(this);
//    }

//    @Override
//    public void onPostUpdated(FeedResponse.Post updatedPost) {
//        if (postList == null || noteAdapter == null) return;
//
//        // 遍历列表，找到 ID 一样的那个旧数据，替换掉
//        for (int i = 0; i < postList.size(); i++) {
//            if (postList.get(i).postId.equals(updatedPost.postId)) {
//                // 更新数据源
//                postList.set(i, updatedPost);
//
//                // 【重点】直接刷新！
//                // 哪怕现在 HomeFragment 在后台不可见，RecyclerView 也会更新缓存。
//                // 等你滑回来时，它已经是新的了。
//                noteAdapter.notifyItemChanged(i);
//                break;
//            }
//        }
//    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 绑定控件
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        emptyView = view.findViewById(R.id.empty_view);
        bottomLoadingBar = view.findViewById(R.id.bottom_loading_bar);

        // 2. 初始化 RecyclerView
        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        recyclerView.setLayoutManager(layoutManager);

//        noteAdapter = new NoteAdapter(postList);
        noteAdapter = new NoteAdapter(postList, (position, post, sharedElement) -> {
            // 当 Adapter 里的卡片被点击时，执行这里：
            Intent intent = new Intent(getContext(), NoteDetailActivity.class);
            intent.putExtra("post_data", post);
            // 创建转场动画选项，这里的 transitionName 必须和 Adapter 里设置的一模一样
//            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    getActivity(),
                    sharedElement,
                    "post_img_" + post.postId // 名字要对上
            );
            detailLauncher.launch(intent, options);
        });

        recyclerView.setAdapter(noteAdapter);

        // 3. 设置下拉刷新 (Pull to Refresh)
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadData(true); // true 代表是刷新动作
        });

        // 4. 设置上滑加载更多 (Load More)
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // 只有向下滑动时才检测 (dy > 0)
                if (dy > 0 && !isLoading) {

                    // 1. 获取布局管理器
                    StaggeredGridLayoutManager layoutManager =
                            (StaggeredGridLayoutManager) recyclerView.getLayoutManager();

                    if (layoutManager != null) {
                        // 2. 获取所有列中，最后可见的 item 位置
                        // 因为是瀑布流，底部可能是参差不齐的，左边可能显示到第 10 个，右边显示到第 11 个。所以返回一个数组 [10, 11]。
                        int[] lastVisiblePositions = null;
                        lastVisiblePositions = layoutManager.findLastVisibleItemPositions(null);

                        // 3. 找出最大的那个位置 (也就是界面上能看到的最底下的那个格子)
                        int lastVisibleItemPosition = 0;
                        if (lastVisiblePositions != null) {
                            for (int pos : lastVisiblePositions) {
                                lastVisibleItemPosition = Math.max(lastVisibleItemPosition, pos);
                            }
                        }

                        // 4. 获取总数据量
                        int totalItemCount = layoutManager.getItemCount();

                        // 5. 核心判断：如果 (总数 - 当前看到的最后位置) <= 6，说明剩余不足6个了
                        // 这里的 6 就是你的"预加载阈值"
                        if ((totalItemCount - lastVisibleItemPosition) <= 6) {
                            loadData(false); // 触发加载更多
                        }
                    }
                }
            }
        });

        // 5. 点击空态页重试
        emptyView.setOnClickListener(v -> {
            swipeRefreshLayout.setRefreshing(true); // 显示转圈
            loadData(true); // 重新发起刷新
        });

        // 6. 首次进入自动刷新
        swipeRefreshLayout.setRefreshing(true);
        loadData(true);
    }

    /**
     * 核心加载数据方法
     * @param isRefresh true=下拉刷新(重置数据), false=上滑加载(追加数据)
     */
    private void loadData(boolean isRefresh) {
        if (isLoading) return;
        isLoading = true;

        if (!isRefresh) {
            bottomLoadingBar.setVisibility(View.VISIBLE);
        }

        //首次强制失败，展示失败空态页面
        if (isRefresh && isFirstForceFail) {
            // 模拟延迟 1 秒后报失败
            new Handler().postDelayed(() -> {
                isFirstForceFail = false;
                Toast.makeText(getContext(), "测试：首次强制失败", Toast.LENGTH_SHORT).show();
                if (postList.isEmpty()) {
                    toggleEmptyView(true);
                }
                finishLoading();
            }, 1000);

            return; // 直接返回，不执行后面的 Retrofit 请求
        }

        // 1. 拿到接口实例
        ApiService apiService = RetrofitClient.getApiService();

        // 2. 创建请求 (count, 暂时不支持视频 false)
        Call<FeedResponse> call = apiService.getFeed(24, false);

        // 3. 发送异步请求 (enqueue)
        call.enqueue(new Callback<FeedResponse>() {
            @Override
            public void onResponse(Call<FeedResponse> call, Response<FeedResponse> response) {
                // === 请求成功 ===
                if (response.isSuccessful() && response.body() != null) {
                    FeedResponse data = response.body();

                    if (data.postList != null) {

                        // --- 加工数据，加入点赞数 ---
                        for (FeedResponse.Post post : data.postList) {
                            // JSON里无点赞数据，模拟点赞数 (范围 0~1000)
                            if (post.likeCount == 0) { // 防止刷新时把已经生成的数重置了
                                post.likeCount = new java.util.Random().nextInt(1000);
                            }

                            // 2. 从本地磁盘恢复点赞状态
                            post.isLiked = LikeManager.isLiked(getContext(), post.postId);

                            // 3. 如果本地已点赞，为了逻辑自洽，把随机数+1
                            if (post.isLiked) {
                                post.likeCount++;
                            }
                        }

                        if (isRefresh) {
                            postList.clear();
                            // 第一次加载成功，关闭空态页
                            toggleEmptyView(false);
                        }

                        // 添加数据到列表
                        postList.addAll(data.postList);
                        noteAdapter.notifyDataSetChanged();
                    }
                } else {
                    // 服务器回了，但可能是 404 或 500
                    Toast.makeText(getContext(), "服务器异常", Toast.LENGTH_SHORT).show();
                    if (postList.isEmpty()) toggleEmptyView(true);
                }

                // 收尾工作
                finishLoading();
            }

            @Override
            public void onFailure(Call<FeedResponse> call, Throwable t) {
                // === 请求失败 (断网了，或地址错了) ===
                Toast.makeText(getContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                if (postList.isEmpty()) toggleEmptyView(true);

                // 收尾工作
                finishLoading();
            }
        });
    }

    // 封装一个收尾方法，避免重复写
    private void finishLoading() {
        isLoading = false;
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if (bottomLoadingBar != null) {
            bottomLoadingBar.setVisibility(View.GONE);
        }
    }

    // 控制空态页显示/隐藏
    private void toggleEmptyView(boolean show) {
        if (show) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}