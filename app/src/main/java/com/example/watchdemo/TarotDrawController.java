package com.example.watchdemo;

import android.os.VibrationEffect;

import java.util.Collections;
import java.util.Random;

/** Owns Tarot draw-session setup, card selection, and completion transition. */
final class TarotDrawController {
    private static final int DECK_SIZE = 78;
    private static final int DEFAULT_SELECTED_INDEX = 39;
    private static final long RESULT_TRANSITION_DELAY_MS = 300L;
    private static final int[] TARGET_COUNTS = {
            -1, 3, 7, 5, 10, 4, 5, 9, 7, 4, 7, 5, 6
    };

    private final MainActivity activity;
    private Random orientationRandom;
    private Runnable pendingResultTransition;

    TarotDrawController(MainActivity activity) {
        this.activity = activity;
    }

    void startSession() {
        activity.tarotTargetCount = targetCountFor(activity.tarotArraySelectedIndex);
        activity.availableTarotCards.clear();
        for (int i = 0; i < DECK_SIZE; i++) activity.availableTarotCards.add(i);
        Collections.shuffle(activity.availableTarotCards, new Random(activity.appStartTime));

        activity.tarotDrawnCount = 0;
        activity.tarotSelectedCardIndex = DEFAULT_SELECTED_INDEX;
        int resultCapacity = activity.tarotTargetCount > 0
                ? activity.tarotTargetCount : DECK_SIZE;
        activity.tarotDrawnIndices = new int[resultCapacity];
        activity.tarotCardStates = new boolean[resultCapacity];
        orientationRandom = new Random(System.currentTimeMillis());
        activity.isTarotFastSlideUnlocked = false;
        activity.switchScreen(MainActivity.ScreenState.TAROT_DRAW);
    }

    void drawSelectedCard(boolean fillFromLeft) {
        int maxCount = activity.tarotTargetCount > 0
                ? activity.tarotTargetCount : DECK_SIZE;
        if (activity.tarotDrawnCount >= maxCount || activity.availableTarotCards.isEmpty()) return;

        int drawnCardId = activity.availableTarotCards.remove(activity.tarotSelectedCardIndex);
        activity.tarotDrawnIndices[activity.tarotDrawnCount] = drawnCardId;
        activity.tarotCardStates[activity.tarotDrawnCount] = orientationRandom != null
                ? orientationRandom.nextBoolean() : Math.random() > 0.5;
        activity.tarotDrawnCount++;
        activity.vibrateCustom(VibrationEffect.EFFECT_CLICK);
        updateSelectionAfterRemoval(fillFromLeft);
        activity.renderScreen();

        if (activity.tarotTargetCount > 0
                && activity.tarotDrawnCount == activity.tarotTargetCount) {
            scheduleResultTransition();
        }
    }

    void release() {
        if (pendingResultTransition != null && activity.mSafeContainer != null) {
            activity.mSafeContainer.removeCallbacks(pendingResultTransition);
            pendingResultTransition = null;
        }
    }

    private void updateSelectionAfterRemoval(boolean fillFromLeft) {
        int remaining = activity.availableTarotCards.size();
        if (remaining == 0) return;
        if (fillFromLeft) {
            activity.tarotSelectedCardIndex =
                    (activity.tarotSelectedCardIndex - 1 + remaining) % remaining;
        } else {
            activity.tarotSelectedCardIndex =
                    Math.min(activity.tarotSelectedCardIndex, remaining - 1);
        }
    }

    private void scheduleResultTransition() {
        if (pendingResultTransition != null) {
            activity.mSafeContainer.removeCallbacks(pendingResultTransition);
        }
        pendingResultTransition = () -> {
            pendingResultTransition = null;
            if (activity.currentScreen != MainActivity.ScreenState.TAROT_DRAW) return;
            activity.tarotResultLayer = 1;
            activity.tarotResultDetailIndex = 0;
            activity.addTarotHistory();
            activity.switchScreen(MainActivity.ScreenState.TAROT_RESULT);
        };
        activity.mSafeContainer.postDelayed(
                pendingResultTransition, RESULT_TRANSITION_DELAY_MS);
    }

    private static int targetCountFor(int arrayIndex) {
        return arrayIndex >= 0 && arrayIndex < TARGET_COUNTS.length
                ? TARGET_COUNTS[arrayIndex] : TARGET_COUNTS[TARGET_COUNTS.length - 1];
    }
}
