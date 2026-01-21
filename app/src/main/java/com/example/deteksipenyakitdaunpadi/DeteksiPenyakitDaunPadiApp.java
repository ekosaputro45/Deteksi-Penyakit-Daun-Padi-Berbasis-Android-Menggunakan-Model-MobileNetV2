package com.example.deteksipenyakitdaunpadi;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class DeteksiPenyakitDaunPadiApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Paksa aplikasi selalu mode terang, abaikan setting dark mode sistem.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
