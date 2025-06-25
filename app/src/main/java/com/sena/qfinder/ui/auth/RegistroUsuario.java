package com.sena.qfinder.ui.auth;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.textfield.TextInputEditText;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.RegisterRequest;
import com.sena.qfinder.data.models.RegisterResponse;
import com.sena.qfinder.ui.home.Login;

import java.io.IOException;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegistroUsuario extends Fragment {

    private TextInputEditText edtNombre, edtApellido, edtCorreo, edtIdentificacion, edtDirrecion, edtTelefono, edtContrasena;
    private Button btnContinuar;
    private ImageView btnBack;
    private ProgressDialog progressDialog;
    private TextView tvCondiciones;
    private CheckBox chkAceptarTerminos;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registro_usuario, container, false);

        // Inicializar vistas
        initViews(view);

        // Configurar listeners
        setupListeners();

        // Configurar texto clickeable de condiciones
        setupCondicionesTexto();

        return view;
    }

    private void initViews(View view) {
        edtNombre = view.findViewById(R.id.edtNombre);
        edtApellido = view.findViewById(R.id.edtApellido);
        edtCorreo = view.findViewById(R.id.edtCorreo);
        edtIdentificacion = view.findViewById(R.id.edtIdentificacion);
        edtDirrecion = view.findViewById(R.id.edtDireccion);
        edtTelefono = view.findViewById(R.id.edtTelefono);
        edtContrasena = view.findViewById(R.id.edtContrasena);
        btnContinuar = view.findViewById(R.id.btnContinuar);
        btnBack = view.findViewById(R.id.btnBack);
        tvCondiciones = view.findViewById(R.id.tvTerminos);
        chkAceptarTerminos = view.findViewById(R.id.chkAceptarTerminos);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            chkAceptarTerminos.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#18A0FB")));
        }

        // Limitar a 10 dígitos y tipo de entrada telefónica
        edtTelefono.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(10)});
        edtTelefono.setInputType(android.text.InputType.TYPE_CLASS_PHONE);

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Registrando");
        progressDialog.setMessage("Por favor espere...");
        progressDialog.setCancelable(false);
    }


    private void setupListeners() {
        btnBack.setOnClickListener(v -> volverALogin());

        btnContinuar.setOnClickListener(v -> {
            if (validarCampos()) {
                registrarUsuarioEnBackend();
            }
        });
    }

    private void setupCondicionesTexto() {
        String texto = "Al continuar aceptas nuestras Políticas de Privacidad y manejo de datos";
        SpannableString spannableString = new SpannableString(texto);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // Abrir el fragmento de condiciones
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, new TerminosCondiciones());
                transaction.addToBackStack(null);
                transaction.commit();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(0xFF18A0FB);// Azul personalizado
                ds.setUnderlineText(false); // Sin subrayado
            }
        };

        int start = texto.indexOf("Políticas de Privacidad y manejo de datos");
        int end = start + "Políticas de Privacidad y manejo de datos".length();
        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvCondiciones.setText(spannableString);
        tvCondiciones.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private boolean validarContrasena(String contrasena) {
        StringBuilder mensajeError = new StringBuilder("La contraseña debe tener:");
        boolean tieneLongitud = contrasena.length() >= 8;
        boolean tieneMayuscula = contrasena.matches(".*[A-Z].*");
        boolean tieneEspecial = contrasena.matches(".*[!@#$%^&*(),.?\":{}|<>].*");

        boolean esValida = tieneLongitud && tieneMayuscula && tieneEspecial;

        if (!tieneLongitud) mensajeError.append("\n- Al menos 8 caracteres");
        if (!tieneMayuscula) mensajeError.append("\n- Al menos una letra mayúscula");
        if (!tieneEspecial) mensajeError.append("\n- Al menos un carácter especial");

        if (!esValida) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View dialogView = inflater.inflate(R.layout.dialog_custom_password, null);

            TextView tvMensaje = dialogView.findViewById(R.id.tvMensaje);
            tvMensaje.setText(mensajeError.toString());

            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setView(dialogView)
                    .setCancelable(false)
                    .create();

            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Entendido", (dialogInterface, i) -> dialog.dismiss());

            dialog.setOnShowListener(dialogInterface -> {
                Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setTextColor(Color.parseColor("#FFFFFF")); // Texto azul
                button.setBackgroundColor(Color.parseColor("#18A0FB")); // Fondo gris claro
            });

            dialog.show();

            // Fondo completo del diálogo
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#F9F9F9")));

            return false;
        }

        return true;
    }





    private boolean validarCampos() {
        boolean valido = true;

        if (edtNombre.getText().toString().trim().isEmpty()) {
            edtNombre.setError("El nombre es obligatorio");
            valido = false;
        }

        if (edtApellido.getText().toString().trim().isEmpty()) {
            edtApellido.setError("El apellido es obligatorio");
            valido = false;
        }

        if (edtCorreo.getText().toString().trim().isEmpty()) {
            edtCorreo.setError("El correo es obligatorio");
            valido = false;
        } else if (!esCorreoValido(edtCorreo.getText().toString().trim())) {
            edtCorreo.setError("Ingrese un correo válido");
            valido = false;
        }

        if (edtIdentificacion.getText().toString().trim().isEmpty()) {
            edtIdentificacion.setError("La identificación es obligatoria");
            valido = false;
        }

        if (edtDirrecion.getText().toString().trim().isEmpty()) {
            edtDirrecion.setError("La dirección es obligatoria");
            valido = false;
        }

        String telefono = edtTelefono.getText().toString().trim();
        if (telefono.isEmpty()) {
            edtTelefono.setError("El teléfono es obligatorio");
            valido = false;
        } else if (!telefono.matches("\\d{10}")) {
            edtTelefono.setError("El teléfono debe tener exactamente 10 dígitos numéricos");
            Toast.makeText(getContext(), "El teléfono debe tener 10 dígitos numéricos", Toast.LENGTH_SHORT).show();
            valido = false;
        }

        if (!chkAceptarTerminos.isChecked()) {
            Toast.makeText(getContext(), "Debes aceptar los Términos y Condiciones", Toast.LENGTH_SHORT).show();
            valido = false;
        }
        String contrasena = edtContrasena.getText().toString().trim();
        if (!validarContrasena(contrasena)) {
            valido = false;
        }

        return valido;
    }

    private boolean esCorreoValido(String correo) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches();
    }

    private void registrarUsuarioEnBackend() {
        progressDialog.show();

        String nombre = edtNombre.getText().toString().trim();
        String apellido = edtApellido.getText().toString().trim();
        String identificacion = edtIdentificacion.getText().toString().trim();
        String direccion = edtDirrecion.getText().toString().trim();
        String telefono = edtTelefono.getText().toString().trim();
        String correo = edtCorreo.getText().toString().trim();
        String contrasena = generarContrasenaSegura(edtContrasena.getText().toString().trim());

        RegisterRequest request = new RegisterRequest(
                nombre,
                apellido,
                identificacion,
                direccion,
                telefono,
                correo,
                contrasena
        );

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        AuthService authService = retrofit.create(AuthService.class);

        Call<RegisterResponse> call = authService.registerUser(request);
        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                progressDialog.dismiss();

                if (response.isSuccessful()) {
                    manejarRegistroExitoso();
                } else {
                    manejarErrorRegistro(response);
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String generarContrasenaSegura(String identificacion) {
        return identificacion;
    }

    private void manejarRegistroExitoso() {
        Toast.makeText(getContext(), "Registro exitoso. Verifica tu correo electrónico.", Toast.LENGTH_LONG).show();
        confirmCorreo();
    }

    private void manejarErrorRegistro(Response<RegisterResponse> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error desconocido";
            Toast.makeText(getContext(), "Error al registrar: " + errorBody, Toast.LENGTH_LONG).show();
            System.out.println(errorBody);
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
        }
    }

    private void volverALogin() {
        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, new Login())
                .commit();
    }

    private void confirmCorreo() {
        String correo = edtCorreo.getText().toString().trim();
        VerficacionCorreo fragment = VerficacionCorreo.newInstance(correo);

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
