package com.sena.qfinder.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.sena.qfinder.R;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.PerfilUsuarioResponse;
import com.sena.qfinder.data.models.UsuarioRequest;
import com.sena.qfinder.controller.MainActivity;
import com.sena.qfinder.ui.auth.EditarUsuarioDialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PerfilUsuario extends Fragment implements EditarUsuarioDialogFragment.OnUsuarioActualizadoListener {

    private TextView tvNombre, tvApellido, tvTelefono, tvCorreo, tvDireccion, tvIdentificacion;
    private LinearLayout cerrarSesion, logoEditar;
    private ImageView btnBack, imagenPerfilP;

    private AuthService authService;
    private Call<PerfilUsuarioResponse> perfilCall;
    private Call<Void> logoutCall;
    private Call<Void> actualizarCall;
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
        imagenPerfilP = view.findViewById(R.id.imgAvatar);
        btnBack = view.findViewById(R.id.btnBack);
        cerrarSesion = view.findViewById(R.id.ivLogout);
        logoEditar = view.findViewById(R.id.btnEditar);

        setupRetrofit();
        cargarPerfil();
        setupBackButton();

        cerrarSesion.setOnClickListener(v -> mostrarDialogoCerrarSesion());
        logoEditar.setOnClickListener(v -> abrirDialogoEditar());

        return view;
    }

    private void setupRetrofit() {
        retrofit = new Retrofit.Builder()
                .baseUrl("https://qfinder-production.up.railway.app/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        authService = retrofit.create(AuthService.class);
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> navigateBack());
    }

    private void mostrarDialogoCerrarSesion() {
        if (!isAdded()) return;

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialogInterface, which) -> logout())
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            // Estilos comunes
            if (positiveButton != null) {
                positiveButton.setAllCaps(false);
                positiveButton.setText("Sí");
                positiveButton.setTextColor(Color.WHITE);
                positiveButton.setBackgroundColor(Color.parseColor("#18A0FB"));

                // Añadir margen derecho
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                params.setMarginStart(32); // Espacio entre botones
                positiveButton.setLayoutParams(params);
            }

            if (negativeButton != null) {
                negativeButton.setAllCaps(false);
                negativeButton.setText("Cancelar");
                negativeButton.setTextColor(Color.parseColor("#18A0FB"));

                // Crear borde personalizado
                GradientDrawable borderDrawable = new GradientDrawable();
                borderDrawable.setColor(Color.WHITE); // Fondo blanco
                borderDrawable.setStroke(60, Color.parseColor("#18A0FB")); // Borde azul
                borderDrawable.setCornerRadius(30); // Esquinas redondeadas

                negativeButton.setBackground(borderDrawable);
            }

        });

        dialog.show();
    }




    private void logout() {
        if (!isAdded()) return;

        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Token no encontrado. Inicia sesión nuevamente.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        logoutCall = authService.logout();
        logoutCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) return;

                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.apply();

                Toast.makeText(requireContext(), "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(requireActivity(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                if (isAdded()) {
                    requireActivity().finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarPerfil() {
        if (!isAdded()) return;

        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);
        if (token == null) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "No se encontró token. Inicia sesión.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        perfilCall = authService.obtenerPerfil("Bearer " + token);
        perfilCall.enqueue(new Callback<PerfilUsuarioResponse>() {
            @Override
            public void onResponse(@NonNull Call<PerfilUsuarioResponse> call, @NonNull Response<PerfilUsuarioResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    usuarioActual = response.body();
                    tvNombre.setText(usuarioActual.getNombre_usuario());
                    tvApellido.setText(usuarioActual.getApellido_usuario());
                    tvTelefono.setText(usuarioActual.getTelefono_usuario());
                    tvCorreo.setText(usuarioActual.getCorreo_usuario());
                    tvDireccion.setText(usuarioActual.getDireccion_usuario());
                    tvIdentificacion.setText(usuarioActual.getIdentificacion_usuario());
                    loadProfileImage(usuarioActual.getImagen_usuario());
                } else {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "No se pudo obtener el perfil.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<PerfilUsuarioResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadProfileImage(String imageData) {
        if (!isAdded() || imagenPerfilP == null || imageData == null) return;

        try {
            if (imageData.startsWith("http")) {
                Glide.with(requireContext())
                        .load(imageData + "?t=" + System.currentTimeMillis())
                        .apply(new RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .circleCrop())
                        .into(imagenPerfilP);
            } else {
                String base64 = imageData.contains(",") ? imageData.split(",")[1] : imageData;
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                Glide.with(requireContext()).load(bitmap).circleCrop().into(imagenPerfilP);
            }
        } catch (Exception e) {
            imagenPerfilP.setImageResource(R.drawable.perfil_paciente);
        }
    }

    private void abrirDialogoEditar() {
        if (!isAdded() || usuarioActual == null) return;

        UsuarioRequest userRequest = new UsuarioRequest(
                usuarioActual.getNombre_usuario(),
                usuarioActual.getApellido_usuario(),
                usuarioActual.getDireccion_usuario(),
                usuarioActual.getTelefono_usuario(),
                usuarioActual.getCorreo_usuario(),
                usuarioActual.getImagen_usuario()
        );

        EditarUsuarioDialogFragment dialog = EditarUsuarioDialogFragment.newInstance(userRequest, this);
        dialog.show(getChildFragmentManager(), "EditarUsuarioDialog");
    }

    @Override
    public void onUsuarioActualizado(UsuarioRequest nuevoUsuario) {
        if (!isAdded()) return;

        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);
        if (token == null) return;

        actualizarCall = authService.actualizarUsuario("Bearer " + token, nuevoUsuario);
        actualizarCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show();
                    cargarPerfil();
                } else {
                    Toast.makeText(requireContext(), "Error al actualizar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateBack() {
        if (!isAdded()) return;

        FragmentManager fragmentManager = getParentFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (perfilCall != null) perfilCall.cancel();
        if (logoutCall != null) logoutCall.cancel();
        if (actualizarCall != null) actualizarCall.cancel();
    }
}