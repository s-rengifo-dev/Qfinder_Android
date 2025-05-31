package com.sena.qfinder.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.JsonObject;
import com.sena.qfinder.data.api.AuthService;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FCMTokenManager {

    public interface TokenUpdateListener {
        void onTokenReceived(String token);
        void onError(Exception e);
    }

    public static void getNewToken(Context context, TokenUpdateListener listener) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        Log.e("FCMTokenManager", "Token refresh failed", e);
                        if (listener != null) {
                            listener.onError(e);
                        }
                        return;
                    }

                    String token = task.getResult();
                    Log.d("FCMTokenManager", "New FCM token: " + token);
                    if (listener != null) {
                        listener.onTokenReceived(token);
                    }

                    sendTokenToServer(context, token);
                });
    }

    private static void sendTokenToServer(Context context, String token) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("fcmToken", token);

        SharedPreferences sharedPreferences = context.getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String authToken = sharedPreferences.getString("token", null);

        if (authToken == null || authToken.isEmpty()) {
            Log.w("FCMTokenManager", "Token de autenticación no disponible");
            return;
        }

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        AuthService authService = retrofit.create(AuthService.class);

        Call<ResponseBody> call = authService.registerFcmToken("Bearer " + authToken, requestBody);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("FCMTokenManager", "Token FCM registrado exitosamente");
                } else {
                    Log.e("FCMTokenManager", "Error al registrar token FCM: " + response.code());
                    // Reintentar después de 30 segundos
                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                            sendTokenToServer(context, token), 30000);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("FCMTokenManager", "Fallo de red al registrar token FCM", t);
                // Reintentar después de 30 segundos
                new Handler(Looper.getMainLooper()).postDelayed(() ->
                        sendTokenToServer(context, token), 30000);
            }
        });
    }
}