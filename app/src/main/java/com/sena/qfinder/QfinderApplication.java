package com.sena.qfinder;

import android.app.Application;
import android.util.Log;

import com.sena.qfinder.utils.AppLifecycleHandler;
import com.sena.qfinder.utils.FirebaseInitializer;

public class QfinderApplication extends Application {
    private static final String TAG = "QFinderApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Inicializar Firebase
        FirebaseInitializer.initialize(this);

        // Registrar el lifecycle handler
        registerActivityLifecycleCallbacks(new AppLifecycleHandler());

        Log.d(TAG, "Application initialized");
    }
}