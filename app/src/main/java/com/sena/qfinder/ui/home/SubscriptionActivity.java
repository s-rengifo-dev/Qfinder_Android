package com.sena.qfinder.ui.home;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.datatransport.backend.cct.BuildConfig;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.ApiClient;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.CheckoutProRequest;
import com.sena.qfinder.data.models.CheckoutProResponse;
import com.sena.qfinder.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubscriptionActivity extends AppCompatActivity {
    private static final String TAG = "SubscriptionActivity";
    private static final int MERCADOPAGO_REQUEST_CODE = 1001;
    private AuthService authService;
    private SharedPrefManager prefManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        prefManager = SharedPrefManager.getInstance(this);
        authService = ApiClient.getClient().create(AuthService.class);

        Button btnPlus = findViewById(R.id.btn_plus_plan);
        Button btnPro = findViewById(R.id.btn_pro_plan);

        btnPlus.setOnClickListener(v -> initiateCheckoutPro("plus"));
        btnPro.setOnClickListener(v -> initiateCheckoutPro("pro"));
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
                    launchCheckoutPro(response.body(), planType);
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

    private void launchCheckoutPro(CheckoutProResponse response, String planType) {
        try {
            String checkoutUrl = BuildConfig.DEBUG ?
                    response.getSandboxInitPoint() :
                    response.getInitPoint();

            if (checkoutUrl == null || checkoutUrl.isEmpty()) {
                Toast.makeText(this, "URL de pago no disponible", Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent = new Intent(this, MercadoPagoCheckoutActivity.class);
            intent.putExtra("checkout_url", checkoutUrl);
            intent.putExtra("plan_type", planType);
            startActivityForResult(intent, MERCADOPAGO_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Error al iniciar pago: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error launching Checkout Pro", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MERCADOPAGO_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                String paymentId = data != null ? data.getStringExtra("payment_id") : "";
                String planType = data != null ? data.getStringExtra("plan_type") : "";
                navigateToPaymentResult(true, paymentId, planType);
            } else {
                navigateToPaymentResult(false, "", "");
            }
        }
    }

    private void navigateToPaymentResult(boolean success, String paymentId, String planType) {
        Intent intent = new Intent(this,
                success ? PaymentSuccessActivity.class : PaymentFailureActivity.class);

        if (success) {
            intent.putExtra("payment_id", paymentId);
            intent.putExtra("plan_type", planType);
        }

        startActivity(intent);
        finish();
    }

    private void showLoading() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Preparando pago...");
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
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
            String errorMsg = !errorBody.isEmpty() ?
                    errorBody : "Error desconocido (Código: " + response.code() + ")";

            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
        }
    }
}