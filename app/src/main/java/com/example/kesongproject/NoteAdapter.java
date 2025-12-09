package com.example.kesongproject;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private List<FeedResponse.Post> mPostList;

    // 定义一个接口：点击监听器
    public interface OnPostClickListener {
        void onPostClick(int position, FeedResponse.Post post, View sharedElement);
    }

    // 持有一个监听器变量
    private OnPostClickListener mListener;

//    public NoteAdapter(List<FeedResponse.Post> postList) {
//        this.mPostList = postList;
//    }
    public NoteAdapter(List<FeedResponse.Post> postList, OnPostClickListener listener) {
        this.mPostList = postList;
        this.mListener = listener; // 记住谁在监听
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note_card, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        FeedResponse.Post post = mPostList.get(position);

        // 1. 标题
        holder.tvTitle.setText(post.title);

        // 2. 作者名 (在 author 对象里)
        if (post.author != null) {
            holder.tvName.setText(post.author.nickname);
        }

        //用 post_img_ + ID来设置唯一的共享元素名称
        holder.ivCover.setTransitionName("post_img_" + post.postId);

        // 3. 点赞
        // ================= 点赞逻辑开始 =================
        LikeController.updateLikeUI(holder.ivLike, holder.tvLikeCount, post);
        holder.ivLike.setOnClickListener(v -> {
            LikeController.handleLikeClick(v.getContext(), post, holder.ivLike, holder.tvLikeCount);
        });
        // ================= 点赞逻辑结束 =================

        // --- 跳转逻辑 ---
        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onPostClick(position, post, holder.ivCover);
            }
        });


        // 4. 封面图 (在 clips 数组的第一个里)
        // 需要判空，防止 clips 为空数组崩溃
        String coverUrl = "";
        if (post.clips != null && !post.clips.isEmpty()) {
            coverUrl = post.clips.get(0).url;
        }

        Glide.with(holder.itemView.getContext())
                .load(coverUrl)
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                .into(holder.ivCover);

        // 5. 头像 (在 author 对象里)
        String avatarUrl = "";
        if (post.author != null) {
            avatarUrl = post.author.avatar;
        }

        Glide.with(holder.itemView.getContext())
                .load(avatarUrl)
                .circleCrop()
                .placeholder(R.mipmap.ic_launcher_round)
                .into(holder.ivAvatar);

    }


    @Override
    public int getItemCount() {
        return mPostList == null ? 0 : mPostList.size();
    }

    // ViewHolder 内部类保持不变 (因为 XML 布局没变)
    static class NoteViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle;
        ImageView ivAvatar;
        TextView tvName;
        ImageView ivLike;
        TextView tvLikeCount;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            ivLike = itemView.findViewById(R.id.iv_like);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
        }
    }
}