package com.sena.qfinder.ui.actividad;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.api.ApiClient;
import com.sena.qfinder.data.models.ActividadGetResponse;
import com.sena.qfinder.data.models.ActividadRequest;
import com.sena.qfinder.data.models.ActividadResponse;
import com.sena.qfinder.data.models.PacienteListResponse;
import com.sena.qfinder.data.models.PacienteResponse;
import com.sena.qfinder.utils.AlarmReceiver;

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
    private static final String ARG_ACTIVIDAD_ID = "actividad_id";
    private static final String ARG_IS_EDIT_MODE = "is_edit_mode";
    private static final int REQUEST_CODE_POST_NOTIFICATION = 1001;
    private static final int REQUEST_CODE_EXACT_ALARM_PERMISSION = 1002;

    private AutoCompleteTextView spinnerPacientes;
    private TextInputEditText etTipoActividad, etFecha, etHora, etDuracion, etDescripcion, etObservaciones;
    private AutoCompleteTextView spinnerIntensidad, spinnerEstado;
    private Button btnGuardar, btnCancelar, btnEliminar;
    private String fechaSeleccionada = "";
    private String horaSeleccionada = "";
    private List<PacienteResponse> listaPacientes = new ArrayList<>();
    private int pacienteId;
    private boolean isEditMode = false;
    private int actividadId = -1;

    // Variables para guardar datos temporales de la alarma
    private int tempActividadId;
    private String tempTitulo;
    private String tempDescripcion;
    private String tempFecha;
    private String tempHora;


    public interface OnActividadGuardadaListener {
        void onActividadGuardada();


    }

    private OnActividadGuardadaListener listener;

    public static AgregarActividadDialogFragment newInstance(int pacienteId) {
        AgregarActividadDialogFragment fragment = new AgregarActividadDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PACIENTE_ID, pacienteId);
        args.putBoolean(ARG_IS_EDIT_MODE, false);
        fragment.setArguments(args);
        return fragment;
    }

    public static AgregarActividadDialogFragment newInstance(ActividadGetResponse actividad) {
        AgregarActividadDialogFragment fragment = new AgregarActividadDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PACIENTE_ID, actividad.getIdPaciente());
        args.putInt(ARG_ACTIVIDAD_ID, actividad.getId());
        args.putBoolean(ARG_IS_EDIT_MODE, true);
        args.putString("tipoActividad", actividad.getTitulo());
        args.putString("fecha", actividad.getFecha());
        args.putString("hora", actividad.getHora());
        args.putString("descripcion", actividad.getDescripcion());
        args.putString("estado", actividad.getEstado());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pacienteId = getArguments().getInt(ARG_PACIENTE_ID, -1);
            isEditMode = getArguments().getBoolean(ARG_IS_EDIT_MODE, false);
            actividadId = getArguments().getInt(ARG_ACTIVIDAD_ID, -1);
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

        if (isEditMode) {
            cargarDatosActividad();
        }

        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(view);
        return dialog;
    }

    private void initViews(View view) {
        spinnerPacientes = view.findViewById(R.id.spinnerPaciente);
        spinnerPacientes.setEnabled(false);
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
        btnEliminar = view.findViewById(R.id.btnEliminar);

        // Configurar visibilidad de botones
        btnGuardar.setVisibility(View.VISIBLE); // Siempre visible
        btnGuardar.setText(isEditMode ? "Actualizar" : "Guardar"); // Cambiar texto

        btnEliminar.setVisibility(isEditMode ? View.VISIBLE : View.GONE); // Solo en edición
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

    private void cargarDatosActividad() {
        if (getArguments() == null) return;

        etTipoActividad.setText(getArguments().getString("tipoActividad", ""));

        // Convertir fecha de formato ISO a dd/MM/yyyy
        String fechaISO = getArguments().getString("fecha", "");
        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            sdfInput.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdfInput.parse(fechaISO);

            if (date != null) {
                SimpleDateFormat sdfOutput = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                fechaSeleccionada = sdfOutput.format(date);
                etFecha.setText(fechaSeleccionada);
            }
        } catch (ParseException e) {
            Log.e("Fecha", "Error al parsear fecha", e);
        }

        // Hora
        String horaISO = getArguments().getString("hora", "");
        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            sdfInput.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdfInput.parse(horaISO);

            if (date != null) {
                SimpleDateFormat sdfOutput = new SimpleDateFormat("HH:mm", Locale.getDefault());
                horaSeleccionada = sdfOutput.format(date);
                etHora.setText(horaSeleccionada);
            }
        } catch (ParseException e) {
            Log.e("Hora", "Error al parsear hora", e);
        }

        etDuracion.setText(String.valueOf(getArguments().getInt("duracion", 0)));
        spinnerIntensidad.setText(getArguments().getString("intensidad", ""), false);
        etDescripcion.setText(getArguments().getString("descripcion", ""));
        spinnerEstado.setText(getArguments().getString("estado", ""), false);
        etObservaciones.setText(getArguments().getString("observaciones", ""));
    }

    private void setupListeners() {
        etFecha.setOnClickListener(v -> mostrarDatePicker());
        etHora.setOnClickListener(v -> mostrarTimePicker());

        btnCancelar.setOnClickListener(v -> dismiss());
        btnGuardar.setOnClickListener(v -> validarYGuardarActividad());
        btnEliminar.setOnClickListener(v -> mostrarDialogoConfirmacionEliminacion());
    }

    private void mostrarDialogoConfirmacionEliminacion() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar esta actividad?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarActividad())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarActividad() {
        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "No se encontró token de autenticación", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthService authService = ApiClient.getClient().create(AuthService.class);
        Call<ActividadResponse> call = authService.eliminarActividad(
                "Bearer " + token,
                pacienteId,
                actividadId
        );

        call.enqueue(new Callback<ActividadResponse>() {
            @Override
            public void onResponse(Call<ActividadResponse> call, Response<ActividadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ActividadResponse actividadResponse = response.body();
                    if (actividadResponse.isSuccess()) {
                        Toast.makeText(getContext(), "Actividad eliminada exitosamente", Toast.LENGTH_SHORT).show();
                        cancelarAlarma(actividadId);
                        terminarGuardado();
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
                Log.e("API", "Error al eliminar actividad", t);
            }
        });
    }

    private void cancelarAlarma(int actividadId) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(requireContext(), AlarmReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                actividadId,
                intent,
                flags
        );

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        Log.d("Alarma", "Alarma cancelada para actividad ID: " + actividadId);
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

        if (isEditMode) {
            actualizarActividadEnAPI(pacienteId, actividadId, request);
        } else {
            guardarActividadEnAPI(pacienteId, request);
        }
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

                        // Guardar datos temporales para programar alarma
                        tempActividadId = actividadResponse.getData().getIdActividad();
                        tempTitulo = request.getTipoActividad();
                        tempDescripcion = request.getDescripcion();
                        tempFecha = fechaSeleccionada;
                        tempHora = horaSeleccionada;

                        // Programar alarma solo si el estado es "pendiente"
                        if ("pendiente".equals(request.getEstado())) {
                            programarAlarmaDespuesDeVerificarPermisos();
                        } else {
                            if (listener != null) {
                                listener.onActividadGuardada();
                            }
                            dismiss();
                        }
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

    private void actualizarActividadEnAPI(int idPaciente, int idActividad, ActividadRequest request) {
        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "No se encontró token de autenticación", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthService authService = ApiClient.getClient().create(AuthService.class);
        Call<ActividadResponse> call = authService.actualizarActividad(
                "Bearer " + token,
                idPaciente,
                idActividad,
                request
        );

        call.enqueue(new Callback<ActividadResponse>() {
            @Override
            public void onResponse(Call<ActividadResponse> call, Response<ActividadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ActividadResponse actividadResponse = response.body();
                    if (actividadResponse.isSuccess()) {
                        Toast.makeText(getContext(), "Actividad actualizada exitosamente", Toast.LENGTH_SHORT).show();

                        // Cancelar alarma anterior y programar nueva si es necesario
                        cancelarAlarma(idActividad);

                        if ("pendiente".equals(request.getEstado())) {
                            tempActividadId = idActividad;
                            tempTitulo = request.getTipoActividad();
                            tempDescripcion = request.getDescripcion();
                            tempFecha = fechaSeleccionada;
                            tempHora = horaSeleccionada;
                            programarAlarmaDespuesDeVerificarPermisos();
                        } else {
                            terminarGuardado();
                        }
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
                Log.e("API", "Error al actualizar actividad", t);
            }
        });
    }

    private void programarAlarmaDespuesDeVerificarPermisos() {
        // Verificar permisos para Android 13+ (notificaciones)
        if (checkNotificationPermission()) {
            // Verificar permisos para Android 12+ (alarmas exactas)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
                if (alarmManager.canScheduleExactAlarms()) {
                    programarAlarma(tempActividadId, tempTitulo, tempDescripcion, tempFecha, tempHora);
                    terminarGuardado();
                } else {
                    // Solicitar permiso para alarmas exactas
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivityForResult(intent, REQUEST_CODE_EXACT_ALARM_PERMISSION);
                    Toast.makeText(getContext(), "Por favor concede permiso para alarmas exactas", Toast.LENGTH_LONG).show();
                }
            } else {
                programarAlarma(tempActividadId, tempTitulo, tempDescripcion, tempFecha, tempHora);
                terminarGuardado();
            }
        } else {
            // Solicitar permiso para notificaciones
            requestNotificationPermission();
        }
    }

    private void programarAlarma(int actividadId, String titulo, String descripcion, String fecha, String hora) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = sdf.parse(fecha + " " + hora);

            if (date == null) {
                Log.e("Alarma", "Fecha/hora inválida");
                return;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            // Verificar si la hora ya pasó
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                Toast.makeText(getContext(), "La hora programada ya ha pasado", Toast.LENGTH_SHORT).show();
                return;
            }

            // Crear intent para el BroadcastReceiver
            Intent intent = new Intent(requireContext(), AlarmReceiver.class);
            intent.putExtra("actividad_id", actividadId);
            intent.putExtra("titulo", titulo);
            intent.putExtra("descripcion", descripcion);
            intent.putExtra("fecha", fecha);
            intent.putExtra("hora", hora);

            // Configurar flags para PendingIntent
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    actividadId,
                    intent,
                    flags
            );

            // Obtener el AlarmManager
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            // Programar la alarma
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }

            Log.d("Alarma", "Alarma programada para: " + calendar.getTime());
        } catch (ParseException e) {
            Log.e("Alarma", "Error al programar alarma", e);
        }
    }

    private void terminarGuardado() {
        if (listener != null) {
            listener.onActividadGuardada();
        }
        dismiss();
    }

    // Verificar permisos de notificación para Android 13+
    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Para versiones anteriores no se necesita permiso
    }

    // Solicitar permiso de notificación
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_CODE_POST_NOTIFICATION
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                programarAlarmaDespuesDeVerificarPermisos();
            } else {
                Toast.makeText(getContext(), "Permiso de notificaciones denegado. Las alarmas no funcionarán", Toast.LENGTH_SHORT).show();
                terminarGuardado();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EXACT_ALARM_PERMISSION) {
            // Verificar nuevamente si se concedió el permiso
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
                if (alarmManager.canScheduleExactAlarms()) {
                    programarAlarma(tempActividadId, tempTitulo, tempDescripcion, tempFecha, tempHora);
                } else {
                    Toast.makeText(getContext(), "Permiso no concedido. Las alarmas exactas no están disponibles", Toast.LENGTH_SHORT).show();
                }
            }
            terminarGuardado();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            // Establecer el tamaño del diálogo
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.95),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.9)
            );

            // Hacer transparente el fondo del diálogo
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

            // Opcional: Quitar las sombras/decoration si lo deseas
            // dialog.getWindow().setDimAmount(0f);
        }
    }
}