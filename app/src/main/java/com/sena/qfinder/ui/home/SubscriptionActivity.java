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
import androidx.appcompat.app.AlertDialog;
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

    // Servicios
    private AuthService authService;
    private SharedPrefManager prefManager;
    private ProgressDialog progressDialog;

    // Vistas
    private FrameLayout planContainer;
    private View plusPlanView;
    private View proPlanView;
    private View planFreeView;
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
        planFreeView = LayoutInflater.from(this).inflate(R.layout.activity_subscription, planContainer, false);

        // Obtener referencias a los botones dentro de cada plan
        btnPlusPlan = plusPlanView.findViewById(R.id.btn_plus_plan);
        btnProPlan = proPlanView.findViewById(R.id.btn_pro_plan);
    }

    private void initServices() {
        prefManager = SharedPrefManager.getInstance(this);
        authService = ApiClient.getClient().create(AuthService.class);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        if (btnPlusPlan != null) {
            btnPlusPlan.setOnClickListener(v -> initiateCheckoutPro("plus"));
        }

        if (btnProPlan != null) {
            btnProPlan.setOnClickListener(v -> initiateCheckoutPro("pro"));
        }
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
                    String status = response.body().getStatus();
                    updatePlanView(membership, status);
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

    private void updatePlanView(String membership, String status) {
        planContainer.removeAllViews();

        if ("pro".equalsIgnoreCase(membership)) {
            planContainer.addView(proPlanView);
            if (btnProPlan != null) {
                boolean isActiveOrPending = "active".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status);
                btnProPlan.setEnabled(!isActiveOrPending);
                btnProPlan.setText(
                        "pending".equalsIgnoreCase(status) ?
                                getString(R.string.payment_pending) :
                                getString(R.string.current_plan)
                );
            }
            updateTitle("Plan Pro");

        } else if ("plus".equalsIgnoreCase(membership)) {
            planContainer.addView(plusPlanView);
            if (btnPlusPlan != null) {
                boolean isActiveOrPending = "active".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status);
                btnPlusPlan.setEnabled(!isActiveOrPending);
                btnPlusPlan.setText(
                        "pending".equalsIgnoreCase(status) ?
                                getString(R.string.payment_pending) :
                                getString(R.string.current_plan)
                );
            }
            updateTitle("Plan Plus");

        } else {
            // Mostrar vista con botones para seleccionar plan
            planContainer.addView(planFreeView);

            Button freePlusButton = planFreeView.findViewById(R.id.btn_plus_plan);
            Button freeProButton = planFreeView.findViewById(R.id.btn_pro_plan);

            if (freePlusButton != null) {
                freePlusButton.setOnClickListener(v -> initiateCheckoutPro("plus"));
            }

            if (freeProButton != null) {
                freeProButton.setOnClickListener(v -> initiateCheckoutPro("pro"));
            }

            updateTitle("Activa tu plan exclusivo");
        }
    }

    private void updateTitle(String title) {
        if (tvPlanTitle != null) {
            tvPlanTitle.setText(title);
        }
    }

    private void showDefaultPlan() {
        planContainer.removeAllViews();
        planContainer.addView(planFreeView);
        updateTitle("Selecciona tu plan");
    }

    private void initiateCheckoutPro(String planType) {
        String userId = prefManager.getUserId();
        String token = prefManager.getToken();

        if (userId == null || userId.isEmpty() || token == null || token.isEmpty()) {
            showUserError(R.string.user_identification_error);
            return;
        }

        showLoading(getString(R.string.preparing_payment));

        CheckoutProRequest request = new CheckoutProRequest(userId, planType);
        Call<CheckoutProResponse> call = authService.createCheckoutProPreference("Bearer " + token, request);

        call.enqueue(new Callback<CheckoutProResponse>() {
            @Override
            public void onResponse(Call<CheckoutProResponse> call, Response<CheckoutProResponse> response) {
                dismissLoading();

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    launchPaymentCheckout(response.body());
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        if (errorBody.contains("El usuario ya tiene una suscripción activa o pendiente")) {
                            showPendingSubscriptionDialog();
                        } else {
                            handlePaymentError(response);
                        }
                    } catch (Exception e) {
                        handlePaymentError(response);
                    }
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

            // CORRECCIÓN PRINCIPAL: Usar launchUrl en lugar de startActivityForResult
            customTabsIntent.launchUrl(this, Uri.parse(checkoutUrl));

            // Eliminado el manejo de onActivityResult ya que no es necesario con este enfoque
        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar checkout", e);
            showUserError(R.string.checkout_process_error);
        }
    }

    private void showPendingSubscriptionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.pending_subscription_title)
                .setMessage(R.string.pending_subscription_message)
                .setPositiveButton(R.string.understand, null)
                .setNeutralButton(R.string.contact_support, (dialog, which) -> {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse("mailto:soporte@tudominio.com"));
                    startActivity(emailIntent);
                })
                .show();
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