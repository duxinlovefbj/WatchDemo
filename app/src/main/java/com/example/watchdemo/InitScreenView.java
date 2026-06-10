package com.example.watchdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.VibrationEffect;
import android.view.View;
import android.view.ViewGroup;

public class InitScreenView extends View {
    private final MainActivity activity;
    private boolean isRunning = false;
    private long elapsedTimeMs = 0;
    private long lastFrameTimeMs = 0;

    private static final int STATE_IDLE = 0;
    private static final int STATE_SWIPE_LEFT_ANIM = 1;
    private static final int STATE_SWIPE_RIGHT_ANIM = 2;
    private int transitionState = STATE_IDLE;
    private long transitionStartTimeMs = 0;
    private float density;

    // Cache objects for drawing to avoid allocation on draw frames
    private final Paint mTitlePaint;
    private final Paint mDiamondPaint;
    private final Paint mBgPaint;
    private final Paint mBorderPaint;
    private final Paint mCircPaint;
    private final Paint mDotPaint;
    private final Paint mLinePaint;
    private final Paint mTextPaint;
    private final Paint mTaijiPaint;
    private final Paint mOrbitPaint;
    private final Paint mCardPaint;
    private final Paint mStarPaint;
    private final Paint mBottomStarPaint;

    private final RectF mBtnRect = new RectF();
    private final RectF mTaijiRect = new RectF();
    private final RectF mCardRect = new RectF();

    private final Path mDiamondPath = new Path();
    private final Path mStarPath = new Path();
    private final Path mMoonPath = new Path();
    private final Path mCutPath = new Path();
    private final Path mSunPath = new Path();
    private final Path mInnerCrescent = new Path();
    private final Path mCutCircle = new Path();
    private final Path mBottomStarPath = new Path();

    private android.graphics.LinearGradient mBgShader;
    private float mCachedShaderH = -1f;

