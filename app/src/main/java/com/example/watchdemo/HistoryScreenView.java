package com.example.watchdemo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class HistoryScreenView extends FrameLayout {
    private final MainActivity activity;

    public HistoryScreenView(final MainActivity activity) {
        super(activity);
        this.activity = activity;

        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LayoutInflater inflater = LayoutInflater.from(activity);
        View root = inflater.inflate(R.layout.screen_history, this, true);

        final ScrollView scrollView = root.findViewById(R.id.scroll_view);
        final LinearLayout container = root.findViewById(R.id.container);
        View dot1 = root.findViewById(R.id.indicator_dot1);
        View dot2 = root.findViewById(R.id.indicator_dot2);

        // Update indicator dots
        GradientDrawable activeDot = new GradientDrawable();
        activeDot.setShape(GradientDrawable.OVAL);
        activeDot.setColor(Color.parseColor("#D4AF37"));

        GradientDrawable inactiveDot = new GradientDrawable();
        inactiveDot.setShape(GradientDrawable.OVAL);
        inactiveDot.setColor(Color.parseColor("#33FFFFFF"));

        if (activity.historyTabIndex == 0) {
            dot1.setBackground(activeDot);
            dot2.setBackground(inactiveDot);
        } else {
            dot1.setBackground(inactiveDot);
            dot2.setBackground(activeDot);
        }

        // Setup Pinned Tab buttons
        TextView btnLiuyao = root.findViewById(R.id.btn_tab_liuyao);
        TextView btnTarot = root.findViewById(R.id.btn_tab_tarot);
        View tabLayout = root.findViewById(R.id.tab_layout);

        if (tabLayout != null) {
            GradientDrawable containerBg = new GradientDrawable();
            containerBg.setColor(Color.parseColor("#11FFFFFF")); // 10% white background
            containerBg.setCornerRadius(16f * activity.density);
            tabLayout.setBackground(containerBg);
            tabLayout.setPadding((int)(2 * activity.density), (int)(2 * activity.density), (int)(2 * activity.density), (int)(2 * activity.density));
        }

        GradientDrawable activeBg = new GradientDrawable();
        activeBg.setColor(Color.parseColor("#D4AF37")); // Gold active tab background
        activeBg.setCornerRadius(14f * activity.density);

        if (activity.historyTabIndex == 0) {
            if (btnLiuyao != null) {
                btnLiuyao.setBackground(activeBg);
                btnLiuyao.setTextColor(Color.BLACK);
            }
            if (btnTarot != null) {
                btnTarot.setBackground(null);
                btnTarot.setTextColor(Color.WHITE);
            }
        } else {
            if (btnLiuyao != null) {
                btnLiuyao.setBackground(null);
                btnLiuyao.setTextColor(Color.WHITE);
            }
            if (btnTarot != null) {
                btnTarot.setBackground(activeBg);
                btnTarot.setTextColor(Color.BLACK);
            }
        }

        if (btnLiuyao != null) {
            btnLiuyao.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (activity.historyTabIndex != 0) {
                        activity.historyTabIndex = 0;
                        activity.historySelectedIndex = 0;
                        activity.vibrateCustom(android.os.VibrationEffect.EFFECT_CLICK);
                        activity.renderScreen();
                    }
                }
            });
        }

        if (btnTarot != null) {
            btnTarot.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (activity.historyTabIndex != 1) {
                        activity.historyTabIndex = 1;
                        activity.historySelectedIndex = 0;
                        activity.vibrateCustom(android.os.VibrationEffect.EFFECT_CLICK);
                        activity.renderScreen();
                    }
                }
            });
        }

        int count = (activity.historyTabIndex == 0) ? activity.liuyaoHistoryList.size() : activity.tarotHistoryList.size();

        if (count == 0) {
            TextView emptyTv = new TextView(activity);
            emptyTv.setText("暂无记录");
            emptyTv.setTextSize(14);
            emptyTv.setTextColor(Color.GRAY);
            emptyTv.setGravity(android.view.Gravity.CENTER);
            emptyTv.setPadding(0, (int)(40 * activity.density), 0, 0);
            container.addView(emptyTv);
        } else {
            for (int i = 0; i < count; i++) {
                final int index = i;
                View card = inflater.inflate(R.layout.item_tarot_array, container, false);

                TextView iconTv = card.findViewById(R.id.tv_icon_char);
                if (iconTv != null) {
                    iconTv.setText(activity.historyTabIndex == 0 ? "爻" : "塔");
                }

                TextView nameTv = card.findViewById(R.id.tv_name);
                if (nameTv != null) {
                    String label = (activity.historyTabIndex == 0) ? 
                        activity.liuyaoHistoryList.get(i).displayStr : 
                        activity.tarotHistoryList.get(i).displayStr;
                    nameTv.setText(label);
                }

                if (index == activity.historySelectedIndex) {
                    if (nameTv != null) {
                        nameTv.setTextColor(Color.parseColor("#D4AF37"));
                    }
                }

                // Set click listener
                card.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.historySelectedIndex = index;
                        if (activity.controller != null) {
                            activity.controller.onClick(0, 0);
                        }
                    }
                });

                // Set touch listener for press animation and swipe left to delete
                card.setOnTouchListener(new OnTouchListener() {
                    private float startX;
                    private float startY;
                    private boolean isSwipeDetected;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                startX = event.getRawX();
                                startY = event.getRawY();
                                isSwipeDetected = false;
                                v.animate().scaleX(0.94f).scaleY(0.94f).alpha(0.8f).setDuration(200).start();
                                break;
                            case MotionEvent.ACTION_MOVE:
                                if (isSwipeDetected) {
                                    return true;
                                }
                                float dx = event.getRawX() - startX;
                                float dy = event.getRawY() - startY;
                                float density = activity.density;
                                if (dx < -40 * density && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                                    isSwipeDetected = true;
                                    v.animate().cancel();
                                    deleteHistoryItemWithAnimation(v, index, scrollView, container);
                                    return true;
                                }
                                break;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                if (isSwipeDetected) {
                                    return true;
                                }
                                v.animate().cancel();
                                applyEdgeEffects(scrollView, container);
                                break;
                        }
                        return isSwipeDetected;
                    }
                });

                container.addView(card);
            }
        }

        // Apply edge effects on scroll
        scrollView.setOnScrollChangeListener(new OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                applyEdgeEffects(scrollView, container);
            }
        });

        container.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                applyEdgeEffects(scrollView, container);
            }
        });
    }

    private void applyEdgeEffects(ScrollView scrollView, LinearLayout container) {
        int parentHeight = scrollView.getHeight();
        if (parentHeight == 0) return;

        int scrollY = scrollView.getScrollY();
        int childCount = container.getChildCount();

        float thresholdTop = 64 * activity.density; 
        float minVisibleTop = -20 * activity.density; 

        float thresholdBottom = parentHeight - 64 * activity.density;
        float maxVisibleBottom = parentHeight + 20 * activity.density; 

        float[] scales = new float[childCount];
        float[] translationY = new float[childCount];

        int lastTopIndex = -1;
        int firstBottomIndex = childCount;

        for (int i = 0; i < childCount; i++) {
            View child = container.getChildAt(i);
            float childHeight = child.getHeight();
            if (childHeight == 0) {
                scales[i] = 1.0f;
                continue;
            }

            float relativeTop = child.getTop() - scrollY;
            float relativeBottom = child.getBottom() - scrollY;

            float scale = 1.0f;
            float alpha = 1.0f;

            if (relativeTop < thresholdTop) {
                float ratio = (relativeTop - minVisibleTop) / (thresholdTop - minVisibleTop);
                ratio = Math.max(0f, Math.min(1f, ratio));
                scale = ratio * 0.15f + 0.85f; 
                alpha = ratio * 0.3f + 0.7f;   
                lastTopIndex = i;
            } else if (relativeBottom > thresholdBottom) {
                float ratio = (maxVisibleBottom - relativeBottom) / (maxVisibleBottom - thresholdBottom);
                ratio = Math.max(0f, Math.min(1f, ratio));
                scale = ratio * 0.15f + 0.85f; 
                alpha = ratio * 0.3f + 0.7f;   
                if (firstBottomIndex == childCount) {
                    firstBottomIndex = i;
                }
            }

            scales[i] = scale;
            child.setAlpha(alpha);
        }

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

        for (int i = 0; i < childCount; i++) {
            View child = container.getChildAt(i);
            child.setScaleX(scales[i]);
            child.setScaleY(scales[i]);
            child.setTranslationY(translationY[i]);
        }
    }

    public void onCrownScroll(boolean clockwise) {
        ScrollView scrollView = findViewById(R.id.scroll_view);
        View container = findViewById(R.id.container);
        if (scrollView != null && container != null) {
            int scrollY = scrollView.getScrollY();
            int maxScrollY = Math.max(0, container.getHeight() - scrollView.getHeight());

            int dy = (int) (64 * activity.density); 
            if (!clockwise) {
                dy = -dy;
            }

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

    private void deleteHistoryItemWithAnimation(final View card, final int index, final ScrollView scrollView, final LinearLayout container) {
        float w = card.getWidth();
        card.animate()
            .translationX(-w)
            .alpha(0f)
            .setDuration(250)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    activity.vibrateCustom(android.os.VibrationEffect.EFFECT_CLICK);
                    if (activity.historyTabIndex == 0) {
                        if (index < activity.liuyaoHistoryList.size()) {
                            activity.liuyaoHistoryList.remove(index);
                        }
                    } else {
                        if (index < activity.tarotHistoryList.size()) {
                            activity.tarotHistoryList.remove(index);
                        }
                    }
                    android.widget.Toast.makeText(activity, "已删除该记录", android.widget.Toast.LENGTH_SHORT).show();
                    activity.historySelectedIndex = 0;
                    activity.renderScreen();
                }
            })
            .start();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (activity.gestureDetector != null) {
            activity.gestureDetector.onTouchEvent(this, event);
        }
        return super.dispatchTouchEvent(event);
    }
}

