package com.sena.qfinder.controller;

import static com.google.firebase.FirebaseApp.initializeApp;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.FirebaseApp;
import com.sena.qfinder.Comunidad;
import com.sena.qfinder.Fragment_Serivicios;
import com.sena.qfinder.R;
import com.sena.qfinder.perfil_usuario;
import com.sena.qfinder.ui.home.DashboardFragment;

public class MainActivityDash extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuración crítica para el espacio de la barra de estado
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
//        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main_dash);

        // Configuración de la navegación inferior
        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(navListener);

        // Carga el fragment inicial solo si es una nueva instancia
        if (savedInstanceState == null) {
            loadInitialFragment();
        }
    }

    private void loadInitialFragment() {
        DashboardFragment dashboardFragment = new DashboardFragment();

        // Puedes pasar argumentos si es necesario
        // Bundle args = new Bundle();
        // args.putString("key", "value");
        // dashboardFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, dashboardFragment)
                .commit();
    }

    private final NavigationBarView.OnItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                // Selección de fragmentos
                if (itemId == R.id.nav_home) {
                    selectedFragment = new DashboardFragment();
                } else if (itemId == R.id.nav_services) {
                    selectedFragment = new Fragment_Serivicios();
                } else if (itemId == R.id.nav_comunidad) {
                    selectedFragment = new Comunidad();
                }
                // Agrega más casos según sea necesario
                // else if (itemId == R.id.nav_profile) {
                //     selectedFragment = new ProfileFragment();
                // }

                else if (itemId == R.id.nav_profile) {
                    selectedFragment = new perfil_usuario();
                }

                // } else if (itemId == R.id.nav_community) {
                //     selectedFragment = new CommunityFragment();
                //

                // Reemplaza el fragmento actual si se seleccionó uno válido
                if (selectedFragment != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                    return true;
                }

                return false;
            };

    @Override
    public void onBackPressed() {
        // Personaliza el comportamiento del botón atrás si es necesario
        if (bottomNavigation.getSelectedItemId() != R.id.nav_home) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        } else {
            super.onBackPressed();
        }
    }
}