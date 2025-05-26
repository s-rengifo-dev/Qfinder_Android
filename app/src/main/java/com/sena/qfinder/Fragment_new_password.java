package com.sena.qfinder;

import android.app.ProgressDialog;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.sena.qfinder.api.ApiClient;
import com.sena.qfinder.api.AuthService;
import com.sena.qfinder.models.CambiarPasswordRequest;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Fragment_new_password extends Fragment {

    private EditText edtNewPassword, edtConfirmPassword;
    private Button btnChangePassword;
    private ProgressDialog progressDialog;
    private String email;
    private String resetToken;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_password, container, false);

        if (getArguments() != null) {
            email = getArguments().getString("email");
            resetToken = getArguments().getString("resetToken");
        }

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);

        edtNewPassword = view.findViewById(R.id.edtNewPassword);
        edtConfirmPassword = view.findViewById(R.id.edtConfirmPassword);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);

        btnChangePassword.setOnClickListener(v -> cambiarContrasena());

        return view;
    }

    private void cambiarContrasena() {
        // Verificar que tenemos el token
        if (resetToken == null || resetToken.isEmpty()) {
            Toast.makeText(getContext(), "Error de autenticación. Por favor inicie el proceso nuevamente.", Toast.LENGTH_LONG).show();
            volverALogin();
            return;
        }

        String newPassword = edtNewPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(getContext(), "Ambos campos son requeridos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(getContext(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Cambiando contraseña...");
        progressDialog.show();

        AuthService authService = ApiClient.getClient().create(AuthService.class);
        CambiarPasswordRequest request = new CambiarPasswordRequest(email, newPassword);
        Log.d("TOKEN_RESET", "[" + resetToken + "]");

        Call<Void> call = authService.cambiarPassword("Bearer " + resetToken, request);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Contraseña cambiada exitosamente", Toast.LENGTH_SHORT).show();
                    limpiarSesion();
                    volverALogin();
                } else {
                    manejarErrorCambioContrasena(response);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void manejarErrorCambioContrasena(Response<Void> response) {
        String errorMessage = "Error al cambiar la contraseña";
        try {
            if (response.code() == 401) {
                errorMessage = "Sesión expirada. Por favor inicie el proceso nuevamente.";
            } else if (response.errorBody() != null) {
                errorMessage += ": " + response.errorBody().string();
            }
        } catch (IOException e) {
            errorMessage = "Error al procesar la respuesta del servidor";
        }
        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
        Log.v("error",errorMessage);
    }

    private void limpiarSesion() {
        ApiClient.setResetToken(null);
        ApiClient.setUserEmail(null);
        if (ApiClient.getCookieJar() != null) {
            ApiClient.getCookieJar().clear();
        }
    }

    private void volverALogin() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new Login());
            transaction.commit();
        }
    }
}