package com.sena.qfinder.controller;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.FirebaseApp;
import com.sena.qfinder.Inicio;
import com.sena.qfinder.R;

public class MainActivity extends AppCompatActivity {

    private boolean isInicioFragmentShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        FirebaseApp.initializeApp(this);
        if (savedInstanceState == null) {
            loadFragment(new Inicio());
            isInicioFragmentShown = true;
        }


    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isInicioFragmentShown) {
            loadFragment(new Inicio());
            isInicioFragmentShown = true;
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}