package com.sena.qfinder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sena.qfinder.api.ApiClient;
import com.sena.qfinder.api.AuthService;
import com.sena.qfinder.models.NotaEpisodio;
import com.sena.qfinder.models.NotaEpisodioListResponse;
import com.sena.qfinder.models.PacienteListResponse;
import com.sena.qfinder.models.PacienteResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class episodios_salud_menu extends AppCompatActivity {

    private Spinner spinnerPacientes, spinnerOrganizar;
    private int pacienteIdSeleccionado = -1;
    private Map<String, Integer> nombreIdMap = new HashMap<>();
    private String token;
    private ListView listViewNotas;
    private List<NotaEpisodio> todasLasNotas = new ArrayList<>();
    private List<NotaEpisodio> notasFiltradas = new ArrayList<>();
    private TextView cantidadRegistros;
    private EditText searchInput;

    private NotaEpisodioAdapter notaAdapter;

    private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episodios_salud_menu);

        spinnerPacientes = findViewById(R.id.SeleccionarPaciente);
        spinnerOrganizar = findViewById(R.id.spinner_organizar);
        listViewNotas = findViewById(R.id.listViewNotas);
        cantidadRegistros = findViewById(R.id.cantidadRegistros);
        searchInput = findViewById(R.id.searchInput);

        notaAdapter = new NotaEpisodioAdapter(this, notasFiltradas);
        listViewNotas.setAdapter(notaAdapter);

        SharedPreferences preferences = getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String rawToken = preferences.getString("token", null);
        if (rawToken != null) {
            token = "Bearer " + rawToken;
            Log.d("episodios_salud_menu", "Token obtenido correctamente");
        } else {
            Toast.makeText(this, "Token no encontrado. Inicia sesión nuevamente.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        configurarSpinners();
        configurarBusqueda();
        cargarPacientes();

        FloatingActionButton btnNuevaNota = findViewById(R.id.btnNuevaNota);
        btnNuevaNota.setOnClickListener(v -> {
            if (pacienteIdSeleccionado != -1) {
                Intent intent = new Intent(episodios_salud_menu.this, episodios_salud_nota.class);
                intent.putExtra("id_paciente", pacienteIdSeleccionado);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Selecciona un paciente primero.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarSpinners() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Fecha", "Severidad"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrganizar.setAdapter(adapter);
        spinnerOrganizar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ordenarNotas();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void configurarBusqueda() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
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
                    List<PacienteResponse> pacientes = response.body().getData();
                    if (pacientes != null && !pacientes.isEmpty()) {
                        List<String> nombres = new ArrayList<>();
                        nombreIdMap.clear();
                        for (PacienteResponse p : pacientes) {
                            String nombre = p.getNombre() + " " + p.getApellido();
                            nombreIdMap.put(nombre, p.getId());
                            nombres.add(nombre);
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(episodios_salud_menu.this, android.R.layout.simple_spinner_item, nombres);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerPacientes.setAdapter(adapter);
                        spinnerPacientes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                String nombreSeleccionado = (String) parent.getItemAtPosition(position);
                                Integer idPaciente = nombreIdMap.get(nombreSeleccionado);
                                if (idPaciente != null) {
                                    pacienteIdSeleccionado = idPaciente;
                                    cargarNotasDelPaciente(pacienteIdSeleccionado);
                                } else {
                                    pacienteIdSeleccionado = -1;
                                }
                            }
                            @Override public void onNothingSelected(AdapterView<?> parent) {
                                pacienteIdSeleccionado = -1;
                                todasLasNotas.clear();
                                notasFiltradas.clear();
                                notaAdapter.notifyDataSetChanged();
                                actualizarCantidadRegistros();
                            }
                        });
                    } else {
                        Toast.makeText(episodios_salud_menu.this, "No se encontraron pacientes", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(episodios_salud_menu.this, "Error en la respuesta al cargar pacientes", Toast.LENGTH_SHORT).show();
                    Log.e("API_ERROR", "Error pacientes - Código: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PacienteListResponse> call, Throwable t) {
                Toast.makeText(episodios_salud_menu.this, "Error al conectar al servidor", Toast.LENGTH_SHORT).show();
                Log.e("API_ERROR", "Falla conexión pacientes", t);
            }
        });
    }

    private void cargarNotasDelPaciente(int idPaciente) {
        AuthService authService = ApiClient.getAuthService();
        authService.obtenerEpisodios(token, idPaciente).enqueue(new Callback<NotaEpisodioListResponse>() {
            @Override
            public void onResponse(Call<NotaEpisodioListResponse> call, Response<NotaEpisodioListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<NotaEpisodio> notas = response.body().getData();
                    if (notas != null) {
                        todasLasNotas.clear();
                        todasLasNotas.addAll(notas);
                        ordenarNotas();
                        filtrarNotas(searchInput.getText().toString());
                    }
                } else {
                    Toast.makeText(episodios_salud_menu.this, "Error al cargar notas", Toast.LENGTH_SHORT).show();
                    Log.e("API_ERROR", "Error notas - Código: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<NotaEpisodioListResponse> call, Throwable t) {
                Toast.makeText(episodios_salud_menu.this, "Error al conectar al servidor para notas", Toast.LENGTH_SHORT).show();
                Log.e("API_ERROR", "Falla conexión notas", t);
            }
        });
    }

    private void ordenarNotas() {
        String criterio = (String) spinnerOrganizar.getSelectedItem();
        if (criterio == null) return;

        if (criterio.equalsIgnoreCase("fecha")) {
            Collections.sort(todasLasNotas, (n1, n2) -> {
                try {
                    String fecha1 = n1.getFechaHoraInicio();
                    String fecha2 = n2.getFechaHoraInicio();

                    if (fecha1 == null || fecha1.trim().isEmpty()) return 1;
                    if (fecha2 == null || fecha2.trim().isEmpty()) return -1;

                    Date d1 = inputFormat.parse(fecha1);
                    Date d2 = inputFormat.parse(fecha2);
                    return d2.compareTo(d1); // Orden descendente
                } catch (Exception e) {
                    Log.e("ordenarNotas", "Error al parsear fecha", e);
                    return 0;
                }
            });
        }

        filtrarNotas(searchInput.getText().toString());
    }

    private int severidadValue(String severidad) {
        if (severidad == null) return 0;
        switch (severidad.toLowerCase(Locale.ROOT)) {
            case "alta": return 3;
            case "media": return 2;
            case "baja": return 1;
            default: return 0;
        }
    }

    private void filtrarNotas(String texto) {
        notasFiltradas.clear();
        if (texto == null || texto.trim().isEmpty()) {
            notasFiltradas.addAll(todasLasNotas);
        } else {
            String filtro = texto.toLowerCase(Locale.ROOT);
            for (NotaEpisodio nota : todasLasNotas) {
                boolean coincide = false;
                if (nota.getDescripcion() != null && nota.getDescripcion().toLowerCase(Locale.ROOT).contains(filtro)) {
                    coincide = true;
                } else if (nota.getIntervenciones() != null && nota.getIntervenciones().toLowerCase(Locale.ROOT).contains(filtro)) {
                    coincide = true;
                } else if (nota.getSeveridad() != null && nota.getSeveridad().toLowerCase(Locale.ROOT).contains(filtro)) {
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
