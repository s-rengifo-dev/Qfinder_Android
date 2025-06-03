package com.sena.qfinder.utils;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.sena.qfinder.R;

public class ActivityAlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "actividades_channel";
    private static final String TAG = "ActivityAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarma recibida!");

        int actividadId = intent.getIntExtra("actividad_id", -1);
        String titulo = intent.getStringExtra("titulo");
        String descripcion = intent.getStringExtra("descripcion");
        String fecha = intent.getStringExtra("fecha");
        String hora = intent.getStringExtra("hora");

        mostrarNotificacion(context, actividadId, titulo, descripcion, fecha, hora);
        lanzarPantallaCompleta(context, actividadId, titulo, descripcion, fecha, hora);
    }

    private void mostrarNotificacion(Context context, int actividadId, String titulo,
                                     String descripcion, String fecha, String hora) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Crear canal de notificación (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios de Actividades",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones para actividades programadas");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{1000, 1000, 1000, 1000, 1000});
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }

        // Intent para la acción de posponer
        Intent snoozeIntent = new Intent(context, SnoozeReceiver.class);
        snoozeIntent.putExtra("actividad_id", actividadId);
        snoozeIntent.putExtra("titulo", titulo);
        snoozeIntent.putExtra("descripcion", descripcion);
        snoozeIntent.putExtra("fecha", fecha);
        snoozeIntent.putExtra("hora", hora);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                actividadId,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Sonido de alarma
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        // Construir la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_actividad_notify)
                .setContentTitle(titulo != null ? titulo : "¡Recordatorio de Actividad!")
                .setContentText(descripcion != null ? descripcion : "Tienes una actividad programada")
                .setSubText("Programado para: " + fecha + " a las " + hora)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(createFullScreenIntent(context, actividadId, titulo, descripcion, fecha, hora), true)
                .setSound(alarmSound)
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                .setLights(Color.RED, 1000, 1000)
                .addAction(R.drawable.ic_snooze, "Posponer 10 min", snoozePendingIntent);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_INSISTENT;

        notificationManager.notify(actividadId, notification);
        Log.d(TAG, "Notificación mostrada para actividad ID: " + actividadId);
    }

    private PendingIntent createFullScreenIntent(Context context, int actividadId, String titulo,
                                                 String descripcion, String fecha, String hora) {
        Intent fullScreenIntent = new Intent(context, AlarmFullScreenActivity.class);
        fullScreenIntent.putExtra("actividad_id", actividadId);
        fullScreenIntent.putExtra("titulo", titulo);
        fullScreenIntent.putExtra("descripcion", descripcion);
        fullScreenIntent.putExtra("fecha", fecha);
        fullScreenIntent.putExtra("hora", hora);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        return PendingIntent.getActivity(
                context,
                actividadId,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void lanzarPantallaCompleta(Context context, int actividadId, String titulo,
                                        String descripcion, String fecha, String hora) {
        Intent fullScreenIntent = new Intent(context, AlarmFullScreenActivity.class);
        fullScreenIntent.putExtra("actividad_id", actividadId);
        fullScreenIntent.putExtra("titulo", titulo);
        fullScreenIntent.putExtra("descripcion", descripcion);
        fullScreenIntent.putExtra("fecha", fecha);
        fullScreenIntent.putExtra("hora", hora);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        context.startActivity(fullScreenIntent);
    }

    // Método para reprogramar alarmas (usado por SnoozeReceiver)
    public static void programarAlarma(Context context, int actividadId, String titulo,
                                       String descripcion, String fecha, String hora, long triggerAtMillis) {
        Intent intent = new Intent(context, ActivityAlarmReceiver.class);
        intent.putExtra("actividad_id", actividadId);
        intent.putExtra("titulo", titulo);
        intent.putExtra("descripcion", descripcion);
        intent.putExtra("fecha", fecha);
        intent.putExtra("hora", hora);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                actividadId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
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
        }
        Log.d(TAG, "Alarma reprogramada para: " + triggerAtMillis);
    }
}