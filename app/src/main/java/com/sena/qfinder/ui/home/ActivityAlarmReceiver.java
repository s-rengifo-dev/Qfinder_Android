package com.sena.qfinder.ui.home;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.sena.qfinder.R;

public class ActivityAlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "ACTIVIDADES_CHANNEL";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        int actividadId = intent.getIntExtra("actividad_id", -1);
        String titulo = intent.getStringExtra("titulo");
        String descripcion = intent.getStringExtra("descripcion");

        if (actividadId == -1) return;

        crearCanalNotificacion(context);
        mostrarNotificacion(context, titulo, descripcion, actividadId);
    }

    private void crearCanalNotificacion(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Recordatorios de Actividades";
            String description = "Notificaciones para recordar actividades programadas";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void mostrarNotificacion(Context context, String titulo, String descripcion, int actividadId) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.campana) // Asegúrate de tener este icono
                .setContentTitle("Recordatorio: " + titulo)
                .setContentText(descripcion)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        if (notificationManager != null) {
            notificationManager.notify(actividadId, notification);
        }
    }
}