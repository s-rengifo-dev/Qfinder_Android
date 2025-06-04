package com.sena.qfinder.utils;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sena.qfinder.R;

public class AlarmFullScreenActivity extends AppCompatActivity {

    private int actividadId;
    private String titulo, descripcion, fecha, hora;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configurar para mostrar sobre pantalla bloqueada
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // Forzar pantalla completa en dispositivos con notch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        setContentView(R.layout.activity_alarm_fullscreen);

        // Obtener datos del intent
        Intent intent = getIntent();
        if (intent == null || intent.getExtras() == null) {
            finish();
            return;
        }

        actividadId = intent.getIntExtra("actividad_id", -1);
        titulo = intent.getStringExtra("titulo");
        descripcion = intent.getStringExtra("descripcion");
        fecha = intent.getStringExtra("fecha");
        hora = intent.getStringExtra("hora");

        if (actividadId == -1) {
            finish();
            return;
        }

        // Inicializar UI
        TextView tvTitulo = findViewById(R.id.tvTitulo);
        TextView tvDetalles = findViewById(R.id.tvDetalles);
        TextView tvHorario = findViewById(R.id.tvHorario);
        Button btnDismiss = findViewById(R.id.btnDismiss);
        Button btnSnooze = findViewById(R.id.btnSnooze);

        // Mostrar información
        tvTitulo.setText(titulo != null ? titulo : "¡Alarma!");
        tvDetalles.setText(descripcion != null ? descripcion : "Tienes una actividad programada");
        tvHorario.setText(fecha != null && hora != null ?
                String.format("Programado para: %s a las %s", fecha, hora) : "Horario no disponible");

        // Configurar botones
        btnDismiss.setOnClickListener(v -> {
            stopAlarmSound();
            finish();
        });

        btnSnooze.setOnClickListener(v -> {
            if (titulo != null && fecha != null && hora != null) {
                SnoozeReceiver.snoozeAlarm(this, actividadId, titulo, descripcion, fecha, hora);
            } else {
                Toast.makeText(this, "No se puede posponer la alarma", Toast.LENGTH_SHORT).show();
            }
            stopAlarmSound();
            finish();
        });
    }

    private void stopAlarmSound() {
        try {
            Intent serviceIntent = new Intent(this, AlarmSoundService.class);
            stopService(serviceIntent);

            // Cancelar notificación
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.cancel(actividadId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarmSound();
    }
}