package com.sena.qfinder.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.sena.qfinder.R;

public class PerfilComunidad extends Fragment {

    private static final String ARG_PARAM1 = "nombreComunidad";
    private static final String ARG_PARAM2 = "descripcionComunidad"; // Cambiado de miembros a descripción
    private static final String ARG_PARAM3 = "imagenComunidad";

    private String nombreComunidad;
    private String descripcionComunidad; // Cambiado de miembros a descripción
    private String imagenRed;

    public PerfilComunidad() {
        // Constructor vacío requerido
    }

    public static PerfilComunidad newInstance(String nombre, String descripcion, String imagenUrl) {
        PerfilComunidad fragment = new PerfilComunidad();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, nombre);
        args.putString(ARG_PARAM2, descripcion);
        args.putString(ARG_PARAM3, imagenUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            nombreComunidad = getArguments().getString(ARG_PARAM1);
            descripcionComunidad = getArguments().getString(ARG_PARAM2);
            imagenRed = getArguments().getString(ARG_PARAM3);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil_comunidad, container, false);

        // Referencias a vistas
        TextView tvNombre = view.findViewById(R.id.tvNombreComunidad);
        TextView tvDescripcion = view.findViewById(R.id.tvMiembrosComunidad);
        ImageView imgComunidad = view.findViewById(R.id.imgComunidad);
        ImageView btnBack = view.findViewById(R.id.btnBack);
        LinearLayout btnShared = view.findViewById(R.id.btnShared);

        // Mostrar los datos
        if (nombreComunidad != null) {
            tvNombre.setText(nombreComunidad);
        }

        if (descripcionComunidad != null && !descripcionComunidad.isEmpty()) {
            tvDescripcion.setText(descripcionComunidad);
        } else {
            tvDescripcion.setText("Esta comunidad no tiene descripción");
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

        // Acción del botón compartir
        btnShared.setOnClickListener(v -> compartirComunidad());

        return view;
    }

    // Función para compartir comunidad
    private void compartirComunidad() {
        String nombre = nombreComunidad != null ? nombreComunidad : "Comunidad";
        String descripcion = descripcionComunidad != null ? descripcionComunidad : "Sin descripción";

        String mensaje = "¡Únete a nuestra comunidad!\n\n" +
                "Nombre: " + nombre + "\n" +
                "Descripción: " + descripcion + "\n" +
                "Descubre más en la app QFinder.";

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "¡Te invito a una comunidad!");
        intent.putExtra(Intent.EXTRA_TEXT, mensaje);

        startActivity(Intent.createChooser(intent, "Compartir comunidad con..."));
    }
}
