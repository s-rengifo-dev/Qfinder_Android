package com.sena.qfinder.ui.auth;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.UsuarioRequest;

import java.io.Serializable;
import java.util.UUID;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EditarUsuarioDialogFragment extends DialogFragment {

    private EditText nombreEditText, apellidoEditText, telefonoEditText, emailEditText, direccionEditText;
    private Button btnGuardar;
    private ImageView imagen;
    private Uri selectedImageUri;

    private UsuarioRequest usuario;
    private AuthService authService;
    private FirebaseStorage storage;

    public interface OnUsuarioActualizadoListener {
        void onUsuarioActualizado(UsuarioRequest nuevoUsuario);
    }

    private static OnUsuarioActualizadoListener tempListener;
    private OnUsuarioActualizadoListener listener;

    private ActivityResultLauncher<String> permisoLauncher;
    private ActivityResultLauncher<String> imagenPickerLauncher;

    public static EditarUsuarioDialogFragment newInstance(UsuarioRequest usuario, OnUsuarioActualizadoListener listener) {
        EditarUsuarioDialogFragment fragment = new EditarUsuarioDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("usuario", (Serializable) usuario);
        fragment.setArguments(args);
        tempListener = listener;
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = tempListener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storage = FirebaseStorage.getInstance();

        permisoLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        abrirSelectorDeImagen();
                    } else {
                        Toast.makeText(getContext(), "Permiso denegado para acceder a imágenes", Toast.LENGTH_SHORT).show();
                    }
                });

        imagenPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            selectedImageUri = uri;
                            imagen.setImageURI(selectedImageUri);
                            guardarUriTemporal(uri.toString());
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_editar_usuario, container, false);

        nombreEditText = view.findViewById(R.id.etNombre);
        apellidoEditText = view.findViewById(R.id.etApellido);
        telefonoEditText = view.findViewById(R.id.edtTelefono);
        emailEditText = view.findViewById(R.id.etCorreo);
        direccionEditText = view.findViewById(R.id.etDireccion);
        imagen = view.findViewById(R.id.imgAvatar);
        btnGuardar = view.findViewById(R.id.btnGuardar);

        if (getArguments() != null) {
            usuario = (UsuarioRequest) getArguments().getSerializable("usuario");
            if (usuario != null) {
                nombreEditText.setText(usuario.getNombre_usuario());
                apellidoEditText.setText(usuario.getApellido_usuario());
                telefonoEditText.setText(usuario.getTelefono_usuario());
                emailEditText.setText(usuario.getCorreo_usuario());
                direccionEditText.setText(usuario.getDireccion_usuario());
            }
        }

        // Mostrar imagen temporal si existe, de lo contrario cargar la del backend
        String uriStr = obtenerUriTemporal();
        if (uriStr != null) {
            selectedImageUri = Uri.parse(uriStr);
            imagen.setImageURI(selectedImageUri);
        } else if (usuario != null && usuario.getImagen_usuario() != null && !usuario.getImagen_usuario().isEmpty()) {
            Glide.with(this)
                    .load(usuario.getImagen_usuario())
                    .placeholder(R.drawable.perfil_paciente)
                    .error(R.drawable.perfil_paciente)
                    .circleCrop()
                    .into(imagen);
        }

        setupRetrofit();

        imagen.setOnClickListener(v -> verificarPermisoDeImagen());
        btnGuardar.setOnClickListener(v -> guardarCambios());

        return view;
    }

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        authService = retrofit.create(AuthService.class);
    }

    private void verificarPermisoDeImagen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permisoLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                abrirSelectorDeImagen();
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permisoLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                abrirSelectorDeImagen();
            }
        }
    }

    private void abrirSelectorDeImagen() {
        imagenPickerLauncher.launch("image/*");
    }

    private void guardarCambios() {
        btnGuardar.setEnabled(false);

        if (selectedImageUri != null) {
            subirImagenAFirebase(selectedImageUri);
        } else {
            String urlVieja = (usuario != null ? usuario.getImagen_usuario() : null);
            enviarDatosUsuario(urlVieja);
        }
    }

    private void subirImagenAFirebase(Uri imagenUri) {
        StorageReference imageRef = storage
                .getReference()
                .child("imagenes_usuarios/" + UUID.randomUUID().toString() + ".jpg");

        imageRef.putFile(imagenUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String downloadUrl = uri.toString();
                                enviarDatosUsuario(downloadUrl);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "No se pudo obtener URL de la imagen", Toast.LENGTH_SHORT).show();
                                btnGuardar.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al subir imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnGuardar.setEnabled(true);
                });
    }

    private void enviarDatosUsuario(String urlImagen) {
        String nombre = nombreEditText.getText().toString().trim();
        String apellido = apellidoEditText.getText().toString().trim();
        String telefono = telefonoEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String direccion = direccionEditText.getText().toString().trim();

        if (nombre.isEmpty() || apellido.isEmpty() || telefono.isEmpty() || email.isEmpty() || direccion.isEmpty()) {
            Toast.makeText(getContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            btnGuardar.setEnabled(true);
            return;
        }

        UsuarioRequest request = new UsuarioRequest(
                nombre,
                apellido,
                direccion,
                telefono,
                email,
                urlImagen
        );

        SharedPreferences prefs = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "Token no disponible. Inicia sesión nuevamente.", Toast.LENGTH_SHORT).show();
            btnGuardar.setEnabled(true);
            return;
        }

        Call<ResponseBody> call = authService.actualizarUsuario(request, "Bearer " + token);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                btnGuardar.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onUsuarioActualizado(request);
                    }
                    limpiarUriTemporal();
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Error " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                btnGuardar.setEnabled(true);
                Toast.makeText(getContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            );
        }
    }


    // ---------- MÉTODOS PARA GUARDAR/RECUPERAR LA URI TEMPORAL DE LA IMAGEN ----------

    private void guardarUriTemporal(String uriStr) {
        SharedPreferences prefs = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        prefs.edit().putString("uriTemporal", uriStr).apply();
    }

    private String obtenerUriTemporal() {
        SharedPreferences prefs = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        return prefs.getString("uriTemporal", null);
    }

    private void limpiarUriTemporal() {
        SharedPreferences prefs = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        prefs.edit().remove("uriTemporal").apply();
    }
}
