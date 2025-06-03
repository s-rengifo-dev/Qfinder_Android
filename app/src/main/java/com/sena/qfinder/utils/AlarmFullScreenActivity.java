package com.sena.qfinder.utils;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sena.qfinder.R;

public class AlarmFullScreenActivity extends AppCompatActivity {

    private int actividadId;
    private String titulo, descripcion, fecha, hora;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_fullscreen);

        // Obtener datos de la alarma
        actividadId = getIntent().getIntExtra("actividad_id", -1);
        titulo = getIntent().getStringExtra("titulo");
        descripcion = getIntent().getStringExtra("descripcion");
        fecha = getIntent().getStringExtra("fecha");
        hora = getIntent().getStringExtra("hora");

        // Configurar para mostrar sobre bloqueo de pantalla
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Inicializar UI
        TextView tvTitulo = findViewById(R.id.tvTitulo);
        TextView tvDetalles = findViewById(R.id.tvDetalles);
        TextView tvHorario = findViewById(R.id.tvHorario);
        Button btnDismiss = findViewById(R.id.btnDismiss);
        Button btnSnooze = findViewById(R.id.btnSnooze);

        // Mostrar información de la alarma
        tvTitulo.setText(titulo != null ? titulo : "¡Alarma!");
        tvDetalles.setText(descripcion != null ? descripcion : "Tienes una actividad programada");
        tvHorario.setText(String.format("Programado para: %s a las %s", fecha, hora));

        // Configurar botones
        btnDismiss.setOnClickListener(v -> {
            stopAlarmSound();
            finish();
        });

        btnSnooze.setOnClickListener(v -> {
            // Posponer 10 minutos
            SnoozeReceiver.snoozeAlarm(this, actividadId, titulo, descripcion, fecha, hora);
            stopAlarmSound();
            finish();
        });
    }

    private void stopAlarmSound() {
        // Detener servicio de sonido
        Intent serviceIntent = new Intent(this, AlarmSoundService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarmSound();

        // Cancelar notificación asociada
        if (actividadId != -1) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(actividadId);
            }
        }
    }
}