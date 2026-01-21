package com.example.deteksipenyakitdaunpadi;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import java.security.SecureRandom;
import java.security.spec.KeySpec;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "PenyakitPadi.db";
    private static final int DATABASE_VERSION = 5;

    // Tabel Users
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "id";
    private static final String COLUMN_USER_EMAIL = "email";
    private static final String COLUMN_USER_PASSWORD = "password";
    private static final String COLUMN_USER_SALT = "salt";
    private static final String COLUMN_USER_USERNAME = "username";
    private static final String COLUMN_USER_PROFILE_IMAGE_URI = "profile_image_uri";

    // Personal info (from registration)
    private static final String COLUMN_USER_FULL_NAME = "full_name";
    private static final String COLUMN_USER_PHONE_NUMBER = "phone_number";
    private static final String COLUMN_USER_GENDER = "gender";
    private static final String COLUMN_USER_DATE_OF_BIRTH = "date_of_birth";

    // Tabel History
    private static final String TABLE_HISTORY = "history";
    private static final String COLUMN_HISTORY_ID = "id";
    private static final String COLUMN_HISTORY_PENYAKIT = "penyakit_name";
    private static final String COLUMN_HISTORY_CONFIDENCE = "confidence";
    private static final String COLUMN_HISTORY_DATE = "date";
    private static final String COLUMN_HISTORY_IMAGE_PATH = "image_path"; // Kolom baru

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_EMAIL + " TEXT UNIQUE,"
            + COLUMN_USER_PASSWORD + " TEXT,"
            + COLUMN_USER_SALT + " TEXT,"
            + COLUMN_USER_USERNAME + " TEXT,"
            + COLUMN_USER_PROFILE_IMAGE_URI + " TEXT,"
            + COLUMN_USER_FULL_NAME + " TEXT,"
            + COLUMN_USER_PHONE_NUMBER + " TEXT,"
            + COLUMN_USER_GENDER + " TEXT,"
            + COLUMN_USER_DATE_OF_BIRTH + " TEXT" + ")";
        db.execSQL(CREATE_USERS_TABLE);

        String CREATE_HISTORY_TABLE = "CREATE TABLE " + TABLE_HISTORY + "("
                + COLUMN_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_HISTORY_PENYAKIT + " TEXT,"
                + COLUMN_HISTORY_CONFIDENCE + " TEXT,"
                + COLUMN_HISTORY_DATE + " TEXT,"
                + COLUMN_HISTORY_IMAGE_PATH + " TEXT" + ")";
        db.execSQL(CREATE_HISTORY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Migration to version 3: Add salt column to users table
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_SALT + " TEXT"); } catch (Exception ignored) {}
        }

        if (oldVersion < 4) {
            // Migration to version 4: Add username + profile image uri
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_USERNAME + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_PROFILE_IMAGE_URI + " TEXT"); } catch (Exception ignored) {}

            // Ensure salt column exists even for DBs created at v3 with old onCreate
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_SALT + " TEXT"); } catch (Exception ignored) {}
        }

        if (oldVersion < 5) {
            // Migration to version 5: Add personal info fields
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_FULL_NAME + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_PHONE_NUMBER + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_GENDER + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_DATE_OF_BIRTH + " TEXT"); } catch (Exception ignored) {}
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle downgrade by recreating tables (data will be lost)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    // --- Fungsi untuk Users ---
    public boolean addUser(String email, String password) {
        return addUser(email, password, null, null, null, null, null);
    }

    public boolean addUser(String email, String password, String fullName, String phoneNumber, String gender, String dateOfBirth, String profileImageUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_EMAIL, email);

        String fullNameTrimmed = fullName != null ? fullName.trim() : "";
        // Use full name as default display name if available.
        String usernameDefault = !fullNameTrimmed.isEmpty() ? fullNameTrimmed : deriveUsernameFromEmail(email);
        String salt = generateSalt();
        String passwordHash = hashPassword(password, salt);
        values.put(COLUMN_USER_PASSWORD, passwordHash);
        values.put(COLUMN_USER_SALT, salt);
        values.put(COLUMN_USER_USERNAME, usernameDefault);
        values.put(COLUMN_USER_PROFILE_IMAGE_URI, profileImageUri != null ? profileImageUri : "");

        values.put(COLUMN_USER_FULL_NAME, fullNameTrimmed);
        values.put(COLUMN_USER_PHONE_NUMBER, phoneNumber != null ? phoneNumber.trim() : "");
        values.put(COLUMN_USER_GENDER, gender != null ? gender.trim() : "");
        values.put(COLUMN_USER_DATE_OF_BIRTH, dateOfBirth != null ? dateOfBirth.trim() : "");

        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result != -1;
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String[] columns = {COLUMN_USER_PASSWORD, COLUMN_USER_SALT};
            String selection = COLUMN_USER_EMAIL + "=?";
            String[] selectionArgs = {email};
            cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
            if (!cursor.moveToFirst()) return false;

            String storedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_PASSWORD));
            String storedSalt = "";
            int saltIdx = cursor.getColumnIndex(COLUMN_USER_SALT);
            if (saltIdx >= 0) {
                storedSalt = cursor.getString(saltIdx);
            }

            // Backward compatibility: if salt is missing/empty, treat storedPassword as plain-text.
            if (storedSalt == null || storedSalt.trim().isEmpty()) {
                boolean ok = storedPassword != null && storedPassword.equals(password);
                if (ok) {
                    // Upgrade in-place to hashed password.
                    String newSalt = generateSalt();
                    String newHash = hashPassword(password, newSalt);
                    upgradePasswordHash(email, newHash, newSalt);
                }
                return ok;
            }

            String computed = hashPassword(password, storedSalt);
            return storedPassword != null && storedPassword.equals(computed);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    public UserAccount getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String[] columns = {
                COLUMN_USER_EMAIL,
                COLUMN_USER_USERNAME,
                COLUMN_USER_PROFILE_IMAGE_URI,
                COLUMN_USER_FULL_NAME,
                COLUMN_USER_PHONE_NUMBER,
                COLUMN_USER_GENDER,
                COLUMN_USER_DATE_OF_BIRTH
            };
            cursor = db.query(TABLE_USERS, columns, COLUMN_USER_EMAIL + "=?", new String[]{email}, null, null, null);
            if (!cursor.moveToFirst()) return null;

            String e = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_EMAIL));
            String username = "";
            int idxUsername = cursor.getColumnIndex(COLUMN_USER_USERNAME);
            if (idxUsername >= 0) {
                username = cursor.getString(idxUsername);
            }
            if (username == null || username.trim().isEmpty()) {
                username = deriveUsernameFromEmail(e);
            }

            String profileUri = "";
            int idxUri = cursor.getColumnIndex(COLUMN_USER_PROFILE_IMAGE_URI);
            if (idxUri >= 0) {
                profileUri = cursor.getString(idxUri);
            }

            String fullName = "";
            int idxFullName = cursor.getColumnIndex(COLUMN_USER_FULL_NAME);
            if (idxFullName >= 0) {
                fullName = cursor.getString(idxFullName);
            }

            String phoneNumber = "";
            int idxPhone = cursor.getColumnIndex(COLUMN_USER_PHONE_NUMBER);
            if (idxPhone >= 0) {
                phoneNumber = cursor.getString(idxPhone);
            }

            String gender = "";
            int idxGender = cursor.getColumnIndex(COLUMN_USER_GENDER);
            if (idxGender >= 0) {
                gender = cursor.getString(idxGender);
            }

            String dateOfBirth = "";
            int idxDob = cursor.getColumnIndex(COLUMN_USER_DATE_OF_BIRTH);
            if (idxDob >= 0) {
                dateOfBirth = cursor.getString(idxDob);
            }

            return new UserAccount(
                    e,
                    username,
                    profileUri != null ? profileUri : "",
                    fullName != null ? fullName : "",
                    phoneNumber != null ? phoneNumber : "",
                    gender != null ? gender : "",
                    dateOfBirth != null ? dateOfBirth : ""
            );
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    public boolean updatePersonalInfo(String email, String fullName, String phoneNumber, String dateOfBirth) {
        if (email == null || email.trim().isEmpty()) return false;

        String fn = fullName != null ? fullName.trim() : "";
        String pn = phoneNumber != null ? phoneNumber.trim() : "";
        String dob = dateOfBirth != null ? dateOfBirth.trim() : "";

        if (fn.isEmpty()) return false;
        if (pn.isEmpty()) return false;
        if (dob.isEmpty()) return false;

        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_USER_FULL_NAME, fn);
            values.put(COLUMN_USER_PHONE_NUMBER, pn);
            values.put(COLUMN_USER_DATE_OF_BIRTH, dob);
            // Keep display name in sync with full name.
            values.put(COLUMN_USER_USERNAME, fn);

            int rows = db.update(TABLE_USERS, values, COLUMN_USER_EMAIL + "=?", new String[]{email});
            return rows > 0;
        } finally {
            db.close();
        }
    }

    public boolean updateUsername(String email, String newUsername) {
        if (email == null || email.trim().isEmpty()) return false;
        if (newUsername == null) return false;
        String u = newUsername.trim();
        if (u.isEmpty()) return false;

        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_USER_USERNAME, u);
            int rows = db.update(TABLE_USERS, values, COLUMN_USER_EMAIL + "=?", new String[]{email});
            return rows > 0;
        } finally {
            db.close();
        }
    }

    public boolean updateProfileImageUri(String email, String uriString) {
        if (email == null || email.trim().isEmpty()) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_USER_PROFILE_IMAGE_URI, uriString != null ? uriString : "");
            int rows = db.update(TABLE_USERS, values, COLUMN_USER_EMAIL + "=?", new String[]{email});
            return rows > 0;
        } finally {
            db.close();
        }
    }

    public boolean changePassword(String email, String oldPassword, String newPassword) {
        if (email == null || email.trim().isEmpty()) return false;
        if (newPassword == null || newPassword.length() < 6) return false;
        if (!checkUser(email, oldPassword != null ? oldPassword : "")) return false;

        String salt = generateSalt();
        String hash = hashPassword(newPassword, salt);

        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_USER_PASSWORD, hash);
            values.put(COLUMN_USER_SALT, salt);
            int rows = db.update(TABLE_USERS, values, COLUMN_USER_EMAIL + "=?", new String[]{email});
            return rows > 0;
        } finally {
            db.close();
        }
    }

    private void upgradePasswordHash(String email, String newHash, String newSalt) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_USER_PASSWORD, newHash);
            values.put(COLUMN_USER_SALT, newSalt);
            db.update(TABLE_USERS, values, COLUMN_USER_EMAIL + "=?", new String[]{email});
        } finally {
            db.close();
        }
    }

    private static String deriveUsernameFromEmail(String email) {
        if (email == null) return "";
        int at = email.indexOf('@');
        if (at > 0) return email.substring(0, at);
        return email;
    }

    private static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    private static String hashPassword(String password, String saltBase64) {
        if (password == null) password = "";
        if (saltBase64 == null) saltBase64 = "";
        try {
            byte[] salt = Base64.decode(saltBase64, Base64.NO_WRAP);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 12000, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) {
            // Worst-case fallback (should not happen)
            return password;
        }
    }

    // --- Fungsi untuk History ---
    public boolean addHistory(HistoryItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HISTORY_PENYAKIT, item.penyakitName);
        values.put(COLUMN_HISTORY_CONFIDENCE, item.confidence);
        values.put(COLUMN_HISTORY_DATE, item.date);
        values.put(COLUMN_HISTORY_IMAGE_PATH, item.imagePath);

        long result = db.insert(TABLE_HISTORY, null, values);
        db.close();
        return result != -1;
    }

    public List<HistoryItem> getAllHistory() {
        List<HistoryItem> historyList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_HISTORY + " ORDER BY " + COLUMN_HISTORY_ID + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_ID));
                String penyakit = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_PENYAKIT));
                String confidence = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_CONFIDENCE));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_DATE));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_IMAGE_PATH));

                HistoryItem item = new HistoryItem(id, penyakit, date, confidence, imagePath);
                historyList.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return historyList;
    }

    public void deleteHistory(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, COLUMN_HISTORY_ID + "=?", new String[]{id});
        db.close();
    }
}
