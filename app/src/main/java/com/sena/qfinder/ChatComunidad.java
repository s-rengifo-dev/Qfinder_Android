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

import com.google.firebase.database.DatabaseReference;
import com.sena.qfinder.api.ApiClient;
import com.sena.qfinder.api.AuthService;
import com.sena.qfinder.api.ChatService;
import com.sena.qfinder.models.Mensaje;
import com.sena.qfinder.models.RedResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private List<Mensaje> mensajesActivos = new ArrayList<>();
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

        if (getArguments() != null) {
            nombreComunidad = getArguments().getString("nombre_comunidad");
            Log.d(TAG, "Comunidad recibida: " + nombreComunidad);
        }

        sharedPreferences = requireActivity().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        idUsuario = sharedPreferences.getString("id_usuario", "");
        nombreUsuario = sharedPreferences.getString("nombre_usuario", "") + " " +
                sharedPreferences.getString("apellido_usuario", "");

        authService = ApiClient.getClient().create(AuthService.class);
        Log.d(TAG, "Servicios inicializados");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creando vista");
        View view = inflater.inflate(R.layout.fragment_chat_comunidad, container, false);

        recyclerView = view.findViewById(R.id.recyclerChat);
        layoutAviso = view.findViewById(R.id.layoutAviso);
        layoutEnviarMensaje = view.findViewById(R.id.layoutEnviarMensaje);
        etMensaje = view.findViewById(R.id.etMensaje);
        btnEnviar = view.findViewById(R.id.btnEnviar);
        btnUnirmeComunidad = view.findViewById(R.id.btnUnirmeComunidad);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mensajeAdapter = new MensajeAdapter(mensajesActivos);
        recyclerView.setAdapter(mensajeAdapter);

        btnEnviar.setOnClickListener(v -> enviarMensaje());
        btnUnirmeComunidad.setOnClickListener(v -> unirseAComunidad());

        configurarEstadoInicial();
        return view;
    }

    private void configurarEstadoInicial() {
        Log.d(TAG, "configurarEstadoInicial: Verificando estado inicial");
        boolean estadoLocal = obtenerEstadoUnion(nombreComunidad);
        Log.d(TAG, "Estado local para " + nombreComunidad + ": " + estadoLocal);

        if (estadoLocal) {
            Log.d(TAG, "Estado local indica que está unido. Obteniendo ID de red...");
            obtenerIdRedDesdeNombre(nombreComunidad);
        } else {
            Log.d(TAG, "Estado local indica que NO está unido. Mostrando opción para unirse...");
            mostrarOpcionUnirse();
            obtenerIdRedDesdeNombre(nombreComunidad);
        }
    }

    private void obtenerIdRedDesdeNombre(String nombreRed) {
        Log.d(TAG, "obtenerIdRedDesdeNombre: " + nombreRed);
        String token = "Bearer " + sharedPreferences.getString("token", "");

        Call<RedResponse> call = authService.obtenerIdRedPorNombre(token, nombreRed);
        call.enqueue(new Callback<RedResponse>() {
            @Override
            public void onResponse(Call<RedResponse> call, Response<RedResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    idRed = String.valueOf(response.body().getId_red());
                    Log.d(TAG, "ID de red obtenido: " + idRed);

                    if (obtenerEstadoUnion(nombreComunidad)) {
                        iniciarServicioChat();
                    }
                } else {
                    String errorMsg = "Error al obtener ID de red: " + response.code();
                    Log.e(TAG, errorMsg);
                    mostrarError("Error", "No se pudo obtener información de la comunidad");
                }
            }

            @Override
            public void onFailure(Call<RedResponse> call, Throwable t) {
                Log.e(TAG, "Error de conexión al obtener ID de red", t);
                mostrarError("Error de conexión", "No se pudo conectar al servidor");
            }
        });
    }

    private void iniciarServicioChat() {
        Log.d(TAG, "iniciarServicioChat: ID Red: " + idRed);

        if (idRed == null || idRed.isEmpty()) {
            Log.e(TAG, "ID de red no disponible");
            mostrarError("Error", "ID de red no disponible");
            return;
        }

        String token = sharedPreferences.getString("token", "");
        if (token.isEmpty()) {
            Log.e(TAG, "Token no disponible");
            mostrarError("Error", "Sesión no válida");
            return;
        }

        try {
            chatService = new ChatService(requireContext(), idRed, token);
            chatService.setCallback(this);
            chatService.verificarMembresia(MAX_REINTENTOS);
            Log.d(TAG, "Servicio de chat iniciado correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar chat", e);
            mostrarError("Error", "No se pudo iniciar el chat");
        }
    }

    @Override
    public void onMembresiaVerificada(boolean esMiembro) {
        Log.d(TAG, "onMembresiaVerificada: " + esMiembro);

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
            }
        });
    }

    @Override
    public void onFirebaseConnected(boolean connected) {
        requireActivity().runOnUiThread(() -> {
            if (!connected) {
                Toast.makeText(getContext(), "Sin conexión con Firebase", Toast.LENGTH_SHORT).show();
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

    private void mostrarOpcionUnirse() {
        Log.d(TAG, "mostrarOpcionUnirse: Configurando UI para unirse");
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
            Log.e(TAG, "ID de red no disponible para unirse");
            mostrarError("Error", "ID de red no disponible");
            return;
        }

        try {
            String token = "Bearer " + sharedPreferences.getString("token", "");
            int redId = Integer.parseInt(idRed);

            ProgressDialog progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("Uniéndose a la comunidad...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            Log.d(TAG, "Solicitando unión a red ID: " + redId);
            Call<ResponseBody> call = authService.unirseRed(token, redId);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    progressDialog.dismiss();
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Unión exitosa a la comunidad");
                        guardarEstadoUnion(nombreComunidad, true);
                        if (chatService != null) {
                            chatService.verificarMembresia(3);
                        }
                    } else {
                        String errorMsg = "Error al unirse: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                errorMsg += " - " + response.errorBody().string();
                            }
                        } catch (Exception e) {
                            errorMsg += " (error al leer cuerpo)";
                        }
                        Log.e(TAG, errorMsg);
                        mostrarDialogoError("Error al unirse",
                                "No se pudo completar la unión a la comunidad. Por favor, intenta nuevamente.",
                                () -> unirseAComunidad());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error de conexión al unirse a comunidad", t);
                    mostrarDialogoError("Error de conexión",
                            "No se pudo conectar al servidor. Verifica tu conexión a internet.",
                            () -> unirseAComunidad());
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "ID de red inválido: " + idRed, e);
            mostrarError("Error", "ID de red inválido");
        }
    }

    private void iniciarChat() {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Iniciando chat para comunidad: " + nombreComunidad);
            layoutAviso.setVisibility(View.GONE);
            layoutEnviarMensaje.setVisibility(View.VISIBLE);
            btnUnirmeComunidad.setVisibility(View.GONE);

            if (chatService != null) {
                chatService.iniciarEscuchaMensajes();
            }
        });
    }

    private void enviarMensaje() {
        String contenido = etMensaje.getText().toString().trim();
        if (contenido.isEmpty()) {
            return;
        }

        if (!hayConexionInternet()) {
            Toast.makeText(getContext(), "Sin conexión a internet. El mensaje se enviará cuando se recupere la conexión.", Toast.LENGTH_LONG).show();
            if (chatService != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                String horaActual = sdf.format(new Date());

                Mensaje nuevoMensaje = new Mensaje();
                nuevoMensaje.setNombreUsuario(nombreUsuario);
                nuevoMensaje.setContenido(contenido);
                nuevoMensaje.setHora(horaActual);
                nuevoMensaje.setComunidad(nombreComunidad);
                nuevoMensaje.setIdUsuario(idUsuario);
                nuevoMensaje.setFecha_envio(System.currentTimeMillis());
                nuevoMensaje.setEstado("pendiente");

                chatService.guardarMensajePendiente(nuevoMensaje);
            }
            etMensaje.setText("");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String horaActual = sdf.format(new Date());

        Mensaje nuevoMensaje = new Mensaje();
        nuevoMensaje.setNombreUsuario(nombreUsuario);
        nuevoMensaje.setContenido(contenido);
        nuevoMensaje.setHora(horaActual);
        nuevoMensaje.setComunidad(nombreComunidad);
        nuevoMensaje.setIdUsuario(idUsuario);
        nuevoMensaje.setFecha_envio(System.currentTimeMillis());
        nuevoMensaje.setEstado("enviando");

        Log.d(TAG, "Enviando mensaje: " + contenido);
        if (chatService != null) {
            chatService.enviarMensaje(nuevoMensaje);
        }
        etMensaje.setText("");
    }

    @Override
    public void onMensajesRecibidos(List<Mensaje> mensajes) {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Mensajes recibidos: " + mensajes.size());
            mensajesActivos.clear();
            mensajesActivos.addAll(mensajes);
            mensajeAdapter.notifyDataSetChanged();
            if (mensajesActivos.size() > 0) {
                recyclerView.scrollToPosition(mensajesActivos.size() - 1);
            }
        });
    }

    @Override
    public void onError(String error) {
        requireActivity().runOnUiThread(() -> {
            Log.e(TAG, "Error en chat: " + error);
            mostrarError("Error en el chat", error);
        });
    }

    @Override
    public void onMensajeEnviado() {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Mensaje enviado con éxito");
            Toast.makeText(getContext(), "Mensaje enviado", Toast.LENGTH_SHORT).show();
        });
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

    private void mostrarError(String titulo, String mensaje) {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(), titulo + ": " + mensaje, Toast.LENGTH_LONG).show();
        });
    }

    private void mostrarDialogoError(String titulo, String mensaje, Runnable onRetry) {
        requireActivity().runOnUiThread(() -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(titulo)
                    .setMessage(mensaje)
                    .setPositiveButton("Reintentar", (dialog, which) -> onRetry.run())
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Fragmento destruido");
        if (chatService != null) {
            chatService.cleanup();
        }
    }

    private void guardarEstadoUnion(String nombreComunidad, boolean unido) {
        Log.d(TAG, "Guardando estado de unión: " + nombreComunidad + " = " + unido);
        SharedPreferences prefs = requireActivity().getSharedPreferences("UnionComunidad", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(nombreComunidad, unido).apply();
    }

    private boolean obtenerEstadoUnion(String nombreComunidad) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UnionComunidad", Context.MODE_PRIVATE);
        boolean estado = prefs.getBoolean(nombreComunidad, false);
        Log.d(TAG, "Obteniendo estado de unión: " + nombreComunidad + " = " + estado);
        return estado;
    }

    private void limpiarEstadoUnion(String nombreComunidad) {
        Log.d(TAG, "Limpiando estado de unión para: " + nombreComunidad);
        SharedPreferences prefs = requireActivity().getSharedPreferences("UnionComunidad", Context.MODE_PRIVATE);
        prefs.edit().remove(nombreComunidad).apply();
    }

    private static class MensajeAdapter extends RecyclerView.Adapter<MensajeAdapter.MensajeViewHolder> {
        private final List<Mensaje> mensajes;

        public MensajeAdapter(List<Mensaje> mensajes) {
            this.mensajes = mensajes;
        }

        @NonNull
        @Override
        public MensajeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_mensaje, parent, false);
            return new MensajeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MensajeViewHolder holder, int position) {
            Mensaje mensaje = mensajes.get(position);
            holder.txtNombreUsuario.setText(mensaje.getNombreUsuario());
            holder.txtContenidoMensaje.setText(mensaje.getContenido());
            holder.txtHoraMensaje.setText(mensaje.getHora());

            if ("pendiente".equals(mensaje.getEstado())) {
                holder.txtEstado.setText("(Enviando...)");
                holder.txtEstado.setVisibility(View.VISIBLE);
            } else if ("error".equals(mensaje.getEstado())) {
                holder.txtEstado.setText("(Error al enviar)");
                holder.txtEstado.setVisibility(View.VISIBLE);
            } else {
                holder.txtEstado.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return mensajes.size();
        }

        static class MensajeViewHolder extends RecyclerView.ViewHolder {
            TextView txtNombreUsuario, txtContenidoMensaje, txtHoraMensaje, txtEstado;

            MensajeViewHolder(View itemView) {
                super(itemView);
                txtNombreUsuario = itemView.findViewById(R.id.txtNombreUsuario);
                txtContenidoMensaje = itemView.findViewById(R.id.txtContenidoMensaje);
                txtHoraMensaje = itemView.findViewById(R.id.txtHoraMensaje);
                txtEstado = itemView.findViewById(R.id.txtEstadoMensaje);
            }
        }
    }
}