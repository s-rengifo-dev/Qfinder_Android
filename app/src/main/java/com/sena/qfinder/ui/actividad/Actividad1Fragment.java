package com.sena.qfinder.ui.actividad;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sena.qfinder.R;
import com.sena.qfinder.data.api.ApiClient;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.ActividadGetResponse;
import com.sena.qfinder.data.models.ActividadListResponse;
import com.sena.qfinder.data.models.PacienteListResponse;
import com.sena.qfinder.data.models.PacienteResponse;
import com.sena.qfinder.ui.home.Fragment_Serivicios;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Actividad1Fragment extends Fragment {

    private RecyclerView recyclerViewActividades;
    private RecyclerView recyclerViewPacientes;
    private Button btnAgregarActividad;
    private ImageView btnBack;
    private ActividadAdapter actividadAdapter;
    private PatientAdapter patientAdapter;
    private Map<Integer, PacienteResponse> pacientesMap = new HashMap<>();
    private List<PacienteResponse> listaPacientes = new ArrayList<>();
    private int selectedPatientId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_actividad1, container, false);

        // Inicialización de vistas
        recyclerViewPacientes = view.findViewById(R.id.recyclerViewPacientes);
        recyclerViewActividades = view.findViewById(R.id.recyclerViewActividades);
        btnAgregarActividad = view.findViewById(R.id.btnAgregarActividad);
        btnBack = view.findViewById(R.id.btnBack);

        // Configurar RecyclerView para pacientes (horizontal)
        recyclerViewPacientes.setLayoutManager(new LinearLayoutManager(
                getContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        ));

        // Adaptador de pacientes con el nuevo sistema de selección
        patientAdapter = new PatientAdapter(new ArrayList<>(), paciente -> {
            selectedPatientId = paciente.getId();
            patientAdapter.setSelectedPatientId(selectedPatientId);
            cargarActividades(paciente.getId());
        });
        recyclerViewPacientes.setAdapter(patientAdapter);

        // Configurar RecyclerView para actividades (vertical) - MANTENIENDO TU LÓGICA ORIGINAL
        recyclerViewActividades.setLayoutManager(new LinearLayoutManager(getContext()));
        actividadAdapter = new ActividadAdapter(new ArrayList<>(), actividad -> {
            // Lógica original para editar actividad
            AgregarActividadDialogFragment dialog = AgregarActividadDialogFragment.newInstance(actividad);
            dialog.setOnActividadGuardadaListener(() -> cargarActividades(selectedPatientId));
            dialog.show(getParentFragmentManager(), "EditarActividadDialog");
        });
        recyclerViewActividades.setAdapter(actividadAdapter);

        // Cargar pacientes
        cargarPacientes();

        // Configurar botón agregar actividad - MANTENIENDO TU LÓGICA ORIGINAL
        btnAgregarActividad.setOnClickListener(v -> {
            if (selectedPatientId == -1) {
                Toast.makeText(getContext(), "Seleccione un paciente primero", Toast.LENGTH_SHORT).show();
                return;
            }

            AgregarActividadDialogFragment dialog = AgregarActividadDialogFragment.newInstance(selectedPatientId);
            dialog.setOnActividadGuardadaListener(() -> cargarActividades(selectedPatientId));
            dialog.show(getParentFragmentManager(), "AgregarActividadDialog");
        });

        // Configurar botón de retroceso - MANTENIENDO TU LÓGICA ORIGINAL
        btnBack.setOnClickListener(v -> {
            // En lugar de navegar a Fragment_Serivicios, simplemente retrocede
            getParentFragmentManager().popBackStack();
        });

        return view;
    }

    private void cargarPacientes() {

        if (!isAdded() || getContext() == null) {
            return; // Salir si el fragmento no está adjunto
        }
        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "No se encontró token de autenticación", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthService authService = ApiClient.getClient().create(AuthService.class);
        Call<PacienteListResponse> call = authService.listarPacientes("Bearer " + token);

        call.enqueue(new Callback<PacienteListResponse>() {
            @Override
            public void onResponse(@NonNull Call<PacienteListResponse> call, @NonNull Response<PacienteListResponse> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    listaPacientes = response.body().getData();
                    if (listaPacientes != null && !listaPacientes.isEmpty()) {
                        // Llenar el mapa de pacientes
                        for (PacienteResponse paciente : listaPacientes) {
                            pacientesMap.put(paciente.getId(), paciente);
                        }
                        // Actualizar el adaptador de pacientes
                        patientAdapter.setPatients(listaPacientes);

                        // Seleccionar el primer paciente por defecto
                        if (selectedPatientId == -1) {
                            selectedPatientId = listaPacientes.get(0).getId();
                            patientAdapter.setSelectedPatientId(selectedPatientId);
                            cargarActividades(selectedPatientId);
                        }
                    } else {
                        Toast.makeText(getContext(), "No hay pacientes registrados", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Error al cargar pacientes", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PacienteListResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarActividades(int idPaciente) {

        if (!isAdded() || getContext() == null) {
            return; // Salir si el fragmento no está adjunto
        }
        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "No se encontró token de autenticación", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthService authService = ApiClient.getClient().create(AuthService.class);
        Call<ActividadListResponse> call = authService.listarActividades("Bearer " + token, idPaciente);

        call.enqueue(new Callback<ActividadListResponse>() {
            @Override
            public void onResponse(@NonNull Call<ActividadListResponse> call, @NonNull Response<ActividadListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<ActividadGetResponse> actividadesResponse = response.body().getData();
                    List<ActividadGetResponse> actividadesEnriquecidas = enriquecerActividades(actividadesResponse);
                    actividadAdapter.setActividades(actividadesEnriquecidas);
                } else {
                    Toast.makeText(getContext(), "No se encontraron actividades", Toast.LENGTH_SHORT).show();
                    actividadAdapter.setActividades(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ActividadListResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                actividadAdapter.setActividades(new ArrayList<>());
            }
        });
    }

    // MÉTODO ORIGINAL SIN MODIFICACIONES
    private List<ActividadGetResponse> enriquecerActividades(List<ActividadGetResponse> actividadesResponse) {
        List<ActividadGetResponse> actividadesEnriquecidas = new ArrayList<>();

        for (ActividadGetResponse actividad : actividadesResponse) {
            PacienteResponse paciente = pacientesMap.get(actividad.getIdPaciente());
            String nombrePaciente = paciente != null ?
                    paciente.getNombre() + " " + paciente.getApellido() : "Paciente desconocido";

            ActividadGetResponse actividadEnriquecida = new ActividadGetResponse();
            actividadEnriquecida.setId(actividad.getId());
            actividadEnriquecida.setIdPaciente(actividad.getIdPaciente());
            actividadEnriquecida.setTitulo(actividad.getTitulo());
            actividadEnriquecida.setFecha(actividad.getFecha());
            actividadEnriquecida.setHora(actividad.getHora());
            actividadEnriquecida.setDescripcion(actividad.getDescripcion());
            actividadEnriquecida.setEstado(actividad.getEstado());

            actividadesEnriquecidas.add(actividadEnriquecida);
        }

        return actividadesEnriquecidas;
    }
}