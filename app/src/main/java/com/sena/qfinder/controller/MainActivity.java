package com.sena.qfinder.controller;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.FirebaseApp;
import com.sena.qfinder.R;
import com.sena.qfinder.utils.ActivityAlarmReceiver;
import com.sena.qfinder.ui.home.Inicio;

public class MainActivity extends AppCompatActivity {

    private boolean isInicioFragmentShown = false;
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1001;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Limpiar alarmas residuales
//        cleanUpStaleAlarms();

        // 2. Verificar optimizaciones para Xiaomi
        if (Build.MANUFACTURER.equalsIgnoreCase("xiaomi")) {
            checkXiaomiOptimizations();
        }

        // 3. Configuración de fragmento inicial
        if (savedInstanceState == null) {
            loadFragment(new Inicio());
            isInicioFragmentShown = true;
        }

        // 4. Configuración de barra de navegación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#4b9af6"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        // 5. Inicialización de Firebase
        FirebaseApp.initializeApp(this);
    }
    // En tu actividad principal o donde configures las alarmas
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (!alarmManager.canScheduleExactAlarms()) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(intent);
                }
            }
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

    // Métodos añadidos para manejo de alarmas y optimizaciones
    private void cleanUpStaleAlarms() {
        ActivityAlarmReceiver.cancelarAlarma(this, -1);
        ActivityAlarmReceiver.cancelarAlarma(this, 0);
    }

    private void checkXiaomiOptimizations() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                showXiaomiOptimizationDialog();
            }
        }
    }

    private void showXiaomiOptimizationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Configuración requerida")
                .setMessage("Para que las alarmas funcionen correctamente en dispositivos Xiaomi:\n\n" +
                        "1. Abre Configuración\n" +
                        "2. Ve a 'Batería y rendimiento'\n" +
                        "3. Selecciona 'Configurar uso de batería'\n" +
                        "4. Elige 'Todas las apps'\n" +
                        "5. Busca esta app y selecciona 'Sin restricciones'")
                .setPositiveButton("Abrir configuración", (d, w) -> openBatterySettings())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void openBatterySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }
}