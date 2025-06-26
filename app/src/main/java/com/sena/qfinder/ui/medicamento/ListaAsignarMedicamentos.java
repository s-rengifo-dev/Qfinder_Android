package com.sena.qfinder.ui.medicamento;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.ApiClient;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.AsignarMedicamentoRequest;
import com.sena.qfinder.data.models.AsignarMedicamentoResponse;
import com.sena.qfinder.data.models.MedicamentoResponse;
import com.sena.qfinder.data.models.PacienteListResponse;
import com.sena.qfinder.data.models.PacienteMedicamento;
import com.sena.qfinder.data.models.PacienteResponse;
import com.sena.qfinder.data.models.AsignacionMedicamentoResponse;
import com.sena.qfinder.database.DatabaseHelper;
import com.sena.qfinder.database.DatabaseMedicamentoHelper;
import com.sena.qfinder.database.entity.AlarmaEntity;
import com.sena.qfinder.database.entity.AlarmaMedicamentoEntity;
import com.sena.qfinder.ui.home.DashboardFragment;
import com.sena.qfinder.ui.home.Fragment_Serivicios;
import com.sena.qfinder.utils.ActivityAlarmReceiver;
import com.sena.qfinder.utils.AlarmCalculator;
import com.sena.qfinder.utils.AlarmReceiver;
import com.sena.qfinder.utils.MedicamentoAlarmManager;
import com.sena.qfinder.utils.MedicamentoAlarmReceiver;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
    private SimpleDateFormat dateFormatter, timeFormatter;
    private LinearLayout patientsContainer, medicamentosContainer;
    private Spinner spinnerPatientsMain;
    private ImageView btnBack;
    private SharedPreferences sharedPreferences;
    private Map<Integer, String> pacientesMap = new HashMap<>();
    private Map<Integer, String> medicamentosMap = new HashMap<>();
    private int selectedPatientId = -1;
    private String selectedPatientName = "";
    private ProgressBar progressBar;
    private TimePickerDialog timePickerDialog;
    private TextView tvStartTime;

    private Call<PacienteListResponse> pacientesCall;
    private Call<List<AsignacionMedicamentoResponse>> medicamentosCall;
    private Call<AsignarMedicamentoResponse> asignarMedicamentoCall;
    private Call<List<MedicamentoResponse>> listarMedicamentosCall;

    public ListaAsignarMedicamentos() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        timeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
        startDate = Calendar.getInstance();
        endDate = Calendar.getInstance();
        endDate.add(Calendar.DAY_OF_MONTH, 7);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_asignar_medicamentos, container, false);

        btnOpenModalAsignar = view.findViewById(R.id.btnOpenModalAsignar);
        btnBack = view.findViewById(R.id.btnBack);
        spinnerPatientsMain = view.findViewById(R.id.spinner_patients_main);
        medicamentosContainer = view.findViewById(R.id.medicamentosContainer);
        progressBar = view.findViewById(R.id.progressBar);

        spinnerPatientsMain.setVisibility(View.GONE);
        setupPatientsSection(view, inflater);

        btnOpenModalAsignar.setOnClickListener(view1 -> mostrarDialogoAgregarMedicamento());

        btnBack.setOnClickListener(v -> {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            Fragment_Serivicios ServiciosFragment = new Fragment_Serivicios();
            fragmentTransaction.replace(R.id.fragment_container, ServiciosFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pacientesCall != null) pacientesCall.cancel();
        if (medicamentosCall != null) medicamentosCall.cancel();
        if (asignarMedicamentoCall != null) asignarMedicamentoCall.cancel();
        if (listarMedicamentosCall != null) listarMedicamentosCall.cancel();
    }

    private long calcularIntervaloMillis(int cantidad, String unidad) {
        switch (unidad.toLowerCase()) {
            case "hora":
            case "horas":
                return cantidad * 60 * 60 * 1000L;
            case "dia":
            case "día":
            case "dias":
            case "días":
                return cantidad * 24 * 60 * 60 * 1000L;
            case "semana":
            case "semanas":
                return cantidad * 7 * 24 * 60 * 60 * 1000L;
            case "mes":
            case "meses":
                return cantidad * 30L * 24 * 60 * 60 * 1000L; // Aproximación de 30 días
            default:
                Log.e("Frecuencia", "Unidad de tiempo no reconocida: " + unidad);
                return 0;
        }
    }


    private void programarAlarmaMedicamento(PacienteMedicamento pm) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = ContextCompat.getSystemService(requireContext(), AlarmManager.class);


            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                return; // Salir hasta que el permiso sea concedido
            }
        }
        try {
            Log.d("Alarma", "Iniciando programación para ID: " + pm.getId_pac_medicamento());
            int horasFrecuencia;
            try {
                horasFrecuencia = Integer.parseInt(pm.getFrecuencia());
            } catch (NumberFormatException e) {
                Log.e("Alarma", "Frecuencia no es un número válido: " + pm.getFrecuencia());
                horasFrecuencia = 1; // Valor por defecto
            }
            // Convertir frecuencia de horas a formato descriptivo
            String frecuenciaDescriptiva = AlarmCalculator.convertirHorasAFrecuencia(horasFrecuencia);
            Log.d("Alarma", "Frecuencia convertida: " + pm.getFrecuencia() + " horas -> " + frecuenciaDescriptiva);

            // Parsear frecuencia
            String[] frecuenciaParts = AlarmCalculator.parseFrecuencia(frecuenciaDescriptiva);
            int cantidad = Integer.parseInt(frecuenciaParts[0]);
            String unidad = frecuenciaParts[1];
            Log.d("Alarma", "Frecuencia parseada: " + cantidad + " " + unidad);

            // Calcular intervalo
            long intervaloMillis = AlarmCalculator.calcularIntervalo(cantidad, unidad);
            Log.d("Alarma", "Intervalo calculado: " + intervaloMillis + " ms");

            // Calcular próxima alarma
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date fechaHoraInicio = sdf.parse(pm.getFecha_inicio() + " " + pm.getHora_inicio());
            long nextTriggerTime = fechaHoraInicio.getTime();

            // Si ya pasó, calcular siguiente ocurrencia
            if (nextTriggerTime < System.currentTimeMillis()) {
                long diferencia = System.currentTimeMillis() - nextTriggerTime;
                long ocurrenciasPasadas = diferencia / intervaloMillis;
                nextTriggerTime = nextTriggerTime + (ocurrenciasPasadas + 1) * intervaloMillis;
                Log.d("Alarma", "Ajustando alarma futura: " + new Date(nextTriggerTime));
            }

            // Crear entidad de alarma
            AlarmaMedicamentoEntity alarma = new AlarmaMedicamentoEntity(
                    pm.getId_pac_medicamento(),
                    pm.getId_medicamento(),
                    pm.getId_paciente(),
                    pm.getNombre_medicamento(),
                    pm.getDosis(),
                    frecuenciaDescriptiva,
                    pm.getFecha_inicio(),
                    pm.getHora_inicio(),
                    pm.getFecha_fin(),
                    nextTriggerTime,
                    true,
                    intervaloMillis
            );

            // Guardar en BD
            DatabaseMedicamentoHelper dbHelper = DatabaseMedicamentoHelper.getInstance(getContext());
            dbHelper.guardarAlarmaMedicamento(alarma);
            Log.d("Alarma", "Alarma guardada en BD");

            // Programar alarma
            MedicamentoAlarmManager.programarAlarmaExacta(
                    getContext(),
                    alarma.getId(),
                    alarma.getIdMedicamento(),
                    alarma.getIdPaciente(),
                    alarma.getNombreMedicamento(),
                    alarma.getDosis(),
                    alarma.getFrecuencia(),
                    alarma.getTimestampProximaAlarma(),
                    alarma.getIntervaloMillis()
            );

            Log.d("Alarma", "Alarma programada exitosamente");

        } catch (Exception e) {
            Log.e("Alarma", "Error al programar alarma", e);
            Toast.makeText(getContext(), "Error al programar alarma: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void cancelarAlarmaMedicamento(int idAsignacion) {
        try {
            MedicamentoAlarmManager.cancelarAlarma(requireContext(), idAsignacion);
            DatabaseMedicamentoHelper dbHelper = DatabaseMedicamentoHelper.getInstance(requireContext());
            dbHelper.cancelarAlarmaMedicamento(idAsignacion);
            Log.d("MedicamentoAlarma", "Alarma cancelada para ID: " + idAsignacion);
        } catch (Exception e) {
            Log.e("MedicamentoAlarma", "Error al cancelar alarma", e);
        }
    }
    private void asignarMedicamento(AsignarMedicamentoRequest request, AlertDialog dialog) {
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

                        if (apiResponse.getData() != null) {
                            // Obtener el nombre del medicamento de otra fuente
                            String nombreMedicamento = obtenerNombreMedicamento(request.getId_medicamento());

                            PacienteMedicamento pm = apiResponse.getData();
                            pm.setNombre_medicamento(nombreMedicamento);
                            Log.d("MedicamentoDebug", "Nombre medicamento asignado: " + pm.getNombre_medicamento());

                            programarAlarmaMedicamento(pm);
                        }

                        dialog.dismiss();
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

    private void actualizarMedicamento(int idAsignacion, AsignarMedicamentoRequest request, AlertDialog dialog) {
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
        Call<AsignarMedicamentoResponse> call = authService.actualizarMedicamento("Bearer " + token, idAsignacion, request);

        call.enqueue(new Callback<AsignarMedicamentoResponse>() {
            @Override
            public void onResponse(@NonNull Call<AsignarMedicamentoResponse> call,
                                   @NonNull Response<AsignarMedicamentoResponse> response) {
                showLoading(false);
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    AsignarMedicamentoResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(getContext(), "Medicamento actualizado correctamente", Toast.LENGTH_SHORT).show();

                        cancelarAlarmaMedicamento(idAsignacion);

                        if (apiResponse.getData() != null) {
                            // Obtener el nombre del medicamento de otra fuente
                            String nombreMedicamento = obtenerNombreMedicamento(request.getId_medicamento());

                            PacienteMedicamento pm = apiResponse.getData();
                            pm.setNombre_medicamento(nombreMedicamento);
                            Log.d("MedicamentoDebug", "Nombre medicamento actualizado: " + pm.getNombre_medicamento());

                            programarAlarmaMedicamento(pm);
                        }

                        dialog.dismiss();
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
                Log.e("API Error", "Error al actualizar medicamento", t);
            }
        });
    }

    // Método auxiliar para obtener el nombre del medicamento
    private String obtenerNombreMedicamento(int idMedicamento) {
        if (medicamentosMap.containsKey(idMedicamento)) {
            return medicamentosMap.get(idMedicamento);
        }
        return "Medicamento"; // Valor por defecto
    }
    private void eliminarMedicamento(AsignacionMedicamentoResponse asignacion) {
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
        Call<AsignarMedicamentoResponse> call = authService.eliminarMedicamento("Bearer " + token, asignacion.getIdAsignacion());

        call.enqueue(new Callback<AsignarMedicamentoResponse>() {
            @Override
            public void onResponse(@NonNull Call<AsignarMedicamentoResponse> call, @NonNull Response<AsignarMedicamentoResponse> response) {
                showLoading(false);
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    AsignarMedicamentoResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(getContext(), "Medicamento eliminado correctamente", Toast.LENGTH_SHORT).show();

                        cancelarAlarmaMedicamento(asignacion.getIdAsignacion());

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
            public void onFailure(@NonNull Call<AsignarMedicamentoResponse> call, @NonNull Throwable t) {
                showLoading(false);
                if (!isAdded() || call.isCanceled()) return;
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API Error", "Error al eliminar medicamento", t);
            }
        });
    }

    private void agregarItemMedicamento(AsignacionMedicamentoResponse asignacion) {
        View itemView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_medicamento_asignado, medicamentosContainer, false);

        TextView tvNombre = itemView.findViewById(R.id.tvNombreMedicamento);
        TextView tvDosis = itemView.findViewById(R.id.tvDosis);
        TextView tvFrecuencia = itemView.findViewById(R.id.tvFrecuencia);
        TextView tvFechas = itemView.findViewById(R.id.tvFechas);
        TextView tvHoraInicio = itemView.findViewById(R.id.tvHoraInicio);
        TextView tvEstado = itemView.findViewById(R.id.tvEstado);
        ImageView ivEdit = itemView.findViewById(R.id.ivEdit);
        ImageView ivDelete = itemView.findViewById(R.id.ivDelete);
        ImageView ivAlarm = itemView.findViewById(R.id.ivAlarm);

        if (asignacion.getMedicamento() != null) {
            tvNombre.setText(asignacion.getMedicamento().getNombre());
        } else {
            tvNombre.setText("Medicamento no disponible");
        }

        tvDosis.setText("Dosis: " + (asignacion.getDosis() != null ? asignacion.getDosis() : "No especificada"));
        tvFrecuencia.setText("Frecuencia: " + (asignacion.getFrecuencia() != null ? asignacion.getFrecuencia() : "No especificada"));

        if (asignacion.getHoraInicio() != null) {
            tvHoraInicio.setText("Hora: " + asignacion.getHoraInicio());
        } else {
            tvHoraInicio.setText("Hora no especificada");
        }

        String textoFechas = "Período: ";
        if (asignacion.getFechaInicio() != null && asignacion.getFechaFin() != null) {
            textoFechas += asignacion.getFechaInicio() + " - " + asignacion.getFechaFin();
        } else {
            textoFechas += "Fechas no especificadas";
        }
        tvFechas.setText(textoFechas);

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar hoy = Calendar.getInstance();

            if (asignacion.getFechaInicio() != null && asignacion.getFechaFin() != null) {
                Calendar inicio = Calendar.getInstance();
                inicio.setTime(sdf.parse(asignacion.getFechaInicio()));
                Calendar fin = Calendar.getInstance();
                fin.setTime(sdf.parse(asignacion.getFechaFin()));

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

        DatabaseMedicamentoHelper dbHelper = DatabaseMedicamentoHelper.getInstance(getContext());
        boolean tieneAlarma = dbHelper.tieneAlarma(asignacion.getIdAsignacion());
        ivAlarm.setVisibility(tieneAlarma ? View.VISIBLE : View.GONE);

        ivEdit.setOnClickListener(v -> mostrarDialogoEditarMedicamento(asignacion));
        ivDelete.setOnClickListener(v -> mostrarDialogoConfirmarEliminacion(asignacion));
        ivAlarm.setOnClickListener(v -> mostrarDialogoGestionAlarmas(asignacion));

        medicamentosContainer.addView(itemView);
    }

    private void mostrarDialogoGestionAlarmas(AsignacionMedicamentoResponse asignacion) {
        new AlertDialog.Builder(getContext())
                .setTitle("Gestión de Alarmas")
                .setMessage("¿Qué deseas hacer con las alarmas de este medicamento?")
                .setPositiveButton("Reprogramar", (dialog, which) -> {
                    cancelarAlarmaMedicamento(asignacion.getIdAsignacion());

                    PacienteMedicamento pm = new PacienteMedicamento();
                    pm.setId_pac_medicamento(asignacion.getIdAsignacion());
                    pm.setFecha_inicio(asignacion.getFechaInicio());
                    pm.setFecha_fin(asignacion.getFechaFin());
                    pm.setHora_inicio(asignacion.getHoraInicio());
                    pm.setDosis(asignacion.getDosis());
                    pm.setFrecuencia(asignacion.getFrecuencia());

                    programarAlarmaMedicamento(pm);

                    Toast.makeText(getContext(), "Alarmas reprogramadas", Toast.LENGTH_SHORT).show();
                    if (selectedPatientId != -1) {
                        cargarMedicamentosPaciente(selectedPatientId);
                    }
                })
                .setNegativeButton("Cancelar Alarmas", (dialog, which) -> {
                    cancelarAlarmaMedicamento(asignacion.getIdAsignacion());
                    Toast.makeText(getContext(), "Alarmas canceladas", Toast.LENGTH_SHORT).show();
                    if (selectedPatientId != -1) {
                        cargarMedicamentosPaciente(selectedPatientId);
                    }
                })
                .setNeutralButton("Cerrar", null)
                .show();
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
                    .placeholder(R.drawable.perfil_familiar)
                    .error(R.drawable.perfil_familiar)
                    .circleCrop()
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

                    Log.d("API_RESPONSE", "Respuesta recibida: " + new Gson().toJson(asignaciones));

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
                        Log.e("API_ERROR", errorBody);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                        Log.e("API_ERROR", "Error al procesar error", e);
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

    private void mostrarDialogoEditarMedicamento(AsignacionMedicamentoResponse asignacion) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.fragment_agregar_medicamento_usuario, null);
        builder.setView(viewInflated);

        Spinner spinnerPatients = viewInflated.findViewById(R.id.spinner_patients);
        Spinner spinnerMedications = viewInflated.findViewById(R.id.spinner_medications);
        Spinner spinnerFrequencyUnit = viewInflated.findViewById(R.id.spinner_frequency_unit);
        TextView tvStartDate = viewInflated.findViewById(R.id.tv_start_date);
        TextView tvEndDate = viewInflated.findViewById(R.id.tv_end_date);
        tvStartTime = viewInflated.findViewById(R.id.tv_start_time);
        EditText etDosage = viewInflated.findViewById(R.id.et_dosage);
        EditText etFrequencyNumber = viewInflated.findViewById(R.id.et_frequency_number);
        Button btnSave = viewInflated.findViewById(R.id.btn_save);
        LinearLayout layoutStartTime = viewInflated.findViewById(R.id.layout_start_time);

        btnSave.setText("Actualizar");

        // Adaptadores para los spinners
        ArrayAdapter<CharSequence> frequencyAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.frequency_units,
                android.R.layout.simple_spinner_item
        );
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrequencyUnit.setAdapter(frequencyAdapter);

        ArrayAdapter<String> patientsAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>(pacientesMap.values())
        );
        patientsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPatients.setAdapter(patientsAdapter);

        ArrayAdapter<String> medicationsAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>(medicamentosMap.values())
        );
        medicationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMedications.setAdapter(medicationsAdapter);

        // Rellenar campos con datos existentes
        if (asignacion != null) {
            if (asignacion.getFechaInicio() != null) {
                tvStartDate.setText(asignacion.getFechaInicio());
            }
            if (asignacion.getFechaFin() != null) {
                tvEndDate.setText(asignacion.getFechaFin());
            }
            if (asignacion.getHoraInicio() != null) {
                tvStartTime.setText(asignacion.getHoraInicio());
            }
            if (asignacion.getDosis() != null) {
                etDosage.setText(asignacion.getDosis());
            }

            // Frecuencia (número y unidad)
            if (asignacion.getFrecuencia() != null && !asignacion.getFrecuencia().isEmpty()) {
                String[] frecuenciaParts = asignacion.getFrecuencia().split(" ");
                if (frecuenciaParts.length >= 2) {
                    etFrequencyNumber.setText(frecuenciaParts[0]);
                    for (int i = 0; i < spinnerFrequencyUnit.getCount(); i++) {
                        String unidad = spinnerFrequencyUnit.getItemAtPosition(i).toString();
                        if (unidad.equalsIgnoreCase(frecuenciaParts[1])) {
                            spinnerFrequencyUnit.setSelection(i);
                            break;
                        }
                    }
                } else {
                    etFrequencyNumber.setText(frecuenciaParts[0]);
                    spinnerFrequencyUnit.setSelection(0);
                }
            }

            // Seleccionar paciente por nombre completo
            if (asignacion.getPaciente() != null) {
                int idPaciente = asignacion.getPaciente().getId();
                String nombreCompleto = asignacion.getPaciente().getNombre() + " " + asignacion.getPaciente().getApellido();

                int index = -1;
                for (int i = 0; i < spinnerPatients.getCount(); i++) {
                    if (spinnerPatients.getItemAtPosition(i).toString().equalsIgnoreCase(nombreCompleto)) {
                        index = i;
                        break;
                    }
                }

                if (index != -1) {
                    spinnerPatients.setSelection(index);
                } else {
                    Log.w("PacienteSpinner", "No se encontró el paciente en el spinner: " + nombreCompleto);
                }
            }

            if (asignacion.getMedicamento() != null) {
                int idMedicamentoAsignado = asignacion.getMedicamento().getId_medicamento();

                for (Map.Entry<Integer, String> entry : medicamentosMap.entrySet()) {
                    if (entry.getKey() == idMedicamentoAsignado) {
                        String nombreMedicamento = entry.getValue();

                        for (int i = 0; i < spinnerMedications.getCount(); i++) {
                            String item = spinnerMedications.getItemAtPosition(i).toString().trim();
                            if (item.equalsIgnoreCase(nombreMedicamento.trim())) {
                                spinnerMedications.setSelection(i);
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }

        // Configurar DatePickers
        LinearLayout layoutStartDate = viewInflated.findViewById(R.id.layout_start_date);
        LinearLayout layoutEndDate = viewInflated.findViewById(R.id.layout_end_date);

        final Calendar dialogStartDate = Calendar.getInstance();
        final Calendar dialogEndDate = Calendar.getInstance();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            if (asignacion.getFechaInicio() != null) {
                dialogStartDate.setTime(sdf.parse(asignacion.getFechaInicio()));
            }
            if (asignacion.getFechaFin() != null) {
                dialogEndDate.setTime(sdf.parse(asignacion.getFechaFin()));
            }
        } catch (Exception e) {
            Log.e("DateError", "Error al parsear fechas", e);
        }

        layoutStartDate.setOnClickListener(v -> showDatePickerDialog(dialogStartDate, tvStartDate, true, dialogEndDate, tvEndDate));
        layoutEndDate.setOnClickListener(v -> showDatePickerDialog(dialogEndDate, tvEndDate, false, dialogStartDate, null));
        layoutStartTime.setOnClickListener(v -> showTimePickerDialog());

        // Bloquear selección
        spinnerPatients.setEnabled(false);
        spinnerMedications.setEnabled(false);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnSave.setOnClickListener(v -> {
            if (validarCampos(spinnerPatients, spinnerMedications, etDosage, etFrequencyNumber, tvStartDate, tvEndDate, tvStartTime)) {
                String frecuenciaNumero = etFrequencyNumber.getText().toString();
                String frecuenciaUnidad = spinnerFrequencyUnit.getSelectedItem().toString();
                String frecuenciaCompleta = frecuenciaNumero + " " + frecuenciaUnidad;
                String horaInicio = tvStartTime.getText().toString();

                AsignarMedicamentoRequest request = new AsignarMedicamentoRequest(
                        asignacion.getPaciente().getId(),
                        asignacion.getMedicamento().getId_medicamento(),
                        tvStartDate.getText().toString(),
                        tvEndDate.getText().toString(),
                        horaInicio,
                        etDosage.getText().toString(),
                        frecuenciaCompleta
                );

                actualizarMedicamento(asignacion.getIdAsignacion(), request, dialog);
            }
        });

        builder.setNegativeButton("Cancelar", (dialogInterface, which) -> dialogInterface.dismiss());
    }


    private void mostrarDialogoConfirmarEliminacion(AsignacionMedicamentoResponse asignacion) {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar este medicamento?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarMedicamento(asignacion))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoAgregarMedicamento() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.fragment_agregar_medicamento_usuario, null);
        builder.setView(viewInflated);

        Spinner spinnerPatients = viewInflated.findViewById(R.id.spinner_patients);
        Spinner spinnerMedications = viewInflated.findViewById(R.id.spinner_medications);
        Spinner spinnerFrequencyUnit = viewInflated.findViewById(R.id.spinner_frequency_unit);
        TextView tvStartDate = viewInflated.findViewById(R.id.tv_start_date);
        TextView tvEndDate = viewInflated.findViewById(R.id.tv_end_date);
        tvStartTime = viewInflated.findViewById(R.id.tv_start_time);
        EditText etDosage = viewInflated.findViewById(R.id.et_dosage);
        EditText etFrequencyNumber = viewInflated.findViewById(R.id.et_frequency_number);
        Button btnSave = viewInflated.findViewById(R.id.btn_save);
        LinearLayout layoutStartTime = viewInflated.findViewById(R.id.layout_start_time);

        ArrayAdapter<CharSequence> frequencyAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.frequency_units,
                android.R.layout.simple_spinner_item
        );
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrequencyUnit.setAdapter(frequencyAdapter);

        LinearLayout layoutStartDate = viewInflated.findViewById(R.id.layout_start_date);
        LinearLayout layoutEndDate = viewInflated.findViewById(R.id.layout_end_date);

        final Calendar dialogStartDate = Calendar.getInstance();
        final Calendar dialogEndDate = Calendar.getInstance();
        dialogEndDate.add(Calendar.DAY_OF_MONTH, 0);

        tvStartDate.setText(dateFormatter.format(dialogStartDate.getTime()));
        tvEndDate.setText(dateFormatter.format(dialogEndDate.getTime()));
        tvStartTime.setText("08:00"); // Hora por defecto

        layoutStartDate.setOnClickListener(v -> showDatePickerDialog(dialogStartDate, tvStartDate, true, dialogEndDate, tvEndDate));
        layoutEndDate.setOnClickListener(v -> showDatePickerDialog(dialogEndDate, tvEndDate, false, dialogStartDate, null));
        layoutStartTime.setOnClickListener(v -> showTimePickerDialog());

        setupSpinners(spinnerPatients, spinnerMedications);

        if (selectedPatientId != -1 && pacientesMap.containsKey(selectedPatientId)) {
            String selectedName = pacientesMap.get(selectedPatientId);
            int position = ((ArrayAdapter<String>) spinnerPatients.getAdapter()).getPosition(selectedName);
            if (position >= 0) {
                spinnerPatients.setSelection(position);
            }

        }

        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        btnSave.setOnClickListener(v -> {
            if (validarCampos(spinnerPatients, spinnerMedications, etDosage, etFrequencyNumber, tvStartDate, tvEndDate, tvStartTime)) {
                String pacienteNombre = spinnerPatients.getSelectedItem().toString();
                String medicamentoNombre = spinnerMedications.getSelectedItem().toString();
                String dosis = etDosage.getText().toString();
                String frecuenciaNumero = etFrequencyNumber.getText().toString();
                String frecuenciaUnidad = spinnerFrequencyUnit.getSelectedItem().toString();
                String fechaInicio = tvStartDate.getText().toString();
                String fechaFin = tvEndDate.getText().toString();
                String horaInicio = tvStartTime.getText().toString();

                String frecuenciaCompleta = frecuenciaNumero + " " + frecuenciaUnidad;

                int idPaciente = obtenerIdPorNombre(pacientesMap, pacienteNombre);
                int idMedicamento = obtenerIdPorNombre(medicamentosMap, medicamentoNombre);

                if (idPaciente == -1 || idMedicamento == -1) {
                    Toast.makeText(getContext(), "Error al obtener datos del paciente o medicamento", Toast.LENGTH_SHORT).show();
                    return;
                }

                AsignarMedicamentoRequest request = new AsignarMedicamentoRequest(
                        idPaciente,
                        idMedicamento,
                        fechaInicio,
                        fechaFin,
                        horaInicio,
                        dosis,
                        frecuenciaCompleta
                );

                asignarMedicamento(request,dialog);
            }
        });

        builder.setNegativeButton("Cancelar", (dialogInterface, which) -> dialogInterface.dismiss());
    }

    private void showTimePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        if (tvStartTime.getText() != null && !tvStartTime.getText().toString().isEmpty()) {
            try {
                String[] timeParts = tvStartTime.getText().toString().split(":");
                if (timeParts.length == 2) {
                    hour = Integer.parseInt(timeParts[0]);
                    minute = Integer.parseInt(timeParts[1]);
                }
            } catch (Exception e) {
                Log.e("TimePicker", "Error al parsear hora existente", e);
            }
        }

        timePickerDialog = new TimePickerDialog(getContext(),
                (view, hourOfDay, minute1) -> {
                    Calendar selectedTime = Calendar.getInstance();
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedTime.set(Calendar.MINUTE, minute1);
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
                    tvStartTime.setText(formattedTime);
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void showDatePickerDialog(final Calendar dateToSet, final TextView textView,
                                      final boolean isStartDate,
                                      final Calendar minDate, final TextView dependentTextView) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    dateToSet.set(year, month, dayOfMonth);
                    textView.setText(dateFormatter.format(dateToSet.getTime()));

                    if (isStartDate && dependentTextView != null && dateToSet.after(minDate)) {
                        minDate.setTime(dateToSet.getTime());
                        minDate.add(Calendar.DAY_OF_MONTH, 1);
                        dependentTextView.setText(dateFormatter.format(minDate.getTime()));
                    }
                },
                dateToSet.get(Calendar.YEAR),
                dateToSet.get(Calendar.MONTH),
                dateToSet.get(Calendar.DAY_OF_MONTH));

        if (!isStartDate && minDate != null) {
            datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        }

        datePickerDialog.show();
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

    private boolean validarCampos(Spinner spinnerPatients, Spinner spinnerMedications,
                                  EditText etDosage, EditText etFrequency,
                                  TextView tvStartDate, TextView tvEndDate, TextView tvStartTime) {
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
        if (etFrequency.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Ingresa la frecuencia", Toast.LENGTH_SHORT).show();
            return false;
        }
        String fechaInicio = tvStartDate.getText().toString().trim();
        String fechaFin = tvEndDate.getText().toString().trim();
        if (fechaInicio.isEmpty() || fechaFin.isEmpty()) {
            Toast.makeText(getContext(), "Selecciona fechas válidas", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (tvStartTime.getText().toString().trim().isEmpty() ||
                tvStartTime.getText().toString().equals("Seleccionar hora")) {
            Toast.makeText(getContext(), "Selecciona una hora de inicio", Toast.LENGTH_SHORT).show();
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