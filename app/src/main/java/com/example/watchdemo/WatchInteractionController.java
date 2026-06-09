package com.example.watchdemo;

import android.app.AlertDialog;
import android.os.VibrationEffect;
import android.view.View;
import android.widget.Toast;

public class WatchInteractionController implements WatchGestureDetector.GestureListener, WatchCrownHandler.CrownListener {
    final MainActivity activity;

    public WatchInteractionController(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onClick(float x, float y) {
        if (activity.currentScreen == MainActivity.ScreenState.TAROT_ARRAY_SELECT 
                || activity.currentScreen == MainActivity.ScreenState.SETTINGS 
                || activity.currentScreen == MainActivity.ScreenState.HISTORY) {
            // 忽略手势检测器触发的列表点击(带有坐标)，仅响应列表项卡片OnClickListener手动调用的(0,0)
            if (x != 0f || y != 0f) {
                return;
            }
        }
        if (activity.currentScreen == MainActivity.ScreenState.INIT) {
            if (activity.mInitScreenView != null) {
                int w = activity.mInitScreenView.getWidth();
                if (x < w / 2f) {
                    activity.mInitScreenView.startTransition(true);
                } else {
                    activity.mInitScreenView.startTransition(false);
                }
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.SETTINGS) {
            if (activity.settingsSelectedIndex == 0) {
                activity.vibrationEnabled = !activity.vibrationEnabled;
                activity.vibrateCustom(VibrationEffect.EFFECT_CLICK);
                activity.saveHistory();
                activity.renderScreen();
            } else if (activity.settingsSelectedIndex == 1) {
                activity.vibrateCustom(VibrationEffect.EFFECT_CLICK);
                Toast.makeText(activity, "历史记录已清空", Toast.LENGTH_SHORT).show();
                activity.liuyaoHistoryList.clear();
                activity.tarotHistoryList.clear();
                activity.saveHistory();
                activity.renderScreen();
            } else if (activity.settingsSelectedIndex == 2) {
                activity.vibrateCustom(VibrationEffect.EFFECT_CLICK);
                final android.app.Dialog dialog = new android.app.Dialog(activity, android.R.style.Theme_Material_NoActionBar_Fullscreen);
                dialog.setContentView(R.layout.dialog_about);
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.black);
                }
                View btnClose = dialog.findViewById(R.id.btn_close);
                if (btnClose != null) {
                    btnClose.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                }
                dialog.show();
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.HISTORY) {
            activity.vibrateCustom(VibrationEffect.EFFECT_CLICK);
            if (activity.historyTabIndex == 0) {
                if (!activity.liuyaoHistoryList.isEmpty() && activity.historySelectedIndex < activity.liuyaoHistoryList.size()) {
                    MainActivity.LiuyaoHistoryItem item = activity.liuyaoHistoryList.get(activity.historySelectedIndex);
                    activity.liuyaoLineResults = item.lineResults.clone();
                    activity.selectedHexagramLineIndex = 0;
                    activity.currentScreen = MainActivity.ScreenState.LIUYAO_RESULT;
                    activity.liuyaoResultSubPage = 0;
                    activity.liuyaoResultShowChanged = false;
                } else {
                    return;
                }
            } else {
                if (!activity.tarotHistoryList.isEmpty() && activity.historySelectedIndex < activity.tarotHistoryList.size()) {
                    MainActivity.TarotHistoryItem item = activity.tarotHistoryList.get(activity.historySelectedIndex);
                    activity.tarotArraySelectedIndex = item.arraySelectedIndex;
                    activity.tarotTargetCount = item.targetCount;
                    activity.tarotDrawnCount = item.drawnCount;
                    activity.tarotDrawnIndices = item.drawnIndices.clone();
                    activity.tarotCardStates = item.cardStates.clone();
                    activity.tarotResultLayer = 1;
                    activity.tarotResultDetailIndex = 0;
                    activity.currentScreen = MainActivity.ScreenState.TAROT_RESULT;
                } else {
                    return;
                }
            }
            activity.renderScreen();
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_ARRAY_SELECT) {
            if (activity.tarotArraySelectedIndex == 0) activity.tarotTargetCount = -1; // 自由抽牌
            else if (activity.tarotArraySelectedIndex == 1) activity.tarotTargetCount = 3;  // 圣三角
            else if (activity.tarotArraySelectedIndex == 2) activity.tarotTargetCount = 7;  // 六芒星
            else if (activity.tarotArraySelectedIndex == 3) activity.tarotTargetCount = 5;  // 时间之箭
            else if (activity.tarotArraySelectedIndex == 4) activity.tarotTargetCount = 10; // 凯尔特十字
            else if (activity.tarotArraySelectedIndex == 5) activity.tarotTargetCount = 4;  // 四元素
            else if (activity.tarotArraySelectedIndex == 6) activity.tarotTargetCount = 5;  // 二选一
            else if (activity.tarotArraySelectedIndex == 7) activity.tarotTargetCount = 9;  // 金字塔
            else if (activity.tarotArraySelectedIndex == 8) activity.tarotTargetCount = 7;  // 七脉轮
            else if (activity.tarotArraySelectedIndex == 9) activity.tarotTargetCount = 4;  // 直击问题
            else if (activity.tarotArraySelectedIndex == 10) activity.tarotTargetCount = 7; // 指引之星
            else if (activity.tarotArraySelectedIndex == 11) activity.tarotTargetCount = 5; // 财务
            else activity.tarotTargetCount = 6; // 人际关系
            
            activity.availableTarotCards.clear();
            for (int i = 0; i < 78; i++) {
                activity.availableTarotCards.add(i);
            }
            // 使用打开程序时间 (appStartTime) 作为种子洗牌
            java.util.Random shuffleRand = new java.util.Random(activity.appStartTime);
            java.util.Collections.shuffle(activity.availableTarotCards, shuffleRand);
            
            activity.tarotDrawnCount = 0;
            activity.tarotSelectedCardIndex = 39;
            int allocSize = activity.tarotTargetCount > 0 ? activity.tarotTargetCount : 78;
            activity.tarotDrawnIndices = new int[allocSize];
            activity.tarotCardStates = new boolean[allocSize];

            // 记录进入抽牌界面的时间作为种子，用于在抽牌时随机判定逆位
            activity.tarotDrawEnterTime = System.currentTimeMillis();
            activity.tarotDrawRandom = new java.util.Random(activity.tarotDrawEnterTime);
            
            activity.switchScreen(MainActivity.ScreenState.TAROT_DRAW);
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_DRAW) {
            View child = activity.getActiveScreenView();
            if (child instanceof TarotDrawScreenView) {
                if (((TarotDrawScreenView) child).isAnimating()) {
                    return;
                }
            }
            // 自由抽牌模式在已抽 >= 1 时，点击屏幕任意位置可完成抽牌
            if (activity.tarotTargetCount == -1 && activity.tarotDrawnCount >= 1) {
                activity.tarotResultLayer = 1;
                activity.tarotResultDetailIndex = 0;
                activity.addTarotHistory();
                activity.switchScreen(MainActivity.ScreenState.TAROT_RESULT);
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_RESULT && activity.tarotResultLayer == 1) {
            // 全景模式下支持点击选中的牌槽进入详情
            View child = activity.getActiveScreenView();
            if (child instanceof TarotResultScreenView) {
                TarotResultScreenView view = (TarotResultScreenView) child;
                int clickedIndex = view.getCardIndexAt(x, y);
                if (clickedIndex >= 0 && clickedIndex < activity.tarotDrawnCount) {
                    activity.tarotResultDetailIndex = clickedIndex;
                    activity.tarotResultLayer = 2;
                    activity.tarotResultJustEnteredLayer2 = true;
                    activity.vibrateCustom(VibrationEffect.EFFECT_CLICK);
                    activity.renderScreen();
                }
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.LIUYAO_RESULT) {
            if (activity.liuyaoResultSubPage == 0) {
                int h = activity.mSafeContainer.getHeight();
                if (y > h * 0.15f && y < h * 0.65f) {
                    activity.liuyaoResultShowChanged = !activity.liuyaoResultShowChanged;
                    activity.vibrateCustom(VibrationEffect.EFFECT_CLICK);
                    activity.renderScreen();
                }
            } else if (activity.liuyaoResultSubPage == 1) {
                activity.liuyaoResultShowChanged = !activity.liuyaoResultShowChanged;
                activity.vibrateCustom(VibrationEffect.EFFECT_CLICK);
                View view = activity.getActiveScreenView();
                if (view instanceof LiuyaoResultScreenView) {
                    ((LiuyaoResultScreenView) view).triggerRotationAnimation();
                } else {
                    activity.renderScreen();
                }
            }
        }
    }

    @Override
    public void onLongPress(float x, float y) {
        if (activity.currentScreen == MainActivity.ScreenState.TAROT_DRAW
                || activity.currentScreen == MainActivity.ScreenState.HISTORY
                || activity.currentScreen == MainActivity.ScreenState.SETTINGS
                || activity.currentScreen == MainActivity.ScreenState.LIUYAO_DRAW
                || activity.currentScreen == MainActivity.ScreenState.TAROT_ARRAY_SELECT) {
            // These screens use swipe-to-back, disable long press return to prevent accidental triggers
            return;
        }
        activity.vibrateCustom(VibrationEffect.EFFECT_CLICK);
        if (activity.currentScreen == MainActivity.ScreenState.INIT) {
            return;
        } else {
            activity.switchScreen(MainActivity.ScreenState.INIT);
        }
    }

    @Override
    public void onSwipeLeft() {
        // Deleted LIUYAO_DRAW left swipe handler to prevent accidental exit
    }

    @Override
    public void onSwipeRight() {
        if (activity.currentScreen == MainActivity.ScreenState.INIT) {
            activity.finish(); // 右滑退出程序 (OPPO 设计规范)
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_ARRAY_SELECT) {
            activity.switchScreen(MainActivity.ScreenState.INIT);
        } else if (activity.currentScreen == MainActivity.ScreenState.LIUYAO_RESULT) {
            activity.vibrateCustom(VibrationEffect.EFFECT_CLICK);
            activity.switchScreen(activity.previousScreen);
        }
    }

    @Override
    public void onSwipeUp() {
        if (activity.currentScreen == MainActivity.ScreenState.INIT) {
            activity.switchScreen(MainActivity.ScreenState.SETTINGS);
        } else if (activity.currentScreen == MainActivity.ScreenState.SETTINGS) {
            // Handled by ScrollView touch scrolling
        } else if (activity.currentScreen == MainActivity.ScreenState.HISTORY) {
            // Handled by ScrollView touch scrolling
        } else if (activity.currentScreen == MainActivity.ScreenState.LIUYAO_RESULT) {
            if (activity.liuyaoResultSubPage == 0) {
                activity.liuyaoResultSubPage = 2; // 上滑去爻辞
                activity.vibrateCustom(VibrationEffect.EFFECT_TICK);
                activity.renderScreen();
            } else if (activity.liuyaoResultSubPage == 1) {
                activity.liuyaoResultSubPage = 0; // 从卦象盘返回主页
                activity.vibrateCustom(VibrationEffect.EFFECT_TICK);
                activity.renderScreen();
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_ARRAY_SELECT) {
            // 已改为 ScrollView 滚动列表，由 ScrollView 处理上下滑动
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_DRAW) {
            if (activity.mSafeContainer.getChildCount() > 0 && activity.mSafeContainer.getChildAt(0) instanceof TarotDrawScreenView) {
                ((TarotDrawScreenView) activity.mSafeContainer.getChildAt(0)).startDrawAnimation();
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_RESULT) {
            if (activity.mSafeContainer.getChildCount() > 0 && activity.mSafeContainer.getChildAt(0) instanceof TarotResultScreenView) {
                ((TarotResultScreenView) activity.mSafeContainer.getChildAt(0)).onCrownScroll(true);
            }
        }
    }

    @Override
    public void onSwipeDown() {
        if (activity.currentScreen == MainActivity.ScreenState.INIT) {
            activity.historySelectedIndex = 0;
            activity.switchScreen(MainActivity.ScreenState.HISTORY);
        } else if (activity.currentScreen == MainActivity.ScreenState.SETTINGS) {
            // Handled by ScrollView touch scrolling
        } else if (activity.currentScreen == MainActivity.ScreenState.HISTORY) {
            // Handled by ScrollView touch scrolling
        } else if (activity.currentScreen == MainActivity.ScreenState.LIUYAO_RESULT) {
            if (activity.liuyaoResultSubPage == 0) {
                activity.liuyaoResultSubPage = 1; // 下滑去卦象盘
                activity.vibrateCustom(VibrationEffect.EFFECT_TICK);
                activity.renderScreen();
            } else if (activity.liuyaoResultSubPage == 2) {
                activity.liuyaoResultSubPage = 0; // 从爻辞返回主页
                activity.vibrateCustom(VibrationEffect.EFFECT_TICK);
                activity.renderScreen();
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_ARRAY_SELECT) {
            // 已改为 ScrollView 滚动列表，由 ScrollView 处理上下滑动
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_DRAW) {
            View child = activity.getActiveScreenView();
            if (child instanceof TarotDrawScreenView) {
                if (((TarotDrawScreenView) child).isAnimating()) {
                    return;
                }
            }
            // 自由抽牌模式在已抽 >= 1 时，向下滑动也可完成抽牌
            if (activity.tarotTargetCount == -1 && activity.tarotDrawnCount >= 1) {
                activity.tarotResultLayer = 1;
                activity.tarotResultDetailIndex = 0;
                activity.addTarotHistory();
                activity.switchScreen(MainActivity.ScreenState.TAROT_RESULT);
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_RESULT) {
            View child = activity.getActiveScreenView();
            if (child instanceof TarotResultScreenView) {
                ((TarotResultScreenView) child).onCrownScroll(false);
            }
        }
    }

    @Override
    public void onStepClockwise() {
        View child = activity.getActiveScreenView();
        if (activity.currentScreen == MainActivity.ScreenState.SETTINGS) {
            if (child instanceof SettingsScreenView) {
                ((SettingsScreenView) child).onCrownScroll(true);
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.HISTORY) {
            if (child instanceof HistoryScreenView) {
                ((HistoryScreenView) child).onCrownScroll(true);
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.LIUYAO_DRAW) {
            activity.handleLiuyaoScrollStep(true);
        } else if (activity.currentScreen == MainActivity.ScreenState.LIUYAO_RESULT) {
            if (activity.liuyaoResultSubPage == 2) {
                if (activity.selectedHexagramLineIndex > 0) {
                    activity.selectedHexagramLineIndex--;
                    activity.vibrateCustom(VibrationEffect.EFFECT_TICK);
                    activity.renderScreen();
                }
            } else {
                if (!activity.liuyaoResultShowChanged) {
                    activity.liuyaoResultShowChanged = true;
                    activity.vibrateCustom(VibrationEffect.EFFECT_TICK);
                    if (child instanceof LiuyaoResultScreenView) {
                        ((LiuyaoResultScreenView) child).triggerRotationAnimation();
                    } else {
                        activity.renderScreen();
                    }
                }
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_ARRAY_SELECT) {
            if (child instanceof TarotArraySelectScreenView) {
                ((TarotArraySelectScreenView) child).onCrownScroll(true);
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_DRAW) {
            if (child instanceof TarotDrawScreenView) {
                ((TarotDrawScreenView) child).onCrownScroll(true);
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_RESULT) {
            if (child instanceof TarotResultScreenView) {
                ((TarotResultScreenView) child).onCrownScroll(true);
            }
        }
    }

    @Override
    public void onStepCounterClockwise() {
        View child = activity.getActiveScreenView();
        if (activity.currentScreen == MainActivity.ScreenState.SETTINGS) {
            if (child instanceof SettingsScreenView) {
                ((SettingsScreenView) child).onCrownScroll(false);
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.HISTORY) {
            if (child instanceof HistoryScreenView) {
                ((HistoryScreenView) child).onCrownScroll(false);
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.LIUYAO_DRAW) {
            activity.handleLiuyaoScrollStep(false);
        } else if (activity.currentScreen == MainActivity.ScreenState.LIUYAO_RESULT) {
            if (activity.liuyaoResultSubPage == 2) {
                if (activity.selectedHexagramLineIndex < 5) {
                    activity.selectedHexagramLineIndex++;
                    activity.vibrateCustom(VibrationEffect.EFFECT_TICK);
                    activity.renderScreen();
                }
            } else {
                if (activity.liuyaoResultShowChanged) {
                    activity.liuyaoResultShowChanged = false;
                    activity.vibrateCustom(VibrationEffect.EFFECT_TICK);
                    if (child instanceof LiuyaoResultScreenView) {
                        ((LiuyaoResultScreenView) child).triggerRotationAnimation();
                    } else {
                        activity.renderScreen();
                    }
                }
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_ARRAY_SELECT) {
            if (child instanceof TarotArraySelectScreenView) {
                ((TarotArraySelectScreenView) child).onCrownScroll(false);
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_DRAW) {
            if (child instanceof TarotDrawScreenView) {
                ((TarotDrawScreenView) child).onCrownScroll(false);
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_RESULT) {
            if (child instanceof TarotResultScreenView) {
                ((TarotResultScreenView) child).onCrownScroll(false);
            }
        }
    }
}
