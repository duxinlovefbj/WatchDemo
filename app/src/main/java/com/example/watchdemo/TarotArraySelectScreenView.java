package com.example.watchdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class TarotArraySelectScreenView extends FrameLayout {
    private final MainActivity activity;

    private static final String[] ARRAY_NAMES = {
            "自由抽牌", "圣三角牌阵", "六芒星牌阵", "时间之箭牌阵", "凯尔特十字牌阵",
            "四元素牌阵", "二选一牌阵", "金字塔牌阵", "七脉轮牌阵", "直击问题牌阵", "指引之星牌阵",
            "财务牌阵", "人际关系牌阵"
    };

    private static final String[] ARRAY_ICONS = {
            "F", "3", "7", "5", "10", "4", "2", "9", "7", "D", "S", "M", "R"
    };

    public TarotArraySelectScreenView(MainActivity activity) {
        super(activity);
        this.activity = activity;

        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 使用 LayoutInflater 渲染主 ScrollView 容器布局
        LayoutInflater inflater = LayoutInflater.from(activity);
        View root = inflater.inflate(R.layout.screen_tarot_array_select, this, true);

        final ScrollView scrollView = root.findViewById(R.id.scroll_view);
        final LinearLayout container = root.findViewById(R.id.container);

        // 动态添加由 XML 定义的单个胶囊 Card 条目
        for (int i = 0; i < ARRAY_NAMES.length; i++) {
            final int index = i;

            View card = inflater.inflate(R.layout.item_tarot_array, container, false);

            TextView iconTv = card.findViewById(R.id.tv_icon_char);
            iconTv.setText(ARRAY_ICONS[i]);

            TextView nameTv = card.findViewById(R.id.tv_name);
            nameTv.setText(ARRAY_NAMES[i]);

            // 设置点击事件 (点击任意卡片立即选择并跳转)
            card.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.tarotArraySelectedIndex = index;
                    if (activity.controller != null) {
                        activity.controller.onClick(0, 0);
                    }
                }
            });

            card.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            v.animate().scaleX(0.94f).scaleY(0.94f).alpha(0.8f).setDuration(200).start();
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            v.animate().cancel();
                            applyEdgeEffects(scrollView, container);
                            break;
                    }
                    return false; // 返回 false 以允许 onClick 事件正确触发
                }
            });

            container.addView(card);
        }

        // 监听滚动事件，实时计算并应用边缘卡片的“缩小、叠放、渐隐”动效 (1:1 还原系统算法)
        scrollView.setOnScrollChangeListener(new OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                applyEdgeEffects(scrollView, container);
            }
        });

        // 监听布局变更，确保初始进入页面时正确应用首帧边缘特效
        container.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                applyEdgeEffects(scrollView, container);
            }
        });
    }

    /**
     * 1:1 还原官方 Wear OS 的边缘特效算法 (RecyclerView$d.smali 算法移植)
     * 在滑动至顶部/底部边缘时，计算可见度比例 ratio (1.0 -> 0.0)，进而调整子项的 Scale, Alpha 和 TranslationY
     * 采用累积偏移算法，确保在缩放过程中相邻卡片之间的物理间隔始终保持不变。
     */
    private void applyEdgeEffects(ScrollView scrollView, LinearLayout container) {
        int parentHeight = scrollView.getHeight();
        if (parentHeight == 0) return;

        int scrollY = scrollView.getScrollY();
        int childCount = container.getChildCount();

        // 设定圆盘顶部和底部触发渐隐缩放动画的“活跃区域值”（一般为 60dp - 70dp）
        float thresholdTop = 64 * activity.density; 
        float minVisibleTop = -20 * activity.density; // 完全滑出边缘的极值

        float thresholdBottom = parentHeight - 64 * activity.density;
        float maxVisibleBottom = parentHeight + 20 * activity.density; // 完全滑出底部的极值

        float[] scales = new float[childCount];
        float[] translationY = new float[childCount];

        int lastTopIndex = -1;
        int firstBottomIndex = childCount;

        // 第一遍扫描：计算所有子 View 的 scale 和 alpha，并记录顶部/底部缩放边界
        for (int i = 0; i < childCount; i++) {
            View child = container.getChildAt(i);
            float childHeight = child.getHeight();
            if (childHeight == 0) {
                scales[i] = 1.0f;
                continue;
            }

            // 计算卡片相对于 ScrollView 视口顶部的相对 top 和 bottom 坐标
            float relativeTop = child.getTop() - scrollY;
            float relativeBottom = child.getBottom() - scrollY;

            float scale = 1.0f;
            float alpha = 1.0f;

            if (relativeTop < thresholdTop) {
                // 1. 顶部边缘滑出（提前在进入顶部 64dp 窄边区域时触发）
                float ratio = (relativeTop - minVisibleTop) / (thresholdTop - minVisibleTop);
                ratio = Math.max(0f, Math.min(1f, ratio));
                scale = ratio * 0.15f + 0.85f; // Scale: 1.0 -> 0.85
                alpha = ratio * 0.3f + 0.7f;   // Alpha: 1.0 -> 0.7
                lastTopIndex = i;
            } else if (relativeBottom > thresholdBottom) {
                // 2. 底部边缘滑出（提前在进入底部 64dp 窄边区域时触发）
                float ratio = (maxVisibleBottom - relativeBottom) / (maxVisibleBottom - thresholdBottom);
                ratio = Math.max(0f, Math.min(1f, ratio));
                scale = ratio * 0.15f + 0.85f; // Scale: 1.0 -> 0.85
                alpha = ratio * 0.3f + 0.7f;   // Alpha: 1.0 -> 0.7
                if (firstBottomIndex == childCount) {
                    firstBottomIndex = i;
                }
            }

            scales[i] = scale;
            child.setAlpha(alpha);
        }

        // 第二遍扫描：从无缩放区域往两端方向累积计算每个 View 的 translationY 补偿量，使相邻卡片之间的物理间隔始终保持恒定。
        // 1. 顶部缩放区域：自下而上累积
        if (lastTopIndex >= 0) {
            for (int j = lastTopIndex; j >= 0; j--) {
                View childJ = container.getChildAt(j);
                float hJ = childJ.getHeight();
                float tNext = 0f;
                float scaleNext = 1f;
                float hNext = 0f;

                if (j + 1 < childCount) {
                    tNext = translationY[j + 1];
                    scaleNext = scales[j + 1];
                    hNext = container.getChildAt(j + 1).getHeight();
                }

                translationY[j] = tNext + hJ / 2f * (1f - scales[j]) + hNext / 2f * (1f - scaleNext);
            }
        }

        // 2. 底部缩放区域：自上而下累积
        if (firstBottomIndex < childCount) {
            for (int j = firstBottomIndex; j < childCount; j++) {
                View childJ = container.getChildAt(j);
                float hJ = childJ.getHeight();
                float tPrev = 0f;
                float scalePrev = 1f;
                float hPrev = 0f;

                if (j - 1 >= 0) {
                    tPrev = translationY[j - 1];
                    scalePrev = scales[j - 1];
                    hPrev = container.getChildAt(j - 1).getHeight();
                }

                translationY[j] = tPrev - hJ / 2f * (1f - scales[j]) - hPrev / 2f * (1f - scalePrev);
            }
        }

        // 应用变换
        for (int i = 0; i < childCount; i++) {
            View child = container.getChildAt(i);
            child.setScaleX(scales[i]);
            child.setScaleY(scales[i]);
            child.setTranslationY(translationY[i]);
        }
    }

    // 物理表冠旋转的接收接口，驱动内部 ScrollView 滚动
    public void onCrownScroll(boolean clockwise) {
        ScrollView scrollView = findViewById(R.id.scroll_view);
        View container = findViewById(R.id.container);
        if (scrollView != null && container != null) {
            int scrollY = scrollView.getScrollY();
            int maxScrollY = Math.max(0, container.getHeight() - scrollView.getHeight());

            int dy = (int) (64 * activity.density); // 每次滚动一个 Item 的间距 (60dp height + 4dp margin = 64dp)
            if (!clockwise) {
                dy = -dy;
            }

            // 限制滚动方向以防越界。如果已经处于边界，则不再调用 smoothScrollBy 避免异常弹跳。
            if ((dy < 0 && scrollY <= 0) || (dy > 0 && scrollY >= maxScrollY)) {
                return;
            }

            int targetScrollY = Math.max(0, Math.min(maxScrollY, scrollY + dy));
            int actualDy = targetScrollY - scrollY;

            if (actualDy != 0) {
                scrollView.smoothScrollBy(0, actualDy);
                activity.vibrateCustom(android.os.VibrationEffect.EFFECT_TICK);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (activity.gestureDetector != null) {
            activity.gestureDetector.onTouchEvent(this, event);
        }
        super.dispatchTouchEvent(event);
        return true;
    }
}