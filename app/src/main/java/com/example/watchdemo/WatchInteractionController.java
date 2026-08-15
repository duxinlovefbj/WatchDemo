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
                activity.clearAllHistory();
            } else if (activity.settingsSelectedIndex == 2) {
                activity.vibrateCustom(VibrationEffect.EFFECT_CLICK);
                activity.showOperationGuide(true);
            } else if (activity.settingsSelectedIndex == 3) {
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
                    LiuyaoHistoryItem item = activity.liuyaoHistoryList.get(activity.historySelectedIndex);
                    activity.liuyaoLineResults = item.lineResults.clone();
                    activity.liuyaoResultTimeMillis = item.createdAt;
                    activity.selectedHexagramLineIndex = 0;
                    activity.currentScreen = MainActivity.ScreenState.LIUYAO_RESULT;
                    activity.liuyaoResultSubPage = 0;
                    activity.liuyaoResultShowChanged = false;
                } else {
                    return;
                }
            } else {
                if (!activity.tarotHistoryList.isEmpty() && activity.historySelectedIndex < activity.tarotHistoryList.size()) {
                    TarotHistoryItem item = activity.tarotHistoryList.get(activity.historySelectedIndex);
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
            activity.startTarotDrawSession();
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
        if (activity.currentScreen == MainActivity.ScreenState.TAROT_DRAW) {
            View child = activity.getActiveScreenView();
            if (child instanceof TarotDrawScreenView) {
                ((TarotDrawScreenView) child).handleLongPress(x, y);
            }
            return;
        }
        if (activity.currentScreen == MainActivity.ScreenState.TAROT_RESULT
                || activity.currentScreen == MainActivity.ScreenState.HISTORY
                || activity.currentScreen == MainActivity.ScreenState.SETTINGS
                || activity.currentScreen == MainActivity.ScreenState.LIUYAO_DRAW
                || activity.currentScreen == MainActivity.ScreenState.TAROT_ARRAY_SELECT
                || activity.currentScreen == MainActivity.ScreenState.LIUYAO_RESULT) {
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
                ((TarotDrawScreenView) child).startDrawAnimation();
            }
        } else if (activity.currentScreen == MainActivity.ScreenState.TAROT_RESULT) {
            View child = activity.getActiveScreenView();
            if (child instanceof TarotResultScreenView) {
                ((TarotResultScreenView) child).onCrownScroll(true);
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
                // 无表冠设备通过下滑按画面从上到下浏览，到初爻后循环回上爻。
                activity.selectedHexagramLineIndex =
                        (activity.selectedHexagramLineIndex + 5) % 6;
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
