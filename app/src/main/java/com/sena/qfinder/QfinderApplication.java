package com.sena.qfinder;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

public class QfinderApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
                FirebaseDatabase.getInstance().setPersistenceEnabled(true); // Opcional: para caché offline
            }
        } catch (Exception e) {
            Log.e("QfinderApp", "Error inicializando Firebase", e);
        }
    }
}