package com.example.watchdemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.VibrationEffect;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.InputStream;

public class TarotDrawScreenView extends FrameLayout {
    private final MainActivity activity;
    private Bitmap cardBackBitmap = null;
    private boolean isFastSlideUnlocked = false;
    private float glowFraction = 0f;
    private ValueAnimator glowAnimator = null;

    private FrameLayout cardContainer;
    private TextView tvCounter;
    private TextView tvTip;
    private View vIndicator;

    // 左右滑动的插值进度值
    private float cardProgress = -1f;
    private float targetProgress = -1f;
    private boolean isAnimating = false;
    private float drawAnimProgress = 0f;
    private boolean fillFromLeft = false; // 随机补位方向：true为左侧补位，false为右侧补位

    private int lastVibratedIndex = -1;
    private long lastCrownScrollTime = 0;
    private boolean isDragging = false;
    private float dragStartX = 0f;
    private float dragStartProgress = 0f;

    private ValueAnimator scrollAnimator = null;
    private ValueAnimator drawAnimator = null;
    private final View[] cardViews = new View[9];

    // 边框性能缓存：避免在每个动画帧内重复调用 findViewById 和 Color.parseColor (每帧约 18 次字符串解析 + 9 次
    // setStroke 重绘)
    private final GradientDrawable[] borderDrawables = new GradientDrawable[9];
    private int strokeWidthGold;
    private int strokeWidthGray;
    private static final int COLOR_BORDER_GOLD = Color.parseColor("#D4AF37");
    private static final int COLOR_BORDER_GRAY = Color.parseColor("#888888");

