package com.example.watchdemo;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

public class WatchGestureDetector {
    public interface GestureListener {
        void onClick(float x, float y);
        void onLongPress(float x, float y);
        void onSwipeLeft();
        void onSwipeRight();
        void onSwipeUp();
        void onSwipeDown();
    }

    private static final int SWIPE_THRESHOLD_DP = 30; // 较小的滑动手势阈值以适应手表
    private static final int LONG_PRESS_TIMEOUT_MS = 600;
    
    private final float density;
    private final GestureListener listener;
    
    private float startX, startY;
    private long startTime;
    private boolean isLongPressedTriggered;
    private boolean isMoving;
    private Runnable longPressRunnable;
    private boolean isGestureActive = false;

    public WatchGestureDetector(Context context, GestureListener listener) {
        this.density = context.getResources().getDisplayMetrics().density;
        this.listener = listener;
    }

    public boolean onTouchEvent(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                startTime = System.currentTimeMillis();
                isLongPressedTriggered = false;
                isMoving = false;
                isGestureActive = true;
                
                if (longPressRunnable != null) {
                    v.removeCallbacks(longPressRunnable);
                }
                longPressRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!isMoving && !isLongPressedTriggered) {
                            isLongPressedTriggered = true;
                            listener.onLongPress(startX, startY);
                        }
                    }
                };
                v.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT_MS);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!isGestureActive) {
                    return false;
                }
                float dx = event.getX() - startX;
                float dy = event.getY() - startY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > 8 * density) { // 稍微移动即取消长按判定
                     isMoving = true;
                     if (longPressRunnable != null) {
                         v.removeCallbacks(longPressRunnable);
                     }
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (!isGestureActive) {
                    return false;
                }
                isGestureActive = false;
                if (longPressRunnable != null) {
                    v.removeCallbacks(longPressRunnable);
                }
                if (isLongPressedTriggered) {
                    return true;
                }
                
                float deltaX = event.getX() - startX;
                float deltaY = event.getY() - startY;
                float thresholdPx = SWIPE_THRESHOLD_DP * density;
                
                if (Math.abs(deltaX) > thresholdPx || Math.abs(deltaY) > thresholdPx) {
                    // 滑动方向判定
                    if (Math.abs(deltaX) > Math.abs(deltaY)) {
                        if (deltaX > 0) {
                            listener.onSwipeRight();
                        } else {
                            listener.onSwipeLeft();
                        }
                    } else {
                        if (deltaY > 0) {
                            listener.onSwipeDown();
                        } else {
                            listener.onSwipeUp();
                        }
                    }
                } else {
                    // 点击判定
                    long duration = System.currentTimeMillis() - startTime;
                    if (duration < LONG_PRESS_TIMEOUT_MS) {
                        listener.onClick(event.getX(), event.getY());
                    }
                }
                return true;
                
            case MotionEvent.ACTION_CANCEL:
                if (!isGestureActive) {
                    return false;
                }
                isGestureActive = false;
                if (longPressRunnable != null) {
                    v.removeCallbacks(longPressRunnable);
                }
                return true;
        }
        return false;
    }
}
