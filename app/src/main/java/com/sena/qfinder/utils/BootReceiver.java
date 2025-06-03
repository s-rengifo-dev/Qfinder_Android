package com.sena.qfinder.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sena.qfinder.database.DatabaseHelper;
import com.sena.qfinder.database.entity.AlarmaEntity;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Dispositivo reiniciado. Reprogramando alarmas...");

            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
            List<AlarmaEntity> alarmas = dbHelper.obtenerTodasAlarmas();

            for (AlarmaEntity alarma : alarmas) {
                if (alarma.isActive() && alarma.getTimestamp() > System.currentTimeMillis()) {
                    ActivityAlarmReceiver.programarAlarma(
                            context,
                            alarma.getId(),
                            alarma.getTitulo(),
                            alarma.getDescripcion(),
                            alarma.getFecha(),
                            alarma.getHora(),
                            alarma.getTimestamp()
                    );
                }
            }
        }
    }
}