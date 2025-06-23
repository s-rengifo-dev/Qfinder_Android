package com.sena.qfinder.utils;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sena.qfinder.R;
import com.sena.qfinder.database.DatabaseMedicamentoHelper;

public class MedicamentoAlarmActivity extends AppCompatActivity {
    private static final String TAG = "MedAlarmActivity";
    private Ringtone ringtone;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Creando actividad de alarma");

        // Configurar para mostrar sobre pantalla bloqueada
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        setContentView(R.layout.activity_medicamento_alarm);

        // Obtener datos del intent
        Intent intent = getIntent();
        if (intent == null || !MedicamentoAlarmReceiver.ACTION_MEDICAMENTO_ALARM.equals(intent.getAction())) {
            Log.e(TAG, "Intent no válido");
            finish();
            return;
        }

        Bundle extras = intent.getExtras();
        if (extras == null) {
            Log.e(TAG, "Sin extras en el intent");
            finish();
            return;
        }

        // Configurar UI
        TextView tvDetalles = findViewById(R.id.tvDetalles);
        TextView tvDosis = findViewById(R.id.tvDosis);
        TextView tvFrecuencia = findViewById(R.id.tvFrecuencia);
        Button btnTomado = findViewById(R.id.btnTomado);
        Button btnPosponer = findViewById(R.id.btnPosponer);

        String nombreMedicamento = extras.getString("nombre_medicamento", "Medicamento");
        String dosis = extras.getString("dosis", "No especificada");
        String frecuencia = extras.getString("frecuencia", "No especificada");
        final int idAlarma = extras.getInt("id_alarma", -1);
        final long intervalo = extras.getLong("intervalo_millis", 0);

        tvDetalles.setText(nombreMedicamento);
        tvDosis.setText("Dosis: " + dosis);
        tvFrecuencia.setText("Frecuencia: " + frecuencia);

        // Reproducir sonido de alarma
        playAlarmSound();

        btnTomado.setOnClickListener(v -> {
            Log.d(TAG, "Medicamento marcado como tomado");
            stopAlarmSound();

            // Registrar en la base de datos
            DatabaseMedicamentoHelper dbHelper = DatabaseMedicamentoHelper.getInstance(this);
            dbHelper.registrarTomaMedicamento(idAlarma, System.currentTimeMillis());

            finish();
        });

        btnPosponer.setOnClickListener(v -> {
            Log.d(TAG, "Alarma pospuesta");
            stopAlarmSound();

            if (intervalo > 0) {
                long nextTrigger = System.currentTimeMillis() + (10 * 60 * 1000); // 10 minutos

                // Reprogramar alarma
                MedicamentoAlarmManager.programarAlarmaExacta(
                        this,
                        idAlarma,
                        extras.getInt("id_medicamento", -1),
                        extras.getInt("id_paciente", -1),
                        nombreMedicamento,
                        dosis,
                        frecuencia,
                        nextTrigger,
                        intervalo
                );
            }

            finish();
        });
    }

    private void playAlarmSound() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            ringtone = RingtoneManager.getRingtone(this, alarmUri);
            if (ringtone != null) {
                ringtone.play();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al reproducir sonido", e);
        }
    }

    private void stopAlarmSound() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarmSound();
    }
}