    public TarotDrawScreenView(MainActivity activity) {
        super(activity);
        this.activity = activity;
        this.isFastSlideUnlocked = activity.isTarotFastSlideUnlocked;
        this.glowFraction = this.isFastSlideUnlocked ? 1f : 0f;
        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LayoutInflater inflater = LayoutInflater.from(activity);
        View root = inflater.inflate(R.layout.screen_tarot_draw, this, true);

        cardContainer = root.findViewById(R.id.card_container);
        tvCounter = root.findViewById(R.id.tv_counter);
        tvTip = root.findViewById(R.id.tv_tip);
        vIndicator = root.findViewById(R.id.v_indicator);

        // 加载卡背图片资源
        try {
            InputStream is = activity.getAssets().open("image/CardBacks.png");
            cardBackBitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 创建 9 个卡牌卡槽子项
        for (int i = 0; i < 9; i++) {
            View card = inflater.inflate(R.layout.item_tarot_card_draw, cardContainer, false);
            ImageView ivCardBack = card.findViewById(R.id.iv_card_back);
            View vFallback = card.findViewById(R.id.v_card_fallback);

            if (cardBackBitmap != null) {
                ivCardBack.setImageBitmap(cardBackBitmap);
                ivCardBack.setVisibility(View.VISIBLE);
                vFallback.setVisibility(View.GONE);
            } else {
                ivCardBack.setVisibility(View.GONE);
                vFallback.setVisibility(View.VISIBLE);
            }

            cardViews[i] = card;
            cardContainer.addView(card);
        }

        // 初始化边框 Drawable 缓存（仅执行一次，彻底替代逐帧的 findViewById + parseColor + setStroke 开销）
        strokeWidthGold = (int) (2.2f * activity.density);
        strokeWidthGray = (int) (1.0f * activity.density);
        for (int i = 0; i < 9; i++) {
            View bv = cardViews[i].findViewById(R.id.v_card_border);
            if (bv != null && bv.getBackground() instanceof GradientDrawable) {
                borderDrawables[i] = (GradientDrawable) bv.getBackground();
            }
        }
        // 稳定状态：slot 4（中心）金色高亮，其余灰色；滚动过程中此状态不变，无需逐帧更新
        applyBordersStable();
        setWillNotDraw(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (cardProgress < 0) {
            cardProgress = activity.tarotSelectedCardIndex;
        }
        targetProgress = cardProgress;
        lastVibratedIndex = Math.round(cardProgress);
        updateCardPositions(cardProgress, 0f);
        updateTexts();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateTexts();
        updateSwipeBackState();
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    private void updateTexts() {
        if (tvCounter != null) {
            if (activity.tarotTargetCount == -1) {
                tvCounter.setText("已抽 " + activity.tarotDrawnCount + " 张");
            } else {
                tvCounter.setText("已选 " + activity.tarotDrawnCount + " / " + activity.tarotTargetCount + " 张");
            }
        }
        if (tvTip != null) {
            String stateTip = isFastSlideUnlocked ? "左右滑牌" : "长按解锁";
            tvTip.setText(stateTip + " · 上滑抽牌");
        }
    }

    public void handleLongPress(float x, float y) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float cx = w / 2f;
        float cy = h / 2f;
        float dx = x - cx;
        float dy = y - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        float centerThreshold = 100f * activity.density;
        if (dist <= centerThreshold) {
            isFastSlideUnlocked = !isFastSlideUnlocked;
            activity.isTarotFastSlideUnlocked = isFastSlideUnlocked;
            activity.vibrateCustom(android.os.VibrationEffect.EFFECT_CLICK);
            animateGlow(isFastSlideUnlocked);
            updateTexts();
            updateSwipeBackState();
        }
    }

    private void animateGlow(boolean show) {
        if (glowAnimator != null) {
            glowAnimator.cancel();
        }
        float start = glowFraction;
        float end = show ? 1f : 0f;
        glowAnimator = ValueAnimator.ofFloat(start, end);
        glowAnimator.setDuration(300);
        glowAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        glowAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                glowFraction = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        glowAnimator.start();
    }

    private void drawBezelGlow(android.graphics.Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float radius = Math.min(w, h) / 2f;
        float cx = w / 2f;
        float cy = h / 2f;
        float r = radius - 6 * activity.density;

        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        p.setStyle(android.graphics.Paint.Style.STROKE);
        p.setStrokeCap(android.graphics.Paint.Cap.ROUND);

        android.graphics.RectF oval = new android.graphics.RectF(cx - r, cy - r, cx + r, cy + r);
        int goldColor = android.graphics.Color.parseColor("#D4AF37");

        // 1. 绘制外圈宽泛发光层
        p.setStrokeWidth(5f * activity.density);
        p.setColor(goldColor);
        p.setAlpha((int) (60 * glowFraction));
        canvas.drawArc(oval, -90f, 360f * glowFraction, false, p);

        // 2. 绘制内圈清晰核心层
        p.setStrokeWidth(1.8f * activity.density);
        p.setAlpha((int) (255 * glowFraction));
        canvas.drawArc(oval, -90f, 360f * glowFraction, false, p);
    }

    @Override
    protected void dispatchDraw(android.graphics.Canvas canvas) {
        super.dispatchDraw(canvas);
        if (glowFraction > 0f) {
            drawBezelGlow(canvas);
        }
    }

    private void updateSwipeBackState() {
        if (getParent() instanceof SwipeBackLayout) {
            ((SwipeBackLayout) getParent()).setSwipeDisabled(isFastSlideUnlocked);
        }
    }

    private float linearAlpha(float offset) {
        float absOffset = Math.abs(offset);
        if (absOffset <= 1.2f) {
            return 1.0f;
        } else if (absOffset >= 3.5f) {
            return 0.0f;
        } else {
            return 1.0f - (absOffset - 1.2f) / (3.5f - 1.2f);
        }
    }

    /**
     * 计算稳定状态下各个卡槽的 X 轴偏移补偿量 (stableT)。
     * 该算法动态计算各张卡牌在特定 deck 大小下的缩放，从而精确得出物理堆叠累加后的无缝贴合间距。
     */
    private float[] computeStableT(int S, float progress) {
        float[] stableT = new float[9];
        if (S <= 0)
            return stableT;

        // 统一在坐标计算首部进行环形轨道归一化，支持外部绝对坐标系运行，防止数值越界和反向突变
        progress = (progress % S + S) % S;

        float cardW = 46 * activity.density;
        float[] stableScales = new float[9];
        int selectedIndex = Math.round(progress);

        for (int i = 0; i < 9; i++) {
            int k = i - 4;
            int targetIndex = (selectedIndex + k) % S;
            if (targetIndex < 0) {
                targetIndex += S;
            }

            float kFloat = targetIndex - progress;
            if (kFloat > S / 2f) {
                kFloat -= S;
            } else if (kFloat < -S / 2f) {
                kFloat += S;
            }
            stableScales[i] = 1.0f - Math.abs(kFloat) * 0.15f;
        }

        stableT[4] = 0f;
        // 左侧 slots (3 到 0) 的间距累加
        for (int j = 3; j >= 0; j--) {
            stableT[j] = stableT[j + 1]
                    + cardW / 2f * (1f - stableScales[j])
                    + cardW / 2f * (1f - stableScales[j + 1]);
        }
        // 右侧 slots (5 到 8) 的间距累加
        for (int j = 5; j <= 8; j++) {
            stableT[j] = stableT[j - 1]
                    - cardW / 2f * (1f - stableScales[j])
                    - cardW / 2f * (1f - stableScales[j - 1]);
        }
        return stableT;
    }

    private float[] computeStableTNoLoop(int S, float progress) {
        float[] stableT = new float[9];
        if (S <= 0)
            return stableT;

        float cardW = 46 * activity.density;
        float[] stableScales = new float[9];
        int selectedIndex = Math.round(progress);

        for (int i = 0; i < 9; i++) {
            int k = i - 4;
            int targetIndex = selectedIndex + k;
            if (targetIndex < 0 || targetIndex >= S) {
                stableScales[i] = 1.0f;
                continue;
            }
            float kFloat = targetIndex - progress;
            stableScales[i] = 1.0f - Math.abs(kFloat) * 0.15f;
        }

        stableT[4] = 0f;
        for (int j = 3; j >= 0; j--) {
            stableT[j] = stableT[j + 1]
                    + cardW / 2f * (1f - stableScales[j])
                    + cardW / 2f * (1f - stableScales[j + 1]);
        }
        for (int j = 5; j <= 8; j++) {
            stableT[j] = stableT[j - 1]
                    - cardW / 2f * (1f - stableScales[j])
                    - cardW / 2f * (1f - stableScales[j - 1]);
        }
        return stableT;
    }

    private void updateCardPositions(float progress, float drawProgress) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0)
            return;

        int S = activity.availableTarotCards.size();
        
        // 统一在坐标计算首部进行环形轨道归一化，支持外部绝对坐标系运行，防止数值越界和反向突变
        progress = (progress % S + S) % S;

        TextView tvEmpty = findViewById(R.id.tv_empty);
        if (S == 0) {
            if (tvEmpty != null)
                tvEmpty.setVisibility(View.VISIBLE);
            if (vIndicator != null)
                vIndicator.setVisibility(View.GONE);
            for (View card : cardViews) {
                card.setVisibility(View.GONE);
            }
            return;
        } else {
            if (tvEmpty != null)
                tvEmpty.setVisibility(View.GONE);
            if (vIndicator != null)
                vIndicator.setVisibility(View.VISIBLE);
        }

        int selectedIndex = Math.round(progress);
        float deckCx = w / 2f;
        float deckCy = h * 0.46f;
        float cardW = 46 * activity.density;
        float cardH = 80 * activity.density;
        float horizontalSpacing = 34 * activity.density;
        float yArcFactor = 4.5f * activity.density;

        if (drawProgress > 0) {
            if (S == 1) {
                // 仅剩一张卡牌时，直接进行向上飞出并缩放渐隐
                for (int i = 0; i < 9; i++) {
                    View card = cardViews[i];
                    if (i == 4) {
                        card.setVisibility(View.VISIBLE);
                        float t = drawProgress;
                        card.setScaleX(1.0f - t * 0.7f);
                        card.setScaleY(1.0f - t * 0.7f);
                        card.setAlpha(1.0f - t);
                        card.setTranslationX(deckCx - cardW / 2f);
                        card.setTranslationY(deckCy - cardH / 2f - t * (deckCy + cardH * 1.5f));
                    } else {
                        card.setVisibility(View.GONE);
                    }
                }
                return;
            }

            // S > 1 时的抽牌补位动画：将各卡牌的坐标、缩放与透明度在 startState 和 endState 之间进行插值。
            // 以此保证动画结尾处的每一帧均完全吻合新界面的起始状态，从根本上解决“补齐卡片动画挤压突变”的问题。
            float[] stableT_old = computeStableT(S, progress);

            // 计算动画结束时的选中卡牌索引
            int newSel;
            if (fillFromLeft) {
                newSel = (selectedIndex - 1 + S - 1) % (S - 1);
            } else {
                newSel = selectedIndex % (S - 1);
            }
            float[] stableT_new = computeStableT(S - 1, newSel);

            for (int i = 0; i < 9; i++) {
                View card = cardViews[i];
                int k = i - 4;
                int targetIndex = (selectedIndex + k) % S;
                if (targetIndex < 0) {
                    targetIndex += S;
                }

                float kFloat = targetIndex - progress;
                if (kFloat > S / 2f) {
                    kFloat -= S;
                } else if (kFloat < -S / 2f) {
                    kFloat += S;
                }

                // 1. 起始状态属性值 (startState)
                float startX = kFloat * horizontalSpacing + stableT_old[i];
                float startY = kFloat * kFloat * yArcFactor;
                float startScale = 1.0f - Math.abs(kFloat) * 0.15f;
                float startAlpha = linearAlpha(kFloat);
                boolean startVisible = (S >= 9 || Math.abs(k) * 2 < S);

                // 2. 结束状态属性值 (endState)
                float endX, endY, endScale, endAlpha;
                boolean endVisible;

                if (targetIndex == selectedIndex) {
                    // 当前被抽走的卡牌：向上飞走并消散
                    endX = 0f;
                    endY = -(deckCy + cardH * 1.5f);
                    endScale = 0.3f;
                    endAlpha = 0.0f;
                    endVisible = false;
                } else {
                    // 其余保留的卡牌
                    int newIdx = (targetIndex < selectedIndex) ? targetIndex : targetIndex - 1;
                    float newK = newIdx - newSel;
                    if (newK > (S - 1) / 2f) {
                        newK -= (S - 1);
                    } else if (newK < -(S - 1) / 2f) {
                        newK += (S - 1);
                    }

                    int newI = 4 + Math.round(newK);
                    if (newI >= 0 && newI < 9) {
                        endX = newK * horizontalSpacing + stableT_new[newI];
                    } else {
                        endX = newK * horizontalSpacing;
                    }
                    endY = newK * newK * yArcFactor;
                    endScale = 1.0f - Math.abs(newK) * 0.15f;
                    endVisible = ((S - 1) >= 9 || Math.abs(Math.round(newK)) * 2 < (S - 1));
                    endAlpha = endVisible ? linearAlpha(newK) : 0.0f;
                }

                // 3. 对属性值进行线性插值 (Liner Interpolation)
                float t = drawProgress;
                float cardX = startX * (1f - t) + endX * t;
                float cardY = startY * (1f - t) + endY * t;
                float cardScale = startScale * (1f - t) + endScale * t;
                float cardAlpha = startAlpha * (1f - t) + endAlpha * t;
                boolean cardVisible = startVisible && (targetIndex == selectedIndex || endVisible || t < 1.0f);

                if (cardVisible && cardAlpha > 0f) {
                    card.setVisibility(View.VISIBLE);
                    card.setScaleX(cardScale);
                    card.setScaleY(cardScale);
                    card.setAlpha(cardAlpha);
                    card.setTranslationX(deckCx - cardW / 2f + cardX);
                    card.setTranslationY(deckCy - cardH / 2f + cardY);

                    // 基于当前和最终插值视觉偏移计算 Z 轴高度 (setElevation)
                    float visualOffset = kFloat * (1f - t);
                    if (targetIndex != selectedIndex) {
                        int newIdx = (targetIndex < selectedIndex) ? targetIndex : targetIndex - 1;
                        float newK = newIdx - newSel;
                        if (newK > (S - 1) / 2f) {
                            newK -= (S - 1);
                        } else if (newK < -(S - 1) / 2f) {
                            newK += (S - 1);
                        }
                        visualOffset = kFloat * (1f - t) + newK * t;
                    }
                    card.setElevation((5f - Math.abs(visualOffset)) * activity.density);

                    // 边框已在 startDrawAnimation() 调用 applyBordersDrawing() 时统一设为灰色，此处无需逐帧更新
                } else {
                    card.setVisibility(View.GONE);
                }
            }
        } else {
            // 稳定状态、表冠滚动状态 或 拖拽滑动状态
            float[] stableT = isDragging ? computeStableTNoLoop(S, progress) : computeStableT(S, progress);
            for (int i = 0; i < 9; i++) {
                View card = cardViews[i];
                int k = i - 4;
                int targetIndex;
                float kFloat;

                if (isDragging) {
                    targetIndex = selectedIndex + k;
                    if (targetIndex < 0 || targetIndex >= S) {
                        card.setVisibility(View.GONE);
                        continue;
                    }
                    kFloat = targetIndex - progress;
                } else {
                    targetIndex = (selectedIndex + k) % S;
                    if (targetIndex < 0) {
                        targetIndex += S;
                    }
                    if (S < 9 && Math.abs(k) * 2 >= S) {
                        card.setVisibility(View.GONE);
                        continue;
                    }
                    kFloat = targetIndex - progress;
                    if (kFloat > S / 2f) {
                        kFloat -= S;
                    } else if (kFloat < -S / 2f) {
                        kFloat += S;
                    }
                }

                float scale = 1.0f - Math.abs(kFloat) * 0.15f;
                float alpha = linearAlpha(kFloat);
                float ty = (kFloat * kFloat) * yArcFactor;

                if (Math.abs(kFloat) < 3.5f && alpha > 0f) {
                    card.setVisibility(View.VISIBLE);
                    card.setScaleX(scale);
                    card.setScaleY(scale);
                    card.setAlpha(alpha);

                    float tx = kFloat * horizontalSpacing + stableT[i];
                    card.setTranslationX(deckCx - cardW / 2f + tx);
                    card.setTranslationY(deckCy - cardH / 2f + ty);

                    card.setElevation((5f - Math.abs(kFloat)) * activity.density);
                } else {
                    card.setVisibility(View.GONE);
                }
            }
        }
    }

