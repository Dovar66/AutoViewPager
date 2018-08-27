package com.dovar.autoviewpager;

import android.content.Context;
import android.support.v4.view.PagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * @Date: 2018/8/27
 * @Author: heweizong
 * @Description:
 */
public abstract class AutoViewPagerAdapter<T> extends PagerAdapter {
    private List<T> mData;
    private Context mContext;

    public AutoViewPagerAdapter(Context mContext) {
        this.mContext = mContext;
        this.mData = new ArrayList<>();
    }

    @Override
    public final int getCount() {
        if (mData == null || mData.size() == 0) {
            return 0;
        }
        return mData.size() == 1 ? 1 : 1000 * mData.size();
    }

    public int getDataCount() {
        if (mData == null) return 0;
        return mData.size();
    }

    public T getItem(int position) {
        if (mData.size() == 0) return null;
        int pos = position % mData.size();
        return mData.get(pos);
    }

    public void addDatas(List<T> list) {
        mData.clear();
        mData.addAll(list);
        notifyDataSetChanged();
    }

    public void delete(T bean) {
        mData.remove(bean);
        notifyDataSetChanged();
    }

}
