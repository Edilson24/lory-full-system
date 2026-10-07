package com.transnacala.lory.utils;

public class Constants {
    public static final String BASE_URL = "https://behalf-dip-steel-mix.trycloudflare.com/";

    public static final String PREF_NAME = "lory_session_prefs";
    public static final String KEY_JWT_TOKEN = "jwt_token";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_USER_EMAIL = "user_email";
    public static final String KEY_USER_ROLE = "user_role"; // CHEFE, ESTUDANTE, etc.
    public static final String KEY_TURMA_ID = "turma_id";
    public static final String KEY_IS_LOGGED_IN = "is_logged_in";

    // Sync Statuses
    public static final String SYNC_STATUS_PENDING_INSERT = "PENDING_INSERT";
    public static final String SYNC_STATUS_PENDING_UPDATE = "PENDING_UPDATE";
    public static final String SYNC_STATUS_PENDING_DELETE = "PENDING_DELETE";
    public static final String SYNC_STATUS_SYNCED = "SYNCED";
}