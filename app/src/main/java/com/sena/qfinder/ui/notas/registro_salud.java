package com.sena.qfinder.ui.notas;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

public class registro_salud extends Fragment {

    public registro_salud() {
        // Constructor vacío requerido
    }

    public static registro_salud newInstance(String param1, String param2) {
        registro_salud fragment = new registro_salud();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }
    /*@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registro_salud, container, false);

        LinearLayout episodiosCard = view.findViewById(R.id.EpisodiosSalud);

        episodiosCard.setOnClickListener(v -> {
            // Crear Intent para abrir la actividad
            Intent intent = new Intent(requireActivity(), episodios_salud_menu.class);
            startActivity(intent);
        });

        return view;
    }*/
}
