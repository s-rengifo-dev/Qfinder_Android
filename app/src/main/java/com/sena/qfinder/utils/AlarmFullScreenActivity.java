package com.sena.qfinder.utils;

import android.app.NotificationManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sena.qfinder.R;

public class AlarmFullScreenActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_fullscreen);

        // Configurar para mostrar sobre bloqueo de pantalla
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int actividadId = getIntent().getIntExtra("actividad_id", -1);
        String titulo = getIntent().getStringExtra("titulo");
        String descripcion = getIntent().getStringExtra("descripcion");
        String fecha = getIntent().getStringExtra("fecha");
        String hora = getIntent().getStringExtra("hora");

        TextView tvTitulo = findViewById(R.id.tvAlarmTitle);
        TextView tvDetalle = findViewById(R.id.tvAlarmDetail);
        Button btnDismiss = findViewById(R.id.btnDismiss);
        Button btnSnooze = findViewById(R.id.btnSnooze);

        tvTitulo.setText(titulo != null ? titulo : "¡RECORDATORIO DE ACTIVIDAD!");
        tvDetalle.setText((descripcion != null ? descripcion + "\n\n" : "")
                + "Programado para: " + fecha + " a las " + hora);

        // Reproducir sonido de alarma
        try {
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            mediaPlayer = MediaPlayer.create(this, alarmSound);

            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        btnDismiss.setOnClickListener(v -> finish());

        btnSnooze.setOnClickListener(v -> {
            // Posponer 10 minutos
            SnoozeReceiver.snoozeAlarm(this, actividadId, titulo, descripcion, fecha, hora);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        // Cancelar notificación asociada
        int actividadId = getIntent().getIntExtra("actividad_id", -1);
        if (actividadId != -1) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(actividadId);
            }
        }
    }
}