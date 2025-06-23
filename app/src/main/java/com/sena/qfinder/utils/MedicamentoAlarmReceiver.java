package com.sena.qfinder.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public class MedicamentoAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "MedAlarmReceiver";
    public static final String ACTION_MEDICAMENTO_ALARM = "com.sena.qfinder.ACTION_MEDICAMENTO_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarma recibida - Action: " + (intent != null ? intent.getAction() : "null"));

        if (intent == null || !ACTION_MEDICAMENTO_ALARM.equals(intent.getAction())) {
            Log.e(TAG, "Intent inválido o acción incorrecta");
            return;
        }

        Bundle extras = intent.getExtras();
        if (extras == null) {
            Log.e(TAG, "Intent sin extras");
            return;
        }

        // Obtener datos con valores por defecto
        String nombreMedicamento = extras.getString("nombre_medicamento", "Medicamento");
        int idAlarma = extras.getInt("id_alarma", -1);
        long intervalo = extras.getLong("intervalo_millis", 0);

        PowerManager.WakeLock wakeLock = null;
        try {
            // Adquirir wake lock
            PowerManager pm = ContextCompat.getSystemService(context, PowerManager.class);
            if (pm != null) {
                wakeLock = pm.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK |
                                PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        TAG + "::AlarmWakeLock"
                );
                wakeLock.acquire(10 * 60 * 1000L); // 10 minutos
            }

            // Mostrar actividad de alarma
            mostrarActividadAlarma(context, extras);

            // Reprogramar si es recurrente
            if (intervalo > 0) {
                long nextTrigger = System.currentTimeMillis() + intervalo;
                reprogramarAlarma(context, extras, nextTrigger, intervalo);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error al procesar alarma", e);
            Toast.makeText(context, "Error al procesar alarma", Toast.LENGTH_LONG).show();
        } finally {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private void mostrarActividadAlarma(Context context, Bundle extras) {
        Intent alarmIntent = new Intent(context, MedicamentoAlarmActivity.class)
                .setAction(ACTION_MEDICAMENTO_ALARM)
                .putExtras(extras)
                .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                );

        context.startActivity(alarmIntent);
    }

    private void reprogramarAlarma(Context context, Bundle extras, long nextTrigger, long intervalo) {
        MedicamentoAlarmManager.programarAlarmaExacta(
                context,
                extras.getInt("id_alarma"),
                extras.getInt("id_medicamento"),
                extras.getInt("id_paciente"),
                extras.getString("nombre_medicamento", "Medicamento"),
                extras.getString("dosis", ""),
                extras.getString("frecuencia", "1 hora"),
                nextTrigger,
                intervalo
        );
    }
}