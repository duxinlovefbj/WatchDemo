package com.example.watchdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

public class SwipeBackLayout extends FrameLayout {
    private float startX, startY;
    private boolean isDragging = false;
    private int touchSlop;
    private View contentView;
    private OnSwipeBackListener listener;
    private float maxDragDistance;
    private boolean isSwipeDisabled = false;

    public void setSwipeDisabled(boolean disabled) {
        this.isSwipeDisabled = disabled;
        Log.d("SwipeBackLayout", "setSwipeDisabled: " + disabled);
    }

    public interface OnSwipeBackListener {
        void onSwipeBack();
    }

    public SwipeBackLayout(Context context) {
        super(context);
        init(context);
    }

    public SwipeBackLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        Log.d("SwipeBackLayout", "Initialized SwipeBackLayout. touchSlop = " + touchSlop);
    }

    public void setOnSwipeBackListener(OnSwipeBackListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (getChildCount() > 0) {
            View newContentView = getChildAt(0);
            if (newContentView != contentView) {
                contentView = newContentView;
                if (contentView != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    contentView.setOutlineProvider(new android.view.ViewOutlineProvider() {
                        @Override
                        public void getOutline(View view, android.graphics.Outline outline) {
                            int radius = view.getWidth() / 2;
                            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                            Log.d("SwipeBackLayout", "contentView getOutline: width=" + view.getWidth() + " height=" + view.getHeight() + " radius=" + radius);
                        }
                    });
                    contentView.setClipToOutline(true);
                    Log.d("SwipeBackLayout", "Enabled clipToOutline on contentView");
                }
            }
        }
        maxDragDistance = getWidth();
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    int radius = view.getWidth() / 2;
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                    Log.d("SwipeBackLayout", "parent getOutline: width=" + view.getWidth() + " height=" + view.getHeight() + " radius=" + radius);
                }
            });
            setClipToOutline(true);
            Log.d("SwipeBackLayout", "Enabled clipToOutline on parent SwipeBackLayout");
        }
        
        Log.d("SwipeBackLayout", "onLayout: getWidth() = " + getWidth() + ", contentView = " + contentView);
    }

    @Override
    protected void dispatchDraw(android.graphics.Canvas canvas) {
        super.dispatchDraw(canvas);
        if (contentView != null) {
            float dx = contentView.getTranslationX();
            if (dx > 0) {
                // Draw a subtle eclipse corona outline on the left edge of the sliding circular mask
                android.graphics.Paint coronaPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                coronaPaint.setStyle(android.graphics.Paint.Style.STROKE);
                coronaPaint.setStrokeWidth(1.2f * getResources().getDisplayMetrics().density);
                coronaPaint.setColor(android.graphics.Color.WHITE);
                float progress = dx / maxDragDistance;
                int alpha = (int) (160 * (1.0f - progress)); // Fades out as the swipe reaches exit
                coronaPaint.setAlpha(alpha);

                float cx = getWidth() / 2f + dx;
                float cy = getHeight() / 2f;
                float radius = getWidth() / 2f - 0.6f * getResources().getDisplayMetrics().density; // slightly inset to prevent screen clipping
                canvas.drawCircle(cx, cy, radius, coronaPaint);
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isSwipeDisabled) {
            return false;
        }
        float density = getResources().getDisplayMetrics().density;
        float edgeThreshold = getWidth() > 0 ? getWidth() * 0.5f : 150 * density;
        Log.d("SwipeBackLayout", "onInterceptTouchEvent action=" + ev.getAction() + " x=" + ev.getRawX() + " y=" + ev.getRawY() + " edgeThreshold=" + edgeThreshold);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getRawX();
                startY = ev.getRawY();
                isDragging = false;
                setActivityDragging(false);
                Log.d("SwipeBackLayout", "ACTION_DOWN: startX=" + startX + ", threshold=" + edgeThreshold);
                if (startX > edgeThreshold) {
                    return false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (startX > edgeThreshold) {
                    return false;
                }
                float dx = ev.getRawX() - startX;
                float dy = ev.getRawY() - startY;
                Log.d("SwipeBackLayout", "ACTION_MOVE: dx=" + dx + ", dy=" + dy + ", touchSlop=" + touchSlop);
                if (dx > touchSlop && dx > Math.abs(dy) * 1.2f) {
                    isDragging = true;
                    setActivityDragging(true);
                    Log.d("SwipeBackLayout", "INTERCEPTED gesture! isDragging=true");
                    return true; // intercept touch
                }
                break;
        }
        return false;
    }

    private View getUnderneathView() {
        if (getParent() != null && getParent().getParent() instanceof android.view.ViewGroup) {
            android.view.ViewGroup root = (android.view.ViewGroup) getParent().getParent();
            if (root.getChildCount() > 0) {
                View first = root.getChildAt(0);
                if (first instanceof InitScreenView) {
                    return first;
                }
            }
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isSwipeDisabled) return super.onTouchEvent(event);
        Log.d("SwipeBackLayout", "onTouchEvent action=" + event.getAction() + " x=" + event.getRawX() + " isDragging=" + isDragging);
        if (contentView == null) return super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getRawX();
                startY = event.getRawY();
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - startX;
                if (dx < 0) dx = 0;
                Log.d("SwipeBackLayout", "onTouchEvent ACTION_MOVE: dx=" + dx + ", maxDrag=" + maxDragDistance);
                contentView.setTranslationX(dx);
                // Reduce alpha slightly to make it fade out as it slides away
                float progress = dx / maxDragDistance;
                contentView.setAlpha(1.0f - progress * 0.4f);

                View under = getUnderneathView();
                if (under != null) {
                    // Revealed view scales down from 1.1 to 1.0 and fades in (matching Oppo fragment_close_enter.xml)
                    float scale = 1.1f - progress * 0.1f;
                    under.setScaleX(scale);
                    under.setScaleY(scale);
                    under.setAlpha(progress);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float finalDx = event.getRawX() - startX;
                Log.d("SwipeBackLayout", "onTouchEvent ACTION_UP/CANCEL: finalDx=" + finalDx + ", threshold=" + (maxDragDistance * 0.35f));
                if (finalDx > maxDragDistance * 0.35f) {
                    // Slide off screen to the right (matching Oppo hey_activity_anim.xml duration of 276ms)
                    long duration = 276;
                    contentView.animate()
                            .translationX(maxDragDistance)
                            .alpha(0.0f)
                            .setDuration(duration)
                            .setInterpolator(new DecelerateInterpolator())
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d("SwipeBackLayout", "Swipe back animation finished, triggering listener");
                                    if (listener != null) {
                                        listener.onSwipeBack();
                                    }
                                }
                            })
                            .start();

                    View underView = getUnderneathView();
                    if (underView != null) {
                        underView.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .alpha(1.0f)
                                .setDuration(duration)
                                .setInterpolator(new DecelerateInterpolator())
                                .start();
                    }
                } else {
                    // Spring back
                    long duration = 200;
                    contentView.animate()
                            .translationX(0)
                            .alpha(1.0f)
                            .setDuration(duration)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();

                    View underView = getUnderneathView();
                    if (underView != null) {
                        underView.animate()
                                .scaleX(1.1f)
                                .scaleY(1.1f)
                                .alpha(0.0f)
                                .setDuration(duration)
                                .setInterpolator(new DecelerateInterpolator())
                                .start();
                    }
                }
                isDragging = false;
                setActivityDragging(false);
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void setActivityDragging(boolean dragging) {
        if (getContext() instanceof MainActivity) {
            ((MainActivity) getContext()).isSwipeBackDragging = dragging;
        }
    }
}
