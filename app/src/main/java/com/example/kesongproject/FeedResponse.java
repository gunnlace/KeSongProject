package com.example.kesongproject;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

/**
 * 对应服务器返回的最外层大括号
 */
public class FeedResponse {

    @SerializedName("status_code")
    public int statusCode; // 状态码

    @SerializedName("has_more")
    public int hasMore;    // 是否有更多

    @SerializedName("post_list")
    public List<Post> postList; // 作品列表

    /**
     * 对应 JSON 里的 "post_list" 中的每一项
     */
    public static class Post implements Serializable {
        @SerializedName("post_id")
        public String postId;

        @SerializedName("title")
        public String title;

        @SerializedName("content")
        public String content;

        @SerializedName("create_time")
        public long createTime;

        @SerializedName("author")
        public Author author;

        @SerializedName("clips")
        public List<Clip> clips;

        @SerializedName("music")
        public Music music;

        @SerializedName("hashtag") // JSON里叫 hashtag
        public List<Hashtag> hashtags; // 我们定义为 List

        public int likeCount;    // 点赞数 (服务器没给)
        public boolean isLiked;  // 是否已点赞 (服务器没给，存本地)
    }

    /**
     * 对应 JSON 里的 "author"
     */
    public static class Author implements Serializable {
        @SerializedName("user_id")
        public String userId;

        @SerializedName("nickname")
        public String nickname;

        @SerializedName("avatar")
        public String avatar;
    }

    /**
     * 对应 JSON 里的 "clips" 数组项 (图片/视频)
     */
    public static class Clip implements Serializable {
        @SerializedName("type")
        public int type; // 0:图片, 1:视频

        @SerializedName("width")
        public int width;

        @SerializedName("height")
        public int height;

        @SerializedName("url")
        public String url;
    }

    /**
     * 对应 JSON 里的 "music"
     */
    public static class Music implements Serializable {
        @SerializedName("volume")
        public int volume;

        @SerializedName("seek_time")
        public int seekTime;

        @SerializedName("url")
        public String url;
    }

    public static class Hashtag implements Serializable {
        @SerializedName("start")
        public int start; // 起始下标

        @SerializedName("end")
        public int end;   // 结束下标
    }
}