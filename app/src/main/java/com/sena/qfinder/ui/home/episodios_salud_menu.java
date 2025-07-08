package com.sena.qfinder.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.ApiClient;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.NotaEpisodio;
import com.sena.qfinder.data.models.NotaEpisodioListResponse;
import com.sena.qfinder.data.models.PacienteListResponse;
import com.sena.qfinder.data.models.PacienteResponse;
import com.sena.qfinder.ui.notas.NotaEpisodioAdapter;
import com.sena.qfinder.ui.notas.episodios_salud_nota;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class episodios_salud_menu extends AppCompatActivity {

    private ImageView btnBack;
    private int selectedPatientId = -1;
    private String token;
    private ListView listViewNotas;
    private List<NotaEpisodio> todasLasNotas = new ArrayList<>();
    private List<NotaEpisodio> notasFiltradas = new ArrayList<>();
    private TextView cantidadRegistros;
    private EditText searchInput;
    private NotaEpisodioAdapter notaAdapter;
    private RecyclerView recyclerPacientes;
    private PatientAdapter patientAdapter;
    private List<PacienteResponse> listaPacientes = new ArrayList<>();
    private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episodios_salud_menu);

        // Inicializar vistas
        listViewNotas = findViewById(R.id.listViewNotas);
        cantidadRegistros = findViewById(R.id.cantidadRegistros);
        searchInput = findViewById(R.id.searchInput);
        btnBack = findViewById(R.id.btnBack);
        recyclerPacientes = findViewById(R.id.recyclerPaciente);

        notaAdapter = new NotaEpisodioAdapter(this, notasFiltradas);
        listViewNotas.setAdapter(notaAdapter);

        // Obtener token de autenticación
        SharedPreferences preferences = getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String rawToken = preferences.getString("token", null);
        if (rawToken != null) {
            token = "Bearer " + rawToken;
        } else {
            Toast.makeText(this, "Token no encontrado. Inicia sesión nuevamente.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        configurarBusqueda();
        setupRecyclerPacientes();
        cargarPacientes();

        FloatingActionButton btnNuevaNota = findViewById(R.id.btnNuevaNota);
        btnNuevaNota.setOnClickListener(v -> {
            if (selectedPatientId != -1) {
                Intent intent = new Intent(this, episodios_salud_nota.class);
                intent.putExtra("id_paciente", selectedPatientId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Selecciona un paciente primero.", Toast.LENGTH_SHORT).show();
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerPacientes() {
        recyclerPacientes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        patientAdapter = new PatientAdapter(listaPacientes, paciente -> {
            selectedPatientId = paciente.getId();
            patientAdapter.setSelectedPatientId(selectedPatientId);
            cargarNotasDelPaciente(selectedPatientId);
        });
        recyclerPacientes.setAdapter(patientAdapter);
    }

    private void configurarBusqueda() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                filtrarNotas(s.toString());
            }
        });
    }

    private void cargarPacientes() {
        AuthService authService = ApiClient.getAuthService();
        authService.listarPacientes(token).enqueue(new Callback<PacienteListResponse>() {
            @Override
            public void onResponse(Call<PacienteListResponse> call, Response<PacienteListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listaPacientes.clear();
                    listaPacientes.addAll(response.body().getData());
                    patientAdapter.notifyDataSetChanged();

                    // Seleccionar primer paciente por defecto si hay pacientes
                    if (!listaPacientes.isEmpty()) {
                        selectedPatientId = listaPacientes.get(0).getId();
                        patientAdapter.setSelectedPatientId(selectedPatientId);
                        cargarNotasDelPaciente(selectedPatientId);
                    }
                } else {
                    Toast.makeText(episodios_salud_menu.this, "Error al cargar pacientes", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PacienteListResponse> call, Throwable t) {
                Toast.makeText(episodios_salud_menu.this, "Fallo la conexión al cargar pacientes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarNotasDelPaciente(int idPaciente) {
        AuthService authService = ApiClient.getAuthService();
        authService.obtenerEpisodios(token, idPaciente).enqueue(new Callback<NotaEpisodioListResponse>() {
            @Override
            public void onResponse(Call<NotaEpisodioListResponse> call, Response<NotaEpisodioListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    todasLasNotas.clear();
                    todasLasNotas.addAll(response.body().getData());
                    ordenarNotas();
                    filtrarNotas(searchInput.getText().toString());
                } else {
                    Toast.makeText(episodios_salud_menu.this, "Error al cargar notas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<NotaEpisodioListResponse> call, Throwable t) {
                Toast.makeText(episodios_salud_menu.this, "Error de conexión al cargar notas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void ordenarNotas() {
        Collections.sort(todasLasNotas, (n1, n2) -> {
            try {
                Date d1 = inputFormat.parse(n1.getFechaHoraInicio());
                Date d2 = inputFormat.parse(n2.getFechaHoraInicio());
                return d2.compareTo(d1);
            } catch (Exception e) {
                return 0;
            }
        });
        filtrarNotas(searchInput.getText().toString());
    }

    private void filtrarNotas(String texto) {
        notasFiltradas.clear();
        if (texto == null || texto.trim().isEmpty()) {
            notasFiltradas.addAll(todasLasNotas);
        } else {
            String filtro = texto.toLowerCase(Locale.ROOT);
            for (NotaEpisodio nota : todasLasNotas) {
                boolean coincide = false;
                if (nota.getTitulo() != null && nota.getTitulo().toLowerCase().contains(filtro)) coincide = true;
                else if (nota.getDescripcion() != null && nota.getDescripcion().toLowerCase().contains(filtro)) coincide = true;
                else if (nota.getIntervenciones() != null && nota.getIntervenciones().toLowerCase().contains(filtro)) coincide = true;
                else if (nota.getTipo() != null && nota.getTipo().toLowerCase().contains(filtro)) coincide = true;

                if (coincide) notasFiltradas.add(nota);
            }
        }
        notaAdapter.notifyDataSetChanged();
        actualizarCantidadRegistros();
    }

    private void actualizarCantidadRegistros() {
        cantidadRegistros.setText("Cantidad de registros: " + notasFiltradas.size());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (selectedPatientId != -1) {
            cargarNotasDelPaciente(selectedPatientId);
        }
    }

    // Adaptador de pacientes con lógica de selección
    public static class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {

        private List<PacienteResponse> pacientes;
        private final OnPatientClickListener listener;
        private int selectedPatientId = -1;

        public interface OnPatientClickListener {
            void onPatientClick(PacienteResponse paciente);
        }

        public PatientAdapter(List<PacienteResponse> pacientes, OnPatientClickListener listener) {
            this.pacientes = pacientes;
            this.listener = listener;
        }

        public void setSelectedPatientId(int patientId) {
            int previousSelected = selectedPatientId;
            selectedPatientId = patientId;

            // Actualizar solo las vistas afectadas
            if (previousSelected != -1) {
                notifyItemChanged(findPositionById(previousSelected));
            }
            if (selectedPatientId != -1) {
                notifyItemChanged(findPositionById(selectedPatientId));
            }
        }

        private int findPositionById(int patientId) {
            for (int i = 0; i < pacientes.size(); i++) {
                if (pacientes.get(i).getId() == patientId) {
                    return i;
                }
            }
            return -1;
        }

        @NonNull
        @Override
        public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_patient_card, parent, false);
            return new PatientViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
            PacienteResponse paciente = pacientes.get(position);
            holder.bind(paciente, paciente.getId() == selectedPatientId);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPatientClick(paciente);
                }
            });
        }

        @Override
        public int getItemCount() {
            return pacientes.size();
        }

        static class PatientViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvName;
            private final ImageView ivProfile;

            public PatientViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvPatientName);
                ivProfile = itemView.findViewById(R.id.ivPatientProfile);
            }

            public void bind(PacienteResponse paciente, boolean isSelected) {
                String nombreCompleto = paciente.getNombre() + " " + paciente.getApellido();
                tvName.setText(nombreCompleto);

                if (paciente.getImagen_paciente() != null && !paciente.getImagen_paciente().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(paciente.getImagen_paciente())
                            .placeholder(R.drawable.perfil_familiar)
                            .error(R.drawable.perfil_familiar)
                            .circleCrop()
                            .into(ivProfile);
                } else {
                    ivProfile.setImageResource(R.drawable.perfil_familiar);
                }

                // Actualizar apariencia según selección (usando valores fijos)
                updateCardAppearance(itemView, isSelected);
            }

            private void updateCardAppearance(View cardView, boolean isSelected) {
                Context context = cardView.getContext();

                // Obtener el fondo original (debe ser un GradientDrawable)
                GradientDrawable drawable = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.card_background).mutate();
                cardView.setBackground(drawable);

                if (isSelected) {
                    // Estilo cuando está seleccionado (valores fijos)
                    drawable.setStroke(4, ContextCompat.getColor(context, R.color.selected_stroke_color));
                    drawable.setColor(ContextCompat.getColor(context, R.color.selected_card_color));
                    cardView.setElevation(8f);
                } else {
                    // Volver al estilo original (valores fijos)
                    drawable.setStroke(1, ContextCompat.getColor(context, R.color.default_stroke_color));
                    drawable.setColor(ContextCompat.getColor(context, R.color.default_card_color));
                    cardView.setElevation(2f);
                }
            }
        }
    }
}