package com.sena.qfinder.ui.paciente;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.sena.qfinder.R;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.RegisterPacienteRequest;
import com.sena.qfinder.data.models.RegisterPacienteResponse;
import com.sena.qfinder.ui.home.DashboardFragment;

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

        // Configurar selección de género (CON FIX PARA QUE SIEMPRE MUESTRE LAS 3 OPCIONES)
        String[] generos = {"masculino", "femenino", "otro"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, generos) {
            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        // Devuelve todas las opciones SIN FILTRAR
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
        editSexo.setThreshold(1); // Mostrar opciones al primer caracter (o al hacer clic)

// Al seleccionar un item (sin cerrar el dropdown)
        editSexo.setOnItemClickListener((parent, view1, position, id) -> {
            String seleccion = (String) parent.getItemAtPosition(position);
            editSexo.setText(seleccion, false); // "false" para no filtrar

            if ("otro".equals(seleccion)) {
                editSexo.setInputType(InputType.TYPE_CLASS_TEXT);
                editSexo.setText("");
                editSexo.setHint("Escribe tu género");
            }
        });

// Mostrar dropdown al hacer clic/focus
        editSexo.setOnClickListener(v -> editSexo.showDropDown());
        editSexo.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) editSexo.showDropDown();
        });
        // Configurar selector de fecha
        editFechaNacimiento.setOnClickListener(v -> showDatePickerDialog());
        editFechaNacimiento.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePickerDialog();
        });
        editFechaNacimiento.setKeyListener(null); // evitar entrada manual

        // Botón para registrar paciente
        btnRegistrar.setOnClickListener(v -> registrarPaciente());

        // Botón para volver al dashboard
        btnBack.setOnClickListener(v -> {
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new DashboardFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
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
                    // Cambiar formato a ISO 8601
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    editFechaNacimiento.setText(sdf.format(selectedDate.getTime()));
                    editFechaNacimiento.clearFocus();
                }, year, month, day);

        // 🔒 Impedir seleccionar el mismo día o fechas futuras
        calendar.add(Calendar.DAY_OF_MONTH, -1); // Resta un día para excluir hoy
        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());

        datePickerDialog.show();
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

        // CORRECCIÓN: agregar espacio después de "Bearer "
        authService.registerPaciente("Bearer " + token, request).enqueue(new Callback<RegisterPacienteResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterPacienteResponse> call, @NonNull Response<RegisterPacienteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), "Paciente registrado exitosamente", Toast.LENGTH_SHORT).show();
                    // Volver al dashboard
                    FragmentManager fragmentManager = getParentFragmentManager();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.replace(R.id.fragment_container, new DashboardFragment());
                    transaction.commit();
                } else {
                    String errorMensaje = "Error al registrar paciente";
                    try {
                        if (response.errorBody() != null) {
                            errorMensaje = response.errorBody().string();
                        }
                    } catch (Exception ignored) {
                    }
                    Toast.makeText(getContext(), errorMensaje, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterPacienteResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
