package com.sena.qfinder.utils;

import android.util.Log;
import java.util.Locale;

public class AlarmCalculator {
    private static final String TAG = "AlarmCalculator";

    public static String[] parseFrecuencia(String frecuencia) {
        if (frecuencia == null || frecuencia.trim().isEmpty()) {
            return new String[]{"1", "horas"};
        }

        frecuencia = frecuencia.trim().toLowerCase(Locale.getDefault());

        try {
            // Si es un número simple, asumir horas
            if (frecuencia.matches("^\\d+$")) {
                return new String[]{frecuencia, "horas"};
            }

            // Intentar dividir cantidad y unidad
            String[] parts = frecuencia.split("\\s+");
            if (parts.length == 2) {
                // Validar que la cantidad sea número
                Integer.parseInt(parts[0]);
                return new String[]{parts[0], normalizarUnidad(parts[1])};
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al parsear frecuencia: " + frecuencia, e);
        }

        // Valor por defecto si hay error
        return new String[]{"1", "horas"};
    }

    public static long calcularIntervalo(String frecuencia) {
        String[] partes = parseFrecuencia(frecuencia);
        int cantidad = Integer.parseInt(partes[0]);
        String unidad = partes[1];
        return calcularIntervalo(cantidad, unidad);
    }

    public static long calcularIntervalo(int cantidad, String unidad) {
        switch (unidad.toLowerCase(Locale.getDefault())) {
            case "hora":
            case "horas":
                return cantidad * 60 * 60 * 1000L;
            case "dia":
            case "día":
            case "dias":
            case "días":
                return cantidad * 24 * 60 * 60 * 1000L;
            case "semana":
            case "semanas":
                return cantidad * 7 * 24 * 60 * 60 * 1000L;
            case "mes":
            case "meses":
                return cantidad * 30L * 24 * 60 * 60 * 1000L;
            default:
                Log.w(TAG, "Unidad de frecuencia no reconocida: " + unidad + ". Usando 1 hora por defecto.");
                return 60 * 60 * 1000L; // 1 hora por defecto
        }
    }

    private static String normalizarUnidad(String unidad) {
        if (unidad == null) return "horas";

        unidad = unidad.toLowerCase(Locale.getDefault());
        if (unidad.startsWith("hora")) return "horas";
        if (unidad.startsWith("día") || unidad.startsWith("dia")) return "días";
        if (unidad.startsWith("semana")) return "semanas";
        if (unidad.startsWith("mes")) return "meses";
        return unidad;
    }

    public static String convertirHorasAFrecuencia(int horas) {
        if (horas <= 0) return "1 hora";

        if (horas % (24*30) == 0) return (horas/(24*30)) + " meses";
        if (horas % (24*7) == 0) return (horas/(24*7)) + " semanas";
        if (horas % 24 == 0) return (horas/24) + " días";
        return horas + " horas";
    }
}