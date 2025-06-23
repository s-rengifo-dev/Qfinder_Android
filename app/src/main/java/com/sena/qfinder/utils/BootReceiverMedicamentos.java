package com.sena.qfinder.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sena.qfinder.database.DatabaseMedicamentoHelper;
import com.sena.qfinder.database.entity.AlarmaMedicamentoEntity;

import java.util.List;

public class BootReceiverMedicamentos extends BroadcastReceiver {
    private static final String TAG = "BootReceiverMed";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Evento recibido: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            DatabaseMedicamentoHelper dbHelper = DatabaseMedicamentoHelper.getInstance(context);
            List<AlarmaMedicamentoEntity> alarmas = dbHelper.obtenerTodasAlarmasActivas();

            if (alarmas != null) {
                for (AlarmaMedicamentoEntity alarma : alarmas) {
                    try {
                        MedicamentoAlarmManager.programarAlarmaExacta(
                                context,
                                alarma.getId(),
                                alarma.getIdMedicamento(),
                                alarma.getIdPaciente(),
                                alarma.getNombreMedicamento(),
                                alarma.getDosis(),
                                alarma.getFrecuencia(),
                                alarma.getTimestampProximaAlarma(),
                                alarma.getIntervaloMillis()
                        );
                    } catch (Exception e) {
                        Log.e(TAG, "Error al reprogramar alarma", e);
                    }
                }
            }
        }
    }
}