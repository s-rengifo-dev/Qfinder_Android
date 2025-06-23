package com.sena.qfinder.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.sena.qfinder.database.entity.AlarmaMedicamentoEntity;
import com.sena.qfinder.utils.AlarmaMedicamentoContract;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseMedicamentoHelper extends SQLiteOpenHelper {
    private static final String TAG = "DBMedicamentoHelper";
    private static final String DATABASE_NAME = "medicamentos_alarmas.db";
    private static final int DATABASE_VERSION = 2;
    private static DatabaseMedicamentoHelper instance;
    private SQLiteDatabase database;

    public static synchronized DatabaseMedicamentoHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseMedicamentoHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseMedicamentoHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(AlarmaMedicamentoContract.SQL_CREATE_ENTRIES);
            Log.d(TAG, "Tabla de alarmas de medicamentos creada");
        } catch (Exception e) {
            Log.e(TAG, "Error al crear la tabla", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL(AlarmaMedicamentoContract.SQL_DELETE_ENTRIES);
            onCreate(db);
            Log.d(TAG, "Base de datos actualizada de versión " + oldVersion + " a " + newVersion);
        } catch (Exception e) {
            Log.e(TAG, "Error al actualizar la base de datos", e);
        }
    }

    private synchronized SQLiteDatabase getDatabase() {
        if (database == null || !database.isOpen()) {
            database = getWritableDatabase();
        }
        return database;
    }

    public long guardarAlarmaMedicamento(AlarmaMedicamentoEntity alarma) {
        SQLiteDatabase db = getDatabase();
        long result = -1;

        try {
            db.beginTransaction();

            ContentValues values = new ContentValues();
            values.put(AlarmaMedicamentoContract.COLUMN_ID, alarma.getId());
            values.put(AlarmaMedicamentoContract.COLUMN_ID_MEDICAMENTO, alarma.getIdMedicamento());
            values.put(AlarmaMedicamentoContract.COLUMN_ID_PACIENTE, alarma.getIdPaciente());
            values.put(AlarmaMedicamentoContract.COLUMN_NOMBRE_MEDICAMENTO, alarma.getNombreMedicamento());
            values.put(AlarmaMedicamentoContract.COLUMN_DOSIS, alarma.getDosis());
            values.put(AlarmaMedicamentoContract.COLUMN_FRECUENCIA, alarma.getFrecuencia());
            values.put(AlarmaMedicamentoContract.COLUMN_FECHA_INICIO, alarma.getFechaInicio());
            values.put(AlarmaMedicamentoContract.COLUMN_HORA_INICIO, alarma.getHoraInicio());
            values.put(AlarmaMedicamentoContract.COLUMN_FECHA_FIN, alarma.getFechaFin());
            values.put(AlarmaMedicamentoContract.COLUMN_TIMESTAMP_PROXIMA, alarma.getTimestampProximaAlarma());
            values.put(AlarmaMedicamentoContract.COLUMN_ACTIVE, alarma.isActive() ? 1 : 0);
            values.put(AlarmaMedicamentoContract.COLUMN_INTERVALO, alarma.getIntervaloMillis());

            result = db.insertWithOnConflict(
                    AlarmaMedicamentoContract.TABLE_NAME,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
            );

            db.setTransactionSuccessful();
            Log.d(TAG, "Alarma guardada/actualizada. ID: " + alarma.getId() +
                    ", Próxima alarma: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new Date(alarma.getTimestampProximaAlarma())));
        } catch (Exception e) {
            Log.e(TAG, "Error al guardar alarma", e);
        } finally {
            if (db != null && db.inTransaction()) {
                db.endTransaction();
            }
        }
        return result;
    }

    public int actualizarProximaAlarma(int idAlarma, long nuevoTimestamp) {
        SQLiteDatabase db = getDatabase();
        int rowsUpdated = 0;

        try {
            ContentValues values = new ContentValues();
            values.put(AlarmaMedicamentoContract.COLUMN_TIMESTAMP_PROXIMA, nuevoTimestamp);

            rowsUpdated = db.update(
                    AlarmaMedicamentoContract.TABLE_NAME,
                    values,
                    AlarmaMedicamentoContract.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(idAlarma)}
            );

            Log.d(TAG, "Actualizada próxima alarma para ID: " + idAlarma +
                    ", filas afectadas: " + rowsUpdated);
        } catch (Exception e) {
            Log.e(TAG, "Error al actualizar alarma", e);
        }
        return rowsUpdated;
    }

    public void registrarTomaMedicamento(int idAlarma, long timestampToma) {
        Log.d(TAG, "Registrando toma de medicamento para alarma ID: " + idAlarma +
                " a las " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestampToma)));
    }

    public void cancelarAlarmaMedicamento(int idAlarma) {
        SQLiteDatabase db = getDatabase();

        try {
            int rowsDeleted = db.delete(
                    AlarmaMedicamentoContract.TABLE_NAME,
                    AlarmaMedicamentoContract.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(idAlarma)}
            );

            Log.d(TAG, "Alarma eliminada. ID: " + idAlarma +
                    ", filas afectadas: " + rowsDeleted);
        } catch (Exception e) {
            Log.e(TAG, "Error al eliminar alarma", e);
        }
    }

    public AlarmaMedicamentoEntity obtenerAlarmaPorId(int idAlarma) {
        SQLiteDatabase db = getDatabase();
        AlarmaMedicamentoEntity alarma = null;
        Cursor cursor = null;

        try {
            cursor = db.query(
                    AlarmaMedicamentoContract.TABLE_NAME,
                    null,
                    AlarmaMedicamentoContract.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(idAlarma)},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                alarma = new AlarmaMedicamentoEntity(
                        cursor.getInt(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_ID_MEDICAMENTO)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_ID_PACIENTE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_NOMBRE_MEDICAMENTO)),
                        cursor.getString(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_DOSIS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_FRECUENCIA)),
                        cursor.getString(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_FECHA_INICIO)),
                        cursor.getString(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_HORA_INICIO)),
                        cursor.getString(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_FECHA_FIN)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_TIMESTAMP_PROXIMA)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_ACTIVE)) == 1,
                        cursor.getLong(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_INTERVALO))
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al obtener alarma por ID", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return alarma;
    }

    public List<AlarmaMedicamentoEntity> obtenerTodasAlarmasActivas() {
        SQLiteDatabase db = getDatabase();
        List<AlarmaMedicamentoEntity> alarmas = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    AlarmaMedicamentoContract.TABLE_NAME,
                    null,
                    AlarmaMedicamentoContract.COLUMN_ACTIVE + " = 1",
                    null,
                    null,
                    null,
                    AlarmaMedicamentoContract.COLUMN_TIMESTAMP_PROXIMA + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    AlarmaMedicamentoEntity alarma = new AlarmaMedicamentoEntity(
                            cursor.getInt(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_ID)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_ID_MEDICAMENTO)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_ID_PACIENTE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_NOMBRE_MEDICAMENTO)),
                            cursor.getString(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_DOSIS)),
                            cursor.getString(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_FRECUENCIA)),
                            cursor.getString(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_FECHA_INICIO)),
                            cursor.getString(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_HORA_INICIO)),
                            cursor.getString(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_FECHA_FIN)),
                            cursor.getLong(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_TIMESTAMP_PROXIMA)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_ACTIVE)) == 1,
                            cursor.getLong(cursor.getColumnIndexOrThrow(AlarmaMedicamentoContract.COLUMN_INTERVALO))
                    );
                    alarmas.add(alarma);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al leer alarmas de medicamentos", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return alarmas;
    }

    public boolean tieneAlarma(int idAsignacion) {
        SQLiteDatabase db = getDatabase();
        Cursor cursor = null;
        boolean existe = false;

        try {
            cursor = db.query(
                    AlarmaMedicamentoContract.TABLE_NAME,
                    new String[]{AlarmaMedicamentoContract.COLUMN_ID},
                    AlarmaMedicamentoContract.COLUMN_ID + " = ? AND " +
                            AlarmaMedicamentoContract.COLUMN_ACTIVE + " = 1",
                    new String[]{String.valueOf(idAsignacion)},
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

    @Override
    public synchronized void close() {
        super.close();
        if (database != null && database.isOpen()) {
            database.close();
            database = null;
        }
    }
}