package com.example.watchdemo;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Owns watch haptic dispatch and the optional OPPO linear-motor integration. */
final class WatchHaptics {
    private static final String TAG = "WatchHaptics";
    private static final int OPPO_CROWN_TICK_EFFECT = 302;

    private final Vibrator vibrator;
    private final HandlerThread thread = new HandlerThread("crown_vibrate");
    private final Handler handler;
    private final VibrateRunnable vibrateRunnable = new VibrateRunnable();

    private Object linearMotorService;
    private Method linearMotorVibrateMethod;
    private Constructor<?> waveformBuilderConstructor;
    private Method setEffectTypeMethod;
    private Method setEffectStrengthMethod;
    private Method setEffectLoopMethod;
    private Method buildMethod;
    private boolean linearMotorSupported;

    WatchHaptics(Context context) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        initLinearMotor(context);
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    void vibrate(int effectId) {
        handler.removeCallbacks(vibrateRunnable);
        vibrateRunnable.effectId = effectId;
        handler.post(vibrateRunnable);
    }

    void release() {
        handler.removeCallbacksAndMessages(null);
        thread.quitSafely();
    }

    private void initLinearMotor(Context context) {
        try {
            linearMotorService = context.getSystemService("linearmotor");
            if (linearMotorService == null) return;

            Class<?> vibratorClass = Class.forName("android.os.linearmotorvibrator.LinearmotorVibrator");
            Class<?> effectClass = Class.forName("android.os.linearmotorvibrator.WaveformEffect");
            Class<?> builderClass = Class.forName("android.os.linearmotorvibrator.WaveformEffect$Builder");
            linearMotorVibrateMethod = vibratorClass.getMethod("vibrate", effectClass);
            waveformBuilderConstructor = builderClass.getConstructor();
            setEffectTypeMethod = builderClass.getMethod("setEffectType", int.class);
            setEffectStrengthMethod = builderClass.getMethod("setEffectStrength", int.class);
            setEffectLoopMethod = builderClass.getMethod("setEffectLoop", boolean.class);
            buildMethod = builderClass.getMethod("build");
            linearMotorSupported = true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize linear motor", e);
            linearMotorSupported = false;
        }
    }

    private boolean vibrateLinearMotor(int effectId) {
        if (!linearMotorSupported || linearMotorService == null) return false;
        try {
            int targetEffectId = effectId == VibrationEffect.EFFECT_TICK
                    ? OPPO_CROWN_TICK_EFFECT : effectId;
            Object builder = waveformBuilderConstructor.newInstance();
            setEffectTypeMethod.invoke(builder, targetEffectId);
            setEffectStrengthMethod.invoke(builder, 2);
            setEffectLoopMethod.invoke(builder, false);
            linearMotorVibrateMethod.invoke(linearMotorService, buildMethod.invoke(builder));
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Linear motor vibration failed; using Android vibrator", e);
            return false;
        }
    }

    private final class VibrateRunnable implements Runnable {
        private int effectId;

        @Override
        public void run() {
            if (vibrateLinearMotor(effectId) || vibrator == null || !vibrator.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(effectId));
            } else {
                vibrator.vibrate(effectId == VibrationEffect.EFFECT_TICK ? 10 : 50);
            }
        }
    }
}
