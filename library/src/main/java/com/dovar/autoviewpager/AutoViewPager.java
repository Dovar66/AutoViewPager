package com.dovar.autoviewpager;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Scroller;

import java.lang.ref.WeakReference;
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

    private boolean isVertical;//是否设置为纵向viewPager
    private boolean isTouch;        //判断是否正在触摸viewpager
    private RadioGroup mIndicator;          //页码指示器

    private final MyHandler mHandler = new MyHandler(this);
    private ExecutorService mExecutorService = Executors.newCachedThreadPool();//创建一个可缓存线程池
    private int count = 0;//每次页面跳转时计数+1
    private final ThreadLocal<Integer> selectcount = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    private boolean autoSrcoll = true;

    private enum STATE {
        idle, isRunning, isPaused
    }

    private STATE runState = STATE.idle;

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

    private void initIsVertical(AttributeSet attrs, int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.AutoViewPager, defStyle, 0);
        isVertical = a.getBoolean(R.styleable.AutoViewPager_isVertical, false);
        a.recycle();
    }

    private void init() {
        if (isVertical) {
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
        PagerAdapter adapter = getAdapter();
        if (adapter instanceof AutoViewPagerAdapter) {
            int realDataCount = ((AutoViewPagerAdapter) adapter).getDataCount();
            if (realDataCount <= 1) {
                return;
            }
            setCurrentItem(realDataCount * 500, false);
        } else {
            throw new RuntimeException("Your PagerAdapter Must Extends AutoViewPagerAdapter");
        }
    }

    @Override
    public final void setAdapter(PagerAdapter adapter) {
        if (adapter instanceof AutoViewPagerAdapter) {
            super.setAdapter(adapter);
        } else {
            throw new RuntimeException("Your PagerAdapter Must Extends AutoViewPagerAdapter");
        }
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        if (getVisibility() == VISIBLE) {
            runState = STATE.isRunning;
            super.setCurrentItem(item, smoothScroll);
        }
    }

    @Override
    public void setCurrentItem(int item) {
        if (getVisibility() == VISIBLE) {
            runState = STATE.isRunning;
            super.setCurrentItem(item);
        }
    }

    public void pause() {
        runState = STATE.isPaused;
        if (mHandler != null) {
            mHandler.removeMessages(0);
        }
    }

    public boolean isRunning() {
        return runState == STATE.isRunning;
    }

    public boolean isPaused() {
        return runState == STATE.isPaused;
    }

    public void startTurnAfterPaused() {
        if (isPaused() && getVisibility() == VISIBLE) {
            runState = STATE.isRunning;
            if (mHandler != null) {
                mHandler.removeMessages(0);
                mHandler.sendEmptyMessageDelayed(0, time);
            }
        }
    }

    /**
     * 设置页码指示器
     *
     * @param mRadioGroup
     */
    public void setIndicator(RadioGroup mRadioGroup) {
        if (mRadioGroup != null) {
            mIndicator = mRadioGroup;
          /*  if (enableClick) {//指示器的radioButton是否需要点击事件
                configIndicator();
            }*/
        }
    }

    /**
     * 配置指示器，代码段有问题，待完善
     */
   /* public void configIndicator() {
        mIndicator.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int count = getAdapter().getCount();
                if (count <= 3) return;//实际只有一页时不轮播

                for (int i = 0; i < group.getChildCount() && i < count; i++) {

                    if (checkedId == group.getChildAt(i).getId()) {
                        int currentItem = getCurrentItem();
                        if (i == 0) {//选中的是第一个button，对应于ViewPager的第二页和最后一页
                            if (currentItem == 1 || currentItem == count - 1)
                                return;
                        } else if (i == group.getChildCount() - 1) {//选中的是最后一个button，对应于ViewPager的第一页和倒数第二页
                            if (currentItem == 0 || currentItem == count - 2)
                                return;
                        } else if (currentItem == i + 1) {
                            return;
                        }

                        setCurrentItem(i + 1);
                        return;
                    }
                }
            }
        });
    }*/

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
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isVertical) {
            return super.onInterceptTouchEvent(swapXY(ev));
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
//        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            oldx = (int) event.getX();
//            oldy = (int) event.getY();
//            isActionDown = true;
//            ignoreEvent = false;
//        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
//            if (isActionDown) {
//                isActionDown = false;
//            //手指在x轴方向上的位移小于y轴方向上位移时，不处理此次touch事件
//                if (Math.abs(oldx - event.getX()) - Math.abs(oldy - event.getY()) < 0) {
//                    ignoreEvent = true;
//                    return false;
//                }
//            }
//            if (ignoreEvent) {
//                return false;
//            }
//            requestDisallowInterceptTouchEvent(true);
//            return true;
//        } else if (event.getAction() == MotionEvent.ACTION_UP) {
//
//        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
//
//        }

//        requestDisallowInterceptTouchEvent(true);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isVertical) {
            swapXY(event);
        }
        isTouch = true;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {

        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {

        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            isTouch = false;
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            isTouch = false;
        }
        return super.onTouchEvent(event);
    }

    OnPageChangeListener changeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(final int position, final float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            PagerAdapter adapter = getAdapter();
            if (adapter instanceof AutoViewPagerAdapter) {
                int realDataCount = ((AutoViewPagerAdapter) adapter).getDataCount();
                if (adapter.getCount() <= 1 || realDataCount <= 1) {
                    return;
                }
                if (mIndicator != null) {
                    int pos = getCurrentItem() % realDataCount;
                    if (mIndicator.getChildCount() > pos) {
                        RadioButton btn = (RadioButton) mIndicator.getChildAt(pos);
                        if (mIndicator.getCheckedRadioButtonId() != btn.getId()) {
                            mIndicator.check(btn.getId());
                        }
                    }
                }

                count++;
                if (autoSrcoll) {
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
                                mHandler.removeMessages(0);
                                mHandler.sendEmptyMessage(0);
                            } else if (selectcount.get() == count && isTouch) {
                                //页面未发生跳转但viewpager当前处于触摸状态，不发送此条消息，准备下一条计时消息
                                mExecutorService.execute(this);
                            }
                        }
                    });
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };


    public void setAutoScroll(boolean autoScroll) {
        this.autoSrcoll = autoScroll;
    }

    private static class MyHandler extends Handler {
        private WeakReference<AutoViewPager> autoVp;

        MyHandler(AutoViewPager vp) {
            autoVp = new WeakReference<>(vp);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (autoVp == null || autoVp.get() == null) return;
            AutoViewPager vp = autoVp.get();
            if (!vp.isRunning() || vp.getAdapter() == null) return;
            int count = vp.getAdapter().getCount();
            if (count <= 1) return;
            int current = vp.getCurrentItem();
            if (current == count - 1) {
                vp.setCurrentItem(0, false);
            } else if (current == 0) {
                vp.setCurrentItem(count - 1, false);
            } else {
                if (count > current + 1) {
                    vp.setCurrentItem(current + 1);
                }
            }
        }
    }

    private class FixedSpeedScroller extends Scroller {
        private int mDuration = 3000;

        public FixedSpeedScroller(Context context) {
            super(context);
        }

        FixedSpeedScroller(Context context, Interpolator interpolator) {
            super(context, interpolator);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            // Ignore received duration, use fixed one instead
            super.startScroll(startX, startY, dx, dy, mDuration);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy) {
            // Ignore received duration, use fixed one instead
            super.startScroll(startX, startY, dx, dy, mDuration);
        }

        public void setDuration(int time) {
            mDuration = time;
        }

        public int getmDuration() {
            return mDuration;
        }
    }
}
