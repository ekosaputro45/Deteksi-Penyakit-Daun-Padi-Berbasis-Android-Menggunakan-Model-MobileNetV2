package com.example.deteksipenyakitdaunpadi;

import android.graphics.Bitmap;

public class BitmapCache {
    private static BitmapCache instance;
    private Bitmap cachedBitmap;

    private BitmapCache() {
    }

    public static BitmapCache getInstance() {
        if (instance == null) {
            instance = new BitmapCache();
        }
        return instance;
    }

    public void setBitmap(Bitmap bitmap) {
        this.cachedBitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return cachedBitmap;
    }

    public void clearBitmap() {
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            cachedBitmap = null;
        }
    }
}

