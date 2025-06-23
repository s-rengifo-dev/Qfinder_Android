package com.sena.qfinder.utils;

public class AlarmaMedicamentoContract {
    public static final String TABLE_NAME = "alarmas_medicamentos";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_ID_MEDICAMENTO = "id_medicamento";
    public static final String COLUMN_ID_PACIENTE = "id_paciente";
    public static final String COLUMN_NOMBRE_MEDICAMENTO = "nombre_medicamento";
    public static final String COLUMN_DOSIS = "dosis";
    public static final String COLUMN_FRECUENCIA = "frecuencia";
    public static final String COLUMN_FECHA_INICIO = "fecha_inicio";
    public static final String COLUMN_HORA_INICIO = "hora_inicio";
    public static final String COLUMN_FECHA_FIN = "fecha_fin";
    public static final String COLUMN_TIMESTAMP_PROXIMA = "timestamp_proxima";
    public static final String COLUMN_ACTIVE = "active";
    public static final String COLUMN_INTERVALO = "intervalo_millis";

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_ID_MEDICAMENTO + " INTEGER," +
                    COLUMN_ID_PACIENTE + " INTEGER," +
                    COLUMN_NOMBRE_MEDICAMENTO + " TEXT," +
                    COLUMN_DOSIS + " TEXT," +
                    COLUMN_FRECUENCIA + " TEXT," +
                    COLUMN_FECHA_INICIO + " TEXT," +
                    COLUMN_HORA_INICIO + " TEXT," +
                    COLUMN_FECHA_FIN + " TEXT," +
                    COLUMN_TIMESTAMP_PROXIMA + " INTEGER," +
                    COLUMN_ACTIVE + " INTEGER," +
                    COLUMN_INTERVALO + " INTEGER)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;
}