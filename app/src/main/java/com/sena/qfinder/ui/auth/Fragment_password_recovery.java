package com.sena.qfinder.ui.auth;

import android.app.ProgressDialog;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.sena.qfinder.ui.home.Login;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.SendCodeRequest;
import com.sena.qfinder.data.models.SendCodeResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Fragment_password_recovery extends Fragment {

    private EditText edtEmail;
    private Button btnSend;
    private ImageView backButton;
    private ProgressDialog progressDialog;

    public Fragment_password_recovery() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_password_recovery, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtEmail = view.findViewById(R.id.emailEditText);
        btnSend = view.findViewById(R.id.btnSend);
        backButton = view.findViewById(R.id.backButton);

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Enviando código...");
        progressDialog.setCancelable(false);

        backButton.setOnClickListener(v -> navigateBackToLogin());

        btnSend.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(getContext(), "Ingresa un correo electrónico", Toast.LENGTH_SHORT).show();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(getContext(), "El correo electrónico no es válido", Toast.LENGTH_SHORT).show();
            } else {
                enviarCodigoRecuperacion(email);
            }
        });
    }

    private void enviarCodigoRecuperacion(String email) {
        progressDialog.show();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        AuthService authService = retrofit.create(AuthService.class);
        Call<SendCodeResponse> call = authService.SendCode(new SendCodeRequest(email));

        call.enqueue(new Callback<SendCodeResponse>() {
            @Override
            public void onResponse(Call<SendCodeResponse> call, Response<SendCodeResponse> response) {
                progressDialog.dismiss();

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Código enviado a " + email, Toast.LENGTH_SHORT).show();

                    Bundle bundle = new Bundle();
                    bundle.putString("email", email);

                    Fragment_verificar_codigo verificarCodigoFragment = new Fragment_verificar_codigo();
                    verificarCodigoFragment.setArguments(bundle);

                    FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                    transaction.replace(R.id.fragment_container, verificarCodigoFragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                } else {
                    Toast.makeText(getContext(), "Error al enviar el código", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<SendCodeResponse> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateBackToLogin() {
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, new Login());
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
