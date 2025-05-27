package com.sena.qfinder.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.sena.qfinder.models.Mensaje;
import com.sena.qfinder.models.MensajeRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatService {
    private static final String TAG = "ChatService";
    private DatabaseReference databaseRef;
    private DatabaseReference mensajesPendientesRef;
    private DatabaseReference connectedRef;
    private String idRedActual;
    private ValueEventListener mensajesListener;
    private ValueEventListener connectionListener;
    private ChatCallback callback;
    private AuthService authService;
    private String authToken;
    private Context context;
    private Retrofit retrofit;
    private FirebaseAuth mAuth;
    private String firebaseToken;

    public interface ChatCallback {
        void onMensajesRecibidos(List<Mensaje> mensajes);
        void onError(String error);
        void onMensajeEnviado();
        void onMembresiaVerificada(boolean esMiembro);
        void onFirebaseConnected(boolean connected);
        void onMensajesCargados(List<Mensaje> mensajes);
        void onFirebaseAuthSuccess();
        void onFirebaseAuthFailed(String error);
    }

    public ChatService(Context context, String idRed, String authToken) {
        this.context = context.getApplicationContext();
        this.idRedActual = idRed;
        this.authToken = authToken;
        this.mAuth = FirebaseAuth.getInstance();
        initializeServices();
    }

    private void initializeServices() {
        try {
            Log.d(TAG, "Inicializando servicios de chat");

            // Configuración de Firebase
            initializeFirebase();

            // Configuración de Retrofit
            initializeRetrofit();

            Log.d(TAG, "Servicios inicializados correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando servicios", e);
            notifyError("Error inicializando chat: " + e.getMessage());
        }
    }

    private void initializeFirebase() {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:943234700783:android:63a905964f25737428521a")
                    .setApiKey("AIzaSyDWsifL9DrQkUSqSmaVstQ7Cr9dhAPoPZg")
                    .setDatabaseUrl("https://qfinder-comunity-default-rtdb.firebaseio.com/")
                    .setProjectId("qfinder-community")
                    .build();
            FirebaseApp.initializeApp(context, options);
            Log.d(TAG, "Firebase inicializado correctamente");
        } else {
            Log.d(TAG, "Firebase ya estaba inicializado");
        }

        this.databaseRef = FirebaseDatabase.getInstance().getReference("chats");
        this.mensajesPendientesRef = FirebaseDatabase.getInstance().getReference("mensajes_pendientes");
        this.connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
    }

    private void initializeRetrofit() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        this.retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.authService = retrofit.create(AuthService.class);
    }

    public void authenticateWithFirebase(String token) {
        mAuth.signInWithCustomToken(token)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase auth success");
                        setupConnectionMonitor();
                        cargarMensajesIniciales();
                        iniciarEscuchaMensajes();
                    } else {
                        Log.e(TAG, "Firebase auth failed", task.getException());
                        notifyError("Error de autenticación con Firebase");
                    }
                });
    }

    public void setCallback(ChatCallback callback) {
        this.callback = callback;
        Log.d(TAG, "Callback establecido");
    }

    private void setupConnectionMonitor() {
        if (connectionListener != null) {
            connectedRef.removeEventListener(connectionListener);
        }

        connectionListener = connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                Log.d(TAG, "Estado conexión Firebase: " + connected);
                if (callback != null) {
                    callback.onFirebaseConnected(connected);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error monitorizando conexión: " + error.getMessage());
            }
        });
    }

    public void cargarMensajesIniciales() {
        if (!validateRedId()) return;

        try {
            int redId = Integer.parseInt(idRedActual);
            Call<List<Mensaje>> call = authService.obtenerMensajes("Bearer " + authToken, redId, 50);

            call.enqueue(new Callback<List<Mensaje>>() {
                @Override
                public void onResponse(Call<List<Mensaje>> call, Response<List<Mensaje>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        notifyMensajesCargados(response.body());
                    } else {
                        Log.e(TAG, "Error al cargar mensajes: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<List<Mensaje>> call, Throwable t) {
                    Log.e(TAG, "Error de conexión al cargar mensajes", t);
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "ID de red inválido: " + idRedActual, e);
            notifyError("ID de red inválido");
        }
    }

    public void verificarMembresia(int maxReintentos) {
        if (!validateRedId()) return;

        try {
            int redId = Integer.parseInt(idRedActual);
            Call<ResponseBody> call = authService.verificarMembresia("Bearer " + authToken, redId);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    boolean esMiembro = response.isSuccessful() && response.code() == 200;
                    notifyMembresiaVerificada(esMiembro);
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e(TAG, "Error de conexión al verificar membresía", t);
                    notifyError("Error de conexión al verificar membresía");
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "ID de red inválido: " + idRedActual, e);
            notifyError("ID de red inválido");
        }
    }

    public void iniciarEscuchaMensajes() {
        if (!validateRedId()) {
            notifyError("ID de comunidad no válido");
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "Usuario no autenticado en Firebase");
            notifyError("Usuario no autenticado en Firebase");
            return;
        }

        Log.d(TAG, "Iniciando escucha de mensajes para red: " + idRedActual);

        if (mensajesListener != null) {
            detenerEscuchaMensajes();
        }

        try {
            DatabaseReference mensajesRef = databaseRef.child(idRedActual).child("mensajes");
            Log.d(TAG, "Referencia a mensajes: " + mensajesRef.toString());

            mensajesListener = mensajesRef.orderByChild("fecha_envio").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Mensaje> nuevosMensajes = new ArrayList<>();
                    Log.d(TAG, "Nuevos mensajes recibidos: " + dataSnapshot.getChildrenCount());

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            Mensaje mensaje = snapshot.getValue(Mensaje.class);
                            if (mensaje != null) {
                                mensaje.setId(snapshot.getKey());
                                nuevosMensajes.add(mensaje);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error procesando mensaje", e);
                        }
                    }

                    notifyMensajesRecibidos(nuevosMensajes);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    handleDatabaseError(databaseError);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error iniciando escucha de mensajes", e);
            notifyError("Error al iniciar escucha: " + e.getMessage());
        }
    }

    public void enviarMensaje(Mensaje mensaje) {
        if (mensaje == null) {
            Log.e(TAG, "Intento de enviar mensaje nulo");
            return;
        }

        Log.d(TAG, "Preparando para enviar mensaje: " + mensaje.getContenido());
        enviarMensajeConReintentos(mensaje, 3);
    }

    private void enviarMensajeConReintentos(Mensaje mensaje, int intentosRestantes) {
        if (intentosRestantes <= 0) {
            Log.e(TAG, "No se pudo enviar el mensaje después de varios intentos");
            notifyError("No se pudo enviar el mensaje después de varios intentos");
            guardarMensajePendiente(mensaje);
            return;
        }

        try {
            MensajeRequest request = new MensajeRequest(
                    mensaje.getContenido(),
                    mensaje.getIdUsuario(),
                    mensaje.getNombreUsuario()
            );

            Call<ResponseBody> call = authService.enviarMensaje(
                    "Bearer " + authToken,
                    Integer.parseInt(idRedActual),
                    request
            );

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        guardarMensajeEnFirebase(mensaje);
                    } else {
                        handleSendMessageError(response, mensaje, intentosRestantes);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    retrySendMessage(mensaje, intentosRestantes);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error enviando mensaje", e);
            guardarMensajePendiente(mensaje);
        }
    }

    private void guardarMensajeEnFirebase(Mensaje mensaje) {
        try {
            DatabaseReference nuevoMensajeRef = databaseRef.child(idRedActual)
                    .child("mensajes")
                    .push();

            mensaje.setId(nuevoMensajeRef.getKey());
            mensaje.setFecha_envio(System.currentTimeMillis());
            mensaje.setEstado("enviado");

            nuevoMensajeRef.setValue(mensaje)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Mensaje guardado en Firebase");
                        notifyMensajeEnviado();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error guardando mensaje en Firebase", e);
                        guardarMensajePendiente(mensaje);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error guardando mensaje en Firebase", e);
            guardarMensajePendiente(mensaje);
        }
    }

    private void guardarMensajePendiente(Mensaje mensaje) {
        try {
            if (mensajesPendientesRef == null) {
                mensajesPendientesRef = FirebaseDatabase.getInstance().getReference("mensajes_pendientes");
            }

            DatabaseReference mensajePendienteRef = mensajesPendientesRef.child(idRedActual)
                    .push();

            mensaje.setId(mensajePendienteRef.getKey());
            mensaje.setFecha_envio(System.currentTimeMillis());
            mensaje.setEstado("pendiente");

            mensajePendienteRef.setValue(mensaje)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Mensaje guardado como pendiente");
                        notifyError("Mensaje guardado como pendiente. Se enviará cuando haya conexión.");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error guardando mensaje pendiente", e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error guardando mensaje pendiente", e);
        }
    }

    public void detenerEscuchaMensajes() {
        if (mensajesListener != null && databaseRef != null && idRedActual != null) {
            try {
                databaseRef.child(idRedActual).child("mensajes")
                        .removeEventListener(mensajesListener);
                mensajesListener = null;
                Log.d(TAG, "Escucha de mensajes detenida");
            } catch (Exception e) {
                Log.e(TAG, "Error deteniendo escucha", e);
            }
        }
    }

    public void detenerConnectionMonitor() {
        if (connectionListener != null && connectedRef != null) {
            connectedRef.removeEventListener(connectionListener);
            connectionListener = null;
            Log.d(TAG, "Monitor de conexión detenido");
        }
    }

    public void cleanup() {
        detenerEscuchaMensajes();
        detenerConnectionMonitor();
        Log.d(TAG, "ChatService limpiado");
    }

    // Métodos auxiliares
    private boolean validateRedId() {
        if (idRedActual == null || idRedActual.isEmpty()) {
            Log.e(TAG, "ID de red no válido");
            return false;
        }
        return true;
    }

    private void handleDatabaseError(DatabaseError error) {
        String errorMsg = "Error en base de datos: " + error.getMessage() + ", Código: " + error.getCode();
        Log.e(TAG, errorMsg);

        String userMsg = "Error: ";
        switch (error.getCode()) {
            case DatabaseError.PERMISSION_DENIED:
                userMsg += "Permiso denegado. Verifica las reglas de Firebase.";
                break;
            case DatabaseError.DISCONNECTED:
                userMsg += "Desconectado de Firebase.";
                break;
            default:
                userMsg += error.getMessage();
        }
        notifyError(userMsg);
    }

    private void handleSendMessageError(Response<ResponseBody> response, Mensaje mensaje, int intentosRestantes) {
        if (response.code() == 403) {
            verificarMembresia(3);
            notifyError("No tienes permiso para enviar mensajes");
        } else {
            retrySendMessage(mensaje, intentosRestantes);
        }
    }

    private void retrySendMessage(Mensaje mensaje, int intentosRestantes) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            enviarMensajeConReintentos(mensaje, intentosRestantes - 1);
        }, 2000);
    }

    // Métodos de notificación
    private void notifyMensajesRecibidos(List<Mensaje> mensajes) {
        if (callback != null) {
            callback.onMensajesRecibidos(mensajes);
        }
    }

    private void notifyError(String error) {
        if (callback != null) {
            callback.onError(error);
        }
    }

    private void notifyMensajeEnviado() {
        if (callback != null) {
            callback.onMensajeEnviado();
        }
    }

    private void notifyMembresiaVerificada(boolean esMiembro) {
        if (callback != null) {
            callback.onMembresiaVerificada(esMiembro);
        }
    }

    private void notifyMensajesCargados(List<Mensaje> mensajes) {
        if (callback != null) {
            callback.onMensajesCargados(mensajes);
        }
    }
}