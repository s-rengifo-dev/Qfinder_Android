package com.sena.qfinder.utils;

public class AlarmaContract {
    public static final String TABLE_NAME = "alarmas";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITULO = "titulo";
    public static final String COLUMN_DESCRIPCION = "descripcion";
    public static final String COLUMN_FECHA = "fecha";
    public static final String COLUMN_HORA = "hora";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_ACTIVE = "active";

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_TITULO + " TEXT," +
                    COLUMN_DESCRIPCION + " TEXT," +
                    COLUMN_FECHA + " TEXT," +
                    COLUMN_HORA + " TEXT," +
                    COLUMN_TIMESTAMP + " INTEGER," +
                    COLUMN_ACTIVE + " INTEGER)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;
}