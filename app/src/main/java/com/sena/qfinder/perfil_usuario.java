package com.sena.qfinder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.card.MaterialCardView;
import com.sena.qfinder.api.AuthService;
import com.sena.qfinder.models.PerfilUsuarioResponse;
import com.sena.qfinder.models.UsuarioRequest;
import com.sena.qfinder.controller.MainActivity;
import com.sena.qfinder.ui.home.DashboardFragment;
import com.sena.qfinder.ui.home.EditarUsuarioDialogFragment;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class perfil_usuario extends Fragment implements EditarUsuarioDialogFragment.OnUsuarioActualizadoListener {

    private TextView tvNombre, tvApellido, tvTelefono, tvCorreo, tvDireccion, tvIdentificacion;
    private LinearLayout cerrarSesion;
    private LinearLayout logoEditar;  // Antes era ImageView
    private ImageView btnBack;

    private AuthService authService;
    private Call<PerfilUsuarioResponse> perfilCall;
    private Call<Void> logoutCall;
    private Retrofit retrofit;

    private PerfilUsuarioResponse usuarioActual;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil_usuario, container, false);

        tvNombre = view.findViewById(R.id.tvNombre);
        tvApellido = view.findViewById(R.id.tvApellido);
        tvTelefono = view.findViewById(R.id.tvTelefono);
        tvCorreo = view.findViewById(R.id.tvCorreo);
        tvDireccion = view.findViewById(R.id.tvDireccion);
        tvIdentificacion = view.findViewById(R.id.tvIdentificacion);
        btnBack = view.findViewById(R.id.btnBack);

        cerrarSesion = view.findViewById(R.id.ivLogout);
        logoEditar = view.findViewById(R.id.btnEditar);

        setupRetrofit();
        cargarPerfil();
        setupBackButton();

        cerrarSesion.setOnClickListener(v -> mostrarDialogoCerrarSesion());



        logoEditar.setOnClickListener(v -> {
            if (usuarioActual != null) {
                UsuarioRequest userRequest = new UsuarioRequest(
                        usuarioActual.getNombre_usuario(),
                        usuarioActual.getApellido_usuario(),
                        usuarioActual.getDireccion_usuario(),
                        usuarioActual.getTelefono_usuario(),
                        usuarioActual.getCorreo_usuario()
                );

                EditarUsuarioDialogFragment dialog = EditarUsuarioDialogFragment.newInstance(userRequest, this);
                dialog.show(getChildFragmentManager(), "EditarUsuarioDialog");
            }
        });

        return view;
    }

    private void setupBackButton() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBack());
        }
    }
    private void mostrarDialogoCerrarSesion() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> logout())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void setupRetrofit() {
        retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        authService = retrofit.create(AuthService.class);
    }

    private void logout() {
        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            Toast.makeText(requireContext(), "Token no encontrado. Inicia sesión nuevamente.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Redirigir a MainActivity
        Intent intent = new Intent(requireActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requireActivity().finish();

        logoutCall = authService.logout();
        logoutCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull retrofit2.Response<Void> response) {
                if (!isAdded()) return;

                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.apply();

                Toast.makeText(requireContext(), "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();

                View bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setVisibility(View.GONE);
                }

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new Login())
                        .commit();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarPerfil() {
        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            Toast.makeText(requireContext(), "No se encontró token. Por favor, inicia sesión.", Toast.LENGTH_SHORT).show();
            return;
        }

        perfilCall = authService.obtenerPerfil("Bearer " + token);

        perfilCall.enqueue(new Callback<PerfilUsuarioResponse>() {
            @Override
            public void onResponse(@NonNull Call<PerfilUsuarioResponse> call, @NonNull retrofit2.Response<PerfilUsuarioResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    usuarioActual = response.body();

                    tvNombre.setText(usuarioActual.getNombre_usuario());
                    tvApellido.setText(usuarioActual.getApellido_usuario());
                    tvTelefono.setText(usuarioActual.getTelefono_usuario());
                    tvCorreo.setText(usuarioActual.getCorreo_usuario());
                    tvDireccion.setText(usuarioActual.getDireccion_usuario());
                    tvIdentificacion.setText(usuarioActual.getIdentificacion_usuario());
                } else {
                    Toast.makeText(requireContext(), "No se pudo obtener el perfil.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PerfilUsuarioResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("PerfilUsuario", "Error al cargar perfil", t);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (perfilCall != null && !perfilCall.isCanceled()) {
            perfilCall.cancel();
        }
        if (logoutCall != null && !logoutCall.isCanceled()) {
            logoutCall.cancel();
        }
    }

    @Override
    public void onUsuarioActualizado() {
        cargarPerfil(); // Vuelve a cargar datos actualizados tras edición
    }
    private void navigateBack() {
        FragmentManager fragmentManager = getParentFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new DashboardFragment());
            transaction.commit();
        }
    }
}
