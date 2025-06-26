package com.sena.qfinder.data.api;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.sena.qfinder.ui.home.Login;
import com.sena.qfinder.utils.SharedPrefManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private final Context context;
    private final SharedPrefManager sharedPrefManager;

    public AuthInterceptor(Context context) {
        this.context = context;
        this.sharedPrefManager = SharedPrefManager.getInstance(context);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        String token = sharedPrefManager.getToken();

        Request request = chain.request();
        if (token != null) {
            request = request.newBuilder()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();
        }

        Response response = chain.proceed(request);

        if (response.code() == 401 || response.code() == 403) {
            sharedPrefManager.clear(); // Borra token y datos de usuario

            Intent intent = new Intent(context, Login.class); // Asegúrate de cambiar esta clase si usas otra
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        }

        return response;
    }
}
