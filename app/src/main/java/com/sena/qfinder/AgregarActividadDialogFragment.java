package com.sena.qfinder;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.sena.qfinder.api.AuthService;
import com.sena.qfinder.api.ApiClient;
import com.sena.qfinder.models.ActividadRequest;
import com.sena.qfinder.models.ActividadResponse;
import com.sena.qfinder.models.PacienteListResponse;
import com.sena.qfinder.models.PacienteResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AgregarActividadDialogFragment extends DialogFragment {

    private static final String ARG_PACIENTE_ID = "paciente_id";
    private AutoCompleteTextView spinnerPacientes;
    private TextInputEditText etTipoActividad, etFecha, etHora, etDuracion, etDescripcion, etObservaciones;
    private AutoCompleteTextView spinnerIntensidad, spinnerEstado;
    private Button btnGuardar, btnCancelar;
    private String fechaSeleccionada = "";
    private String horaSeleccionada = "";
    private List<PacienteResponse> listaPacientes = new ArrayList<>();
    private int pacienteId;

    public interface OnActividadGuardadaListener {
        void onActividadGuardada();
    }

    private OnActividadGuardadaListener listener;

    public static AgregarActividadDialogFragment newInstance(int pacienteId) {
        AgregarActividadDialogFragment fragment = new AgregarActividadDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PACIENTE_ID, pacienteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pacienteId = getArguments().getInt(ARG_PACIENTE_ID, -1);
        }
    }

    public void setOnActividadGuardadaListener(OnActividadGuardadaListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_agregar_actividad, null);

        initViews(view);
        setupSpinners();
        setupListeners();
        cargarPacientesDesdeAPI();

        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(view);
        return dialog;
    }

    private void initViews(View view) {
        spinnerPacientes = view.findViewById(R.id.spinnerPaciente);
        spinnerPacientes.setEnabled(false); // Disable patient selection
        etTipoActividad = view.findViewById(R.id.etTipoActividad);
        etFecha = view.findViewById(R.id.etFecha);
        etHora = view.findViewById(R.id.etHora);
        etDuracion = view.findViewById(R.id.etDuracion);
        spinnerIntensidad = view.findViewById(R.id.spinnerIntensidad);
        spinnerEstado = view.findViewById(R.id.spinnerEstado);
        etDescripcion = view.findViewById(R.id.etDescripcion);
        etObservaciones = view.findViewById(R.id.etObservaciones);
        btnGuardar = view.findViewById(R.id.btnGuardar);
        btnCancelar = view.findViewById(R.id.btnCancelar);
    }

    private void setupSpinners() {
        String[] intensidades = {"baja", "media", "alta"};
        ArrayAdapter<String> adapterIntensidad = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                intensidades
        );
        spinnerIntensidad.setAdapter(adapterIntensidad);

        String[] estados = {"pendiente", "en_progreso", "completada", "cancelada"};
        ArrayAdapter<String> adapterEstado = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                estados
        );
        spinnerEstado.setAdapter(adapterEstado);
    }

    private void setupListeners() {
        etFecha.setOnClickListener(v -> mostrarDatePicker());
        etHora.setOnClickListener(v -> mostrarTimePicker());

        btnCancelar.setOnClickListener(v -> dismiss());

        btnGuardar.setOnClickListener(v -> validarYGuardarActividad());
    }

    private void mostrarDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, yearSelected, monthOfYear, dayOfMonth) -> {
                    fechaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, monthOfYear + 1, yearSelected);
                    etFecha.setText(fechaSeleccionada);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void mostrarTimePicker() {
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minuteSelected) -> {
                    horaSeleccionada = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteSelected);
                    etHora.setText(horaSeleccionada);
                },
                hour, minute, true
        );
        timePickerDialog.show();
    }

    private void cargarPacientesDesdeAPI() {
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
            public void onResponse(Call<PacienteListResponse> call, Response<PacienteListResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    listaPacientes = response.body().getData();

                    if (listaPacientes != null && !listaPacientes.isEmpty()) {
                        for (PacienteResponse paciente : listaPacientes) {
                            if (paciente.getId() == pacienteId) {
                                String nombreCompleto = paciente.getNombre() + " " + paciente.getApellido();
                                spinnerPacientes.setText(nombreCompleto, false);
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<PacienteListResponse> call, Throwable t) {
                if (!isAdded()) return;
                Log.e("API", "Error de conexión", t);
            }
        });
    }

    private void validarYGuardarActividad() {
        String tipoActividad = etTipoActividad.getText().toString().trim();
        if (TextUtils.isEmpty(tipoActividad)) {
            etTipoActividad.setError("Ingrese el tipo de actividad");
            return;
        } else if (tipoActividad.length() < 3) {
            etTipoActividad.setError("El tipo debe tener al menos 3 caracteres");
            return;
        }

        if (TextUtils.isEmpty(fechaSeleccionada)) {
            etFecha.setError("Seleccione una fecha");
            return;
        }

        if (TextUtils.isEmpty(horaSeleccionada)) {
            etHora.setError("Seleccione una hora");
            return;
        }

        String duracionStr = etDuracion.getText().toString().trim();
        if (TextUtils.isEmpty(duracionStr)) {
            etDuracion.setError("Ingrese la duración");
            return;
        }
        int duracion;
        try {
            duracion = Integer.parseInt(duracionStr);
            if (duracion <= 0) {
                etDuracion.setError("La duración debe ser positiva");
                return;
            }
        } catch (NumberFormatException e) {
            etDuracion.setError("Duración inválida");
            return;
        }

        if (TextUtils.isEmpty(spinnerIntensidad.getText().toString())) {
            spinnerIntensidad.setError("Seleccione la intensidad");
            return;
        }

        if (TextUtils.isEmpty(spinnerEstado.getText().toString())) {
            spinnerEstado.setError("Seleccione el estado");
            return;
        }

        String descripcion = etDescripcion.getText().toString().trim();
        if (TextUtils.isEmpty(descripcion)) {
            etDescripcion.setError("Ingrese una descripción");
            return;
        } else if (descripcion.length() < 5) {
            etDescripcion.setError("La descripción debe tener al menos 5 caracteres");
            return;
        }

        String observaciones = etObservaciones.getText().toString().trim();

        if (pacienteId == -1) {
            Toast.makeText(getContext(), "Error: Paciente no válido", Toast.LENGTH_SHORT).show();
            return;
        }

        String fechaISO = convertirFechaHoraISO(fechaSeleccionada, horaSeleccionada);
        if (fechaISO.isEmpty()) {
            Toast.makeText(getContext(), "Error en formato de fecha/hora", Toast.LENGTH_SHORT).show();
            return;
        }

        ActividadRequest request = new ActividadRequest(
                fechaISO,
                duracion,
                tipoActividad,
                spinnerIntensidad.getText().toString(),
                descripcion,
                spinnerEstado.getText().toString(),
                observaciones
        );

        guardarActividadEnAPI(pacienteId, request);
    }

    private String convertirFechaHoraISO(String fecha, String hora) {
        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = sdfInput.parse(fecha + " " + hora);

            if (date == null) {
                return "";
            }

            SimpleDateFormat sdfOutput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            sdfOutput.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdfOutput.format(date);
        } catch (ParseException e) {
            Log.e("FechaHora", "Error al convertir fecha/hora", e);
            return "";
        }
    }

    private void guardarActividadEnAPI(int idPaciente, ActividadRequest request) {
        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "No se encontró token de autenticación", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthService authService = ApiClient.getClient().create(AuthService.class);
        Call<ActividadResponse> call = authService.crearActividad("Bearer " + token, idPaciente, request);

        call.enqueue(new Callback<ActividadResponse>() {
            @Override
            public void onResponse(Call<ActividadResponse> call, Response<ActividadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ActividadResponse actividadResponse = response.body();
                    if (actividadResponse.isSuccess()) {
                        Toast.makeText(getContext(), "Actividad creada exitosamente", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onActividadGuardada();
                        }
                        dismiss();
                    } else {
                        Toast.makeText(getContext(), "Error: " + actividadResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Error en la respuesta del servidor", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ActividadResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API", "Error al crear actividad", t);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.95),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.9)
            );
        }
    }
}