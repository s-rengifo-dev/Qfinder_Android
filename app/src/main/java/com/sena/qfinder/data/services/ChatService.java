package com.sena.qfinder.data.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.ApiResponse;
import com.sena.qfinder.data.models.FirebaseTokenResponse;
import com.sena.qfinder.data.models.Mensaje;
import com.sena.qfinder.data.models.MensajeRequest;
import com.sena.qfinder.data.models.MensajesResponse;

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
    private String apiToken;
    private Context context;
    private Retrofit retrofit;
    private FirebaseAuth mAuth;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isFirebaseInitialized = false;
    private int reintentosAuth = 0;
    private static final int MAX_REINTENTOS_AUTH = 3;

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

    public ChatService(Context context, String idRed, String apiToken) {
        this.context = context.getApplicationContext();
        this.idRedActual = idRed;
        this.apiToken = apiToken;
        this.mAuth = FirebaseAuth.getInstance();

        Log.d(TAG, "Inicializando ChatService | Red: " + idRed);
        initializeServices();
        obtenerTokenFirebase();
    }

    private void initializeServices() {
        Log.d(TAG, "Inicializando servicios...");
        try {
            initializeFirebase();
            initializeRetrofit();

            if (isFirebaseInitialized) {
                setupConnectionMonitoring();
            }

            Log.d(TAG, "Servicios inicializados correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error crítico inicializando servicios", e);
            notifyError("Error inicializando chat: " + e.getMessage());
        }
    }

    private void initializeFirebase() {
        if (FirebaseApp.getApps(context).size() > 0) {
            Log.d(TAG, "Firebase ya está inicializado globalmente");
            isFirebaseInitialized = true;
            initializeDatabaseReferences();
            return;
        }

        Log.d(TAG, "Inicializando Firebase...");
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:943234700783:android:63a905964f25737428521a")
                    .setApiKey("AIzaSyDWsifL9DrQkUSqSmaVstQ7Cr9dhAPoPZg")
                    .setDatabaseUrl("https://qfinder-comunity-default-rtdb.firebaseio.com/")
                    .setProjectId("qfinder-community")
                    .build();

            FirebaseApp.initializeApp(context, options);
            Log.d(TAG, "Firebase inicializado correctamente");
            isFirebaseInitialized = true;
            initializeDatabaseReferences();
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                Log.w(TAG, "Firebase ya estaba inicializado, usando instancia existente");
                isFirebaseInitialized = true;
                initializeDatabaseReferences();
            } else {
                Log.e(TAG, "Error inicializando Firebase", e);
                isFirebaseInitialized = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando Firebase", e);
            isFirebaseInitialized = false;
        }
    }

    private void initializeDatabaseReferences() {
        if (isFirebaseInitialized) {
            this.databaseRef = FirebaseDatabase.getInstance().getReference("chats");
            this.mensajesPendientesRef = FirebaseDatabase.getInstance().getReference("mensajes_pendientes");
            this.connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
            Log.d(TAG, "Referencias de Firebase Database inicializadas");
        }
    }

    private void initializeRetrofit() {
        Log.d(TAG, "Inicializando Retrofit...");

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
        if (connectedRef == null) {
            Log.e(TAG, "No se puede monitorear conexión: connectedRef es nulo");
            return;
        }

        Log.d(TAG, "Configurando monitoreo de conexión Firebase...");

        connectionListener = connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                Log.d(TAG, "Estado conexión Firebase: " + connected + " | Red: " + idRedActual);

                if (connected != null && connected) {
                    Log.d(TAG, "Conectado a Firebase");
                    if (callback != null) callback.onFirebaseConnected(true);

                    // Reconectar servicios si es necesario
                    if (mensajesListener == null) {
                        iniciarEscuchaMensajes();
                    }
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

    private void obtenerTokenFirebase() {
        if (idRedActual == null) {
            Log.e(TAG, "ID de red no disponible para obtener token Firebase");
            return;
        }

        try {
            int redId = Integer.parseInt(idRedActual);
            Log.d(TAG, "Solicitando token Firebase para red: " + redId);

            Call<FirebaseTokenResponse> call = authService.getFirebaseToken(
                    "Bearer " + apiToken,
                    redId
            );

            call.enqueue(new Callback<FirebaseTokenResponse>() {
                @Override
                public void onResponse(Call<FirebaseTokenResponse> call, Response<FirebaseTokenResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        FirebaseTokenResponse tokenResponse = response.body();
                        if (tokenResponse.isSuccess()) {
                            String firebaseToken = tokenResponse.getToken();

                            if (firebaseToken == null || firebaseToken.isEmpty() || firebaseToken.length() < 100) {
                                String errorDetails = "Token inválido: ";
                                if (firebaseToken == null) errorDetails += "null";
                                else if (firebaseToken.isEmpty()) errorDetails += "vacío";
                                else errorDetails += "longitud: " + firebaseToken.length();

                                Log.e(TAG, errorDetails);
                                notifyError("Token de autenticación inválido");
                                return;
                            }

                            Log.d(TAG, "Token Firebase recibido - Longitud: " + firebaseToken.length());
                            Log.d(TAG, "Token (inicio): " + firebaseToken.substring(0, 20) + "...");
                            authenticateWithFirebase(firebaseToken);
                        } else {
                            String errorMsg = tokenResponse.getMessage() != null ?
                                    tokenResponse.getMessage() : "Error en respuesta del servidor";
                            Log.e(TAG, errorMsg);
                            notifyError(errorMsg);
                        }
                    } else {
                        String errorMsg = "Error en el servidor: " + response.code();
                        if (response.errorBody() != null) {
                            try {
                                String errorBody = response.errorBody().string();
                                if (errorBody.startsWith("<!DOCTYPE html>")) {
                                    errorMsg = "Error interno del servidor (HTML)";
                                    Log.e(TAG, "Respuesta HTML recibida en lugar de JSON");
                                } else {
                                    errorMsg += " - " + errorBody;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error leyendo cuerpo de error", e);
                                errorMsg += " (error leyendo cuerpo)";
                            }
                        }
                        Log.e(TAG, errorMsg);
                        notifyError(errorMsg);
                    }
                }

                @Override
                public void onFailure(Call<FirebaseTokenResponse> call, Throwable t) {
                    Log.e(TAG, "Error de red al obtener token Firebase: " + t.getMessage(), t);
                    notifyError("Error de conexión: " + t.getMessage());
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "ID de red inválido: " + idRedActual, e);
            notifyError("ID de comunidad no válido");
        }
    }

    public void authenticateWithFirebase(String token) {
        Log.d(TAG, "Autenticando con Firebase con token personalizado");

        if (token == null) {
            Log.e(TAG, "ERROR: Token Firebase es NULL!");
            notifyError("Token de autenticación inválido");
            return;
        }

        String[] tokenParts = token.split("\\.");
        int segmentCount = tokenParts.length;
        Log.d(TAG, "Token Firebase - Segmentos: " + segmentCount + ", Longitud: " + token.length());

        if (segmentCount != 3) {
            Log.e(TAG, "ERROR: Formato de token inválido. Se esperaban 3 segmentos");
            notifyError("Formato de token inválido");
            return;
        }

        mAuth.signInWithCustomToken(token)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        reintentosAuth = 0;
                        if (mAuth.getCurrentUser() != null) {
                            Log.d(TAG, "Autenticación exitosa | UID: " + mAuth.getCurrentUser().getUid());
                            if (callback != null) callback.onFirebaseAuthSuccess();
                        } else {
                            Log.e(TAG, "Autenticación exitosa pero usuario es NULL");
                            notifyError("Error interno de autenticación");
                        }
                    } else {
                        reintentosAuth++;
                        String errorMsg = "Error de autenticación: ";
                        Exception e = task.getException();

                        if (e instanceof FirebaseAuthException) {
                            String errorCode = ((FirebaseAuthException) e).getErrorCode();
                            errorMsg += "Código: " + errorCode + " | " + e.getMessage();
                            Log.e(TAG, errorMsg, e);

                            if ("INVALID_CUSTOM_TOKEN".equals(errorCode) && reintentosAuth < MAX_REINTENTOS_AUTH) {
                                Log.d(TAG, "Reintentando obtener token...");
                                obtenerTokenFirebase();
                                return;
                            }
                        } else if (e != null) {
                            errorMsg += e.getMessage();
                            Log.e(TAG, errorMsg, e);
                        }

                        if (callback != null) callback.onFirebaseAuthFailed(errorMsg);
                    }
                });
    }

    public void setCallback(ChatCallback callback) {
        Log.d(TAG, "Callback establecido");
        this.callback = callback;
    }

    public void cargarMensajesIniciales() {
        Log.d(TAG, "Solicitando mensajes iniciales...");

        if (!validateRedId()) {
            Log.e(TAG, "ID de red no válido para cargar mensajes");
            return;
        }

        try {
            int redId = Integer.parseInt(idRedActual);
            Log.d(TAG, "Cargando mensajes iniciales para red: " + redId);

            Call<ApiResponse<MensajesResponse>> call = authService.obtenerMensajes("Bearer " + apiToken, redId, 50);

            call.enqueue(new Callback<ApiResponse<MensajesResponse>>() {
                @Override
                public void onResponse(Call<ApiResponse<MensajesResponse>> call, Response<ApiResponse<MensajesResponse>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<MensajesResponse> apiResponse = response.body();

                        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                            List<Mensaje> mensajes = apiResponse.getData().getMessages();

                            if (mensajes == null) {
                                mensajes = new ArrayList<>();
                                Log.w(TAG, "La API devolvió lista de mensajes NULL - Usando lista vacía");
                            }

                            Log.d(TAG, "Mensajes cargados exitosamente: " + mensajes.size());
                            notifyMensajesCargados(mensajes);
                        } else {
                            String errorMsg = apiResponse.getMessage() != null ?
                                    apiResponse.getMessage() : "Respuesta API sin mensaje de error";
                            Log.e(TAG, "Error en respuesta API: " + errorMsg);
                            notifyError(errorMsg);
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
                public void onFailure(Call<ApiResponse<MensajesResponse>> call, Throwable t) {
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
        Log.d(TAG, "Verificando membresía...");

        if (!validateRedId()) {
            Log.e(TAG, "ID de red no válido para verificar membresía");
            return;
        }

        try {
            int redId = Integer.parseInt(idRedActual);
            Log.d(TAG, "Verificando membresía para red: " + redId);

            Call<ResponseBody> call = authService.verificarMembresia("Bearer " + apiToken, redId);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    boolean esMiembro = response.isSuccessful() && response.code() == 200;
                    Log.d(TAG, "Membresía verificada: " + (esMiembro ? "MIEMBRO" : "NO MIEMBRO"));

                    // SOLUCIÓN CLAVE: Eliminamos la sincronización con Firebase
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
        Log.d(TAG, "Intentando iniciar escucha de mensajes...");

        // Detener cualquier escucha previa primero
        detenerEscuchaMensajes();

        if (!validateRedId()) {
            Log.e(TAG, "ID de red no válido para iniciar escucha");
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "USUARIO FIREBASE NO AUTENTICADO - No se puede iniciar escucha");
            notifyError("Usuario no autenticado en Firebase");
            return;
        } else {
            Log.d(TAG, "Usuario autenticado. UID: " + mAuth.getCurrentUser().getUid());
        }

        Log.d(TAG, "Iniciando escucha de mensajes para red: " + idRedActual);
        DatabaseReference mensajesRef = databaseRef.child(idRedActual).child("mensajes");
        Log.d(TAG, "Ruta Firebase: " + mensajesRef.toString());

        mensajesListener = mensajesRef.orderByChild("fecha_envio").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Evento dataChange recibido | Hijos: " + dataSnapshot.getChildrenCount());

                List<Mensaje> nuevosMensajes = new ArrayList<>();
                int messageCount = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Mensaje mensaje = snapshot.getValue(Mensaje.class);
                        if (mensaje != null) {
                            mensaje.setId(snapshot.getKey());
                            if (mensaje.getHora() == null && mensaje.getFecha_envio() > 0) {
                                mensaje.setHora(convertirTimestampAHora(mensaje.getFecha_envio()));
                            }
                            nuevosMensajes.add(mensaje);
                            messageCount++;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error procesando mensaje", e);
                    }
                }
                Log.d(TAG, "Nuevos mensajes recibidos: " + messageCount);
                notifyMensajesRecibidos(nuevosMensajes);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error en base de datos: " + databaseError.getMessage());
                notifyError("Error en base de datos: " + databaseError.getMessage());
            }
        });
    }

    public void detenerEscuchaMensajes() {
        if (mensajesListener != null) {
            Log.d(TAG, "Deteniendo escucha de mensajes activa");
            DatabaseReference mensajesRef = databaseRef.child(idRedActual).child("mensajes");
            mensajesRef.removeEventListener(mensajesListener);
            mensajesListener = null;
        } else {
            Log.d(TAG, "No hay escucha activa para detener");
        }
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

        Log.d(TAG, "Preparando envío de mensaje: " + mensaje.getContenido()
                + " | Usuario: " + mensaje.getIdUsuario()
                + " | Red: " + idRedActual);

        MensajeRequest request = new MensajeRequest(
                mensaje.getContenido(),
                mensaje.getIdUsuario(),
                mensaje.getNombreUsuario()
        );

        Call<ResponseBody> call = authService.enviarMensaje(
                "Bearer " + apiToken,
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
                Log.e(TAG, "Fallo de net al enviar mensaje: " + t.getMessage(), t);
                guardarMensajePendiente(mensaje);
            }
        });
    }

    private void guardarMensajeEnFirebase(Mensaje mensaje) {
        Log.d(TAG, "Guardando mensaje en Firebase: " + mensaje.getContenido());

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

    public void cleanup() {
        Log.d(TAG, "Realizando limpieza...");
        detenerEscuchaMensajes();
        if (connectionListener != null) {
            connectedRef.removeEventListener(connectionListener);
            connectionListener = null;
            Log.d(TAG, "Monitoreo de conexión detenido");
        }
        // Limpiar referencias
        idRedActual = null;
        apiToken = null;
        callback = null;
        reintentosAuth = 0;
        Log.d(TAG, "Limpieza completada");
    }

    private boolean validateRedId() {
        if (idRedActual == null || idRedActual.isEmpty()) {
            Log.e(TAG, "ID de comunidad vacío o nulo");
            notifyError("ID de comunidad no válido");
            return false;
        }
        try {
            Integer.parseInt(idRedActual);
            return true;
        } catch (NumberFormatException e) {
            Log.e(TAG, "ID de comunidad no numérico: " + idRedActual);
            notifyError("ID de comunidad no numérico");
            return false;
        }
    }

    private void notifyMensajesRecibidos(List<Mensaje> mensajes) {
        Log.d(TAG, "Notificando " + mensajes.size() + " mensajes recibidos");
        if (callback != null) {
            mainHandler.post(() -> {
                Log.d(TAG, "Ejecutando callback onMensajesRecibidos");
                callback.onMensajesRecibidos(mensajes);
            });
        } else {
            Log.w(TAG, "Callback no definido para onMensajesRecibidos");
        }
    }

    private void notifyError(String error) {
        Log.e(TAG, "Notificando error: " + error);
        if (callback != null) {
            mainHandler.post(() -> {
                Log.d(TAG, "Ejecutando callback onError");
                callback.onError(error);
            });
        } else {
            Log.w(TAG, "Callback no definido para onError");
        }
    }

    private void notifyMensajeEnviado() {
        Log.d(TAG, "Notificando mensaje enviado");
        if (callback != null) {
            mainHandler.post(() -> {
                Log.d(TAG, "Ejecutando callback onMensajeEnviado");
                callback.onMensajeEnviado();
            });
        } else {
            Log.w(TAG, "Callback no definido para onMensajeEnviado");
        }
    }

    private void notifyMembresiaVerificada(boolean esMiembro) {
        Log.d(TAG, "Notificando verificación de membresía: " + esMiembro);
        if (callback != null) {
            mainHandler.post(() -> {
                Log.d(TAG, "Ejecutando callback onMembresiaVerificada");
                callback.onMembresiaVerificada(esMiembro);
            });
        } else {
            Log.w(TAG, "Callback no definido para onMembresiaVerificada");
        }
    }

    private void notifyMensajesCargados(List<Mensaje> mensajes) {
        Log.d(TAG, "Notificando mensajes cargados: " + mensajes.size());
        if (callback != null) {
            mainHandler.post(() -> {
                Log.d(TAG, "Ejecutando callback onMensajesCargados");
                callback.onMensajesCargados(mensajes);
            });
        } else {
            Log.w(TAG, "Callback no definido para onMensajesCargados");
        }
    }
}