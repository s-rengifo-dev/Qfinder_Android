package com.sena.qfinder.data.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.ColaboradorListResponse;
import com.sena.qfinder.data.models.ColaboradorResponse;

import java.util.*;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class GestionColaboradorFragment extends Fragment {

    private LinearLayout containerColaboradores;
    private final String BASE_URL = "https://qfinder-production.up.railway.app/";

    public GestionColaboradorFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_gestion_colaborador, container, false);

        containerColaboradores = root.findViewById(R.id.containerColaboradores);

        ImageView btnBack = root.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {requireActivity().getSupportFragmentManager().popBackStack();});

        cargarColaboradores(inflater);

        return root;
    }

    private void cargarColaboradores(LayoutInflater inflater) {
        SharedPreferences prefs = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "Token no encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(logging).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        AuthService authService = retrofit.create(AuthService.class);

        authService.listarColaboradores("Bearer " + token).enqueue(new Callback<ColaboradorListResponse>() {
            @Override
            public void onResponse(Call<ColaboradorListResponse> call, Response<ColaboradorListResponse> response) {
                if (!isAdded()) return;

                // Si la respuesta es exitosa y tiene cuerpo
                if (response.isSuccessful() && response.body() != null) {
                    List<ColaboradorResponse> lista = response.body().getColaboradores();
                    mostrarColaboradores(inflater, lista, authService, token);
                } else {
                    // Mostrar mensaje "no tienes colaboradores" también en caso de error o lista nula
                    mostrarColaboradores(inflater, new ArrayList<>(), authService, token);
                }
            }

            @Override
            public void onFailure(Call<ColaboradorListResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                Log.e("COLABORADORES", "Fallo: ", t);
            }
        });

    }

    private void mostrarColaboradores(LayoutInflater inflater, List<ColaboradorResponse> colaboradores, AuthService authService, String token) {
        containerColaboradores.removeAllViews();

        if (colaboradores.isEmpty()) {
            TextView mensaje = new TextView(requireContext());
            mensaje.setText("No tienes colaboradores asignados");
            mensaje.setTextSize(18);
            mensaje.setPadding(20, 120, 20, 20);
            mensaje.setGravity(Gravity.CENTER);
            containerColaboradores.addView(mensaje);
            return;
        }

        for (ColaboradorResponse colaborador : colaboradores) {
            View card = inflater.inflate(R.layout.item_colaborador, containerColaboradores, false);

            TextView nombre = card.findViewById(R.id.nombreColaborador);
            TextView correo = card.findViewById(R.id.correoColaborador);
            TextView paciente = card.findViewById(R.id.pacienteAsociado);
            ImageView imagen = card.findViewById(R.id.imagenColaborador);
            ImageButton btnEliminar = card.findViewById(R.id.btnEliminarColaborador);

            nombre.setText(colaborador.getNombre() + " " + colaborador.getApellido());
            correo.setText(colaborador.getCorreo());
            paciente.setText("Paciente: " + colaborador.getNombre_paciente());

            Glide.with(requireContext())
                    .load(colaborador.getImagen_usuario())
                    .placeholder(R.drawable.perfil_paciente)
                    .error(R.drawable.perfil_paciente)
                    .circleCrop()
                    .into(imagen);

            btnEliminar.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Confirmar eliminación")
                        .setMessage("¿Estás seguro de que deseas eliminar este colaborador?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            Map<String, Integer> body = new HashMap<>();
                            body.put("id_usuario", colaborador.getId_usuario());
                            body.put("id_paciente", colaborador.getId_paciente());

                            authService.eliminarColaborador("Bearer " + token, body).enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(getContext(), "Colaborador eliminado", Toast.LENGTH_SHORT).show();
                                        cargarColaboradores(inflater);
                                    } else {
                                        Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    Toast.makeText(getContext(), "Fallo al eliminar", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("No", null)
                        .show();
            });

            containerColaboradores.addView(card);
        }
    }
}
