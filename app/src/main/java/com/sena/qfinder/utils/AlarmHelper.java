package com.sena.qfinder.utils;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AlarmHelper {

    public static void setActivityAlarm(Context context, int actividadId, String titulo, String descripcion, String fecha, String hora) {
        try {
            // Parsear fecha y hora
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date date = sdf.parse(fecha + " " + hora);

            if (date == null) {
                Log.e("AlarmHelper", "Fecha/hora inválida");
                return;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            // Configurar el intent para el BroadcastReceiver
            Intent intent = new Intent(context, ActivityAlarmReceiver.class);
            intent.putExtra("actividad_id", actividadId);
            intent.putExtra("titulo", titulo);
            intent.putExtra("descripcion", descripcion);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    actividadId, // ID único para cada alarma
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Programar la alarma
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                }

                Log.d("AlarmHelper", "Alarma programada para: " + calendar.getTime());
            }
        } catch (ParseException e) {
            Log.e("AlarmHelper", "Error al parsear fecha/hora", e);
        }
    }

    public static void cancelActivityAlarm(Context context, int actividadId) {
        Intent intent = new Intent(context, ActivityAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                actividadId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d("AlarmHelper", "Alarma cancelada para actividad ID: " + actividadId);
        }
    }
}