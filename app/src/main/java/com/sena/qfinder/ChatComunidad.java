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
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
    private static final String ARG_NOMBRE_COMUNIDAD = "nombre_comunidad";
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
    private DatabaseReference mensajesPendientesRef;

    public static ChatComunidad newInstance(String nombreComunidad) {
        ChatComunidad fragment = new ChatComunidad();
        Bundle args = new Bundle();
        args.putString(ARG_NOMBRE_COMUNIDAD, nombreComunidad);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            nombreComunidad = getArguments().getString(ARG_NOMBRE_COMUNIDAD);
            Log.d(TAG, "Comunidad recibida: " + nombreComunidad);
        }

        sharedPreferences = requireActivity().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        idUsuario = sharedPreferences.getString("id_usuario", "");
        nombreUsuario = sharedPreferences.getString("nombre_usuario", "") + " " +
                sharedPreferences.getString("apellido_usuario", "");

        authService = ApiClient.getClient().create(AuthService.class);

        // Inicializar Firebase para mensajes pendientes
        try {
            if (FirebaseApp.getApps(requireContext()).isEmpty()) {
                FirebaseApp.initializeApp(requireContext());
            }
            mensajesPendientesRef = FirebaseDatabase.getInstance().getReference("mensajes_pendientes");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando Firebase", e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        verificarMensajesPendientes();

        return view;
    }

    private void verificarMensajesPendientes() {
        if (mensajesPendientesRef == null || idRed == null) return;

        mensajesPendientesRef.child(idRed).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Mensaje mensajePendiente = snapshot.getValue(Mensaje.class);
                        if (mensajePendiente != null && "pendiente".equals(mensajePendiente.getEstado())) {
                            reenviarMensajePendiente(mensajePendiente, snapshot.getKey());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error verificando mensajes pendientes", databaseError.toException());
            }
        });
    }

    private void reenviarMensajePendiente(Mensaje mensaje, String mensajeId) {
        if (chatService == null || !hayConexionInternet()) return;

        chatService.enviarMensaje(mensaje);
        mensajesPendientesRef.child(idRed).child(mensajeId).removeValue()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Mensaje pendiente eliminado después de reenvío"))
                .addOnFailureListener(e -> Log.e(TAG, "Error eliminando mensaje pendiente", e));
    }

    private boolean hayConexionInternet() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void configurarEstadoInicial() {
        if (obtenerEstadoUnion(nombreComunidad)) {
            Log.d(TAG, "Estado local: Ya unido a " + nombreComunidad);
            obtenerIdRedDesdeNombre(nombreComunidad);
        } else {
            Log.d(TAG, "Estado local: No unido a " + nombreComunidad);
            obtenerIdRedDesdeNombre(nombreComunidad);
        }
    }

    private void obtenerIdRedDesdeNombre(String nombreRed) {
        String token = "Bearer " + sharedPreferences.getString("token", "");
        Log.d(TAG, "Obteniendo ID de red para: " + nombreRed);

        Call<RedResponse> call = authService.obtenerIdRedPorNombre(token, nombreRed);
        call.enqueue(new Callback<RedResponse>() {
            @Override
            public void onResponse(Call<RedResponse> call, Response<RedResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    idRed = String.valueOf(response.body().getId_red());
                    Log.d(TAG, "ID de red obtenido: " + idRed);
                    iniciarServicioChat();
                } else {
                    String errorMsg = "Error al obtener ID de red";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += ": " + response.errorBody().string();
                        } catch (Exception e) {
                            errorMsg += " (error al leer cuerpo)";
                        }
                    }
                    Log.e(TAG, errorMsg);
                    mostrarError("Error al cargar la comunidad", errorMsg);
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
            if (FirebaseApp.getApps(requireContext()).isEmpty()) {
                FirebaseApp.initializeApp(requireContext());
            }

            chatService = new ChatService(requireContext(), idRed, token);
//            chatService.setCallback(this);
            chatService.verificarMembresia();
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar chat", e);
            mostrarError("Error", "No se pudo iniciar el chat");
        }
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
                            chatService.verificarMembresia();
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
        if (!contenido.isEmpty()) {
            if (!hayConexionInternet()) {
                Toast.makeText(getContext(), "Sin conexión a internet. El mensaje se enviará cuando se recupere la conexión.", Toast.LENGTH_LONG).show();
                guardarMensajePendiente(contenido);
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
    }

    private void guardarMensajePendiente(String contenido) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String horaActual = sdf.format(new Date());

        Mensaje mensajePendiente = new Mensaje();
        mensajePendiente.setNombreUsuario(nombreUsuario);
        mensajePendiente.setContenido(contenido);
        mensajePendiente.setHora(horaActual);
        mensajePendiente.setComunidad(nombreComunidad);
        mensajePendiente.setIdUsuario(idUsuario);
        mensajePendiente.setFecha_envio(System.currentTimeMillis());
        mensajePendiente.setEstado("pendiente");

        if (mensajesPendientesRef != null && idRed != null) {
            mensajesPendientesRef.child(idRed).push().setValue(mensajePendiente)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Mensaje guardado como pendiente"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error guardando mensaje pendiente", e));
        }
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

    @Override
    public void onMembresiaVerificada(boolean esMiembro) {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Membresía verificada: " + esMiembro);
            if (esMiembro) {
                reintentosVerificacion = 0;
                guardarEstadoUnion(nombreComunidad, true);
                iniciarChat();
            } else {
                if (obtenerEstadoUnion(nombreComunidad)) {
                    reintentosVerificacion++;
                    Log.w(TAG, "Estado inconsistente: local=unido, servidor=no unido. Reintento " + reintentosVerificacion);

                    if (reintentosVerificacion >= MAX_REINTENTOS) {
                        reintentosVerificacion = 0;
                        limpiarEstadoUnion(nombreComunidad);
                        mostrarDialogoInconsistencia();
                        mostrarOpcionUnirse();
                    } else {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (chatService != null) {
                                chatService.verificarMembresia();
                            }
                        }, 2000);
                    }
                } else {
                    reintentosVerificacion = 0;
                    mostrarOpcionUnirse();
                }
            }
        });
    }

    private void mostrarOpcionUnirse() {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Mostrando opción para unirse");
            layoutAviso.setVisibility(View.VISIBLE);
            layoutEnviarMensaje.setVisibility(View.GONE);
            btnUnirmeComunidad.setVisibility(View.VISIBLE);
        });
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
            chatService.detenerEscuchaMensajes();
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

            // Mostrar estado del mensaje si es relevante
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
//                txtEstado = itemView.findViewById(R.id.txtEstadoMensaje);
            }
        }
    }
}