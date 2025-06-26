package com.sena.qfinder.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.AuthInterceptor;
import com.sena.qfinder.ui.auth.RegistroUsuario;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.controller.MainActivityDash;
import com.sena.qfinder.data.models.LoginRequest;
import com.sena.qfinder.data.models.LoginResponse;
import com.sena.qfinder.data.models.PerfilUsuarioResponse;
import com.sena.qfinder.ui.auth.Fragment_password_recovery;
import com.sena.qfinder.utils.SharedPrefManager;

import okhttp3.OkHttpClient;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Login extends Fragment {

    private EditText emailEditText, passwordEditText;
    private TextView btnRegistro, btnOlvidarContrasena, manualLink;
    private LinearLayout tuto;
    private Button btnLogin;
    private AlertDialog progressDialog;
    private AuthService authService;
    private Retrofit retrofit;
    private SharedPrefManager sharedPrefManager;
    private MaterialShowcaseSequence sequence;

    final int totalItems = 5; // Total de pasos del tutorial

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupSharedPrefManager();
        setupRetrofit();
        setupListeners();

        // Mostrar tutorial solo la primera vez o si el usuario lo solicita
        if (shouldShowTutorial()) {
            showTutorial();
        }
    }

    private boolean shouldShowTutorial() {
        if (getContext() == null) return false;
        SharedPreferences preferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        return preferences.getBoolean("should_show_login_tutorial", true);
    }

    private void markTutorialAsShown() {
        if (getContext() == null) return;
        SharedPreferences preferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        preferences.edit().putBoolean("should_show_login_tutorial", false).apply();
    }

    private void showTutorial() {
        if (getActivity() == null || getView() == null) return;

        // Configuración común para todos los pasos del tutorial
        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(300);
        config.setMaskColor(Color.parseColor("#CC1A1A1A"));
        config.setShapePadding(16);

        sequence = new MaterialShowcaseSequence(getActivity(), "LOGIN_TUTORIAL");
        sequence.setConfig(config);

        sequence.setOnItemDismissedListener((itemView, position) -> {
            if (position + 1 == totalItems) {
                markTutorialAsShown();
            }
        });

        // Crear un diálogo de confirmación antes de mostrar el tutorial
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tutorial de inicio de sesión")
                .setMessage("¿Deseas ver el tutorial de cómo iniciar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    // Mostrar tutorial si el usuario selecciona "Sí"
                    showTutorialSteps();
                })
                .setNegativeButton("Omitir", (dialog, which) -> {
                    markTutorialAsShown();
                })
                .setCancelable(false)
                .show();
    }

    private void showTutorialSteps() {
        // 1. Explicación del campo de email
        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setTarget(tuto)
                        .setTitleText("Paso 1: Tu Correo")
                        .setDismissText("Siguiente")
                        .setContentText("Aquí debes ingresar el correo electrónico con el que te registraste.")
                        .withRectangleShape()
                        .build()
        );

        // 2. Explicación del campo de contraseña
        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setTarget(passwordEditText)
                        .setTitleText("Paso 2: Contraseña")
                        .setDismissText("Siguiente")
                        .setContentText("Escribe tu contraseña. Si la olvidaste, puedes recuperarla abajo.")
                        .withRectangleShape()
                        .build()
        );

        // 3. Explicación del botón de inicio de sesión
        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setTarget(btnLogin)
                        .setTitleText("Paso 3: Iniciar Sesión")
                        .setDismissText("Siguiente")
                        .setContentText("Presiona aquí para acceder a tu cuenta después de llenar los datos.")
                        .withRectangleShape()
                        .build()
        );

        // 4. Explicación del enlace de registro
        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setTarget(btnRegistro)
                        .setTitleText("¿No tienes cuenta?")
                        .setDismissText("Siguiente")
                        .setContentText("Toca aquí para registrarte si eres nuevo en la app.")
                        .withRectangleShape()
                        .build()
        );

        // 5. Explicación de "Olvidé mi contraseña"
        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setTarget(btnOlvidarContrasena)
                        .setTitleText("Recuperar Contraseña")
                        .setDismissText("¡Listo!")
                        .setContentText("Si no recuerdas tu contraseña, toca aquí para recuperarla.")
                        .withRectangleShape()
                        .build()
        );

        sequence.start();
    }

    private void initViews(View view) {
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        btnLogin = view.findViewById(R.id.loginButton);
        btnRegistro = view.findViewById(R.id.registerLink);
        btnOlvidarContrasena = view.findViewById(R.id.forgotPassword);
        manualLink = view.findViewById(R.id.ManualLink);
        tuto = view.findViewById(R.id.emailField);
    }

    private void setupSharedPrefManager() {
        if (getContext() == null) return;
        sharedPrefManager = SharedPrefManager.getInstance(requireContext());
    }

    private void showProgressDialog() {
        if (getContext() == null) return;
        if (progressDialog != null && progressDialog.isShowing()) {
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_progress_minimal, null);

        progressDialog = new MaterialAlertDialogBuilder(requireContext(), R.style.MinimalProgressDialog)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (progressDialog.getWindow() != null) {
            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void setupRetrofit() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(requireContext()))
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .client(client) // <-- Aquí se agrega el interceptor
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        authService = retrofit.create(AuthService.class);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> {
            if (validarCampos()) {
                iniciarSesionEnBackend();
            }
        });

        btnRegistro.setOnClickListener(v -> navegarARegistro());
        btnOlvidarContrasena.setOnClickListener(v -> forgotPassword());

        manualLink.setOnClickListener(v -> {
            String videoUrl = "https://youtu.be/mfbW0sEKE1U?si=poAT9WWoe-gCpZTF";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
            intent.setPackage("com.google.android.youtube");

            if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
                startActivity(browserIntent);
            }
        });
    }

    private boolean validarCampos() {
        boolean valido = true;
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("El correo es obligatorio");
            valido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Ingrese un correo válido");
            valido = false;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("La contraseña es obligatoria");
            valido = false;
        }

        return valido;
    }

    private void iniciarSesionEnBackend() {
        showProgressDialog();

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        LoginRequest request = new LoginRequest(email, password);

        authService.LoginUser(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getToken();
                    Log.d("LOGIN", "Token recibido: " + token);

                    if (sharedPrefManager == null) {
                        Log.e("LOGIN", "sharedPrefManager es null!");
                        setupSharedPrefManager();
                    }

                    sharedPrefManager.saveToken(token);
                    sharedPrefManager.saveEmail(email);

                    obtenerPerfilUsuario(token);
                    guardarDatosUsuario(email, response.body().getToken());

                    String tokenGuardado = sharedPrefManager.getToken();
                    Log.d("LOGIN", "Token guardado: " + tokenGuardado);

                    obtenerPerfilUsuario(token);
                } else {
                    dismissProgressDialog();
                    Toast.makeText(getContext(), "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                dismissProgressDialog();
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarDatosUsuario(String email, String token) {
        if (getContext() == null) return;
        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        preferences.edit()
                .putString("correo_usuario", email)
                .putString("token", token)
                .apply();
    }

    private void obtenerPerfilUsuario(String token) {
        authService.obtenerPerfil("Bearer " + token).enqueue(new Callback<PerfilUsuarioResponse>() {
            @Override
            public void onResponse(@NonNull Call<PerfilUsuarioResponse> call, @NonNull Response<PerfilUsuarioResponse> response) {
                dismissProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    PerfilUsuarioResponse perfil = response.body();

                    sharedPrefManager.saveUserProfile(
                            perfil.getId_usuario(),
                            perfil.getNombre_usuario(),
                            perfil.getApellido_usuario(),
                            perfil.getCorreo_usuario(),
                            perfil.getTelefono_usuario(),
                            perfil.getDireccion_usuario(),
                            perfil.getIdentificacion_usuario(),
                            perfil.getImagen_usuario()
                    );
                    guardarDatosCompletosUsuario(
                            perfil.getId_usuario(),
                            perfil.getNombre_usuario(),
                            perfil.getApellido_usuario(),
                            perfil.getCorreo_usuario(),
                            perfil.getTelefono_usuario(),
                            perfil.getDireccion_usuario(),
                            perfil.getIdentificacion_usuario(),
                            perfil.getImagen_usuario()
                    );
                    iniciarSesionExitoso();
                } else {
                    Toast.makeText(getContext(), "Error al obtener perfil de usuario", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PerfilUsuarioResponse> call, @NonNull Throwable t) {
                dismissProgressDialog();
                Toast.makeText(getContext(), "Error al obtener perfil: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarDatosCompletosUsuario(String idUsuario, String nombre, String apellido,
                                              String correo, String telefono, String direccion,
                                              String identificacion, String imagenUsuario) {
        if (getContext() == null) return;
        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        preferences.edit()
                .putString("id_usuario", idUsuario)
                .putString("nombre_usuario", nombre)
                .putString("apellido_usuario", apellido)
                .putString("correo_usuario", correo)
                .putString("telefono_usuario", telefono)
                .putString("direccion_usuario", direccion)
                .putString("identificacion_usuario", identificacion)
                .apply();
    }

    private void iniciarSesionExitoso() {
        try {
            startActivity(new Intent(requireContext(), MainActivityDash.class));
            requireActivity().finish();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error al iniciar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void navegarARegistro() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new RegistroUsuario())
                .addToBackStack(null)
                .commit();
    }

    private void forgotPassword() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new Fragment_password_recovery())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissProgressDialog();
    }
}