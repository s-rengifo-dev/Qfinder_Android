package com.sena.qfinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.sena.qfinder.ui.home.RecordatoriosFragment;

public class Fragment_Serivicios extends Fragment {


    public Fragment_Serivicios() {
        // Constructor requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment__serivicios, container, false);

        //Llamamentos fragment medicamentos
        LinearLayout cardGestionMedicamentos = view.findViewById(R.id.btnMedicamentos);
        cardGestionMedicamentos.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new Medicamentos());
            transaction.addToBackStack(null); // Para poder volver atrás
            transaction.commit();
        });

        LinearLayout cardGestionPacientes = view.findViewById(R.id.cardGestionPacientes);
        cardGestionPacientes.setOnClickListener(v -> {
            // Reemplazar fragmento actual por GestionPacienteFragment
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new GestionPacienteFragment()); // Asegúrate de que R.id.fragment_container exista
            transaction.addToBackStack(null); // Para poder volver atrás
            transaction.commit();
        });

        LinearLayout cardRegistroSalud = view.findViewById(R.id.registrosalud);
        cardRegistroSalud.setOnClickListener(v -> {
            // Iniciar nueva actividad
            Intent intent = new Intent(getActivity(), episodios_salud_menu.class);
            startActivity(intent);
        });


        // Card para recordatorios
        LinearLayout cardRecordatorio = view.findViewById(R.id.cardRecordatorio);
        cardRecordatorio.setOnClickListener(v -> {
            // Reemplazar fragmento actual por Actividad1Fragment
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new RecordatoriosFragment()); // Aquí carga el nuevo fragmento
            transaction.addToBackStack(null); // Para poder volver atrás
            transaction.commit();
        });


        // Card para recordatorios
        LinearLayout cardActividad = view.findViewById(R.id.cardActividad);
        cardActividad.setOnClickListener(v -> {
            // Reemplazar fragmento actual por Actividad1Fragment
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new Actividad1Fragment()); // Aquí carga el nuevo fragmento
            transaction.addToBackStack(null); // Para poder volver atrás
            transaction.commit();
        });



        return view;

    }
}
