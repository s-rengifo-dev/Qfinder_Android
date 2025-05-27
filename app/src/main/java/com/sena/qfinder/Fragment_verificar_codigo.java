package com.sena.qfinder;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.sena.qfinder.api.ApiClient;
import com.sena.qfinder.api.AuthService;
import com.sena.qfinder.models.SendCodeRequest;
import com.sena.qfinder.models.SendCodeResponse;
import com.sena.qfinder.models.VerificarCodigoRequest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cookie;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Fragment_verificar_codigo extends Fragment {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RESEND_DELAY_MINUTES = 1;
    private static final String TAG = "CodeVerification";

    private ImageView backButton;
    private String email;
    private int attempts = 0;
    private long lastResendTime = 0;
    private ProgressDialog progressDialog;

    public Fragment_verificar_codigo() {
        super(R.layout.fragment_verificar_codigo);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);

        if (getArguments() != null) {
            email = getArguments().getString("email");
        }

        backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> navigateBackToRecovery());

        setupAutoFocus(view);

        Button confirmButton = view.findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(v -> validateAndVerifyCode(view));

        TextView resendCode = view.findViewById(R.id.resendCodeText);
        resendCode.setOnClickListener(v -> handleResendCode());
    }

    private void setupAutoFocus(View view) {
        EditText digit1 = view.findViewById(R.id.digit1);
        EditText digit2 = view.findViewById(R.id.digit2);
        EditText digit3 = view.findViewById(R.id.digit3);
        EditText digit4 = view.findViewById(R.id.digit4);
        EditText digit5 = view.findViewById(R.id.digit5);

        setupDigitNavigation(digit1, digit2);
        setupDigitNavigation(digit2, digit3);
        setupDigitNavigation(digit3, digit4);
        setupDigitNavigation(digit4, digit5);
    }

    private void setupDigitNavigation(EditText current, EditText next) {
        current.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) next.requestFocus();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void validateAndVerifyCode(View view) {
        String code = String.format("%s%s%s%s%s",
                ((EditText) view.findViewById(R.id.digit1)).getText().toString().trim(),
                ((EditText) view.findViewById(R.id.digit2)).getText().toString().trim(),
                ((EditText) view.findViewById(R.id.digit3)).getText().toString().trim(),
                ((EditText) view.findViewById(R.id.digit4)).getText().toString().trim(),
                ((EditText) view.findViewById(R.id.digit5)).getText().toString().trim()
        );

        if (code.length() != 5) {
            Toast.makeText(requireContext(), "Por favor ingresa los 5 dígitos del código", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email == null || email.isEmpty()) {
            Toast.makeText(requireContext(), "Error: correo no disponible", Toast.LENGTH_SHORT).show();
            navigateBackToRecovery();
            return;
        }

        verifyCodeWithServer(email, code);
    }

    private void verifyCodeWithServer(String correo, String codigo) {
        showProgress("Verificando código...");

        AuthService authService = ApiClient.getClient().create(AuthService.class);
        VerificarCodigoRequest request = new VerificarCodigoRequest(correo.trim(), codigo);

        Call<Void> call = authService.verificarCodigo(request);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                dismissProgress();
                if (response.isSuccessful()) {
                    // Extraer token de las cookies
                    List<Cookie> cookies = ApiClient.getCookieJar().loadForRequest(call.request().url());
                    String token = null;

                    for (Cookie cookie : cookies) {
                        if ("token".equals(cookie.name())) {
                            token = cookie.value();
                            Log.d(TAG, "Token encontrado en cookies: " + token);
                            break;
                        }
                    }

                    // Si no está en cookies, buscar en headers
                    if (token == null && response.headers() != null) {
                        String authHeader = response.headers().get("Authorization");
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            token = authHeader.substring(7);
                            Log.d(TAG, "Token encontrado en headers: " + token);
                        }
                    }

                    if (token != null) {
                        handleSuccessfulVerification(correo, token);
                    } else {
                        Toast.makeText(getContext(), "Error: no se recibió token de autenticación", Toast.LENGTH_LONG).show();
                    }
                } else {
                    handleVerificationError(response);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                dismissProgress();
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error en la llamada", t);
            }
        });
    }

    private void handleSuccessfulVerification(String correo, String token) {
        attempts = 0;

        Bundle bundle = new Bundle();
        bundle.putString("email", correo);
        bundle.putString("resetToken", token);

        Fragment_new_password newPasswordFragment = new Fragment_new_password();
        newPasswordFragment.setArguments(bundle);

        // Limpiar back stack
        getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newPasswordFragment);
        transaction.commit();
    }

    private void handleVerificationError(Response<Void> response) {
        attempts++;

        String errorMessage = "Error desconocido";
        try {
            if (response.code() == 400) {
                errorMessage = "Código incorrecto o expirado";
            } else if (response.code() == 404) {
                errorMessage = "Usuario no encontrado";
            } else if (response.errorBody() != null) {
                errorMessage = response.errorBody().string();
            }
        } catch (IOException e) {
            errorMessage = "Error al procesar la respuesta del servidor";
        }

        if (attempts >= MAX_ATTEMPTS) {
            errorMessage = "Has excedido el máximo de intentos. Por favor solicita un nuevo código.";
            navigateBackToRecovery();
        }

        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
    }

    private void handleResendCode() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastResendTime < TimeUnit.MINUTES.toMillis(RESEND_DELAY_MINUTES)) {
            long remainingTime = RESEND_DELAY_MINUTES - TimeUnit.MILLISECONDS.toMinutes(currentTime - lastResendTime);
            Toast.makeText(getContext(), "Espera " + remainingTime + " minuto(s) antes de reenviar", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress("Enviando nuevo código...");

        AuthService authService = ApiClient.getClient().create(AuthService.class);
        Call<SendCodeResponse> call = authService.SendCode(new SendCodeRequest(email));

        call.enqueue(new Callback<SendCodeResponse>() {
            @Override
            public void onResponse(Call<SendCodeResponse> call, Response<SendCodeResponse> response) {
                dismissProgress();
                if (response.isSuccessful()) {
                    lastResendTime = System.currentTimeMillis();
                    attempts = 0;
                    Toast.makeText(getContext(), "Nuevo código enviado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Error al reenviar código", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SendCodeResponse> call, Throwable t) {
                dismissProgress();
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateBackToRecovery() {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new Fragment_password_recovery());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void showProgress(String message) {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.setMessage(message);
            progressDialog.show();
        }
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        dismissProgress();
        super.onDestroyView();
    }
}