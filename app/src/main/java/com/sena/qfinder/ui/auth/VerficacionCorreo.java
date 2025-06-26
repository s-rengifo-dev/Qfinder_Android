package com.sena.qfinder.ui.auth;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.sena.qfinder.R;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.CodeVerificationRequest;
import com.sena.qfinder.data.models.CodeVerificationResponse;
import com.sena.qfinder.data.models.ResendCodeRequest;
import com.sena.qfinder.data.models.ResendCodeResponse;
import com.sena.qfinder.ui.home.Login;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class VerficacionCorreo extends Fragment {

    private static final String ARG_EMAIL = "correo_usuario";
    private static final int MAX_ATTEMPTS = 3;
    private static final long RESEND_DELAY_MINUTES = 1;
    private static final String TAG = "EmailVerification";

    private String correoUsuario;
    private ImageView backButton;
    private TextView resendCodeButton;
    private ProgressDialog progressDialog;
    private int attempts = 0;
    private long lastResendTime = 0;

    public VerficacionCorreo() {
        // Constructor público vacío requerido
    }

    public static VerficacionCorreo newInstance(String correo) {
        VerficacionCorreo fragment = new VerficacionCorreo();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, correo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            correoUsuario = getArguments().getString(ARG_EMAIL);
        }

        // Inicializar ProgressDialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.verificacion_correo, container, false);

        EditText[] digits = {
                view.findViewById(R.id.digit1),
                view.findViewById(R.id.digit2),
                view.findViewById(R.id.digit3),
                view.findViewById(R.id.digit4),
                view.findViewById(R.id.digit5)
        };

        Button confirmButton = view.findViewById(R.id.confirmButton);
        resendCodeButton = view.findViewById(R.id.resendCodeText);

        backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> volverAtras());

        configurarAutoFoco(digits);

        confirmButton.setOnClickListener(v -> validarYVerificarCodigo(digits));

        configurarBotonReenvio();

        return view;
    }

    private void volverAtras() {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new RegistroUsuario());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void configurarAutoFoco(EditText[] digits) {
        for (int i = 0; i < digits.length - 1; i++) {
            final int nextIndex = i + 1;
            digits[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1) {
                        digits[nextIndex].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void validarYVerificarCodigo(EditText[] digits) {
        StringBuilder codeBuilder = new StringBuilder();
        for (EditText digit : digits) {
            String digitText = digit.getText().toString().trim();
            if (digitText.isEmpty()) {
                Toast.makeText(getContext(), "Por favor completa todos los dígitos", Toast.LENGTH_SHORT).show();
                return;
            }
            codeBuilder.append(digitText);
        }

        String codeEntered = codeBuilder.toString();
        verificarCodigoConBackend(correoUsuario, codeEntered);
    }

    private void configurarBotonReenvio() {
        resendCodeButton.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastResend = currentTime - lastResendTime;

            if (timeSinceLastResend < TimeUnit.MINUTES.toMillis(RESEND_DELAY_MINUTES)) {
                long remainingTime = RESEND_DELAY_MINUTES - TimeUnit.MILLISECONDS.toMinutes(timeSinceLastResend);
                String message = "Espera " + remainingTime + " minuto(s) antes de reenviar";
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                return;
            }

            reenviarCodigoVerificacion(correoUsuario);
        });
    }

    private void verificarCodigoConBackend(String correo, String codigo) {
        if (correo == null || correo.isEmpty()) {
            Toast.makeText(getContext(), "Correo electrónico no válido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (codigo == null || codigo.length() != 5) {
            Toast.makeText(getContext(), "El código debe tener exactamente 5 dígitos", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Verificando código: " + codigo + " para correo: " + correo);
        showProgress("Verificando código...");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        AuthService service = retrofit.create(AuthService.class);
        CodeVerificationRequest request = new CodeVerificationRequest(correo, codigo);

        service.verificarCodigo(request).enqueue(new Callback<CodeVerificationResponse>() {
            @Override
            public void onResponse(Call<CodeVerificationResponse> call, Response<CodeVerificationResponse> response) {
                dismissProgress();

                if (!response.isSuccessful()) {
                    attempts++;
                    manejarErrorVerificacion(response);
                    return;
                }

                CodeVerificationResponse verificationResponse = response.body();
                if (verificationResponse != null) {
                    if (verificationResponse.isSuccess()) {
                        attempts = 0; // Resetear intentos si es exitoso
                        manejarVerificacionExitosa(verificationResponse);
                    } else {
                        attempts++;
                        Toast.makeText(getContext(), verificationResponse.getMessage(), Toast.LENGTH_LONG).show();

                        if (attempts >= MAX_ATTEMPTS) {
                            Toast.makeText(getContext(),
                                    "Has excedido el máximo de intentos. Por favor solicita un nuevo código.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<CodeVerificationResponse> call, Throwable t) {
                dismissProgress();
                Log.e(TAG, "Error de red: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void manejarErrorVerificacion(Response<CodeVerificationResponse> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error desconocido";
            Log.e(TAG, "Código de error: " + response.code() + ", Mensaje: " + errorBody);

            String errorMessage = "Error en la verificación";
            if (response.code() == 400) {
                errorMessage = "Código incorrecto o expirado";
            } else if (response.code() == 404) {
                errorMessage = "Usuario no encontrado";
            }

            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "Error al leer el cuerpo del error", e);
            Toast.makeText(getContext(), "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
        }
    }

    private void manejarVerificacionExitosa(CodeVerificationResponse verificationResponse) {
        Toast.makeText(getContext(), verificationResponse.getMessage(), Toast.LENGTH_SHORT).show();

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new Login());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void reenviarCodigoVerificacion(String correo) {
        if (correo == null || correo.isEmpty()) {
            Toast.makeText(getContext(), "Correo electrónico no válido", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Solicitando reenvío de código para: " + correo);
        showProgress("Enviando nuevo código...");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        AuthService service = retrofit.create(AuthService.class);
        ResendCodeRequest request = new ResendCodeRequest(correo);

        service.reenviarCodigo(request).enqueue(new Callback<ResendCodeResponse>() {
            @Override
            public void onResponse(Call<ResendCodeResponse> call, Response<ResendCodeResponse> response) {
                dismissProgress();

                if (!response.isSuccessful()) {
                    manejarErrorReenvio(response);
                    return;
                }

                ResendCodeResponse resendResponse = response.body();
                if (resendResponse != null) {
                    lastResendTime = System.currentTimeMillis();
                    attempts = 0; // Resetear intentos al reenviar
                    Toast.makeText(getContext(), "Nuevo código enviado", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResendCodeResponse> call, Throwable t) {
                dismissProgress();
                Log.e(TAG, "Error de red al reenviar código: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Error de conexión al reenviar código: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void manejarErrorReenvio(Response<ResendCodeResponse> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error desconocido";
            Log.e(TAG, "Código de error al reenviar: " + response.code() + ", Mensaje: " + errorBody);
            Toast.makeText(getContext(), "Error al reenviar código: " + errorBody, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "Error al leer el cuerpo del error al reenviar", e);
            Toast.makeText(getContext(), "Error al procesar la respuesta del reenvío", Toast.LENGTH_SHORT).show();
        }
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