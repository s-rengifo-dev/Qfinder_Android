package com.sena.qfinder.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.sena.qfinder.models.Mensaje;
import com.sena.qfinder.models.MensajeRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private DatabaseReference mensajesPendientesRef;
    private DatabaseReference connectedRef;
    private String idRedActual;
    private ValueEventListener mensajesListener;
    private ValueEventListener connectionListener;
    private ChatCallback callback;
    private AuthService authService;
    private String authToken;
    private Context context;
    private Retrofit retrofit;
    private FirebaseAuth mAuth;

    public interface ChatCallback {
        void onMensajesRecibidos(List<Mensaje> mensajes);
        void onError(String error);
        void onMensajeEnviado();
        void onMembresiaVerificada(boolean esMiembro);
        void onFirebaseConnected(boolean connected);
        void onMensajesCargados(List<Mensaje> mensajes);
        void onFirebaseAuthSuccess();
        void onFirebaseAuthFailed(String error);
    }

    public ChatService(Context context, String idRed, String authToken) {
        this.context = context.getApplicationContext();
        this.idRedActual = idRed;
        this.authToken = authToken;
        this.mAuth = FirebaseAuth.getInstance();
        initializeServices();
    }

    private void initializeServices() {
        try {
            initializeFirebase();
            initializeRetrofit();
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando servicios", e);
            notifyError("Error inicializando chat: " + e.getMessage());
        }
    }

    private void initializeFirebase() {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:943234700783:android:63a905964f25737428521a")
                    .setApiKey("AIzaSyDWsifL9DrQkUSqSmaVstQ7Cr9dhAPoPZg")
                    .setDatabaseUrl("https://qfinder-comunity-default-rtdb.firebaseio.com/")
                    .setProjectId("qfinder-community")
                    .build();
            FirebaseApp.initializeApp(context, options);
        }
        this.databaseRef = FirebaseDatabase.getInstance().getReference("chats");
        this.mensajesPendientesRef = FirebaseDatabase.getInstance().getReference("mensajes_pendientes");
        this.connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
    }

    private void initializeRetrofit() {
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
    }

    public void authenticateWithFirebase(String token) {
        mAuth.signInWithCustomToken(token)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) callback.onFirebaseAuthSuccess();
                    } else {
                        notifyError("Error de autenticación con Firebase");
                    }
                });
    }

    public void setCallback(ChatCallback callback) {
        this.callback = callback;
    }

    public void cargarMensajesIniciales() {
        if (!validateRedId()) return;

        try {
            int redId = Integer.parseInt(idRedActual);
            Call<List<Mensaje>> call = authService.obtenerMensajes("Bearer " + authToken, redId, 50);

            call.enqueue(new Callback<List<Mensaje>>() {
                @Override
                public void onResponse(Call<List<Mensaje>> call, Response<List<Mensaje>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        notifyMensajesCargados(response.body());
                    }
                }

                @Override
                public void onFailure(Call<List<Mensaje>> call, Throwable t) {
                    Log.e(TAG, "Error al cargar mensajes", t);
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "ID de red inválido", e);
        }
    }

    public void verificarMembresia(int maxReintentos) {
        if (!validateRedId()) return;

        try {
            int redId = Integer.parseInt(idRedActual);
            Call<ResponseBody> call = authService.verificarMembresia("Bearer " + authToken, redId);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    boolean esMiembro = response.isSuccessful() && response.code() == 200;
                    notifyMembresiaVerificada(esMiembro);
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e(TAG, "Error verificando membresía", t);
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "ID de red inválido", e);
        }
    }

    public void iniciarEscuchaMensajes() {
        if (!validateRedId() || mAuth.getCurrentUser() == null) {
            notifyError("ID de comunidad o usuario no válido");
            return;
        }

        if (mensajesListener != null) {
            detenerEscuchaMensajes();
        }

        DatabaseReference mensajesRef = databaseRef.child(idRedActual).child("mensajes");
        mensajesListener = mensajesRef.orderByChild("fecha_envio").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Mensaje> nuevosMensajes = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Mensaje mensaje = snapshot.getValue(Mensaje.class);
                        if (mensaje != null) {
                            mensaje.setId(snapshot.getKey());
                            // Asegurar campos faltantes
                            if (mensaje.getHora() == null) {
                                mensaje.setHora(convertirTimestampAHora(mensaje.getFecha_envio()));
                            }
                            nuevosMensajes.add(mensaje);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error procesando mensaje", e);
                    }
                }
                notifyMensajesRecibidos(nuevosMensajes);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error en base de datos: " + databaseError.getMessage());
            }
        });
    }

    private String convertirTimestampAHora(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public void enviarMensaje(Mensaje mensaje) {
        if (mensaje == null) return;

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
                    guardarMensajeEnFirebase(mensaje);
                } else {
                    guardarMensajePendiente(mensaje);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                guardarMensajePendiente(mensaje);
            }
        });
    }

    private void guardarMensajeEnFirebase(Mensaje mensaje) {
        DatabaseReference nuevoMensajeRef = databaseRef.child(idRedActual).child("mensajes").push();

        Map<String, Object> mensajeMap = new HashMap<>();
        mensajeMap.put("id", nuevoMensajeRef.getKey());
        mensajeMap.put("nombreUsuario", mensaje.getNombreUsuario());
        mensajeMap.put("contenido", mensaje.getContenido());
        mensajeMap.put("hora", mensaje.getHora());
        mensajeMap.put("comunidad", mensaje.getComunidad());
        mensajeMap.put("idUsuario", mensaje.getIdUsuario());
        mensajeMap.put("fecha_envio", mensaje.getFecha_envio());
        mensajeMap.put("estado", "enviado");

        nuevoMensajeRef.setValue(mensajeMap)
                .addOnSuccessListener(aVoid -> notifyMensajeEnviado())
                .addOnFailureListener(e -> guardarMensajePendiente(mensaje));
    }

    private void guardarMensajePendiente(Mensaje mensaje) {
        DatabaseReference mensajePendienteRef = mensajesPendientesRef.child(idRedActual).push();

        Map<String, Object> mensajeMap = new HashMap<>();
        mensajeMap.put("id", mensajePendienteRef.getKey());
        mensajeMap.put("nombreUsuario", mensaje.getNombreUsuario());
        mensajeMap.put("contenido", mensaje.getContenido());
        mensajeMap.put("hora", mensaje.getHora());
        mensajeMap.put("comunidad", mensaje.getComunidad());
        mensajeMap.put("idUsuario", mensaje.getIdUsuario());
        mensajeMap.put("fecha_envio", mensaje.getFecha_envio());
        mensajeMap.put("estado", "pendiente");

        mensajePendienteRef.setValue(mensajeMap);
    }

    public void detenerEscuchaMensajes() {
        if (mensajesListener != null) {
            databaseRef.child(idRedActual).child("mensajes").removeEventListener(mensajesListener);
        }
    }

    public void cleanup() {
        detenerEscuchaMensajes();
        if (connectionListener != null) {
            connectedRef.removeEventListener(connectionListener);
        }
    }

    private boolean validateRedId() {
        return idRedActual != null && !idRedActual.isEmpty();
    }


    private void notifyMensajesRecibidos(List<Mensaje> mensajes) {
        if (callback != null) callback.onMensajesRecibidos(mensajes);
    }

    private void notifyError(String error) {
        if (callback != null) callback.onError(error);
    }

    private void notifyMensajeEnviado() {
        if (callback != null) callback.onMensajeEnviado();
    }

    private void notifyMembresiaVerificada(boolean esMiembro) {
        if (callback != null) callback.onMembresiaVerificada(esMiembro);
    }

    private void notifyMensajesCargados(List<Mensaje> mensajes) {
        if (callback != null) callback.onMensajesCargados(mensajes);
    }
}