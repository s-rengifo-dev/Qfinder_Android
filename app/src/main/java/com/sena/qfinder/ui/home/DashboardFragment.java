package com.sena.qfinder.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sena.qfinder.ui.actividad.Actividad1Fragment;
import com.sena.qfinder.ui.medicamento.ListaAsignarMedicamentos;
import com.sena.qfinder.R;
import com.sena.qfinder.ui.paciente.RegistrarPaciente;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.api.ApiClient;
import com.sena.qfinder.data.models.ActividadGetResponse;
import com.sena.qfinder.data.models.ActividadListResponse;
import com.sena.qfinder.data.models.AsignacionMedicamentoResponse;
import com.sena.qfinder.data.models.PacienteListResponse;
import com.sena.qfinder.data.models.PacienteResponse;
import com.sena.qfinder.data.models.PerfilUsuarioResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DashboardFragment extends Fragment {

    private LinearLayout patientsContainer, activitiesContainer;

    private ImageButton boton1,boton2;
    private RecyclerView rvMedications;
    private SharedPreferences sharedPreferences;
    private TextView tvUserName;
    private int selectedPatientId = -1;
    private String selectedPatientName = "";
    private LayoutInflater currentInflater;
    private View rootView;
    private Map<Integer, String> pacientesMap = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);
        currentInflater = inflater;

        sharedPreferences = requireContext().getSharedPreferences("prefs_qfinder", Context.MODE_PRIVATE);

        tvUserName = rootView.findViewById(R.id.tvUserName);

        boton1=rootView.findViewById(R.id.botonActividad);
        boton2=rootView.findViewById(R.id.botonMedicamento);

        setupUserInfo();
        setupButtonListeners();

        // Primero cargamos los pacientes, las otras secciones se cargarán automáticamente
        setupPatientsSection();

        return rootView;
    }


    private void navigateToFragment1() {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new Actividad1Fragment()); // Reemplaza con tu Fragment
        transaction.addToBackStack("dashboard"); // Opcional: para poder volver atrás
        transaction.commit();
    }

    private void navigateToFragment2() {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new ListaAsignarMedicamentos()); // Reemplaza con tu Fragment
        transaction.addToBackStack("dashboard"); // Opcional: para poder volver atrás
        transaction.commit();
    }

    private void setupButtonListeners() {
        boton1.setOnClickListener(v -> {
            // Navegar a Fragment/Activity 1
            navigateToFragment1();
        });

        boton2.setOnClickListener(v -> {
            // Navegar a Fragment/Activity 2
            navigateToFragment2();
        });
    }
    private void setupUserInfo() {
        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            tvUserName.setText("Usuario desconocido");
            return;
        }

        ImageView ivUserProfile = rootView.findViewById(R.id.ivUserProfile); // Asegúrate de tener este ImageView en tu layout

        Retrofit retrofit = ApiClient.getClient();
        AuthService authService = retrofit.create(AuthService.class);
        Call<PerfilUsuarioResponse> call = authService.obtenerPerfil("Bearer " + token);

        call.enqueue(new Callback<PerfilUsuarioResponse>() {
            @Override
            public void onResponse(@NonNull Call<PerfilUsuarioResponse> call, @NonNull Response<PerfilUsuarioResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PerfilUsuarioResponse usuario = response.body();
                    String nombreCompleto = usuario.getNombre_usuario() + " " + usuario.getApellido_usuario();
                    tvUserName.setText(nombreCompleto);

                    // Guardar nombre en SharedPreferences
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("nombre_completo", nombreCompleto);

                    // Guardar URL de la imagen si existe
                    if (usuario.getImagen_usuario() != null && !usuario.getImagen_usuario().isEmpty()) {
                        editor.putString("imagen_usuario", usuario.getImagen_usuario());

                        // Cargar imagen con Glide
                        Glide.with(requireContext())
                                .load(usuario.getImagen_usuario())
                                .placeholder(R.drawable.perfil_familiar) // Imagen por defecto
                                .error(R.drawable.perfil_familiar) // Imagen si hay error
                                .circleCrop() // Para hacerla circular
                                .into(ivUserProfile);
                    } else {
                        ivUserProfile.setImageResource(R.drawable.perfil_familiar);
                    }

                    editor.apply();
                } else {
                    String nombreGuardado = preferences.getString("nombre_completo", "Usuario");
                    tvUserName.setText(nombreGuardado);

                    // Cargar imagen guardada si existe
                    String imagenGuardada = preferences.getString("imagen_usuario", null);
                    if (imagenGuardada != null && !imagenGuardada.isEmpty()) {
                        Glide.with(requireContext())
                                .load(imagenGuardada)
                                .placeholder(R.drawable.perfil_familiar)
                                .error(R.drawable.perfil_familiar)
                                .circleCrop()
                                .into(ivUserProfile);
                    } else {
                        ivUserProfile.setImageResource(R.drawable.perfil_familiar);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<PerfilUsuarioResponse> call, @NonNull Throwable t) {
                Log.e("DashboardFragment", "Error al obtener perfil", t);
                String nombreGuardado = preferences.getString("nombre_completo", "Usuario");
                tvUserName.setText(nombreGuardado);

                // Cargar imagen guardada si existe
                String imagenGuardada = preferences.getString("imagen_usuario", null);
                if (imagenGuardada != null && !imagenGuardada.isEmpty()) {
                    Glide.with(requireContext())
                            .load(imagenGuardada)
                            .placeholder(R.drawable.perfil_familiar)
                            .error(R.drawable.perfil_familiar)
                            .circleCrop()
                            .into(ivUserProfile);
                } else {
                    ivUserProfile.setImageResource(R.drawable.perfil_familiar);
                }
            }
        });
    }

    private void setupPatientsSection() {
        patientsContainer = rootView.findViewById(R.id.patientsContainer);
        patientsContainer.removeAllViews();

        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            mostrarPacientes(new ArrayList<>());
            return;
        }

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        AuthService authService = retrofit.create(AuthService.class);
        Call<PacienteListResponse> call = authService.listarPacientes("Bearer " + token);

        call.enqueue(new Callback<PacienteListResponse>() {
            @Override
            public void onResponse(@NonNull Call<PacienteListResponse> call, @NonNull Response<PacienteListResponse> response) {
                if (!isAdded()) return;

                try {
                    if (response.isSuccessful()) {
                        if (response.body() != null) {
                            List<PacienteResponse> pacientes = response.body().getData();
                            if (pacientes != null) {
                                for (PacienteResponse paciente : pacientes) {
                                    String nombreCompleto = paciente.getNombre() + " " + paciente.getApellido();
                                    pacientesMap.put(paciente.getId(), nombreCompleto);
                                }
                            }
                            mostrarPacientes(pacientes != null ? pacientes : new ArrayList<>());
                        }
                    } else {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Sin detalles";
                        Log.e("API", "Error: " + response.code() + " - " + errorBody);
                        mostrarPacientes(new ArrayList<>());
                    }
                } catch (Exception e) {
                    Log.e("API", "Error procesando respuesta", e);
                    mostrarPacientes(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PacienteListResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e("API", "Fallo en la conexión", t);
                mostrarPacientes(new ArrayList<>());
            }
        });
    }

    private void mostrarPacientes(List<PacienteResponse> pacientes) {
        patientsContainer.removeAllViews();

        // ÚNICO CAMBIO REALIZADO: Selección automática del primer paciente
        if (pacientes != null && !pacientes.isEmpty()) {
            PacienteResponse primerPaciente = pacientes.get(0);
            selectedPatientId = primerPaciente.getId();
            selectedPatientName = primerPaciente.getNombre() + " " + primerPaciente.getApellido();

            // Cargar sus datos automáticamente
            loadPatientActivities();
            setupMedicationsSection();
        }

        // Resto del método permanece exactamente igual
        for (PacienteResponse paciente : pacientes) {
            String nombreCompleto = paciente.getNombre() + " " + paciente.getApellido();
            String diagnostico = paciente.getDiagnostico_principal() != null ?
                    paciente.getDiagnostico_principal() : "Sin diagnóstico";

            String fecha_nacimiento = paciente.getFecha_nacimiento();
            String imagenPaciente=paciente.getImagen_paciente();

            addPatientCard(nombreCompleto, fecha_nacimiento, diagnostico,
                    imagenPaciente, paciente.getId());
        }

        View addCard = currentInflater.inflate(R.layout.item_add_patient_card, patientsContainer, false);
        addCard.setOnClickListener(v -> navigateToAddPatient());
        patientsContainer.addView(addCard);
    }

    private void addPatientCard(String name, String relation, String conditions, String imageUrl, int patientId) {
        View patientCard = currentInflater.inflate(R.layout.item_patient_card, patientsContainer, false);
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

        // Cargar imagen con Glide
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(requireContext())
                    .load(imageUrl)
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
            updatePatientCardsHighlight();
            loadPatientActivities();
            setupMedicationsSection();
            Toast.makeText(getContext(), "Mostrando actividades y medicamentos de " + name, Toast.LENGTH_SHORT).show();
        });
        patientsContainer.addView(patientCard);
    }

    private void updatePatientCardsHighlight() {
        // Método permanece exactamente igual sin cambios
        for (int i = 0; i < patientsContainer.getChildCount(); i++) {
            View child = patientsContainer.getChildAt(i);
            if (child.getTag() instanceof Integer) {
                // Lógica existente sin modificaciones
            }
        }
    }

    private void setupActivitiesSection() {
        activitiesContainer = rootView.findViewById(R.id.activitiesContainer);
        activitiesContainer.removeAllViews();

        View headerView = currentInflater.inflate(R.layout.section_header, activitiesContainer, false);
        TextView tvTitle = headerView.findViewById(R.id.tvSectionTitle);
        TextView tvSubtitle = headerView.findViewById(R.id.tvSectionSubtitle);

        if (selectedPatientId == -1) {
            tvTitle.setText("Actividades");
            tvSubtitle.setText("Seleccione un paciente");
            activitiesContainer.addView(headerView);

            View noPatientView = currentInflater.inflate(R.layout.item_no_selection, activitiesContainer, false);
            ((TextView) noPatientView.findViewById(R.id.tvMessage)).setText("Selecciona un paciente para ver sus actividades");
            activitiesContainer.addView(noPatientView);
        } else {
            tvTitle.setText("Actividades de " + selectedPatientName);
            tvSubtitle.setText("Últimas actividades registradas");
            activitiesContainer.addView(headerView);

            View loadingView = currentInflater.inflate(R.layout.item_loading, activitiesContainer, false);
            activitiesContainer.addView(loadingView);
        }
    }

    private void loadPatientActivities() {
        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            return;
        }

        if (selectedPatientId == -1) {
            setupActivitiesSection();
            return;
        }

        Retrofit retrofit = ApiClient.getClient();
        AuthService authService = retrofit.create(AuthService.class);
        Call<ActividadListResponse> call = authService.listarActividades("Bearer " + token, selectedPatientId);

        call.enqueue(new Callback<ActividadListResponse>() {
            @Override
            public void onResponse(@NonNull Call<ActividadListResponse> call, @NonNull Response<ActividadListResponse> response) {
                if (!isAdded()) return;

                Log.d("API_ACTIVIDADES", "Código de respuesta: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    ActividadListResponse respuesta = response.body();
                    if (respuesta.isSuccess() && respuesta.getData() != null) {
                        List<ActividadGetResponse> actividades = respuesta.getData();
                        Log.d("API_ACTIVIDADES", "Actividades recibidas: " + actividades.size());
                        mostrarActividades(actividades);
                    } else {
                        mostrarActividades(new ArrayList<>());
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Sin detalles";
                        Log.e("API_ACTIVIDADES", "Error: " + response.code() + " - " + errorBody);
                    } catch (Exception e) {
                        Log.e("API_ACTIVIDADES", "Error al leer errorBody", e);
                    }
                    mostrarActividades(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ActividadListResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e("API_ACTIVIDADES", "Fallo en la conexión", t);
                mostrarActividades(new ArrayList<>());
            }
        });
    }

    private void mostrarActividades(List<ActividadGetResponse> actividades) {
        LinearLayout activitiesContainer = rootView.findViewById(R.id.activitiesContainer);

        if (activitiesContainer != null) {
            activitiesContainer.removeAllViews();
        } else {
            Log.e("DashboardFragment", "activitiesContainer es null");
            return; // Salir si es null para evitar NullPointerException
        }

        if (actividades == null || actividades.isEmpty()) {
            View noActivitiesView = currentInflater.inflate(R.layout.item_no_selection, activitiesContainer, false);
            ((TextView) noActivitiesView.findViewById(R.id.tvMessage)).setText("No hay actividades registradas");
            activitiesContainer.addView(noActivitiesView);
            return;
        }

        View tableView = currentInflater.inflate(R.layout.item_activity_table, activitiesContainer, false);
        TableLayout tableLayout = tableView.findViewById(R.id.activitiesTable);

        for (ActividadGetResponse actividad : actividades) {
            TableRow row = new TableRow(getContext());
            row.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT));

            String titulo = actividad.getTitulo() != null ?
                    actividad.getTitulo() : "Sin título";
            String descripcion = actividad.getDescripcion() != null ?
                    actividad.getDescripcion() : "Sin descripción";

            String fecha = actividad.getFecha() != null ?
                    formatDate(actividad.getFecha()) :
                    new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

            String hora = actividad.getHora() != null ?
                    formatTime(actividad.getHora()) :
                    new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

            String estado = actividad.getEstado() != null ?
                    actividad.getEstado() : "pendiente";

            addDataCell(row, titulo, 1.5f, 14);
            addDataCell(row, fecha, 1f, 12);
            addDataCell(row, hora, 1f, 14);

            TextView statusCell = new TextView(getContext());
            statusCell.setText(estado);
            statusCell.setTextSize(12);
            statusCell.setPadding(4, 4, 4, 4);
            statusCell.setLayoutParams(new TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f));

            switch(estado.toLowerCase()) {
                case "cancelada":
                    statusCell.setTextColor(ContextCompat.getColor(getContext(), R.color.rojopasion));
                    break;
                case "completada":
                    statusCell.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
                    break;
                case "en_progreso":
                    statusCell.setTextColor(ContextCompat.getColor(getContext(), R.color.azulito));
                    break;
                default:
                    statusCell.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
            }

            row.addView(statusCell);
            tableLayout.addView(row);

            View divider = new View(getContext());
            divider.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    1));
            divider.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.azulitoPrincipio));
            tableLayout.addView(divider);
        }

        activitiesContainer.addView(tableView);
    }


    private void addDataCell(TableRow row, String text, float weight, int textSizeSp) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        textView.setPadding(4, 4, 4, 4);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setMaxLines(1);

        TableRow.LayoutParams params = new TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.WRAP_CONTENT,
                weight);
        textView.setLayoutParams(params);
        row.addView(textView);
    }

    private String formatDate(String fecha) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = inputFormat.parse(fecha);
            return outputFormat.format(date);
        } catch (Exception e) {
            return fecha;
        }
    }

    private String formatTime(String hora) {
        try {
            if (hora.length() > 8) hora = hora.substring(0, 8);
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            Date time = inputFormat.parse(hora);

            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return outputFormat.format(time);
        } catch (Exception e) {
            return hora.length() > 5 ? hora.substring(0, 5) : hora;
        }
    }

    private void setupMedicationsSection() {
        rvMedications = rootView.findViewById(R.id.rvMedications);
        rvMedications.setLayoutManager(new LinearLayoutManager(getContext()));

        ProgressBar progressBar = rootView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        Retrofit retrofit = ApiClient.getClient();
        AuthService authService = retrofit.create(AuthService.class);

        int pacienteId = selectedPatientId != -1 ? selectedPatientId : -1;

        Call<List<AsignacionMedicamentoResponse>> call = authService.listarAsignacionesMedicamentos("Bearer " + token, pacienteId);

        call.enqueue(new Callback<List<AsignacionMedicamentoResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<AsignacionMedicamentoResponse>> call,
                                   @NonNull Response<List<AsignacionMedicamentoResponse>> response) {
                progressBar.setVisibility(View.GONE);

                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    List<AsignacionMedicamentoResponse> asignaciones = response.body();

                    if (asignaciones != null && !asignaciones.isEmpty()) {
                        List<Medication> medications = new ArrayList<>();

                        for (AsignacionMedicamentoResponse asignacion : asignaciones) {
                            String nombreMedicamento = asignacion.getMedicamento() != null ?
                                    asignacion.getMedicamento().getNombre() : "Medicamento desconocido";

                            String pacienteNombre = pacientesMap.containsKey(asignacion.getPaciente()) ?
                                    pacientesMap.get(asignacion.getPaciente()) : "Paciente desconocido";

                            medications.add(new Medication(
                                    nombreMedicamento,
                                    asignacion.getDosis() != null ? asignacion.getDosis() : "Dosis no especificada",
                                    asignacion.getFrecuencia() != null ? asignacion.getFrecuencia() : "Frecuencia no especificada",
                                    pacienteNombre,
                                    asignacion.getFechaInicio(),
                                    asignacion.getFechaFin()
                            ));
                        }

                        rvMedications.setAdapter(new MedicationAdapter(medications));
                    } else {
                        rvMedications.setAdapter(new MedicationAdapter(new ArrayList<>()));
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ?
                                "Error: " + response.errorBody().string() :
                                "Error: Código " + response.code();
                    } catch (Exception e) {
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AsignacionMedicamentoResponse>> call,
                                  @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                if (!isAdded() || call.isCanceled()) return;
                Log.e("API Error", "Error al obtener medicamentos", t);
            }
        });
    }

    private void navigateToAddPatient() {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new RegistrarPaciente());
        transaction.addToBackStack("dashboard");
        transaction.commit();
    }

    static class Medication {
        String name;
        String dosage;
        String schedule;
        String patientName;
        String startDate;
        String endDate;

        Medication(String name, String dosage, String schedule, String patientName, String startDate, String endDate) {
            this.name = name;
            this.dosage = dosage;
            this.schedule = schedule;
            this.patientName = patientName;
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    static class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.ViewHolder> {
        private final List<Medication> medications;

        MedicationAdapter(List<Medication> medications) {
            this.medications = medications;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_medication, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Medication med = medications.get(position);
            holder.tvName.setText(med.name);
            holder.tvDosage.setText(med.dosage);
            holder.tvSchedule.setText(med.schedule);

            if (med.patientName != null) {
                holder.tvPatient.setText(med.patientName);
                holder.tvPatient.setVisibility(View.VISIBLE);
            } else {
                holder.tvPatient.setVisibility(View.GONE);
            }

            String datesText = "";
            if (med.startDate != null) {
                datesText += "Inicio: " + formatDate(med.startDate);
            }
            if (med.endDate != null) {
                datesText += (datesText.isEmpty() ? "" : "\n") + "Fin: " + formatDate(med.endDate);
            }
            holder.tvDates.setText(datesText);
        }

        @Override
        public int getItemCount() {
            return medications.size();
        }

        private String formatDate(String dateStr) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = inputFormat.parse(dateStr);
                return outputFormat.format(date);
            } catch (Exception e) {
                return dateStr;
            }
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDosage, tvSchedule, tvPatient, tvDates;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvMedicationName);
                tvDosage = itemView.findViewById(R.id.tvDosage);
                tvSchedule = itemView.findViewById(R.id.tvSchedule);
                tvPatient = itemView.findViewById(R.id.tvPatient);
                tvDates = itemView.findViewById(R.id.tvDates);
            }
        }
    }
}