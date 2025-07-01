package com.sena.qfinder.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.sena.qfinder.ui.paciente.PatientAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class episodios_salud_menu extends AppCompatActivity {

    private ImageView btnBack;

    private Spinner spinnerOrganizar;
    private int pacienteIdSeleccionado = -1;
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
        listViewNotas = findViewById(R.id.listViewNotas);
        cantidadRegistros = findViewById(R.id.cantidadRegistros);
        searchInput = findViewById(R.id.searchInput);
        btnBack = findViewById(R.id.btnBack);
        recyclerPacientes = findViewById(R.id.recyclerPaciente);

        notaAdapter = new NotaEpisodioAdapter(this, notasFiltradas);
        listViewNotas.setAdapter(notaAdapter);

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
            if (pacienteIdSeleccionado != -1) {
                Intent intent = new Intent(this, episodios_salud_nota.class);
                intent.putExtra("id_paciente", pacienteIdSeleccionado);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Selecciona un paciente primero.", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón de retroceso
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerPacientes() {
        recyclerPacientes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        patientAdapter = new PatientAdapter(listaPacientes, paciente -> {
            pacienteIdSeleccionado = paciente.getId();
            cargarNotasDelPaciente(pacienteIdSeleccionado);
        });
        recyclerPacientes.setAdapter(patientAdapter);
    }



    private void configurarBusqueda() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
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
        String criterio = (String) spinnerOrganizar.getSelectedItem();
        if (criterio == null) return;

        if (criterio.equalsIgnoreCase("fecha")) {
            Collections.sort(todasLasNotas, (n1, n2) -> {
                try {
                    Date d1 = inputFormat.parse(n1.getFechaHoraInicio());
                    Date d2 = inputFormat.parse(n2.getFechaHoraInicio());
                    return d2.compareTo(d1); // Más reciente primero
                } catch (Exception e) {
                    return 0;
                }
            });
        } else if (criterio.equalsIgnoreCase("tipo")) {
            Collections.sort(todasLasNotas, (n1, n2) -> {
                String t1 = n1.getTipo() != null ? n1.getTipo() : "";
                String t2 = n2.getTipo() != null ? n2.getTipo() : "";
                return t1.compareToIgnoreCase(t2);
            });
        }

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

                // Búsqueda en título
                if (nota.getTitulo() != null && nota.getTitulo().toLowerCase().contains(filtro)) {
                    coincide = true;
                }
                // Búsqueda en descripción
                else if (nota.getDescripcion() != null && nota.getDescripcion().toLowerCase().contains(filtro)) {
                    coincide = true;
                }
                // Búsqueda en intervenciones
                else if (nota.getIntervenciones() != null && nota.getIntervenciones().toLowerCase().contains(filtro)) {
                    coincide = true;
                }
                // Búsqueda en tipo
                else if (nota.getTipo() != null && nota.getTipo().toLowerCase().contains(filtro)) {
                    coincide = true;
                }

                if (coincide) {
                    notasFiltradas.add(nota);
                }
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
        if (pacienteIdSeleccionado != -1) {
            cargarNotasDelPaciente(pacienteIdSeleccionado);
        }
    }
}