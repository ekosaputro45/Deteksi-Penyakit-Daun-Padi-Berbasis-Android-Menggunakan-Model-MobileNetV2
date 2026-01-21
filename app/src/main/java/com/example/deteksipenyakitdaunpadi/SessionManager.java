package com.example.deteksipenyakitdaunpadi;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    // Reuse existing shared prefs name from LoginActivity to keep behavior consistent.
    private static final String PREFS = LoginActivity.SHARED_PREFS;

    private static final String KEY_REMEMBER_ME = LoginActivity.REMEMBER_ME;
    private static final String KEY_LOGGED_IN_EMAIL = "loggedInEmail";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void login(String email, boolean rememberMe) {
        prefs.edit()
                .putString(KEY_LOGGED_IN_EMAIL, email)
                .putBoolean(KEY_REMEMBER_ME, rememberMe)
                .apply();
    }

    public String getLoggedInEmail() {
        return prefs.getString(KEY_LOGGED_IN_EMAIL, null);
    }

    public boolean isRememberMe() {
        return prefs.getBoolean(KEY_REMEMBER_ME, false);
    }

    public boolean hasSession() {
        String email = getLoggedInEmail();
        return email != null && !email.trim().isEmpty();
    }

    public void logout() {
        prefs.edit()
                .remove(KEY_LOGGED_IN_EMAIL)
                .putBoolean(KEY_REMEMBER_ME, false)
                .apply();
    }
}
