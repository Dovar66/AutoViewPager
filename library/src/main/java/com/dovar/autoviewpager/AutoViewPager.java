package com.dovar.autoviewpager;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.RadioButton;
import android.widget.RadioGroup;


import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * 实现自动轮播，无限轮播的viewpager
 * 需要对传入adapter的list做处理：在list的首、尾两个位置分别添加原来list的list.get(getAdapter().getCount()()-1)和list.get(0);
 * 即传入的数据源比实际要展示的多两项；
 * Created by Dovar_66 on 2016/9/30 .
 */


public class AutoViewPager extends ViewPager {
    private int time = 2500;        //默认跳转间隔2.5秒
    private int oldx = 0;           //点击位置的x坐标
    private int oldy = 0;           //点击位置的y坐标
    private int current = 0;//当前图片

    private boolean isTouch = false;        //判断是否正在触摸viewpager
    private boolean isLeft = false;             //判断是向左滑还是右滑
    private RadioGroup mIndicator;         //页码指示器

    private boolean isVertical = false;//是否设置为纵向viewPager

    private ExecutorService mExecutorService = Executors.newCachedThreadPool();//创建一个可缓存线程池
    private int count = 0;//每次页面跳转时计数+1
    private final ThreadLocal<Integer> selectcount = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };


    public AutoViewPager(Context context) {
        this(context, null);
        init();
        this.setOnPageChangeListener(changeListener);
    }

    public AutoViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        initIsVertical(attrs, 0);
        init();
        this.setOnPageChangeListener(changeListener);
    }

    public AutoViewPager(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        initIsVertical(attrs, defStyle);
        init();
        this.setOnPageChangeListener(changeListener);
    }

    private void initIsVertical(AttributeSet attrs, int defStyle){
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.HorizonVerticalViewPager, defStyle, 0);
        isVertical = a.getBoolean(R.styleable.HorizonVerticalViewPager_isVertical, false);
        a.recycle();
    }

    private void init() {
        if(isVertical){
            // The majority of the magic happens here
            setPageTransformer(true, new AutoViewPager.VerticalPageTransformer());
            // The easiest way to get rid of the overscroll drawing that happens on the left and right
            setOverScrollMode(OVER_SCROLL_NEVER);
        }
    }

    private class VerticalPageTransformer implements ViewPager.PageTransformer {

        @Override
        public void transformPage(View view, float position) {

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 1) { // [-1,1]
                view.setAlpha(1);

                // Counteract the default slide transition
                view.setTranslationX(view.getWidth() * -position);

                //set Y position to swipe in from top
                float yPosition = position * view.getHeight();
                view.setTranslationY(yPosition);

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }

    /**
     * 设置自动滑动的间隔时间
     *
     * @param time 时间
     */
    public void setTime(int time) {
        this.time = time;
    }

    /**
     * 开启自动滑动
     */
    public void startTurn() {
        if (getAdapter().getCount() == 0) {
            return;
        }
        setCurrentItem(1);
    }

    /**
     * 设置页码指示器
     *
     * @param mRadioGroup
     */
    public void setIndicator(RadioGroup mRadioGroup) {
        mIndicator = mRadioGroup;
    }

    /**
     * 修改滑动速度
     */
    public void setSpeed(int duration) {
        try {
            Field mScroller = ViewPager.class.getDeclaredField("mScroller");
            mScroller.setAccessible(true);
            FixedSpeedScroller scroller = new FixedSpeedScroller(getContext(), new AccelerateInterpolator());
            scroller.setDuration(duration);
            mScroller.set(this, scroller);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Swaps the X and Y coordinates of your touch event.
     */
    private MotionEvent swapXY(MotionEvent ev) {
        float width = getWidth();
        float height = getHeight();

        float newX = (ev.getY() / height) * width;
        float newY = (ev.getX() / width) * height;

        ev.setLocation(newX, newY);

        return ev;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev){
        if (isVertical) {
            boolean intercepted = super.onInterceptTouchEvent(swapXY(ev));
            swapXY(ev); // return touch coordinates to original reference frame for any child views
            return intercepted;
        }else {
            return super.onInterceptTouchEvent(ev);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isVertical){
            swapXY(event);
        }
        oldx = (int) event.getX();
        oldy = (int) event.getY();
        int mMoveX = 0;
        isTouch = true;
        //当按下时，正好显示的是最后一页，设置当前页为第二页
        //当按下时，正好显示的是第一页，设置当前页为倒数第二页
        if (current == getAdapter().getCount() - 1) {
            current = 1;
            this.setCurrentItem(current, false);
        } else if (current == 0) {
            current = getAdapter().getCount() - 2;
            this.setCurrentItem(current, false);
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            //dosomething
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            //手指在x轴方向上的位移小于y轴方向上位移时，不处理此次touch事件
            if (Math.abs(oldx - event.getX()) - Math.abs(oldy - event.getY()) < 0) return false;
            mMoveX = (int) event.getX();
            //判断向左滑还是向右滑
            isLeft = mMoveX - event.getX() < 0;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            isTouch = false;
        }
        return super.onTouchEvent(event);
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (current == getAdapter().getCount() - 1) {
                endToHead();
            } else {
                current++;
                setCurrentItem(current);
            }
        }
    };

    OnPageChangeListener changeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(final int position, final float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            if (mIndicator != null) {
                int pos;
                if (position == 0) {
                    pos = getAdapter().getCount() - 3;
                } else if (position == getAdapter().getCount() - 1) {
                    pos = 0;
                } else {
                    pos = position - 1;
                }
                RadioButton btn = (RadioButton) mIndicator.getChildAt(pos);
                mIndicator.clearCheck();
                mIndicator.check(btn.getId());
            }

            count++;
            mExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    selectcount.set(count);
                    try {
                        Thread.sleep(time);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (selectcount.get() == count && !isTouch) {
                        //线程沉睡期间页面未发生跳转并且viewpager当前不处于触摸状态则发送消息，否则不发送此条消息
                        mHandler.sendEmptyMessage(0);
                    } else if (selectcount.get() == count && isTouch) {
                        //页面未发生跳转但viewpager当前处于触摸状态，不发送此条消息，准备下一条计时消息
                        mExecutorService.execute(this);
                    }
                }
            });

            if (position != getAdapter().getCount()) {
                current = position;
            }
            int pageIndex = position;

            if (position == getAdapter().getCount() - 1 && isTouch) {
                if (!isLeft) {
                    endToHead();
                }
                pageIndex = 1;
            } else if (position == 0 && isTouch) {
                if (isLeft) {
                    headToEnd();
                }
                pageIndex = getAdapter().getCount() - 2;
            }

            if (position != pageIndex) {
                setCurrentItem(pageIndex, true);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    /**
     * 最后一页转至第二页
     */
    private void endToHead() {
        current = 1;//最后一页与第二页的内容相同，第一页与倒数第二页的内容相同
        this.setCurrentItem(current, false);
        current++;
        this.setCurrentItem(current, true);
    }

    /**
     * 第一页转至倒数第二页
     */
    private void headToEnd() {
        current = getAdapter().getCount() - 1;
        this.setCurrentItem(current, false);
        current--;
        this.setCurrentItem(current, true);

    }
}
