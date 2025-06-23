package com.sena.qfinder.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class MedicamentoAlarmManager {
    private static final String TAG = "MedAlarmManager";

    public static void programarAlarmaExacta(Context context, int idAlarma,
                                             int idMedicamento, int idPaciente,
                                             String nombreMedicamento, String dosis,
                                             String frecuencia, long triggerAtMillis,
                                             long intervaloMillis) {

        // Validación robusta de parámetros
        if (nombreMedicamento == null || nombreMedicamento.isEmpty()) {
            nombreMedicamento = "Medicamento";
            Log.w(TAG, "Nombre de medicamento vacío, usando valor por defecto");
        }

        if (dosis == null) dosis = "";
        if (frecuencia == null) frecuencia = "1 hora";

        Log.d(TAG, String.format(
                "Programando alarma - ID: %d, Medicamento: %s, Hora: %s, Intervalo: %dms",
                idAlarma, nombreMedicamento, new java.util.Date(triggerAtMillis), intervaloMillis));

        // Verificar permisos para Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = ContextCompat.getSystemService(context, AlarmManager.class);
            if (am != null && !am.canScheduleExactAlarms()) {
                mostrarDialogoPermisos(context);
                return;
            }
        }

        // Crear intent para el receiver
        Intent intent = new Intent(context, MedicamentoAlarmReceiver.class)
                .setAction(MedicamentoAlarmReceiver.ACTION_MEDICAMENTO_ALARM)
                .putExtra("id_alarma", idAlarma)
                .putExtra("id_medicamento", idMedicamento)
                .putExtra("id_paciente", idPaciente)
                .putExtra("nombre_medicamento", nombreMedicamento)
                .putExtra("dosis", dosis)
                .putExtra("frecuencia", frecuencia)
                .putExtra("intervalo_millis", intervaloMillis);

        // Configurar PendingIntent
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                idAlarma,
                intent,
                flags
        );

        // Programar la alarma según versión de Android
        AlarmManager alarmManager = ContextCompat.getSystemService(context, AlarmManager.class);
        if (alarmManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                    );
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                    );
                } else {
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                    );
                }
                Log.d(TAG, "Alarma programada exitosamente");
            } catch (SecurityException e) {
                Log.e(TAG, "Error de seguridad al programar alarma", e);
                mostrarDialogoPermisos(context);
            }
        }
    }

    public static void cancelarAlarma(Context context, int idAlarma) {
        Log.d(TAG, "Cancelando alarma - ID: " + idAlarma);

        Intent intent = new Intent(context, MedicamentoAlarmReceiver.class)
                .setAction(MedicamentoAlarmReceiver.ACTION_MEDICAMENTO_ALARM);

        int flags = PendingIntent.FLAG_NO_CREATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                idAlarma,
                intent,
                flags
        );

        if (pendingIntent != null) {
            AlarmManager am = ContextCompat.getSystemService(context, AlarmManager.class);
            if (am != null) {
                am.cancel(pendingIntent);
            }
            pendingIntent.cancel();
            Log.d(TAG, "Alarma cancelada exitosamente");
        }
    }

    private static void mostrarDialogoPermisos(Context context) {
        new android.app.AlertDialog.Builder(context)
                .setTitle("Permiso necesario")
                .setMessage("La aplicación necesita permiso para programar alarmas exactas.")
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        context.startActivity(intent);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}