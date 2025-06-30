package com.sena.qfinder.ui.paciente;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.RegisterPacienteRequest;
import com.sena.qfinder.data.models.RegisterPacienteResponse;
import com.sena.qfinder.ui.home.DashboardFragment;
import com.sena.qfinder.ui.home.SubscriptionActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegistrarPaciente extends Fragment {

    private EditText editNombre, editApellido, editFechaNacimiento, editDiagnostico, editIdentificacion;
    private AutoCompleteTextView editSexo;
    private Button btnRegistrar;
    private ImageView btnBack;
    private TextView tvPlanInfo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registrar_paciente, container, false);

        // Inicializar vistas
        editNombre = view.findViewById(R.id.edtNombre);
        editApellido = view.findViewById(R.id.edtApellido);
        editFechaNacimiento = view.findViewById(R.id.etFechaNacimiento);
        editSexo = view.findViewById(R.id.etSexo);
        editDiagnostico = view.findViewById(R.id.etDiagnostico);
        editIdentificacion = view.findViewById(R.id.etIdentificacion);
        btnRegistrar = view.findViewById(R.id.btnRegistrar);
        btnBack = view.findViewById(R.id.btnBack);


        // Configurar selección de género
        configurarSelectorGenero();

        // Configurar selector de fecha
        configurarSelectorFecha();

        // Botón para registrar paciente
        btnRegistrar.setOnClickListener(v -> registrarPaciente());

        // Botón para volver al dashboard
        btnBack.setOnClickListener(v -> volverAlDashboard());

        return view;
    }



    private void configurarSelectorGenero() {
        String[] generos = {"masculino", "femenino", "otro"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, generos) {
            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        results.values = generos;
                        results.count = generos.length;
                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        if (results.count > 0) {
                            notifyDataSetChanged();
                        } else {
                            notifyDataSetInvalidated();
                        }
                    }
                };
            }
        };

        editSexo.setAdapter(adapter);
        editSexo.setThreshold(1);

        editSexo.setOnItemClickListener((parent, view1, position, id) -> {
            String seleccion = (String) parent.getItemAtPosition(position);
            editSexo.setText(seleccion, false);

            if ("otro".equals(seleccion)) {
                editSexo.setInputType(InputType.TYPE_CLASS_TEXT);
                editSexo.setText("");
                editSexo.setHint("Escribe tu género");
            }
        });

        editSexo.setOnClickListener(v -> editSexo.showDropDown());
        editSexo.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) editSexo.showDropDown();
        });
    }

    private void configurarSelectorFecha() {
        editFechaNacimiento.setOnClickListener(v -> showDatePickerDialog());
        editFechaNacimiento.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePickerDialog();
        });
        editFechaNacimiento.setKeyListener(null);
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    editFechaNacimiento.setText(sdf.format(selectedDate.getTime()));
                    editFechaNacimiento.clearFocus();
                }, year, month, day);

        calendar.add(Calendar.DAY_OF_MONTH, -1);
        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    private void volverAlDashboard() {
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, new DashboardFragment());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void registrarPaciente() {
        String nombre = editNombre.getText().toString().trim();
        String apellido = editApellido.getText().toString().trim();
        String fechaNacimiento = editFechaNacimiento.getText().toString().trim();
        String sexo = editSexo.getText().toString().trim();
        String diagnostico = editDiagnostico.getText().toString().trim();
        String identificacionStr = editIdentificacion.getText().toString().trim();

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(apellido) || TextUtils.isEmpty(fechaNacimiento)
                || TextUtils.isEmpty(sexo) || TextUtils.isEmpty(diagnostico) || TextUtils.isEmpty(identificacionStr)) {
            Toast.makeText(getContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        RegisterPacienteRequest request = new RegisterPacienteRequest(
                nombre, apellido, fechaNacimiento, sexo, diagnostico, identificacionStr
        );

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);

        if (token == null || token.isEmpty()) {
            Toast.makeText(getContext(), "Token no encontrado. Inicia sesión nuevamente.", Toast.LENGTH_SHORT).show();
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        AuthService authService = retrofit.create(AuthService.class);

        authService.registerPaciente("Bearer " + token, request).enqueue(new Callback<RegisterPacienteResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterPacienteResponse> call, @NonNull Response<RegisterPacienteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Actualizar contador de pacientes en SharedPreferences
                    SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("usuario", Context.MODE_PRIVATE);
                    int pacientesRegistrados = sharedPreferences.getInt("pacientes_registrados", 0);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("pacientes_registrados", pacientesRegistrados + 1);
                    editor.apply();

                    Toast.makeText(getContext(), "Paciente registrado exitosamente", Toast.LENGTH_SHORT).show();
                    volverAlDashboard();
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();

                            // Intentar parsear el error como JSON
                            try {
                                ErrorResponse errorResponse = new Gson().fromJson(errorBody, ErrorResponse.class);
                                if (errorResponse != null && errorResponse.getError() != null &&
                                        errorResponse.getError().contains("límite")) {
                                    mostrarMensajeLimitePlan(
                                            errorResponse.getPlan_actual(),
                                            errorResponse.getPacientes_registrados(),
                                            errorResponse.getLimite()
                                    );
                                    return;
                                }
                            } catch (Exception e) {
                                // Si no se puede parsear como JSON, manejar como texto plano
                                if (errorBody.contains("límite de 2 pacientes")) {
                                    mostrarMensajeLimitePlan("free", 2, 2);
                                } else if (errorBody.contains("límite de 5 pacientes")) {
                                    mostrarMensajeLimitePlan("plus", 5, 5);
                                } else if (errorBody.contains("límite de 15 pacientes")) {
                                    mostrarMensajeLimitePlan("pro", 15, 15);
                                } else {
                                    Toast.makeText(getContext(), errorBody, Toast.LENGTH_LONG).show();
                                }
                            }
                        } else {
                            Toast.makeText(getContext(), "Error desconocido al registrar paciente", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error al procesar la respuesta", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterPacienteResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarMensajeLimitePlan(String planActual, int pacientesRegistrados, int limite) {
        View view = getView();
        if (view != null) {
            Snackbar snackbar = Snackbar.make(view, "", Snackbar.LENGTH_INDEFINITE);
            Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
            layout.setPadding(0, 0, 0, 0);
            layout.setBackgroundColor(Color.TRANSPARENT);

            View customView = LayoutInflater.from(requireContext()).inflate(R.layout.snackbar_limit_exceeded, null);

            TextView message = customView.findViewById(R.id.snackbar_message);
            Button actionButton = customView.findViewById(R.id.snackbar_action);
            ImageView icon = customView.findViewById(R.id.snackbar_icon);

            String mensaje = String.format("Plan %s: %d/%d pacientes registrados. ¡Has alcanzado el límite!",
                    planActual.toUpperCase(), pacientesRegistrados, limite);
            message.setText(mensaje);
            actionButton.setText("Actualizar Plan");
            icon.setImageResource(R.drawable.premium);

            actionButton.setOnClickListener(v -> {
                snackbar.dismiss();
                Intent intent = new Intent(requireActivity(), SubscriptionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });

            layout.addView(customView, 0);
            snackbar.show();
        }
    }

    // Clase para manejar la respuesta de error del servidor
    private static class ErrorResponse {
        private String error;
        private String plan_actual;
        private int pacientes_registrados;
        private int limite;

        // Getters y Setters
        public String getError() { return error; }
        public String getPlan_actual() { return plan_actual; }
        public int getPacientes_registrados() { return pacientes_registrados; }
        public int getLimite() { return limite; }
    }
}