package com.sena.qfinder;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sena.qfinder.api.AuthService;
import com.sena.qfinder.api.ChatService;
import com.sena.qfinder.models.Mensaje;
import com.sena.qfinder.models.RedResponse;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatComunidad extends Fragment implements ChatService.ChatCallback {
    private static final String TAG = "ChatComunidad";
    private static final int MAX_REINTENTOS = 3;

    private String nombreComunidad;
    private String idRed;
    private String idUsuario;
    private String nombreUsuario;
    private int reintentosVerificacion = 0;

    private RecyclerView recyclerView;
    private MensajeAdapter mensajeAdapter;
    private final List<Mensaje> mensajesActivos = new ArrayList<>();
    private LinearLayout layoutAviso;
    private LinearLayout layoutEnviarMensaje;
    private EditText etMensaje;
    private Button btnEnviar;
    private Button btnUnirmeComunidad;

    private ChatService chatService;
    private SharedPreferences sharedPreferences;
    private AuthService authService;

    public static ChatComunidad newInstance(String nombreComunidad) {
        ChatComunidad fragment = new ChatComunidad();
        Bundle args = new Bundle();
        args.putString("nombre_comunidad", nombreComunidad);
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
            Log.d(TAG, "Comunidad recibida: " + nombreComunidad);
        }
    }

    private void mostrarErrorYSalir(String mensaje) {
        showToast(mensaje);
        requireActivity().finish();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_comunidad, container, false);

        if (idUsuario == null || idUsuario.equals("-1")) {
            mostrarErrorYSalir("Sesión expirada");
            return view;
        }

        setupViews(view);
        setupRecyclerView();
        setupButtons();
        configurarEstadoInicial();

        return view;
    }

    private void setupViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerChat);
        layoutAviso = view.findViewById(R.id.layoutAviso);
        layoutEnviarMensaje = view.findViewById(R.id.layoutEnviarMensaje);
        etMensaje = view.findViewById(R.id.etMensaje);
        btnEnviar = view.findViewById(R.id.btnEnviar);
        btnUnirmeComunidad = view.findViewById(R.id.btnUnirmeComunidad);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mensajeAdapter = new MensajeAdapter(mensajesActivos, idUsuario);
        recyclerView.setAdapter(mensajeAdapter);
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
            Log.e(TAG, "AuthService no inicializado - Reintentando...");
            inicializarAuthService();
            if (authService == null) {
                showErrorDialog("Error", "Servicio no disponible");
                return;
            }
        }

        String token = "Bearer " + sharedPreferences.getString("token", "");
        if (token.equals("Bearer ")) {
            Log.e(TAG, "Token no disponible");
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
            chatService.setCallback(ChatComunidad.this);

            Call<ResponseBody> call = authService.verificarMembresia("Bearer " + token, Integer.parseInt(idRed));
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            JSONObject json = new JSONObject(response.body().string());
                            if (json.getBoolean("success")) {
                                chatService.authenticateWithFirebase(json.getString("firebaseToken"));
                            } else {
                                handleVerificacionFallida();
                            }
                        } else {
                            handleVerificacionFallida();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response", e);
                        handleVerificacionFallida();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e(TAG, "Error verifying membership", t);
                    handleVerificacionFallida();
                }
            });
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

    private void handleVerificacionFallida() {
        Log.e(TAG, "Falló la verificación de membresía con Firebase");
        if (chatService != null) {
            chatService.verificarMembresia(MAX_REINTENTOS);
        }
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
                showToast("Sin conexión con Firebase");
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
                    chatService.iniciarEscuchaMensajes();
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
            mensajesActivos.clear();
            mensajesActivos.addAll(mensajes);
            mensajeAdapter.notifyDataSetChanged();

            if (mensajesActivos.size() > 0) {
                recyclerView.scrollToPosition(mensajesActivos.size() - 1);
            }
        });
    }

    @Override
    public void onMensajesRecibidos(List<Mensaje> mensajes) {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Nuevos mensajes recibidos: " + mensajes.size());

            for (Mensaje mensaje : mensajes) {
                boolean mensajeExistente = false;

                // Buscar si el mensaje ya existe en la lista
                for (int i = 0; i < mensajesActivos.size(); i++) {
                    Mensaje existente = mensajesActivos.get(i);

                    // Comparar por ID si está disponible
                    if (mensaje.getId() != null && mensaje.getId().equals(existente.getId())) {
                        mensajeExistente = true;

                        // Actualizar estado si era un mensaje local "enviando"
                        if ("enviando".equals(existente.getEstado())) {
                            existente.setEstado("enviado");
                            existente.setHora(mensaje.getHora());
                            mensajeAdapter.notifyItemChanged(i);
                        }
                        break;
                    }

                    // Comparar por contenido y tiempo si no hay ID
                    if (mensaje.getContenido().equals(existente.getContenido()) &&
                            Math.abs(mensaje.getFecha_envio() - existente.getFecha_envio()) < 2000 &&
                            mensaje.getIdUsuario().equals(existente.getIdUsuario())) {
                        mensajeExistente = true;
                        break;
                    }
                }

                // Solo agregar si no es un mensaje existente
                if (!mensajeExistente) {
                    mensajesActivos.add(mensaje);
                    mensajeAdapter.notifyItemInserted(mensajesActivos.size() - 1);
                }
            }

            if (!mensajes.isEmpty()) {
                recyclerView.scrollToPosition(mensajesActivos.size() - 1);
            }
        });
    }

    @Override
    public void onError(String error) {
        requireActivity().runOnUiThread(() -> {
            Log.e(TAG, "Error en chat: " + error);
            showToast("Error en el chat: " + error);
        });
    }

    @Override
    public void onMensajeEnviado() {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Mensaje enviado con éxito");
            showToast("Mensaje enviado");
        });
    }

    private void mostrarOpcionUnirse() {
        requireActivity().runOnUiThread(() -> {
            layoutAviso.setVisibility(View.VISIBLE);
            layoutEnviarMensaje.setVisibility(View.GONE);
            btnUnirmeComunidad.setVisibility(View.VISIBLE);
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

            if (chatService != null) {
                chatService.cargarMensajesIniciales();
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
        if (contenido.isEmpty()) {
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String horaActual = sdf.format(new Date());
        long fechaEnvio = System.currentTimeMillis();

        // Generar un ID único para el mensaje con UUID
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

        // Limpiar el campo de texto
        etMensaje.setText("");

        if (chatService != null) {
            // Mostrar el mensaje localmente primero con estado "enviando"
            mensajesActivos.add(nuevoMensaje);
            mensajeAdapter.notifyItemInserted(mensajesActivos.size() - 1);
            recyclerView.scrollToPosition(mensajesActivos.size() - 1);

            // Luego enviar el mensaje al servidor
            chatService.enviarMensaje(nuevoMensaje);
            Log.d(TAG, "Mensaje local agregado con ID: " + mensajeId);
        }
    }

    private boolean hayConexionInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
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
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatService != null) {
            chatService.cleanup();
        }
    }
}