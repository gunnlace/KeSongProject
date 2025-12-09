package com.example.kesongproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class DetailImageAdapter extends RecyclerView.Adapter<DetailImageAdapter.ImageViewHolder> {

    private List<FeedResponse.Clip> clips;

    public DetailImageAdapter(List<FeedResponse.Clip> clips) {
        this.clips = clips;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 这里为了简单，我们直接动态创建一个 ImageView，不用新建 xml 了
        ImageView imageView = new ImageView(parent.getContext());
        // 关键：填满父容器
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        // 关键：按比例裁切充满 (CenterCrop)
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        // 背景设为浅灰，作为加载时的占位
        imageView.setBackgroundColor(0xFFE0E0E0);
        return new ImageViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String url = clips.get(position).url;

        // 使用 Glide 加载
        Glide.with(holder.itemView.getContext())
                .load(url)
                // 可以在这里加 .placeholder() 实现片段级加载态
                // .error() 实现失败态
                .into((ImageView) holder.itemView);
    }

    @Override
    public int getItemCount() {
        return clips == null ? 0 : clips.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}