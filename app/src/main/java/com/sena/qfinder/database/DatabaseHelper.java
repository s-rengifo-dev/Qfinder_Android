package com.sena.qfinder.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sena.qfinder.database.entity.AlarmaEntity;
import com.sena.qfinder.utils.AlarmaContract;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "alarmas.db";
    private static final int DATABASE_VERSION = 2; // Incrementamos la versión
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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(AlarmaContract.SQL_DELETE_ENTRIES);
        onCreate(db);
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

        db.insertWithOnConflict(
                AlarmaContract.TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );
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
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                AlarmaEntity alarma = new AlarmaEntity(
                        cursor.getInt(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_TITULO)),
                        cursor.getString(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_DESCRIPCION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_FECHA)),
                        cursor.getString(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_HORA)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_TIMESTAMP)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(AlarmaContract.COLUMN_ACTIVE)) == 1
                );
                alarmas.add(alarma);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return alarmas;
    }

    public void eliminarAlarma(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(AlarmaContract.TABLE_NAME,
                AlarmaContract.COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    public boolean tieneAlarma(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                AlarmaContract.TABLE_NAME,
                new String[]{AlarmaContract.COLUMN_ID},
                AlarmaContract.COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null
        );

        boolean existe = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }
        return existe;
    }
}