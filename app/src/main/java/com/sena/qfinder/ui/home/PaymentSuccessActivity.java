package com.sena.qfinder.ui.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.sena.qfinder.R;
import com.sena.qfinder.controller.MainActivityDash;
import com.sena.qfinder.utils.SharedPrefManager;

public class PaymentSuccessActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_success);

        ImageView ivSuccess = findViewById(R.id.iv_success_icon);
        TextView tvTitle = findViewById(R.id.tv_success_title);
        TextView tvMessage = findViewById(R.id.tv_success_message);
        Button btnContinue = findViewById(R.id.btn_continue);

        // Obtener datos del intent o del deep link
        String planType = getIntent().getStringExtra("plan_type");
        String paymentId = getIntent().getStringExtra("payment_id");

        // Manejar deep link
        if (getIntent().getData() != null) {
            Uri data = getIntent().getData();
            if (data.getScheme().equals("qfinder") || data.getScheme().equals("https")) {
                planType = data.getQueryParameter("plan_type");
                paymentId = data.getQueryParameter("payment_id");
            }
        }

        // Actualizar membresía del usuario
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        if (planType != null) {
            prefManager.setUserMembership(planType);
        }

        // Configurar mensaje según el plan
        String message;
        if (planType != null) {
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
        } else {
            message = getString(R.string.payment_success_generic_message);
        }

        tvTitle.setText(getString(R.string.payment_success_title));
        tvMessage.setText(message);

        btnContinue.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivityDash.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        navigateToMainActivity();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivityDash.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}