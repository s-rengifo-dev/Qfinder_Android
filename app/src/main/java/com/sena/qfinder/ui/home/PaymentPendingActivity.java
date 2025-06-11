package com.sena.qfinder.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.sena.qfinder.R;
import com.sena.qfinder.controller.MainActivityDash;

public class PaymentPendingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_pending);

        TextView tvTitle = findViewById(R.id.tv_pending_title);
        TextView tvMessage = findViewById(R.id.tv_pending_message);
        Button btnCheckStatus = findViewById(R.id.btn_check_status);
        Button btnGoHome = findViewById(R.id.btn_go_home);

        // Configurar los textos
        tvTitle.setText(getString(R.string.payment_pending_title));
        tvMessage.setText(getString(R.string.payment_pending_message));

        // Configurar los botones
        btnCheckStatus.setOnClickListener(v -> {
            // Aquí puedes implementar la lógica para verificar el estado del pago
            // Por ejemplo, volver a verificar con el servidor
            finish(); // O cualquier otra lógica que necesites
        });

        btnGoHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivityDash.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}