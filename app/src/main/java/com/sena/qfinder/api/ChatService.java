package com.sena.qfinder.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.*;
import com.sena.qfinder.models.Mensaje;
import com.sena.qfinder.models.MensajeRequest;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatService {
    private static final String TAG = "ChatService";
    private DatabaseReference databaseRef;
    private String idRedActual;
    private ValueEventListener mensajesListener;
    private ChatCallback callback;
    private AuthService authService;
    private String authToken;
    private Context context;
    private Retrofit retrofit;

    public interface ChatCallback {
        void onMensajesRecibidos(List<Mensaje> mensajes);
        void onError(String error);
        void onMensajeEnviado();
        void onMembresiaVerificada(boolean esMiembro);
    }

    public ChatService(Context context, String idRed, String authToken) {
        this.context = context.getApplicationContext();
        this.idRedActual = idRed;
        this.authToken = authToken;

        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
            }
            this.databaseRef = FirebaseDatabase.getInstance().getReference();

            // Configurar Retrofit con timeouts extendidos
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            this.retrofit = new Retrofit.Builder()
                    .baseUrl("https://qfinder-production.up.railway.app/")
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            this.authService = retrofit.create(AuthService.class);
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando Firebase", e);
            if (callback != null) {
                callback.onError("Error inicializando chat");
            }
        }
    }

    public void verificarMembresia() {
        try {
            int redId = Integer.parseInt(idRedActual);
            Call<ResponseBody> call = authService.verificarMembresia("Bearer " + authToken, redId);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (callback != null) {
                        if (response.isSuccessful()) {
                            try {
                                String responseBody = response.body() != null ? response.body().string() : "";
                                boolean esMiembro = !responseBody.isEmpty(); // Ajusta según tu API
                                callback.onMembresiaVerificada(esMiembro);
                            } catch (Exception e) {
                                callback.onError("Error procesando respuesta");
                                Log.e(TAG, "Error procesando membresía", e);
                            }
                        } else {
                            callback.onMembresiaVerificada(false);
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    if (callback != null) {
                        callback.onError("Error de conexión: " + t.getMessage());
                    }
                    Log.e(TAG, "Error verificando membresía", t);
                }
            });
        } catch (NumberFormatException e) {
            if (callback != null) {
                callback.onError("ID de red inválido");
            }
            Log.e(TAG, "ID de red inválido", e);
        }
    }

    public void iniciarEscuchaMensajes() {
        if (mensajesListener != null) {
            detenerEscuchaMensajes();
        }

        try {
            DatabaseReference mensajesRef = databaseRef.child("chats")
                    .child(idRedActual)
                    .child("mensajes");

            mensajesListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Mensaje> nuevosMensajes = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            Mensaje mensaje = snapshot.getValue(Mensaje.class);
                            if (mensaje != null) {
                                mensaje.setId(snapshot.getKey());
                                nuevosMensajes.add(mensaje);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error procesando mensaje", e);
                        }
                    }
                    if (callback != null) {
                        callback.onMensajesRecibidos(nuevosMensajes);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    if (callback != null) {
                        callback.onError("Error de base de datos: " + databaseError.getMessage());
                    }
                    Log.e(TAG, "Error escuchando mensajes", databaseError.toException());
                }
            };

            mensajesRef.orderByChild("fecha_envio").addValueEventListener(mensajesListener);
        } catch (Exception e) {
            if (callback != null) {
                callback.onError("Error al iniciar escucha: " + e.getMessage());
            }
            Log.e(TAG, "Error iniciando escucha", e);
        }
    }

    public void detenerEscuchaMensajes() {
        if (mensajesListener != null) {
            try {
                databaseRef.child("chats").child(idRedActual).child("mensajes")
                        .removeEventListener(mensajesListener);
                mensajesListener = null;
            } catch (Exception e) {
                Log.e(TAG, "Error deteniendo escucha", e);
            }
        }
    }

    public void enviarMensaje(Mensaje mensaje) {
        enviarMensajeConReintentos(mensaje, 3); // 3 intentos máximo
    }

    private void enviarMensajeConReintentos(Mensaje mensaje, int intentosRestantes) {
        if (intentosRestantes <= 0) {
            if (callback != null) {
                callback.onError("No se pudo enviar el mensaje después de varios intentos");
            }
            return;
        }

        try {
            MensajeRequest request = new MensajeRequest(
                    mensaje.getContenido(),
                    mensaje.getIdUsuario(),
                    mensaje.getNombreUsuario()
            );

            Call<ResponseBody> call = authService.enviarMensaje(
                    "Bearer " + authToken,
                    Integer.parseInt(idRedActual),
                    request
            );

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        DatabaseReference nuevoMensajeRef = databaseRef.child("chats")
                                .child(idRedActual)
                                .child("mensajes")
                                .push();

                        mensaje.setId(nuevoMensajeRef.getKey());
                        mensaje.setFecha_envio(System.currentTimeMillis());

                        nuevoMensajeRef.setValue(mensaje)
                                .addOnSuccessListener(aVoid -> {
                                    if (callback != null) {
                                        callback.onMensajeEnviado();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (callback != null) {
                                        callback.onError("Error al guardar mensaje localmente: " + e.getMessage());
                                    }
                                    Log.e(TAG, "Error guardando mensaje en Firebase", e);
                                });
                    } else {
                        String errorMsg = "Error al enviar mensaje al backend: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                errorMsg += " - " + response.errorBody().string();
                            }
                        } catch (Exception e) {
                            errorMsg += " (error leyendo cuerpo)";
                        }
                        if (callback != null) {
                            callback.onError(errorMsg);
                        }
                        Log.e(TAG, errorMsg);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    String errorMsg;
                    if (t instanceof SocketTimeoutException) {
                        errorMsg = "Tiempo de espera agotado. Reintentando...";
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            enviarMensajeConReintentos(mensaje, intentosRestantes - 1);
                        }, 2000); // Reintentar después de 2 segundos
                    } else {
                        errorMsg = "Error de conexión: " + t.getMessage();
                        if (callback != null) {
                            callback.onError(errorMsg);
                        }
                    }
                    Log.e(TAG, errorMsg, t);

                    // Guardar mensaje localmente para intentar enviar más tarde
                    guardarMensajeLocalmente(mensaje);
                }
            });
        } catch (Exception e) {
            if (callback != null) {
                callback.onError("Error al enviar mensaje: " + e.getMessage());
            }
            Log.e(TAG, "Error enviando mensaje", e);
            guardarMensajeLocalmente(mensaje);
        }
    }

    private void guardarMensajeLocalmente(Mensaje mensaje) {
        try {
            DatabaseReference mensajesPendientesRef = databaseRef.child("mensajes_pendientes")
                    .child(idRedActual)
                    .push();

            mensaje.setId(mensajesPendientesRef.getKey());
            mensaje.setFecha_envio(System.currentTimeMillis());
            mensaje.setEstado("pendiente");

            mensajesPendientesRef.setValue(mensaje)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Mensaje guardado localmente para reenvío"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error guardando mensaje localmente", e));
        } catch (Exception e) {
            Log.e(TAG, "Error guardando mensaje localmente", e);
        }
    }
}