    /**
     * 稳定浏览状态下的边框：中心 slot（编号 4）金色高亮，其余 slot 灰色。
     * 在构造器调用一次即可——滚动期间 slot 4 的 kFloat 始终 ∈ (-0.5, 0.5)，是永恒的中心选中项，无需逐帧变更。
     */
    private void applyBordersStable() {
        for (int i = 0; i < 9; i++) {
            if (borderDrawables[i] != null) {
                borderDrawables[i].setStroke(
                        i == 4 ? strokeWidthGold : strokeWidthGray,
                        i == 4 ? COLOR_BORDER_GOLD : COLOR_BORDER_GRAY);
            }
        }
    }

    /**
     * 抽牌动画期间：将所有卡牌边框统一设为灰色，避免金色高亮在动画中突变。
     * 在 startDrawAnimation() 开始时调用一次即可。
     */
    private void applyBordersDrawing() {
        for (int i = 0; i < 9; i++) {
            if (borderDrawables[i] != null) {
                borderDrawables[i].setStroke(strokeWidthGray, COLOR_BORDER_GRAY);
            }
        }
    }

    public void onCrownScroll(boolean clockwise) {
        long now = System.currentTimeMillis();
        if (now - lastCrownScrollTime < 25) { // 减少到 25ms 以进一步提升跟手与响应速度
            return;
        }
        lastCrownScrollTime = now;

        if (isAnimating)
            return;

        int S = activity.availableTarotCards.size();
        if (S <= 0)
            return;

        // 如果滚动动画没有在运行，将目标进度同步为当前卡片进度
        if (scrollAnimator == null || !scrollAnimator.isRunning()) {
            targetProgress = cardProgress;
        }

        if (clockwise) {
            targetProgress += 1.0f;
        } else {
            targetProgress -= 1.0f;
        }

        // 限制 targetProgress 的超前量，防止高频旋转下卡牌速度过快产生倒走幻觉（Wagon-wheel effect）及停止后的长距离回弹
        // 且利用 Math.floor / Math.ceil 确保限制后的目标值是绝对整数，从而在停止时能完美吸附对齐到中间指示器
        float diff = targetProgress - cardProgress;
        float maxAhead = Math.min(2.5f, S / 2.0f);
        if (diff > maxAhead) {
            targetProgress = (float) Math.floor(cardProgress + maxAhead);
        } else if (diff < -maxAhead) {
            targetProgress = (float) Math.ceil(cardProgress - maxAhead);
        }

        int targetIndex = (Math.round(targetProgress) % S + S) % S;
        activity.tarotSelectedCardIndex = targetIndex;

        // 立即触发震动，保证物理转动与触觉反馈零延迟，体感与垂直列表完全一致
        activity.vibrateCustom(VibrationEffect.EFFECT_TICK);

        animateScrollTo(targetProgress);
    }

