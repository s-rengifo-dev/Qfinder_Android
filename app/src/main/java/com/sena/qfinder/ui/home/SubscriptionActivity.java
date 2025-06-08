package com.sena.qfinder.ui.home;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.datatransport.backend.cct.BuildConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.CheckoutProRequest;
import com.sena.qfinder.data.models.CheckoutProResponse;
import com.sena.qfinder.utils.SharedPrefManager;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SubscriptionActivity extends AppCompatActivity {

    private static final String TAG = "SubscriptionActivity";
    private AuthService authService;
    private SharedPrefManager prefManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        prefManager = SharedPrefManager.getInstance(this);
        setupRetrofit();

        Button btnPlus = findViewById(R.id.btn_plus_plan);
        Button btnPro = findViewById(R.id.btn_pro_plan);

        btnPlus.setOnClickListener(v -> initiateCheckoutPro("plus"));
        btnPro.setOnClickListener(v -> initiateCheckoutPro("pro"));
    }

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        authService = retrofit.create(AuthService.class);
    }

    private void initiateCheckoutPro(String planType) {
        String userId = prefManager.getUserId();
        showLoading();

        CheckoutProRequest request = new CheckoutProRequest(userId, planType);
        Call<CheckoutProResponse> call = authService.createCheckoutProPreference(
                "Bearer " + prefManager.getToken(),
                request
        );

        call.enqueue(new Callback<CheckoutProResponse>() {
            @Override
            public void onResponse(Call<CheckoutProResponse> call, Response<CheckoutProResponse> response) {
                dismissLoading();

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    launchCheckoutPro(response.body());
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<CheckoutProResponse> call, Throwable t) {
                dismissLoading();
                Toast.makeText(SubscriptionActivity.this,
                        "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    private void launchCheckoutPro(CheckoutProResponse response) {
        try {
            // Usar sandbox en desarrollo, producción en release
            String checkoutUrl = BuildConfig.DEBUG ?
                    response.getSandboxInitPoint() :
                    response.getInitPoint();

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(checkoutUrl));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir MercadoPago", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error launching Checkout Pro", e);
        }
    }

    private void handleErrorResponse(Response<CheckoutProResponse> response) {
        try {
            String errorBody = response.errorBody().string();
            JsonObject errorJson = JsonParser.parseString(errorBody).getAsJsonObject();
            String errorMsg = errorJson.has("error") ?
                    errorJson.get("error").getAsString() :
                    "Error desconocido";

            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
        }
    }

    private void showLoading() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Preparando pago...");
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
    }

    private void dismissLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}