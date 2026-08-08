package com.example.watchdemo;

import android.os.VibrationEffect;
import android.view.View;

import java.util.Arrays;

/** Drives the six-step Liuyao casting animation and result transition. */
final class LiuyaoCastingController {
    private static final long FRAME_DELAY_MS = 66L;
    private static final float ROTATION_DEGREES_PER_SECOND = 360f;
    private static final long STOP_DELAY_MS = 350L;
    private static final long COIN_RESULT_DELAY_MS = 1500L;

    private final MainActivity activity;
    private long vibrateStartTime;
    private long lastScrollTime;
    private Runnable pendingCommit;

    LiuyaoCastingController(MainActivity activity) {
        this.activity = activity;
    }

    void reset() {
        activity.liuyaoRollCount = 0;
        activity.isCoinsRolling = false;
        activity.isShowingCoinResult = false;
        Arrays.fill(activity.currentCoinResults, false);
        activity.currentCastYaoValue = 0;
        activity.taijiTransitionStartTime = 0L;
        activity.liuyaoScrollIsClockwise = true;
        activity.liuyaoRingAngles = new float[6];
        activity.liuyaoRingDirections = new int[6];
        Arrays.fill(activity.liuyaoRingDirections, 1);
        Arrays.fill(activity.liuyaoLineResults, 0);
        if (pendingCommit != null && activity.mSafeContainer != null) {
            activity.mSafeContainer.removeCallbacks(pendingCommit);
            pendingCommit = null;
        }
    }

    void onScrollStep(boolean clockwise) {
        if (activity.liuyaoRollCount >= 6 || activity.isShowingCoinResult) return;

        activity.liuyaoScrollIsClockwise = clockwise;
        long now = System.currentTimeMillis();
        lastScrollTime = now;
        activity.vibrateCustom(VibrationEffect.EFFECT_TICK);
        cancelPendingCommit();

        activity.liuyaoRingDirections[activity.liuyaoRollCount] = clockwise ? 1 : -1;
        if (activity.isCoinsRolling) return;

        activity.isCoinsRolling = true;
        vibrateStartTime = now;
        if (activity.liuyaoRollCount == 0) activity.taijiTransitionStartTime = now;
        activity.mSafeContainer.post(animationFrame);
        activity.mSafeContainer.postDelayed(stopCheck, 100L);
    }

    void release() {
        cancelPendingCommit();
        activity.mSafeContainer.removeCallbacks(animationFrame);
        activity.mSafeContainer.removeCallbacks(stopCheck);
    }

    private final Runnable animationFrame = new Runnable() {
        @Override
        public void run() {
            if (!activity.isCoinsRolling) return;
            float step = ROTATION_DEGREES_PER_SECOND * FRAME_DELAY_MS / 1000f;
            if (activity.liuyaoScrollIsClockwise) step = -step;
            for (int i = 0; i < activity.liuyaoRingAngles.length; i++) {
                int direction = i < activity.liuyaoRollCount
                        ? activity.liuyaoRingDirections[i] : 1;
                activity.liuyaoRingAngles[i] += direction * step;
            }
            refreshDrawScreen();
            activity.mSafeContainer.postDelayed(this, FRAME_DELAY_MS);
        }
    };

    private final Runnable stopCheck = new Runnable() {
        @Override
        public void run() {
            if (!activity.isCoinsRolling) return;
            long now = System.currentTimeMillis();
            if (now - lastScrollTime < STOP_DELAY_MS) {
                activity.mSafeContainer.postDelayed(this, FRAME_DELAY_MS);
                return;
            }
            finishCoinRoll(now);
        }
    };

    private void finishCoinRoll(long now) {
        activity.isCoinsRolling = false;
        for (int i = 0; i < activity.liuyaoRingAngles.length; i++) {
            float rounded = Math.round(activity.liuyaoRingAngles[i] / 120f) * 120f;
            activity.liuyaoRingAngles[i] = ((rounded % 360f) + 360f) % 360f;
        }

        Sfc32 random = new Sfc32((int) (vibrateStartTime ^ now));
        int sum = 0;
        for (int i = 0; i < activity.currentCoinResults.length; i++) {
            activity.currentCoinResults[i] = random.next() < 0.5f;
            sum += activity.currentCoinResults[i] ? 3 : 2;
        }
        activity.currentCastYaoValue = sum;
        activity.isShowingCoinResult = true;
        refreshDrawScreen();

        pendingCommit = () -> {
            commitYao();
            pendingCommit = null;
        };
        activity.mSafeContainer.postDelayed(pendingCommit, COIN_RESULT_DELAY_MS);
    }

    private void commitYao() {
        activity.isShowingCoinResult = false;
        activity.liuyaoLineResults[activity.liuyaoRollCount] = activity.currentCastYaoValue;
        activity.liuyaoRollCount++;
        activity.vibrateCustom(VibrationEffect.EFFECT_CLICK);
        refreshDrawScreen();

        if (activity.liuyaoRollCount == 6) {
            activity.mSafeContainer.postDelayed(() -> {
                if (activity.currentScreen == MainActivity.ScreenState.LIUYAO_DRAW) {
                    activity.selectedHexagramLineIndex = 0;
                    activity.addLiuyaoHistory();
                    activity.currentScreen = MainActivity.ScreenState.LIUYAO_RESULT;
                    activity.renderScreen();
                }
            }, 1200L);
        }
    }

    private void refreshDrawScreen() {
        View child = activity.getActiveScreenView();
        if (child instanceof LiuyaoDrawScreenView) {
            ((LiuyaoDrawScreenView) child).updateText();
        }
    }

    private void cancelPendingCommit() {
        if (pendingCommit == null) return;
        activity.mSafeContainer.removeCallbacks(pendingCommit);
        pendingCommit = null;
    }

    static int generateSingleYao(int seed) {
        Sfc32 random = new Sfc32(seed);
        int sum = 0;
        for (int i = 0; i < 3; i++) sum += random.next() < 0.5f ? 3 : 2;
        return sum;
    }

    private static final class Sfc32 {
        private int a;
        private int b;
        private int c;
        private int d;

        Sfc32(int seed) {
            a = 0x9E3779B9;
            b = 0x243F6A88;
            c = 0xB7E15162;
            d = seed == 0 ? 12345 : Math.abs(seed);
            for (int i = 0; i < 12; i++) next();
        }

        float next() {
            int value = a + b;
            a = b ^ (b >>> 9);
            b = c + (c << 3);
            c = (c << 21) | (c >>> 11);
            d++;
            value += d;
            c += value;
            return (float) ((value & 0xFFFFFFFFL) / 4294967296.0);
        }
    }
}
