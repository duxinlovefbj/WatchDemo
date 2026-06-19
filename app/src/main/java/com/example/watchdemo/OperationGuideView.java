package com.example.watchdemo;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.VibrationEffect;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class OperationGuideView extends FrameLayout {
    private final MainActivity activity;
    private final boolean isFromSettings;

    private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private LinearLayout content;

    public OperationGuideView(MainActivity activity, boolean isFromSettings) {
        super(activity);
        this.activity = activity;
        this.isFromSettings = isFromSettings;
        setClickable(true);
        setFocusable(true);
        setBackgroundColor(Color.BLACK);
        setWillNotDraw(false);

        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(Color.parseColor("#D4AF37"));

        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.setVerticalScrollBarEnabled(false);
        addView(scrollView, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(28), dp(24), dp(28), dp(20));
        scrollView.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        TextView title = text("操作指南", 17, "#D4AF37", true);
        title.setGravity(Gravity.CENTER);
        content.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView body = text(
                "左侧进入六爻，右侧进入塔罗\n" +
                "上滑设置，下滑历史\n" +
                "六爻可旋转表冠\n" +
                "也可上下滑动抽取",
                12.0f, "#E8E8E8", false);
        body.setGravity(Gravity.CENTER);
        body.setLineSpacing(dp(2), 1.0f);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = dp(8);
        content.addView(body, bodyLp);

        // Add "Confirm" button at the bottom (matching About dialog capsule style)
        TextView btnConfirm = text("确定", 13, "#FFFFFF", true);
        btnConfirm.setBackgroundResource(R.drawable.item_capsule_bg);
        btnConfirm.setPadding(dp(24), dp(8), dp(24), dp(8));
        btnConfirm.setClickable(true);
        btnConfirm.setFocusable(true);
        btnConfirm.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.topMargin = dp(10);
        content.addView(btnConfirm, btnLp);

        btnConfirm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.vibrateCustom(VibrationEffect.EFFECT_CLICK);
                activity.dismissOperationGuide();
            }
        });

        btnConfirm.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.94f).scaleY(0.94f).alpha(0.8f).setDuration(150).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(150).start();
                        break;
                }
                return false;
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // 1. Draw solid background covering the entire layout canvas
        canvas.drawColor(Color.BLACK);

        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(cx, cy) - 2 * activity.density;

        // 2. Draw thin base gold border
        mBorderPaint.setStrokeWidth(1.2f * activity.density);
        mBorderPaint.setAlpha(60); // Faint base ring
        canvas.drawCircle(cx, cy, radius, mBorderPaint);
    }

    private TextView text(String value, float sp, String color, boolean bold) {
        TextView tv = new TextView(activity);
        tv.setText(value);
        tv.setTextSize(sp);
        tv.setTextColor(Color.parseColor(color));
        if (bold) {
            tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return tv;
    }

    private int dp(float value) {
        return (int) (value * activity.density + 0.5f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Consumes touch events to prevent click-through.
        return true;
    }
}
