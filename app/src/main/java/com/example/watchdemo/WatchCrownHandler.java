package com.example.watchdemo;

public class WatchCrownHandler {
    public interface CrownListener {
        void onStepClockwise();
        void onStepCounterClockwise();
    }

    private final CrownListener listener;
    private float accumulatedDelta = 0f;
    private float threshold = 0.5f; // 触发步进的累积阈值
    private long lastEventTime = 0;
    private long lastTriggerTime = 0;
    private long cooldownMs = 20; // 两次触发之间的防抖冷却时间 (ms)，20ms 为消抖阈值，防止高频下震动粘连
    private boolean useSpeedAdaptiveDeadzone = false;

    public WatchCrownHandler(CrownListener listener) {
        this.listener = listener;
    }

    public void setCooldownMs(long cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public void setUseSpeedAdaptiveDeadzone(boolean use) {
        this.useSpeedAdaptiveDeadzone = use;
    }

    public void onScroll(float scrollDelta) {
        long now = System.currentTimeMillis();
        long dt = now - lastEventTime;
        
        // 如果两次滚动事件间隔超过 400 毫秒，判定为新的手势开始，重置累积器
        if (dt > 400) {
            accumulatedDelta = 0f;
            dt = 400;
        }
        lastEventTime = now;

        // 检测旋转方向的突变，突变时重置累积，防止滞后感
        if ((scrollDelta > 0 && accumulatedDelta < 0) || (scrollDelta < 0 && accumulatedDelta > 0)) {
            accumulatedDelta = 0f;
        }

        accumulatedDelta += scrollDelta;

        float activeThreshold = threshold;
        if (useSpeedAdaptiveDeadzone) {
            // dt 越大，说明转得越慢。
            // 慢速旋转时增加死区阈值以过滤手抖微震；快速旋转时降低阈值以保证跟手响应。
            if (dt > 100) {
                float factor = 1.0f + Math.min(200f, dt - 100f) * 0.0025f; // 最大 1.5 倍
                activeThreshold = threshold * factor;
            } else if (dt < 40) {
                activeThreshold = threshold * 0.8f;
            }
        }

        if (Math.abs(accumulatedDelta) >= activeThreshold) {
            if (now - lastTriggerTime >= cooldownMs) {
                if (accumulatedDelta > 0) {
                    listener.onStepClockwise();
                } else {
                    listener.onStepCounterClockwise();
                }
                lastTriggerTime = now;
                accumulatedDelta = 0f;
            }
            // 冷却时间未过时，保留累积值等待下一次滚动，防止输入数据丢失
        }
    }
}
