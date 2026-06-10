package com.example.watchdemo;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SettingsScreenView extends FrameLayout {
    private final MainActivity activity;
    private boolean isApplyingEffects = false;

    public SettingsScreenView(final MainActivity activity) {
        super(activity);
        this.activity = activity;

        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LayoutInflater inflater = LayoutInflater.from(activity);
        View root = inflater.inflate(R.layout.screen_settings, this, true);

        final ScrollView scrollView = root.findViewById(R.id.scroll_view);
        final LinearLayout container = root.findViewById(R.id.container);

        String[] itemTexts = {
                "起卦震动: " + (activity.vibrationEnabled ? "开" : "关"),
                "清除历史记录",
                "关于星曜"
        };

        String[] itemIcons = {
                "震", "清", "曜"
        };

        for (int i = 0; i < 3; i++) {
            final int index = i;
            View card = inflater.inflate(R.layout.item_tarot_array, container, false);

            TextView iconTv = card.findViewById(R.id.tv_icon_char);
            if (iconTv != null) {
                iconTv.setText(itemIcons[i]);
            }

            TextView nameTv = card.findViewById(R.id.tv_name);
            if (nameTv != null) {
                nameTv.setText(itemTexts[i]);
                if (index == activity.settingsSelectedIndex) {
                    nameTv.setTextColor(Color.parseColor("#D4AF37"));
                }
            }

            card.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.settingsSelectedIndex = index;
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
                    return false;
                }
            });

            container.addView(card);
        }

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
                if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                    applyEdgeEffects(scrollView, container);
                }
            }
        });
    }

    private void applyEdgeEffects(ScrollView scrollView, LinearLayout container) {
        if (isApplyingEffects) return;
        isApplyingEffects = true;
        try {
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
        } finally {
            isApplyingEffects = false;
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

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (activity.gestureDetector != null) {
            activity.gestureDetector.onTouchEvent(this, event);
        }
        return super.dispatchTouchEvent(event);
    }
}
