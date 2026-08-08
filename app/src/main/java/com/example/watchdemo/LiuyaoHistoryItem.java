package com.example.watchdemo;

/** Immutable snapshot of a saved Liuyao reading. */
public final class LiuyaoHistoryItem {
    public final long id;
    public final String displayStr;
    public final int[] lineResults;
    public final long createdAt;

    public LiuyaoHistoryItem(long id, String displayStr, int[] lineResults, long createdAt) {
        this.id = id;
        this.displayStr = displayStr;
        this.lineResults = lineResults.clone();
        this.createdAt = createdAt;
    }
}
