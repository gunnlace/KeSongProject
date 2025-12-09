package com.example.kesongproject;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.kesongproject.HomeFragment;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // 声明控件
    private TextView navHome;
    private TextView navMe;

    // 记录当前正在显示的 Fragment
    private Fragment mCurrentFragment;

    // 声明 Fragment 对象，避免重复创建
    private HomeFragment homeFragment = new HomeFragment();
    private MeFragment meFragment = new MeFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 找到底部按钮
        navHome = findViewById(R.id.nav_home);
        navMe = findViewById(R.id.nav_me);

        // 2. 设置点击监听器
        navHome.setOnClickListener(this);
        navMe.setOnClickListener(this);

        // 3. 默认选中“首页”
        if (savedInstanceState == null) {
            // 默认加载首页 Fragment
            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.fragment_container, homeFragment)
                    .add(R.id.fragment_container, homeFragment)
                    .commit();

            // 记录当前显示的是首页
            mCurrentFragment = homeFragment;
            // 更新按钮颜色状态
            updateTabStyle(true);
        }
    }

    // 处理点击事件
    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.nav_home) {
            // 点击了首页
            switchFragment(homeFragment);
            updateTabStyle(true); // 首页变黑，我变灰

        } else if (id == R.id.nav_me) {
            // 点击了我
            switchFragment(meFragment);
            updateTabStyle(false); // 首页变灰，我变黑
        }
    }

    // 封装一个切换 Fragment 的方法
//    private void switchFragment(Fragment fragment) {
//        getSupportFragmentManager().beginTransaction()
//                .replace(R.id.fragment_container, fragment)
//                .commit();
//    }
    private void switchFragment(Fragment targetFragment) {
        // 如果点击的是当前已经在显示的页面，不做任何操作
        if (targetFragment == mCurrentFragment) {
            return;
        }

        // 开启事务
        androidx.fragment.app.FragmentTransaction transaction =
                getSupportFragmentManager().beginTransaction();

        // 1. 先隐藏当前显示的页面
        if (mCurrentFragment != null) {
            transaction.hide(mCurrentFragment);
        }

        // 2. 判断目标页面是否已经添加过？
        if (!targetFragment.isAdded()) {
            // 如果没添加过（第一次点），就 add 进去
            // (add 只会执行一次 onCreateView)
            transaction.add(R.id.fragment_container, targetFragment);
        } else {
            // 如果已经添加过了，直接 show 出来
            // (show 不会重新执行 onCreateView，所以界面和数据都在)
            transaction.show(targetFragment);
        }

        // 3. 提交事务
        transaction.commit();

        // 4. 更新当前页面标记
        mCurrentFragment = targetFragment;
    }

    // 更新底部按钮的颜色样式 (选中变黑加粗，未选中变灰)
    private void updateTabStyle(boolean isHomeSelected) {
        if (isHomeSelected) {
            // 选中首页
            navHome.setTextColor(Color.parseColor("#161823")); // 黑
            navHome.getPaint().setFakeBoldText(true);          // 加粗

            navMe.setTextColor(Color.parseColor("#999999"));   // 灰
            navMe.getPaint().setFakeBoldText(false);           // 取消加粗
        } else {
            // 选中我
            navHome.setTextColor(Color.parseColor("#999999"));
            navHome.getPaint().setFakeBoldText(false);

            navMe.setTextColor(Color.parseColor("#161823"));
            navMe.getPaint().setFakeBoldText(true);
        }
    }
}