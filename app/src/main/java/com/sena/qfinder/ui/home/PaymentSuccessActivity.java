package com.sena.qfinder.ui.home;

import android.content.Intent;
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

        // Initialize views
        ImageView ivSuccess = findViewById(R.id.iv_success_icon);
        TextView tvTitle = findViewById(R.id.tv_success_title);
        TextView tvMessage = findViewById(R.id.tv_success_message);
        Button btnContinue = findViewById(R.id.btn_continue);

        // Get plan type from intent
        String planType = getIntent().getStringExtra("plan_type");
        String paymentId = getIntent().getStringExtra("payment_id");
        double amount = getIntent().getDoubleExtra("amount", 0.0);

        // Update user membership
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        if (planType != null) {
            prefManager.setUserMembership(planType);
        }

        // Set appropriate message based on plan type
        String message;
        if (planType != null) {
            switch (planType.toLowerCase()) {
                case "plus":
                    message = getString(R.string.payment_success_plus_message);
                    break;
                case "pro":
                    message = getString(R.string.payment_success_pro_message);
                    break;
                case "premium":
                    message = getString(R.string.payment_success_premium_message);
                    break;
                default:
                    message = getString(R.string.payment_success_generic_message);
            }
        } else {
            message = getString(R.string.payment_success_generic_message);
        }

        tvTitle.setText(getString(R.string.payment_success_title));
        tvMessage.setText(message);

        // Configure continue button
        btnContinue.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivityDash.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed(); // Call super method first
        navigateToMainActivity();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivityDash.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}