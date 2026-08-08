package com.example.watchdemo;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.InputStream;

/** Decodes and releases the card images used by one result-screen instance. */
final class TarotBitmapStore {
    private static final String TAG = "TarotBitmapStore";
    private Bitmap[] bitmaps = new Bitmap[0];

    void load(AssetManager assets, int[] drawnIndices, int drawnCount) {
        if (bitmaps.length != drawnCount) {
            recycle();
            bitmaps = new Bitmap[drawnCount];
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        for (int i = 0; i < drawnCount; i++) {
            if (bitmaps[i] != null && !bitmaps[i].isRecycled()) continue;
            String path = "image/" + TarotDeck.getCardFilename(drawnIndices[i]);
            try (InputStream stream = assets.open(path)) {
                bitmaps[i] = BitmapFactory.decodeStream(stream, null, options);
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode " + path, e);
            }
        }
    }

    Bitmap get(int index) {
        return index >= 0 && index < bitmaps.length ? bitmaps[index] : null;
    }

    void recycle() {
        for (Bitmap bitmap : bitmaps) {
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        }
        bitmaps = new Bitmap[0];
    }
}
