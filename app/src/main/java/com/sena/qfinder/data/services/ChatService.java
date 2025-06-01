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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private Set<String> mensajesEntregados = new HashSet<>();

    private DatabaseReference databaseRef;          // Referencia a la base de datos de Firebase para chats
    private DatabaseReference mensajesPendientesRef; // Referencia para mensajes pendientes de envío
    private DatabaseReference connectedRef;         // Referencia para monitorear conexión con Firebase
    private String idRedActual;                     // ID de la red/comunidad actual
    private ValueEventListener mensajesListener;    // Listener para cambios en los mensajes
    private ValueEventListener connectionListener;  // Listener para cambios en la conexión
    private ChatCallback callback;                  // Interfaz de callback para comunicación con UI
    private AuthService authService;                // Servicio API Retrofit
    private String apiToken;                        // Token de autenticación del usuario
    private Context context;                        // Contexto de la aplicación
    private Retrofit retrofit;                      // Cliente Retrofit
    private FirebaseAuth mAuth;                     // Instancia de Firebase Authentication
    private Handler mainHandler = new Handler(Looper.getMainLooper()); // Handler para ejecutar en hilo principal
    private boolean isFirebaseInitialized = false;  // Bandera de inicialización de Firebase
    private int reintentosAuth = 0;                 // Contador de reintentos de autenticación
    private static final int MAX_REINTENTOS_AUTH = 3; // Máximo de reintentos para autenticación

    private int lastNotifiedSize = -1;
    private long lastNotificationTime = 0;
    private static final long DEBOUNCE_TIME_MS = 500; // 500 milisegundos
    // Interfaz de callback para eventos del chat
    public interface ChatCallback {
        void onMensajesRecibidos(List<Mensaje> mensajes);  // Nuevos mensajes recibidos
        void onError(String error);                        // Error en operaciones
        void onMensajeEnviado();                           // Mensaje enviado con éxito
        void onMembresiaVerificada(boolean esMiembro);      // Resultado de verificación de membresía
        void onFirebaseConnected(boolean connected);        // Estado de conexión con Firebase
        void onMensajesCargados(List<Mensaje> mensajes);    // Mensajes iniciales cargados desde API
        void onFirebaseAuthSuccess();                      // Autenticación en Firebase exitosa
        void onFirebaseAuthFailed(String error);            // Fallo en autenticación con Firebase
    }

    /**
     * Constructor - Inicializa el servicio de chat
     * @param context Contexto de la aplicación
     * @param idRed ID de la red/comunidad
     * @param apiToken Token de autenticación del usuario
     */
    public ChatService(Context context, String idRed, String apiToken) {
        this.context = context.getApplicationContext();
        this.idRedActual = idRed;
        this.apiToken = apiToken;
        this.mAuth = FirebaseAuth.getInstance();

        Log.d(TAG, "Inicializando ChatService | Red: " + idRed);
        initializeServices();      // Inicializa Firebase y Retrofit
        obtenerTokenFirebase();    // Obtiene token para autenticar en Firebase
    }

    /**
     * Inicializa los servicios principales (Firebase y Retrofit)
     */
    private void initializeServices() {
        Log.d(TAG, "Inicializando servicios...");
        try {
            initializeFirebase();  // Configura Firebase
            initializeRetrofit();  // Configura Retrofit

            // Monitorea conexión si Firebase está inicializado
            if (isFirebaseInitialized) {
                setupConnectionMonitoring();
            }

            Log.d(TAG, "Servicios inicializados correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error crítico inicializando servicios", e);
            notifyError("Error inicializando chat: " + e.getMessage());
        }
    }

    /**
     * Inicializa Firebase si no está ya inicializado
     */
    private void initializeFirebase() {
        // Verifica si Firebase ya está inicializado
        if (FirebaseApp.getApps(context).size() > 0) {
            Log.d(TAG, "Firebase ya está inicializado globalmente");
            isFirebaseInitialized = true;
            initializeDatabaseReferences();
            return;
        }

        Log.d(TAG, "Inicializando Firebase...");
        try {
            // Configuración personalizada de Firebase
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
            // Maneja caso donde Firebase ya está inicializado
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

    /**
     * Inicializa las referencias a la base de datos de Firebase
     */
    private void initializeDatabaseReferences() {
        if (isFirebaseInitialized) {
            this.databaseRef = FirebaseDatabase.getInstance().getReference("chats");
            this.mensajesPendientesRef = FirebaseDatabase.getInstance().getReference("mensajes_pendientes");
            this.connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
            Log.d(TAG, "Referencias de Firebase Database inicializadas");
        }
    }

    /**
     * Configura el cliente Retrofit para llamadas a la API
     */
    private void initializeRetrofit() {
        Log.d(TAG, "Inicializando Retrofit...");

        // Configura cliente HTTP con timeouts
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // Crea instancia de Retrofit
        this.retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.authService = retrofit.create(AuthService.class);
        Log.d(TAG, "Servicio Retrofit inicializado");
    }

    /**
     * Monitorea el estado de conexión con Firebase
     */
    private void setupConnectionMonitoring() {
        if (connectedRef == null) {
            Log.e(TAG, "No se puede monitorear conexión: connectedRef es nulo");
            return;
        }

        Log.d(TAG, "Configurando monitoreo de conexión Firebase...");

        connectionListener = connectedRef.addValueEventListener(new ValueEventListener() {
            private final Set<String> mensajesEntregados = new HashSet<>(); // Variable de clase

            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Evento dataChange recibido | Hijos: " + dataSnapshot.getChildrenCount());

                List<Mensaje> nuevosMensajes = new ArrayList<>();
                int messageCount = 0;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {

                        Mensaje mensaje = snapshot.getValue(Mensaje.class);
                        // 🔒 Verifica si el mensaje ya fue entregado
                        if (!mensajesEntregados.contains(mensaje.getId())) {
                            nuevosMensajes.add(mensaje);
                            mensajesEntregados.add(mensaje.getId());
                            messageCount++;
                        }
                        if (mensaje != null) {
                            mensaje.setId(snapshot.getKey());

                            // Formatea la hora si no existe
                            if (mensaje.getHora() == null && mensaje.getFecha_envio() > 0) {
                                mensaje.setHora(convertirTimestampAHora(mensaje.getFecha_envio()));
                            }

                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error procesando mensaje", e);
                    }
                }

                Log.d(TAG, "Nuevos mensajes recibidos: " + messageCount);

                if (!nuevosMensajes.isEmpty()) {
                    notifyMensajesRecibidos(nuevosMensajes);
                } else {
                    Log.d(TAG, "Ignorando notificación duplicada de " + dataSnapshot.getChildrenCount() + " mensajes");
                }
            }




            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error monitoreando conexión: " + error.getMessage());
            }
        });
    }

    /**
     * Obtiene token de Firebase desde el backend
     */
    private void obtenerTokenFirebase() {
        if (idRedActual == null) {
            Log.e(TAG, "ID de red no disponible para obtener token Firebase");
            return;
        }

        try {
            int redId = Integer.parseInt(idRedActual);
            Log.d(TAG, "Solicitando token Firebase para red: " + redId);

            // Llama al endpoint de la API para obtener token
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

                            // Valida token recibido
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
                            authenticateWithFirebase(firebaseToken); // Autentica con Firebase
                        } else {
                            String errorMsg = tokenResponse.getMessage() != null ?
                                    tokenResponse.getMessage() : "Error en respuesta del servidor";
                            Log.e(TAG, errorMsg);
                            notifyError(errorMsg);
                        }
                    } else {
                        // Maneja errores HTTP
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

    /**
     * Autentica en Firebase usando un token personalizado
     * @param token Token de autenticación personalizado
     */
    public void authenticateWithFirebase(String token) {
        Log.d(TAG, "Autenticando con Firebase con token personalizado");

        if (token == null) {
            Log.e(TAG, "ERROR: Token Firebase es NULL!");
            notifyError("Token de autenticación inválido");
            return;
        }

        // Valida formato del token
        String[] tokenParts = token.split("\\.");
        int segmentCount = tokenParts.length;
        Log.d(TAG, "Token Firebase - Segmentos: " + segmentCount + ", Longitud: " + token.length());

        if (segmentCount != 3) {
            Log.e(TAG, "ERROR: Formato de token inválido. Se esperaban 3 segmentos");
            notifyError("Formato de token inválido");
            return;
        }

        // Intenta autenticar con Firebase
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

                        // Maneja errores específicos de Firebase
                        if (e instanceof FirebaseAuthException) {
                            String errorCode = ((FirebaseAuthException) e).getErrorCode();
                            errorMsg += "Código: " + errorCode + " | " + e.getMessage();
                            Log.e(TAG, errorMsg, e);

                            // Reintenta si el token es inválido
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

    /**
     * Establece el callback para eventos del chat
     * @param callback Instancia de ChatCallback
     */
    public void setCallback(ChatCallback callback) {
        Log.d(TAG, "Callback establecido");
        this.callback = callback;
    }

    /**
     * Carga los mensajes iniciales desde la API REST
     */
    public void cargarMensajesIniciales() {
        Log.d(TAG, "Solicitando mensajes iniciales...");

        if (!validateRedId()) {
            Log.e(TAG, "ID de red no válido para cargar mensajes");
            return;
        }

        try {
            int redId = Integer.parseInt(idRedActual);
            Log.d(TAG, "Cargando mensajes iniciales para red: " + redId);

            // Llama a la API para obtener mensajes
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

    /**
     * Verifica si el usuario es miembro de la comunidad
     * @param maxReintentos Número máximo de reintentos (no utilizado en esta implementación)
     */
    public void verificarMembresia(int maxReintentos) {
        Log.d(TAG, "Verificando membresía...");

        if (!validateRedId()) {
            Log.e(TAG, "ID de red no válido para verificar membresía");
            return;
        }

        try {
            int redId = Integer.parseInt(idRedActual);
            Log.d(TAG, "Verificando membresía para red: " + redId);

            // Llama al endpoint de verificación de membresía
            Call<ResponseBody> call = authService.verificarMembresia("Bearer " + apiToken, redId);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    boolean esMiembro = response.isSuccessful() && response.code() == 200;
                    Log.d(TAG, "Membresía verificada: " + (esMiembro ? "MIEMBRO" : "NO MIEMBRO"));
                    notifyMembresiaVerificada(esMiembro); // Notifica resultado
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

    /**
     * Inicia la escucha de nuevos mensajes en Firebase Realtime Database
     */
    public void iniciarEscuchaMensajes() {
        Log.d(TAG, "Intentando iniciar escucha de mensajes...");

        // Detiene cualquier escucha previa
        detenerEscuchaMensajes();

        if (!validateRedId()) {
            Log.e(TAG, "ID de red no válido para iniciar escucha");
            return;
        }

        // Verifica autenticación
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

        // Crea listener para cambios en los mensajes
        mensajesListener = mensajesRef.orderByChild("fecha_envio").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Evento dataChange recibido | Hijos: " + dataSnapshot.getChildrenCount());

                List<Mensaje> nuevosMensajes = new ArrayList<>();
                int messageCount = 0;
                // Procesa cada mensaje
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Mensaje mensaje = snapshot.getValue(Mensaje.class);
                        if (mensaje != null) {
                            mensaje.setId(snapshot.getKey());
                            // Formatea hora si es necesario
                            if (mensaje.getHora() == null && mensaje.getFecha_envio() > 0) {
                                mensaje.setHora(convertirTimestampAHora(mensaje.getFecha_envio()));
                            }

                            if (!mensajesEntregados.contains(mensaje.getId())) {
                                nuevosMensajes.add(mensaje);
                                mensajesEntregados.add(mensaje.getId());
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

    /**
     * Detiene la escucha de mensajes en Firebase
     */
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

    /**
     * Convierte timestamp a formato de hora legible
     * @param timestamp Marca de tiempo en milisegundos
     * @return Hora formateada (hh:mm a)
     */
    private String convertirTimestampAHora(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            Log.e(TAG, "Error formateando hora", e);
            return "";
        }
    }

    /**
     * Envía un mensaje a través de la API
     * @param mensaje Objeto Mensaje a enviar
     */
    public void enviarMensaje(Mensaje mensaje) {
        if (mensaje == null) {
            Log.e(TAG, "Intento de enviar mensaje nulo");
            return;
        }

        Log.d(TAG, "Preparando envío de mensaje: " + mensaje.getContenido()
                + " | Usuario: " + mensaje.getIdUsuario()
                + " | Red: " + idRedActual);

        // Crea objeto de petición para la API
        MensajeRequest request = new MensajeRequest(
                mensaje.getContenido(),
                mensaje.getIdUsuario(),
                mensaje.getNombreUsuario()
        );

        // Envía mensaje a través de Retrofit
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
//                    guardarMensajeEnFirebase(mensaje); // Guarda en Firebase si éxito
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
//                    guardarMensajePendiente(mensaje); // Guarda como pendiente si falla
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Fallo de red al enviar mensaje: " + t.getMessage(), t);
//                guardarMensajePendiente(mensaje); // Guarda como pendiente si error de red
            }
        });
    }

    /**
     * Guarda mensaje en Firebase Realtime Database
     * @param mensaje Mensaje a guardar
     */
    private void guardarMensajeEnFirebase(Mensaje mensaje) {
        Log.d(TAG, "Guardando mensaje en Firebase: " + mensaje.getContenido());

        // Crea nueva referencia para el mensaje
        DatabaseReference nuevoMensajeRef = databaseRef.child(idRedActual).child("mensajes").push();

        // Prepara datos para Firebase
        Map<String, Object> mensajeMap = new HashMap<>();
        mensajeMap.put("id", nuevoMensajeRef.getKey());
        mensajeMap.put("nombreUsuario", mensaje.getNombreUsuario());
        mensajeMap.put("contenido", mensaje.getContenido());
        mensajeMap.put("hora", mensaje.getHora() != null ? mensaje.getHora() : convertirTimestampAHora(System.currentTimeMillis()));
        mensajeMap.put("comunidad", mensaje.getComunidad());
        mensajeMap.put("idUsuario", mensaje.getIdUsuario());
        mensajeMap.put("fecha_envio", System.currentTimeMillis());
        mensajeMap.put("estado", "enviado");

        // Guarda en Firebase
        nuevoMensajeRef.setValue(mensajeMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Mensaje guardado en Firebase");
                    notifyMensajeEnviado();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error guardando mensaje en Firebase", e);
                    guardarMensajePendiente(mensaje); // Guarda como pendiente si falla
                });
    }

    /**
     * Guarda mensaje en la cola de pendientes
     * @param mensaje Mensaje a guardar como pendiente
     */
    private void guardarMensajePendiente(Mensaje mensaje) {
        Log.w(TAG, "Guardando mensaje pendiente: " + mensaje.getContenido());
        DatabaseReference mensajePendienteRef = mensajesPendientesRef.child(idRedActual).push();

        // Prepara datos para Firebase
        Map<String, Object> mensajeMap = new HashMap<>();
        mensajeMap.put("id", mensajePendienteRef.getKey());
        mensajeMap.put("nombreUsuario", mensaje.getNombreUsuario());
        mensajeMap.put("contenido", mensaje.getContenido());
        mensajeMap.put("hora", mensaje.getHora() != null ? mensaje.getHora() : convertirTimestampAHora(System.currentTimeMillis()));
        mensajeMap.put("comunidad", mensaje.getComunidad());
        mensajeMap.put("idUsuario", mensaje.getIdUsuario());
        mensajeMap.put("fecha_envio", System.currentTimeMillis());
        mensajeMap.put("estado", "pendiente");

        // Guarda en pendientes
        mensajePendienteRef.setValue(mensajeMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Mensaje pendiente guardado"))
                .addOnFailureListener(e -> Log.e(TAG, "Error guardando mensaje pendiente", e));
    }

    /**
     * Limpia recursos y listeners
     */
    public void cleanup() {
        Log.d(TAG, "Realizando limpieza...");
        detenerEscuchaMensajes();
        if (connectionListener != null) {
            connectedRef.removeEventListener(connectionListener);
            connectionListener = null;
            Log.d(TAG, "Monitoreo de conexión detenido");
        }
        // Limpia referencias
        idRedActual = null;
        apiToken = null;
        callback = null;
        reintentosAuth = 0;
        Log.d(TAG, "Limpieza completada");
    }

    /**
     * Valida que el ID de red sea numérico
     * @return true si es válido, false si no
     */
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

    // Métodos de notificación (ejecutan callbacks en el hilo principal)

    private void notifyMensajesRecibidos(List<Mensaje> mensajes) {
        int currentSize = mensajes.size();
        long currentTime = System.currentTimeMillis();

        // Verificar si es una notificación duplicada (mismo tamaño y tiempo muy cercano)
        if (currentSize == lastNotifiedSize &&
                (currentTime - lastNotificationTime) < DEBOUNCE_TIME_MS) {
            Log.d(TAG, "Ignorando notificación duplicada de " + currentSize + " mensajes");
            return;
        }

        Log.d(TAG, "Notificando " + currentSize + " mensajes recibidos");
        if (callback != null) {
            mainHandler.post(() -> {
                Log.d(TAG, "Ejecutando callback onMensajesRecibidos");
                callback.onMensajesRecibidos(mensajes);
            });
        } else {
            Log.w(TAG, "Callback no definido para onMensajesRecibidos");
        }

        // Actualizar estado
        lastNotifiedSize = currentSize;
        lastNotificationTime = currentTime;
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