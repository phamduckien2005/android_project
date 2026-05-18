package com.example.sqlite;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public final class UserSession {
    public static final String PREF_NAME = "USER_FILE";
    public static final String KEY_CURRENT_USER_ID = "current_user_id";
    public static final String KEY_CURRENT_USER_EMAIL = "current_user_email";
    public static final String KEY_CURRENT_USER_NAME = "current_user_name";

    private UserSession() {
    }

    public static void saveCurrentUser(Context context, String userId, String email) {
        saveCurrentUser(context, userId, email, "");
    }

    public static void saveCurrentUser(Context context, String userId, String email, String name) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CURRENT_USER_ID, userId)
                .putString(KEY_CURRENT_USER_EMAIL, email == null ? "" : email)
                .putString(KEY_CURRENT_USER_NAME, name == null ? "" : name)
                .apply();
    }

    public static void clearCurrentUser(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_CURRENT_USER_ID)
                .remove(KEY_CURRENT_USER_EMAIL)
                .remove(KEY_CURRENT_USER_NAME)
                .apply();
    }

    public static String getCurrentUserId(Context context) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null && firebaseUser.getUid() != null && !firebaseUser.getUid().isEmpty()) {
            return firebaseUser.getUid();
        }

        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedUserId = preferences.getString(KEY_CURRENT_USER_ID, "");
        if (savedUserId != null && !savedUserId.isEmpty()) {
            return savedUserId;
        }

        return "guest";
    }

    public static String getDisplayName(Context context) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String firebaseName = firebaseUser.getDisplayName();
            if (firebaseName != null && !firebaseName.trim().isEmpty()) {
                return firebaseName.trim();
            }
            String firebaseEmail = firebaseUser.getEmail();
            if (firebaseEmail != null && !firebaseEmail.trim().isEmpty()) {
                return formatEmailName(firebaseEmail);
            }
        }

        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String currentName = preferences.getString(KEY_CURRENT_USER_NAME, "");
        if (currentName != null && !currentName.trim().isEmpty()) {
            return currentName.trim();
        }

        String savedName = preferences.getString("name", "");
        if (savedName != null && !savedName.trim().isEmpty()) {
            return savedName.trim();
        }

        String email = preferences.getString(KEY_CURRENT_USER_EMAIL, "");
        if (email == null || email.trim().isEmpty()) {
            email = preferences.getString("email", "");
        }
        if (email != null && !email.trim().isEmpty()) {
            return formatEmailName(email);
        }

        return "bạn";
    }

    private static String formatEmailName(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex > 0) {
            return email.substring(0, atIndex);
        }
        return email;
    }
}
