package com.dovar.autoviewpagerDemo;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * 自动循环滑动的vp的适配器
 * Created by Administrator on 2016/10/8 0008.
 */
public class AutoViewPagerAdapter extends com.dovar.autoviewpager.AutoViewPagerAdapter<View> {

    public AutoViewPagerAdapter(Context mContext) {
        super(mContext);
    }

    public AutoViewPagerAdapter(Context mContext, List<View> views) {
        super(mContext);
        addDatas(views);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = getItem(position);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

}
