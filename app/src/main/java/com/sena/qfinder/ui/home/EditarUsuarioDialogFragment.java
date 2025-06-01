package com.sena.qfinder.ui.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.sena.qfinder.R;
import com.sena.qfinder.api.AuthService;
import com.sena.qfinder.models.UsuarioRequest;

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

        storage = FirebaseStorage.getInstance();

        if (getArguments() != null) {
            usuario = (UsuarioRequest) getArguments().getSerializable("usuario");
            if (usuario != null) {
                nombreEditText.setText(usuario.getNombre_usuario());
                apellidoEditText.setText(usuario.getApellido_usuario());
                telefonoEditText.setText(usuario.getTelefono_usuario());
                emailEditText.setText(usuario.getCorreo_usuario());
                direccionEditText.setText(usuario.getDireccion_usuario());

                if (usuario.getImagen_usuario() != null && !usuario.getImagen_usuario().isEmpty()) {
                    Glide.with(this)
                            .load(usuario.getImagen_usuario())
                            .placeholder(R.drawable.perfil_paciente)
                            .error(R.drawable.perfil_paciente)
                            .circleCrop()
                            .into(imagen);
                }
            }
        }

        setupRetrofit();

        btnGuardar.setOnClickListener(v -> guardarCambios());
        imagen.setOnClickListener(v -> abrirSelectorDeImagen());

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
            imagen.setImageURI(selectedImageUri);
        }
    }

    private void subirImagenAFirebase(Uri imagenUri) {
        StorageReference imageRef = storage.getReference()
                .child("imagenes_usuarios/" + UUID.randomUUID().toString() + ".jpg");

        imageRef.putFile(imagenUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> enviarDatosUsuario(uri.toString())))
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE", "Error al subir imagen", e);
                    Toast.makeText(getContext(), "Error al subir imagen", Toast.LENGTH_SHORT).show();
                    btnGuardar.setEnabled(true);
                });
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
            enviarDatosUsuario(usuario != null ? usuario.getImagen_usuario() : null);
        }
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

        // Aquí el cambio principal: agregamos el booleano manejarImagenes
        UsuarioRequest request = new UsuarioRequest(nombre, apellido, direccion, telefono, email, urlImagen, true);

        SharedPreferences prefs = getContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "Token no disponible. Por favor inicia sesión de nuevo.", Toast.LENGTH_SHORT).show();
            btnGuardar.setEnabled(true);
            return;
        }

        Log.d("EDITAR_USUARIO", "Request JSON: " + new Gson().toJson(request));
        Log.d("EDITAR_USUARIO", "Token: Bearer " + token);

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
                    dismiss();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Respuesta vacía";
                        Log.e("EDITAR_USUARIO", "Error al actualizar: " + response.code());
                        Log.e("EDITAR_USUARIO", "Cuerpo del error: " + errorBody);
                        Toast.makeText(getContext(), "Error " + response.code() + ": " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error desconocido al procesar la respuesta", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                btnGuardar.setEnabled(true);
                Toast.makeText(getContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("EDITAR_USUARIO", "Fallo de red", t);
            }
        });
    }


    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
