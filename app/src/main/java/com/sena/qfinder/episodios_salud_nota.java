package com.sena.qfinder;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sena.qfinder.api.ApiClient;
import com.sena.qfinder.api.AuthService;
import com.sena.qfinder.models.NotaEpisodioRequest;
import com.sena.qfinder.models.NotaEpisodioResponse;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class episodios_salud_nota extends AppCompatActivity {

    private String nivelGravedadActual = "baja";
    private int idPacienteSeleccionado = -1;

    private EditText editTextDescripcion, editTextIntervenciones;
    private EditText editTextFechaInicio, editTextFechaFin;
    private Button btnGuardar, btnGravedad;

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

        btnGravedad = findViewById(R.id.gravedad);
        btnGuardar = findViewById(R.id.btnGuardar);
        editTextDescripcion = findViewById(R.id.editTextDescripcion);
        editTextIntervenciones = findViewById(R.id.editTextIntervenciones);
        editTextFechaInicio = findViewById(R.id.editTextFechaInicio);
        editTextFechaFin = findViewById(R.id.editTextFechaFin);

        editTextFechaInicio.setFocusable(false);
        editTextFechaFin.setFocusable(false);

        calendarInicio.setTimeInMillis(System.currentTimeMillis());
        calendarInicio.add(Calendar.MINUTE, -2);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        editTextFechaInicio.setText(sdf.format(calendarInicio.getTime()));

        final int[] estadoGravedad = {0};
        actualizarBotonGravedad(estadoGravedad[0]);

        btnGravedad.setOnClickListener(v -> {
            estadoGravedad[0]++;
            if (estadoGravedad[0] > 2) estadoGravedad[0] = 0;
            actualizarBotonGravedad(estadoGravedad[0]);
        });

        editTextFechaInicio.setOnClickListener(v -> mostrarSelectorFechaHora(editTextFechaInicio, calendarInicio));
        editTextFechaFin.setOnClickListener(v -> mostrarSelectorFechaHora(editTextFechaFin, calendarFin));

        btnGuardar.setOnClickListener(v -> guardarNota());
    }

    private void actualizarBotonGravedad(int estado) {
        switch (estado) {
            case 0:
                nivelGravedadActual = "baja";
                btnGravedad.setBackgroundColor(ContextCompat.getColor(this, R.color.baja));
                btnGravedad.setText("Baja");
                break;
            case 1:
                nivelGravedadActual = "media";
                btnGravedad.setBackgroundColor(ContextCompat.getColor(this, R.color.media));
                btnGravedad.setText("Media");
                break;
            case 2:
                nivelGravedadActual = "alta";
                btnGravedad.setBackgroundColor(ContextCompat.getColor(this, R.color.alta));
                btnGravedad.setText("Alta");
                break;
        }
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
        String descripcion = editTextDescripcion.getText().toString().trim();
        String intervenciones = editTextIntervenciones.getText().toString().trim();
        String fechaInicio = editTextFechaInicio.getText().toString().trim();
        String fechaFin = editTextFechaFin.getText().toString().trim();

        if (descripcion.length() < 5) {
            Toast.makeText(this, "La descripción debe tener al menos 5 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fechaInicio.isEmpty()) {
            Toast.makeText(this, "Seleccione la fecha y hora de inicio", Toast.LENGTH_SHORT).show();
            return;
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
                // Si la fecha de inicio es futura, la ajustamos a hace 5 segundos para evitar error 400
                inicioSeleccionado.setTimeInMillis(ahora.getTimeInMillis() - 5000);
                fechaInicio = sdf.format(inicioSeleccionado.getTime());
            }

            if (!fechaFin.isEmpty()) {
                Calendar finSeleccionado = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                finSeleccionado.setTime(sdf.parse(fechaFin));
                finSeleccionado.set(Calendar.SECOND, 0);
                finSeleccionado.set(Calendar.MILLISECOND, 0);

                if (finSeleccionado.before(inicioSeleccionado)) {
                    Toast.makeText(this, "La fecha de fin no puede ser antes de la fecha de inicio", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al validar fechas", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        }

        if (idPacienteSeleccionado == -1) {
            Toast.makeText(this, "Error: paciente no seleccionado", Toast.LENGTH_SHORT).show();
            return;
        }

        String tokenGuardado = obtenerTokenActual();
        if (tokenGuardado == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }
        String token = "Bearer " + tokenGuardado;

        int registradoPor = obtenerIdUsuarioRegistrado();
        String registradoPorRole = obtenerRolUsuarioRegistrado();

        NotaEpisodioRequest nuevaNota = new NotaEpisodioRequest(
                idPacienteSeleccionado,
                fechaInicio,
                fechaFin.isEmpty() ? null : fechaFin,
                nivelGravedadActual,
                descripcion,
                intervenciones.isEmpty() ? null : intervenciones,
                registradoPor,
                registradoPorRole,
                "usuario",
                "app_android",
                "nota"
        );

        AuthService authService = ApiClient.getClient().create(AuthService.class);
        Call<NotaEpisodioResponse> call = authService.crearEpisodio(token, idPacienteSeleccionado, nuevaNota);

        call.enqueue(new Callback<NotaEpisodioResponse>() {
            @Override
            public void onResponse(Call<NotaEpisodioResponse> call, Response<NotaEpisodioResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(episodios_salud_nota.this, "Nota guardada correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(episodios_salud_nota.this, "Error al guardar la nota: " + errorBody, Toast.LENGTH_LONG).show();
                    Log.e("API_ERROR", "Respuesta no exitosa: " + response.code() + "\n" + errorBody);
                }
            }

            @Override
            public void onFailure(Call<NotaEpisodioResponse> call, Throwable t) {
                Toast.makeText(episodios_salud_nota.this, "Fallo de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("API_FAILURE", "Error en la llamada API", t);
            }
        });
    }

    private String obtenerTokenActual() {
        SharedPreferences preferences = getSharedPreferences("MiPreferencias", MODE_PRIVATE);
        String idUsuarioString = preferences.getString("id_usuario", "-1");
        int idUsuario = Integer.parseInt(idUsuarioString);
        return idUsuarioString;
    }

    private int obtenerIdUsuarioRegistrado() {
        SharedPreferences prefs = getSharedPreferences("usuario", MODE_PRIVATE);
        return prefs.getInt("id_usuario", -1);
    }

    private String obtenerRolUsuarioRegistrado() {
        SharedPreferences prefs = getSharedPreferences("usuario", MODE_PRIVATE);
        return prefs.getString("rol_usuario", "Usuario");
    }
}
