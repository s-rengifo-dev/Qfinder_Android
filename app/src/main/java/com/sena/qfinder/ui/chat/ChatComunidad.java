package com.sena.qfinder.ui.chat;

// Importaciones necesarias
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.sena.qfinder.ui.home.Comunidad;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.services.ChatService;
import com.sena.qfinder.data.models.Mensaje;
import com.sena.qfinder.data.models.RedResponse;
import com.sena.qfinder.utils.Constants;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatComunidad extends Fragment implements ChatService.ChatCallback {
    // Variables y constantes
    private static final String TAG = "ChatComunidad"; // Etiqueta para logs
    private static final int MAX_REINTENTOS = 3; // Máximo de reintentos para verificar membresía
    private static final long DEDUP_TIME_THRESHOLD = 1000; // Umbral de tiempo para detección de mensajes duplicados (1 segundo)

    private String nombreComunidad; // Nombre de la comunidad actual
    private String idRed; // ID de la red (comunidad) actual
    private String idUsuario; // ID del usuario actual
    private String nombreUsuario; // Nombre completo del usuario
    private int reintentosVerificacion = 0; // Contador de reintentos para verificación de membresía
    private boolean isInitialLoad = true; // Bandera para la carga inicial de mensajes

    // Componentes de UI
    private RecyclerView recyclerView;
    private MensajeAdapter mensajeAdapter;
    private final List<Mensaje> mensajesActivos = new ArrayList<>(); // Lista de mensajes mostrados
    private final Set<String> mensajesProcesados = Collections.newSetFromMap(new ConcurrentHashMap<>()); // IDs de mensajes ya procesados (para evitar duplicados)
    private LinearLayout layoutAviso;
    private LinearLayout layoutEnviarMensaje;
    private EditText etMensaje;
    private Button btnEnviar;
    private Button btnUnirmeComunidad;
    private ImageView volverComunidad;
    private TextView txtTitulo;

    // Servicios
    private ChatService chatService; // Servicio para operaciones de chat
    private SharedPreferences sharedPreferences; // Preferencias para obtener datos del usuario
    private AuthService authService; // Servicio para autenticación y operaciones de red
    private BroadcastReceiver notificationReceiver; // Receptor de notificaciones FCM

    /**
     * Factory method para crear una nueva instancia del fragmento
     * @param nombreComunidad Nombre de la comunidad a mostrar en el chat
     * @return Nueva instancia del fragmento ChatComunidad
     */
    public static ChatComunidad newInstance(String nombreComunidad) {
        ChatComunidad fragment = new ChatComunidad();
        Bundle args = new Bundle();
        args.putString("nombre_comunidad", nombreComunidad);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Inicialización del fragmento:
     * - Obtiene preferencias de usuario
     * - Valida sesión de usuario
     * - Recibe parámetros (nombre comunidad)
     * - Inicializa servicio de autenticación
     * - Registra receptor de notificaciones push
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Inicializando fragmento");

        // Obtener SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        // Obtener ID de usuario desde preferencias
        idUsuario = sharedPreferences.getString("id_usuario", null);
        // Validar sesión
        if (idUsuario == null || idUsuario.equals("-1") || idUsuario.isEmpty()) {
            Log.e(TAG, "ID de usuario no disponible o inválido");
            mostrarErrorYSalir("Error: Sesión no válida");
            return;
        }

        // Construir nombre de usuario
        String nombre = sharedPreferences.getString("nombre_usuario", "");
        String apellido = sharedPreferences.getString("apellido_usuario", "");
        nombreUsuario = !nombre.isEmpty() && !apellido.isEmpty() ?
                nombre + " " + apellido : "Usuario";

        inicializarAuthService(); // Inicializar servicio de autenticación

        // Obtener argumentos: nombre de comunidad
        if (getArguments() != null) {
            nombreComunidad = getArguments().getString("nombre_comunidad");
            Log.d(TAG, "Comunidad recibida: " + nombreComunidad);
        }

//        registerNotificationReceiver(); // Registrar receptor de notificaciones
    }

    /**
     * Configuración de la UI:
     * - Infla el layout
     * - Oculta la barra de navegación inferior
     * - Inicializa vistas
     * - Configura RecyclerView
     * - Establece listeners de botones
     * - Verifica estado inicial (unido/no unido a comunidad)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_comunidad, container, false);

        // Validar sesión
        if (idUsuario == null || idUsuario.equals("-1")) {
            mostrarErrorYSalir("Sesión expirada");
            return view;
        }

        // Ocultar barra de navegación inferior
        BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.GONE);
        }

        // Configurar componentes UI
        setupViews(view);
        setupRecyclerView();
        setupButtons();
        setupBackButton();
        configurarEstadoInicial(); // Verificar estado de unión a comunidad
        configurarComportamientoTeclado(); // Adaptar UI a teclado

        return view;
    }

    /**
     * Limpieza al destruir la vista:
     * - Muestra nuevamente la barra de navegación
     * - Anula registro de receptores
     * - Limpia estructuras de datos
     * - Detiene servicios de chat
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Mostrar barra de navegación
        BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.VISIBLE);
        }

        // Limpiar recursos
        unregisterNotificationReceiver();
        mensajesProcesados.clear();

        // Detener servicio de chat
        if (chatService != null) {
            chatService.cleanup();
        }
    }

    /**
     * Registrar receptor para notificaciones push de mensajes
//     */
    private void registerNotificationReceiver() {
        notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Procesar notificaciones de mensajes para esta comunidad
                if (intent != null && Constants.FCM_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
                    handleIncomingNotification(intent);
                }
            }
        };

        // Registrar con LocalBroadcastManager
        IntentFilter filter = new IntentFilter(Constants.FCM_NOTIFICATION_RECEIVED);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(notificationReceiver, filter);
    }

    /**
     * Anular registro del receptor de notificaciones
     */
    private void unregisterNotificationReceiver() {
        if (notificationReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(notificationReceiver);
        }
    }

    /**
     * Procesar notificación entrante de mensaje
     * @param intent Intent con datos de la notificación
     */
    private void handleIncomingNotification(Intent intent) {
        // Extraer datos de la notificación
        String type = intent.getStringExtra("type");
        String comunidadId = intent.getStringExtra("comunidadId");

        // Filtrar solo mensajes de chat para esta comunidad
        if ("chat".equals(type) && idRed != null && idRed.equals(comunidadId)) {
            // Construir objeto Mensaje
            Mensaje nuevoMensaje = new Mensaje();
            nuevoMensaje.setId(intent.getStringExtra("mensajeId"));
            nuevoMensaje.setNombreUsuario(intent.getStringExtra("senderName"));
            nuevoMensaje.setContenido(intent.getStringExtra("message"));

            // Formatear hora si es necesario
            String hora = intent.getStringExtra("hora");
            if (hora == null) {
                hora = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
            }
            nuevoMensaje.setHora(hora);

            nuevoMensaje.setComunidad(nombreComunidad);
            nuevoMensaje.setIdUsuario(intent.getStringExtra("senderId"));
            nuevoMensaje.setFecha_envio(intent.getLongExtra("fecha_envio", System.currentTimeMillis()));
            nuevoMensaje.setEstado("recibido");

            agregarMensajeConDeduplicacion(nuevoMensaje); // Agregar con control de duplicados
        }
    }

    /**
     * Agregar mensaje evitando duplicados
     * @param mensaje Mensaje a agregar
     */
    private void agregarMensajeConDeduplicacion(Mensaje mensaje) {
        if (mensaje == null || mensaje.getId() == null) return;

        // Verificar por ID
        if (mensajesProcesados.contains(mensaje.getId())) {
            Log.d(TAG, "Mensaje duplicado ignorado (ID): " + mensaje.getId());
            return;
        }

        // Verificar por contenido y timestamp
        for (Mensaje existente : mensajesActivos) {
            if (existeMensajeDuplicado(existente, mensaje)) {
                Log.d(TAG, "Mensaje duplicado ignorado (contenido): " + mensaje.getContenido());
                return;
            }
        }

        // Agregar a la UI
        requireActivity().runOnUiThread(() -> {
            if (mensajesProcesados.add(mensaje.getId())) {
                mensajesActivos.add(mensaje);
                mensajeAdapter.notifyItemInserted(mensajesActivos.size() - 1);
                scrollToBottomImmediately(); // Desplazar al final
            }
        });
    }

    /**
     * Comprobar si un mensaje es duplicado de otro
     * @param existente Mensaje existente en la lista
     * @param nuevo Nuevo mensaje a comparar
     * @return true si se considera duplicado
     */
    private boolean existeMensajeDuplicado(Mensaje existente, Mensaje nuevo) {
        // Comparación por ID
        if (existente.getId() != null && nuevo.getId() != null &&
                existente.getId().equals(nuevo.getId())) {
            return true;
        }

        // Comparación por contenido + timestamp
        boolean mismoContenido = existente.getContenido().equals(nuevo.getContenido());
        boolean mismoUsuario = existente.getIdUsuario().equals(nuevo.getIdUsuario());
        boolean mismoComunidad = existente.getComunidad().equals(nuevo.getComunidad());
        boolean tiempoCercano = Math.abs(existente.getFecha_envio() - nuevo.getFecha_envio()) < DEDUP_TIME_THRESHOLD;

        return mismoContenido && mismoUsuario && mismoComunidad && tiempoCercano;
    }

    /**
     * Inicializar referencias a componentes UI
     * @param view Vista raíz del fragmento
     */
    private void setupViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerChat);
        layoutAviso = view.findViewById(R.id.layoutAviso);
        layoutEnviarMensaje = view.findViewById(R.id.layoutEnviarMensaje);
        etMensaje = view.findViewById(R.id.etMensaje);
        btnEnviar = view.findViewById(R.id.btnEnviar);
        btnUnirmeComunidad = view.findViewById(R.id.btnUnirmeComunidad);
        txtTitulo = view.findViewById(R.id.txtTitulo);
        volverComunidad = view.findViewById(R.id.btnvolver);

        // Establecer título con nombre de comunidad
        if (nombreComunidad != null) {
            txtTitulo.setText(nombreComunidad);
        }
    }

    /**
     * Configurar RecyclerView y adaptador
     */
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mensajeAdapter = new MensajeAdapter(mensajesActivos, idUsuario);

        recyclerView.setAdapter(mensajeAdapter);
    }

    /**
     * Configurar botón de retroceso
     */
    private void setupBackButton() {
        if (volverComunidad != null) {
            volverComunidad.setOnClickListener(v -> navigateBack());
        }
    }

    /**
     * Configurar listeners de botones
     */
    private void setupButtons() {
        // Botón enviar mensaje
        btnEnviar.setOnClickListener(v -> {
            if (validarSesion()) {
                enviarMensaje();
            }
        });

        // Botón unirse a comunidad
        btnUnirmeComunidad.setOnClickListener(v -> {
            if (validarSesion()) {
                unirseAComunidad();
            }
        });

        // Enviar al presionar "enter" en teclado
        etMensaje.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                enviarMensaje();
                return true;
            }
            return false;
        });
    }

    /**
     * Adaptar UI a aparición/desaparición de teclado
     */
    private void configurarComportamientoTeclado() {
        requireActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        final View rootView = getView();
        if (rootView == null) return;

        // Listener para cambios en el teclado
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private int previousKeyboardHeight = -1;

            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keyboardHeight = screenHeight - r.bottom;

                // Detectar cambios significativos
                if (Math.abs(keyboardHeight - previousKeyboardHeight) > 100 || previousKeyboardHeight == -1) {
                    previousKeyboardHeight = keyboardHeight;

                    // Si teclado visible, desplazar al final
                    if (keyboardHeight > screenHeight * 0.15) {
                        scrollToBottomWithDelay(150);
                    }
                }
            }
        });
    }

    /**
     * Inicializar servicio de autenticación (Retrofit)
     */
    private void inicializarAuthService() {
        try {
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

            authService = retrofit.create(AuthService.class);
            Log.d(TAG, "AuthService inicializado correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando AuthService", e);
            mostrarErrorYSalir("Error al inicializar el servicio");
        }
    }

    /**
     * Mostrar error y cerrar actividad
     * @param mensaje Mensaje de error a mostrar
     */
    private void mostrarErrorYSalir(String mensaje) {
        showToast(mensaje);
        requireActivity().finish();
    }

    /**
     * Validar sesión de usuario
     * @return true si sesión es válida
     */
    private boolean validarSesion() {
        if (idUsuario == null || idUsuario.equals("-1")) {
            mostrarErrorYSalir("Sesión expirada");
            return false;
        }
        return true;
    }

    /**
     * Configurar estado inicial del fragmento
     * (Verificar si usuario está unido a la comunidad)
     */
    private void configurarEstadoInicial() {
        if (!validarSesion()) return;

        // Obtener estado desde SharedPreferences
        boolean estadoLocal = obtenerEstadoUnion(nombreComunidad);
        Log.d(TAG, "Estado local para " + nombreComunidad + ": " + estadoLocal);

        if (estadoLocal) {
            // Si está unido: obtener ID de red e iniciar chat
            obtenerIdRedDesdeNombre(nombreComunidad);
        } else {
            // Si no está unido: mostrar opción de unión
            mostrarOpcionUnirse();
            obtenerIdRedDesdeNombre(nombreComunidad);
        }
    }

    /**
     * Obtener ID de red desde backend usando nombre
     * @param nombreRed Nombre de la comunidad
     */
    private void obtenerIdRedDesdeNombre(String nombreRed) {
        if (!validarSesion()) return;

        // Validar servicio de autenticación
        if (authService == null) {
            inicializarAuthService();
            if (authService == null) {
                showErrorDialog("Error", "Servicio no disponible");
                return;
            }
        }

        // Obtener token de autenticación
        String token = "Bearer " + sharedPreferences.getString("token", "");
        if (token.equals("Bearer ")) {
            showErrorDialog("Error", "Sesión no válida");
            return;
        }

        // Llamada al backend
        Call<RedResponse> call = authService.obtenerIdRedPorNombre(token, nombreRed);
        call.enqueue(new Callback<RedResponse>() {
            @Override
            public void onResponse(Call<RedResponse> call, Response<RedResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RedResponse apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getId_red() > 0) {
                        idRed = String.valueOf(apiResponse.getId_red());
                        Log.d(TAG, "ID de red obtenido: " + idRed);

                        // Si usuario está unido, iniciar chat
                        if (obtenerEstadoUnion(nombreComunidad)) {
                            iniciarServicioChat();
                        }
                    } else {
                        Log.e(TAG, "Error del servidor: " + apiResponse.getMessage());
                        showErrorDialog("Error", apiResponse.getMessage());
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                        Log.e(TAG, "Error en respuesta: " + response.code() + " - " + errorBody);
                        showErrorDialog("Error", "Error al obtener información");
                    } catch (IOException e) {
                        Log.e(TAG, "Error al leer errorBody", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<RedResponse> call, Throwable t) {
                Log.e(TAG, "Error de red: ", t);
                showErrorDialog("Error", "Problema de conexión");
            }
        });
    }

    /**
     * Iniciar servicio de chat
     */
    private void iniciarServicioChat() {
        if (idRed == null || idRed.isEmpty()) {
            showErrorDialog("Error", "ID de red no disponible");
            return;
        }

        String token = sharedPreferences.getString("token", "");
        if (token.isEmpty()) {
            showErrorDialog("Error", "Sesión no válida");
            return;
        }

        try {
            chatService = new ChatService(requireContext(), idRed, token);
            chatService.setCallback(this); // Establecer este fragmento como callback
            chatService.verificarMembresia(MAX_REINTENTOS); // Iniciar verificación
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar chat", e);
            showErrorDialog("Error", "No se pudo iniciar el chat");
        }
    }

    /**
     * Callback: Autenticación con Firebase exitosa
     */
    @Override
    public void onFirebaseAuthSuccess() {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Autenticación con Firebase exitosa");
            if (chatService != null) {
                chatService.verificarMembresia(MAX_REINTENTOS); // Verificar membresía
            }
        });
    }

    /**
     * Callback: Error en autenticación con Firebase
     * @param error Mensaje de error
     */
    @Override
    public void onFirebaseAuthFailed(String error) {
        requireActivity().runOnUiThread(() -> {
            Log.e(TAG, "Error en autenticación Firebase: " + error);
            showToast("Error al conectar con el chat. Intentando método alternativo...");
            if (chatService != null) {
                chatService.verificarMembresia(MAX_REINTENTOS); // Intentar de todos modos
            }
        });
    }

    /**
     * Callback: Resultado de verificación de membresía
     * @param esMiembro Indica si el usuario es miembro de la comunidad
     */
    @Override
    public void onMembresiaVerificada(boolean esMiembro) {
        requireActivity().runOnUiThread(() -> {
            guardarEstadoUnion(nombreComunidad, esMiembro); // Persistir estado

            if (esMiembro) {
                Log.d(TAG, "Usuario es miembro. Iniciando chat...");
                reintentosVerificacion = 0; // Resetear contador
                iniciarChat(); // Iniciar chat
            } else {
                Log.d(TAG, "Usuario NO es miembro. Mostrando opción para unirse...");
                mostrarOpcionUnirse(); // Mostrar UI de unión

                // Manejar inconsistencia (estado local vs servidor)
                if (obtenerEstadoUnion(nombreComunidad)) {
                    handleInconsistenciaMembresia();
                }
            }
        });
    }

    /**
     * Manejar inconsistencia entre estado local y servidor
     */
    private void handleInconsistenciaMembresia() {
        reintentosVerificacion++;
        Log.w(TAG, "Estado inconsistente: local=unido, servidor=no unido. Reintento " + reintentosVerificacion);

        if (reintentosVerificacion >= MAX_REINTENTOS) {
            reintentosVerificacion = 0;
            limpiarEstadoUnion(nombreComunidad); // Limpiar estado local
            mostrarDialogoInconsistencia(); // Notificar al usuario
        } else {
            // Reintentar después de retraso
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (chatService != null) {
                    chatService.verificarMembresia(MAX_REINTENTOS);
                }
            }, 2000);
        }
    }

    /**
     * Callback: Cambio en estado de conexión Firebase
     * @param connected Indica si hay conexión con Firebase
     */
    @Override
    public void onFirebaseConnected(boolean connected) {
        requireActivity().runOnUiThread(() -> {
            if (!connected) {
                // Mostrar UI de reconexión
                layoutAviso.setVisibility(View.VISIBLE);
                btnUnirmeComunidad.setText("Reconectar");
                btnUnirmeComunidad.setOnClickListener(v -> {
                    if (chatService != null) {
                        chatService.verificarMembresia(MAX_REINTENTOS);
                    }
                });
                layoutEnviarMensaje.setVisibility(View.GONE); // Ocultar área de envío
            } else {
                // Si hay conexión y es miembro, mostrar chat
                if (chatService != null && obtenerEstadoUnion(nombreComunidad)) {
                    layoutAviso.setVisibility(View.GONE);
                    layoutEnviarMensaje.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /**
     * Callback: Mensajes iniciales cargados
     * @param mensajes Lista de mensajes históricos
     */
    @Override
    public void onMensajesCargados(List<Mensaje> mensajes) {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Mensajes iniciales cargados: " + mensajes.size());

            int nuevos = 0;
            // Agregar mensajes evitando duplicados
            for (Mensaje mensaje : mensajes) {
                if (!mensajesProcesados.contains(mensaje.getId())) {
                    mensajesProcesados.add(mensaje.getId());
                    mensajesActivos.add(mensaje);
                    nuevos++;
                }
            }

            // Actualizar UI si hay nuevos mensajes
            if (nuevos > 0) {
                mensajeAdapter.notifyDataSetChanged();
                scrollToBottomImmediately(); // Desplazar al final
            }
        });
    }

    /**
     * Desplazar al final de la lista con retraso
     * @param delayMillis Retraso en milisegundos
     */
    private void scrollToBottomWithDelay(int delayMillis) {
        if (mensajesActivos.isEmpty()) return;

        recyclerView.postDelayed(() -> {
            recyclerView.scrollToPosition(mensajesActivos.size() - 1);
        }, delayMillis);
    }

    /**
     * Desplazar al final de la lista inmediatamente
     */
    private void scrollToBottomImmediately() {
        if (mensajesActivos.isEmpty()) return;

        recyclerView.post(() -> {
            recyclerView.scrollToPosition(mensajesActivos.size() - 1);
            // Asegurar que último elemento sea completamente visible
            recyclerView.post(() -> {
                View lastItem = recyclerView.getLayoutManager().findViewByPosition(mensajesActivos.size() - 1);
                if (lastItem != null) {
                    int target = lastItem.getBottom() + recyclerView.getPaddingBottom();
                    recyclerView.smoothScrollBy(0, target - recyclerView.getHeight());
                }
            });
        });
    }

    /**
     * Callback: Nuevos mensajes recibidos en tiempo real
     * @param mensajes Lista de nuevos mensajes
     */
    @Override
    public void onMensajesRecibidos(List<Mensaje> mensajes) {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Nuevos mensajes recibidos: " + mensajes.size());

            int nuevos = 0;
            int startPos = mensajesActivos.size();

            // Agregar mensajes evitando duplicados
            for (Mensaje mensaje : mensajes) {
                if (!mensajesProcesados.contains(mensaje.getId())) {
                    mensajesProcesados.add(mensaje.getId());
                    mensajesActivos.add(mensaje);
                    nuevos++;
                }
            }

            // Actualizar UI si hay nuevos mensajes
            if (nuevos > 0) {
                mensajeAdapter.notifyItemRangeInserted(startPos, nuevos);
                scrollToBottomImmediately(); // Desplazar al final
            }
        });
    }

    /**
     * Callback: Error en servicio de chat
     * @param error Mensaje de error
     */
    @Override
    public void onError(String error) {
        requireActivity().runOnUiThread(() -> {
            Log.e(TAG, "Error en chat: " + error);
            showToast("Error en el chat: " + error);

            // Si error es por permisos, mostrar opción de unirse
            if (error.contains("permiso") || error.contains("membresía")) {
                mostrarOpcionUnirse();
            }
        });
    }

    /**
     * Callback: Mensaje enviado exitosamente
     */
    @Override
    public void onMensajeEnviado() {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Mensaje enviado con éxito");
        });
    }

    /**
     * Mostrar UI para unirse a la comunidad
     */
    private void mostrarOpcionUnirse() {
        requireActivity().runOnUiThread(() -> {
            layoutAviso.setVisibility(View.VISIBLE);
            layoutEnviarMensaje.setVisibility(View.GONE);
            btnUnirmeComunidad.setVisibility(View.VISIBLE);
            btnUnirmeComunidad.setText("Unirme");
            btnUnirmeComunidad.setOnClickListener(v -> unirseAComunidad());
        });
    }

    /**
     * Proceso para unirse a la comunidad
     */
    private void unirseAComunidad() {
        if (idRed == null || idRed.isEmpty()) {
            showErrorDialog("Error", "ID de red no disponible");
            return;
        }

        try {
            String token = "Bearer " + sharedPreferences.getString("token", "");
            int redId = Integer.parseInt(idRed);

            // Mostrar diálogo de progreso
            ProgressDialog progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("Uniéndose a la comunidad...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Llamada API para unirse
            Call<ResponseBody> call = authService.unirseRed(token, redId);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    progressDialog.dismiss();
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Unión exitosa a la comunidad");
                        guardarEstadoUnion(nombreComunidad, true); // Guardar estado
                        if (chatService != null) {
                            iniciarServicioChat(); // Iniciar chat
                        }
                    } else {
                        handleUnirseError(response); // Manejar error
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error de conexión al unirse a comunidad", t);
                    showRetryDialog("Error de conexión", "No se pudo conectar...", () -> unirseAComunidad());
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "ID de red inválido: " + idRed, e);
            showErrorDialog("Error", "ID de red inválido");
        }
    }

    /**
     * Manejar error en unión a comunidad
     * @param response Respuesta de error
     */
    private void handleUnirseError(Response<ResponseBody> response) {
        String errorMsg = "Error al unirse: " + response.code();
        try {
            if (response.errorBody() != null) {
                errorMsg += " - " + response.errorBody().string();
            }
        } catch (Exception e) {
            errorMsg += " (error al leer cuerpo)";
        }
        Log.e(TAG, errorMsg);
        showRetryDialog("Error al unirse",
                "No se pudo completar la unión a la comunidad. Por favor, intenta nuevamente.",
                this::unirseAComunidad);
    }

    /**
     * Iniciar chat: mostrar área de mensajes
     */
    private void iniciarChat() {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Iniciando chat para comunidad: " + nombreComunidad);
            layoutAviso.setVisibility(View.GONE);
            layoutEnviarMensaje.setVisibility(View.VISIBLE);
            btnUnirmeComunidad.setVisibility(View.GONE);

            // Iniciar escucha de mensajes en tiempo real
            if (chatService != null) {
                chatService.detenerEscuchaMensajes();
                chatService.iniciarEscuchaMensajes();
            }
        });
    }

    /**
     * Enviar nuevo mensaje
     */
    private void enviarMensaje() {
        // Validar información de usuario
        if (idUsuario == null || idUsuario.isEmpty() || nombreUsuario == null || nombreUsuario.isEmpty()) {
            showToast("Información de usuario no disponible");
            return;
        }

        String contenido = etMensaje.getText().toString().trim();
        if (contenido.isEmpty()) return;

        // Crear objeto Mensaje
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String horaActual = sdf.format(new Date());
        long fechaEnvio = System.currentTimeMillis();

        String mensajeId = idUsuario + "_" + fechaEnvio + "_" + UUID.randomUUID().toString().substring(0, 8);

        Mensaje nuevoMensaje = new Mensaje();
        nuevoMensaje.setId(mensajeId);
        nuevoMensaje.setNombreUsuario(nombreUsuario);
        nuevoMensaje.setContenido(contenido);
        nuevoMensaje.setHora(horaActual);
        nuevoMensaje.setComunidad(nombreComunidad);
        nuevoMensaje.setIdUsuario(idUsuario);
        nuevoMensaje.setFecha_envio(fechaEnvio);
        nuevoMensaje.setEstado("enviando"); // Estado inicial

        etMensaje.setText(""); // Limpiar campo

//        // Agregar localmente con deduplicación
//        if (mensajesProcesados.add(mensajeId)) {
//            mensajesActivos.add(nuevoMensaje);
//            mensajeAdapter.notifyItemInserted(mensajesActivos.size() - 1);
//            scrollToBottomImmediately(); // Desplazar al final
//        }

        // Enviar a través del servicio
        if (chatService != null) {
            try {
                chatService.enviarMensaje(nuevoMensaje);
                Log.d(TAG, "Mensaje enviado con ID: " + mensajeId);
            } catch (Exception e) {
                Log.e(TAG, "Error al enviar mensaje", e);
                nuevoMensaje.setEstado("error"); // Actualizar estado
//                mensajeAdapter.notifyItemChanged(mensajesActivos.size() - 1); // Actualizar UI
                showToast("Error al enviar mensaje");
            }
        }
    }

    /**
     * Mostrar diálogo de inconsistencia en membresía
     */
    private void mostrarDialogoInconsistencia() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Problema de membresía")
                .setMessage("Hubo un problema verificando tu membresía. Por favor, únete a la comunidad nuevamente.")
                .setPositiveButton("Aceptar", null)
                .show();
    }

    /**
     * Guardar estado de unión en SharedPreferences
     * @param nombreComunidad Nombre de la comunidad
     * @param unido Estado de unión
     */
    private void guardarEstadoUnion(String nombreComunidad, boolean unido) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UnionComunidad", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(nombreComunidad, unido).apply();
    }

    /**
     * Obtener estado de unión desde SharedPreferences
     * @param nombreComunidad Nombre de la comunidad
     * @return Estado de unión
     */
    private boolean obtenerEstadoUnion(String nombreComunidad) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UnionComunidad", Context.MODE_PRIVATE);
        return prefs.getBoolean(nombreComunidad, false);
    }

    /**
     * Limpiar estado de unión en SharedPreferences
     * @param nombreComunidad Nombre de la comunidad
     */
    private void limpiarEstadoUnion(String nombreComunidad) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UnionComunidad", Context.MODE_PRIVATE);
        prefs.edit().remove(nombreComunidad).apply();
    }

    /**
     * Mostrar Toast
     * @param message Mensaje a mostrar
     */
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    /**
     * Mostrar diálogo de error
     * @param title Título del diálogo
     * @param message Mensaje del diálogo
     */
    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Aceptar", null)
                .show();
    }

    /**
     * Mostrar diálogo de reintento
     * @param title Título del diálogo
     * @param message Mensaje del diálogo
     * @param onRetry Acción a ejecutar al reintentar
     */
    private void showRetryDialog(String title, String message, Runnable onRetry) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Reintentar", (dialog, which) -> onRetry.run())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Navegar hacia atrás en la pila de fragmentos
     */
    private void navigateBack() {
        FragmentManager fragmentManager = getParentFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack(); // Volver en la pila
        } else {
            // Reemplazar con fragmento de comunidades
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new Comunidad());
            transaction.commit();
        }
    }
}