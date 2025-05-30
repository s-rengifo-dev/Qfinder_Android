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
import com.sena.qfinder.models.ApiResponse;
import com.sena.qfinder.models.Mensaje;
import com.sena.qfinder.models.MensajeRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private Handler mainHandler = new Handler(Looper.getMainLooper());

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
            initializeFirebase();
            initializeRetrofit();
            setupConnectionMonitoring();
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
        Log.d(TAG, "Servicio Retrofit inicializado");
    }

    private void setupConnectionMonitoring() {
        connectionListener = connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null && connected) {
                    Log.d(TAG, "Conectado a Firebase");
                    if (callback != null) callback.onFirebaseConnected(true);
                } else {
                    Log.w(TAG, "Desconectado de Firebase");
                    if (callback != null) callback.onFirebaseConnected(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error monitoreando conexión: " + error.getMessage());
            }
        });
    }

    public void authenticateWithFirebase(String token) {
        Log.d(TAG, "Autenticando con Firebase con token personalizado");
        mAuth.signInWithCustomToken(token)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Autenticación con Firebase exitosa");
                        if (callback != null) callback.onFirebaseAuthSuccess();
                    } else {
                        String errorMsg = "Error de autenticación con Firebase";
                        if (task.getException() != null) {
                            errorMsg += ": " + task.getException().getMessage();
                        }
                        Log.e(TAG, errorMsg);
                        if (callback != null) callback.onFirebaseAuthFailed(errorMsg);
                    }
                });
    }

    public void setCallback(ChatCallback callback) {
        this.callback = callback;
    }

    public void cargarMensajesIniciales() {
        if (!validateRedId()) {
            Log.e(TAG, "ID de red no válido para cargar mensajes");
            return;
        }

        try {
            int redId = Integer.parseInt(idRedActual);
            Log.d(TAG, "Cargando mensajes iniciales para red: " + redId);

            Call<ApiResponse<List<Mensaje>>> call = authService.obtenerMensajes("Bearer " + authToken, redId, 50);

            call.enqueue(new Callback<ApiResponse<List<Mensaje>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Mensaje>>> call, Response<ApiResponse<List<Mensaje>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<Mensaje>> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            Log.d(TAG, "Mensajes cargados exitosamente: " + apiResponse.getData().size());
                            notifyMensajesCargados(apiResponse.getData());
                        } else {
                            Log.e(TAG, "Error en respuesta API: " + apiResponse.getMessage());
                            notifyError(apiResponse.getMessage());
                        }
                    } else {
                        String errorMsg = "Respuesta no exitosa: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                errorMsg += " - " + response.errorBody().string();
                            }
                        } catch (Exception e) {
                            errorMsg += " (error al leer cuerpo)";
                        }
                        Log.e(TAG, errorMsg);
                        notifyError(errorMsg);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Mensaje>>> call, Throwable t) {
                    Log.e(TAG, "Error de red al cargar mensajes: " + t.getMessage(), t);
                    notifyError("Error de conexión: " + t.getMessage());
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "ID de red inválido: " + idRedActual, e);
            notifyError("ID de comunidad no válido");
        }
    }

    public void verificarMembresia(int maxReintentos) {
        if (!validateRedId()) {
            Log.e(TAG, "ID de red no válido para verificar membresía");
            return;
        }

        try {
            int redId = Integer.parseInt(idRedActual);
            Log.d(TAG, "Verificando membresía para red: " + redId);

            Call<ResponseBody> call = authService.verificarMembresia("Bearer " + authToken, redId);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    boolean esMiembro = response.isSuccessful() && response.code() == 200;
                    Log.d(TAG, "Membresía verificada: " + (esMiembro ? "MIEMBRO" : "NO MIEMBRO"));
                    notifyMembresiaVerificada(esMiembro);
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e(TAG, "Error verificando membresía: " + t.getMessage(), t);
                    notifyMembresiaVerificada(false);
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "ID de red inválido: " + idRedActual, e);
            notifyMembresiaVerificada(false);
        }
    }

    public void iniciarEscuchaMensajes() {
        if (!validateRedId()) {
            Log.e(TAG, "ID de red no válido para iniciar escucha");
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "Usuario Firebase no autenticado");
            notifyError("Usuario no autenticado en Firebase");
            return;
        }

        if (mensajesListener != null) {
            detenerEscuchaMensajes();
        }

        Log.d(TAG, "Iniciando escucha de mensajes para red: " + idRedActual);
        DatabaseReference mensajesRef = databaseRef.child(idRedActual).child("mensajes");
        mensajesListener = mensajesRef.orderByChild("fecha_envio").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Mensaje> nuevosMensajes = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Mensaje mensaje = snapshot.getValue(Mensaje.class);
                        if (mensaje != null) {
                            mensaje.setId(snapshot.getKey());
                            if (mensaje.getHora() == null && mensaje.getFecha_envio() > 0) {
                                mensaje.setHora(convertirTimestampAHora(mensaje.getFecha_envio()));
                            }
                            nuevosMensajes.add(mensaje);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error procesando mensaje", e);
                    }
                }
                Log.d(TAG, "Nuevos mensajes recibidos: " + nuevosMensajes.size());
                notifyMensajesRecibidos(nuevosMensajes);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error en base de datos: " + databaseError.getMessage());
                notifyError("Error en base de datos: " + databaseError.getMessage());
            }
        });
    }

    private String convertirTimestampAHora(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            Log.e(TAG, "Error formateando hora", e);
            return "";
        }
    }

    public void enviarMensaje(Mensaje mensaje) {
        if (mensaje == null) {
            Log.e(TAG, "Intento de enviar mensaje nulo");
            return;
        }

        // Crear request con todos los campos requeridos
        MensajeRequest request = new MensajeRequest(
                mensaje.getContenido(),
                mensaje.getIdUsuario(),
                mensaje.getNombreUsuario()
        );

        Log.d(TAG, "Enviando mensaje: " + mensaje.getContenido());
        Call<ResponseBody> call = authService.enviarMensaje(
                "Bearer " + authToken,
                Integer.parseInt(idRedActual),
                request
        );

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Mensaje enviado al servidor exitosamente");
                    guardarMensajeEnFirebase(mensaje);
                } else {
                    Log.e(TAG, "Error al enviar mensaje al servidor: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Detalles error: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error leyendo cuerpo de error", e);
                    }
                    guardarMensajePendiente(mensaje);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Fallo de red al enviar mensaje: " + t.getMessage(), t);
                guardarMensajePendiente(mensaje);
            }
        });
    }

    private void guardarMensajeEnFirebase(Mensaje mensaje) {
        DatabaseReference nuevoMensajeRef = databaseRef.child(idRedActual).child("mensajes").push();

        Map<String, Object> mensajeMap = new HashMap<>();
        mensajeMap.put("id", nuevoMensajeRef.getKey());
        mensajeMap.put("nombreUsuario", mensaje.getNombreUsuario());
        mensajeMap.put("contenido", mensaje.getContenido());
        mensajeMap.put("hora", mensaje.getHora() != null ? mensaje.getHora() : convertirTimestampAHora(System.currentTimeMillis()));
        mensajeMap.put("comunidad", mensaje.getComunidad());
        mensajeMap.put("idUsuario", mensaje.getIdUsuario());
        mensajeMap.put("fecha_envio", mensaje.getFecha_envio() > 0 ? mensaje.getFecha_envio() : System.currentTimeMillis());
        mensajeMap.put("estado", "enviado");

        nuevoMensajeRef.setValue(mensajeMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Mensaje guardado en Firebase");
                    notifyMensajeEnviado();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error guardando mensaje en Firebase", e);
                    guardarMensajePendiente(mensaje);
                });
    }

    private void guardarMensajePendiente(Mensaje mensaje) {
        Log.w(TAG, "Guardando mensaje pendiente: " + mensaje.getContenido());
        DatabaseReference mensajePendienteRef = mensajesPendientesRef.child(idRedActual).push();

        Map<String, Object> mensajeMap = new HashMap<>();
        mensajeMap.put("id", mensajePendienteRef.getKey());
        mensajeMap.put("nombreUsuario", mensaje.getNombreUsuario());
        mensajeMap.put("contenido", mensaje.getContenido());
        mensajeMap.put("hora", mensaje.getHora() != null ? mensaje.getHora() : convertirTimestampAHora(System.currentTimeMillis()));
        mensajeMap.put("comunidad", mensaje.getComunidad());
        mensajeMap.put("idUsuario", mensaje.getIdUsuario());
        mensajeMap.put("fecha_envio", mensaje.getFecha_envio() > 0 ? mensaje.getFecha_envio() : System.currentTimeMillis());
        mensajeMap.put("estado", "pendiente");

        mensajePendienteRef.setValue(mensajeMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Mensaje pendiente guardado"))
                .addOnFailureListener(e -> Log.e(TAG, "Error guardando mensaje pendiente", e));
    }

    public void detenerEscuchaMensajes() {
        if (mensajesListener != null) {
            databaseRef.child(idRedActual).child("mensajes").removeEventListener(mensajesListener);
            mensajesListener = null;
            Log.d(TAG, "Escucha de mensajes detenida");
        }
    }

    public void cleanup() {
        detenerEscuchaMensajes();
        if (connectionListener != null) {
            connectedRef.removeEventListener(connectionListener);
            connectionListener = null;
            Log.d(TAG, "Monitoreo de conexión detenido");
        }
    }

    private boolean validateRedId() {
        if (idRedActual == null || idRedActual.isEmpty()) {
            notifyError("ID de comunidad no válido");
            return false;
        }
        try {
            Integer.parseInt(idRedActual);
            return true;
        } catch (NumberFormatException e) {
            notifyError("ID de comunidad no numérico");
            return false;
        }
    }

    private void notifyMensajesRecibidos(List<Mensaje> mensajes) {
        if (callback != null) {
            mainHandler.post(() -> callback.onMensajesRecibidos(mensajes));
        }
    }

    private void notifyError(String error) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(error));
        }
    }

    private void notifyMensajeEnviado() {
        if (callback != null) {
            mainHandler.post(() -> callback.onMensajeEnviado());
        }
    }

    private void notifyMembresiaVerificada(boolean esMiembro) {
        if (callback != null) {
            mainHandler.post(() -> callback.onMembresiaVerificada(esMiembro));
        }
    }

    private void notifyMensajesCargados(List<Mensaje> mensajes) {
        if (callback != null) {
            mainHandler.post(() -> callback.onMensajesCargados(mensajes));
        }
    }
}