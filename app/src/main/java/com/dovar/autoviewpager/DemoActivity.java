package com.dovar.autoviewpager;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DemoActivity extends AppCompatActivity {
    private List<View> list01;//数据
    private AutoViewPager mViewPager;//横向自动轮播

    private List<View> list02;//数据
    private HorizonVerticalViewPager viewPager_vertical;//纵向自动轮播

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        setViewPager();
    }


    /**
     * 添加数据
     */
    private void initdata() {
        List<Integer> mList = new ArrayList<>();
        mList.add(R.drawable.idcard3);
        mList.add(R.drawable.idcard1);
        mList.add(R.drawable.idcard2);
        mList.add(R.drawable.idcard3);
        mList.add(R.drawable.idcard1);
        list01 = new ArrayList<>();
        for (int i = 0; i < mList.size(); i++) {
            ImageView iv = new ImageView(this);
            iv.setImageResource(mList.get(i));
            iv.setScaleType(ImageView.ScaleType.FIT_XY);
            list01.add(iv);
        }

        List<String> listStr = new ArrayList<>();
        listStr.add("最高现金券1000元");
        listStr.add("最高现金券100元");
        listStr.add("最高现金券300元");
        listStr.add("最高现金券500元");
        listStr.add("最高现金券1000元");
        listStr.add("最高现金券100元");
        list02 = new ArrayList<>();
        for (int i = 0; i < listStr.size(); i++) {
            TextView textView = new TextView(this);
            textView.setTextColor(Color.WHITE);
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setSingleLine(true);
            textView.setText(listStr.get(i));
            list02.add(textView);
        }
    }

    /**
     * 配置viewPager自动轮播
     */
    public void setViewPager() {
        initdata();
        //初始化横向ViewPager
        mViewPager = (AutoViewPager) findViewById(R.id.viewpager01);
        AutoViewPagerAdapter adapter01 = new AutoViewPagerAdapter(this, list01);
        mViewPager.setAdapter(adapter01);
        configVP(mViewPager, 200);

        //初始化纵向Viewpager
        viewPager_vertical = (HorizonVerticalViewPager) findViewById(R.id.viewpager02);
        AutoViewPagerAdapter adapter02 = new AutoViewPagerAdapter(this, list02);
        viewPager_vertical.setAdapter(adapter02);
        configVP(viewPager_vertical, 500);
    }

    /**
     * 基本配置
     */
    private void configVP(AutoViewPager vp, int speed) {
        //修改viewpager滑动速度
        vp.setSpeed(speed);
        //设置自动跳转间隔
        vp.setTime(5000);
        //开始自动轮播
        vp.startTurn();
    }

}
