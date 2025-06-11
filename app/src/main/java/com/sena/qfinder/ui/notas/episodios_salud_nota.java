package com.sena.qfinder.ui.notas;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sena.qfinder.R;
import com.sena.qfinder.data.api.ApiClient;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.controller.MainActivity;
import com.sena.qfinder.data.models.NotaEpisodioRequest;
import com.sena.qfinder.data.models.NotaEpisodioResponse;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class episodios_salud_nota extends AppCompatActivity {
    private int idPacienteSeleccionado = -1;

    private EditText editTextTitulo, editTextDescripcion, editTextIntervenciones;
    private EditText editTextFechaInicio, editTextFechaFin;
    private Button btnGuardar;

    private final Calendar calendarInicio = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    private final Calendar calendarFin = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_episodios_salud_nota);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        idPacienteSeleccionado = getIntent().getIntExtra("id_paciente", -1);
        if (idPacienteSeleccionado == -1) {
            Toast.makeText(this, "Error: No se ha seleccionado un paciente", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        inicializarVistas();
        configurarFechaInicioPredeterminada();
        configurarListeners();

        // Botón de retroceso personalizado
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
        });
    }

    private void inicializarVistas() {

        btnGuardar = findViewById(R.id.btnGuardar);
        editTextTitulo = findViewById(R.id.editTextTitulo);
        editTextDescripcion = findViewById(R.id.editTextDescripcion);
        editTextIntervenciones = findViewById(R.id.editTextIntervenciones);
        editTextFechaInicio = findViewById(R.id.editTextFechaInicio);
        editTextFechaFin = findViewById(R.id.editTextFechaFin);

        editTextFechaInicio.setFocusable(false);
        editTextFechaFin.setFocusable(false);
    }

    private void configurarFechaInicioPredeterminada() {
        calendarInicio.setTimeInMillis(System.currentTimeMillis());
        calendarInicio.add(Calendar.MINUTE, -2);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        editTextFechaInicio.setText(sdf.format(calendarInicio.getTime()));
    }



    private void configurarListeners() {
        editTextFechaInicio.setOnClickListener(v -> mostrarSelectorFechaHora(editTextFechaInicio, calendarInicio));
        editTextFechaFin.setOnClickListener(v -> mostrarSelectorFechaHora(editTextFechaFin, calendarFin));
        btnGuardar.setOnClickListener(v -> guardarNota());
    }


    private void mostrarSelectorFechaHora(EditText campo, Calendar calendar) {
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePicker = new TimePickerDialog(this,
                            (view1, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);

                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                campo.setText(sdf.format(calendar.getTime()));
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                    );
                    timePicker.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    private void guardarNota() {
        if (!validarCampos()) {
            return;
        }

        String token = obtenerTokenConVerificacion();
        if (token == null) {
            return;
        }

        int userId = obtenerIdUsuarioRegistrado();
        if (userId == -1) {
            Toast.makeText(this, "No se pudo identificar al usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("DEPURACION", "userId obtenido: " + userId);
        SharedPreferences prefs = getSharedPreferences("usuario", MODE_PRIVATE);
        Log.d("DEPURACION", "Contenido completo de SharedPreferences: " + prefs.getAll());

        crearYEnviarNota(token, userId);
    }

    private boolean validarCampos() {
        String titulo = editTextTitulo.getText().toString().trim();
        String descripcion = editTextDescripcion.getText().toString().trim();
        String fechaInicio = editTextFechaInicio.getText().toString().trim();
        String fechaFin = editTextFechaFin.getText().toString().trim();

        if (titulo.isEmpty()) {
            Toast.makeText(this, "Ingrese un título para el episodio", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (descripcion.length() < 5) {
            Toast.makeText(this, "La descripción debe tener al menos 5 caracteres", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (fechaInicio.isEmpty()) {
            Toast.makeText(this, "Seleccione la fecha y hora de inicio", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            Calendar ahora = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            Calendar inicioSeleccionado = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            inicioSeleccionado.setTime(sdf.parse(fechaInicio));
            inicioSeleccionado.set(Calendar.SECOND, 0);
            inicioSeleccionado.set(Calendar.MILLISECOND, 0);

            if (inicioSeleccionado.after(ahora)) {
                inicioSeleccionado.setTimeInMillis(ahora.getTimeInMillis() - 5000);
                editTextFechaInicio.setText(sdf.format(inicioSeleccionado.getTime()));
            }

            if (!fechaFin.isEmpty()) {
                Calendar finSeleccionado = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                finSeleccionado.setTime(sdf.parse(fechaFin));
                finSeleccionado.set(Calendar.SECOND, 0);
                finSeleccionado.set(Calendar.MILLISECOND, 0);

                if (finSeleccionado.before(inicioSeleccionado)) {
                    Toast.makeText(this, "La fecha de fin no puede ser antes de la fecha de inicio", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al validar fechas", Toast.LENGTH_SHORT).show();
            Log.e("VALIDACION_FECHAS", "Error al validar fechas", e);
            return false;
        }

        return true;
    }

    private void crearYEnviarNota(String token, int userId) {
        String titulo = editTextTitulo.getText().toString().trim();
        String descripcion = editTextDescripcion.getText().toString().trim();
        String intervenciones = editTextIntervenciones.getText().toString().trim();
        String fechaInicio = editTextFechaInicio.getText().toString().trim();
        String fechaFin = editTextFechaFin.getText().toString().trim();
        String rolUsuario = obtenerRolUsuarioRegistrado();

        NotaEpisodioRequest nuevaNota = new NotaEpisodioRequest(
                idPacienteSeleccionado,
                fechaInicio,
                fechaFin.isEmpty() ? null : fechaFin,
                titulo,
                descripcion,
                intervenciones.isEmpty() ? null : intervenciones,
                userId,
                rolUsuario,
                "usuario",
                "app_android"
        );

        AuthService authService = ApiClient.getClient().create(AuthService.class);
        Call<NotaEpisodioResponse> call = authService.crearEpisodio(token, idPacienteSeleccionado, nuevaNota);

        call.enqueue(new Callback<NotaEpisodioResponse>() {
            @Override
            public void onResponse(Call<NotaEpisodioResponse> call, Response<NotaEpisodioResponse> response) {
                if (response.code() == 401) {
                    manejarTokenInvalido();
                    return;
                }

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(episodios_salud_nota.this, "Episodio guardado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    manejarErrorRespuesta(response);
                }
            }

            @Override
            public void onFailure(Call<NotaEpisodioResponse> call, Throwable t) {
                Toast.makeText(episodios_salud_nota.this, "Fallo de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("API_FAILURE", "Error en la llamada API", t);
            }
        });
    }

    private void manejarErrorRespuesta(Response<NotaEpisodioResponse> response) {
        String errorBody = "";
        try {
            if (response.errorBody() != null) {
                errorBody = response.errorBody().string();
            }
        } catch (Exception e) {
            Log.e("API_ERROR", "Error al leer errorBody", e);
        }

        String mensajeError = "Error al guardar el episodio";
        if (!errorBody.isEmpty()) {
            mensajeError += ": " + errorBody;
        }

        Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show();
        Log.e("API_ERROR", "Respuesta no exitosa: " + response.code() + "\n" + errorBody);
    }

    private String obtenerTokenConVerificacion() {
        SharedPreferences prefs = getSharedPreferences("usuario", MODE_PRIVATE);
        String token = prefs.getString("token", null);

        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Sesión expirada. Por favor inicie sesión nuevamente.", Toast.LENGTH_SHORT).show();
            redirigirALogin();
            return null;
        }

        return "Bearer " + token;
    }

    private int obtenerIdUsuarioRegistrado() {
        SharedPreferences prefs = getSharedPreferences("usuario", MODE_PRIVATE);

        try {
            int idInt = prefs.getInt("id_usuario", -1);
            if (idInt != -1) {
                return idInt;
            }
        } catch (ClassCastException e) {
            Log.e("USER_ID", "id_usuario no es un int", e);
        }

        try {
            String idString = prefs.getString("id_usuario", null);
            if (idString != null && !idString.isEmpty()) {
                return Integer.parseInt(idString);
            }
        } catch (Exception e) {
            Log.e("USER_ID", "Error al leer id_usuario como String", e);
        }

        // 👉 SOLUCIÓN TEMPORAL
        Log.w("USER_ID", "ID de usuario no encontrado. Usando valor temporal.");
        return 1; // Reemplaza "1" con un ID válido para pruebas
    }



    private String obtenerRolUsuarioRegistrado() {
        SharedPreferences prefs = getSharedPreferences("usuario", MODE_PRIVATE);
        return prefs.getString("rol_usuario", "Usuario");
    }

    private void manejarTokenInvalido() {
        Toast.makeText(this, "Sesión expirada. Por favor inicie sesión nuevamente.", Toast.LENGTH_SHORT).show();
        limpiarSesionYRedirigir();
    }

    private void limpiarSesionYRedirigir() {
        SharedPreferences.Editor editor = getSharedPreferences("usuario", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        redirigirALogin();
    }

    private void redirigirALogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}