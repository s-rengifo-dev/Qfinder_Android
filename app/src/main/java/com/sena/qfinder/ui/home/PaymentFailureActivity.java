package com.sena.qfinder.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.sena.qfinder.R;
import com.sena.qfinder.controller.MainActivityDash;

public class PaymentFailureActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_failure);

        TextView tvTitle = findViewById(R.id.tv_failure_title);
        TextView tvMessage = findViewById(R.id.tv_failure_message);
        Button btnRetry = findViewById(R.id.btn_retry);
        Button btnCancel = findViewById(R.id.btn_cancel);

        tvTitle.setText(getString(R.string.payment_failure_title));
        tvMessage.setText(getString(R.string.payment_failure_message));

        btnRetry.setOnClickListener(v -> {
            finish(); // Vuelve a la actividad de suscripción
        });

        btnCancel.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivityDash.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}