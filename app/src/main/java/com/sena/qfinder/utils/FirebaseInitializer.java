package com.sena.qfinder.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

public class FirebaseInitializer {
    private static final String TAG = "FirebaseInitializer";
    private static boolean initialized = false;

    public static synchronized void initialize(Context context) {
        if (initialized) {
            return;
        }

        try {
            // Verificar si Firebase ya está inicializado
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setApplicationId("1:943234700783:android:63a905964f25737428521a")
                        .setApiKey("AIzaSyDWsifL9DrQkUSqSmaVstQ7Cr9dhAPoPZg")
                        .setProjectId("qfinder-community")
                        .setDatabaseUrl("https://qfinder-comunity-default-rtdb.firebaseio.com/")
                        .build();

                FirebaseApp.initializeApp(context, options);
                Log.d(TAG, "Firebase initialized successfully");
            } else {
                Log.d(TAG, "Firebase already initialized");
            }

            // Obtener token FCM después de inicializar Firebase
            obtenerTokenFCM(context);

            initialized = true;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
        }
    }

    private static void obtenerTokenFCM(Context context) {
        FCMTokenManager.getNewToken(context, new FCMTokenManager.TokenUpdateListener() {
            @Override
            public void onTokenReceived(String token) {
                Log.d(TAG, "Token FCM obtenido: " + token);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error obteniendo token FCM", e);
            }
        });
    }
}