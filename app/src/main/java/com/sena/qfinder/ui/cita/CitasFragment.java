package com.sena.qfinder.ui.cita;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.ApiClient;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.CitaMedica;
import com.sena.qfinder.data.models.PacienteListResponse;
import com.sena.qfinder.data.models.PacienteResponse;
import com.sena.qfinder.ui.home.Fragment_Serivicios;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
    private MaterialCalendarView calendarView;
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

    // Decoradores para diferentes estados de citas
    private EventDecorator decoratorCitasPendientes;
    private EventDecorator decoratorCitasCompletadas;
    private EventDecorator decoratorCitasCanceladas;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_citas, container, false);
        currentInflater = inflater;
        context = getContext();

        // Inicializar vistas
        calendarView = rootView.findViewById(R.id.calendarView);
        recyclerCitas = rootView.findViewById(R.id.recyclerCitas);
        patientsContainer = rootView.findViewById(R.id.patientsContainer);
        Button btnAgregarRecordatorio = rootView.findViewById(R.id.btnAgregarRecordatorio);
        ImageView btnBack = rootView.findViewById(R.id.btnBack);

        // Configurar RecyclerView
        recyclerCitas.setLayoutManager(new LinearLayoutManager(getContext()));
        citaAdapter = new CitaAdapter(new ArrayList<>());
        recyclerCitas.setAdapter(citaAdapter);

        // Configurar listeners
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (selected && selectedPatientId != -1) {
                mostrarCitasParaFecha(date);
            }
        });

        // Listener para el botón de agregar recordatorio
        btnAgregarRecordatorio.setOnClickListener(v -> mostrarDialogoAgregarRecordatorio());

        // Listener para el botón de retroceso
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                Fragment_Serivicios serviciosFragment = new Fragment_Serivicios(); // Asegúrate de tener esta clase creada
                fragmentTransaction.replace(R.id.fragment_container, serviciosFragment); // Usa el ID del contenedor de tus fragments
                fragmentTransaction.addToBackStack(null); // Opcional: para que puedas volver hacia adelante también
                fragmentTransaction.commit();
            }
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

        patientCard.setOnClickListener(v -> {
            selectedPatientId = patientId;
            selectedPatientName = name;
            updatePatientCardsHighlight(patientId);
            loadCitasDelPaciente(patientId);
            Toast.makeText(getContext(), "Mostrando citas de " + name, Toast.LENGTH_SHORT).show();
        });

        patientsContainer.addView(patientCard);
    }

    private void updatePatientCardsHighlight(int selectedId) {
        for (int i = 0; i < patientsContainer.getChildCount(); i++) {
            View child = patientsContainer.getChildAt(i);
            if (child.getTag() instanceof Integer) {
                int patientId = (int) child.getTag();
                if (patientId == selectedId) {
                    child.setBackgroundColor(Color.parseColor("#E3F2FD"));
                } else {
                    child.setBackgroundColor(Color.TRANSPARENT);
                }
            }
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
                        marcarDiasConCitasEnCalendario();
                        mostrarCitasParaFecha(calendarView.getSelectedDate());
                    } else {
                        todasLasCitas = new ArrayList<>();
                        limpiarMarcadoresCalendario();
                        citaAdapter.updateData(new ArrayList<>());
                        Toast.makeText(getContext(), "No hay citas programadas para este paciente", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Error al cargar citas", Toast.LENGTH_SHORT).show();
                    todasLasCitas = new ArrayList<>();
                    limpiarMarcadoresCalendario();
                    citaAdapter.updateData(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CitaMedica>> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                todasLasCitas = new ArrayList<>();
                limpiarMarcadoresCalendario();
                citaAdapter.updateData(new ArrayList<>());
            }
        });
    }

    private void marcarDiasConCitasEnCalendario() {
        limpiarMarcadoresCalendario();

        List<CalendarDay> fechasPendientes = new ArrayList<>();
        List<CalendarDay> fechasCompletadas = new ArrayList<>();
        List<CalendarDay> fechasCanceladas = new ArrayList<>();

        for (CitaMedica cita : todasLasCitas) {
            if (cita.getFechaCita() != null && !cita.getFechaCita().isEmpty()) {
                try {
                    // Parsear la fecha ISO (yyyy-MM-dd'T'HH:mm:ss.SSS'Z')
                    String fechaPart = cita.getFechaCita().split("T")[0];
                    String[] partes = fechaPart.split("-");

                    int year = Integer.parseInt(partes[0]);
                    int month = Integer.parseInt(partes[1]); // Esto ya es 1-12
                    int day = Integer.parseInt(partes[2]);

                    // CalendarDay.from() espera meses 0-11, así que restamos 1
                    CalendarDay calendarDay = CalendarDay.from(year, month - 1, day);

                    String estado = cita.getEstado() != null ? cita.getEstado().toLowerCase() : "";
                    switch (estado) {
                        case "completada":
                            fechasCompletadas.add(calendarDay);
                            break;
                        case "cancelada":
                            fechasCanceladas.add(calendarDay);
                            break;
                        default:
                            fechasPendientes.add(calendarDay);
                            break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error al procesar fecha: " + cita.getFechaCita(), e);
                }
            }
        }

        // Resto del método permanece igual...
        if (!fechasPendientes.isEmpty()) {
            decoratorCitasPendientes = new EventDecorator(Color.RED, fechasPendientes);
            calendarView.addDecorator(decoratorCitasPendientes);
        }

        if (!fechasCompletadas.isEmpty()) {
            decoratorCitasCompletadas = new EventDecorator(Color.GREEN, fechasCompletadas);
            calendarView.addDecorator(decoratorCitasCompletadas);
        }

        if (!fechasCanceladas.isEmpty()) {
            decoratorCitasCanceladas = new EventDecorator(Color.BLACK, fechasCanceladas);
            calendarView.addDecorator(decoratorCitasCanceladas);
        }

        calendarView.invalidateDecorators();
    }

    private void limpiarMarcadoresCalendario() {
        calendarView.removeDecorators();
        decoratorCitasPendientes = null;
        decoratorCitasCompletadas = null;
        decoratorCitasCanceladas = null;
    }

    private void mostrarCitasParaFecha(CalendarDay date) {
        if (date == null || todasLasCitas.isEmpty()) {
            citaAdapter.updateData(new ArrayList<>());
            return;
        }

        String fechaSeleccionada = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                date.getYear(), date.getMonth() + 1, date.getDay());

        List<CitaMedica> citasParaFecha = new ArrayList<>();
        for (CitaMedica cita : todasLasCitas) {
            if (cita.getFechaCita() != null && cita.getFechaCita().startsWith(fechaSeleccionada)) {
                citasParaFecha.add(cita);
            }
        }

        citaAdapter.updateData(citasParaFecha);

        if (citasParaFecha.isEmpty()) {
            Toast.makeText(getContext(), "No hay citas para esta fecha", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogoAgregarRecordatorio() {
        if (selectedPatientId == -1) {
            Toast.makeText(getContext(), "Por favor selecciona un paciente primero", Toast.LENGTH_SHORT).show();
            return;
        }

        CalendarDay selectedDate = calendarView.getSelectedDate();
        if (selectedDate == null) {
            Toast.makeText(getContext(), "Por favor selecciona una fecha en el calendario", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_agregar_recordatorio);
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

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.estados_cita, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEstado.setAdapter(adapter);

        tvTituloDialogo.setText("Nueva cita para " + selectedPatientName);

        // Set initial date and time
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        etFechaRecordatorio.setText(dateFormat.format(calendar.getTime()));
        etHoraCita.setText(timeFormat.format(calendar.getTime()));

        etFechaRecordatorio.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                    (view, year, month, dayOfMonth) -> {
                        String fechaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        etFechaRecordatorio.setText(fechaSeleccionada);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        etHoraCita.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                    (view, hourOfDay, minute) -> {
                        String horaSeleccionada = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                        etHoraCita.setText(horaSeleccionada);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true);
            timePickerDialog.show();
        });

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
                // Parse date and time
                SimpleDateFormat sdfFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date fecha = sdfFecha.parse(fechaStr);

                SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date hora = sdfHora.parse(horaStr);

                // Format for API
                SimpleDateFormat sdfFechaApi = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat sdfHoraApi = new SimpleDateFormat("HH:mm", Locale.getDefault()); // Cambiado a HH:mm
                SimpleDateFormat sdfRecordatorioApi = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

                String fechaCita = sdfFechaApi.format(fecha);
                String horaCita = sdfHoraApi.format(hora); // Sin agregar :00

                // Resto del código permanece igual...
                Calendar reminderCal = Calendar.getInstance();
                reminderCal.setTime(fecha);
                reminderCal.add(Calendar.DAY_OF_YEAR, -1);
                reminderCal.set(Calendar.HOUR_OF_DAY, 8);
                reminderCal.set(Calendar.MINUTE, 0);
                reminderCal.set(Calendar.SECOND, 0);
                String fechaRecordatorio = sdfRecordatorioApi.format(reminderCal.getTime());

                CitaMedica nuevaCita = new CitaMedica();
                nuevaCita.setTituloCita(titulo);
                nuevaCita.setDescripcion(descripcion);
                nuevaCita.setFechaCita(fechaCita + "T00:00:00.000Z");
                nuevaCita.setHoraCita(horaCita); // Ahora en formato HH:mm
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
                    Toast.makeText(getContext(), "Cita guardada exitosamente", Toast.LENGTH_SHORT).show();
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

    private static class EventDecorator implements DayViewDecorator {
        private final int color;
        private final HashSet<CalendarDay> dates;

        public EventDecorator(int color, Collection<CalendarDay> dates) {
            this.color = color;
            this.dates = new HashSet<>(dates);
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new DotSpan(10, color));
        }
    }
}