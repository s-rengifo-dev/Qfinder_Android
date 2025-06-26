package com.sena.qfinder.ui.home;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.google.gson.Gson;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.ApiClient;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.CheckoutProRequest;
import com.sena.qfinder.data.models.CheckoutProResponse;
import com.sena.qfinder.utils.CustomTabsHelper;
import com.sena.qfinder.utils.SharedPrefManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubscriptionActivity extends AppCompatActivity {
    private static final String TAG = "SubscriptionActivity";
    private static final int REQUEST_CODE_PAYMENT = 1001;

    private AuthService authService;
    private SharedPrefManager prefManager;
    private ProgressDialog progressDialog;
    private String currentPlanType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        prefManager = SharedPrefManager.getInstance(this);
        authService = ApiClient.getClient().create(AuthService.class);

        Button btnPlus = findViewById(R.id.btn_plus_plan);
        Button btnPro = findViewById(R.id.btn_pro_plan);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnPlus.setOnClickListener(v -> initiateCheckoutPro("plus"));
        btnPro.setOnClickListener(v -> initiateCheckoutPro("pro"));

        btnBack.setOnClickListener(v -> finish());
    }

    private void initiateCheckoutPro(String planType) {
        currentPlanType = planType;
        String userId = prefManager.getUserId();

        if (userId == null || userId.isEmpty()) {
            showUserError(R.string.user_identification_error);
            return;
        }

        showLoading(getString(R.string.preparing_payment));

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
                    launchMercadoPagoCheckout(response.body());
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<CheckoutProResponse> call, Throwable t) {
                dismissLoading();
                showUserError(R.string.connectionerror);
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    private void launchMercadoPagoCheckout(CheckoutProResponse response) {
        try {
            String checkoutUrl = response.getInitPoint();
            Log.d(TAG, "Iniciando checkout con URL: " + checkoutUrl);

            if (checkoutUrl == null || checkoutUrl.isEmpty()) {
                showUserError(R.string.payment_service_error);
                return;
            }

            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setShowTitle(true);
            builder.setUrlBarHidingEnabled(true);
            builder.setToolbarColor(getResources().getColor(R.color.colorPrimaryDark));

            CustomTabsIntent customTabsIntent = builder.build();

            String packageName = CustomTabsHelper.getPackageNameToUse(this);
            if (packageName != null) {
                customTabsIntent.intent.setPackage(packageName);
            }

            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            customTabsIntent.launchUrl(this, Uri.parse(checkoutUrl));
        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar checkout", e);
            showUserError(R.string.checkout_process_error);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PAYMENT) {
            if (resultCode == RESULT_OK) {
                handlePaymentResult(true, data);
            } else {
                handlePaymentResult(false, data);
            }
        }
    }

    private void handlePaymentResult(boolean success, Intent data) {
        if (success) {
            Toast.makeText(this,
                    getString(R.string.payment_success_message_short),
                    Toast.LENGTH_SHORT).show();
        } else {
            showUserError(R.string.payment_cancelled_error);
        }
    }

    private void showLoading(String message) {
        dismissLoading();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void dismissLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void handleErrorResponse(Response<CheckoutProResponse> response) {
        try {
            // Registramos el error completo para debugging
            if (response.errorBody() != null) {
                Log.e(TAG, "Error response: " + response.errorBody().string());
            } else {
                Log.e(TAG, "Error with code: " + response.code());
            }

            // Mostramos mensaje genérico al usuario
            showUserError(R.string.service_temporarily_unavailable);
        } catch (Exception e) {
            showUserError(R.string.unexpected_error);
            Log.e(TAG, "Error al procesar respuesta fallida", e);
        }
    }

    private void showUserError(int stringResId) {
        Toast.makeText(this, getString(stringResId), Toast.LENGTH_LONG).show();
    }
}