package com.sena.qfinder.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sena.qfinder.api.ApiClient;
import com.sena.qfinder.api.AuthService;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActionReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationAction";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action != null) {
            switch (action) {
                case "MARK_MEDICATION_TAKEN":
                    handleMedicationTaken(context, intent);
                    break;
                // Agregar más acciones según sea necesario
            }
        }
    }

    private void handleMedicationTaken(Context context, Intent intent) {
        String medicamentoId = intent.getStringExtra("medicamentoId");
        if (medicamentoId == null) return;

        AuthService authService = ApiClient.getAuthService();
        Call<ResponseBody> call = authService.markMedicationAsTaken(
                ApiClient.getBearerToken(),
                medicamentoId
        );

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Medication marked as taken");
                } else {
                    Log.e(TAG, "Failed to mark medication as taken");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Error marking medication as taken", t);
            }
        });
    }
}