package com.sena.qfinder.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.sena.qfinder.models.Mensaje;
import com.sena.qfinder.models.MensajeRequest;

import java.net.SocketTimeoutException;
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

    public interface ChatCallback {
        void onMensajesRecibidos(List<Mensaje> mensajes);
        void onError(String error);
        void onMensajeEnviado();
        void onMembresiaVerificada(boolean esMiembro);
        void onFirebaseConnected(boolean connected);
    }

    public ChatService(Context context, String idRed, String authToken) {
        this.context = context.getApplicationContext();
        this.idRedActual = idRed;
        this.authToken = authToken;

        try {
            Log.d(TAG, "Inicializando Firebase para ChatService");

            // Inicialización segura de Firebase
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setApplicationId("tu-app-id") // Reemplaza con tus valores reales
                        .setApiKey("tu-api-key")
                        .setDatabaseUrl("https://qfinder-comunity-default-rtdb.firebaseio.com/")
                        .build();
                FirebaseApp.initializeApp(context, options);
                Log.d(TAG, "Firebase inicializado correctamente");
            } else {
                Log.d(TAG, "Firebase ya estaba inicializado");
            }

            this.databaseRef = FirebaseDatabase.getInstance().getReference("comunidades");
            this.mensajesPendientesRef = FirebaseDatabase.getInstance().getReference("mensajes_pendientes");

            // Configurar monitor de conexión
            this.connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
            setupConnectionMonitor();

            Log.d(TAG, "Referencias a Firebase inicializadas correctamente");

            // Configurar cliente HTTP con timeout extendido
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(90, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS)
                    .writeTimeout(90, TimeUnit.SECONDS)
                    .build();

            this.retrofit = new Retrofit.Builder()
                    .baseUrl("https://qfinder-production.up.railway.app/")
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            this.authService = retrofit.create(AuthService.class);
            Log.d(TAG, "Servicios inicializados correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando Firebase", e);
            if (callback != null) {
                callback.onError("Error inicializando chat: " + e.getMessage());
            }
        }
    }

    private void setupConnectionMonitor() {
        connectionListener = connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                Log.d(TAG, "Conectado a Firebase: " + connected);
                if (callback != null) {
                    callback.onFirebaseConnected(connected);
                }
                if (!connected) {
                    Log.w(TAG, "Advertencia: Sin conexión a Firebase");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error monitorizando conexión: " + error.getMessage());
            }
        });
    }

    public void setCallback(ChatCallback callback) {
        this.callback = callback;
        Log.d(TAG, "Callback establecido");
    }

    public void verificarMembresia(int maxReintentos) {
        if (idRedActual == null || idRedActual.isEmpty()) {
            Log.e(TAG, "ID de red no válido para verificar membresía");
            if (callback != null) {
                callback.onError("ID de red no válido");
            }
            return;
        }

        Log.d(TAG, "Iniciando verificación de membresía para red: " + idRedActual +
                ", Intentos restantes: " + maxReintentos);

        if (maxReintentos <= 0) {
            Log.e(TAG, "Máximo de reintentos alcanzado para verificación de membresía");
            if (callback != null) {
                callback.onError("No se pudo verificar la membresía después de varios intentos");
            }
            return;
        }

        try {
            int redId = Integer.parseInt(idRedActual);
            Call<ResponseBody> call = authService.verificarMembresia("Bearer " + authToken, redId);
            Log.d(TAG, "Realizando llamada a verificarMembresia para redId: " + redId);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    Log.d(TAG, "Respuesta recibida de verificarMembresia. Código: " + response.code());

                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body() != null ? response.body().string() : "";
                            boolean esMiembro = responseBody.toLowerCase().contains("\"success\":true");
                            Log.d(TAG, "Resultado verificación membresía: " + esMiembro);

                            if (callback != null) {
                                callback.onMembresiaVerificada(esMiembro);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error procesando respuesta de membresía", e);
                            if (callback != null) {
                                callback.onError("Error procesando respuesta del servidor");
                            }
                        }
                    } else {
                        Log.w(TAG, "Respuesta no exitosa al verificar membresía. Código: " + response.code());
                        if (response.code() == 401 || response.code() == 403) {
                            // No autorizado o prohibido - no es miembro
                            if (callback != null) {
                                callback.onMembresiaVerificada(false);
                            }
                        } else {
                            // Reintentar para otros errores
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                Log.d(TAG, "Reintentando verificación de membresía...");
                                verificarMembresia(maxReintentos - 1);
                            }, 3000);
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    if (t instanceof SocketTimeoutException) {
                        Log.e(TAG, "Timeout al verificar membresía");
                    } else {
                        Log.e(TAG, "Error de conexión al verificar membresía", t);
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Log.d(TAG, "Reintentando verificación de membresía por error de conexión...");
                        verificarMembresia(maxReintentos - 1);
                    }, 3000);
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "ID de red inválido: " + idRedActual, e);
            if (callback != null) {
                callback.onError("ID de red inválido");
            }
        }
    }

    public void iniciarEscuchaMensajes() {
        if (idRedActual == null || idRedActual.isEmpty()) {
            Log.e(TAG, "ID de red no válido para escuchar mensajes");
            if (callback != null) {
                callback.onError("ID de comunidad no válido");
            }
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
                    Log.d(TAG, "Nuevos datos recibidos. Cantidad: " + dataSnapshot.getChildrenCount());

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            Mensaje mensaje = snapshot.getValue(Mensaje.class);
                            if (mensaje != null) {
                                mensaje.setId(snapshot.getKey());
                                nuevosMensajes.add(mensaje);
                                Log.d(TAG, "Mensaje procesado: " + mensaje.getContenido());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error procesando mensaje", e);
                        }
                    }

                    if (callback != null) {
                        callback.onMensajesRecibidos(nuevosMensajes);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    String errorMsg = "Error escuchando mensajes: " + databaseError.getMessage() +
                            ", Código: " + databaseError.getCode();
                    Log.e(TAG, errorMsg);

                    if (callback != null) {
                        String userMsg = "Error de base de datos: ";
                        switch (databaseError.getCode()) {
                            case DatabaseError.PERMISSION_DENIED:
                                userMsg += "Permiso denegado. Verifica las reglas de Firebase.";
                                break;
                            case DatabaseError.DISCONNECTED:
                                userMsg += "Desconectado de Firebase.";
                                break;
                            default:
                                userMsg += databaseError.getMessage();
                        }
                        callback.onError(userMsg);
                    }
                }
            });
            Log.d(TAG, "Escucha de mensajes iniciada correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error iniciando escucha de mensajes", e);
            if (callback != null) {
                callback.onError("Error al iniciar escucha: " + e.getMessage());
            }
        }
    }

    public void detenerEscuchaMensajes() {
        if (mensajesListener != null && databaseRef != null && idRedActual != null) {
            try {
                databaseRef.child(idRedActual).child("mensajes")
                        .removeEventListener(mensajesListener);
                mensajesListener = null;
                Log.d(TAG, "Escucha de mensajes detenida correctamente");
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

    public void enviarMensaje(Mensaje mensaje) {
        if (mensaje == null) {
            Log.e(TAG, "Intento de enviar mensaje nulo");
            return;
        }

        Log.d(TAG, "Preparando para enviar mensaje: " + mensaje.getContenido());
        enviarMensajeConReintentos(mensaje, 3); // 3 intentos máximo
    }

    private void enviarMensajeConReintentos(Mensaje mensaje, int intentosRestantes) {
        if (intentosRestantes <= 0) {
            Log.e(TAG, "No se pudo enviar el mensaje después de varios intentos");
            if (callback != null) {
                callback.onError("No se pudo enviar el mensaje después de varios intentos");
            }
            guardarMensajePendiente(mensaje);
            return;
        }

        try {
            MensajeRequest request = new MensajeRequest(
                    mensaje.getContenido(),
                    mensaje.getIdUsuario(),
                    mensaje.getNombreUsuario()
            );

            Log.d(TAG, "Enviando mensaje al servidor. Intento: " + (4 - intentosRestantes));
            Call<ResponseBody> call = authService.enviarMensaje(
                    "Bearer " + authToken,
                    Integer.parseInt(idRedActual),
                    request
            );

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Mensaje aceptado por el servidor. Guardando en Firebase...");
                        guardarMensajeEnFirebase(mensaje);
                    } else {
                        String errorMsg = "Error al enviar mensaje al backend: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                errorMsg += " - " + response.errorBody().string();
                            }
                        } catch (Exception e) {
                            errorMsg += " (error leyendo cuerpo)";
                        }
                        Log.e(TAG, errorMsg);

                        if (response.code() == 401 || response.code() == 403) {
                            // No autorizado o prohibido - no reintentar
                            if (callback != null) {
                                callback.onError("No tienes permiso para enviar mensajes");
                            }
                        } else {
                            // Reintentar para otros errores
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                Log.d(TAG, "Reintentando envío de mensaje...");
                                enviarMensajeConReintentos(mensaje, intentosRestantes - 1);
                            }, 2000);
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    String errorMsg;
                    if (t instanceof SocketTimeoutException) {
                        errorMsg = "Tiempo de espera agotado. Reintentando...";
                        Log.w(TAG, errorMsg);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            enviarMensajeConReintentos(mensaje, intentosRestantes - 1);
                        }, 2000);
                    } else {
                        errorMsg = "Error de conexión: " + t.getMessage();
                        Log.e(TAG, errorMsg, t);
                        if (callback != null) {
                            callback.onError(errorMsg);
                        }
                        guardarMensajePendiente(mensaje);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error enviando mensaje", e);
            if (callback != null) {
                callback.onError("Error al enviar mensaje: " + e.getMessage());
            }
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
                        Log.d(TAG, "Mensaje guardado en Firebase correctamente");
                        if (callback != null) {
                            callback.onMensajeEnviado();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error guardando mensaje en Firebase", e);
                        if (callback != null) {
                            callback.onError("Error al guardar mensaje localmente");
                        }
                        // Intenta guardar como pendiente si falla
                        guardarMensajePendiente(mensaje);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error guardando mensaje en Firebase", e);
            if (callback != null) {
                callback.onError("Error al guardar mensaje");
            }
            guardarMensajePendiente(mensaje);
        }
    }

    public void guardarMensajePendiente(Mensaje mensaje) {
        try {
            Log.d(TAG, "Guardando mensaje pendiente: " + mensaje.getContenido());

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
                        Log.d(TAG, "Mensaje guardado como pendiente correctamente");
                        if (callback != null) {
                            callback.onError("Mensaje guardado como pendiente. Se enviará cuando haya conexión.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error guardando mensaje pendiente", e);
                        if (callback != null) {
                            callback.onError("Error al guardar mensaje pendiente: " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error guardando mensaje pendiente", e);
            if (callback != null) {
                callback.onError("Error crítico al guardar mensaje pendiente");
            }
        }
    }

    public void cleanup() {
        detenerEscuchaMensajes();
        detenerConnectionMonitor();
        Log.d(TAG, "ChatService limpiado correctamente");
    }
}