package com.sena.qfinder.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class ActivityAlarmReceiver {
    private static final String TAG = "ActivityAlarmReceiver";

    public static void programarAlarma(Context context, int actividadId, String titulo,
                                       String descripcion, String fecha, String hora,
                                       long timestamp) {

        // Validación crítica de datos
        if (titulo == null || fecha == null || hora == null) {
            Log.e(TAG, "Intento de programar alarma con datos nulos. ID: " + actividadId);
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class); // Cambio clave aquí
        intent.putExtra("actividad_id", actividadId);
        intent.putExtra("titulo", titulo);
        intent.putExtra("descripcion", descripcion);
        intent.putExtra("fecha", fecha);
        intent.putExtra("hora", hora);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                actividadId, // Usar ID como requestCode
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Programar con el método más preciso disponible
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timestamp,
                        pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent);
            }
            Log.d(TAG, "Alarma programada para ID: " + actividadId + " en: " + timestamp);
        } else {
            Log.e(TAG, "AlarmManager es nulo, no se puede programar alarma");
        }
    }

    public static void cancelarAlarma(Context context, int actividadId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                actividadId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Alarma cancelada para ID: " + actividadId);
        }
        pendingIntent.cancel();
    }
}