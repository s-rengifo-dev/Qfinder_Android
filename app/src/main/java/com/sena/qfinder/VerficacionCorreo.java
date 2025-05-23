package com.sena.qfinder;

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
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.gson.annotations.SerializedName;
import com.sena.qfinder.api.AuthService;
import com.sena.qfinder.models.CodeVerificationRequest;
import com.sena.qfinder.models.CodeVerificationResponse;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class VerficacionCorreo extends Fragment {

    private static final String ARG_EMAIL = "correo_usuario";
    private String correoUsuario;
    private ImageView backButton;



    public VerficacionCorreo() {
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

        // Botón de imagen para regresar a RegistroUsuario
        ImageView backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new RegistroUsuario());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        // Configura el auto-foco para cada campo de dígito
        for (int i = 0; i < digits.length - 1; i++) {
            setupAutoFocus(digits[i], digits[i + 1]);
        }

        confirmButton.setOnClickListener(v -> {
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
        });

        return view;
    }


    private void setupAutoFocus(EditText current, EditText next) {
        current.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    next.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void verificarCodigoConBackend(String correo, String codigo) {
        // Validaciones básicas
        if (correo == null || correo.isEmpty()) {
            Toast.makeText(getContext(), "Correo electrónico no válido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (codigo == null || codigo.length() != 5) {
            Toast.makeText(getContext(), "El código debe tener exactamente 5 dígitos", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("VerificacionCodigo", "Verificando código: " + codigo + " para correo: " + correo);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        AuthService service = retrofit.create(AuthService.class);
        CodeVerificationRequest request = new CodeVerificationRequest(correo, codigo);

        service.verificarCodigo(request).enqueue(new Callback<CodeVerificationResponse>() {
            @Override
            public void onResponse(Call<CodeVerificationResponse> call, Response<CodeVerificationResponse> response) {
                if (!response.isSuccessful()) {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error desconocido";
                        Log.e("API_ERROR", "Código de error: " + response.code() + ", Mensaje: " + errorBody);
                        Toast.makeText(getContext(), "Error en la verificación: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Log.e("API_ERROR", "Error al leer el cuerpo del error", e);
                        Toast.makeText(getContext(), "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                CodeVerificationResponse verificationResponse = response.body();
                if (verificationResponse != null) {
                    if (verificationResponse.isSuccess()) {
                        // Verificación exitosa
                        Toast.makeText(getContext(), verificationResponse.getMessage(), Toast.LENGTH_SHORT).show();

                        // Cambiar de fragmento
                        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                        transaction.replace(R.id.fragment_container, new Login());
                        transaction.addToBackStack(null);
                        transaction.commit();

                    } else {
                        // Código incorrecto u otro error
                        Toast.makeText(getContext(), verificationResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<CodeVerificationResponse> call, Throwable t) {
                Log.e("NETWORK_ERROR", "Error de red: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}