package com.sena.qfinder.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.sena.qfinder.R;

public class PerfilComunidad extends Fragment {

    private static final String ARG_PARAM1 = "nombreComunidad";
    private static final String ARG_PARAM2 = "miembrosComunidad";

    private String nombreComunidad;
    private String miembrosComunidad;

    public PerfilComunidad() {
        // Constructor vacío requerido
    }

    public static PerfilComunidad newInstance(String nombre, String miembros) {
        PerfilComunidad fragment = new PerfilComunidad();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, nombre);
        args.putString(ARG_PARAM2, miembros);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            nombreComunidad = getArguments().getString(ARG_PARAM1);
            miembrosComunidad = getArguments().getString(ARG_PARAM2);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil_comunidad, container, false);

        // Referencias a vistas
        TextView tvNombre = view.findViewById(R.id.tvNombreComunidad);
        TextView tvMiembros = view.findViewById(R.id.tvMiembrosComunidad);
        ImageButton btnBack = view.findViewById(R.id.btnBack);

        // Mostrar los datos
        if (nombreComunidad != null) {
            tvNombre.setText(nombreComunidad);
        }

        if (miembrosComunidad != null) {
            tvMiembros.setText("Comunidad " + miembrosComunidad + " miembros");
        }

        // Acción del botón volver
        btnBack.setOnClickListener(v -> {
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new Comunidad()); // Asegúrate que `Comunidad` es el fragment correcto
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
    }
}
