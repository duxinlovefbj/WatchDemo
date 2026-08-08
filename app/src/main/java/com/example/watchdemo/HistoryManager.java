package com.example.watchdemo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Handles history persistence and serialization for the activity. */
final class HistoryManager {
    private static final String TAG = "HistoryManager";
    private static final String[] TAROT_ARRAY_NAMES = {
            "自由抽牌", "圣三角", "六芒星", "时间之箭", "凯尔特十字", "四元素",
            "二选一", "金字塔", "七脉轮", "直击问题", "指引之星", "财务", "人际关系"
    };

    interface LoadCallback {
        void onLoaded(List<LiuyaoHistoryItem> liuyao, List<TarotHistoryItem> tarot);
    }

    interface LiuyaoInsertCallback {
        void onInserted(LiuyaoHistoryItem item);
    }

    interface TarotInsertCallback {
        void onInserted(TarotHistoryItem item);
    }

    private final HistoryDbHelper dbHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    HistoryManager(android.content.Context context) {
        dbHelper = HistoryDbHelper.getInstance(context);
    }

    void load(LoadCallback callback) {
        executor.execute(() -> {
            List<LiuyaoHistoryItem> liuyao = new ArrayList<>();
            List<TarotHistoryItem> tarot = new ArrayList<>();
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                readLiuyao(db, liuyao);
                readTarot(db, tarot);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load history", e);
            }
            callback.onLoaded(liuyao, tarot);
        });
    }

    void addLiuyao(int[] lineResults, long createdAt, LiuyaoInsertCallback callback) {
        int[] values = lineResults.clone();
        String display = date(createdAt) + " " + liuyaoName(values);
        executor.execute(() -> {
            ContentValues row = new ContentValues();
            row.put("display_str", display);
            row.put("line_results", join(values));
            row.put("create_time", createdAt);
            long id = insert("liuyao_history", row);
            callback.onInserted(new LiuyaoHistoryItem(id, display, values, createdAt));
        });
    }

    void addTarot(int arrayIndex, int targetCount, int drawnCount, int[] drawnIndices,
                  boolean[] cardStates, String primaryCardName, TarotInsertCallback callback) {
        int[] indices = drawnIndices.clone();
        boolean[] states = cardStates.clone();
        String suffix = drawnCount > 0 ? " (" + primaryCardName + ")" : "";
        String display = today() + " " + tarotArrayName(arrayIndex) + suffix;
        executor.execute(() -> {
            ContentValues row = new ContentValues();
            row.put("display_str", display);
            row.put("target_count", targetCount);
            row.put("drawn_count", drawnCount);
            row.put("drawn_indices", join(indices));
            row.put("card_states", join(states));
            row.put("array_selected_index", arrayIndex);
            row.put("create_time", System.currentTimeMillis());
            long id = insert("tarot_history", row);
            callback.onInserted(new TarotHistoryItem(
                    id, display, targetCount, drawnCount, indices, states, arrayIndex));
        });
    }

    void deleteLiuyao(long id) {
        delete("liuyao_history", id);
    }

    void deleteTarot(long id) {
        delete("tarot_history", id);
    }

    void clearAll(Runnable callback) {
        executor.execute(() -> {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete("liuyao_history", null, null);
                db.delete("tarot_history", null, null);
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear history", e);
            }
            callback.run();
        });
    }

    void close() {
        executor.shutdown();
    }

    private void readLiuyao(SQLiteDatabase db, List<LiuyaoHistoryItem> output) {
        try (Cursor cursor = db.query(
                "liuyao_history", null, null, null, null, null, "create_time DESC")) {
            int id = cursor.getColumnIndexOrThrow("id");
            int display = cursor.getColumnIndexOrThrow("display_str");
            int results = cursor.getColumnIndexOrThrow("line_results");
            int createdAt = cursor.getColumnIndexOrThrow("create_time");
            while (cursor.moveToNext()) {
                output.add(new LiuyaoHistoryItem(cursor.getLong(id), cursor.getString(display),
                        parseInts(cursor.getString(results)), cursor.getLong(createdAt)));
            }
        }
    }

    private void readTarot(SQLiteDatabase db, List<TarotHistoryItem> output) {
        try (Cursor cursor = db.query(
                "tarot_history", null, null, null, null, null, "create_time DESC")) {
            int id = cursor.getColumnIndexOrThrow("id");
            int display = cursor.getColumnIndexOrThrow("display_str");
            int target = cursor.getColumnIndexOrThrow("target_count");
            int drawn = cursor.getColumnIndexOrThrow("drawn_count");
            int indices = cursor.getColumnIndexOrThrow("drawn_indices");
            int states = cursor.getColumnIndexOrThrow("card_states");
            int array = cursor.getColumnIndexOrThrow("array_selected_index");
            while (cursor.moveToNext()) {
                output.add(new TarotHistoryItem(cursor.getLong(id), cursor.getString(display),
                        cursor.getInt(target), cursor.getInt(drawn), parseInts(cursor.getString(indices)),
                        parseBooleans(cursor.getString(states)), cursor.getInt(array)));
            }
        }
    }

    private long insert(String table, ContentValues values) {
        try {
            return dbHelper.getWritableDatabase().insert(table, null, values);
        } catch (Exception e) {
            Log.e(TAG, "Failed to insert into " + table, e);
            return -1;
        }
    }

    private void delete(String table, long id) {
        executor.execute(() -> {
            try {
                dbHelper.getWritableDatabase().delete(
                        table, "id = ?", new String[]{String.valueOf(id)});
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete from " + table, e);
            }
        });
    }

    private static String liuyaoName(int[] values) {
        LiuyaoCalculator.Result result = LiuyaoCalculator.calculate(values);
        String name = result.benGuaIdx >= 0
                ? LiuyaoCalculator.GUA_DATA[result.benGuaIdx].name : "未知卦";
        if (result.sumBian > 0) {
            String changed = result.zhiGuaIdx >= 0
                    ? LiuyaoCalculator.GUA_DATA[result.zhiGuaIdx].name : "未知卦";
            name += "之" + changed;
        }
        return name;
    }

    private static String tarotArrayName(int index) {
        return index >= 0 && index < TAROT_ARRAY_NAMES.length
                ? TAROT_ARRAY_NAMES[index] : TAROT_ARRAY_NAMES[0];
    }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private static String date(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(timestamp));
    }

    private static int[] parseInts(String value) {
        if (value == null || value.trim().isEmpty()) return new int[0];
        String[] parts = value.split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) result[i] = Integer.parseInt(parts[i].trim());
        return result;
    }

    private static boolean[] parseBooleans(String value) {
        if (value == null || value.trim().isEmpty()) return new boolean[0];
        String[] parts = value.split(",");
        boolean[] result = new boolean[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = "1".equals(parts[i].trim())
                    || "true".equalsIgnoreCase(parts[i].trim());
        }
        return result;
    }

    private static String join(int[] values) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) result.append(',');
            result.append(values[i]);
        }
        return result.toString();
    }

    private static String join(boolean[] values) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) result.append(',');
            result.append(values[i] ? '1' : '0');
        }
        return result.toString();
    }
}
