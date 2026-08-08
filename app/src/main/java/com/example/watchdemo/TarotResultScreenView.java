package com.example.watchdemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.VibrationEffect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class TarotResultScreenView extends FrameLayout {
    private final MainActivity activity;
    private final TarotBitmapStore bitmapStore = new TarotBitmapStore();

    // 三层级视图结构
    private FrameLayout layer1Layout;    // Layer 1: 牌阵全景
    private TextView tvTitle;            // Layer 1: 标题
    private FrameLayout spreadContainer; // 牌阵卡牌容器
    private View dimOverlay;            // Layer 1 与 Layer 2 之间的暗化遮罩
    private FrameLayout layer2Layout;    // Layer 2: 单牌详情
    private View detailCardView;        // 详情卡牌视图 (原生)
    private ScrollView infoScrollView;  // 释义滚动包裹容器
    private TextView detailTextView;    // 释义文本 (纯意思，居中对齐)
    private TextView positionTextView;  // 牌位文本

    // 单牌详情二段式动画状态 (Stage A = 0, Stage B = 1)
    private int detailState = 0;
    private ValueAnimator detailAnimator = null;

    // Swipe-to-dismiss 右滑返回状态
    private boolean isSwiping = false;
    private float startX = 0f;
    private float startY = 0f;
    private ValueAnimator dismissAnimator = null;

    // Crown scroll debounce throttle timestamp
    private long lastCrownTime = 0L;

    // Cache objects to avoid allocations in drawBezelGlow
    private final Paint mBezelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBezelOval = new RectF();

    public TarotResultScreenView(MainActivity activity) {
        super(activity);
        this.activity = activity;
        setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setWillNotDraw(false); // 启用 FrameLayout 的 onDraw 用以绘制霓虹圆环

        mBezelPaint.setStyle(Paint.Style.STROKE);
        mBezelPaint.setStrokeCap(Paint.Cap.ROUND);

        loadBitmaps();
        initLayouts();
    }

    private void loadBitmaps() {
        bitmapStore.load(activity.getAssets(), activity.tarotDrawnIndices, activity.tarotDrawnCount);
    }

    private void updateSwipeBackState() {
        if (getParent() instanceof SwipeBackLayout) {
            boolean disableSwipe = (activity.tarotResultLayer == 2);
            ((SwipeBackLayout) getParent()).setSwipeDisabled(disableSwipe);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        loadBitmaps();
        int w = getWidth();
        int h = getHeight();
        if (w > 0 && h > 0) {
            setupLayouts(w, h);
        }
        updateSwipeBackState();
    }

    private void initLayouts() {
        // 1. 初始化 Layer 1 (牌阵全景)
        layer1Layout = new FrameLayout(activity);
        layer1Layout.setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(layer1Layout);

        // 顶部标题 "抽牌结果"
        tvTitle = new TextView(activity);
        tvTitle.setText("抽牌结果");
        tvTitle.setTextSize(14);
        tvTitle.setTextColor(Color.parseColor("#D4AF37"));
        tvTitle.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        titleParams.topMargin = (int) (24 * activity.density);
        tvTitle.setLayoutParams(titleParams);
        layer1Layout.addView(tvTitle);

        // 牌阵卡牌容器 (铺满全屏，以便卡牌可以自由在安全区内布局，无视高度限制)
        spreadContainer = new FrameLayout(activity);
        FrameLayout.LayoutParams spreadParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        spreadContainer.setLayoutParams(spreadParams);
        layer1Layout.addView(spreadContainer);

        // 提前在构造器中 Inflate 并添加所有卡牌子项，避免在 onSizeChanged/onLayout 期间添加子项导致无法 measurement 的 Bug
        int targetCount = activity.tarotDrawnCount;
        LayoutInflater inflater = LayoutInflater.from(activity);
        for (int i = 0; i < targetCount; i++) {
            View card = inflater.inflate(R.layout.item_tarot_card_draw, spreadContainer, false);
            spreadContainer.addView(card);
        }

        // 2. 初始化 Dim 遮罩层
        dimOverlay = new View(activity);
        dimOverlay.setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dimOverlay.setBackgroundColor(Color.BLACK);
        dimOverlay.setAlpha(0f);
        dimOverlay.setVisibility(View.GONE);
        addView(dimOverlay);

        // 3. 初始化 Layer 2 (单牌详情)
        layer2Layout = new FrameLayout(activity);
        layer2Layout.setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layer2Layout.setVisibility(View.GONE);
        addView(layer2Layout);

        // 详情卡牌
        detailCardView = inflater.inflate(R.layout.item_tarot_card_draw, layer2Layout, false);
        FrameLayout.LayoutParams detailCardParams = new FrameLayout.LayoutParams(
                (int) (84 * activity.density), (int) (146 * activity.density));
        detailCardParams.gravity = Gravity.CENTER;
        detailCardView.setLayoutParams(detailCardParams);
        layer2Layout.addView(detailCardView);

        // 释义说明滚动栏
        infoScrollView = new ScrollView(activity);
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (62 * activity.density));
        scrollParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        scrollParams.bottomMargin = (int) (12 * activity.density);
        scrollParams.leftMargin = (int) (26 * activity.density);
        scrollParams.rightMargin = (int) (26 * activity.density);
        infoScrollView.setLayoutParams(scrollParams);

        LinearLayout infoContainer = new LinearLayout(activity);
        infoContainer.setOrientation(LinearLayout.VERTICAL);
        infoContainer.setGravity(Gravity.CENTER);
        infoContainer.setLayoutParams(new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        infoScrollView.addView(infoContainer);

        detailTextView = new TextView(activity);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        detailTextView.setLayoutParams(detailParams);
        detailTextView.setTextSize(13.5f);
        detailTextView.setTextColor(Color.parseColor("#E0E0E0"));
        detailTextView.setGravity(Gravity.CENTER);
        detailTextView.setLineSpacing(2 * activity.density, 1.1f);
        infoContainer.addView(detailTextView);

        positionTextView = new TextView(activity);
        LinearLayout.LayoutParams positionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        positionParams.topMargin = (int) (8 * activity.density); // 与卡片/文本外部间距一致，均为 8dp
        positionTextView.setLayoutParams(positionParams);
        positionTextView.setTextSize(10.5f); // 字体更小
        positionTextView.setTextColor(Color.parseColor("#999999")); // 浅灰色，突出对比
        positionTextView.setGravity(Gravity.CENTER);
        positionTextView.setLineSpacing(1 * activity.density, 1.1f);
        infoContainer.addView(positionTextView);

        layer2Layout.addView(infoScrollView);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setupLayouts(w, h);
    }

    private void setupLayouts(int w, int h) {
        layoutSpread(w, h);

        if (activity.tarotResultLayer == 1) {
            layer1Layout.setVisibility(View.VISIBLE);
            layer1Layout.setTranslationX(0f);
            tvTitle.setVisibility(View.VISIBLE);
            tvTitle.setAlpha(1.0f);
            dimOverlay.setVisibility(View.GONE);
            dimOverlay.setAlpha(0f);
            layer2Layout.setVisibility(View.GONE);
        } else {
            layer2Layout.setVisibility(View.VISIBLE);

            layoutDetail(w, h);

            // 如果是刚从全景牌阵点击跳转进来，则播放顺滑推入转场动画
            if (activity.tarotResultJustEnteredLayer2) {
                layer1Layout.setVisibility(View.VISIBLE);
                dimOverlay.setVisibility(View.VISIBLE);
                runEntryTransition(w);
            } else {
                layer2Layout.setTranslationX(0f);
                layer1Layout.setTranslationX(-w / 3f);
                dimOverlay.setAlpha(0.7f);
                tvTitle.setVisibility(View.GONE);
                tvTitle.setAlpha(0.0f);

                // 静态处于详情页时，完全隐藏背景，防止边缘露出
                layer1Layout.setVisibility(View.GONE);
                dimOverlay.setVisibility(View.GONE);
            }
        }
        updateSwipeBackState();
    }

    /**
     * 绑定原生卡牌组件数据并更新牌阵中它们的位置
     */
    private void layoutSpread(int w, int h) {
        int targetCount = activity.tarotTargetCount;
        int drawnCount = activity.tarotDrawnCount;
        if (drawnCount <= 0) return;

        for (int i = 0; i < drawnCount; i++) {
            if (i >= spreadContainer.getChildCount()) break;
            View card = spreadContainer.getChildAt(i);
            ImageView ivCardBack = card.findViewById(R.id.iv_card_back);
            View vFallback = card.findViewById(R.id.v_card_fallback);

            if (bitmapStore.get(i) != null) {
                ivCardBack.setImageBitmap(bitmapStore.get(i));
                ivCardBack.setVisibility(View.VISIBLE);
                vFallback.setVisibility(View.GONE);
            } else {
                ivCardBack.setVisibility(View.GONE);
                vFallback.setVisibility(View.VISIBLE);
            }

            View border = card.findViewById(R.id.v_card_border);
            if (border != null) {
                GradientDrawable borderDrawable = (GradientDrawable) border.getBackground();
                if (borderDrawable != null) {
                    borderDrawable = (GradientDrawable) borderDrawable.mutate();
                    if (i == activity.tarotResultDetailIndex) {
                        borderDrawable.setStroke((int) (1.8f * activity.density), Color.parseColor("#D4AF37"));
                    } else {
                        borderDrawable.setStroke((int) (0.8f * activity.density), Color.parseColor("#44FFFFFF"));
                    }
                }
            }

            float[] info = getVirtualCardCenterAndSize(i, targetCount, drawnCount, w, h);
            float cx = info[0];
            float cy = info[1];
            float cardW = info[2];
            float cardH = info[3];

            float defaultW = 46f * activity.density;
            float defaultH = 80f * activity.density;

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) card.getLayoutParams();
            if (lp == null) {
                lp = new FrameLayout.LayoutParams((int) defaultW, (int) defaultH);
            }
            lp.width = (int) defaultW;
            lp.height = (int) defaultH;
            lp.leftMargin = 0;
            lp.topMargin = 0;
            lp.rightMargin = 0;
            lp.bottomMargin = 0;
            lp.gravity = Gravity.TOP | Gravity.LEFT;
            card.setLayoutParams(lp);

            // 居中定位：将默认大小的卡牌中心平移到 (cx, cy)
            card.setTranslationX(cx - defaultW / 2f);
            card.setTranslationY(cy - defaultH / 2f);

            // 缩放尺寸与选卡高亮：选中放大 1.12 倍且完全亮起，非选中半透明暗化，提升选中卡 Z 轴高程
            float finalScaleX = cardW / defaultW;
            float finalScaleY = cardH / defaultH;
            if (i == activity.tarotResultDetailIndex) {
                finalScaleX *= 1.12f;
                finalScaleY *= 1.12f;
                card.setAlpha(1.0f);
                card.setElevation(5f * activity.density);
            } else {
                card.setAlpha(0.45f);
                card.setElevation(0f);
            }
            card.setScaleX(finalScaleX);
            card.setScaleY(finalScaleY);

            boolean isUpright = activity.tarotCardStates[i];
            boolean isRotated90 = (activity.tarotArraySelectedIndex == 4 && i == 1);
            boolean isRotated25 = (activity.tarotArraySelectedIndex == 12 && i == 5);
            float rotation = 0f;
            if (isRotated90) {
                rotation += 90f;
            }
            if (isRotated25) {
                rotation += 25f;
            }
            if (!isUpright) {
                rotation += 180f;
            }
            card.setRotation(rotation);

            final int index = i;
            card.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.tarotResultDetailIndex = index;
                    activity.tarotResultLayer = 2;
                    activity.tarotResultJustEnteredLayer2 = true;
                    activity.vibrateCustom(VibrationEffect.EFFECT_CLICK);
                    activity.renderScreen();
                }
            });
        }
        // 强制重绘/更新布局，应用动态计算的卡牌大小与定位
        spreadContainer.requestLayout();
    }

    /**
     * 配置单卡详情原生视图及动画初始状态
     */
    private void layoutDetail(int w, int h) {
        if (activity.tarotDrawnCount == 0 || activity.tarotResultDetailIndex >= activity.tarotDrawnCount) return;
        bindDetailCard();

        detailCardView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.vibrateCustom(VibrationEffect.EFFECT_CLICK);
                toggleDetailState();
            }
        });

        // 默认进入 Stage A (整张牌大屏，不显示文本)
        detailState = 0;
        detailCardView.setScaleX(1.0f);
        detailCardView.setScaleY(1.0f);
        detailCardView.setTranslationY(0f);
        infoScrollView.setAlpha(0f);
        infoScrollView.setTranslationY(32 * activity.density);
        infoScrollView.setVisibility(View.INVISIBLE);
    }

    /**
     * 触发二段式详情展示动画 (大牌居中 <-> 缩放至顶部并显现居中说明)
     */
    private void toggleDetailState() {
        if (detailAnimator != null) {
            detailAnimator.cancel();
        }

        final int targetState = (detailState == 0) ? 1 : 0;
        detailAnimator = ValueAnimator.ofFloat(detailState, targetState);
        detailAnimator.setDuration(280);
        detailAnimator.setInterpolator(new DecelerateInterpolator(1.2f));
        detailAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                applyDetailTransition(val);
            }
        });
        detailAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                detailState = targetState;
                if (detailState == 0) {
                    infoScrollView.setVisibility(View.INVISIBLE);
                }
            }
        });

        if (targetState == 1) {
            infoScrollView.setVisibility(View.VISIBLE);
        }
        detailAnimator.start();
    }

    private void applyDetailTransition(float val) {
        int h = getHeight();
        if (h <= 0) return;

        // 1. 动态计算 Stage B 状态下最优的卡牌高度与缩放比例
        float originalCardH = 146f * activity.density;
        float cardTop = 18f * activity.density; // 增加 3dp 安全距离，与圆环保持更宽裕的间距
        float textTop = h - (12f + 62f) * activity.density; // 说明文本框顶端位置
        float gap = 8f * activity.density; // 留出间距
        float cardH = Math.max(0, textTop - cardTop - gap);
        float targetScale = cardH / originalCardH;

        // 2. 动态计算缩放平移终点，使卡牌完美贴合卡牌高度区间
        float cyB = cardTop + cardH / 2f;
        float targetTranslationY = cyB - h / 2f;

        // 线性插值过度缩放比
        float scale = 1.0f - val * (1.0f - targetScale);
        detailCardView.setScaleX(scale);
        detailCardView.setScaleY(scale);

        // 线性插值过度平移量
        detailCardView.setTranslationY(val * targetTranslationY);

        // 文字渐变显现和滑入：0.0f -> 1.0f
        infoScrollView.setAlpha(val);
        infoScrollView.setTranslationY((1f - val) * 32 * activity.density);
    }

    /**
     * 运行右侧滑入 entry 转场动效
     */
    private void runEntryTransition(final int w) {
        activity.tarotResultJustEnteredLayer2 = false;

        layer2Layout.setTranslationX(w);
        layer1Layout.setTranslationX(0f);
        dimOverlay.setAlpha(0f);
        tvTitle.setVisibility(View.VISIBLE);
        tvTitle.setAlpha(1.0f);

        if (dismissAnimator != null) {
            dismissAnimator.cancel();
        }
        dismissAnimator = ValueAnimator.ofFloat(w, 0f);
        dismissAnimator.setDuration(260);
        dismissAnimator.setInterpolator(new DecelerateInterpolator(1.3f));
        dismissAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                layer2Layout.setTranslationX(val);
                float fraction = (w > 0) ? (val / w) : 0f;
                layer1Layout.setTranslationX(-w / 3f + fraction * (w / 3f));
                dimOverlay.setAlpha(0.7f * (1f - fraction));
                tvTitle.setAlpha(fraction); // 标题伴随转场慢慢滑出渐隐
            }
        });
        dismissAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                tvTitle.setVisibility(View.GONE);
                // 动画结束时完全隐藏底层
                layer1Layout.setVisibility(View.GONE);
                dimOverlay.setVisibility(View.GONE);
            }
        });
        dismissAnimator.start();
    }

    /**
     * 拦截与处理 Wear OS 右滑返回手势，自带阻尼和视差效果
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // 全局手势拦截与检测，确保长按返回在任何层级下均能正常触发
        if (activity.gestureDetector != null) {
            activity.gestureDetector.onTouchEvent(this, event);
        }

        if (activity.tarotResultLayer != 2) {
            return super.dispatchTouchEvent(event);
        }

        int w = getWidth();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getRawX();
                startY = event.getRawY();
                isSwiping = false;
                super.dispatchTouchEvent(event);
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - startX;
                float dy = event.getRawY() - startY;

                if (isSwiping) {
                    float translationX = Math.max(0, dx);
                    layer2Layout.setTranslationX(translationX);
                    float fraction = (w > 0) ? (translationX / w) : 0f;
                    layer1Layout.setTranslationX(-w / 3f + fraction * (w / 3f));
                    dimOverlay.setAlpha(0.7f * (1f - fraction));
                    if (tvTitle.getVisibility() != View.VISIBLE) {
                        tvTitle.setVisibility(View.VISIBLE);
                    }
                    tvTitle.setAlpha(fraction); // 右滑时 Layer 1 标题逐渐浮现

                    // 确保在滑动开始时，底层全景视图和暗化遮罩变为可见，以显示转场视差
                    if (layer1Layout.getVisibility() != View.VISIBLE) {
                        layer1Layout.setVisibility(View.VISIBLE);
                    }
                    if (dimOverlay.getVisibility() != View.VISIBLE) {
                        dimOverlay.setVisibility(View.VISIBLE);
                    }
                    return true;
                } else {
                    float threshold = 15 * activity.density;
                    if (dx > threshold && dx > Math.abs(dy) * 1.5f) {
                        isSwiping = true;
                        // 向下级子 View 发送 CANCEL 事件取消按压或点击触发
                        MotionEvent cancelEvent = MotionEvent.obtain(event);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(cancelEvent);
                        cancelEvent.recycle();
                        return true;
                    }
                    return super.dispatchTouchEvent(event);
                }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isSwiping) {
                    float finalDx = event.getRawX() - startX;
                    if (finalDx > w / 3f && event.getAction() == MotionEvent.ACTION_UP) {
                        animateDismiss();
                    } else {
                        animateCancelDismiss();
                    }
                    return true;
                }
                return super.dispatchTouchEvent(event);
        }
        return super.dispatchTouchEvent(event);
    }

    private void animateDismiss() {
        if (dismissAnimator != null) {
            dismissAnimator.cancel();
        }
        final float startVal = layer2Layout.getTranslationX();
        final int w = getWidth();
        dismissAnimator = ValueAnimator.ofFloat(startVal, w);
        dismissAnimator.setDuration(220);
        dismissAnimator.setInterpolator(new DecelerateInterpolator());
        dismissAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                layer2Layout.setTranslationX(val);
                float fraction = (w > 0) ? (val / w) : 0f;
                layer1Layout.setTranslationX(-w / 3f + fraction * (w / 3f));
                dimOverlay.setAlpha(0.7f * (1f - fraction));
                tvTitle.setAlpha(fraction);
            }
        });
        dismissAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isSwiping = false;
                activity.tarotResultLayer = 1;
                layer2Layout.setTranslationX(0f);
                layer1Layout.setTranslationX(0f);
                dimOverlay.setAlpha(0f);
                tvTitle.setVisibility(View.VISIBLE);
                tvTitle.setAlpha(1.0f);

                // 返回 Layer 1 时确保底层可见性正常，隐藏 Layer 2 详情
                layer1Layout.setVisibility(View.VISIBLE);
                dimOverlay.setVisibility(View.GONE);
                layer2Layout.setVisibility(View.GONE);

                // 局部渲染更新，规避重绘 activity.renderScreen() 导致的卡顿
                updateSpreadBorders();
                updateSwipeBackState();
            }
        });
        dismissAnimator.start();
    }

    private void animateCancelDismiss() {
        if (dismissAnimator != null) {
            dismissAnimator.cancel();
        }
        final float startVal = layer2Layout.getTranslationX();
        final int w = getWidth();
        dismissAnimator = ValueAnimator.ofFloat(startVal, 0f);
        dismissAnimator.setDuration(200);
        dismissAnimator.setInterpolator(new DecelerateInterpolator());
        dismissAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                layer2Layout.setTranslationX(val);
                float fraction = (w > 0) ? (val / w) : 0f;
                layer1Layout.setTranslationX(-w / 3f + fraction * (w / 3f));
                dimOverlay.setAlpha(0.7f * (1f - fraction));
                tvTitle.setAlpha(fraction);
            }
        });
        dismissAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isSwiping = false;
                tvTitle.setVisibility(View.GONE);
                // 取消返回后，再次隐藏底层
                layer1Layout.setVisibility(View.GONE);
                dimOverlay.setVisibility(View.GONE);
            }
        });
        dismissAnimator.start();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        // 发光圈置顶绘制，层叠在暗化遮罩（Dim Overlay）及所有卡牌最上方，保持始终高亮
        if (activity.tarotResultLayer == 2) {
            drawBezelGlow(canvas);
        }
    }

    /**
     * 在屏幕周边绘制两段融合式霓虹圆环 (上半2/3表示卡牌元素，下半1/3表示卡牌属性)
     */
    private void drawBezelGlow(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float radius = Math.min(w, h) / 2f;
        float cx = w / 2f;
        float cy = h / 2f;

        if (activity.tarotDrawnCount == 0 || activity.tarotResultDetailIndex >= activity.tarotDrawnCount) return;
        int cardIdx = activity.tarotDrawnIndices[activity.tarotResultDetailIndex];
        TarotDeck.TarotCard card = TarotDeck.TAROT_DECK[cardIdx];

        // 属性色 (宫廷牌=低饱和古铜金, 大阿卡纳=淡雅薰衣草紫, 普通小阿卡纳=微透白)
        int attributeColor;
        if (card.isCourt) {
            attributeColor = Color.parseColor("#C9A96E");
        } else if ("Major".equals(card.arcana)) {
            attributeColor = Color.parseColor("#9B8EB9");
        } else {
            attributeColor = Color.parseColor("#33FFFFFF");
        }

        // 元素色 (火=低饱和暗红, 水=低饱和钢蓝, 风=低饱和板岩灰, 土=低饱和鼠尾草绿)
        int elementColor;
        if ("Fire".equalsIgnoreCase(card.element)) {
            elementColor = Color.parseColor("#BD5A5A");
        } else if ("Water".equalsIgnoreCase(card.element)) {
            elementColor = Color.parseColor("#5A8CBD");
        } else if ("Air".equalsIgnoreCase(card.element)) {
            elementColor = Color.parseColor("#8E9AA8");
        } else { // Earth
            elementColor = Color.parseColor("#6E8C6E");
        }

        float r = radius - 6 * activity.density;
        mBezelOval.set(cx - r, cy - r, cx + r, cy + r);

        // 1. 绘制上半部 2/3 的元素弧 (150° 到 390°)
        mBezelPaint.setStrokeWidth(5f * activity.density);
        mBezelPaint.setColor(elementColor);
        mBezelPaint.setAlpha(60);
        canvas.drawArc(mBezelOval, 150f, 240f, false, mBezelPaint);

        mBezelPaint.setStrokeWidth(1.8f * activity.density);
        mBezelPaint.setAlpha(255);
        canvas.drawArc(mBezelOval, 150f, 240f, false, mBezelPaint);

        // 2. 绘制下半部 1/3 的属性弧 (30° 到 150°)
        mBezelPaint.setStrokeWidth(5f * activity.density);
        mBezelPaint.setColor(attributeColor);
        mBezelPaint.setAlpha(60);
        canvas.drawArc(mBezelOval, 30f, 120f, false, mBezelPaint);

        mBezelPaint.setStrokeWidth(1.8f * activity.density);
        mBezelPaint.setAlpha(255);
        canvas.drawArc(mBezelOval, 30f, 120f, false, mBezelPaint);
    }

    /**
     * 根据当前选中的卡牌以及牌阵类型，获取该卡牌在牌阵中的位置名称与位置含义
     */
    private String getCardPositionNameAndMeaning(int index) {
        return TarotSpreadMetadata.positionLabel(activity.tarotArraySelectedIndex, index);
    }

    /**
     * 获取指定索引卡牌的原始虚拟空间坐标
     */
    public float[] getVirtualCardCenterAndSize(int i, int targetCount, int drawnCount, int w, int h) {
        return TarotSpreadLayoutCalculator.cardBounds(
                activity.tarotArraySelectedIndex, i, drawnCount, w, h, activity.density);
    }
    public int getCardIndexAt(float x, float y) {
        int w = getWidth();
        int h = getHeight();
        int targetCount = activity.tarotTargetCount;
        int drawnCount = activity.tarotDrawnCount;

        int bestIndex = -1;
        float bestDist = Float.MAX_VALUE;
        float limit = 32 * activity.density;

        for (int i = drawnCount - 1; i >= 0; i--) {
            float[] info = getVirtualCardCenterAndSize(i, targetCount, drawnCount, w, h);
            float cx = info[0];
            float cy = info[1];
            float dx = x - cx;
            float dy = y - cy;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < bestDist && dist < limit) {
                bestDist = dist;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    /**
     * 响应表冠局部刷新滚动逻辑，毫秒级无缝同步
     */
    public void onCrownScroll(boolean clockwise) {
        long now = System.currentTimeMillis();
        if (now - lastCrownTime < 150) { // 增加表冠旋转防抖冷却时间，防止旋转过快导致在卡牌间飞速选择
            return;
        }
        lastCrownTime = now;

        int count = activity.tarotDrawnCount;
        if (count <= 0) return;

        int oldIdx = activity.tarotResultDetailIndex;
        if (clockwise) {
            activity.tarotResultDetailIndex = (activity.tarotResultDetailIndex + 1) % count;
        } else {
            activity.tarotResultDetailIndex = (activity.tarotResultDetailIndex - 1 + count) % count;
        }
        int newIdx = activity.tarotResultDetailIndex;

        if (oldIdx != newIdx) {
            activity.vibrateCustom(VibrationEffect.EFFECT_TICK);
            if (activity.tarotResultLayer == 1) {
                updateSpreadBorders();
            } else {
                updateDetailCard();
            }
        }
    }

    private void updateSpreadBorders() {
        int targetCount = activity.tarotDrawnCount;
        int w = getWidth();
        int h = getHeight();
        int mainTargetCount = activity.tarotTargetCount;

        for (int i = 0; i < targetCount; i++) {
            if (i >= spreadContainer.getChildCount()) break;
            View card = spreadContainer.getChildAt(i);
            
            // 1. 更新描边样式
            View border = card.findViewById(R.id.v_card_border);
            if (border != null) {
                GradientDrawable borderDrawable = (GradientDrawable) border.getBackground();
                if (borderDrawable != null) {
                    borderDrawable = (GradientDrawable) borderDrawable.mutate();
                    if (i == activity.tarotResultDetailIndex) {
                        borderDrawable.setStroke((int) (1.8f * activity.density), Color.parseColor("#D4AF37"));
                    } else {
                        borderDrawable.setStroke((int) (0.8f * activity.density), Color.parseColor("#44FFFFFF"));
                    }
                }
            }

            // 2. 更新选中状态的缩放、透明度遮罩及高程
            float[] info = getVirtualCardCenterAndSize(i, mainTargetCount, targetCount, w, h);
            float cardW = info[2];
            float cardH = info[3];
            float defaultW = 46f * activity.density;
            float defaultH = 80f * activity.density;

            float finalScaleX = cardW / defaultW;
            float finalScaleY = cardH / defaultH;

            if (i == activity.tarotResultDetailIndex) {
                finalScaleX *= 1.12f;
                finalScaleY *= 1.12f;
                card.setAlpha(1.0f);
                card.setElevation(5f * activity.density);
            } else {
                card.setAlpha(0.45f);
                card.setElevation(0f);
            }
            card.setScaleX(finalScaleX);
            card.setScaleY(finalScaleY);
        }
    }

    private void updateDetailCard() {
        if (activity.tarotDrawnCount == 0 || activity.tarotResultDetailIndex >= activity.tarotDrawnCount) return;
        bindDetailCard();
        invalidate();
    }

    private void bindDetailCard() {
        int cardIdx = activity.tarotDrawnIndices[activity.tarotResultDetailIndex];
        TarotDeck.TarotCard card = TarotDeck.TAROT_DECK[cardIdx];
        boolean isUpright = activity.tarotCardStates[activity.tarotResultDetailIndex];

        ImageView ivCardBack = detailCardView.findViewById(R.id.iv_card_back);
        View vFallback = detailCardView.findViewById(R.id.v_card_fallback);

        if (bitmapStore.get(activity.tarotResultDetailIndex) != null) {
            ivCardBack.setImageBitmap(bitmapStore.get(activity.tarotResultDetailIndex));
            ivCardBack.setVisibility(View.VISIBLE);
            vFallback.setVisibility(View.GONE);
        } else {
            ivCardBack.setVisibility(View.GONE);
            vFallback.setVisibility(View.VISIBLE);
        }

        View border = detailCardView.findViewById(R.id.v_card_border);
        if (border != null) {
            GradientDrawable borderDrawable = (GradientDrawable) border.getBackground();
            if (borderDrawable != null) {
                borderDrawable = (GradientDrawable) borderDrawable.mutate();
                borderDrawable.setStroke((int) (1.8f * activity.density), Color.parseColor("#D4AF37"));
            }
        }

        boolean isRotated90 = (activity.tarotArraySelectedIndex == 4 && activity.tarotResultDetailIndex == 1);
        boolean isRotated25 = (activity.tarotArraySelectedIndex == 12 && activity.tarotResultDetailIndex == 5);
        float rot = isUpright ? 0f : 180f;
        if (isRotated90) rot += 90f;
        if (isRotated25) rot += 25f;
        detailCardView.setRotation(rot);

        String meaningText = isUpright ? card.upright : card.reversed;
        detailTextView.setText(meaningText);
        String positionText = getCardPositionNameAndMeaning(activity.tarotResultDetailIndex);
        positionTextView.setText(positionText);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (detailAnimator != null) {
            detailAnimator.cancel();
        }
        if (dismissAnimator != null) {
            dismissAnimator.cancel();
        }
        // 主动回收 Bitmap 像素内存，杜绝内存泄漏和手表息屏后被后台杀死重启的 Bug
        bitmapStore.recycle();
        super.onDetachedFromWindow();
    }
}
