package com.sena.qfinder.ui.medicamento;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.ApiClient;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.AsignarMedicamentoRequest;
import com.sena.qfinder.data.models.AsignarMedicamentoResponse;
import com.sena.qfinder.data.models.MedicamentoResponse;
import com.sena.qfinder.data.models.PacienteListResponse;
import com.sena.qfinder.data.models.PacienteResponse;
import com.sena.qfinder.data.models.AsignacionMedicamentoResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ListaAsignarMedicamentos extends Fragment {

    private Button btnOpenModalAsignar;
    private Calendar startDate, endDate;
    private boolean isSelectingStartDate = true;
    private SimpleDateFormat dateFormatter;
    private LinearLayout patientsContainer, medicamentosContainer;
    private Spinner spinnerPatientsMain;
    private SharedPreferences sharedPreferences;
    private Map<Integer, String> pacientesMap = new HashMap<>();
    private Map<Integer, String> medicamentosMap = new HashMap<>();
    private int selectedPatientId = -1;
    private String selectedPatientName = "";
    private ProgressBar progressBar;

    // Llamadas Retrofit para poder cancelarlas
    private Call<PacienteListResponse> pacientesCall;
    private Call<List<AsignacionMedicamentoResponse>> medicamentosCall;
    private Call<AsignarMedicamentoResponse> asignarMedicamentoCall;
    private Call<List<MedicamentoResponse>> listarMedicamentosCall;

    public ListaAsignarMedicamentos() {}

    public static ListaAsignarMedicamentos newInstance() {
        return new ListaAsignarMedicamentos();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_asignar_medicamentos, container, false);

        btnOpenModalAsignar = view.findViewById(R.id.btnOpenModalAsignar);
        spinnerPatientsMain = view.findViewById(R.id.spinner_patients_main);
        medicamentosContainer = view.findViewById(R.id.medicamentosContainer);
        progressBar = view.findViewById(R.id.progressBar);

        spinnerPatientsMain.setVisibility(View.GONE);
        setupPatientsSection(view, inflater);

        btnOpenModalAsignar.setOnClickListener(view1 -> mostrarDialogoAgregarMedicamento());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancelar todas las llamadas en curso
        if (pacientesCall != null) pacientesCall.cancel();
        if (medicamentosCall != null) medicamentosCall.cancel();
        if (asignarMedicamentoCall != null) asignarMedicamentoCall.cancel();
        if (listarMedicamentosCall != null) listarMedicamentosCall.cancel();
    }

    private void setupPatientsSection(View rootView, LayoutInflater inflater) {
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(getContext());
        horizontalScrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        horizontalScrollView.setScrollbarFadingEnabled(true);
        horizontalScrollView.setHorizontalScrollBarEnabled(false);

        patientsContainer = new LinearLayout(getContext());
        patientsContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        patientsContainer.setOrientation(LinearLayout.HORIZONTAL);
        horizontalScrollView.addView(patientsContainer);

        LinearLayout layoutMedicamentos = rootView.findViewById(R.id.layoutMedicamentos);
        layoutMedicamentos.addView(horizontalScrollView, 1);

        loadPatients();
    }

    private void loadPatients() {
        showLoading(true);
        sharedPreferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "No se encontró token de autenticación", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        Retrofit retrofit = ApiClient.getClient();
        AuthService authService = retrofit.create(AuthService.class);
        pacientesCall = authService.listarPacientes("Bearer " + token);

        pacientesCall.enqueue(new Callback<PacienteListResponse>() {
            @Override
            public void onResponse(@NonNull Call<PacienteListResponse> call, @NonNull Response<PacienteListResponse> response) {
                showLoading(false);
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<PacienteResponse> pacientes = response.body().getData();
                    if (pacientes != null && !pacientes.isEmpty()) {
                        pacientesMap.clear();
                        List<String> pacientesNombres = new ArrayList<>();

                        for (PacienteResponse paciente : pacientes) {
                            String nombreCompleto = paciente.getNombre() + " " + paciente.getApellido();
                            pacientesMap.put(paciente.getId(), nombreCompleto);
                            pacientesNombres.add(nombreCompleto);
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                getContext(),
                                android.R.layout.simple_spinner_item,
                                pacientesNombres);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerPatientsMain.setAdapter(adapter);

                        mostrarPacientes(pacientes);

                        // Seleccionar el primer paciente por defecto
                        if (!pacientes.isEmpty()) {
                            selectedPatientId = pacientes.get(0).getId();
                            selectedPatientName = pacientesNombres.get(0);
                            cargarMedicamentosPaciente(selectedPatientId);
                        }
                    } else {
                        showEmptyState("No hay pacientes registrados");
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ?
                                "Error: " + response.errorBody().string() :
                                "Error: Código " + response.code();
                        Toast.makeText(getContext(), errorBody, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error al obtener pacientes", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<PacienteListResponse> call, @NonNull Throwable t) {
                showLoading(false);
                if (!isAdded() || call.isCanceled()) return;
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API Error", "Error al obtener pacientes", t);
            }
        });
    }

    private void mostrarPacientes(List<PacienteResponse> pacientes) {
        patientsContainer.removeAllViews();

        for (PacienteResponse paciente : pacientes) {
            String nombreCompleto = paciente.getNombre() + " " + paciente.getApellido();
            String diagnostico = paciente.getDiagnostico_principal() != null ?
                    paciente.getDiagnostico_principal() : "Sin diagnóstico";
            String fechaNacimiento = paciente.getFecha_nacimiento() != null ?
                    paciente.getFecha_nacimiento() : "Fecha desconocida";
            String imagenUrl = paciente.getImagen_paciente();

            addPatientCard(nombreCompleto, fechaNacimiento, diagnostico,
                    imagenUrl, paciente.getId());
        }
    }

    private void addPatientCard(String name, String birthDate, String conditions,
                                String imagenUrl, int patientId) {
        View patientCard = LayoutInflater.from(getContext())
                .inflate(R.layout.item_patient_card, patientsContainer, false);
        patientCard.setTag(patientId);

        TextView tvName = patientCard.findViewById(R.id.tvPatientName);
        TextView tvConditions = patientCard.findViewById(R.id.tvPatientConditions);
        ImageView ivProfile = patientCard.findViewById(R.id.ivPatientProfile);

        tvName.setText(name);

        if (conditions != null && !conditions.isEmpty()) {
            String[] conditionsList = conditions.split(",");
            StringBuilder formattedConditions = new StringBuilder();
            for (String condition : conditionsList) {
                formattedConditions.append("• ").append(condition.trim()).append("\n");
            }
            tvConditions.setText(formattedConditions.toString().trim());
        } else {
            tvConditions.setText("• Sin diagnóstico");
        }

        if (imagenUrl != null && !imagenUrl.isEmpty()) {
            Glide.with(requireContext())
                    .load(imagenUrl)
                    .placeholder(R.drawable.perfil_familiar) // Imagen por defecto
                    .error(R.drawable.perfil_familiar) // Imagen si hay error
                    .circleCrop() // Para hacerla circular
                    .into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.perfil_familiar);
        }


        patientCard.setOnClickListener(v -> {
            selectedPatientId = patientId;
            selectedPatientName = name;
            updatePatientSelection();
            cargarMedicamentosPaciente(patientId);
        });

        patientsContainer.addView(patientCard);
    }

    private void cargarMedicamentosPaciente(int pacienteId) {
        showLoading(true);
        sharedPreferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "No se encontró token de autenticación", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        Retrofit retrofit = ApiClient.getClient();
        AuthService authService = retrofit.create(AuthService.class);
        medicamentosCall = authService.listarAsignacionesMedicamentos("Bearer " + token, pacienteId);

        medicamentosCall.enqueue(new Callback<List<AsignacionMedicamentoResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<AsignacionMedicamentoResponse>> call,
                                   @NonNull Response<List<AsignacionMedicamentoResponse>> response) {
                showLoading(false);
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    List<AsignacionMedicamentoResponse> asignaciones = response.body();

                    Log.d("API_RESPONSE", "Respuesta cruda: " + response.toString());
                    Log.d("API_RESPONSE", "Cuerpo de respuesta: " + new Gson().toJson(asignaciones));
                    if (asignaciones != null && !asignaciones.isEmpty()) {
                        mostrarMedicamentosAsignados(asignaciones);

                    } else {
                        showEmptyState("No hay medicamentos asignados para " + selectedPatientName);
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ?
                                "Error: " + response.errorBody().string() :
                                "Error: Código " + response.code();
                        Toast.makeText(getContext(), errorBody, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                    }
                    showEmptyState("Error al cargar medicamentos");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AsignacionMedicamentoResponse>> call,
                                  @NonNull Throwable t) {
                showLoading(false);
                if (!isAdded() || call.isCanceled()) return;
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API Error", "Error al obtener medicamentos", t);
                showEmptyState("Error de conexión");
            }
        });
    }

    private void mostrarMedicamentosAsignados(List<AsignacionMedicamentoResponse> asignaciones) {
        medicamentosContainer.removeAllViews();

        if (asignaciones == null || asignaciones.isEmpty()) {
            showEmptyState("No hay medicamentos asignados para " + selectedPatientName);
            return;
        }

        TextView title = new TextView(getContext());
        title.setText("Medicamentos asignados a " + selectedPatientName);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 16, 0, 16);
        medicamentosContainer.addView(title);

        for (AsignacionMedicamentoResponse asignacion : asignaciones) {
            agregarItemMedicamento(asignacion);
        }
    }

    private void agregarItemMedicamento(AsignacionMedicamentoResponse asignacion) {
        Log.d("MEDICAMENTO_DEBUG", "Asignación recibida: " + new Gson().toJson(asignacion));
        Log.d("MEDICAMENTO_DEBUG", "ID Medicamento: " + asignacion.getIdAsignacion());
        Log.d("MEDICAMENTO_DEBUG", "Objeto Medicamento: " + (asignacion.getMedicamento() != null ? "Presente" : "Nulo"));
        View itemView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_medicamento_asignado, medicamentosContainer, false);

        TextView tvNombre = itemView.findViewById(R.id.tvNombreMedicamento);
        TextView tvDosis = itemView.findViewById(R.id.tvDosis);
        TextView tvFrecuencia = itemView.findViewById(R.id.tvFrecuencia);
        TextView tvFechas = itemView.findViewById(R.id.tvFechas);
        TextView tvEstado = itemView.findViewById(R.id.tvEstado);

        MedicamentoResponse medicamento = asignacion.getMedicamento();
        if (medicamento != null) {
            tvNombre.setText(medicamento.getNombre());
        } else {
            tvNombre.setText("Medicamento desconocido");
        }

        tvDosis.setText("Dosis: " + (asignacion.getDosis() != null ? asignacion.getDosis() : "No especificada"));
        tvFrecuencia.setText("Frecuencia: " + (asignacion.getFrecuencia() != null ? asignacion.getFrecuencia() : "No especificada"));

        // Validación de fechas
        String fechaInicio = asignacion.getFechaInicio();
        String fechaFin = asignacion.getFechaFin();
        String textoFechas = "Período: ";

        if (fechaInicio != null && fechaFin != null) {
            textoFechas += fechaInicio + " - " + fechaFin;
        } else if (fechaInicio != null) {
            textoFechas += fechaInicio + " - Sin fecha fin";
        } else if (fechaFin != null) {
            textoFechas += "Sin fecha inicio - " + fechaFin;
        } else {
            textoFechas += "Fechas no especificadas";
        }

        tvFechas.setText(textoFechas);

        // Determinar estado basado en fechas
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar hoy = Calendar.getInstance();

            // Solo intentar parsear si ambas fechas existen
            if (fechaInicio != null && fechaFin != null) {
                Calendar inicio = Calendar.getInstance();
                inicio.setTime(sdf.parse(fechaInicio));
                Calendar fin = Calendar.getInstance();
                fin.setTime(sdf.parse(fechaFin));

                if (hoy.before(inicio)) {
                    tvEstado.setText("Pendiente");
                    tvEstado.setTextColor(getResources().getColor(R.color.rojopasion));
                } else if (hoy.after(fin)) {
                    tvEstado.setText("Finalizado");
                    tvEstado.setTextColor(getResources().getColor(R.color.green));
                } else {
                    tvEstado.setText("En curso");
                    tvEstado.setTextColor(getResources().getColor(R.color.azul_link));
                }
            } else {
                tvEstado.setText("Fechas incompletas");
                tvEstado.setTextColor(getResources().getColor(R.color.gray));
            }
        } catch (Exception e) {
            Log.e("DateError", "Error al parsear fechas", e);
            tvEstado.setText("Error en fechas");
            tvEstado.setTextColor(getResources().getColor(R.color.gray));
        }

        medicamentosContainer.addView(itemView);
    }

    private void updatePatientSelection() {
        if (selectedPatientId != -1 && pacientesMap.containsKey(selectedPatientId)) {
            String selectedName = pacientesMap.get(selectedPatientId);
            int position = ((ArrayAdapter<String>) spinnerPatientsMain.getAdapter())
                    .getPosition(selectedName);
            if (position >= 0) {
                spinnerPatientsMain.setSelection(position);
            }
        }
    }

    private void mostrarDialogoAgregarMedicamento() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.fragment_agregar_medicamento_usuario, null);
        builder.setView(viewInflated);

        Spinner spinnerPatients = viewInflated.findViewById(R.id.spinner_patients);
        Spinner spinnerMedications = viewInflated.findViewById(R.id.spinner_medications);
        LinearLayout layoutDatePicker = viewInflated.findViewById(R.id.layout_date_picker);
        TextView tvSelectedDates = viewInflated.findViewById(R.id.tv_selected_dates);
        EditText etDosage = viewInflated.findViewById(R.id.et_dosage);
        EditText etFrequency = viewInflated.findViewById(R.id.et_description);
        Button btnSave = viewInflated.findViewById(R.id.btn_save);

        setupSpinners(spinnerPatients, spinnerMedications);
        setupDatePicker(layoutDatePicker, tvSelectedDates);

        // Seleccionar el paciente actual por defecto
        if (selectedPatientId != -1 && pacientesMap.containsKey(selectedPatientId)) {
            String selectedName = pacientesMap.get(selectedPatientId);
            int position = ((ArrayAdapter<String>) spinnerPatients.getAdapter()).getPosition(selectedName);
            if (position >= 0) {
                spinnerPatients.setSelection(position);
            }
        }

        btnSave.setOnClickListener(v -> {
            if (validarCampos(spinnerPatients, spinnerMedications, etDosage, tvSelectedDates)) {
                String pacienteNombre = spinnerPatients.getSelectedItem().toString();
                String medicamentoNombre = spinnerMedications.getSelectedItem().toString();
                String dosis = etDosage.getText().toString();
                String frecuencia = etFrequency.getText().toString();
                String[] fechas = tvSelectedDates.getText().toString().split(" - ");

                int idPaciente = obtenerIdPorNombre(pacientesMap, pacienteNombre);
                int idMedicamento = obtenerIdPorNombre(medicamentosMap, medicamentoNombre);

                if (idPaciente == -1 || idMedicamento == -1) {
                    Toast.makeText(getContext(), "Error al obtener datos del paciente o medicamento", Toast.LENGTH_SHORT).show();
                    return;
                }

                AsignarMedicamentoRequest request = new AsignarMedicamentoRequest(
                        idPaciente,
                        idMedicamento,
                        fechas[0],
                        fechas[1],
                        dosis,
                        frecuencia
                );

                asignarMedicamento(request);
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.create().show();
    }

    private void asignarMedicamento(AsignarMedicamentoRequest request) {
        showLoading(true);
        sharedPreferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "No se encontró token de autenticación", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        Retrofit retrofit = ApiClient.getClient();
        AuthService authService = retrofit.create(AuthService.class);
        asignarMedicamentoCall = authService.asignarMedicamento("Bearer " + token, request);

        asignarMedicamentoCall.enqueue(new Callback<AsignarMedicamentoResponse>() {
            @Override
            public void onResponse(@NonNull Call<AsignarMedicamentoResponse> call,
                                   @NonNull Response<AsignarMedicamentoResponse> response) {
                showLoading(false);
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    AsignarMedicamentoResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(getContext(), "Medicamento asignado correctamente", Toast.LENGTH_SHORT).show();
                        if (selectedPatientId != -1) {
                            cargarMedicamentosPaciente(selectedPatientId);
                        }
                    } else {
                        Toast.makeText(getContext(), "Error: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ?
                                "Error: " + response.errorBody().string() :
                                "Error: Código " + response.code();
                        Toast.makeText(getContext(), errorBody, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AsignarMedicamentoResponse> call,
                                  @NonNull Throwable t) {
                showLoading(false);
                if (!isAdded() || call.isCanceled()) return;
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API Error", "Error al asignar medicamento", t);
            }
        });
    }

    private void setupSpinners(Spinner spinnerPatients, Spinner spinnerMedications) {
        List<String> pacientesNombres = new ArrayList<>(pacientesMap.values());
        ArrayAdapter<String> patientsAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                pacientesNombres
        );
        patientsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPatients.setAdapter(patientsAdapter);

        ArrayAdapter<String> medicationsAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        medicationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMedications.setAdapter(medicationsAdapter);

        loadMedications(spinnerMedications);
    }

    private void loadMedications(Spinner spinnerMedications) {
        sharedPreferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "No se encontró token de autenticación", Toast.LENGTH_SHORT).show();
            return;
        }

        Retrofit retrofit = ApiClient.getClient();
        AuthService authService = retrofit.create(AuthService.class);
        listarMedicamentosCall = authService.listarMedicamentos("Bearer " + token);

        listarMedicamentosCall.enqueue(new Callback<List<MedicamentoResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<MedicamentoResponse>> call,
                                   @NonNull Response<List<MedicamentoResponse>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<MedicamentoResponse> medicamentos = response.body();
                    medicamentosMap.clear();
                    List<String> nombresMedicamentos = new ArrayList<>();

                    for (MedicamentoResponse medicamento : medicamentos) {
                        medicamentosMap.put(medicamento.getId_medicamento(), medicamento.getNombre());
                        nombresMedicamentos.add(medicamento.getNombre());
                    }

                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerMedications.getAdapter();
                    adapter.clear();
                    adapter.addAll(nombresMedicamentos);
                    adapter.notifyDataSetChanged();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ?
                                "Error: " + response.errorBody().string() :
                                "Error: Código " + response.code();
                        Toast.makeText(getContext(), errorBody, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error al obtener medicamentos", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<MedicamentoResponse>> call,
                                  @NonNull Throwable t) {
                if (!isAdded() || call.isCanceled()) return;
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDatePicker(LinearLayout layout, TextView tv) {
        startDate = Calendar.getInstance();
        endDate = Calendar.getInstance();
        endDate.add(Calendar.DAY_OF_MONTH, 7);

        String fechaInicio = dateFormatter.format(startDate.getTime());
        String fechaFin = dateFormatter.format(endDate.getTime());
        tv.setText(fechaInicio + " - " + fechaFin);

        layout.setOnClickListener(v -> {
            Calendar currentDate = isSelectingStartDate ? startDate : endDate;

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view, year, month, dayOfMonth) -> {
                        currentDate.set(year, month, dayOfMonth);
                        isSelectingStartDate = !isSelectingStartDate;

                        if (endDate.before(startDate)) {
                            endDate = (Calendar) startDate.clone();
                            endDate.add(Calendar.DAY_OF_MONTH, 1);
                        }

                        String fechaInicioStr = dateFormatter.format(startDate.getTime());
                        String fechaFinStr = dateFormatter.format(endDate.getTime());
                        tv.setText(fechaInicioStr + " - " + fechaFinStr);
                    },
                    currentDate.get(Calendar.YEAR),
                    currentDate.get(Calendar.MONTH),
                    currentDate.get(Calendar.DAY_OF_MONTH));

            if (!isSelectingStartDate) {
                datePickerDialog.getDatePicker().setMinDate(startDate.getTimeInMillis());
            }

            datePickerDialog.show();
        });
    }

    private boolean validarCampos(Spinner spinnerPatients, Spinner spinnerMedications,
                                  EditText etDosage, TextView tvSelectedDates) {
        if (spinnerPatients.getSelectedItem() == null) {
            Toast.makeText(getContext(), "Selecciona un paciente", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (spinnerMedications.getSelectedItem() == null) {
            Toast.makeText(getContext(), "Selecciona un medicamento", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etDosage.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Ingresa la dosis", Toast.LENGTH_SHORT).show();
            return false;
        }
        String fechas = tvSelectedDates.getText().toString().trim();
        if (fechas.isEmpty() || !fechas.contains(" - ")) {
            Toast.makeText(getContext(), "Selecciona un rango de fechas válido", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private int obtenerIdPorNombre(Map<Integer, String> map, String nombre) {
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            if (entry.getValue().equals(nombre)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        medicamentosContainer.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(String message) {
        medicamentosContainer.removeAllViews();
        TextView tvEmpty = new TextView(getContext());
        tvEmpty.setText(message);
        tvEmpty.setTextSize(16);
        tvEmpty.setGravity(Gravity.CENTER);
        tvEmpty.setPadding(0, 32, 0, 32);
        medicamentosContainer.addView(tvEmpty);
    }
}