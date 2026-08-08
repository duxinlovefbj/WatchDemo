package com.example.watchdemo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.VibrationEffect;
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
    private WatchHaptics haptics;
    private LiuyaoCastingController liuyaoCastingController;
    private TarotDrawController tarotDrawController;

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

    // 历史记录状态
    int historyTabIndex = 0; // 0 = 六爻, 1 = 塔罗
    int historySelectedIndex = 0;
    public java.util.List<LiuyaoHistoryItem> liuyaoHistoryList = new java.util.ArrayList<>();
    public java.util.List<TarotHistoryItem> tarotHistoryList = new java.util.ArrayList<>();
    private HistoryManager historyManager;

    // 六爻起卦状态
    int liuyaoRollCount = 0;
    int[] liuyaoLineResults = new int[6]; // 6=老阴, 7=少阳, 8=少阴, 9=老阳
    boolean isCoinsRolling = false;
    public boolean isShowingCoinResult = false;
    public boolean[] currentCoinResults = new boolean[3];
    public int currentCastYaoValue = 0;
    public long taijiTransitionStartTime = 0L;
    public boolean liuyaoScrollIsClockwise = true;
    public float[] liuyaoRingAngles = new float[6];
    public int[] liuyaoRingDirections = new int[6];

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
    public long liuyaoResultTimeMillis = 0L;
    // 塔罗结果状态
    int tarotResultLayer = 1; // 1 = 牌阵全景, 2 = 单牌详情
    int tarotResultDetailIndex = 0;
    boolean tarotResultJustEnteredLayer2 = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appStartTime = System.currentTimeMillis(); // 记录打开程序的时间

        haptics = new WatchHaptics(this);
        liuyaoCastingController = new LiuyaoCastingController(this);
        tarotDrawController = new TarotDrawController(this);
        
        density = getResources().getDisplayMetrics().density;

        // Initialize state (load from Database)
        historyManager = new HistoryManager(this);
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
        if (vibrationEnabled && haptics != null) haptics.vibrate(effectId);
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
            liuyaoCastingController.reset();
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
        tarotDrawController.drawSelectedCard(fillFromLeft);
    }

    void startTarotDrawSession() {
        tarotDrawController.startSession();
    }

    public void handleLiuyaoScrollStep(boolean isClockwise) {
        liuyaoCastingController.onScrollStep(isClockwise);
    }

    public static int generateSingleYao(int seed) {
        return LiuyaoCastingController.generateSingleYao(seed);
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
        historyManager.load((liuyao, tarot) -> runOnUiThread(() -> {
            liuyaoHistoryList.clear();
            liuyaoHistoryList.addAll(liuyao);
            tarotHistoryList.clear();
            tarotHistoryList.addAll(tarot);
            renderScreen();
        }));
    }

    public void addLiuyaoHistory() {
        historyManager.addLiuyao(liuyaoLineResults, liuyaoResultTimeMillis,
                item -> runOnUiThread(() -> {
            liuyaoHistoryList.add(0, item);
            saveHistory();
        }));
    }

    public void addTarotHistory() {
        int firstCardId = tarotDrawnCount > 0 ? tarotDrawnIndices[0] : -1;
        String cardName = firstCardId >= 0 && firstCardId < 22
                ? TarotDeck.TAROT_DECK[firstCardId].name : "塔罗牌";
        historyManager.addTarot(tarotArraySelectedIndex, tarotTargetCount, tarotDrawnCount,
                tarotDrawnIndices, tarotCardStates, cardName,
                item -> runOnUiThread(() -> {
                    tarotHistoryList.add(0, item);
                    saveHistory();
                }));
    }

    public void deleteLiuyaoHistory(final long id) {
        historyManager.deleteLiuyao(id);
    }

    public void deleteTarotHistory(final long id) {
        historyManager.deleteTarot(id);
    }

    public void clearAllHistory() {
        historyManager.clearAll(() -> runOnUiThread(() -> {
            liuyaoHistoryList.clear();
            tarotHistoryList.clear();
            renderScreen();
        }));
    }

    @Override
    protected void onDestroy() {
        if (tarotDrawController != null) tarotDrawController.release();
        if (liuyaoCastingController != null) liuyaoCastingController.release();
        if (historyManager != null) historyManager.close();
        if (haptics != null) haptics.release();
        super.onDestroy();
    }
}
