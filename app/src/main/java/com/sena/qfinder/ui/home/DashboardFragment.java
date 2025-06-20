package com.sena.qfinder.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
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
import androidx.fragment.app.FragmentManager;
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
    private ImageButton boton1, boton2;
    private RecyclerView rvMedications;
    private SharedPreferences sharedPreferences;
    private TextView tvUserName;
    private int selectedPatientId = -1;
    private String selectedPatientName = "";
    private LayoutInflater currentInflater;
    private View rootView;
    private Map<Integer, String> pacientesMap = new HashMap<>();

    // Lista para almacenar las llamadas activas y poder cancelarlas
    private List<Call<?>> activeCalls = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);
        currentInflater = inflater;

        // Inicialización del RecyclerView
        rvMedications = rootView.findViewById(R.id.rvMedications);
        rvMedications.setLayoutManager(new LinearLayoutManager(getContext()));

        Context context = getContext();
        if (context == null) {
            return rootView;
        }

        sharedPreferences = context.getSharedPreferences("prefs_qfinder", Context.MODE_PRIVATE);

        tvUserName = rootView.findViewById(R.id.tvUserName);
        boton1 = rootView.findViewById(R.id.botonActividad);
        boton2 = rootView.findViewById(R.id.botonMedicamento);
        patientsContainer = rootView.findViewById(R.id.patientsContainer);
        activitiesContainer = rootView.findViewById(R.id.activitiesContainer);

        // Configurar botón de soporte (WhatsApp)
        ImageView soporteBtn = rootView.findViewById(R.id.imSoporte);
        if (soporteBtn != null) {
            soporteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String numeroWhatsApp = "573234103221"; // SIN +, solo código país + número
                    String mensaje = "Hola, necesito soporte desde la app QFinder.";
                    String url = "https://api.whatsapp.com/send?phone=" + numeroWhatsApp + "&text=" + Uri.encode(mensaje);

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    intent.setPackage("com.whatsapp");

                    try {
                        startActivity(intent);
                    } catch (android.content.ActivityNotFoundException e) {
                        Toast.makeText(requireContext(), "WhatsApp no está instalado.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }


        setupUserInfo();
        setupButtonListeners();
        setupPatientsSection();

        return rootView;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancelar todas las llamadas activas cuando el fragmento se destruye
        for (Call<?> call : activeCalls) {
            if (call != null && !call.isCanceled()) {
                call.cancel();
            }
        }
        activeCalls.clear();
    }

    private void navigateToFragment1() {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new Actividad1Fragment());
        transaction.addToBackStack("dashboard");
        transaction.commit();
    }

    private void navigateToFragment2() {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new ListaAsignarMedicamentos());
        transaction.addToBackStack("dashboard");
        transaction.commit();
    }

    private void navigateToAddPatient() {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new RegistrarPaciente());
        transaction.addToBackStack("dashboard");
        transaction.commit();
    }

    private void setupButtonListeners() {
        boton1.setOnClickListener(v -> navigateToFragment1());
        boton2.setOnClickListener(v -> navigateToFragment2());
    }

    private void setupUserInfo() {
        Context context = getContext();
        if (context == null) return;

        SharedPreferences preferences = context.getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            tvUserName.setText("Usuario desconocido");
            return;
        }

        ImageView ivUserProfile = rootView.findViewById(R.id.ivUserProfile);

        ivUserProfile.setOnClickListener(v -> openPerfilUsuarioFragment());

        Retrofit retrofit = ApiClient.getClient();
        AuthService authService = retrofit.create(AuthService.class);
        Call<PerfilUsuarioResponse> call = authService.obtenerPerfil("Bearer " + token);
        activeCalls.add(call);

        call.enqueue(new Callback<PerfilUsuarioResponse>() {
            @Override
            public void onResponse(@NonNull Call<PerfilUsuarioResponse> call, @NonNull Response<PerfilUsuarioResponse> response) {
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    PerfilUsuarioResponse usuario = response.body();
                    String nombreCompleto = usuario.getNombre_usuario() + " " + usuario.getApellido_usuario();
                    tvUserName.setText(nombreCompleto);

                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("nombre_completo", nombreCompleto);

                    if (usuario.getImagen_usuario() != null && !usuario.getImagen_usuario().isEmpty()) {
                        editor.putString("imagen_usuario", usuario.getImagen_usuario());
                        Glide.with(context)
                                .load(usuario.getImagen_usuario())
                                .placeholder(R.drawable.perfil_familiar)
                                .error(R.drawable.perfil_familiar)
                                .circleCrop()
                                .into(ivUserProfile);
                    } else {
                        ivUserProfile.setImageResource(R.drawable.perfil_familiar);
                    }
                    editor.apply();
                } else {
                    handleUserInfoFallback(preferences, ivUserProfile);
                }
            }

            @Override
            public void onFailure(@NonNull Call<PerfilUsuarioResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                if (!call.isCanceled()) {
                    Log.e("DashboardFragment", "Error al obtener perfil", t);
                    handleUserInfoFallback(preferences, ivUserProfile);
                }
            }
        });
    }

    private void openPerfilUsuarioFragment() {
        Fragment perfilFragment = new PerfilUsuario(); // Reemplaza con el nombre real del fragmento
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, perfilFragment); // Reemplaza con el ID de tu contenedor
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void handleUserInfoFallback(SharedPreferences preferences, ImageView ivUserProfile) {
        Context context = getContext();
        if (context == null) return;

        String nombreGuardado = preferences.getString("nombre_completo", "Usuario");
        tvUserName.setText(nombreGuardado);

        String imagenGuardada = preferences.getString("imagen_usuario", null);
        if (imagenGuardada != null && !imagenGuardada.isEmpty()) {
            Glide.with(context)
                    .load(imagenGuardada)
                    .placeholder(R.drawable.perfil_familiar)
                    .error(R.drawable.perfil_familiar)
                    .circleCrop()
                    .into(ivUserProfile);
        } else {
            ivUserProfile.setImageResource(R.drawable.perfil_familiar);
        }
    }

    private void setupPatientsSection() {
        patientsContainer.removeAllViews();

        Context context = getContext();
        if (context == null) {
            mostrarPacientes(new ArrayList<>());
            return;
        }

        SharedPreferences preferences = context.getSharedPreferences("usuario", Context.MODE_PRIVATE);
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
        activeCalls.add(call);

        call.enqueue(new Callback<PacienteListResponse>() {
            @Override
            public void onResponse(@NonNull Call<PacienteListResponse> call, @NonNull Response<PacienteListResponse> response) {
                if (!isAdded() || getContext() == null) return;

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
                if (!isAdded() || getContext() == null) return;
                if (!call.isCanceled()) {
                    Log.e("API", "Fallo en la conexión", t);
                    mostrarPacientes(new ArrayList<>());
                }
            }
        });
    }

    private void mostrarPacientes(List<PacienteResponse> pacientes) {
        patientsContainer.removeAllViews();

        if (pacientes != null && !pacientes.isEmpty()) {
            PacienteResponse primerPaciente = pacientes.get(0);
            selectedPatientId = primerPaciente.getId();
            selectedPatientName = primerPaciente.getNombre() + " " + primerPaciente.getApellido();

            loadPatientActivities();
            setupMedicationsSection();
        }

        for (PacienteResponse paciente : pacientes) {
            String nombreCompleto = paciente.getNombre() + " " + paciente.getApellido();
            String diagnostico = paciente.getDiagnostico_principal() != null ?
                    paciente.getDiagnostico_principal() : "Sin diagnóstico";

            String fecha_nacimiento = paciente.getFecha_nacimiento();
            String imagenPaciente = paciente.getImagen_paciente();

            addPatientCard(nombreCompleto, fecha_nacimiento, diagnostico, imagenPaciente, paciente.getId());
        }

        View addCard = currentInflater.inflate(R.layout.item_add_patient_card, patientsContainer, false);
        addCard.setOnClickListener(v -> navigateToAddPatient());
        patientsContainer.addView(addCard);
    }

    private void addPatientCard(String name, String relation, String conditions, String imageUrl, int patientId) {
        Context context = getContext();
        if (context == null) return;

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

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
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
            updatePatientCardsHighlight();
            loadPatientActivities();
            setupMedicationsSection();
            Toast.makeText(context, "Mostrando actividades y medicamentos de " + name, Toast.LENGTH_SHORT).show();
        });
        patientsContainer.addView(patientCard);
    }

    private void updatePatientCardsHighlight() {
        if (patientsContainer == null || getContext() == null) return;

        for (int i = 0; i < patientsContainer.getChildCount(); i++) {
            View child = patientsContainer.getChildAt(i);
            if (child.getTag() instanceof Integer) {
                int patientId = (int) child.getTag();

                // Solo cambiamos el estado seleccionado sin tocar el fondo
                child.setSelected(patientId == selectedPatientId);

                // Opcional: Cambiar la elevación para feedback visual
                child.setElevation(patientId == selectedPatientId ? 8f : 2f);
            }
        }
    }

    private void setupActivitiesSection() {
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

            View loadingView = currentInflater.inflate(R.layout.item_loading, activitiesContainer, false);
            activitiesContainer.addView(loadingView);
        }
    }

    private void loadPatientActivities() {
        Context context = getContext();
        if (context == null) return;

        setupActivitiesSection();

        SharedPreferences preferences = context.getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null || selectedPatientId == -1) {
            return;
        }

        Retrofit retrofit = ApiClient.getClient();
        AuthService authService = retrofit.create(AuthService.class);
        Call<ActividadListResponse> call = authService.listarActividades("Bearer " + token, selectedPatientId);
        activeCalls.add(call);

        call.enqueue(new Callback<ActividadListResponse>() {
            @Override
            public void onResponse(@NonNull Call<ActividadListResponse> call, @NonNull Response<ActividadListResponse> response) {
                if (!isAdded() || getContext() == null) return;

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
                if (!isAdded() || getContext() == null) return;
                if (!call.isCanceled()) {
                    Log.e("API_ACTIVIDADES", "Fallo en la conexión", t);
                    mostrarActividades(new ArrayList<>());
                }
            }
        });
    }

    private void mostrarActividades(List<ActividadGetResponse> actividades) {
        activitiesContainer.removeAllViews();

        if (actividades == null || actividades.isEmpty()) {
            View noActivitiesView = currentInflater.inflate(R.layout.item_no_selection, activitiesContainer, false);
            ((TextView) noActivitiesView.findViewById(R.id.tvMessage)).setText("No hay actividades registradas");
            activitiesContainer.addView(noActivitiesView);
            return;
        }

        Context context = getContext();
        if (context == null) return;

        View tableView = currentInflater.inflate(R.layout.item_activity_table, activitiesContainer, false);
        TableLayout tableLayout = tableView.findViewById(R.id.activitiesTable);

        for (ActividadGetResponse actividad : actividades) {
            TableRow row = new TableRow(context);
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

            TextView statusCell = new TextView(context);
            statusCell.setText(estado);
            statusCell.setTextSize(12);
            statusCell.setPadding(4, 4, 4, 4);
            statusCell.setLayoutParams(new TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f));

            switch(estado.toLowerCase()) {
                case "cancelada":
                    statusCell.setTextColor(ContextCompat.getColor(context, R.color.rojopasion));
                    break;
                case "completada":
                    statusCell.setTextColor(ContextCompat.getColor(context, R.color.green));
                    break;
                case "en_progreso":
                    statusCell.setTextColor(ContextCompat.getColor(context, R.color.azulito));
                    break;
                default:
                    statusCell.setTextColor(ContextCompat.getColor(context, R.color.black));
            }

            row.addView(statusCell);
            tableLayout.addView(row);

            View divider = new View(context);
            divider.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    1));
            divider.setBackgroundColor(ContextCompat.getColor(context, R.color.azulitoPrincipio));
            tableLayout.addView(divider);
        }

        activitiesContainer.addView(tableView);
    }

    private void addDataCell(TableRow row, String text, float weight, int textSizeSp) {
        Context context = getContext();
        if (context == null) return;

        TextView textView = new TextView(context);
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
        // Inicializar vistas
        rvMedications = rootView.findViewById(R.id.rvMedications);
        ProgressBar progressBar = rootView.findViewById(R.id.progressBar);
        TextView tvNoMedications = rootView.findViewById(R.id.tvNoMedications);
        TextView tvLoading = rootView.findViewById(R.id.tvLoading);
        LinearLayout loadingContainer = rootView.findViewById(R.id.loadingContainer);

        // Configurar RecyclerView
        if (rvMedications != null) {
            rvMedications.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        // Estado inicial
        loadingContainer.setVisibility(View.VISIBLE);
        if (rvMedications != null) {
            rvMedications.setVisibility(View.GONE);
        }
        if (tvNoMedications != null) {
            tvNoMedications.setVisibility(View.GONE);
        }

        Context context = getContext();
        if (context == null) {
            loadingContainer.setVisibility(View.GONE);
            if (tvNoMedications != null) {
                tvNoMedications.setVisibility(View.VISIBLE);
                tvNoMedications.setText("Error de contexto");
            }
            return;
        }

        SharedPreferences preferences = context.getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            loadingContainer.setVisibility(View.GONE);
            if (tvNoMedications != null) {
                tvNoMedications.setVisibility(View.VISIBLE);
                tvNoMedications.setText("No autenticado");
            }
            return;
        }

        // Si no hay paciente seleccionado
        if (selectedPatientId == -1) {
            loadingContainer.setVisibility(View.GONE);
            if (rvMedications != null) {
                rvMedications.setVisibility(View.GONE);
            }
            if (tvNoMedications != null) {
                tvNoMedications.setVisibility(View.VISIBLE);
                tvNoMedications.setText("Seleccione un paciente");
            }
            return;
        }

        // Configurar Retrofit y llamada API
        Retrofit retrofit = ApiClient.getClient();
        AuthService authService = retrofit.create(AuthService.class);

        Call<List<AsignacionMedicamentoResponse>> call = authService.listarAsignacionesMedicamentos("Bearer " + token, selectedPatientId);
        activeCalls.add(call);

        call.enqueue(new Callback<List<AsignacionMedicamentoResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<AsignacionMedicamentoResponse>> call,
                                   @NonNull Response<List<AsignacionMedicamentoResponse>> response) {
                loadingContainer.setVisibility(View.GONE);

                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful()) {
                    List<AsignacionMedicamentoResponse> asignaciones = response.body();

                    if (asignaciones != null && !asignaciones.isEmpty()) {
                        // Caso: Hay medicamentos
                        if (rvMedications != null) {
                            rvMedications.setVisibility(View.VISIBLE);
                        }
                        if (tvNoMedications != null) {
                            tvNoMedications.setVisibility(View.GONE);
                        }

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

                        if (rvMedications != null) {
                            rvMedications.setAdapter(new MedicationAdapter(medications));
                        }
                    } else {
                        // Caso: No hay medicamentos
                        if (rvMedications != null) {
                            rvMedications.setVisibility(View.GONE);
                            rvMedications.setAdapter(null);
                        }
                        if (tvNoMedications != null) {
                            tvNoMedications.setVisibility(View.VISIBLE);
                            tvNoMedications.setText("No hay medicamentos registrados");
                        }
                    }
                } else {
                    // Caso: Error en la respuesta
                    if (rvMedications != null) {
                        rvMedications.setVisibility(View.GONE);
                        rvMedications.setAdapter(null);
                    }
                    if (tvNoMedications != null) {
                        tvNoMedications.setVisibility(View.VISIBLE);
                        tvNoMedications.setText("Error al cargar medicamentos");
                    }

                    try {
                        String errorBody = response.errorBody() != null ?
                                response.errorBody().string() : "Sin detalles";
                        Log.e("API_MEDICAMENTOS", "Error: " + response.code() + " - " + errorBody);
                    } catch (Exception e) {
                        Log.e("API_MEDICAMENTOS", "Error al leer errorBody", e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AsignacionMedicamentoResponse>> call,
                                  @NonNull Throwable t) {
                loadingContainer.setVisibility(View.GONE);
                if (!isAdded() || getContext() == null || call.isCanceled()) return;

                if (rvMedications != null) {
                    rvMedications.setVisibility(View.GONE);
                    rvMedications.setAdapter(null);
                }
                if (tvNoMedications != null) {
                    tvNoMedications.setVisibility(View.VISIBLE);
                    tvNoMedications.setText("Error de conexión");
                }

                Log.e("API Error", "Error al obtener medicamentos", t);
            }
        });
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

        private static String formatDate(String dateStr) {
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