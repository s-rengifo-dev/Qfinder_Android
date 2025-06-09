package com.sena.qfinder.data.api;

import com.google.gson.JsonObject;
import com.sena.qfinder.data.models.ActividadListResponse;
import com.sena.qfinder.data.models.ActividadRequest;
import com.sena.qfinder.data.models.ActividadResponse;
import com.sena.qfinder.data.models.ApiResponse;
import com.sena.qfinder.data.models.AsignacionMedicamentoResponse;
import com.sena.qfinder.data.models.AsignarMedicamentoRequest;
import com.sena.qfinder.data.models.AsignarMedicamentoResponse;
import com.sena.qfinder.data.models.CheckoutProRequest;
import com.sena.qfinder.data.models.CheckoutProResponse;
import com.sena.qfinder.data.models.CitaMedica;
import com.sena.qfinder.data.models.CodeVerificationRequest;
import com.sena.qfinder.data.models.CodeVerificationResponse;
import com.sena.qfinder.data.models.FirebaseTokenResponse;
import com.sena.qfinder.data.models.MedicamentoRequest;
import com.sena.qfinder.data.models.MedicamentoResponse;
import com.sena.qfinder.data.models.MedicamentoSimpleResponse;
import com.sena.qfinder.data.models.MensajeRequest;
import com.sena.qfinder.data.models.MensajesResponse;
import com.sena.qfinder.data.models.NotaEpisodio;
import com.sena.qfinder.data.models.NotaEpisodioListResponse;
import com.sena.qfinder.data.models.NotaEpisodioRequest;
import com.sena.qfinder.data.models.NotaEpisodioResponse;
import com.sena.qfinder.data.models.PacienteListResponse;
import com.sena.qfinder.data.models.PacienteRequest;
import com.sena.qfinder.data.models.PacienteResponse;
import com.sena.qfinder.data.models.RedListResponse;
import com.sena.qfinder.data.models.RedRequest;
import com.sena.qfinder.data.models.RedResponse;
import com.sena.qfinder.data.models.RegisterPacienteRequest;
import com.sena.qfinder.data.models.RegisterPacienteResponse;
import com.sena.qfinder.data.models.LoginRequest;
import com.sena.qfinder.data.models.LoginResponse;
import com.sena.qfinder.data.models.PerfilUsuarioResponse;
import com.sena.qfinder.data.models.SendCodeRequest;
import com.sena.qfinder.data.models.SendCodeResponse;
import com.sena.qfinder.data.models.RegisterRequest;
import com.sena.qfinder.data.models.RegisterResponse;
import com.sena.qfinder.data.models.SubscriptionRequest;
import com.sena.qfinder.data.models.SubscriptionResponse;
import com.sena.qfinder.data.models.SubscriptionStatusResponse;
import com.sena.qfinder.data.models.UsuarioRequest;
import com.sena.qfinder.data.models.VerificarCodigoRequest;
import com.sena.qfinder.data.models.CambiarPasswordRequest;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AuthService {
    @POST("api/auth/register")
    Call<RegisterResponse> registerUser(@Body RegisterRequest request);

    @POST("api/auth/verify")
    Call<CodeVerificationResponse> verificarCodigo(@Body CodeVerificationRequest request);

    @POST("api/auth/login")
    Call<LoginResponse> LoginUser(@Body LoginRequest request);
    @POST("api/auth/recuperar")
    Call<SendCodeResponse> SendCode(@Body SendCodeRequest request);
    @POST("api/auth/verificar-codigo")
    Call<Void> verificarCodigo(@Body VerificarCodigoRequest request);

    @POST("api/auth/cambiar-password")
    Call<Void> cambiarPassword(
            @Header("Authorization") String authToken,
            @Body CambiarPasswordRequest request
    );
    @GET("api/auth/perfil")
    Call<PerfilUsuarioResponse> obtenerPerfil(@Header("Authorization") String token);
    @POST("api/paciente/register")
    Call<RegisterPacienteResponse> registerPaciente(
            @Header("Authorization") String token,
            @Body RegisterPacienteRequest request
    );
    @GET("api/paciente/listarPacientes/{id_paciente}")
    Call<PacienteResponse> obtenerPacientePorId(
            @Header("Authorization") String token,
            @Path("id_paciente") int pacienteId
    );

    @GET("api/paciente/listarPacientes")
    Call<PacienteListResponse> listarPacientes(@Header("Authorization") String token);
    @GET("api/paciente/listarPacientes")
    Call<PacienteListResponse> obtenerPacientes(@Header("Authorization") String token);
    @POST("api/auth/logout")
    Call<Void> logout();

    @PUT("api/paciente/actualizarPaciente/{id_paciente}")
    Call<Void> actualizarPaciente(
            @Header("Authorization") String token,
            @Path("id_paciente") int pacienteId,
            @Body PacienteRequest pacienteRequest
    );

    @PUT("api/auth/actualizarUser")
    Call<Void> actualizarUsuario(
            @Header("Authorization") String token,
            @Body UsuarioRequest request
    );
    // EPISODIOS DE SALUD
    @GET("api/episodios/episodioSalud/{id_paciente}")
    Call<NotaEpisodioListResponse> obtenerEpisodios(
            @Header("Authorization") String token,
            @Path("id_paciente") int pacienteId
    );

    @POST("api/episodios/episodioSalud/{id_paciente}")
    Call<NotaEpisodioResponse> crearEpisodio(
            @Header("Authorization") String token,
            @Path("id_paciente") int idPaciente,
            @Body NotaEpisodioRequest notaRequest
    );

    @PUT("api/episodios/pacientes/{id_paciente}/episodioSalud/{id_episodio}")
    Call<NotaEpisodio> actualizarEpisodio(
            @Header("Authorization") String token,
            @Path("id_paciente") int pacienteId,
            @Path("id_episodio") int idEpisodio,
            @Body NotaEpisodio episodio
    );

    @DELETE("api/episodios/eliminarEpis/{id_paciente}/{id_episodio}")
    Call<Void> eliminarEpisodio(
            @Header("Authorization") String token,
            @Path("id_paciente") int pacienteId,
            @Path("id_episodio") int idEpisodio
    );
    @POST("/api/medicamentos/crear")
    Call<MedicamentoResponse> agregarMedicamento(
            @Header("Authorization") String token,
            @Body MedicamentoRequest request
    );
    @GET("api/medicamentos/listar")
    Call<List<MedicamentoResponse>> listarMedicamentos(@Header("Authorization") String token);

    @DELETE("api/medicamentos/eliminar/{id}")
    Call<MedicamentoSimpleResponse> eliminarMedicamento(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    @POST("api/actividades/crearActivdad/{id_paciente}")
    Call<ActividadResponse> crearActividad(
            @Header("Authorization") String token,
            @Path("id_paciente") int idPaciente,
            @Body ActividadRequest request
    );

    @PUT("api/auth/actualizarUser")
    Call<ResponseBody> actualizarUsuario(@Body UsuarioRequest usuario, @Header("Authorization") String token);

    @GET("api/actividades/listarActividades/{id_paciente}")
    Call<ActividadListResponse> listarActividades(
            @Header("Authorization") String token,
            @Path("id_paciente") int pacienteId
    );
    @POST("api/paciente-medicamento/crear")
    Call<AsignarMedicamentoResponse> asignarMedicamento(
            @Header("Authorization") String token,
            @Body AsignarMedicamentoRequest request
    );
    @GET("api/paciente-medicamento/asignaciones/{id_paciente}")
    Call<List<AsignacionMedicamentoResponse>> listarAsignacionesMedicamentos(
            @Header("Authorization") String token,
            @Path("id_paciente") int pacienteId
    );

    @GET("api/redes/listarRedes")
    Call<RedListResponse> listarRedes(@Header("Authorization") String token);

    @POST("api/redes/crear")
    Call<RedResponse> crearRed(
            @Header("Authorization") String token,
            @Body RedRequest request
    );

    @PUT("api/redes/actualizar/{id}")
    Call<RedResponse> actualizarRed(
            @Header("Authorization") String token,
            @Path("id") int idRed,
            @Body RedRequest request
    );

    @DELETE("api/redes/eliminar/{id}")
    Call<Void> eliminarRed(
            @Header("Authorization") String token,
            @Path("id") int idRed
    );
    @POST("api/membresiaRed/unirseRed/{id_red}")
    Call<ResponseBody> unirseRed(
            @Header("Authorization") String token,
            @Path("id_red") int idRed
    );
    @DELETE("api/membresiaRed/salirRed/{id_red}")
    Call<ResponseBody> salirRed(
            @Header("Authorization") String token,
            @Path("id_red") int idRed
    );

    @GET("api/membresiaRed/verificarMembresia/{id_red}")
    Call<ResponseBody> verificarMembresia(
            @Header("Authorization") String token,
            @Path("id_red") int idRed
    );
    @GET("api/membresiaRed/listarRedPertenece")
    Call<List<RedResponse>> listarRedesPertenecientes(
            @Header("Authorization") String token
    );
    @GET("api/chat/{id_red}/mensajes")
    Call<ApiResponse<MensajesResponse>> obtenerMensajes(
            @Header("Authorization") String token,
            @Path("id_red") int idRed,
            @Query("limite") int limite
    );
        @POST("api/firebase/token/{id_red}")
        Call<FirebaseTokenResponse> getFirebaseToken(
                @Header("Authorization") String authHeader,
                @Path("id_red") int redId
        );

    @POST("api/chat/{id_red}/enviar")
    Call<ResponseBody> enviarMensaje(
            @Header("Authorization") String token,
            @Path("id_red") int idRed,
            @Body MensajeRequest mensaje
    );

    @GET("api/redes/obtenerIdRed")
    Call<RedResponse> obtenerIdRedPorNombre(
            @Header("Authorization") String token,
            @Query("nombre") String nombreRed
    );

    @POST("api/citaMedica/crearCita/{id_paciente}")
    Call<CitaMedica> crearCitaMedica(
            @Header("Authorization") String token,
            @Path("id_paciente") int idPaciente,
            @Body CitaMedica cita
    );

    @GET("api/citaMedica/listarCitas/{id_paciente}")
    Call<List<CitaMedica>> listarCitasMedicas(
            @Header("Authorization") String token,
            @Path("id_paciente") int idPaciente
    );

    @GET("listarCitasId/{id_paciente}/{id_cita}")
    Call<List<CitaMedica>> obtenerCitaPorId(
            @Header("Authorization") String token,
            @Path("id_paciente") int idPaciente,
            @Path("id_cita") int idCita
    );

    @PUT("actualizarCita/{id_paciente}/{id_cita}")
    Call<Void> actualizarCita(
            @Header("Authorization") String token,
            @Path("id_paciente") int idPaciente,
            @Path("id_cita") int idCita,
            @Body CitaMedica cita
    );

    @DELETE("eliminarCita/{id_paciente}/{id_cita}")
    Call<Void> eliminarCita(
            @Header("Authorization") String token,
            @Path("id_paciente") int idPaciente,
            @Path("id_cita") int idCita
    );
    // En tu interfaz AuthService
    @POST("api/firebase/register-fcm")
    Call<ResponseBody> registerFcmToken(
            @Header("Authorization") String token,
            @Body JsonObject fcmTokenData
    );
    @POST("api/medicamentos/marcar-tomado/{id}")
    Call<ResponseBody> markMedicationAsTaken(
            @Header("Authorization") String token,
            @Path("id") String medicamentoId
    );
    // Crear suscripción
    @POST("api/payments/subscriptions")
    Call<SubscriptionResponse> createSubscription(
            @Header("Authorization") String token,
            @Body SubscriptionRequest request
    );

    // Obtener estado de suscripción

        @POST("api/payments/checkout-pro")
        Call<CheckoutProResponse> createCheckoutProPreference(
                @Header("Authorization") String authToken,
                @Body CheckoutProRequest request
        );
}