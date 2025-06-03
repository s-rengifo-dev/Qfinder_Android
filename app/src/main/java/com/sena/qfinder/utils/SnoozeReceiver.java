package com.sena.qfinder.utils;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

public class SnoozeReceiver extends BroadcastReceiver {
    private static final String TAG = "SnoozeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        int actividadId = intent.getIntExtra("actividad_id", -1);
        String titulo = intent.getStringExtra("titulo");
        String descripcion = intent.getStringExtra("descripcion");
        String fecha = intent.getStringExtra("fecha");
        String hora = intent.getStringExtra("hora");

        snoozeAlarm(context, actividadId, titulo, descripcion, fecha, hora);
    }

    public static void snoozeAlarm(Context context, int actividadId, String titulo,
                                   String descripcion, String fecha, String hora) {
        // Reprogramar para 10 minutos después
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 10);

        // Cancelar notificación actual
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(actividadId);
        }

        // Reprogramar alarma
        ActivityAlarmReceiver.programarAlarma(
                context,
                actividadId,
                titulo,
                descripcion,
                fecha,
                hora,
                calendar.getTimeInMillis()
        );

        Log.d(TAG, "Alarma pospuesta 10 minutos para actividad ID: " + actividadId);
    }
}