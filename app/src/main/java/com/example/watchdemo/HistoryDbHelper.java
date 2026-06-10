package com.example.watchdemo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HistoryDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "xingyao_history.db";
    private static final int DATABASE_VERSION = 1;

    private static HistoryDbHelper instance;

    public static synchronized HistoryDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new HistoryDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    public HistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setWriteAheadLoggingEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS liuyao_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "display_str TEXT," +
                "line_results TEXT," +
                "create_time INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS tarot_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "display_str TEXT," +
                "target_count INTEGER," +
                "drawn_count INTEGER," +
                "drawn_indices TEXT," +
                "card_states TEXT," +
                "array_selected_index INTEGER," +
                "create_time INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS liuyao_history");
        db.execSQL("DROP TABLE IF EXISTS tarot_history");
        onCreate(db);
    }
}
