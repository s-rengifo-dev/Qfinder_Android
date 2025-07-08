package com.sena.qfinder.ui.cita;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.ApiClient;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.CitaMedica;
import com.sena.qfinder.data.models.PacienteListResponse;
import com.sena.qfinder.data.models.PacienteResponse;
import com.sena.qfinder.ui.home.Fragment_Serivicios;
import com.sena.qfinder.utils.CitaAlarmManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class CitasFragment extends Fragment {

    private static final String TAG = "CitasFragment";
    private RecyclerView recyclerCitas;
    private LinearLayout patientsContainer;
    private CitaAdapter citaAdapter;
    private ImageView btnBack;
    private int selectedPatientId = -1;
    private String selectedPatientName = "";
    private Map<Integer, String> pacientesMap = new HashMap<>();
    private List<CitaMedica> todasLasCitas = new ArrayList<>();
    private LayoutInflater currentInflater;
    private Context context;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_citas, container, false);
        currentInflater = inflater;
        context = getContext();

        // Inicializar vistas
        recyclerCitas = rootView.findViewById(R.id.recyclerCitas);
        patientsContainer = rootView.findViewById(R.id.patientsContainer);
        Button btnAgregarRecordatorio = rootView.findViewById(R.id.btnAgregarRecordatorio);
        btnBack = rootView.findViewById(R.id.btnBack);

        // Configurar RecyclerView
        recyclerCitas.setLayoutManager(new LinearLayoutManager(getContext()));
        citaAdapter = new CitaAdapter(new ArrayList<>());
        recyclerCitas.setAdapter(citaAdapter);

        // Listener para el botón de agregar recordatorio
        btnAgregarRecordatorio.setOnClickListener(v -> mostrarDialogoAgregarRecordatorio());

        // Listener para el botón de retroceso
        btnBack.setOnClickListener(v -> {
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            Fragment_Serivicios serviciosFragment = new Fragment_Serivicios();
            fragmentTransaction.replace(R.id.fragment_container, serviciosFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });

        // Cargar pacientes
        loadPacientes();

        return rootView;
    }

    private void loadPacientes() {
        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "No se encontró token de autenticación", Toast.LENGTH_SHORT).show();
            return;
        }

        Retrofit retrofit = ApiClient.getClient();
        AuthService authService = retrofit.create(AuthService.class);
        Call<PacienteListResponse> call = authService.listarPacientes("Bearer " + token);

        call.enqueue(new Callback<PacienteListResponse>() {
            @Override
            public void onResponse(@NonNull Call<PacienteListResponse> call, @NonNull Response<PacienteListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PacienteResponse> pacientes = response.body().getData();
                    if (pacientes != null && !pacientes.isEmpty()) {
                        mostrarPacientes(pacientes);
                    }
                } else {
                    Toast.makeText(getContext(), "Error al cargar pacientes", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PacienteListResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarPacientes(List<PacienteResponse> pacientes) {
        patientsContainer.removeAllViews();

        for (PacienteResponse paciente : pacientes) {
            String nombreCompleto = paciente.getNombre() + " " + paciente.getApellido();
            String diagnostico = paciente.getDiagnostico_principal() != null ?
                    paciente.getDiagnostico_principal() : "Sin diagnóstico";
            String imagenUrl = paciente.getImagen_paciente();

            addPatientCard(nombreCompleto, diagnostico, imagenUrl, paciente.getId());
        }
    }

    private void addPatientCard(String name, String conditions, String imagenUrl, int patientId) {
        View patientCard = currentInflater.inflate(R.layout.item_patient_card, patientsContainer, false);
        patientCard.setTag(patientId);

        TextView tvName = patientCard.findViewById(R.id.tvPatientName);
        TextView tvConditions = patientCard.findViewById(R.id.tvPatientConditions);
        ImageView ivProfile = patientCard.findViewById(R.id.ivPatientProfile);

        tvName.setText(name);
        tvConditions.setText(conditions != null && !conditions.isEmpty() ? "• " + conditions : "• Sin diagnóstico");

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

        // Aplicar apariencia inicial (idéntico a DashboardFragment)
        updateCardAppearance(patientCard, patientId == selectedPatientId);

        patientCard.setOnClickListener(v -> {
            selectedPatientId = patientId;
            selectedPatientName = name;
            updatePatientCardsHighlight();
            loadCitasDelPaciente(patientId);
            Toast.makeText(getContext(), "Mostrando citas de " + name, Toast.LENGTH_SHORT).show();
        });

        patientsContainer.addView(patientCard);
    }

    // Método idéntico al de DashboardFragment
    private void updatePatientCardsHighlight() {
        if (patientsContainer == null || getContext() == null) return;

        for (int i = 0; i < patientsContainer.getChildCount(); i++) {
            View child = patientsContainer.getChildAt(i);
            if (child.getTag() instanceof Integer) {
                int patientId = (int) child.getTag();
                updateCardAppearance(child, patientId == selectedPatientId);
            }
        }
    }

    // Método idéntico al de DashboardFragment
    private void updateCardAppearance(View cardView, boolean isSelected) {
        Context context = getContext();
        if (context == null) return;

        // Obtener el fondo original (debe ser un GradientDrawable)
        GradientDrawable drawable = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.card_background).mutate();
        cardView.setBackground(drawable);

        if (isSelected) {
            // Estilo cuando está seleccionado (mismos valores que DashboardFragment)
            drawable.setStroke(4, ContextCompat.getColor(context, R.color.selected_stroke_color));
            drawable.setColor(ContextCompat.getColor(context, R.color.selected_card_color));
            cardView.setElevation(8f);
        } else {
            // Volver al estilo original (mismos valores que DashboardFragment)
            drawable.setStroke(1, ContextCompat.getColor(context, R.color.default_stroke_color));
            drawable.setColor(ContextCompat.getColor(context, R.color.default_card_color));
            cardView.setElevation(2f);
        }
    }

    private void loadCitasDelPaciente(int pacienteId) {
        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "No se encontró token de autenticación", Toast.LENGTH_SHORT).show();
            return;
        }

        Retrofit retrofit = ApiClient.getClient();
        AuthService authService = retrofit.create(AuthService.class);
        Call<List<CitaMedica>> call = authService.listarCitasMedicas("Bearer " + token, pacienteId);

        call.enqueue(new Callback<List<CitaMedica>>() {
            @Override
            public void onResponse(@NonNull Call<List<CitaMedica>> call, @NonNull Response<List<CitaMedica>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    todasLasCitas = response.body();
                    Log.d(TAG, "Citas recibidas: " + todasLasCitas.size());
                    if (todasLasCitas != null && !todasLasCitas.isEmpty()) {
                        citaAdapter.updateData(todasLasCitas);
                    } else {
                        todasLasCitas = new ArrayList<>();
                        citaAdapter.updateData(new ArrayList<>());
                        Toast.makeText(getContext(), "No hay citas programadas para este paciente", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Error al cargar citas", Toast.LENGTH_SHORT).show();
                    todasLasCitas = new ArrayList<>();
                    citaAdapter.updateData(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CitaMedica>> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                todasLasCitas = new ArrayList<>();
                citaAdapter.updateData(new ArrayList<>());
            }
        });
    }

    private void mostrarDialogoAgregarRecordatorio() {
        if (selectedPatientId == -1) {
            Toast.makeText(getContext(), "Por favor selecciona un paciente primero", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_agregar_recordatorio);

        // Hacer fondo transparente
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);

        TextView tvTituloDialogo = dialog.findViewById(R.id.tvTituloDialogo);
        EditText etTitulo = dialog.findViewById(R.id.etTitulo);
        EditText etDescripcion = dialog.findViewById(R.id.etDescripcion);
        Spinner spinnerEstado = dialog.findViewById(R.id.spinnerEstado);
        Button btnGuardar = dialog.findViewById(R.id.btnGuardar);
        Button btnCancelar = dialog.findViewById(R.id.btnCancelar);
        EditText etFechaRecordatorio = dialog.findViewById(R.id.etFechaCita);
        EditText etHoraCita = dialog.findViewById(R.id.etHoraCita);

        etFechaRecordatorio.setShowSoftInputOnFocus(false);
        etHoraCita.setShowSoftInputOnFocus(false);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.estados_cita, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEstado.setAdapter(adapter);

        tvTituloDialogo.setText("Nueva cita para " + selectedPatientName);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        etFechaRecordatorio.setText(dateFormat.format(calendar.getTime()));
        etHoraCita.setText(timeFormat.format(calendar.getTime()));

        etFechaRecordatorio.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDatePicker(etFechaRecordatorio);
            }
        });
        etFechaRecordatorio.setOnClickListener(v -> showDatePicker(etFechaRecordatorio));

        etHoraCita.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showTimePicker(etHoraCita);
            }
        });
        etHoraCita.setOnClickListener(v -> showTimePicker(etHoraCita));

        btnGuardar.setOnClickListener(v -> {
            String titulo = etTitulo.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();
            String estado = spinnerEstado.getSelectedItem().toString().toLowerCase();
            String fechaStr = etFechaRecordatorio.getText().toString().trim();
            String horaStr = etHoraCita.getText().toString().trim();

            if (titulo.isEmpty()) {
                etTitulo.setError("El título es obligatorio");
                return;
            }
            if (titulo.length() < 5 || titulo.length() > 100) {
                etTitulo.setError("El título debe tener entre 5 y 100 caracteres");
                return;
            }
            if (fechaStr.isEmpty()) {
                etFechaRecordatorio.setError("La fecha es obligatoria");
                return;
            }
            if (horaStr.isEmpty()) {
                etHoraCita.setError("La hora es obligatoria");
                return;
            }

            try {
                SimpleDateFormat sdfFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date fecha = sdfFecha.parse(fechaStr);

                SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date hora = sdfHora.parse(horaStr);

                Calendar fechaCitaCal = Calendar.getInstance();
                fechaCitaCal.setTime(fecha);
                Calendar horaCitaCal = Calendar.getInstance();
                horaCitaCal.setTime(hora);

                fechaCitaCal.set(Calendar.HOUR_OF_DAY, horaCitaCal.get(Calendar.HOUR_OF_DAY));
                fechaCitaCal.set(Calendar.MINUTE, horaCitaCal.get(Calendar.MINUTE));
                fechaCitaCal.set(Calendar.SECOND, 0);
                fechaCitaCal.set(Calendar.MILLISECOND, 0);

                SimpleDateFormat sdfFechaCitaApi = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                sdfFechaCitaApi.setTimeZone(TimeZone.getTimeZone("UTC"));
                String fechaCita = sdfFechaCitaApi.format(fechaCitaCal.getTime());

                SimpleDateFormat sdfHoraApi = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String horaCita = sdfHoraApi.format(hora);

                Calendar reminderCal = Calendar.getInstance();
                reminderCal.setTime(fecha);
                reminderCal.add(Calendar.DAY_OF_YEAR, -1);
                reminderCal.set(Calendar.HOUR_OF_DAY, 8);
                reminderCal.set(Calendar.MINUTE, 0);
                reminderCal.set(Calendar.SECOND, 0);
                reminderCal.set(Calendar.MILLISECOND, 0);

                SimpleDateFormat sdfRecordatorioApi = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                String fechaRecordatorio = sdfRecordatorioApi.format(reminderCal.getTime());

                CitaMedica nuevaCita = new CitaMedica();
                nuevaCita.setTituloCita(titulo);
                nuevaCita.setDescripcion(descripcion);
                nuevaCita.setFechaCita(fechaCita);
                nuevaCita.setHoraCita(horaCita);
                nuevaCita.setFechaRecordatorio(fechaRecordatorio);
                nuevaCita.setEstado(estado);
                nuevaCita.setIdPaciente(selectedPatientId);
                nuevaCita.setNotificado1h(false);
                nuevaCita.setNotificado24h(false);

                Log.d(TAG, "Enviando cita: " + nuevaCita.toString());
                guardarCita(nuevaCita);
                dialog.dismiss();

            } catch (Exception e) {
                Toast.makeText(context, "Error al procesar fecha/hora", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error al procesar fecha/hora", e);
            }
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDatePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String fechaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    editText.setText(fechaSeleccionada);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showTimePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    String horaSeleccionada = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    editText.setText(horaSeleccionada);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);
        timePickerDialog.show();
    }

    private void guardarCita(CitaMedica cita) {
        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "No se encontró token de autenticación", Toast.LENGTH_SHORT).show();
            return;
        }

        Retrofit retrofit = ApiClient.getClient();
        AuthService authService = retrofit.create(AuthService.class);

        Call<CitaMedica> call = authService.crearCitaMedica("Bearer " + token, cita.getIdPaciente(), cita);

        call.enqueue(new Callback<CitaMedica>() {
            @Override
            public void onResponse(@NonNull Call<CitaMedica> call, @NonNull Response<CitaMedica> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CitaMedica citaCreada = response.body();
                    Toast.makeText(getContext(), "Cita guardada exitosamente", Toast.LENGTH_SHORT).show();

                    // Programar alarmas para la nueva cita
                    CitaAlarmManager.programarAlarmasParaCita(getContext(), citaCreada);

                    loadCitasDelPaciente(cita.getIdPaciente());
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Sin detalles";
                        Log.e(TAG, "Error al guardar cita. Código: " + response.code() + ", Mensaje: " + errorBody);
                        Toast.makeText(getContext(), "Error al guardar la cita: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error al leer errorBody", e);
                        Toast.makeText(getContext(), "Error al guardar la cita", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<CitaMedica> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error de conexión al guardar: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error de conexión al guardar cita", t);
            }
        });
    }
}