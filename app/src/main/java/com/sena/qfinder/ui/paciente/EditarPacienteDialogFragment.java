package com.sena.qfinder.ui.paciente;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.PacienteRequest;
import com.sena.qfinder.data.models.PacienteResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EditarPacienteDialogFragment extends DialogFragment {

    private EditText nombreEditText, apellidoEditText, identificacionEditText, fechaNacimientoEditText, diagnosticoEditText;
    private ImageView imagen;
    private Uri selectedImageUri;
    private Spinner sexoSpinner;
    private Button btnGuardar;
    private int pacienteId;
    private PacienteResponse paciente;
    private AuthService authService;
    private FirebaseStorage storage;
    private OnPacienteActualizadoListener listener;

    public interface OnPacienteActualizadoListener {
        void onPacienteActualizado(PacienteResponse pacienteActualizado);
    }

    public EditarPacienteDialogFragment(PacienteResponse paciente) {
        this.paciente = paciente;
        this.pacienteId = paciente.getId();
    }

    public void setOnPacienteActualizadoListener(OnPacienteActualizadoListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_editar_paciente, container, false);

        nombreEditText = view.findViewById(R.id.etNombre);
        apellidoEditText = view.findViewById(R.id.etApellido);
        identificacionEditText = view.findViewById(R.id.edtIdentificacion);
        sexoSpinner = view.findViewById(R.id.spinnerSexo);
        fechaNacimientoEditText = view.findViewById(R.id.etFechaNacimiento);
        diagnosticoEditText = view.findViewById(R.id.etDiagnostico);
        imagen = view.findViewById(R.id.ivFotoPaciente);
        btnGuardar = view.findViewById(R.id.btnGuardar);

        storage = FirebaseStorage.getInstance();
        setupRetrofit();

        // Prellenar campos
        nombreEditText.setText(paciente.getNombre());
        apellidoEditText.setText(paciente.getApellido());
        identificacionEditText.setText(paciente.getIdentificacion());
        fechaNacimientoEditText.setText(convertirFechaParaMostrar(paciente.getFecha_nacimiento()));
        diagnosticoEditText.setText(paciente.getDiagnostico_principal());

        // Cargar imagen actual
        if (paciente.getImagen_paciente() != null && !paciente.getImagen_paciente().isEmpty()) {
            Glide.with(this)
                    .load(paciente.getImagen_paciente() + "?t=" + System.currentTimeMillis())
                    .placeholder(R.drawable.perfil_paciente)
                    .error(R.drawable.perfil_paciente)
                    .circleCrop()
                    .into(imagen);
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.sexo_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sexoSpinner.setAdapter(adapter);
        sexoSpinner.setSelection(paciente.getSexo().equalsIgnoreCase("masculino") ? 0 : 1);

        fechaNacimientoEditText.setOnClickListener(v -> mostrarDatePicker());
        imagen.setOnClickListener(v -> abrirSelectorDeImagen());
        btnGuardar.setOnClickListener(v -> guardarCambios());

        return view;
    }

    private void abrirSelectorDeImagen() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 1001);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imagen.setImageURI(selectedImageUri);  // Muestra la imagen seleccionada
        }
    }

    private void mostrarDatePicker() {
        final Calendar calendario = Calendar.getInstance();
        new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    String fechaFormateada = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    fechaNacimientoEditText.setText(fechaFormateada);
                },
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        authService = retrofit.create(AuthService.class);
    }

    private void guardarCambios() {
        btnGuardar.setEnabled(false);
        if (selectedImageUri != null) {
            subirImagenAFirebase(selectedImageUri);
        } else {
            // Usar la imagen que ya tenía el paciente si no cambió
            enviarPaciente(paciente.getImagen_paciente());
        }
    }

    private void subirImagenAFirebase(Uri imagenUri) {
        StorageReference imageRef = storage.getReference()
                .child("imagenes_pacientes/" + UUID.randomUUID().toString() + ".jpg");

        imageRef.putFile(imagenUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> enviarPaciente(uri.toString())))
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE", "Error al subir imagen", e);
                    Toast.makeText(getContext(), "Error al subir imagen", Toast.LENGTH_SHORT).show();
                    btnGuardar.setEnabled(true);
                });
    }

    private void enviarPaciente(String urlImagen) {
        String nombre = nombreEditText.getText().toString().trim();
        String apellido = apellidoEditText.getText().toString().trim();
        String identificacion = identificacionEditText.getText().toString().trim();
        String sexo = sexoSpinner.getSelectedItem().toString();
        String fechaNacimientoFormateada = convertirFechaParaEnviar(fechaNacimientoEditText.getText().toString().trim());
        String diagnostico = diagnosticoEditText.getText().toString().trim();

        if (nombre.isEmpty() || apellido.isEmpty() || identificacion.isEmpty() || fechaNacimientoFormateada == null || diagnostico.isEmpty()) {
            Toast.makeText(getContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            btnGuardar.setEnabled(true);
            return;
        }

        PacienteRequest request = new PacienteRequest(
                nombre, apellido, fechaNacimientoFormateada, sexo, diagnostico, identificacion, urlImagen
        );

        String token = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE)
                .getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "Token no disponible. Por favor inicia sesión de nuevo.", Toast.LENGTH_SHORT).show();
            btnGuardar.setEnabled(true);
            return;
        }

        Call<Void> call = authService.actualizarPaciente("Bearer " + token, pacienteId, request);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                btnGuardar.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Paciente actualizado correctamente", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        // Actualizar el objeto paciente con los nuevos datos
                        paciente.setNombre(nombre);
                        paciente.setApellido(apellido);
                        paciente.setIdentificacion(identificacion);
                        paciente.setSexo(sexo);
                        paciente.setFecha_nacimiento(fechaNacimientoFormateada);
                        paciente.setDiagnostico_principal(diagnostico);
                        paciente.setImagen_paciente(urlImagen);
                        listener.onPacienteActualizado(paciente);
                    }
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Error al actualizar paciente", Toast.LENGTH_SHORT).show();
                    try {
                        Log.e("EDITAR_PACIENTE", "Error body: " + response.errorBody().string());
                    } catch (Exception e) {
                        Log.e("EDITAR_PACIENTE", "Excepción al leer errorBody", e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                btnGuardar.setEnabled(true);
                Toast.makeText(getContext(), "Error en la conexión", Toast.LENGTH_SHORT).show();
                Log.e("EDITAR_PACIENTE", "Error en llamada API", t);
            }
        });
    }

    private String convertirFechaParaEnviar(String fechaFormateada) {
        try {
            SimpleDateFormat formatoEntrada = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date fecha = formatoEntrada.parse(fechaFormateada);
            SimpleDateFormat formatoSalida = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return formatoSalida.format(fecha);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String convertirFechaParaMostrar(String fechaIso) {
        try {
            SimpleDateFormat formatoEntrada = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date fecha = formatoEntrada.parse(fechaIso);
            SimpleDateFormat formatoSalida = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return formatoSalida.format(fecha);
        } catch (ParseException e) {
            e.printStackTrace();
            return fechaIso;
        }
    }
}
