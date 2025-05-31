package com.sena.qfinder.controller;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.FirebaseApp;
//import com.google.firebase.appcheck.FirebaseAppCheck;
//import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.sena.qfinder.Inicio;
import com.sena.qfinder.R;

public class MainActivity extends AppCompatActivity {

    private boolean isInicioFragmentShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Cargar el fragmento Inicio si es la primera vez
        if (savedInstanceState == null) {
            loadFragment(new Inicio());
            isInicioFragmentShown = true;
        }

        // Cambiar el color de la barra de navegación (opcional)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#4b9af6")); // Azul
        }

        // Íconos oscuros para barra de navegación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        // Inicializar Firebase y AppCheck en modo Debug
        FirebaseApp.initializeApp(this);

//        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
//        firebaseAppCheck.installAppCheckProviderFactory(
//                DebugAppCheckProviderFactory.getInstance()
//        );
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
