package com.sena.qfinder.utils;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE,
                "AlarmReceiver::AlarmWakeLock"
        );
        wakeLock.acquire(10 * 60 * 1000L /*10 minutos*/);

        try {
            int actividadId = intent.getIntExtra("actividad_id", -1);
            String titulo = intent.getStringExtra("titulo");
            String descripcion = intent.getStringExtra("descripcion");
            String fecha = intent.getStringExtra("fecha");
            String hora = intent.getStringExtra("hora");
            boolean esRecurrente = intent.getBooleanExtra("es_recurrente", false);
            long intervaloMillis = intent.getLongExtra("intervalo_millis", 0);

            Log.d(TAG, "Alarma recibida para ID: " + actividadId +
                    (esRecurrente ? " (Recurrente)" : ""));

            // Validar datos
            if (titulo == null || fecha == null || hora == null) {
                Log.e(TAG, "Datos incompletos en alarma recibida");
                return;
            }

            // Iniciar servicio de sonido
            Intent soundIntent = new Intent(context, AlarmSoundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(soundIntent);
            } else {
                context.startService(soundIntent);
            }

            // Lanzar actividad de pantalla completa
            Intent fullScreenIntent = new Intent(context, AlarmFullScreenActivity.class);
            fullScreenIntent.putExtras(intent.getExtras());
            fullScreenIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                            Intent.FLAG_ACTIVITY_NO_HISTORY
            );

            // Para Android 10+ usamos PendingIntent
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                        context,
                        actividadId,
                        fullScreenIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                try {
                    fullScreenPendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "Error al lanzar actividad", e);
                }
            } else {
                context.startActivity(fullScreenIntent);
            }

            // Si es recurrente, programar la próxima alarma
            if (esRecurrente && intervaloMillis > 0) {
                long nextTriggerTime = System.currentTimeMillis() + intervaloMillis;

                // Actualizar la fecha para mostrar en la próxima alarma
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar nextCalendar = Calendar.getInstance();
                nextCalendar.setTimeInMillis(nextTriggerTime);
                String nextDate = sdf.format(nextCalendar.getTime());

                // Programar próxima alarma
                ActivityAlarmReceiver.programarAlarma(
                        context,
                        actividadId,
                        titulo,
                        descripcion,
                        nextDate,
                        hora,
                        nextTriggerTime
                );
            }

        } finally {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }
}