package com.sena.qfinder.ui.home;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.sena.qfinder.R;
import com.sena.qfinder.data.api.ApiClient;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.CheckoutProRequest;
import com.sena.qfinder.data.models.CheckoutProResponse;
import com.sena.qfinder.data.models.MembresiaResponse;
import com.sena.qfinder.utils.CustomTabsHelper;
import com.sena.qfinder.utils.SharedPrefManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubscriptionActivity extends AppCompatActivity {
    private static final String TAG = "SubscriptionActivity";
    private static final int REQUEST_CODE_PAYMENT = 1001;

    // Servicios
    private AuthService authService;
    private SharedPrefManager prefManager;
    private ProgressDialog progressDialog;

    // Vistas
    private FrameLayout planContainer;
    private View plusPlanView;
    private View proPlanView;
    private Button btnPlusPlan;
    private Button btnProPlan;
    private ImageView btnBack;
    private TextView tvPlanTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription_base);

        // Inicializar vistas
        initViews();

        // Inicializar servicios
        initServices();

        // Configurar listeners
        setupListeners();

        // Cargar membresía del usuario
        loadUserMembership();
    }

    private void initViews() {
        planContainer = findViewById(R.id.plan_container);
        tvPlanTitle = findViewById(R.id.tvPlanTitle);
        btnBack = findViewById(R.id.btnBack);

        // Inflar los layouts de los planes
        plusPlanView = LayoutInflater.from(this).inflate(R.layout.fragment_plan_plus, planContainer, false);
        proPlanView = LayoutInflater.from(this).inflate(R.layout.fragment_plan_pro, planContainer, false);

        // Obtener referencias a los botones dentro de cada plan (asegúrate de que existan)
        btnPlusPlan = plusPlanView.findViewById(R.id.btn_plus_plan);
        btnProPlan = proPlanView.findViewById(R.id.btn_pro_plan);
    }

    private void initServices() {
        prefManager = SharedPrefManager.getInstance(this);
        authService = ApiClient.getClient().create(AuthService.class);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        if (btnPlusPlan != null)
            btnPlusPlan.setOnClickListener(v -> initiateCheckoutPro("plus"));

        if (btnProPlan != null)
            btnProPlan.setOnClickListener(v -> initiateCheckoutPro("pro"));
    }

    private void loadUserMembership() {
        String userId = prefManager.getUserId();
        String token = prefManager.getToken();

        if (userId == null || userId.isEmpty() || token == null || token.isEmpty()) {
            showUserError(R.string.user_identification_error);
            showDefaultPlan();
            return;
        }

        showLoading(getString(R.string.loading_user_data));

        Call<MembresiaResponse> call = authService.getUserMembership("Bearer " + token, userId);

        call.enqueue(new Callback<MembresiaResponse>() {
            @Override
            public void onResponse(Call<MembresiaResponse> call, Response<MembresiaResponse> response) {
                dismissLoading();

                if (response.isSuccessful() && response.body() != null) {
                    String membership = response.body().getMembresia();
                    updatePlanView(membership);
                } else {
                    handleMembershipError(response);
                    showDefaultPlan();
                }
            }

            @Override
            public void onFailure(Call<MembresiaResponse> call, Throwable t) {
                dismissLoading();
                Log.e(TAG, "Error al cargar membresía", t);
                showUserError(R.string.connection_error);
                showDefaultPlan();
            }
        });
    }

    private void updatePlanView(String membership) {
        planContainer.removeAllViews();

        if ("pro".equalsIgnoreCase(membership)) {
            planContainer.addView(proPlanView);
            btnProPlan.setEnabled(false);
            btnProPlan.setText(getString(R.string.current_plan));
            updateTitle("Plan Pro");
        } else {
            planContainer.addView(plusPlanView);
            if ("plus".equalsIgnoreCase(membership)) {
                btnPlusPlan.setEnabled(false);
                btnPlusPlan.setText(getString(R.string.current_plan));
                updateTitle("Plan Plus");
            } else {
                updateTitle("Selecciona tu plan");
            }
        }
    }

    private void updateTitle(String title) {
        if (tvPlanTitle != null) {
            tvPlanTitle.setText(title);
        }
    }

    private void showDefaultPlan() {
        planContainer.removeAllViews();
        planContainer.addView(plusPlanView);
        updateTitle("Selecciona tu plan");
    }

    private void initiateCheckoutPro(String planType) {
        String userId = prefManager.getUserId();

        if (userId == null || userId.isEmpty()) {
            showUserError(R.string.user_identification_error);
            return;
        }

        showLoading(getString(R.string.preparing_payment));

        CheckoutProRequest request = new CheckoutProRequest(userId, planType);
        Call<CheckoutProResponse> call = authService.createCheckoutProPreference("Bearer " + prefManager.getToken(), request);

        call.enqueue(new Callback<CheckoutProResponse>() {
            @Override
            public void onResponse(Call<CheckoutProResponse> call, Response<CheckoutProResponse> response) {
                dismissLoading();

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    launchPaymentCheckout(response.body());
                } else {
                    handlePaymentError(response);
                }
            }

            @Override
            public void onFailure(Call<CheckoutProResponse> call, Throwable t) {
                dismissLoading();
                Log.e(TAG, "Error en el pago", t);
                showUserError(R.string.connection_error);
            }
        });
    }

    private void launchPaymentCheckout(CheckoutProResponse response) {
        try {
            String checkoutUrl = response.getInitPoint();

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

    private void handleMembershipError(Response<MembresiaResponse> response) {
        try {
            Log.e(TAG, "Error en membresía: " + response.errorBody().string());
            showUserError(R.string.service_error);
        } catch (Exception e) {
            Log.e(TAG, "Error al procesar error de membresía", e);
        }
    }

    private void handlePaymentError(Response<CheckoutProResponse> response) {
        try {
            Log.e(TAG, "Error en pago: " + response.errorBody().string());
            showUserError(R.string.payment_error);
        } catch (Exception e) {
            Log.e(TAG, "Error al procesar error de pago", e);
        }
    }

    private void showLoading(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void dismissLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showUserError(int stringResId) {
        Toast.makeText(this, getString(stringResId), Toast.LENGTH_LONG).show();
    }
}
