package com.sena.qfinder.ui.home;

import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sena.qfinder.R;

public class Inicio extends Fragment {

    public Inicio() {}

    public static Inicio newInstance() {
        return new Inicio();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Retraso para cambiar al fragmento Login
        new Handler().postDelayed(() -> {
            if (isAdded() && getActivity() != null) { // Verifica que el fragmento esté adjunto
                FragmentTransaction transaction = requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction();
                transaction.replace(R.id.fragment_container, new Login());
                transaction.commit();
            }
        }, 2000);



    }
}