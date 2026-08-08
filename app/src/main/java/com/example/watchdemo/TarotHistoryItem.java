package com.example.watchdemo;

/** Immutable snapshot of a saved Tarot reading. */
public final class TarotHistoryItem {
    public final long id;
    public final String displayStr;
    public final int targetCount;
    public final int drawnCount;
    public final int[] drawnIndices;
    public final boolean[] cardStates;
    public final int arraySelectedIndex;

    public TarotHistoryItem(long id, String displayStr, int targetCount, int drawnCount,
                            int[] drawnIndices, boolean[] cardStates, int arraySelectedIndex) {
        this.id = id;
        this.displayStr = displayStr;
        this.targetCount = targetCount;
        this.drawnCount = drawnCount;
        this.drawnIndices = drawnIndices.clone();
        this.cardStates = cardStates.clone();
        this.arraySelectedIndex = arraySelectedIndex;
    }
}
