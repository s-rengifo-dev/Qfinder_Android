package com.sena.qfinder.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.sena.qfinder.R;

public class PerfilComunidad extends Fragment {

    private static final String ARG_PARAM1 = "nombreComunidad";
    private static final String ARG_PARAM2 = "miembrosComunidad";
    private static final String ARG_PARAM3 = "imagenComunidad";

    private String nombreComunidad;
    private String miembrosComunidad;
    private String imagenRed;

    public PerfilComunidad() {
        // Constructor vacío requerido
    }

    public static PerfilComunidad newInstance(String nombre, String miembros, String imagenUrl) {
        PerfilComunidad fragment = new PerfilComunidad();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, nombre);
        args.putString(ARG_PARAM2, miembros);
        args.putString(ARG_PARAM3, imagenUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            nombreComunidad = getArguments().getString(ARG_PARAM1);
            miembrosComunidad = getArguments().getString(ARG_PARAM2);
            imagenRed = getArguments().getString(ARG_PARAM3);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil_comunidad, container, false);

        // Referencias a vistas
        TextView tvNombre = view.findViewById(R.id.tvNombreComunidad);
        TextView tvMiembros = view.findViewById(R.id.tvMiembrosComunidad);
        ImageView imgComunidad = view.findViewById(R.id.imgComunidad);
        ImageButton btnBack = view.findViewById(R.id.btnBack);

        // Mostrar los datos
        if (nombreComunidad != null) {
            tvNombre.setText(nombreComunidad);
        }

        if (miembrosComunidad != null) {
            tvMiembros.setText("Comunidad " + miembrosComunidad + " miembros");
        }

        // Cargar la imagen con Glide
        if (imagenRed != null && !imagenRed.isEmpty()) {
            Glide.with(this)
                    .load(imagenRed)
                    .placeholder(R.drawable.imgcomunidad)
                    .circleCrop()
                    .error(R.drawable.imgcomunidad)
                    .into(imgComunidad);
        } else {
            // Si no hay imagen, mostrar una por defecto
            imgComunidad.setImageResource(R.drawable.imgcomunidad);
        }

        // Acción del botón volver
        btnBack.setOnClickListener(v -> {
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new Comunidad());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
    }
}