    private final android.view.Choreographer.FrameCallback frameCallback = new android.view.Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!isRunning)
                return;

            long now = System.currentTimeMillis();
            if (lastFrameTimeMs == 0) {
                lastFrameTimeMs = now;
            }
            long df = now - lastFrameTimeMs;
            lastFrameTimeMs = now;

            if (df > 100)
                df = 16;

            elapsedTimeMs += df;

            if (transitionState != STATE_IDLE) {
                long dt = now - transitionStartTimeMs;
                if (dt >= 350) {
                    transitionComplete();
                    return;
                }
            }

            invalidate();
            android.view.Choreographer.getInstance().postFrameCallback(this);
        }
    };

    public InitScreenView(MainActivity activity) {
        super(activity);
        this.activity = activity;
        this.density = getResources().getDisplayMetrics().density;
        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Initialize Paints
        mTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTitlePaint.setTextSize(17 * density);
        mTitlePaint.setColor(Color.WHITE);
        mTitlePaint.setFakeBoldText(true);
        mTitlePaint.setTextAlign(Paint.Align.CENTER);

        mDiamondPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDiamondPaint.setStyle(Paint.Style.FILL);
        mDiamondPaint.setColor(Color.WHITE);

        mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBgPaint.setStyle(Paint.Style.FILL);

        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(2f * density);
        mBorderPaint.setColor(Color.WHITE);

        mCircPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCircPaint.setStyle(Paint.Style.STROKE);
        mCircPaint.setStrokeWidth(0.8f * density);
        mCircPaint.setColor(Color.WHITE);

        mDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDotPaint.setStyle(Paint.Style.FILL);
        mDotPaint.setColor(Color.WHITE);

        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setStyle(Paint.Style.FILL);
        mLinePaint.setColor(Color.WHITE);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(13.5f * density);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setFakeBoldText(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mTaijiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mOrbitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOrbitPaint.setStyle(Paint.Style.STROKE);
        mOrbitPaint.setStrokeWidth(0.8f * density);
        mOrbitPaint.setColor(Color.WHITE);

        mCardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCardPaint.setStyle(Paint.Style.STROKE);
        mCardPaint.setStrokeWidth(1.2f * density);
        mCardPaint.setColor(Color.WHITE);

        mStarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStarPaint.setStyle(Paint.Style.FILL);
        mStarPaint.setColor(Color.WHITE);

        mBottomStarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBottomStarPaint.setStyle(Paint.Style.FILL);
        mBottomStarPaint.setColor(Color.WHITE);
    }

    public void startAnimation() {
        isRunning = true;
        lastFrameTimeMs = System.currentTimeMillis();
        android.view.Choreographer.getInstance().postFrameCallback(frameCallback);
    }

    public void stopAnimation() {
        isRunning = false;
    }

    public void startTransition(boolean isLeftSwipe) {
        if (transitionState != STATE_IDLE)
            return;
        transitionState = isLeftSwipe ? STATE_SWIPE_LEFT_ANIM : STATE_SWIPE_RIGHT_ANIM;
        transitionStartTimeMs = System.currentTimeMillis();
        activity.vibrateCustom(VibrationEffect.EFFECT_CLICK);
    }

    private void transitionComplete() {
        isRunning = false;
        final MainActivity.ScreenState nextState = (transitionState == STATE_SWIPE_LEFT_ANIM)
                ? MainActivity.ScreenState.LIUYAO_DRAW
                : MainActivity.ScreenState.TAROT_ARRAY_SELECT;

        post(new Runnable() {
            @Override
            public void run() {
                activity.switchScreen(nextState);
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0)
            return;

        // Draw solid black background
        canvas.drawColor(Color.BLACK);

        long now = System.currentTimeMillis();
        long dt = (transitionState != STATE_IDLE) ? (now - transitionStartTimeMs) : 0;

        // Enlarge buttons slightly (w*0.36f, h*0.52f) and place them at y=h*0.57f
        float btnW = w * 0.36f;
        float btnH = h * 0.52f;
        float btnCy = h * 0.57f;
        float btnOffsetX = w * 0.21f;

        float leftScale = 1.0f;
        float leftCx = w / 2f - btnOffsetX;
        float leftCy = btnCy;
        int leftAlpha = 255;

        float rightScale = 1.0f;
        float rightCx = w / 2f + btnOffsetX;
        float rightCy = btnCy;
        int rightAlpha = 255;

        int titleAlpha = 255;

        if (transitionState == STATE_SWIPE_LEFT_ANIM) {
            float progress = Math.min(1.0f, dt / 300f);
            float interp = 1f - (1f - progress) * (1f - progress);
            leftScale = 1.0f + 0.20f * interp;
            leftCx = (w / 2f - btnOffsetX) * (1f - interp) + (w / 2f) * interp;
            leftCy = btnCy * (1f - interp) + (h / 2f) * interp;
            rightAlpha = (int) (255 * (1f - progress));
            titleAlpha = (int) (255 * (1f - progress));
        } else if (transitionState == STATE_SWIPE_RIGHT_ANIM) {
            float progress = Math.min(1.0f, dt / 300f);
            float interp = 1f - (1f - progress) * (1f - progress);
            rightScale = 1.0f + 0.20f * interp;
            rightCx = (w / 2f + btnOffsetX) * (1f - interp) + (w / 2f) * interp;
            rightCy = btnCy * (1f - interp) + (h / 2f) * interp;
            leftAlpha = (int) (255 * (1f - progress));
            titleAlpha = (int) (255 * (1f - progress));
        }

        // 1. Draw top Title
        if (titleAlpha > 0) {
            drawTitle(canvas, w, h, titleAlpha);
        }

        // 2. Draw buttons in correct Z-order (Active button always drawn on top to prevent overlap peeking)
        if (transitionState == STATE_SWIPE_LEFT_ANIM) {
            if (rightAlpha > 0) {
                drawTarotButton(canvas, rightCx, rightCy, btnW, btnH, rightAlpha, rightScale);
            }
            if (leftAlpha > 0) {
                drawLiuyaoButton(canvas, leftCx, leftCy, btnW, btnH, leftAlpha, leftScale);
            }
        } else {
            if (leftAlpha > 0) {
                drawLiuyaoButton(canvas, leftCx, leftCy, btnW, btnH, leftAlpha, leftScale);
            }
            if (rightAlpha > 0) {
                drawTarotButton(canvas, rightCx, rightCy, btnW, btnH, rightAlpha, rightScale);
            }
        }
    }

    private void drawTitle(Canvas canvas, int w, int h, int alpha) {
        float cx = w / 2f;
        float cy = h * 0.16f; // Move text higher to the top edge

        mTitlePaint.setAlpha(alpha);

        String titleText = "星 曜";
        canvas.drawText(titleText, cx, cy, mTitlePaint);

        // Draw white diamonds and dots next to title
        float textWidth = mTitlePaint.measureText(titleText);
        float diamondOffset = textWidth / 2f + 12 * density;

        mDiamondPaint.setAlpha((int) (alpha * 0.7f));

        float dSize = 3f * density;

        // Draw left diamond and dot
        drawDiamond(canvas, cx - diamondOffset, cy - 5.5f * density, dSize, mDiamondPaint);
        canvas.drawCircle(cx - diamondOffset - 6 * density, cy - 5.5f * density, 0.8f * density, mDiamondPaint);

        // Draw right diamond and dot
        drawDiamond(canvas, cx + diamondOffset, cy - 5.5f * density, dSize, mDiamondPaint);
        canvas.drawCircle(cx + diamondOffset + 6 * density, cy - 5.5f * density, 0.8f * density, mDiamondPaint);
    }

    private void drawDiamond(Canvas canvas, float cx, float cy, float size, Paint paint) {
        mDiamondPath.reset();
        mDiamondPath.moveTo(cx, cy - size);
        mDiamondPath.lineTo(cx + size, cy);
        mDiamondPath.lineTo(cx, cy + size);
        mDiamondPath.lineTo(cx - size, cy);
        mDiamondPath.close();
        canvas.drawPath(mDiamondPath, paint);
    }

    private void drawLiuyaoButton(Canvas canvas, float cx, float cy, float w, float h, int alpha, float scale) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);

        mBtnRect.set(-w / 2f, -h / 2f, w / 2f, h / 2f);

        // Draw neutral dark capsule background
        if (mBgShader == null || mCachedShaderH != h) {
            mCachedShaderH = h;
            int colorStart = Color.parseColor("#1C1C1C");
            int colorEnd = Color.parseColor("#0A0A0A");
            mBgShader = new android.graphics.LinearGradient(
                    0, -h / 2f, 0, h / 2f, colorStart, colorEnd, android.graphics.Shader.TileMode.CLAMP);
        }
        mBgPaint.setShader(mBgShader);
        mBgPaint.setAlpha(alpha);
        canvas.drawRoundRect(mBtnRect, 30 * density, 30 * density, mBgPaint);

        // Draw white capsule border
        mBorderPaint.setAlpha((int) (alpha * 0.85f));
        canvas.drawRoundRect(mBtnRect, 30 * density, 30 * density, mBorderPaint);

        // Illustration Center Y at -28dp to avoid overlap
        float hexCy = -28 * density;

        // Draw thin decorative white circle
        mCircPaint.setAlpha((int) (alpha * 0.2f));
        float r_circ = 20 * density;
        canvas.drawCircle(0, hexCy, r_circ, mCircPaint);

        // Draw small dots at cardinal directions
        mDotPaint.setAlpha(alpha);
        float dotR = 1f * density;
        canvas.drawCircle(0, hexCy - r_circ, dotR, mDotPaint);
        canvas.drawCircle(0, hexCy + r_circ, dotR, mDotPaint);
        canvas.drawCircle(-r_circ, hexCy, dotR, mDotPaint);
        canvas.drawCircle(r_circ, hexCy, dotR, mDotPaint);

        // Draw vertical hexagram inside circle (randomly changes every 1.2s)
        mLinePaint.setAlpha(alpha);
        float lineW = 12 * density;
        float thickness = 1.8f * density;
        float lineGap = 2.5f * density;
        float totalH = 5 * (thickness + lineGap) + thickness;

        long timeId = elapsedTimeMs / 1200;
        boolean[] hexLines = new boolean[6];
        for (int i = 0; i < 6; i++) {
            int hash = (int) ((timeId + 37) * 31 + i * 17);
            hexLines[i] = (hash % 2 == 0);
        }

        for (int i = 0; i < 6; i++) {
            float ly = hexCy + (totalH / 2f) - i * (thickness + lineGap) - (thickness / 2f);
            boolean isSolid = hexLines[i];
            if (isSolid) {
                canvas.drawRect(-lineW / 2f, ly - thickness / 2f, lineW / 2f, ly + thickness / 2f, mLinePaint);
            } else {
                float splitW = 2.5f * density;
                canvas.drawRect(-lineW / 2f, ly - thickness / 2f, -splitW / 2f, ly + thickness / 2f, mLinePaint);
                canvas.drawRect(splitW / 2f, ly - thickness / 2f, lineW / 2f, ly + thickness / 2f, mLinePaint);
            }
        }

        // Draw Text "六爻" at Y = 18dp to avoid overlap
        mTextPaint.setAlpha(alpha);
        canvas.drawText("六爻", 0, 18 * density, mTextPaint);

        // Draw white Taiji symbol at Y = 33dp to avoid overlap (visually sized consistently)
        float taijiR = 7.5f * density;
        float taijiCy = 33 * density;
        canvas.save();

        mTaijiPaint.setStyle(Paint.Style.STROKE);
        mTaijiPaint.setStrokeWidth(1f * density);
        mTaijiPaint.setColor(Color.WHITE);
        mTaijiPaint.setAlpha(alpha);
        canvas.drawCircle(0, taijiCy, taijiR, mTaijiPaint);

        mTaijiPaint.setStyle(Paint.Style.FILL);
        mTaijiRect.set(-taijiR, taijiCy - taijiR, taijiR, taijiCy + taijiR);
        canvas.drawArc(mTaijiRect, -90, 180, true, mTaijiPaint);

        float tjSmallR = taijiR / 2f;
        canvas.drawCircle(0, taijiCy - tjSmallR, tjSmallR, mTaijiPaint);
        mTaijiPaint.setColor(Color.parseColor("#0A0A0A"));
        canvas.drawCircle(0, taijiCy + tjSmallR, tjSmallR, mTaijiPaint);

        canvas.drawCircle(0, taijiCy - tjSmallR, tjSmallR / 3f, mTaijiPaint);
        mTaijiPaint.setColor(Color.WHITE);
        canvas.drawCircle(0, taijiCy + tjSmallR, tjSmallR / 3f, mTaijiPaint);

        canvas.restore();
        canvas.restore();
    }

    private void drawTarotButton(Canvas canvas, float cx, float cy, float w, float h, int alpha, float scale) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);

        mBtnRect.set(-w / 2f, -h / 2f, w / 2f, h / 2f);

        // Draw neutral dark capsule background
        if (mBgShader == null || mCachedShaderH != h) {
            mCachedShaderH = h;
            int colorStart = Color.parseColor("#1C1C1C");
            int colorEnd = Color.parseColor("#0A0A0A");
            mBgShader = new android.graphics.LinearGradient(
                    0, -h / 2f, 0, h / 2f, colorStart, colorEnd, android.graphics.Shader.TileMode.CLAMP);
        }
        mBgPaint.setShader(mBgShader);
        mBgPaint.setAlpha(alpha);
        canvas.drawRoundRect(mBtnRect, 30 * density, 30 * density, mBgPaint);

        // Draw white capsule border
        mBorderPaint.setAlpha((int) (alpha * 0.85f));
        canvas.drawRoundRect(mBtnRect, 30 * density, 30 * density, mBorderPaint);

        // Illustration Center Y at -28dp to avoid overlap
        float cardCy = -28 * density;

        // Draw planetary orbits design
        mOrbitPaint.setAlpha((int) (alpha * 0.15f));
        canvas.drawCircle(0, cardCy, 20 * density, mOrbitPaint);
        canvas.drawCircle(0, cardCy, 25 * density, mOrbitPaint);

        // Calculate card dynamic organic rotation and Y-axis flip
        float cardRotation = (float) (elapsedTimeMs / 30f + Math.sin(elapsedTimeMs / 500.0) * 15.0);
        long cycle = elapsedTimeMs % 6000;
        boolean isFlippedSide = false;
        float scaleX = 1.0f;

        if (cycle < 2500) {
            isFlippedSide = false;
            scaleX = 1.0f;
        } else if (cycle < 3000) {
            float t = (cycle - 2500) / 500f;
            float angle = t * 180f;
            scaleX = (float) Math.cos(Math.toRadians(angle));
            isFlippedSide = (angle > 90f);
        } else if (cycle < 5500) {
            isFlippedSide = true;
            scaleX = -1.0f;
        } else {
            float t = (cycle - 5500) / 500f;
            float angle = 180f + t * 180f;
            scaleX = (float) Math.cos(Math.toRadians(angle));
            isFlippedSide = (angle < 270f);
        }

        canvas.save();
        canvas.translate(0, cardCy);
        canvas.scale(scaleX, 1.0f);
        canvas.rotate(cardRotation);

        // Draw Tarot card frame
        mCardPaint.setAlpha(alpha);
        float cardW = 16 * density;
        float cardH = 28 * density;
        mCardRect.set(-cardW / 2f, -cardH / 2f, cardW / 2f, cardH / 2f);
        canvas.drawRoundRect(mCardRect, 3 * density, 3 * density, mCardPaint);

        // Draw card interior
        mStarPaint.setAlpha(alpha);

        if (!isFlippedSide) {
            // Front side: Draw 8-pointed star in card center
            mStarPath.reset();
            float outerR = 5.5f * density;
            float innerR = 1.8f * density;
            for (int step = 0; step < 8; step++) {
                double angleRad = Math.toRadians(step * 45f);
                float xOuter = (float) (Math.cos(angleRad) * outerR);
                float yOuter = (float) (Math.sin(angleRad) * outerR);

                double angleInnerRad = Math.toRadians(step * 45f + 22.5f);
                float xInner = (float) (Math.cos(angleInnerRad) * innerR);
                float yInner = (float) (Math.sin(angleInnerRad) * innerR);

                if (step == 0) {
                    mStarPath.moveTo(xOuter, yOuter);
                } else {
                    mStarPath.lineTo(xOuter, yOuter);
                }
                mStarPath.lineTo(xInner, yInner);
            }
            mStarPath.close();
            canvas.drawPath(mStarPath, mStarPaint);

            // Draw small crescent moon next to star
            float moonX = 3 * density;
            float moonY = 3 * density;
            float moonR = 2.5f * density;
            canvas.save();
            mMoonPath.reset();
            mMoonPath.addCircle(moonX, moonY, moonR, Path.Direction.CW);
            mCutPath.reset();
            mCutPath.addCircle(moonX - 1.5f * density, moonY - 0.8f * density, moonR, Path.Direction.CW);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                mMoonPath.op(mCutPath, Path.Op.DIFFERENCE);
                canvas.drawPath(mMoonPath, mStarPaint);
            } else {
                canvas.drawCircle(moonX, moonY, moonR, mStarPaint);
            }
            canvas.restore();
        } else {
            // Back/Flipped side: Gothic Sun pattern (哥特式太阳图案)
            mSunPath.reset();
            float sunR = 2.8f * density;
            mSunPath.addCircle(0, 0, sunR, Path.Direction.CW);

            for (int i = 0; i < 8; i++) {
                float angleDeg = i * 45f;
                double angleRad = Math.toRadians(angleDeg);

                // Alternating: straight pointed rays (0, 90, 180, 270) and wavy flame rays (45, 135, 225, 315)
                if (i % 2 == 0) {
                    // Straight pointed ray (triangle)
                    float tipR = 6.8f * density;
                    float tipX = (float) (Math.cos(angleRad) * tipR);
                    float tipY = (float) (Math.sin(angleRad) * tipR);

                    double leftRad = Math.toRadians(angleDeg - 12f);
                    float leftX = (float) (Math.cos(leftRad) * sunR);
                    float leftY = (float) (Math.sin(leftRad) * sunR);

                    double rightRad = Math.toRadians(angleDeg + 12f);
                    float rightX = (float) (Math.cos(rightRad) * sunR);
                    float rightY = (float) (Math.sin(rightRad) * sunR);

                    mSunPath.moveTo(leftX, leftY);
                    mSunPath.lineTo(tipX, tipY);
                    mSunPath.lineTo(rightX, rightY);
                    mSunPath.close();
                } else {
                    // Wavy flame ray (curved)
                    float tipR = 6.2f * density;
                    double tipRad = Math.toRadians(angleDeg + 15f);
                    float tipX = (float) (Math.cos(tipRad) * tipR);
                    float tipY = (float) (Math.sin(tipRad) * tipR);

                    double leftRad = Math.toRadians(angleDeg - 12f);
                    float leftX = (float) (Math.cos(leftRad) * sunR);
                    float leftY = (float) (Math.sin(leftRad) * sunR);

                    double rightRad = Math.toRadians(angleDeg + 12f);
                    float rightX = (float) (Math.cos(rightRad) * sunR);
                    float rightY = (float) (Math.sin(rightRad) * sunR);

                    double ctrl1Rad = Math.toRadians(angleDeg + 5f);
                    float ctrl1R = 4.8f * density;
                    float ctrlX1 = (float) (Math.cos(ctrl1Rad) * ctrl1R);
                    float ctrlY1 = (float) (Math.sin(ctrl1Rad) * ctrl1R);

                    double ctrl2Rad = Math.toRadians(angleDeg + 18f);
                    float ctrl2R = 4.0f * density;
                    float ctrlX2 = (float) (Math.cos(ctrl2Rad) * ctrl2R);
                    float ctrlY2 = (float) (Math.sin(ctrl2Rad) * ctrl2R);

                    mSunPath.moveTo(leftX, leftY);
                    mSunPath.quadTo(ctrlX1, ctrlY1, tipX, tipY);
                    mSunPath.quadTo(ctrlX2, ctrlY2, rightX, rightY);
                    mSunPath.close();
                }
            }

            // Cut out an inner crescent shape inside the sun disk for a mystical/Gothic union look
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                mInnerCrescent.reset();
                mInnerCrescent.addCircle(0.4f * density, 0, 1.5f * density, Path.Direction.CW);
                mCutCircle.reset();
                mCutCircle.addCircle(-0.2f * density, -0.2f * density, 1.5f * density, Path.Direction.CW);
                mInnerCrescent.op(mCutCircle, Path.Op.DIFFERENCE);
                mSunPath.op(mInnerCrescent, Path.Op.DIFFERENCE);
            }

            canvas.drawPath(mSunPath, mStarPaint);
        }

        canvas.restore(); // restore card translate/scale/rotate

        // Draw Text "塔罗" at Y = 18dp to avoid overlap
        mTextPaint.setAlpha(alpha);
        canvas.drawText("塔罗", 0, 18 * density, mTextPaint);

        // Draw white compass star at Y = 33dp to avoid overlap (visually sized consistently)
        float bottomStarR = 8.5f * density;
        float bottomStarCy = 33 * density;
        mBottomStarPaint.setAlpha(alpha);

        mBottomStarPath.reset();
        float bInnerR = 2.5f * density;
        for (int step = 0; step < 8; step++) {
            double angleRad = Math.toRadians(step * 45f);
            float xOuter = (float) (Math.cos(angleRad) * bottomStarR);
            float yOuter = (float) (Math.sin(angleRad) * bottomStarR);

            double angleInnerRad = Math.toRadians(step * 45f + 22.5f);
            float xInner = (float) (Math.cos(angleInnerRad) * bInnerR);
            float yInner = (float) (Math.sin(angleInnerRad) * bInnerR);

            if (step == 0) {
                mBottomStarPath.moveTo(xOuter, bottomStarCy + yOuter);
            } else {
                mBottomStarPath.lineTo(xOuter, bottomStarCy + yOuter);
            }
            mBottomStarPath.lineTo(xInner, bottomStarCy + yInner);
        }
        mBottomStarPath.close();
        canvas.drawPath(mBottomStarPath, mBottomStarPaint);

        canvas.restore(); // restore button save
    }
}
