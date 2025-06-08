package com.sena.qfinder.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {
    private static final String PREF_NAME = "QfinderPrefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_LASTNAME = "user_lastname";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_ADDRESS = "user_address";
    private static final String KEY_USER_IDENTIFICATION = "user_identification";
    private static final String KEY_USER_IMAGE = "user_image";
    private static final String KEY_USER_MEMBERSHIP = "user_membership";

    private static SharedPrefManager mInstance;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    private SharedPrefManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SharedPrefManager(context);
        }
        return mInstance;
    }

    // Métodos para el ID de usuario
    public void saveUserId(String userId) {
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    // Métodos para el token
    public void saveToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    // Métodos para el email
    public void saveEmail(String email) {
        editor.putString(KEY_USER_EMAIL, email);
        editor.apply();
    }

    public String getEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, null);
    }

    // Métodos para membresía
    public void setUserMembership(String membership) {
        editor.putString(KEY_USER_MEMBERSHIP, membership);
        editor.apply();
    }

    public String getUserMembership() {
        return sharedPreferences.getString(KEY_USER_MEMBERSHIP, "free");
    }

    // Métodos para el perfil completo
    public void saveUserProfile(String id, String name, String lastName, String email,
                                String phone, String address, String identification, String image) {
        editor.putString(KEY_USER_ID, id);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_LASTNAME, lastName);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_PHONE, phone);
        editor.putString(KEY_USER_ADDRESS, address);
        editor.putString(KEY_USER_IDENTIFICATION, identification);
        editor.putString(KEY_USER_IMAGE, image);
        editor.apply();
    }

    // Limpiar datos al hacer logout
    public void clear() {
        editor.clear();
        editor.apply();
    }

    // Verificar si el usuario está logueado
    public boolean isLoggedIn() {
        return getToken() != null;
    }
}