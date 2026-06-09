package com.example.watchdemo;

import android.os.Bundle;

public class SubActivity extends MainActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.content.Intent intent = getIntent();
        if (intent != null && intent.hasExtra("TARGET_SCREEN")) {
            currentScreen = (ScreenState) intent.getSerializableExtra("TARGET_SCREEN");
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    void switchScreen(ScreenState newState) {
        if (newState == ScreenState.INIT) {
            finish();
        } else {
            super.switchScreen(newState);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}
