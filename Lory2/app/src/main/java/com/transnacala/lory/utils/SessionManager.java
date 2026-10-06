package com.transnacala.lory.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, String userId, String name, String email, String role, String turmaId) {
        prefs.edit()
                .putString(Constants.KEY_JWT_TOKEN, token)
                .putString(Constants.KEY_USER_ID, userId)
                .putString(Constants.KEY_USER_NAME, name)
                .putString(Constants.KEY_USER_EMAIL, email)
                .putString(Constants.KEY_USER_ROLE, role)
                .putString(Constants.KEY_TURMA_ID, turmaId)
                .putBoolean(Constants.KEY_IS_LOGGED_IN, true)
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(Constants.KEY_IS_LOGGED_IN, false);
    }

    public String getToken() {
        return prefs.getString(Constants.KEY_JWT_TOKEN, "");
    }

    public String getUserId() {
        return prefs.getString(Constants.KEY_USER_ID, "");
    }

    public String getUserName() {
        return prefs.getString(Constants.KEY_USER_NAME, "Estudante");
    }

    public String getUserEmail() {
        return prefs.getString(Constants.KEY_USER_EMAIL, "");
    }

    public String getUserRole() {
        return prefs.getString(Constants.KEY_USER_ROLE, "ESTUDANTE");
    }

    public String getTurmaId() {
        return prefs.getString(Constants.KEY_TURMA_ID, "");
    }

    public boolean isChefe() {
        return "CHEFE".equalsIgnoreCase(getUserRole());
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}