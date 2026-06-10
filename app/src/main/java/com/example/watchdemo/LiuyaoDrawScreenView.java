package com.example.watchdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;

public class LiuyaoDrawScreenView extends View {
    private final MainActivity activity;
    private float taijiAngle = 0f;

    // Cache Paint and RectFs to avoid allocations per frame
    private final Paint mPaint;
    private final android.graphics.RectF mRingRect = new android.graphics.RectF();
    private final android.graphics.RectF mShakeRect = new android.graphics.RectF();

    private long lastTime = 0L;

    private final Runnable taijiRotateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isShown() && getWindowVisibility() == VISIBLE && 
                activity.liuyaoRollCount == 0 && !activity.isCoinsRolling && !activity.isShowingCoinResult) {
                long now = System.currentTimeMillis();
                if (lastTime > 0) {
                    long dt = now - lastTime;
                    taijiAngle = (taijiAngle + 0.024f * dt) % 360f; // 24度每秒
                }
                lastTime = now;
                postInvalidateOnAnimation();
                postOnAnimation(this);
            } else {
                lastTime = 0L;
            }
        }
    };

    public LiuyaoDrawScreenView(MainActivity activity) {
        super(activity);
        this.activity = activity;
        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(taijiRotateRunnable);
        lastTime = 0L;
        postOnAnimation(taijiRotateRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(taijiRotateRunnable);
        lastTime = 0L;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            removeCallbacks(taijiRotateRunnable);
            lastTime = 0L;
            postOnAnimation(taijiRotateRunnable);
        } else {
            removeCallbacks(taijiRotateRunnable);
            lastTime = 0L;
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            removeCallbacks(taijiRotateRunnable);
            lastTime = 0L;
            postOnAnimation(taijiRotateRunnable);
        } else {
            removeCallbacks(taijiRotateRunnable);
            lastTime = 0L;
        }
    }

    public void updateText() {
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // 1. 背景绘制：纯黑色
        canvas.drawColor(Color.BLACK);

        mPaint.reset();
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        float cx = w / 2f;
        float cy = h / 2f;
        float R = Math.min(w, h) / 2f;

        // 2. 6 根装饰线圈的半径计算 (整体弧线往外靠，给中间留下更大空间)
        float R_0 = 52 * activity.density;             // 最内圈半径从 50dp 扩大至 52dp，中心卦象可显示更大
        float R_5 = R - 7 * activity.density;           // 最外圈从 R-10dp 扩大至 R-7dp，尽量向边缘贴合
        float dR = (R_5 - R_0) / 5f;

        // 三等分点对应的角度 (90°底，210°左上，330°右上，呈倒等边三角形)
        float[] baseAngles = {90f, 210f, 330f};

        // 3. 绘制 6 根同心装饰线圈
        for (int j = 0; j < 6; j++) {
            float r_ring = R_0 + j * dR;
            mRingRect.set(cx - r_ring, cy - r_ring, cx + r_ring, cy + r_ring);
            
            // 每次抽爻由外向内转换：第一爻(i=0)改变最外圈(j=5)
            int castIdx = 5 - j;
            float ringAngle = activity.liuyaoRingAngles[castIdx];

            // 计算该圈的自适应弧度与间距，保持所有圈的物理间隔与最内圈(R_0)一致
            float ratio = R_0 / r_ring;
            float gapAngle = 8f * ratio;         // 最内圈间隙为 8°
            float splitAngle = 4f * ratio;       // 最内圈阴爻断开间隙为 4°
            float halfSweep = 60f - gapAngle / 2f;
            
            if (castIdx < activity.liuyaoRollCount) {
                // 已掷出的爻环：三等分弧线直接渲染该爻的结果（阳金阴银，实线/断开，以及修饰点）
                int val = activity.liuyaoLineResults[castIdx];
                boolean isYang = (val == 7 || val == 9);
                boolean isOld = (val == 6 || val == 9);

                int ringColor;
                if (val == 7) {
                    ringColor = Color.parseColor("#F5E6CA");
                } else if (val == 8) {
                    ringColor = Color.parseColor("#8B7355");
                } else if (val == 9) {
                    ringColor = Color.parseColor("#C9A96E");
                } else {
                    ringColor = Color.parseColor("#C84B31");
                }

                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(1.5f * activity.density); // 爻线变换时粗细保持 1.5dp 不变
                mPaint.setColor(ringColor); // 100%不透明

                // 绘制三等分弧线
                for (int k = 0; k < 3; k++) {
                    float theta = baseAngles[k] + ringAngle;
                    if (isYang) {
                        canvas.drawArc(mRingRect, theta - halfSweep, halfSweep * 2f, false, mPaint);
                    } else {
                        canvas.drawArc(mRingRect, theta - halfSweep, halfSweep - splitAngle / 2f, false, mPaint);
                        canvas.drawArc(mRingRect, theta + splitAngle / 2f, halfSweep - splitAngle / 2f, false, mPaint);
                    }
                }

                // 绘制装饰小圆点
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setColor(ringColor);
                float dotRadius = 1.5f * activity.density;

                for (int k = 0; k < 3; k++) {
                    float theta = baseAngles[k] + ringAngle;
                    if (isYang) {
                        // 阳爻：
                        // 1. 绘制 1/3 处的中心主圆点
                        drawSingleDot(canvas, cx, cy, r_ring, theta, dotRadius, mPaint);
                        if (isOld) {
                            // 老阳：在中心主圆点两侧额外多出两个新的装饰圆点
                            drawSingleDot(canvas, cx, cy, r_ring, theta - 8f * ratio, dotRadius, mPaint);
                            drawSingleDot(canvas, cx, cy, r_ring, theta + 8f * ratio, dotRadius, mPaint);
                        }
                    } else {
                        // 阴爻（银色）装饰点：
                        drawSingleDot(canvas, cx, cy, r_ring, theta - halfSweep, dotRadius, mPaint);
                        drawSingleDot(canvas, cx, cy, r_ring, theta - splitAngle / 2f, dotRadius, mPaint);
                        drawSingleDot(canvas, cx, cy, r_ring, theta + splitAngle / 2f, dotRadius, mPaint);
                        drawSingleDot(canvas, cx, cy, r_ring, theta + halfSweep, dotRadius, mPaint);
                        
                        if (isOld) {
                            // 老阴：在两端的装饰点之间多一个装饰点
                            float midAngle = (halfSweep + splitAngle / 2f) / 2f;
                            drawSingleDot(canvas, cx, cy, r_ring, theta - midAngle, dotRadius, mPaint);
                            drawSingleDot(canvas, cx, cy, r_ring, theta + midAngle, dotRadius, mPaint);
                        }
                    }
                }

            } else if (castIdx == activity.liuyaoRollCount && activity.isShowingCoinResult) {
                // 停止旋转后的 1.5s 显示投币结果阶段：分别渲染三个币的结果（金/银，实线/断开，以及修饰点）
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(1.5f * activity.density);

                for (int k = 0; k < 3; k++) {
                    boolean isGold = activity.currentCoinResults[k];
                    mPaint.setColor(Color.parseColor(isGold ? "#F5E6CA" : "#8B7355")); // 依靠颜色显示单个币的结果

                    float theta = baseAngles[k] + ringAngle;
                    if (isGold) {
                        canvas.drawArc(mRingRect, theta - halfSweep, halfSweep * 2f, false, mPaint);
                    } else {
                        canvas.drawArc(mRingRect, theta - halfSweep, halfSweep - splitAngle / 2f, false, mPaint);
                        canvas.drawArc(mRingRect, theta + splitAngle / 2f, halfSweep - splitAngle / 2f, false, mPaint);
                    }
                }

                // 绘制各币对应的点
                mPaint.setStyle(Paint.Style.FILL);
                float dotRadius = 1.5f * activity.density;

                for (int k = 0; k < 3; k++) {
                    boolean isGold = activity.currentCoinResults[k];
                    mPaint.setColor(Color.parseColor(isGold ? "#F5E6CA" : "#8B7355"));
                    float theta = baseAngles[k] + ringAngle;
                    if (isGold) {
                        drawSingleDot(canvas, cx, cy, r_ring, theta, dotRadius, mPaint);
                    } else {
                        drawSingleDot(canvas, cx, cy, r_ring, theta - halfSweep, dotRadius, mPaint);
                        drawSingleDot(canvas, cx, cy, r_ring, theta - splitAngle / 2f, dotRadius, mPaint);
                        drawSingleDot(canvas, cx, cy, r_ring, theta + splitAngle / 2f, dotRadius, mPaint);
                        drawSingleDot(canvas, cx, cy, r_ring, theta + halfSweep, dotRadius, mPaint);
                    }
                }

            } else if (castIdx == activity.liuyaoRollCount && activity.isCoinsRolling) {
                // 正在投掷/翻滚的当前圈层：三等分弧线各自随机闪烁金/银、实线/断开，模拟抛掷过程
                for (int k = 0; k < 3; k++) {
                    boolean isGold = Math.random() > 0.5;
                    mPaint.setStyle(Paint.Style.STROKE);
                    mPaint.setStrokeWidth(1.2f * activity.density);
                    mPaint.setColor(Color.parseColor(isGold ? "#80F5E6CA" : "#808B7355")); // 使用结果页的少阳/少阴色，带 50% 透明度

                    float theta = baseAngles[k] + ringAngle;
                    // 加入少许抖动以增强物理动态感
                    float shakeOffset = (float)(Math.random() - 0.5) * 1.5f * activity.density;
                    float r_shake = r_ring + shakeOffset;
                    mShakeRect.set(cx - r_shake, cy - r_shake, cx + r_shake, cy + r_shake);

                    if (isGold) {
                        canvas.drawArc(mShakeRect, theta - halfSweep, halfSweep * 2f, false, mPaint);
                    } else {
                        canvas.drawArc(mShakeRect, theta - halfSweep, halfSweep - splitAngle / 2f, false, mPaint);
                        canvas.drawArc(mShakeRect, theta + splitAngle / 2f, halfSweep - splitAngle / 2f, false, mPaint);
                    }
                }
            } else {
                // 未起卦的装饰线圈（一开始是完全连在一起的整圈圆）
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(1.0f * activity.density);
                mPaint.setColor(Color.parseColor("#25C9A96E")); // 25% 透明暗金色 (C9A96E)
                canvas.drawCircle(cx, cy, r_ring, mPaint);
                
                // 绘制未起卦的 3 个主顶点圆点
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setColor(Color.parseColor("#45C9A96E")); // 45% 透明暗金色 (C9A96E)
                for (float angle : baseAngles) {
                    drawSingleDot(canvas, cx, cy, r_ring, angle + ringAngle, 1.2f * activity.density, mPaint);
                }
            }
        }

        // 4. 绘制中心区域内容 (欢迎状态的太极图 或 随着起卦出现的卦象，伴随过渡淡入淡出与缩放动画)
        boolean showTaiji = false;
        boolean showHexagram = false;
        float taijiAlpha = 1.0f;
        float taijiScale = 1.0f;
        float hexagramAlpha = 0.0f;
        float hexagramScale = 1.0f;

        if (activity.liuyaoRollCount == 0) {
            if (activity.taijiTransitionStartTime == 0L) {
                // 起卦前，展示太极图
                showTaiji = true;
                showHexagram = false;
                taijiAlpha = 1.0f;
                taijiScale = 1.0f;
            } else {
                // 开始第一次掷爻，执行 500ms 太极->卦象过渡动画
                long elapsed = System.currentTimeMillis() - activity.taijiTransitionStartTime;
                float progress = Math.min(1.0f, elapsed / 500f);
                showTaiji = (progress < 1.0f);
                showHexagram = true;
                taijiAlpha = 1.0f - progress;
                taijiScale = 1.0f - progress;
                hexagramAlpha = progress;
                hexagramScale = 0.8f + 0.2f * progress; // 卦象从 80% 尺寸平滑放大到 100%
                
                if (progress < 1.0f) {
                    invalidate(); // 强制在动画帧更新时触发下一帧重绘
                }
            }
        } else {
            // 已有掷爻 data，只显示六爻卦象
            showTaiji = false;
            showHexagram = true;
            hexagramAlpha = 1.0f;
            hexagramScale = 1.0f;
        }

        if (showTaiji) {
            float taijiR = 20 * activity.density;
            canvas.save();
            canvas.translate(cx, cy);
            canvas.scale(taijiScale, taijiScale);
            canvas.rotate(taijiAngle);

            int alphaVal = Math.round(taijiAlpha * 255);

            // 绘制阳半圆 (金色F5E6CA)
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(Color.parseColor("#F5E6CA"));
            mPaint.setAlpha(alphaVal);
            canvas.drawArc(-taijiR, -taijiR, taijiR, taijiR, -90f, 180f, true, mPaint);

            // 绘制阴半圆 (深紫灰2D2D44)
            mPaint.setColor(Color.parseColor("#2D2D44"));
            mPaint.setAlpha(alphaVal);
            canvas.drawArc(-taijiR, -taijiR, taijiR, taijiR, 90f, 180f, true, mPaint);

            // 绘制鱼头圆
            mPaint.setColor(Color.parseColor("#F5E6CA"));
            mPaint.setAlpha(alphaVal);
            canvas.drawCircle(0, -taijiR / 2f, taijiR / 2f, mPaint);

            mPaint.setColor(Color.parseColor("#2D2D44"));
            mPaint.setAlpha(alphaVal);
            canvas.drawCircle(0, taijiR / 2f, taijiR / 2f, mPaint);

            // 绘制鱼眼
            mPaint.setColor(Color.parseColor("#2D2D44"));
            mPaint.setAlpha(alphaVal);
            canvas.drawCircle(0, -taijiR / 2f, taijiR / 6f, mPaint);

            mPaint.setColor(Color.parseColor("#F5E6CA"));
            mPaint.setAlpha(alphaVal);
            canvas.drawCircle(0, taijiR / 2f, taijiR / 6f, mPaint);

            // 绘制细金边圈 (C9A96E)
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(1.0f * activity.density);
            mPaint.setColor(Color.parseColor("#C9A96E"));
            mPaint.setAlpha(alphaVal);
            canvas.drawCircle(0, 0, taijiR, mPaint);

            canvas.restore();
        }

        if (showHexagram) {
            float lineW = 50 * activity.density;            // 从 28dp 增大到 50dp
            float thickness = 6.5f * activity.density;      // 从 3.0dp 增大到 6.5dp
            float gap = 7.5f * activity.density;            // 从 4.0dp 增大到 7.5dp
            float yinGap = 12.0f * activity.density;         // 从 6.0dp 增大到 12.0dp
            float H = 5 * (thickness + gap) + thickness;    // 总高度从 38dp 增大到 76.5dp

            int alphaVal = Math.round(hexagramAlpha * 255);

            canvas.save();
            canvas.translate(cx, cy);
            canvas.scale(hexagramScale, hexagramScale);

            mPaint.setStyle(Paint.Style.FILL);

            for (int i = 0; i < 6; i++) {
                float lineY = (H / 2f) - i * (thickness + gap) - (thickness / 2f);

                if (i < activity.liuyaoRollCount) {
                    // 已确定的爻线
                    int val = activity.liuyaoLineResults[i];
                    boolean isYang = (val == 7 || val == 9);
                    boolean isOld = (val == 6 || val == 9);
                    
                    int lineColor;
                    if (val == 7) {
                        lineColor = Color.parseColor("#F5E6CA");
                    } else if (val == 8) {
                        lineColor = Color.parseColor("#8B7355");
                    } else if (val == 9) {
                        lineColor = Color.parseColor("#C9A96E");
                    } else {
                        lineColor = Color.parseColor("#C84B31");
                    }
                    mPaint.setColor(lineColor);

                    if (isOld) {
                        // 绘制很薄一层半透明外侧辉光 (先于实心线绘制，保持核心边缘清晰)
                        mPaint.setStyle(Paint.Style.STROKE);
                        mPaint.setStrokeWidth(3.0f * activity.density);
                        mPaint.setAlpha(Math.round(alphaVal * 0.25f));
                        if (isYang) {
                            canvas.drawRect(-lineW / 2f, lineY - thickness / 2f, lineW / 2f, lineY + thickness / 2f, mPaint);
                        } else {
                            canvas.drawRect(-lineW / 2f, lineY - thickness / 2f, -yinGap / 2f, lineY + thickness / 2f, mPaint);
                            canvas.drawRect(yinGap / 2f, lineY - thickness / 2f, lineW / 2f, lineY + thickness / 2f, mPaint);
                        }
                    }

                    // 绘制实心爻线
                    mPaint.setStyle(Paint.Style.FILL);
                    mPaint.setAlpha(alphaVal);
                    if (isYang) {
                        canvas.drawRect(-lineW / 2f, lineY - thickness / 2f, lineW / 2f, lineY + thickness / 2f, mPaint);
                    } else {
                        canvas.drawRect(-lineW / 2f, lineY - thickness / 2f, -yinGap / 2f, lineY + thickness / 2f, mPaint);
                        canvas.drawRect(yinGap / 2f, lineY - thickness / 2f, lineW / 2f, lineY + thickness / 2f, mPaint);
                    }
                } else if (i == activity.liuyaoRollCount && activity.isShowingCoinResult) {
                    // 正在展示结果 of the current cast line
                    int val = activity.currentCastYaoValue;
                    boolean isYang = (val == 7 || val == 9);
                    boolean isOld = (val == 6 || val == 9);
                    
                    int lineColor;
                    if (val == 7) {
                        lineColor = Color.parseColor("#F5E6CA");
                    } else if (val == 8) {
                        lineColor = Color.parseColor("#8B7355");
                    } else if (val == 9) {
                        lineColor = Color.parseColor("#C9A96E");
                    } else {
                        lineColor = Color.parseColor("#C84B31");
                    }
                    mPaint.setColor(lineColor);

                    if (isOld) {
                        // 绘制很薄一层半透明外侧辉光
                        mPaint.setStyle(Paint.Style.STROKE);
                        mPaint.setStrokeWidth(3.0f * activity.density);
                        mPaint.setAlpha(Math.round(alphaVal * 0.25f));
                        if (isYang) {
                            canvas.drawRect(-lineW / 2f, lineY - thickness / 2f, lineW / 2f, lineY + thickness / 2f, mPaint);
                        } else {
                            canvas.drawRect(-lineW / 2f, lineY - thickness / 2f, -yinGap / 2f, lineY + thickness / 2f, mPaint);
                            canvas.drawRect(yinGap / 2f, lineY - thickness / 2f, lineW / 2f, lineY + thickness / 2f, mPaint);
                        }
                    }

                    // 绘制实心爻线
                    mPaint.setStyle(Paint.Style.FILL);
                    mPaint.setAlpha(alphaVal);
                    if (isYang) {
                        canvas.drawRect(-lineW / 2f, lineY - thickness / 2f, lineW / 2f, lineY + thickness / 2f, mPaint);
                    } else {
                        canvas.drawRect(-lineW / 2f, lineY - thickness / 2f, -yinGap / 2f, lineY + thickness / 2f, mPaint);
                        canvas.drawRect(yinGap / 2f, lineY - thickness / 2f, lineW / 2f, lineY + thickness / 2f, mPaint);
                    }
                } else if (i == activity.liuyaoRollCount && activity.isCoinsRolling) {
                    // 滚动中的当前爻线：随机闪烁
                    boolean flashYang = Math.random() > 0.5;
                    mPaint.setColor(Color.parseColor(flashYang ? "#80F5E6CA" : "#808B7355"));
                    mPaint.setAlpha(Math.round(hexagramAlpha * 128));

                    if (flashYang) {
                        canvas.drawRect(-lineW / 2f, lineY - thickness / 2f, lineW / 2f, lineY + thickness / 2f, mPaint);
                    } else {
                        canvas.drawRect(-lineW / 2f, lineY - thickness / 2f, -yinGap / 2f, lineY + thickness / 2f, mPaint);
                        canvas.drawRect(yinGap / 2f, lineY - thickness / 2f, lineW / 2f, lineY + thickness / 2f, mPaint);
                    }
                } else {
                    // 未开始的爻位：画极其淡的灰色占位符线
                    mPaint.setColor(Color.parseColor("#15FFFFFF"));
                    mPaint.setAlpha(Math.round(hexagramAlpha * 21));
                    canvas.drawRect(-lineW / 2f, lineY - thickness / 2f, lineW / 2f, lineY + thickness / 2f, mPaint);
                }
            }
            canvas.restore();
        }
    }

    private void drawSingleDot(Canvas canvas, float cx, float cy, float r_ring, float angle, float dotRadius, Paint paint) {
        double rad = Math.toRadians(angle);
        float dx = cx + (float)(Math.cos(rad) * r_ring);
        float dy = cy + (float)(Math.sin(rad) * r_ring);
        canvas.drawCircle(dx, dy, dotRadius, paint);
    }
    
    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if (activity.gestureDetector != null) {
            activity.gestureDetector.onTouchEvent(this, event);
            return true;
        }
        return super.dispatchTouchEvent(event);
    }
}