    private void animateScrollTo(float targetProgressVal) {
        if (scrollAnimator != null) {
            scrollAnimator.cancel();
        }

        int S = activity.availableTarotCards.size();
        if (S <= 0)
            return;

        float start = cardProgress;
        float end = targetProgressVal;

        // 在绝对坐标系下，不需要任何 S/2 的环形调整，物理间距就是 algebraic difference
        float diff = end - start;

        final float finalEnd = end;
        targetProgress = finalEnd;
        scrollAnimator = ValueAnimator.ofFloat(start, finalEnd);
        scrollAnimator.setInterpolator(new DecelerateInterpolator(1.5f)); // 减速阻尼，与系统 OverScroller 体感一致
        
        // 根据滚动距离动态调整动画持续时间，设置更轻快敏捷的 90ms 基础过渡时间
        int duration = 90;
        float absDiff = Math.abs(diff);
        if (absDiff > 1.0f) {
            duration = 90 + (int) ((absDiff - 1.0f) * 40);
        }
        scrollAnimator.setDuration(duration);

        scrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                cardProgress = val;
                updateCardPositions(cardProgress, 0f);
            }
        });
        scrollAnimator.start();
    }

    public void startDrawAnimation() {
        if (isAnimating)
            return;

        int S = activity.availableTarotCards.size();
        if (S <= 0)
            return;

        isAnimating = true;
        applyBordersDrawing(); // 抽牌动画开始前统一设为灰色（一次性调用，替代逐帧更新）
        // 随机选择左侧或者右侧闭合补齐
        fillFromLeft = Math.random() > 0.5;

        if (scrollAnimator != null) {
            scrollAnimator.cancel();
        }

        drawAnimator = ValueAnimator.ofFloat(0f, 1f);
        drawAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        drawAnimator.setDuration(450);
        drawAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                drawAnimProgress = (float) animation.getAnimatedValue();
                updateCardPositions(cardProgress, drawAnimProgress);
            }
        });
        drawAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                drawAnimProgress = 0f;
                isAnimating = false;
                activity.executeDrawCardAction(fillFromLeft);
            }
        });
        drawAnimator.start();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (activity.gestureDetector != null) {
            activity.gestureDetector.onTouchEvent(this, event);
        }

        if (isAnimating) {
            return super.dispatchTouchEvent(event);
        }

        int w = getWidth();
        int S = activity.availableTarotCards.size();
        if (S <= 0 || w <= 0) {
            return super.dispatchTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dragStartX = event.getX();
                dragStartProgress = cardProgress;
                isDragging = false;
                if (scrollAnimator != null && scrollAnimator.isRunning()) {
                    scrollAnimator.cancel();
                }
                super.dispatchTouchEvent(event);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!isFastSlideUnlocked) {
                    break;
                }
                float dx = event.getX() - dragStartX;
                if (!isDragging && Math.abs(dx) > 8 * activity.density) {
                    isDragging = true;
                    if (scrollAnimator != null && scrollAnimator.isRunning()) {
                        scrollAnimator.cancel();
                    }
                    MotionEvent cancelEvent = MotionEvent.obtain(event);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                }

                if (isDragging) {
                    // 少量滑动即可到达牌堆头尾：设定滑动 0.6 倍屏幕宽度即可横跨整个牌堆 S
                    float progressDelta = -(dx / (w * 0.6f)) * S;
                    float newProgress = dragStartProgress + progressDelta;
                    if (newProgress < 0f) {
                        newProgress = 0f;
                    } else if (newProgress > S - 1) {
                        newProgress = S - 1;
                    }
                    cardProgress = newProgress;

                    int curIndex = Math.round(cardProgress);
                    if (curIndex != lastVibratedIndex && curIndex >= 0 && curIndex < S) {
                        activity.vibrateCustom(VibrationEffect.EFFECT_TICK);
                        lastVibratedIndex = curIndex;
                        activity.tarotSelectedCardIndex = curIndex;
                    }

                    updateCardPositions(cardProgress, 0f);
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;
                    targetProgress = Math.round(cardProgress);
                    activity.tarotSelectedCardIndex = (int) targetProgress;
                    animateScrollTo(targetProgress);
                    return true;
                }
                break;
        }

        return super.dispatchTouchEvent(event);
    }
}
