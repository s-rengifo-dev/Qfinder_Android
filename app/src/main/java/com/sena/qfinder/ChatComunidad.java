package com.sena.qfinder;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.sena.qfinder.R;
import com.sena.qfinder.api.ApiClient;
import com.sena.qfinder.api.AuthService;
import com.sena.qfinder.api.ChatService;
import com.sena.qfinder.models.Mensaje;
import com.sena.qfinder.models.RedResponse;

import java.io.IOException;
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
    private String nombreComunidad;
    private String idRed;
    private String idUsuario;
    private String nombreUsuario;

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
        args.putString(ARG_NOMBRE_COMUNIDAD, nombreComunidad);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            nombreComunidad = getArguments().getString(ARG_NOMBRE_COMUNIDAD);
        }

        sharedPreferences = requireActivity().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        idUsuario = sharedPreferences.getString("id_usuario", "");
        nombreUsuario = sharedPreferences.getString("nombre_usuario", "") + " " +
                sharedPreferences.getString("apellido_usuario", "");

        authService = ApiClient.getClient().create(AuthService.class);
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

        obtenerIdRedDesdeNombre(nombreComunidad);

        return view;
    }

    private void obtenerIdRedDesdeNombre(String nombreRed) {
        String token = "Bearer " + sharedPreferences.getString("token", "");

        Call<RedResponse> call = authService.obtenerIdRedPorNombre(token, nombreRed);
        call.enqueue(new Callback<RedResponse>() {
            @Override
            public void onResponse(Call<RedResponse> call, Response<RedResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    idRed = String.valueOf(response.body().getId_red());
                    iniciarServicioChat();
                } else {
                    Toast.makeText(getContext(), "Error al obtener ID de red", Toast.LENGTH_SHORT).show();
                    try {
                        if (response.errorBody() != null) {
                            Log.e("ChatError", "Error obtenerIdRed: " + response.errorBody().string());
                        }
                    } catch (IOException e) {
                        Log.e("ChatError", "Error al leer errorBody", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<RedResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                Log.e("ChatError", "Error obtenerIdRed", t);
            }
        });
    }

    private void iniciarServicioChat() {
        if (idRed == null || idRed.isEmpty()) {
            Toast.makeText(getContext(), "ID de red no disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = sharedPreferences.getString("token", "");
        if (token.isEmpty()) {
            Toast.makeText(getContext(), "Sesión no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (FirebaseApp.getApps(requireContext()).isEmpty()) {
                FirebaseApp.initializeApp(requireContext());
            }

            chatService = new ChatService(requireContext(), idRed, token);
            chatService.setCallback(this);
            chatService.verificarMembresia();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al inicializar chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("ChatError", "Error al inicializar chat", e);
        }
    }

    private void unirseAComunidad() {
        if (idRed == null || idRed.isEmpty()) {
            Toast.makeText(getContext(), "ID de red no disponible", Toast.LENGTH_SHORT).show();
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
                        guardarEstadoUnion(nombreComunidad, true);
                        iniciarChat();
                    } else {
                        Toast.makeText(getContext(), "Error al unirse: " + response.code(), Toast.LENGTH_SHORT).show();
                        try {
                            if (response.errorBody() != null) {
                                Log.e("ChatError", "Error al unirse: " + response.errorBody().string());
                            }
                        } catch (IOException e) {
                            Log.e("ChatError", "Error al leer errorBody", e);
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                    Log.e("ChatError", "Error al unirse", t);
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "ID de red inválido", Toast.LENGTH_SHORT).show();
            Log.e("ChatError", "ID de red: " + idRed, e);
        }
    }

    private void iniciarChat() {
        requireActivity().runOnUiThread(() -> {
            layoutAviso.setVisibility(View.GONE);
            layoutEnviarMensaje.setVisibility(View.VISIBLE);
            btnUnirmeComunidad.setVisibility(View.GONE);
        });

        if (chatService != null) {
            chatService.iniciarEscuchaMensajes();
        }
    }

    private void enviarMensaje() {
        String contenido = etMensaje.getText().toString().trim();
        if (!contenido.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String horaActual = sdf.format(new Date());

            Mensaje nuevoMensaje = new Mensaje();
            nuevoMensaje.setNombreUsuario(nombreUsuario);
            nuevoMensaje.setContenido(contenido);
            nuevoMensaje.setHora(horaActual);
            nuevoMensaje.setComunidad(nombreComunidad);
            nuevoMensaje.setIdUsuario(idUsuario);
            nuevoMensaje.setFecha_envio(System.currentTimeMillis());

            chatService.enviarMensaje(nuevoMensaje);
            etMensaje.setText("");
        }
    }

    @Override
    public void onMensajesRecibidos(List<Mensaje> mensajes) {
        requireActivity().runOnUiThread(() -> {
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
            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            Log.e("ChatError", error);
        });
    }

    @Override
    public void onMensajeEnviado() {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(), "Mensaje enviado", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onMembresiaVerificada(boolean esMiembro) {
        requireActivity().runOnUiThread(() -> {
            if (esMiembro) {
                iniciarChat();
            } else {
                mostrarOpcionUnirse();
            }
        });
    }

    private void mostrarOpcionUnirse() {
        requireActivity().runOnUiThread(() -> {
            layoutAviso.setVisibility(View.VISIBLE);
            layoutEnviarMensaje.setVisibility(View.GONE);
            btnUnirmeComunidad.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatService != null) {
            chatService.detenerEscuchaMensajes();
        }
    }

    private void guardarEstadoUnion(String nombreComunidad, boolean unido) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UnionComunidad", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(nombreComunidad, unido).apply();
    }

    private boolean obtenerEstadoUnion(String nombreComunidad) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UnionComunidad", Context.MODE_PRIVATE);
        return prefs.getBoolean(nombreComunidad, false);
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
        }

        @Override
        public int getItemCount() {
            return mensajes.size();
        }

        static class MensajeViewHolder extends RecyclerView.ViewHolder {
            TextView txtNombreUsuario, txtContenidoMensaje, txtHoraMensaje;

            MensajeViewHolder(View itemView) {
                super(itemView);
                txtNombreUsuario = itemView.findViewById(R.id.txtNombreUsuario);
                txtContenidoMensaje = itemView.findViewById(R.id.txtContenidoMensaje);
                txtHoraMensaje = itemView.findViewById(R.id.txtHoraMensaje);
            }
        }
    }
}