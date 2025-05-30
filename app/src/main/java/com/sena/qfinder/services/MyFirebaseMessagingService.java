package com.sena.qfinder.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.JsonObject;
import com.sena.qfinder.utils.Constants;
import com.sena.qfinder.R;
import com.sena.qfinder.ChatComunidad;
import com.sena.qfinder.api.AuthService;
import com.sena.qfinder.controller.MainActivityDash;
import com.sena.qfinder.utils.AppLifecycleHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "QFinderFCMService";
    private static final String CHANNEL_ID = "qfinder_channel_01";
    private static final String GROUP_KEY = "com.sena.qfinder.NOTIFICATIONS";
    public static final String FCM_NOTIFICATION_RECEIVED = Constants.FCM_NOTIFICATION_RECEIVED;

    // Tipos de notificaciones
    private static final String TYPE_CHAT = "chat";
    private static final String TYPE_MEDICATION = "medication";
    private static final String TYPE_APPOINTMENT = "appointment";
    private static final String TYPE_ACTIVITY = "activity";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM token: " + token);

        // Guardar el token en SharedPreferences
        getSharedPreferences("_", MODE_PRIVATE).edit().putString("fcm_token", token).apply();

        // Enviar el nuevo token al servidor
        sendTokenToServer(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Verificar si la aplicación está en primer plano
        boolean isForeground = AppLifecycleHandler.isApplicationInForeground();

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            Map<String, String> data = remoteMessage.getData();

            if (isForeground) {
                // Manejar notificación en primer plano
                handleForegroundNotification(data);
            } else {
                // Mostrar notificación en segundo plano
                handleBackgroundNotification(data);
            }
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            // Mostrar notificación básica si no hay datos
            showBasicNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    remoteMessage.getData()
            );
        }
    }

    private void handleForegroundNotification(Map<String, String> data) {
        String type = data.get("type");

        // Enviar broadcast para actualizar la UI
        Intent intent = new Intent(FCM_NOTIFICATION_RECEIVED);
        intent.putExtra("type", type);

        // Agregar todos los datos extras necesarios
        for (Map.Entry<String, String> entry : data.entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        // Mostrar notificación solo si es importante
        if (!TYPE_CHAT.equals(type)) {
            handleBackgroundNotification(data);
        }
    }

    private void handleBackgroundNotification(Map<String, String> data) {
        String type = data.get("type");
        String title = data.get("title");
        String message = data.get("message");

        if (title == null) title = getString(R.string.app_name);
        if (message == null) message = "Nueva notificación";

        if (type == null) {
            showBasicNotification(title, message, data);
            return;
        }

        switch (type) {
            case TYPE_CHAT:
                showChatNotification(data);
                break;
            case TYPE_MEDICATION:
                showMedicationNotification(data);
                break;
            case TYPE_APPOINTMENT:
                showAppointmentNotification(data);
                break;
            case TYPE_ACTIVITY:
                showActivityNotification(data);
                break;
            default:
                showBasicNotification(title, message, data);
                break;
        }
    }

    private void showChatNotification(Map<String, String> data) {
        String comunidadId = data.get("comunidadId");
        String mensajeId = data.get("mensajeId");
        String senderName = data.get("senderName");
        String message = data.get("message");
        String imageUrl = data.get("imageUrl");

        Intent intent = new Intent(this, ChatComunidad.class);
        intent.putExtra("id_red", comunidadId);
        intent.putExtra("nombre_comunidad", data.get("comunidadNombre"));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                generateRequestCode(comunidadId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat_notification)
                .setContentTitle(senderName != null ? senderName : "Nuevo mensaje")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup(GROUP_KEY)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Bitmap bitmap = getBitmapFromUrl(imageUrl);
                NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .setSummaryText(message);

                // Solo establecer bigLargeIcon si tenemos una miniatura
                Bitmap thumbnail = createThumbnail(bitmap);
                if (thumbnail != null) {
                    style.bigLargeIcon(thumbnail);
                }

                builder.setLargeIcon(thumbnail)
                        .setStyle(style);
            } catch (Exception e) {
                Log.e(TAG, "Error loading image for notification", e);
            }
        }

        showNotification(builder, generateNotificationId(comunidadId));
    }

    private Bitmap createThumbnail(Bitmap original) {
        if (original == null) return null;
        try {
            return Bitmap.createScaledBitmap(original, 64, 64, true);
        } catch (Exception e) {
            Log.e(TAG, "Error creating thumbnail", e);
            return null;
        }
    }

    private void showMedicationNotification(Map<String, String> data) {
        String medicamentoId = data.get("medicamentoId");
        String medicamentoNombre = data.get("medicamentoNombre");
        String hora = data.get("hora");
        String dosis = data.get("dosis");

        Intent intent = new Intent(this, MainActivityDash.class);
        intent.putExtra("fragmentToOpen", "medication");
        intent.putExtra("medicamentoId", medicamentoId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                generateRequestCode(medicamentoId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Acción para marcar como tomado
        Intent takenIntent = new Intent(this, NotificationActionReceiver.class);
        takenIntent.setAction("MARK_MEDICATION_TAKEN");
        takenIntent.putExtra("medicamentoId", medicamentoId);
        PendingIntent takenPendingIntent = PendingIntent.getBroadcast(
                this,
                generateRequestCode(medicamentoId + "_action"),
                takenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_medication_notification)
                .setContentTitle("Recordatorio de medicamento")
                .setContentText("Es hora de tomar " + medicamentoNombre)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup(GROUP_KEY)
                .addAction(R.drawable.ic_check, "Marcar como tomado", takenPendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(String.format("Medicamento: %s\nDosis: %s\nHora: %s",
                                medicamentoNombre, dosis, hora)));

        showNotification(builder, generateNotificationId(medicamentoId));
    }

    private void showAppointmentNotification(Map<String, String> data) {
        // Implementación similar para citas médicas
    }

    private void showActivityNotification(Map<String, String> data) {
        // Implementación similar para actividades
    }

    private void showBasicNotification(String title, String message, Map<String, String> data) {
        Intent intent = new Intent(this, MainActivityDash.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                generateRequestCode("basic"),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup(GROUP_KEY);

        showNotification(builder, generateNotificationId("basic"));
    }

    private void showNotification(NotificationCompat.Builder builder, int notificationId) {
        createNotificationChannel();

        // Añadir configuración común
        builder.setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Error showing notification", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "QFinder Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for QFinder app notifications");
            channel.enableLights(true);
            channel.setLightColor(Color.parseColor("#FF6200EE")); // Usar color directamente
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 100});

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Bitmap getBitmapFromUrl(String imageUrl) throws Exception {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.connect();
        InputStream input = connection.getInputStream();
        return BitmapFactory.decodeStream(input);
    }

    private int generateNotificationId(String seed) {
        return seed != null ? seed.hashCode() : new Random().nextInt();
    }

    private int generateRequestCode(String seed) {
        return generateNotificationId(seed);
    }

    private void sendTokenToServer(String token) {
        // Obtener SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String authToken = sharedPreferences.getString("token", null);

        if (authToken == null || authToken.isEmpty()) {
            Log.w(TAG, "No se encontró token de autorización. No se puede enviar token FCM al servidor.");
            return;
        }

        // Asegurar que el token de autorización tiene el formato correcto
        String fullAuthToken = "Bearer " + authToken;

        // Crear instancia de Retrofit
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Crear servicio
        AuthService authService = retrofit.create(AuthService.class);

        // Crear cuerpo de la solicitud
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("fcmToken", token);

        // Hacer la llamada al servidor
        Call<ResponseBody> call = authService.registerFcmToken(fullAuthToken, jsonBody);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Token FCM enviado exitosamente al servidor");
                } else {
                    try {
                        String errorBody = response.errorBody() != null ?
                                response.errorBody().string() : "Respuesta sin cuerpo";

                        Log.e(TAG, "Error al enviar token FCM al servidor. Código: " +
                                response.code() + " - " + errorBody);
                    } catch (IOException e) {
                        Log.e(TAG, "Error al leer cuerpo de error", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Fallo al enviar token FCM al servidor", t);
            }
        });
    }
}