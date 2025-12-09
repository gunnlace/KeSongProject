package com.example.kesongproject;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HashtagActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hashtag);

        // 接收传递过来的话题名字
        String tagName = getIntent().getStringExtra("tag_name");

        TextView tvTitle = findViewById(R.id.tv_hashtag_title);
        if (tagName != null) {
            tvTitle.setText(tagName);
        }
        // 返回逻辑
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            finish(); // 关键代码：关闭当前页面，相当于按了手机物理返回键
        });
    }
}