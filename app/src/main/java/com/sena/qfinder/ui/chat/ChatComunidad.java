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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.sena.qfinder.ui.home.Comunidad;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.services.ChatService;
import com.sena.qfinder.data.models.Mensaje;
import com.sena.qfinder.data.models.RedResponse;
import com.sena.qfinder.ui.home.PerfilComunidad;
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
    private static final String TAG = "ChatComunidad";
    private static final int MAX_REINTENTOS = 3;
    private static final long DEDUP_TIME_THRESHOLD = 1000;

    private String nombreComunidad;
    private String idRed;
    private String idUsuario;
    private String nombreUsuario;
    private int reintentosVerificacion = 0;
    private boolean isInitialLoad = true;

    // Componentes de UI
    private RecyclerView recyclerView;
    private MensajeAdapter mensajeAdapter;
    private final List<Mensaje> mensajesActivos = new ArrayList<>();
    private final Set<String> mensajesProcesados = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private LinearLayout layoutAviso;
    private LinearLayout layoutEnviarMensaje;
    private EditText etMensaje;
    private Button btnEnviar;
    private Button btnUnirmeComunidad;
    private Button btnAbandonarComunidad;
    private Button informacion;
    private ImageView volverComunidad;
    private TextView txtTitulo;

    // Servicios
    private ChatService chatService;
    private SharedPreferences sharedPreferences;
    private AuthService authService;
    private BroadcastReceiver notificationReceiver;

    public static ChatComunidad newInstance(String nombreComunidad, String imagenRed) {
        ChatComunidad fragment = new ChatComunidad();
        Bundle args = new Bundle();
        args.putString("nombre_comunidad", nombreComunidad);
        args.putString("imagen_red", imagenRed);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Inicializando fragmento");

        sharedPreferences = requireActivity().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        idUsuario = sharedPreferences.getString("id_usuario", null);

        if (idUsuario == null || idUsuario.equals("-1") || idUsuario.isEmpty()) {
            Log.e(TAG, "ID de usuario no disponible o inválido");
            mostrarErrorYSalir("Error: Sesión no válida");
            return;
        }

        String nombre = sharedPreferences.getString("nombre_usuario", "");
        String apellido = sharedPreferences.getString("apellido_usuario", "");
        nombreUsuario = !nombre.isEmpty() && !apellido.isEmpty() ?
                nombre + " " + apellido : "Usuario";

        inicializarAuthService();

        if (getArguments() != null) {
            nombreComunidad = getArguments().getString("nombre_comunidad");
            String imagenRed = getArguments().getString("imagen_red");
            Log.d(TAG, "Comunidad recibida: " + nombreComunidad + ", Imagen: " + imagenRed);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_comunidad, container, false);

        if (idUsuario == null || idUsuario.equals("-1")) {
            mostrarErrorYSalir("Sesión expirada");
            return view;
        }

        BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.GONE);
        }

        setupViews(view);
        setupRecyclerView();
        setupButtons();
        setupBackButton();
        configurarEstadoInicial();
        configurarComportamientoTeclado();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.VISIBLE);
        }

        unregisterNotificationReceiver();
        mensajesProcesados.clear();

        if (chatService != null) {
            chatService.cleanup();
        }
    }


    private void unregisterNotificationReceiver() {
        if (notificationReceiver != null && LocalBroadcastManager.getInstance(requireContext()) != null) {
            try {
                LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(notificationReceiver);
                notificationReceiver = null; // Limpia la referencia
            } catch (IllegalArgumentException e) {
                // El receptor no estaba registrado, podemos ignorar este error
                Log.w(TAG, "Receiver no estaba registrado o ya fue desregistrado");
            }
        }
    }

    private void setupViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerChat);
        layoutAviso = view.findViewById(R.id.layoutAviso);
        layoutEnviarMensaje = view.findViewById(R.id.layoutEnviarMensaje);
        etMensaje = view.findViewById(R.id.etMensaje);
        btnEnviar = view.findViewById(R.id.btnEnviar);
        btnUnirmeComunidad = view.findViewById(R.id.btnUnirmeComunidad);
        btnAbandonarComunidad = view.findViewById(R.id.btnAbandonarComunidad);
        txtTitulo = view.findViewById(R.id.txtTitulo);
        volverComunidad = view.findViewById(R.id.btnvolver);
        informacion = view.findViewById(R.id.btnInfo);

        ImageView imgComunidadHeader = view.findViewById(R.id.imgComunidad);

        if (nombreComunidad != null) {
            txtTitulo.setText(nombreComunidad);
        }

        if (getArguments() != null && getArguments().getString("imagen_red") != null) {
            String imagenUrl = getArguments().getString("imagen_red");
            Glide.with(this)
                    .load(imagenUrl)
                    .placeholder(R.drawable.imgcomunidad)
                    .error(R.drawable.imgcomunidad)
                    .circleCrop()
                    .into(imgComunidadHeader);
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mensajeAdapter = new MensajeAdapter(mensajesActivos, idUsuario);
        recyclerView.setAdapter(mensajeAdapter);
    }

    private void setupBackButton() {
        if (volverComunidad != null) {
            volverComunidad.setOnClickListener(v -> navigateBack());
        }
    }

    private void setupButtons() {
        btnEnviar.setOnClickListener(v -> {
            if (validarSesion()) {
                enviarMensaje();
            }
        });

        btnUnirmeComunidad.setOnClickListener(v -> {
            if (validarSesion()) {
                unirseAComunidad();
            }
        });

        informacion.setOnClickListener(v -> {
                    if (validarSesion()) {
                        navegarAPerfilComunidad();
                    }
                });

        etMensaje.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                enviarMensaje();
                return true;
            }
            return false;
        });

        btnAbandonarComunidad.setOnClickListener(v -> {
            if (validarSesion()) {
                mostrarDialogoConfirmacionAbandono();
            }
        });
    }


    private void navegarAPerfilComunidad() {
        requireActivity().runOnUiThread(() -> {
            // Obtener los datos necesarios
            String nombre = nombreComunidad;
            String imagenUrl = getArguments() != null ? getArguments().getString("imagen_red") : null;
            String descripcion = sharedPreferences.getString("descripcion_comunidad", "Descripción no disponible");

            // Crear instancia del fragmento PerfilComunidad con los datos
            PerfilComunidad perfilFragment = PerfilComunidad.newInstance(
                    nombre,
                    descripcion,
                    imagenUrl
            );

            // Configurar la transacción
            FragmentTransaction transaction = requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction();

            transaction.replace(R.id.fragment_container, perfilFragment);
            transaction.addToBackStack("chat_a_perfil"); // Para poder volver al chat
            transaction.commit();
        });
    }
    private void mostrarDialogoConfirmacionAbandono() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Abandonar Comunidad")
                .setMessage("¿Estás seguro de que deseas abandonar esta comunidad?")
                .setPositiveButton("Sí", (dialog, which) -> abandonarComunidad())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void abandonarComunidad() {
        if (idRed == null || idRed.isEmpty()) {
            showErrorDialog("Error", "ID de red no disponible");
            return;
        }

        try {
            String token = "Bearer " + sharedPreferences.getString("token", "");
            int redId = Integer.parseInt(idRed);

            ProgressDialog progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("Abandonando la comunidad...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            Call<ResponseBody> call = authService.abandonarRed(token, redId);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    progressDialog.dismiss();
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Abandono exitoso de la comunidad");
                        limpiarEstadoUnion(nombreComunidad);

                        if (chatService != null) {
                            chatService.cleanup();
                            chatService = null;
                        }

                        showToast("Has abandonado la comunidad");
                        redirigirAListaComunidades();
                    } else {
                        handleAbandonoError(response);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error de conexión al abandonar comunidad", t);
                    showRetryDialog("Error de conexión",
                            "No se pudo conectar al servidor...",
                            () -> abandonarComunidad());
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "ID de red inválido: " + idRed, e);
            showErrorDialog("Error", "ID de red inválido");
        }
    }

    private void redirigirAListaComunidades() {
        requireActivity().runOnUiThread(() -> {
            // Mostrar la bottom navigation
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);
                bottomNav.setSelectedItemId(R.id.nav_comunidad);
            }

            // Crear nueva instancia del fragmento Comunidad
            Fragment comunidadFragment = new Comunidad();

            // Reemplazar el fragmento actual
            FragmentTransaction transaction = requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction();

            transaction.replace(R.id.fragment_container, comunidadFragment);
            transaction.commit();
        });
    }

    private void handleAbandonoError(Response<ResponseBody> response) {
        String errorMsg = "Error al abandonar: " + response.code();
        try {
            if (response.errorBody() != null) {
                errorMsg += " - " + response.errorBody().string();
            }
        } catch (Exception e) {
            errorMsg += " (error al leer cuerpo)";
        }
        Log.e(TAG, errorMsg);
        showErrorDialog("Error al abandonar",
                "No se pudo completar la solicitud. Por favor, intenta nuevamente.");
    }

    private void configurarComportamientoTeclado() {
        requireActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        final View rootView = getView();
        if (rootView == null) return;

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private int previousKeyboardHeight = -1;

            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keyboardHeight = screenHeight - r.bottom;

                if (Math.abs(keyboardHeight - previousKeyboardHeight) > 100 || previousKeyboardHeight == -1) {
                    previousKeyboardHeight = keyboardHeight;

                    if (keyboardHeight > screenHeight * 0.15) {
                        scrollToBottomWithDelay(150);
                    }
                }
            }
        });
    }

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

    private void mostrarErrorYSalir(String mensaje) {
        showToast(mensaje);
        requireActivity().finish();
    }

    private boolean validarSesion() {
        if (idUsuario == null || idUsuario.equals("-1")) {
            mostrarErrorYSalir("Sesión expirada");
            return false;
        }
        return true;
    }

    private void configurarEstadoInicial() {
        if (!validarSesion()) return;

        boolean estadoLocal = obtenerEstadoUnion(nombreComunidad);
        Log.d(TAG, "Estado local para " + nombreComunidad + ": " + estadoLocal);

        if (estadoLocal) {
            obtenerIdRedDesdeNombre(nombreComunidad);
        } else {
            mostrarOpcionUnirse();
            obtenerIdRedDesdeNombre(nombreComunidad);
        }
    }

    private void obtenerIdRedDesdeNombre(String nombreRed) {
        if (!validarSesion()) return;

        if (authService == null) {
            inicializarAuthService();
            if (authService == null) {
                showErrorDialog("Error", "Servicio no disponible");
                return;
            }
        }

        String token = "Bearer " + sharedPreferences.getString("token", "");
        if (token.equals("Bearer ")) {
            showErrorDialog("Error", "Sesión no válida");
            return;
        }

        Call<RedResponse> call = authService.obtenerIdRedPorNombre(token, nombreRed);
        call.enqueue(new Callback<RedResponse>() {
            @Override
            public void onResponse(Call<RedResponse> call, Response<RedResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RedResponse apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getId_red() > 0) {
                        idRed = String.valueOf(apiResponse.getId_red());
                        Log.d(TAG, "ID de red obtenido: " + idRed);

                        String imagenRed = apiResponse.getImagen_red();
                        String descripcionRed = apiResponse.getDescripcion_red();

                        // Guardar la descripción para usarla luego
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("descripcion_comunidad", descripcionRed);
                        editor.apply();

                        Log.d(TAG, "Imagen de la red: " + imagenRed);
                        mostrarImagenRed(imagenRed);

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

    private void mostrarImagenRed(String imagenUrl) {
        if (getView() == null || imagenUrl == null || imagenUrl.isEmpty()) return;

        ImageView imagenRedView = getView().findViewById(R.id.imgComunidad);
        if (imagenRedView != null) {
            Glide.with(this)
                    .load(imagenUrl)
                    .placeholder(R.drawable.imgcomunidad)
                    .error(R.drawable.imgcomunidad)
                    .into(imagenRedView);
        }
    }

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
            chatService.setCallback(this);
            chatService.verificarMembresia(MAX_REINTENTOS);
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar chat", e);
            showErrorDialog("Error", "No se pudo iniciar el chat");
        }
    }

    @Override
    public void onFirebaseAuthSuccess() {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Autenticación con Firebase exitosa");
            if (chatService != null) {
                chatService.verificarMembresia(MAX_REINTENTOS);
            }
        });
    }

    @Override
    public void onFirebaseAuthFailed(String error) {
        requireActivity().runOnUiThread(() -> {
            Log.e(TAG, "Error en autenticación Firebase: " + error);
            showToast("Error al conectar con el chat. Intentando método alternativo...");
            if (chatService != null) {
                chatService.verificarMembresia(MAX_REINTENTOS);
            }
        });
    }

    @Override
    public void onMembresiaVerificada(boolean esMiembro) {
        requireActivity().runOnUiThread(() -> {
            guardarEstadoUnion(nombreComunidad, esMiembro);

            if (esMiembro) {
                Log.d(TAG, "Usuario es miembro. Iniciando chat...");
                reintentosVerificacion = 0;
                iniciarChat();
            } else {
                Log.d(TAG, "Usuario NO es miembro. Mostrando opción para unirse...");
                mostrarOpcionUnirse();

                if (obtenerEstadoUnion(nombreComunidad)) {
                    handleInconsistenciaMembresia();
                }
            }
        });
    }

    private void handleInconsistenciaMembresia() {
        reintentosVerificacion++;
        Log.w(TAG, "Estado inconsistente: local=unido, servidor=no unido. Reintento " + reintentosVerificacion);

        if (reintentosVerificacion >= MAX_REINTENTOS) {
            reintentosVerificacion = 0;
            limpiarEstadoUnion(nombreComunidad);
            mostrarDialogoInconsistencia();
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (chatService != null) {
                    chatService.verificarMembresia(MAX_REINTENTOS);
                }
            }, 2000);
        }
    }

    @Override
    public void onFirebaseConnected(boolean connected) {
        requireActivity().runOnUiThread(() -> {
            if (!connected) {
                layoutAviso.setVisibility(View.VISIBLE);
                btnUnirmeComunidad.setText("Reconectar");
                btnUnirmeComunidad.setOnClickListener(v -> {
                    if (chatService != null) {
                        chatService.verificarMembresia(MAX_REINTENTOS);
                    }
                });
                layoutEnviarMensaje.setVisibility(View.GONE);
            } else {
                if (chatService != null && obtenerEstadoUnion(nombreComunidad)) {
                    layoutAviso.setVisibility(View.GONE);
                    layoutEnviarMensaje.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onMensajesCargados(List<Mensaje> mensajes) {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Mensajes iniciales cargados: " + mensajes.size());

            int nuevos = 0;
            for (Mensaje mensaje : mensajes) {
                if (!mensajesProcesados.contains(mensaje.getId())) {
                    mensajesProcesados.add(mensaje.getId());
                    mensajesActivos.add(mensaje);
                    nuevos++;
                }
            }

            if (nuevos > 0) {
                mensajeAdapter.notifyDataSetChanged();
                scrollToBottomImmediately();
            }
        });
    }

    private void scrollToBottomWithDelay(int delayMillis) {
        if (mensajesActivos.isEmpty()) return;

        recyclerView.postDelayed(() -> {
            recyclerView.scrollToPosition(mensajesActivos.size() - 1);
        }, delayMillis);
    }

    private void scrollToBottomImmediately() {
        if (mensajesActivos.isEmpty()) return;

        recyclerView.post(() -> {
            recyclerView.scrollToPosition(mensajesActivos.size() - 1);
            recyclerView.post(() -> {
                View lastItem = recyclerView.getLayoutManager().findViewByPosition(mensajesActivos.size() - 1);
                if (lastItem != null) {
                    int target = lastItem.getBottom() + recyclerView.getPaddingBottom();
                    recyclerView.smoothScrollBy(0, target - recyclerView.getHeight());
                }
            });
        });
    }

    @Override
    public void onMensajesRecibidos(List<Mensaje> mensajes) {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Nuevos mensajes recibidos: " + mensajes.size());

            int nuevos = 0;
            int startPos = mensajesActivos.size();

            for (Mensaje mensaje : mensajes) {
                if (!mensajesProcesados.contains(mensaje.getId())) {
                    mensajesProcesados.add(mensaje.getId());
                    mensajesActivos.add(mensaje);
                    nuevos++;
                }
            }

            if (nuevos > 0) {
                mensajeAdapter.notifyItemRangeInserted(startPos, nuevos);
                scrollToBottomImmediately();
            }
        });
    }

    @Override
    public void onError(String error) {
        requireActivity().runOnUiThread(() -> {
            Log.e(TAG, "Error en chat: " + error);
            showToast("Error en el chat: " + error);

            if (error.contains("permiso") || error.contains("membresía")) {
                mostrarOpcionUnirse();
            }
        });
    }

    @Override
    public void onMensajeEnviado() {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Mensaje enviado con éxito");
        });
    }

    private void mostrarOpcionUnirse() {
        requireActivity().runOnUiThread(() -> {
            layoutAviso.setVisibility(View.VISIBLE);
            layoutEnviarMensaje.setVisibility(View.GONE);
            btnUnirmeComunidad.setVisibility(View.VISIBLE);
            btnAbandonarComunidad.setVisibility(View.GONE);
            btnUnirmeComunidad.setText("Unirme");
            btnUnirmeComunidad.setOnClickListener(v -> unirseAComunidad());
        });
    }

    private void unirseAComunidad() {
        if (idRed == null || idRed.isEmpty()) {
            showErrorDialog("Error", "ID de red no disponible");
            return;
        }

        try {
            String token = "Bearer " + sharedPreferences.getString("token", "");
            int redId = Integer.parseInt(idRed);

            ProgressDialog progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("Uniéndose a la comunidad...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            Call<ResponseBody> call = authService.unirseRed(token, redId);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    progressDialog.dismiss();
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Unión exitosa a la comunidad");
                        guardarEstadoUnion(nombreComunidad, true);
                        if (chatService != null) {
                            iniciarServicioChat();
                        }
                    } else {
                        handleUnirseError(response);
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

    private void iniciarChat() {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Iniciando chat para comunidad: " + nombreComunidad);
            layoutAviso.setVisibility(View.GONE);
            layoutEnviarMensaje.setVisibility(View.VISIBLE);
            btnUnirmeComunidad.setVisibility(View.GONE);
            btnAbandonarComunidad.setVisibility(View.VISIBLE);

            if (chatService != null) {
                chatService.detenerEscuchaMensajes();
                chatService.iniciarEscuchaMensajes();
            }
        });
    }

    private void enviarMensaje() {
        if (idUsuario == null || idUsuario.isEmpty() || nombreUsuario == null || nombreUsuario.isEmpty()) {
            showToast("Información de usuario no disponible");
            return;
        }

        String contenido = etMensaje.getText().toString().trim();
        if (contenido.isEmpty()) return;

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
        nuevoMensaje.setEstado("enviando");

        etMensaje.setText("");

        if (chatService != null) {
            try {
                chatService.enviarMensaje(nuevoMensaje);
                Log.d(TAG, "Mensaje enviado con ID: " + mensajeId);
            } catch (Exception e) {
                Log.e(TAG, "Error al enviar mensaje", e);
                nuevoMensaje.setEstado("error");
                showToast("Error al enviar mensaje");
            }
        }
    }

    private void mostrarDialogoInconsistencia() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Problema de membresía")
                .setMessage("Hubo un problema verificando tu membresía. Por favor, únete a la comunidad nuevamente.")
                .setPositiveButton("Aceptar", null)
                .show();
    }

    private void guardarEstadoUnion(String nombreComunidad, boolean unido) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UnionComunidad", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(nombreComunidad, unido).apply();
    }

    private boolean obtenerEstadoUnion(String nombreComunidad) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UnionComunidad", Context.MODE_PRIVATE);
        return prefs.getBoolean(nombreComunidad, false);
    }

    private void limpiarEstadoUnion(String nombreComunidad) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UnionComunidad", Context.MODE_PRIVATE);
        prefs.edit().remove(nombreComunidad).apply();
    }

    private void showToast(String message) {
        // Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Aceptar", null)
                .show();
    }

    private void showRetryDialog(String title, String message, Runnable onRetry) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Reintentar", (dialog, which) -> onRetry.run())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void navigateBack() {
        redirigirAListaComunidades();
    }
}