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
        ImageView btnBack = findViewById(R.id.btnBack); // ← Agregamos el botón "atrás"

        btnPlus.setOnClickListener(v -> initiateCheckoutPro("plus"));
        btnPro.setOnClickListener(v -> initiateCheckoutPro("pro"));

        btnBack.setOnClickListener(v -> finish()); // ← Cierra la actividad actual
    }


    private void initiateCheckoutPro(String planType) {
        currentPlanType = planType;
        String userId = prefManager.getUserId();

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Error: No se pudo identificar al usuario", Toast.LENGTH_LONG).show();
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
                Toast.makeText(SubscriptionActivity.this,
                        getString(R.string.connection_error), Toast.LENGTH_LONG).show();
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    private void launchMercadoPagoCheckout(CheckoutProResponse response) {
        try {
            String checkoutUrl = response.getInitPoint();  // Usar sandbox solo en desarrollo
            Log.d(TAG, "Iniciando checkout con URL: " + checkoutUrl);

            if (checkoutUrl == null || checkoutUrl.isEmpty()) {
                Toast.makeText(this, getString(R.string.invalid_payment_url), Toast.LENGTH_LONG).show();
                return;
            }

            // Configurar Custom Tabs
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setShowTitle(true);
            builder.setUrlBarHidingEnabled(true);

            // Personalizar la barra de herramientas
            builder.setToolbarColor(getResources().getColor(R.color.colorPrimaryDark));

            CustomTabsIntent customTabsIntent = builder.build();

            // Asegurar que usa Chrome si está disponible
            String packageName = CustomTabsHelper.getPackageNameToUse(this);
            if (packageName != null) {
                customTabsIntent.intent.setPackage(packageName);
            }

            // Añadir flags para manejar mejor la navegación
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Lanzar el checkout
            customTabsIntent.launchUrl(this, Uri.parse(checkoutUrl));
        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar checkout", e);
            Toast.makeText(this,
                    getString(R.string.checkout_launch_error),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PAYMENT) {
            if (resultCode == RESULT_OK) {
                // El pago fue exitoso
                handlePaymentResult(true, data);
            } else {
                // El pago falló o fue cancelado
                handlePaymentResult(false, data);
            }
        }
    }

    private void handlePaymentResult(boolean success, Intent data) {
        if (success) {
            String paymentId = data != null ? data.getStringExtra("payment_id") : null;
            Toast.makeText(this,
                    getString(R.string.payment_success_message_short),
                    Toast.LENGTH_SHORT).show();

            // Podrías verificar el pago con tu backend aquí si es necesario
        } else {
            Toast.makeText(this,
                    getString(R.string.payment_cancelled_or_failed),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showLoading(String message) {
        dismissLoading(); // Asegurarse de que no hay diálogos previos
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
            String errorMsg;
            if (response.errorBody() != null) {
                errorMsg = response.errorBody().string();
            } else {
                errorMsg = getString(R.string.unknown_error, response.code());
            }

            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error en la respuesta: " + errorMsg);
        } catch (Exception e) {
            Toast.makeText(this,
                    getString(R.string.error_processing_response),
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error al procesar respuesta fallida", e);
        }
    }
}