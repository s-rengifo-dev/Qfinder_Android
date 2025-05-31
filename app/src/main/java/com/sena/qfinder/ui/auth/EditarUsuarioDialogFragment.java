package com.sena.qfinder.ui.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.gson.Gson;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.UsuarioRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EditarUsuarioDialogFragment extends DialogFragment {

    private EditText nombreEditText, apellidoEditText, telefonoEditText, emailEditText, direccionEditText;
    private Button btnGuardar;

    private UsuarioRequest usuario;
    private AuthService authService;

    public interface OnUsuarioActualizadoListener {
        void onUsuarioActualizado();
    }

    private static OnUsuarioActualizadoListener tempListener;

    public static EditarUsuarioDialogFragment newInstance(UsuarioRequest usuario, OnUsuarioActualizadoListener listener) {
        EditarUsuarioDialogFragment fragment = new EditarUsuarioDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("usuario", usuario);
        fragment.setArguments(args);
        tempListener = listener;
        return fragment;
    }

    private OnUsuarioActualizadoListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Usamos el listener temporal asignado en newInstance
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

        setupRetrofit();

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

    private void guardarCambios() {
        String nombre = nombreEditText.getText().toString().trim();
        String apellido = apellidoEditText.getText().toString().trim();
        String telefono = telefonoEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String direccion = direccionEditText.getText().toString().trim();

        if (nombre.isEmpty() || apellido.isEmpty() || telefono.isEmpty() || email.isEmpty() || direccion.isEmpty()) {
            Toast.makeText(getContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        UsuarioRequest request = new UsuarioRequest(nombre, apellido, direccion, telefono, email);

        SharedPreferences prefs = getContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "Token no disponible. Por favor inicia sesión de nuevo.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("EDITAR_USUARIO", "Request JSON: " + new Gson().toJson(request));
        Log.d("EDITAR_USUARIO", "Token: Bearer " + token);

        Call<ResponseBody> call = authService.actualizarUsuario(request, "Bearer " + token);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onUsuarioActualizado();
                    }
                    dismiss();
                } else {
                    try {
                        String errorBody = response.errorBody().string();
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
            public void onFailure(Call<ResponseBody> call, Throwable t) {
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
