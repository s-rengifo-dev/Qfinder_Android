package com.sena.qfinder.ui.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.sena.qfinder.R;
import com.sena.qfinder.controller.MainActivityDash;
import com.sena.qfinder.utils.SharedPrefManager;

public class PaymentSuccessActivity extends AppCompatActivity {

    private static final String TAG = "PaymentSuccessActivity";
    private SharedPrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_success);

        prefManager = SharedPrefManager.getInstance(this);

        // Inicializar vistas
        ImageView ivSuccess = findViewById(R.id.iv_success_icon);
        TextView tvTitle = findViewById(R.id.tv_success_title);
        TextView tvMessage = findViewById(R.id.tv_success_message);
        Button btnContinue = findViewById(R.id.btn_continue);

        // Manejar el intent entrante
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        String userId = null;
        String planType = null;
        String paymentId = null;

        // 1. Verificar si viene de extras (flujo normal)
        if (intent.getExtras() != null) {
            userId = intent.getStringExtra("user_id");
            planType = intent.getStringExtra("plan_type");
            paymentId = intent.getStringExtra("payment_id");
            Log.d(TAG, "Datos recibidos por extras - userId: " + userId + ", planType: " + planType);
        }

        // 2. Verificar si es un deep link (URI)
        Uri data = intent.getData();
        if (data != null) {
            Log.d(TAG, "URI recibida: " + data.toString());

            // Verificar si es nuestro esquema qfinder://
            if ("qfinder".equals(data.getScheme())) {
                if ("payment".equals(data.getHost())) {
                    String path = data.getPath();
                    if (path != null) {
                        if (path.startsWith("/success")) {
                            userId = data.getQueryParameter("user_id");
                            planType = data.getQueryParameter("plan_type");
                            Log.d(TAG, "Datos recibidos por deep link - userId: " + userId + ", planType: " + planType);
                        }
                    }
                }
            }
        }

        // Validar que tenemos los datos necesarios
        if (userId == null || planType == null) {
            Log.e(TAG, "Faltan parámetros requeridos");
            showErrorAndFinish();
            return;
        }

        // Procesar el pago exitoso
        processSuccessfulPayment(userId, planType);
    }

    private void processSuccessfulPayment(String userId, String planType) {
        // Guardar la membresía en preferencias
        prefManager.setUserMembership(planType);

        // Actualizar la UI
        TextView tvTitle = findViewById(R.id.tv_success_title);
        TextView tvMessage = findViewById(R.id.tv_success_message);
        Button btnContinue = findViewById(R.id.btn_continue);

        tvTitle.setText(getString(R.string.payment_success_title));

        // Mensaje según el plan
        String message;
        switch (planType.toLowerCase()) {
            case "plus":
                message = getString(R.string.payment_success_plus_message);
                break;
            case "pro":
                message = getString(R.string.payment_success_pro_message);
                break;
            default:
                message = getString(R.string.payment_success_generic_message);
        }
        tvMessage.setText(message);

        // Configurar botón de continuar
        btnContinue.setOnClickListener(v -> navigateToMainActivity());

        Toast.makeText(this,
                getString(R.string.payment_success_toast, planType),
                Toast.LENGTH_LONG).show();
    }

    private void showErrorAndFinish() {
        Toast.makeText(this,
                getString(R.string.payment_error_processing),
                Toast.LENGTH_LONG).show();
        navigateToMainActivity();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivityDash.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}