package com.sena.qfinder.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.sena.qfinder.database.entity.AlarmaEntity;
import com.sena.qfinder.utils.AlarmaContract;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "alarmas.db";
    private static final int DATABASE_VERSION = 3; // Incrementado por las nuevas columnas
    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(AlarmaContract.SQL_CREATE_ENTRIES);
        Log.d(TAG, "Base de datos creada con versión: " + DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Migración para mantener datos existentes
        if (oldVersion < 3) {
            Log.d(TAG, "Actualizando base de datos de versión " + oldVersion + " a " + newVersion);

            // 1. Crear tabla temporal con la estructura antigua
            db.execSQL("ALTER TABLE " + AlarmaContract.TABLE_NAME + " RENAME TO " + AlarmaContract.TABLE_NAME + "_old;");

            // 2. Crear nueva tabla con estructura actualizada
            db.execSQL(AlarmaContract.SQL_CREATE_ENTRIES);

            // 3. Copiar datos existentes
            String[] columns = {
                    AlarmaContract.COLUMN_ID,
                    AlarmaContract.COLUMN_TITULO,
                    AlarmaContract.COLUMN_DESCRIPCION,
                    AlarmaContract.COLUMN_FECHA,
                    AlarmaContract.COLUMN_HORA,
                    AlarmaContract.COLUMN_TIMESTAMP,
                    AlarmaContract.COLUMN_ACTIVE
            };

            db.execSQL("INSERT INTO " + AlarmaContract.TABLE_NAME + " (" +
                    String.join(", ", columns) + ") " +
                    "SELECT " + String.join(", ", columns) +
                    " FROM " + AlarmaContract.TABLE_NAME + "_old;");

            // 4. Eliminar tabla temporal
            db.execSQL("DROP TABLE " + AlarmaContract.TABLE_NAME + "_old;");

            Log.d(TAG, "Migración completada exitosamente");
        } else {
            // Si no hay migración necesaria, simplemente recrear la tabla
            db.execSQL(AlarmaContract.SQL_DELETE_ENTRIES);
            onCreate(db);
        }
    }

    public void guardarAlarma(AlarmaEntity alarma) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(AlarmaContract.COLUMN_ID, alarma.getId());
        values.put(AlarmaContract.COLUMN_TITULO, alarma.getTitulo());
        values.put(AlarmaContract.COLUMN_DESCRIPCION, alarma.getDescripcion());
        values.put(AlarmaContract.COLUMN_FECHA, alarma.getFecha());
        values.put(AlarmaContract.COLUMN_HORA, alarma.getHora());
        values.put(AlarmaContract.COLUMN_TIMESTAMP, alarma.getTimestamp());
        values.put(AlarmaContract.COLUMN_ACTIVE, alarma.isActive() ? 1 : 0);

        // Nuevos campos para alarmas recurrentes
        values.put(AlarmaContract.COLUMN_RECURRENTE, alarma.isEsRecurrente() ? 1 : 0);
        values.put(AlarmaContract.COLUMN_INTERVALO, alarma.getIntervaloMillis());

        long result = db.insertWithOnConflict(
                AlarmaContract.TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );

        if (result == -1) {
            Log.e(TAG, "Error al guardar alarma con ID: " + alarma.getId());
        } else {
            Log.d(TAG, "Alarma guardada exitosamente con ID: " + alarma.getId());
        }
    }

    public List<AlarmaEntity> obtenerTodasAlarmas() {
        List<AlarmaEntity> alarmas = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                AlarmaContract.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                AlarmaContract.COLUMN_TIMESTAMP + " ASC" // Ordenar por timestamp
        );

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        AlarmaEntity alarma = new AlarmaEntity(
                                cursor.getInt(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_ID)),
                                cursor.getString(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_TITULO)),
                                cursor.getString(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_DESCRIPCION)),
                                cursor.getString(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_FECHA)),
                                cursor.getString(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_HORA)),
                                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_TIMESTAMP)),
                                cursor.getInt(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_ACTIVE)) == 1,
                                cursor.getInt(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_RECURRENTE)) == 1,
                                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_INTERVALO))
                        );
                        alarmas.add(alarma);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al leer alarmas de la base de datos", e);
            } finally {
                cursor.close();
            }
        }

        Log.d(TAG, "Alarmas recuperadas: " + alarmas.size());
        return alarmas;
    }

    public void eliminarAlarma(int id) {
        SQLiteDatabase db = getWritableDatabase();
        int rowsDeleted = db.delete(AlarmaContract.TABLE_NAME,
                AlarmaContract.COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});

        if (rowsDeleted > 0) {
            Log.d(TAG, "Alarma eliminada con ID: " + id);
        } else {
            Log.d(TAG, "No se encontró alarma con ID: " + id + " para eliminar");
        }
    }

    public boolean tieneAlarma(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        boolean existe = false;

        try {
            cursor = db.query(
                    AlarmaContract.TABLE_NAME,
                    new String[]{AlarmaContract.COLUMN_ID},
                    AlarmaContract.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(id)},
                    null, null, null
            );

            existe = cursor != null && cursor.getCount() > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error al verificar existencia de alarma", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return existe;
    }

    public AlarmaEntity obtenerAlarma(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        AlarmaEntity alarma = null;

        try {
            cursor = db.query(
                    AlarmaContract.TABLE_NAME,
                    null,
                    AlarmaContract.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(id)},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                alarma = new AlarmaEntity(
                        cursor.getInt(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_TITULO)),
                        cursor.getString(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_DESCRIPCION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_FECHA)),
                        cursor.getString(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_HORA)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_TIMESTAMP)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_ACTIVE)) == 1,
                        cursor.getInt(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_RECURRENTE)) == 1,
                        cursor.getLong(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_INTERVALO))
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al obtener alarma con ID: " + id, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return alarma;
    }

    public void actualizarEstadoAlarma(int id, boolean activa) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(AlarmaContract.COLUMN_ACTIVE, activa ? 1 : 0);

        int rowsUpdated = db.update(
                AlarmaContract.TABLE_NAME,
                values,
                AlarmaContract.COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)}
        );

        if (rowsUpdated > 0) {
            Log.d(TAG, "Estado de alarma actualizado para ID: " + id + " a " + (activa ? "activa" : "inactiva"));
        } else {
            Log.d(TAG, "No se pudo actualizar estado para alarma con ID: " + id);
        }
    }
}