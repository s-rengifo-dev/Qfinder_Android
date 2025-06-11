package com.sena.qfinder.utils;

import android.content.Context;
import android.util.Log;

import com.sena.qfinder.database.DatabaseHelper;
import com.sena.qfinder.database.entity.AlarmaEntity;
import com.sena.qfinder.data.models.CitaMedica;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CitaAlarmManager {
    private static final String TAG = "CitaAlarmManager";

    public static void programarAlarmasParaCita(Context context, CitaMedica cita) {
        // Programar alarma principal (en el momento de la cita)
        programarAlarmaPrincipal(context, cita);

        // Programar recordatorios (24 horas y 1 hora antes)
        programarRecordatorios(context, cita);
    }

    private static void programarAlarmaPrincipal(Context context, CitaMedica cita) {
        try {
            // Combinar fecha y hora de la cita
            String fechaHoraCita = cita.getFechaCita() + " " + cita.getHoraCita();

            // Parsear fecha y hora combinadas
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date fechaCita = sdf.parse(fechaHoraCita);

            if (fechaCita == null) {
                Log.e(TAG, "Fecha de cita no válida");
                return;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(fechaCita);

            // Crear entidad de alarma para guardar en BD local
            AlarmaEntity alarma = new AlarmaEntity(
                    cita.getIdCita(), // Usamos el ID de la cita como ID de alarma
                    cita.getTituloCita() != null ? cita.getTituloCita() : "Recordatorio de cita",
                    cita.getDescripcion() != null ? cita.getDescripcion() : "Tienes una cita médica programada",
                    formatDate(calendar),
                    formatTime(calendar),
                    calendar.getTimeInMillis(),
                    true
            );

            // Guardar en base de datos local
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
            dbHelper.guardarAlarma(alarma);

            // Programar alarma en el sistema
            ActivityAlarmReceiver.programarAlarma(
                    context,
                    alarma.getId(),
                    alarma.getTitulo(),
                    alarma.getDescripcion(),
                    alarma.getFecha(),
                    alarma.getHora(),
                    alarma.getTimestamp()
            );

            Log.d(TAG, "Alarma principal programada para cita ID: " + cita.getIdCita());

        } catch (ParseException e) {
            Log.e(TAG, "Error al parsear fecha de cita", e);
        }
    }

    private static void programarRecordatorios(Context context, CitaMedica cita) {
        try {
            // Combinar fecha y hora de la cita
            String fechaHoraCita = cita.getFechaCita() + " " + cita.getHoraCita();

            // Parsear fecha y hora combinadas
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date fechaCita = sdf.parse(fechaHoraCita);

            if (fechaCita == null) {
                Log.e(TAG, "Fecha de cita no válida para recordatorios");
                return;
            }

            Calendar calendar24h = Calendar.getInstance();
            calendar24h.setTime(fechaCita);
            calendar24h.add(Calendar.HOUR_OF_DAY, -24); // 24 horas antes

            Calendar calendar1h = Calendar.getInstance();
            calendar1h.setTime(fechaCita);
            calendar1h.add(Calendar.HOUR_OF_DAY, -1); // 1 hora antes

            // Programar recordatorio 24 horas antes
            if (!cita.isNotificado24h()) {
                AlarmaEntity alarma24h = new AlarmaEntity(
                        cita.getIdCita() * 10 + 1, // ID único para este recordatorio
                        "Recordatorio: " + (cita.getTituloCita() != null ? cita.getTituloCita() : "Cita médica"),
                        "Tienes una cita programada para mañana",
                        formatDate(calendar24h),
                        formatTime(calendar24h),
                        calendar24h.getTimeInMillis(),
                        true
                );

                DatabaseHelper.getInstance(context).guardarAlarma(alarma24h);
                ActivityAlarmReceiver.programarAlarma(
                        context,
                        alarma24h.getId(),
                        alarma24h.getTitulo(),
                        alarma24h.getDescripcion(),
                        alarma24h.getFecha(),
                        alarma24h.getHora(),
                        alarma24h.getTimestamp()
                );
            }

            // Programar recordatorio 1 hora antes
            if (!cita.isNotificado1h()) {
                AlarmaEntity alarma1h = new AlarmaEntity(
                        cita.getIdCita() * 10 + 2, // ID único para este recordatorio
                        "Recordatorio: " + (cita.getTituloCita() != null ? cita.getTituloCita() : "Cita médica"),
                        "Tienes una cita programada en 1 hora",
                        formatDate(calendar1h),
                        formatTime(calendar1h),
                        calendar1h.getTimeInMillis(),
                        true
                );

                DatabaseHelper.getInstance(context).guardarAlarma(alarma1h);
                ActivityAlarmReceiver.programarAlarma(
                        context,
                        alarma1h.getId(),
                        alarma1h.getTitulo(),
                        alarma1h.getDescripcion(),
                        alarma1h.getFecha(),
                        alarma1h.getHora(),
                        alarma1h.getTimestamp()
                );
            }

        } catch (ParseException e) {
            Log.e(TAG, "Error al parsear fecha de cita para recordatorios", e);
        }
    }

    public static void cancelarAlarmasParaCita(Context context, int citaId) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);

        // Cancelar alarma principal
        ActivityAlarmReceiver.cancelarAlarma(context, citaId);
        dbHelper.eliminarAlarma(citaId);

        // Cancelar recordatorios (usamos IDs derivados)
        ActivityAlarmReceiver.cancelarAlarma(context, citaId * 10 + 1);
        dbHelper.eliminarAlarma(citaId * 10 + 1);

        ActivityAlarmReceiver.cancelarAlarma(context, citaId * 10 + 2);
        dbHelper.eliminarAlarma(citaId * 10 + 2);

        Log.d(TAG, "Todas las alarmas canceladas para cita ID: " + citaId);
    }

    private static String formatDate(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    private static String formatTime(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }
}