package com.sena.qfinder.controller;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.sena.qfinder.ui.home.Comunidad;
import com.sena.qfinder.utils.Constants;
import com.sena.qfinder.ui.home.Fragment_Serivicios;
import com.sena.qfinder.R;
import com.sena.qfinder.ui.home.perfil_usuario;
import com.sena.qfinder.ui.home.DashboardFragment;
import com.sena.qfinder.utils.FCMTokenManager;

public class MainActivityDash extends AppCompatActivity {
    private static final String TAG = "MainActivityDash";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    private BottomNavigationView bottomNavigation;
    private BroadcastReceiver notificationReceiver;
    private boolean isReceiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuración de la ventana
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_main_dash);

        // Inicializar Firebase y FCM
        initializeFirebase();
        requestNotificationPermissions();

        // Configurar navegación inferior
        setupBottomNavigation();

        // Cargar fragmento inicial
        if (savedInstanceState == null) {
            loadInitialFragment();
        }

        // Registrar receptor de notificaciones
        registerNotificationReceiver();
    }

    private void initializeFirebase() {
        try {
            // Suscribirse a temas generales
            FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Subscribed to general topic");
                        } else {
                            Log.e(TAG, "Failed to subscribe to general topic");
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
        }
    }

    private void requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
            } else {
                Toast.makeText(this, "Las notificaciones estarán desactivadas", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(navListener);
    }

    private void loadInitialFragment() {
        DashboardFragment dashboardFragment = new DashboardFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, dashboardFragment)
                .commit();
    }

    private final NavigationBarView.OnItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    selectedFragment = new DashboardFragment();
                } else if (itemId == R.id.nav_services) {
                    selectedFragment = new Fragment_Serivicios();
                } else if (itemId == R.id.nav_comunidad) {
                    selectedFragment = new Comunidad();
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new perfil_usuario();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                    return true;
                }

                return false;
            };

    private void registerNotificationReceiver() {
        notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && Constants.FCM_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
                    handleIncomingNotification(intent);
                }
            }
        };

        IntentFilter filter = new IntentFilter(Constants.FCM_NOTIFICATION_RECEIVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, filter);
        isReceiverRegistered = true;
    }

    private void handleIncomingNotification(Intent intent) {
        String type = intent.getStringExtra("type");
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");

        // Manejar diferentes tipos de notificaciones
        if ("chat".equals(type)) {
            String comunidadId = intent.getStringExtra("comunidadId");
            Log.d(TAG, "Nuevo mensaje en comunidad: " + comunidadId);

            // Mostrar notificación o actualizar UI
            Toast.makeText(this, "Nuevo mensaje en " + comunidadId, Toast.LENGTH_SHORT).show();
        } else if ("medication".equals(type)) {
            String medicamento = intent.getStringExtra("medicamentoNombre");
            Log.d(TAG, "Recordatorio de medicamento: " + medicamento);

            // Mostrar notificación
            Toast.makeText(this, "Es hora de tomar " + medicamento, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Asegurar que el token está actualizado
        FCMTokenManager.getNewToken(this, new FCMTokenManager.TokenUpdateListener() {
            @Override
            public void onTokenReceived(String token) {
                Log.d(TAG, "FCM Token actualizado: " + token);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error actualizando FCM token", e);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpiar el receptor de notificaciones
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
            isReceiverRegistered = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (bottomNavigation.getSelectedItemId() != R.id.nav_home) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        } else {
            super.onBackPressed();
        }
    }
}