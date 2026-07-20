package com.example.watchdemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    // 界面状态枚举
    public enum ScreenState {
        INIT,
        SETTINGS,
        HISTORY,
        LIUYAO_DRAW,
        LIUYAO_RESULT,
        TAROT_ARRAY_SELECT,
        TAROT_DRAW,
        TAROT_RESULT
    }

    ScreenState currentScreen = ScreenState.INIT;
    public boolean isSwipeBackDragging = false;
    private boolean isStartingActivity = false;
    Vibrator vibrator;
    private android.os.HandlerThread vibrateThread;
    private android.os.Handler vibrateHandler;
    private final VibrateRunnable vibrateRunnable = new VibrateRunnable();
    
    // 线性马达反射缓存
    private Object mLinearMotorService = null;
    private java.lang.reflect.Method mLMVibrateMethod = null;
    private java.lang.reflect.Constructor<?> mWaveformBuilderConstructor = null;
    private java.lang.reflect.Method mSetEffectTypeMethod = null;
    private java.lang.reflect.Method mSetEffectStrengthMethod = null;
    private java.lang.reflect.Method mSetEffectLoopMethod = null;
    private java.lang.reflect.Method mBuildMethod = null;
    private boolean mIsLinearMotorSupported = false;

    WatchGestureDetector gestureDetector;
    WatchCrownHandler crownHandler;
    WatchInteractionController controller;
    
    FrameLayout mRootLayout;
    FrameLayout mSafeContainer;
    InitScreenView mInitScreenView;
    OperationGuideView mOperationGuideView;
    float density;

    // 设置状态
    boolean vibrationEnabled = true;
    int settingsSelectedIndex = 0;

    public static class LiuyaoHistoryItem {
        public long id;
        public String displayStr;
        public int[] lineResults;
        
        public LiuyaoHistoryItem(long id, String displayStr, int[] lineResults) {
            this.id = id;
            this.displayStr = displayStr;
            this.lineResults = lineResults.clone();
        }
    }

    public static class TarotHistoryItem {
        public long id;
        public String displayStr;
        public int targetCount;
        public int drawnCount;
        public int[] drawnIndices;
        public boolean[] cardStates;
        public int arraySelectedIndex;
        
        public TarotHistoryItem(long id, String displayStr, int targetCount, int drawnCount, int[] drawnIndices, boolean[] cardStates, int arraySelectedIndex) {
            this.id = id;
            this.displayStr = displayStr;
            this.targetCount = targetCount;
            this.drawnCount = drawnCount;
            this.drawnIndices = drawnIndices.clone();
            this.cardStates = cardStates.clone();
            this.arraySelectedIndex = arraySelectedIndex;
        }
    }

    // 历史记录状态
    int historyTabIndex = 0; // 0 = 六爻, 1 = 塔罗
    int historySelectedIndex = 0;
    public java.util.List<LiuyaoHistoryItem> liuyaoHistoryList = new java.util.ArrayList<>();
    public java.util.List<TarotHistoryItem> tarotHistoryList = new java.util.ArrayList<>();
    HistoryDbHelper dbHelper;
    static final java.util.concurrent.Executor dbExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

    // 六爻起卦状态
    int liuyaoRollCount = 0;
    int[] liuyaoLineResults = new int[6]; // 6=老阴, 7=少阳, 8=少阴, 9=老阳
    boolean isCoinsRolling = false;
    public boolean isShowingCoinResult = false;
    public boolean[] currentCoinResults = new boolean[3];
    public int currentCastYaoValue = 0;
    public long taijiTransitionStartTime = 0L;
    public boolean liuyaoScrollIsClockwise = true;
    int coinAnimFrame = 0;
    // 六爻滚动特效限制为 15fps；按时间补偿旋转角度，保持与原先相同的视觉转速。
    private static final long LIUYAO_ANIMATION_FRAME_DELAY_MS = 66L;
    private static final float LIUYAO_ROTATION_DEGREES_PER_SECOND = 360f;
    long liuyaoVibrateStartTime = 0L;
    long liuyaoLastScrollTime = 0L;
    public float[] liuyaoRingAngles = new float[6];
    public int[] liuyaoRingDirections = new int[6];
    private Runnable pendingCommitRunnable = null;

    // 六爻解卦状态
    int selectedHexagramLineIndex = 0;

    // 六爻结果页状态
    public int liuyaoResultSubPage = 0; // 0 = 本卦/变卦, 1 = 卦象盘, 2 = 爻辞
    public boolean liuyaoResultShowChanged = false; // 是否展示变卦
    public ScreenState previousScreen = ScreenState.INIT;

    // 塔罗牌阵选择状态
    int tarotArraySelectedIndex = 0;
    int tarotTargetCount = 3;

    // 塔罗抽牌状态
    int tarotDrawnCount = 0;
    int tarotSelectedCardIndex = 39; // 默认选中中间张 (0~77)
    int[] tarotDrawnIndices = new int[78];
    boolean[] tarotCardStates = new boolean[78];
    List<Integer> availableTarotCards = new ArrayList<>();
    public boolean isTarotFastSlideUnlocked = false;

    // 随机化种子与变量
    public long appStartTime = 0L;
    public long tarotDrawEnterTime = 0L;
    public java.util.Random tarotDrawRandom = null;

    // 塔罗结果状态
    int tarotResultLayer = 1; // 1 = 牌阵全景, 2 = 单牌详情
    int tarotResultDetailIndex = 0;
    boolean tarotResultJustEnteredLayer2 = false;

    // 动画状态
    int lastDrawnCardId = -1;
    long lastDrawnTime = 0;

    static final String[] TAROT_CARDS = {
        "愚人", "魔术师", "女祭司", "女皇", "皇帝", "教皇", "恋人", "战车",
        "力量", "隐士", "命运之轮", "正义", "倒吊人", "死神", "节制", "恶魔",
        "高塔", "星星", "月亮", "太阳", "审判", "世界"
    };

    static final String[] TAROT_MEANINGS = {
        "【释义】正位代表新的起点、自由、冒险与无限可能性。逆位提示盲目粗心、逃避现实、受挫或决策错误。",
        "【释义】正位代表创造力、专注力、主动性与坚强的意志力。逆位提示缺乏规划、意志消沉、欺骗或才能受阻。",
        "【释义】正位代表直觉、深层潜意识、智慧与静止思考。逆位提示情绪波动、忽视直觉、过于冷漠或肤浅。",
        "【释义】正位代表丰收、自然、母亲、丰饶与物质享受。逆位提示过度控制、缺乏成长、创造力受阻或感情危机。",
        "【释义】正位代表权力、控制、父亲、纪律与秩序。逆位提示专制暴政、控制欲过强、软弱或缺乏行动力。",
        "【释义】正位代表传统、精神指引、体制、信仰与社会规范。逆位提示打破陈规、盲目顺从、叛逆或信仰动摇。",
        "【释义】正位代表选择、结合、和谐的感情关系。逆位提示关系不协调、分离、面临艰难抉择或沟通不畅。",
        "【释义】正位代表坚强意志、胜利、掌控与勇往直前。逆位提示失控、方向错误、受挫退缩或过度好斗。",
        "【释义】正位代表勇气、内在力量、耐心与坚韧自控.逆位提示自我怀疑、软弱、丧失自信或滥用暴力。",
        "【释义】正位代表自我反省、孤独、精神引导与追寻真理.逆位提示过度孤立、顽固不化、逃避社交或感到迷茫。",
        "【释义】正位代表命运转变、重大机遇、好运与新循环开始。逆位提示坏运气、抗拒变化、时运不济或自动卷入恶性循环。",
        "【释义】正位代表公正、真理、道德责任与生活平衡。逆位提示失衡、不公、偏见或面临法律纠纷、推卸责任。",
        "【释义】正位代表奉献牺牲、换位思考、暂停行动与静候时机。逆位提示无谓挣扎、抗拒牺牲、停滞不前或错失时机。",
        "【释义】正位代表旧事物终结、深刻转变、告别过去与新生。逆位提示抗拒改变、停滞不前、恐惧死亡或沉溺过去。",
        "【释义】正位代表适度平衡、和谐节制、顺畅沟通与身心净化。逆位提示失衡、不和谐、缺乏沟通或过度消耗。",
        "【释义】正位代表强烈欲望、物质束缚、诱惑与沉溺。逆位提示摆脱束缚、精神觉醒、直面阴暗面或不再堕落。",
        "【释义】正位代表剧烈变革、灾难性崩塌、打破幻想与灵魂觉醒。逆位提示危机渐去、害怕改变、面临小警告或侥幸逃脱。",
        "【释义】正位代表希望、灵感、宁静、新生与未来期许。逆位提示缺乏灵感、感到失望、悲观主义或自我怀疑。",
        "【释义】正位代表幻觉、不安恐惧、潜意识梦境与未知迷茫。逆位提示解开束缚、驱散迷雾、真相大白。",
        "【释义】正位代表活力四射、大获成功、无比快乐与光明前程。逆位提示虚幻成功、缺乏活力、骄傲自满或遭遇小阻碍。",
        "【释义】正位代表专一觉醒、审判、决定、救赎与因果召示。逆位提示拖延决断、拒绝觉醒、悔恨过去或逃避责任。",
        "【释义】正位代表圆满成功、旅程终点、全球视野与完美契合。逆位提示未完成的目标、受阻停滞或抗拒结束旧篇章。"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appStartTime = System.currentTimeMillis(); // 记录打开程序的时间

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        initLinearMotorReflection();
        vibrateThread = new android.os.HandlerThread("crown_vibrate");
        vibrateThread.start();
        vibrateHandler = new android.os.Handler(vibrateThread.getLooper());
        
        density = getResources().getDisplayMetrics().density;

        // Initialize state (load from Database)
        dbHelper = HistoryDbHelper.getInstance(this);
        loadHistory();

        // 初始化自定义手势与表冠控制器
        controller = new WatchInteractionController(this);
        gestureDetector = new WatchGestureDetector(this, controller);
        crownHandler = new WatchCrownHandler(controller);

        // 计算圆形屏幕 76% 安全区尺寸
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int circleSize = (int) (Math.min(screenWidth, screenHeight) * 0.76f);

        // 根布局
        mRootLayout = new FrameLayout(this);
        mRootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        mRootLayout.setBackgroundColor(this instanceof SubActivity ? Color.TRANSPARENT : Color.BLACK);

        // 安全区容器 (铺满全屏)
        mSafeContainer = new FrameLayout(this);
        mSafeContainer.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        
        mRootLayout.addView(mSafeContainer);
        setContentView(mRootLayout);

        mRootLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(v, event);
            }
        });

        renderScreen();
        showOperationGuideIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isStartingActivity = false;
        loadHistory();
        if (currentScreen == ScreenState.INIT && mInitScreenView != null) {
            mInitScreenView.startAnimation();
        }
        renderScreen();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mInitScreenView != null) {
            mInitScreenView.stopAnimation();
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (isSwipeBackDragging) {
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_SCROLL) {
            float scrollDelta = event.getAxisValue(MotionEvent.ACTION_SCROLL);
            if (scrollDelta == 0) {
                scrollDelta = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            }
            if (scrollDelta != 0) {
                crownHandler.onScroll(scrollDelta);
                return true;
            }
        }
        return super.onGenericMotionEvent(event);
    }

    void vibrateCustom(final int effectId) {
        if (!vibrationEnabled) return;
        if (vibrateHandler != null) {
            // 防抖去重：先移除队列中未执行的相同 Runnable，防止震动请求因高频操作而挤压阻塞
            vibrateHandler.removeCallbacks(vibrateRunnable);
            vibrateRunnable.setEffectId(effectId);
            vibrateHandler.post(vibrateRunnable);
        }
    }

    void switchScreen(ScreenState newState) {
        if (!(this instanceof SubActivity) && newState != ScreenState.INIT) {
            if (isStartingActivity) {
                return;
            }
            isStartingActivity = true;
            android.content.Intent intent = new android.content.Intent(this, SubActivity.class);
            intent.putExtra("TARGET_SCREEN", newState);
            startActivity(intent);
            overridePendingTransition(0, 0);
            return;
        }
        vibrateCustom(VibrationEffect.EFFECT_CLICK);
        previousScreen = currentScreen;
        currentScreen = newState;
        if (newState == ScreenState.LIUYAO_RESULT) {
            liuyaoResultSubPage = 0;
            liuyaoResultShowChanged = false;
        }
        if (newState == ScreenState.LIUYAO_DRAW) {
            liuyaoRollCount = 0;
            isCoinsRolling = false;
            isShowingCoinResult = false;
            java.util.Arrays.fill(currentCoinResults, false);
            currentCastYaoValue = 0;
            taijiTransitionStartTime = 0L;
            liuyaoScrollIsClockwise = true;
            liuyaoRingAngles = new float[6];
            liuyaoRingDirections = new int[6];
            java.util.Arrays.fill(liuyaoRingDirections, 1);
            if (pendingCommitRunnable != null && mSafeContainer != null) {
                mSafeContainer.removeCallbacks(pendingCommitRunnable);
                pendingCommitRunnable = null;
            }
            java.util.Arrays.fill(liuyaoLineResults, 0);
        }
        renderScreen();
    }

    void renderScreen() {
        if (crownHandler != null) {
            if (currentScreen == ScreenState.LIUYAO_DRAW) {
                crownHandler.setCooldownMs(20);
                crownHandler.setThreshold(0.5f);
                crownHandler.setUseSpeedAdaptiveDeadzone(false);
            } else if (currentScreen == ScreenState.LIUYAO_RESULT || currentScreen == ScreenState.TAROT_RESULT) {
                crownHandler.setCooldownMs(500); // 强防抖冷却时间
                crownHandler.setThreshold(2.0f);  // 强防抖触发阈值
                crownHandler.setUseSpeedAdaptiveDeadzone(false);
            } else if (currentScreen == ScreenState.TAROT_DRAW) {
                crownHandler.setCooldownMs(40); // 减少防抖以提升旋转抽牌流畅度
                crownHandler.setThreshold(0.4f);
                crownHandler.setUseSpeedAdaptiveDeadzone(false);
            } else {
                crownHandler.setCooldownMs(150);
                crownHandler.setThreshold(0.5f);
                crownHandler.setUseSpeedAdaptiveDeadzone(false);
            }
        }

        boolean keepInitUnderneath = !(this instanceof SubActivity) && (currentScreen == ScreenState.SETTINGS 
                || currentScreen == ScreenState.HISTORY 
                || currentScreen == ScreenState.LIUYAO_DRAW 
                || currentScreen == ScreenState.TAROT_ARRAY_SELECT);

        if (!keepInitUnderneath && mInitScreenView != null) {
            mRootLayout.removeView(mInitScreenView);
            mInitScreenView.stopAnimation();
            mInitScreenView = null;
        }
        mSafeContainer.removeAllViews();

        if (currentScreen == ScreenState.INIT) {
            mSafeContainer.setVisibility(View.GONE);
            if (mInitScreenView == null) {
                mInitScreenView = new InitScreenView(this);
                mInitScreenView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ));
                mRootLayout.addView(mInitScreenView, 0); // 插入最底层
                mInitScreenView.startAnimation();
            } else {
                mInitScreenView.setVisibility(View.VISIBLE);
                mInitScreenView.setScaleX(1.0f);
                mInitScreenView.setScaleY(1.0f);
                mInitScreenView.setAlpha(1.0f);
            }
        } else {
            mSafeContainer.setVisibility(View.VISIBLE);

            if (keepInitUnderneath && mInitScreenView == null) {
                mInitScreenView = new InitScreenView(this);
                mInitScreenView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ));
                mRootLayout.addView(mInitScreenView, 0); // 插入最底层
                mInitScreenView.startAnimation();
            }

            if (keepInitUnderneath && mInitScreenView != null) {
                mInitScreenView.setScaleX(1.1f);
                mInitScreenView.setScaleY(1.1f);
                mInitScreenView.setAlpha(0.0f);
                mInitScreenView.setVisibility(View.VISIBLE);
            }

            View screenView = null;
            switch (currentScreen) {
                case SETTINGS:
                    screenView = new SettingsScreenView(this);
                    break;
                case HISTORY:
                    screenView = new HistoryScreenView(this);
                    break;
                case LIUYAO_DRAW:
                    screenView = new LiuyaoDrawScreenView(this);
                    break;
                case LIUYAO_RESULT:
                    screenView = new LiuyaoResultScreenView(this);
                    break;
                case TAROT_ARRAY_SELECT:
                    screenView = new TarotArraySelectScreenView(this);
                    break;
                case TAROT_DRAW:
                    screenView = new TarotDrawScreenView(this);
                    break;
                case TAROT_RESULT:
                    screenView = new TarotResultScreenView(this);
                    break;
            }
            if (screenView != null) {
                screenView.setBackgroundColor(Color.BLACK);
                boolean shouldWrap = (this instanceof SubActivity) && (currentScreen == ScreenState.SETTINGS 
                        || currentScreen == ScreenState.HISTORY 
                        || currentScreen == ScreenState.LIUYAO_DRAW 
                        || currentScreen == ScreenState.TAROT_ARRAY_SELECT
                        || currentScreen == ScreenState.TAROT_DRAW
                        || currentScreen == ScreenState.TAROT_RESULT
                        || currentScreen == ScreenState.LIUYAO_RESULT);
                if (shouldWrap) {
                    SwipeBackLayout swipeBackLayout = new SwipeBackLayout(this);
                    swipeBackLayout.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    screenView.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    swipeBackLayout.addView(screenView);
                    swipeBackLayout.setOnSwipeBackListener(new SwipeBackLayout.OnSwipeBackListener() {
                        @Override
                        public void onSwipeBack() {
                            if (currentScreen == ScreenState.LIUYAO_RESULT) {
                                switchScreen(previousScreen);
                            } else {
                                switchScreen(ScreenState.INIT);
                            }
                        }
                    });
                    mSafeContainer.addView(swipeBackLayout);
                } else {
                    screenView.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    mSafeContainer.addView(screenView);
                }
            }
        }
        updateSystemGestureExclusion();
    }

    private void updateSystemGestureExclusion() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (mRootLayout != null) {
                if (currentScreen == ScreenState.INIT) {
                    mRootLayout.setSystemGestureExclusionRects(new java.util.ArrayList<android.graphics.Rect>());
                } else {
                    int screenHeight = getResources().getDisplayMetrics().heightPixels;
                    int exclusionWidth = (int) (60 * density);
                    java.util.List<android.graphics.Rect> rects = new java.util.ArrayList<>();
                    rects.add(new android.graphics.Rect(0, 0, exclusionWidth, screenHeight));
                    mRootLayout.setSystemGestureExclusionRects(rects);
                }
            }
        }
    }

    private void showOperationGuideIfNeeded() {
        if (this instanceof SubActivity || currentScreen != ScreenState.INIT) {
            return;
        }
        android.content.SharedPreferences prefs = getSharedPreferences("watch_demo_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("operation_guide_shown", false)) {
            prefs.edit().putBoolean("operation_guide_shown", true).apply();
            showOperationGuide(false);
        }
    }

    public void showOperationGuide(boolean isFromSettings) {
        if (mRootLayout == null) {
            return;
        }
        dismissOperationGuide();
        mOperationGuideView = new OperationGuideView(this, isFromSettings);
        mOperationGuideView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        mRootLayout.addView(mOperationGuideView);
    }

    public void dismissOperationGuide() {
        if (mRootLayout != null && mOperationGuideView != null) {
            android.view.ViewParent parent = mOperationGuideView.getParent();
            if (parent instanceof SwipeBackLayout) {
                SwipeBackLayout sbl = (SwipeBackLayout) parent;
                View under = sbl.getUnderneathView();
                if (under != null) {
                    under.setScaleX(1.0f);
                    under.setScaleY(1.0f);
                    under.setAlpha(1.0f);
                }
            }
            if (parent == mRootLayout) {
                mRootLayout.removeView(mOperationGuideView);
            } else if (parent instanceof android.view.ViewGroup) {
                mRootLayout.removeView((android.view.View) parent);
            }
            mOperationGuideView = null;
        }
    }

    public View getActiveScreenView() {
        if (mSafeContainer == null || mSafeContainer.getChildCount() == 0) return null;
        View child = mSafeContainer.getChildAt(0);
        if (child instanceof SwipeBackLayout) {
            SwipeBackLayout sbl = (SwipeBackLayout) child;
            if (sbl.getChildCount() > 0) {
                return sbl.getChildAt(0);
            }
        }
        return child;
    }

    public void executeDrawCardAction(boolean fillFromLeft) {
        int maxCount = tarotTargetCount > 0 ? tarotTargetCount : 78;
        if (tarotDrawnCount < maxCount && availableTarotCards.size() > 0) {
            int drawnCardId = availableTarotCards.remove(tarotSelectedCardIndex);
            tarotDrawnIndices[tarotDrawnCount] = drawnCardId;
            if (tarotDrawRandom != null) {
                tarotCardStates[tarotDrawnCount] = tarotDrawRandom.nextBoolean(); // 用进入抽牌界面的时间戳种子来随机判定逆位
            } else {
                tarotCardStates[tarotDrawnCount] = Math.random() > 0.5;
            }
            tarotDrawnCount++;
            vibrateCustom(VibrationEffect.EFFECT_CLICK);
            
            lastDrawnCardId = drawnCardId;
            lastDrawnTime = System.currentTimeMillis();
            
            if (availableTarotCards.size() > 0) {
                if (fillFromLeft) {
                    // 左侧向中线补位，相当于选中索引在剩余的牌库中向左移一位
                    tarotSelectedCardIndex = (tarotSelectedCardIndex - 1 + availableTarotCards.size()) % availableTarotCards.size();
                } else {
                    // 右侧向中线补位，后续的牌左移，由于移除了当前牌，新的选中索引依然是原位置（若越界则取最大值）
                    tarotSelectedCardIndex = Math.min(tarotSelectedCardIndex, availableTarotCards.size() - 1);
                }
            }
            
            renderScreen();
            
            if (tarotTargetCount > 0 && tarotDrawnCount == tarotTargetCount) {
                mSafeContainer.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (currentScreen == ScreenState.TAROT_DRAW) {
                            tarotResultLayer = 1;
                            tarotResultDetailIndex = 0;
                            addTarotHistory();
                            switchScreen(MainActivity.ScreenState.TAROT_RESULT);
                        }
                    }
                }, 300);
            }
        }
    }

    public void handleLiuyaoScrollStep(boolean isClockwise) {
        if (liuyaoRollCount >= 6 || isShowingCoinResult) return;
        
        liuyaoScrollIsClockwise = isClockwise;
        long now = System.currentTimeMillis();
        liuyaoLastScrollTime = now;
        
        vibrateCustom(android.os.VibrationEffect.EFFECT_TICK);
        
        if (pendingCommitRunnable != null) {
            mSafeContainer.removeCallbacks(pendingCommitRunnable);
            pendingCommitRunnable = null;
        }
        
        if (liuyaoRollCount < 6) {
            liuyaoRingDirections[liuyaoRollCount] = isClockwise ? 1 : -1;
        }
        
        if (!isCoinsRolling) {
            isCoinsRolling = true;
            liuyaoVibrateStartTime = now;
            if (liuyaoRollCount == 0) {
                taijiTransitionStartTime = now;
            }
            
            final Runnable animRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isCoinsRolling) {
                        coinAnimFrame++;
                        float step = LIUYAO_ROTATION_DEGREES_PER_SECOND
                                * LIUYAO_ANIMATION_FRAME_DELAY_MS / 1000f;
                        if (liuyaoScrollIsClockwise) {
                            step = -step;
                        }
                        for (int k = 0; k < 6; k++) {
                            if (k < liuyaoRollCount) {
                                liuyaoRingAngles[k] += liuyaoRingDirections[k] * step;
                            } else {
                                liuyaoRingAngles[k] += step;
                            }
                        }
                        View child = getActiveScreenView();
                        if (child instanceof LiuyaoDrawScreenView) {
                            ((LiuyaoDrawScreenView) child).updateText();
                        }
                        mSafeContainer.postDelayed(this, LIUYAO_ANIMATION_FRAME_DELAY_MS);
                    }
                }
            };
            mSafeContainer.post(animRunnable);
            
            final Runnable stopCheckRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isCoinsRolling) {
                        final long current = System.currentTimeMillis();
                        if (current - liuyaoLastScrollTime >= 350) {
                            isCoinsRolling = false;
                            
                            // 停止滚动，将所有圈层角度对齐到 120 度的倍数，并规范到 [0, 360) 范围内，确保静止时完美对齐
                            for (int k = 0; k < 6; k++) {
                                float rounded = Math.round(liuyaoRingAngles[k] / 120f) * 120f;
                                rounded = ((rounded % 360f) + 360f) % 360f;
                                liuyaoRingAngles[k] = rounded;
                            }
                            
                            // 立即生成本次抛币的 3 个硬币随机结果
                            int seed = (int)(liuyaoVibrateStartTime ^ current);
                            SFC32 rng = new SFC32(seed);
                            currentCoinResults[0] = (rng.next() < 0.5f);
                            currentCoinResults[1] = (rng.next() < 0.5f);
                            currentCoinResults[2] = (rng.next() < 0.5f);
                            
                            int sum = 0;
                            for (int i = 0; i < 3; i++) {
                                sum += currentCoinResults[i] ? 3 : 2;
                            }
                            currentCastYaoValue = sum;
                            isShowingCoinResult = true;

                            // 立即刷新画面使旋转停止，并冻结展示本轮 of 3 coins (0ms)
                            View child = getActiveScreenView();
                            if (child instanceof LiuyaoDrawScreenView) {
                                ((LiuyaoDrawScreenView) child).updateText();
                            }
                            
                            // 延时 1.5 秒 (1-2s内) 维持显示当前投币结果，随后合并变换为最终的爻线并 commit
                            pendingCommitRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    commitLiuyaoYao();
                                    pendingCommitRunnable = null;
                                }
                            };
                            mSafeContainer.postDelayed(pendingCommitRunnable, 1500);
                        } else {
                            mSafeContainer.postDelayed(this, LIUYAO_ANIMATION_FRAME_DELAY_MS);
                        }
                    }
                }
            };
            mSafeContainer.postDelayed(stopCheckRunnable, 100);
        }
    }

    private void commitLiuyaoYao() {
        isShowingCoinResult = false;
        liuyaoLineResults[liuyaoRollCount] = currentCastYaoValue;
        liuyaoRollCount++;
        
        vibrateCustom(android.os.VibrationEffect.EFFECT_CLICK);
        
        View child = getActiveScreenView();
        if (child instanceof LiuyaoDrawScreenView) {
            ((LiuyaoDrawScreenView) child).updateText();
        }
        
        if (liuyaoRollCount == 6) {
            mSafeContainer.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (currentScreen == ScreenState.LIUYAO_DRAW) {
                        selectedHexagramLineIndex = 0;
                        addLiuyaoHistory();
                        currentScreen = ScreenState.LIUYAO_RESULT;
                        renderScreen();
                    }
                }
            }, 1200);
        }
    }

    public static class SFC32 {
        private int a, b, c, d;

        public SFC32(int seed) {
            a = 0x9E3779B9;
            b = 0x243F6A88;
            c = 0xB7E15162;
            d = seed == 0 ? 12345 : Math.abs(seed);
            for (int i = 0; i < 12; i++) {
                next();
            }
        }

        public float next() {
            int t = a + b;
            a = b ^ (b >>> 9);
            b = c + (c << 3);
            c = (c << 21) | (c >>> 11);
            d = d + 1;
            t = t + d;
            c = c + t;
            return (float) ((t & 0xFFFFFFFFL) / 4294967296.0);
        }
    }

    public static int generateSingleYao(int seed) {
        SFC32 rng = new SFC32(seed);
        int sum = 0;
        for (int i = 0; i < 3; i++) {
            sum += (rng.next() < 0.5f) ? 3 : 2;
        }
        return sum; // 6, 7, 8, 9
    }

    String getLiuyaoResultText(int lineIndex) {
        String title = "【乾为天】 变卦 【天风姤】\n";
        String info = "";
        switch (lineIndex) {
            case 0: info = "初九：潜龙勿用。断曰：时机未至，宜积蓄力量，切忌盲动。"; break;
            case 1: info = "九二：见龙在田，利见大人。断曰：贵人相助，崭露头角，利于合作。"; break;
            case 2: info = "九三：君子终日乾乾，夕惕若，厉无咎。断曰：朝乾夕惕，虽然处境艰难但无大碍。"; break;
            case 3: info = "九四：或跃在渊，无咎。断曰：进退自如，顺应时势，把握良机即可无咎。"; break;
            case 4: info = "九五：飞龙在天，利见大人。断曰：大吉大利，事业达到巅峰，展现宏图。"; break;
            case 5: info = "上九：亢龙有悔。断曰：盈不可久，物极必反，宜居安思危，戒骄戒躁。"; break;
        }
        return title + info;
    }

    private void initLinearMotorReflection() {
        try {
            mLinearMotorService = getSystemService("linearmotor");
            if (mLinearMotorService != null) {
                Class<?> vibratorClass = Class.forName("android.os.linearmotorvibrator.LinearmotorVibrator");
                Class<?> waveformEffectClass = Class.forName("android.os.linearmotorvibrator.WaveformEffect");
                Class<?> builderClass = Class.forName("android.os.linearmotorvibrator.WaveformEffect$Builder");
                
                mLMVibrateMethod = vibratorClass.getMethod("vibrate", waveformEffectClass);
                mWaveformBuilderConstructor = builderClass.getConstructor();
                mSetEffectTypeMethod = builderClass.getMethod("setEffectType", int.class);
                mSetEffectStrengthMethod = builderClass.getMethod("setEffectStrength", int.class);
                mSetEffectLoopMethod = builderClass.getMethod("setEffectLoop", boolean.class);
                mBuildMethod = builderClass.getMethod("build");
                mIsLinearMotorSupported = true;
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Failed to initialize LinearmotorVibrator reflection", e);
            mIsLinearMotorSupported = false;
        }
    }

    public void saveHistory() {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("watch_demo_prefs", MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("vibration_enabled", vibrationEnabled);
            editor.apply();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Failed to save settings", e);
        }
    }

    public void loadHistory() {
        if (dbHelper == null) {
            dbHelper = HistoryDbHelper.getInstance(this);
        }
        dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final java.util.List<LiuyaoHistoryItem> tempLiuyao = new java.util.ArrayList<>();
                final java.util.List<TarotHistoryItem> tempTarot = new java.util.ArrayList<>();
                
                android.database.sqlite.SQLiteDatabase db = null;
                android.database.Cursor cursor = null;
                try {
                    db = dbHelper.getReadableDatabase();
                    
                    // 1. Load Liuyao history
                    cursor = db.query("liuyao_history", null, null, null, null, null, "create_time DESC");
                    if (cursor != null && cursor.moveToFirst()) {
                        int idCol = cursor.getColumnIndex("id");
                        int displayCol = cursor.getColumnIndex("display_str");
                        int resultsCol = cursor.getColumnIndex("line_results");
                        do {
                            long id = cursor.getLong(idCol);
                            String displayStr = cursor.getString(displayCol);
                            String resultsStr = cursor.getString(resultsCol);
                            
                            String[] parts = resultsStr.split(",");
                            int[] lineResults = new int[parts.length];
                            for (int i = 0; i < parts.length; i++) {
                                lineResults[i] = Integer.parseInt(parts[i].trim());
                            }
                            tempLiuyao.add(new LiuyaoHistoryItem(id, displayStr, lineResults));
                        } while (cursor.moveToNext());
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                    
                    // 2. Load Tarot history
                    cursor = db.query("tarot_history", null, null, null, null, null, "create_time DESC");
                    if (cursor != null && cursor.moveToFirst()) {
                        int idCol = cursor.getColumnIndex("id");
                        int displayCol = cursor.getColumnIndex("display_str");
                        int targetCol = cursor.getColumnIndex("target_count");
                        int drawnCol = cursor.getColumnIndex("drawn_count");
                        int indicesCol = cursor.getColumnIndex("drawn_indices");
                        int statesCol = cursor.getColumnIndex("card_states");
                        int arraySelCol = cursor.getColumnIndex("array_selected_index");
                        do {
                            long id = cursor.getLong(idCol);
                            String displayStr = cursor.getString(displayCol);
                            int targetCount = cursor.getInt(targetCol);
                            int drawnCount = cursor.getInt(drawnCol);
                            String indicesStr = cursor.getString(indicesCol);
                            String statesStr = cursor.getString(statesCol);
                            int arraySelectedIndex = cursor.getInt(arraySelCol);
                            
                            int[] drawnIndices;
                            if (indicesStr.trim().isEmpty()) {
                                drawnIndices = new int[0];
                            } else {
                                String[] parts = indicesStr.split(",");
                                drawnIndices = new int[parts.length];
                                for (int i = 0; i < parts.length; i++) {
                                    drawnIndices[i] = Integer.parseInt(parts[i].trim());
                                }
                            }
                            
                            boolean[] cardStates;
                            if (statesStr.trim().isEmpty()) {
                                cardStates = new boolean[0];
                            } else {
                                String[] parts = statesStr.split(",");
                                cardStates = new boolean[parts.length];
                                for (int i = 0; i < parts.length; i++) {
                                    cardStates[i] = "1".equals(parts[i].trim()) || "true".equalsIgnoreCase(parts[i].trim());
                                }
                            }
                            
                            tempTarot.add(new TarotHistoryItem(id, displayStr, targetCount, drawnCount, drawnIndices, cardStates, arraySelectedIndex));
                        } while (cursor.moveToNext());
                    }
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Failed to load history from database", e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        liuyaoHistoryList.clear();
                        liuyaoHistoryList.addAll(tempLiuyao);
                        tarotHistoryList.clear();
                        tarotHistoryList.addAll(tempTarot);
                        renderScreen();
                    }
                });
            }
        });
    }

    public void addLiuyaoHistory() {
        LiuyaoCalculator.Result res = LiuyaoCalculator.calculate(liuyaoLineResults);
        String guaName = res.benGuaIdx >= 0 ? LiuyaoCalculator.GUA_DATA[res.benGuaIdx].name : "未知卦";
        if (res.sumBian > 0) {
            String zhiName = res.zhiGuaIdx >= 0 ? LiuyaoCalculator.GUA_DATA[res.zhiGuaIdx].name : "未知卦";
            guaName = guaName + "之" + zhiName;
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        final String dateStr = sdf.format(new java.util.Date());
        final String displayStr = dateStr + " " + guaName;
        final int[] lineResultsClone = liuyaoLineResults.clone();
        
        dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                android.database.sqlite.SQLiteDatabase db = null;
                long insertedId = -1;
                try {
                    db = dbHelper.getWritableDatabase();
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put("display_str", displayStr);
                    
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < lineResultsClone.length; i++) {
                        sb.append(lineResultsClone[i]);
                        if (i < lineResultsClone.length - 1) {
                            sb.append(",");
                        }
                    }
                    values.put("line_results", sb.toString());
                    values.put("create_time", System.currentTimeMillis());
                    
                    insertedId = db.insert("liuyao_history", null, values);
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Failed to insert liuyao history", e);
                }
                
                final long finalId = insertedId;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        liuyaoHistoryList.add(0, new LiuyaoHistoryItem(finalId, displayStr, lineResultsClone));
                        saveHistory();
                    }
                });
            }
        });
    }

    public void addTarotHistory() {
        String arrayName = "自由抽牌";
        if (tarotArraySelectedIndex == 1) arrayName = "圣三角";
        else if (tarotArraySelectedIndex == 2) arrayName = "六芒星";
        else if (tarotArraySelectedIndex == 3) arrayName = "时间之箭";
        else if (tarotArraySelectedIndex == 4) arrayName = "凯尔特十字";
        else if (tarotArraySelectedIndex == 5) arrayName = "四元素";
        else if (tarotArraySelectedIndex == 6) arrayName = "二选一";
        else if (tarotArraySelectedIndex == 7) arrayName = "金字塔";
        else if (tarotArraySelectedIndex == 8) arrayName = "七脉轮";
        else if (tarotArraySelectedIndex == 9) arrayName = "直击问题";
        else if (tarotArraySelectedIndex == 10) arrayName = "指引之星";
        else if (tarotArraySelectedIndex == 11) arrayName = "财务";
        else if (tarotArraySelectedIndex == 12) arrayName = "人际关系";

        String primaryCard = "";
        if (tarotDrawnCount > 0) {
            int firstCardId = tarotDrawnIndices[0];
            String cardName = firstCardId < TAROT_CARDS.length ? TAROT_CARDS[firstCardId] : "塔罗牌";
            primaryCard = " (" + cardName + ")";
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String dateStr = sdf.format(new java.util.Date());
        final String displayStr = dateStr + " " + arrayName + primaryCard;
        final int targetCount = tarotTargetCount;
        final int drawnCount = tarotDrawnCount;
        final int[] drawnIndicesClone = tarotDrawnIndices.clone();
        final boolean[] cardStatesClone = tarotCardStates.clone();
        final int arraySelIdx = tarotArraySelectedIndex;

        dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                android.database.sqlite.SQLiteDatabase db = null;
                long insertedId = -1;
                try {
                    db = dbHelper.getWritableDatabase();
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put("display_str", displayStr);
                    values.put("target_count", targetCount);
                    values.put("drawn_count", drawnCount);
                    
                    StringBuilder idxSb = new StringBuilder();
                    for (int i = 0; i < drawnIndicesClone.length; i++) {
                        idxSb.append(drawnIndicesClone[i]);
                        if (i < drawnIndicesClone.length - 1) {
                            idxSb.append(",");
                        }
                    }
                    values.put("drawn_indices", idxSb.toString());
                    
                    StringBuilder stateSb = new StringBuilder();
                    for (int i = 0; i < cardStatesClone.length; i++) {
                        stateSb.append(cardStatesClone[i] ? "1" : "0");
                        if (i < cardStatesClone.length - 1) {
                            stateSb.append(",");
                        }
                    }
                    values.put("card_states", stateSb.toString());
                    values.put("array_selected_index", arraySelIdx);
                    values.put("create_time", System.currentTimeMillis());
                    
                    insertedId = db.insert("tarot_history", null, values);
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Failed to insert tarot history", e);
                }
                
                final long finalId = insertedId;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tarotHistoryList.add(0, new TarotHistoryItem(finalId, displayStr, targetCount, drawnCount, drawnIndicesClone, cardStatesClone, arraySelIdx));
                        saveHistory();
                    }
                });
            }
        });
    }

    public void deleteLiuyaoHistory(final long id) {
        dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.delete("liuyao_history", "id = ?", new String[]{String.valueOf(id)});
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Failed to delete liuyao history", e);
                }
            }
        });
    }

    public void deleteTarotHistory(final long id) {
        dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.delete("tarot_history", "id = ?", new String[]{String.valueOf(id)});
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Failed to delete tarot history", e);
                }
            }
        });
    }

    public void clearAllHistory() {
        dbExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.delete("liuyao_history", null, null);
                    db.delete("tarot_history", null, null);
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Failed to clear all history", e);
                }
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        liuyaoHistoryList.clear();
                        tarotHistoryList.clear();
                        renderScreen();
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (vibrateThread != null) {
            vibrateThread.quitSafely();
        }
        super.onDestroy();
    }

    private class VibrateRunnable implements Runnable {
        private int effectId;

        public void setEffectId(int effectId) {
            this.effectId = effectId;
        }

        @Override
        public void run() {
            if (mIsLinearMotorSupported && mLinearMotorService != null) {
                try {
                    int targetEffectId = effectId;
                    if (effectId == android.os.VibrationEffect.EFFECT_TICK) {
                        targetEffectId = 302; // OPPO Watch X2 crisp crown detent tick effect ID
                    }
                    Object builderObj = mWaveformBuilderConstructor.newInstance();
                    mSetEffectTypeMethod.invoke(builderObj, targetEffectId);
                    mSetEffectStrengthMethod.invoke(builderObj, 2); // strength = 2
                    mSetEffectLoopMethod.invoke(builderObj, false); // loop = false
                    Object effectObj = mBuildMethod.invoke(builderObj);
                    
                    mLMVibrateMethod.invoke(mLinearMotorService, effectObj);
                    return; // Success
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Linearmotor vibration failed, falling back to standard vibrator", e);
                }
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(effectId));
                } else {
                    if (effectId == VibrationEffect.EFFECT_TICK) {
                        vibrator.vibrate(10);
                    } else {
                        vibrator.vibrate(50);
                    }
                }
            }
        }
    }
}
