package com.sena.qfinder.api;

import com.sena.qfinder.models.ActividadGetResponse;
import com.sena.qfinder.models.ActividadListResponse;
import com.sena.qfinder.models.ActividadRequest;
import com.sena.qfinder.models.ActividadResponse;
import com.sena.qfinder.models.AsignacionMedicamentoResponse;
import com.sena.qfinder.models.AsignarMedicamentoRequest;
import com.sena.qfinder.models.AsignarMedicamentoResponse;
import com.sena.qfinder.models.CodeVerificationRequest;
import com.sena.qfinder.models.CodeVerificationResponse;
import com.sena.qfinder.models.MedicamentoRequest;
import com.sena.qfinder.models.MedicamentoResponse;
import com.sena.qfinder.models.MedicamentoSimpleResponse;
import com.sena.qfinder.models.MedicamentosResponse;
import com.sena.qfinder.models.Mensaje;
import com.sena.qfinder.models.MensajeRequest;
import com.sena.qfinder.models.PacienteListResponse;
import com.sena.qfinder.models.PacienteRequest;
import com.sena.qfinder.models.PacienteResponse;
import com.sena.qfinder.models.RedListResponse;
import com.sena.qfinder.models.RedRequest;
import com.sena.qfinder.models.RedResponse;
import com.sena.qfinder.models.RegisterPacienteRequest;
import com.sena.qfinder.models.RegisterPacienteResponse;
import com.sena.qfinder.models.LoginRequest;
import com.sena.qfinder.models.LoginResponse;
import com.sena.qfinder.models.PerfilUsuarioResponse;
import com.sena.qfinder.models.SendCodeRequest;
import com.sena.qfinder.models.SendCodeResponse;
import com.sena.qfinder.models.RegisterRequest;
import com.sena.qfinder.models.RegisterResponse;
import com.sena.qfinder.models.UsuarioRequest;
import com.sena.qfinder.models.VerificarCodigoRequest;
import com.sena.qfinder.models.CambiarPasswordRequest;

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

    @POST("api/auth/cambiar-password") // o "/api/auth/change-password" según tu backend
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
    // En tu AuthService, añade este método
    @GET("api/paciente/listarPacientes/{id_paciente}")
    Call<PacienteResponse> obtenerPacientePorId(
            @Header("Authorization") String token,
            @Path("id_paciente") int pacienteId
    );
    @GET("api/paciente/listarPacientes")
    Call<PacienteListResponse> listarPacientes(@Header("Authorization") String token);
    @POST("api/auth/logout")
    Call<Void> logout();

    @PUT("api/paciente/actualizarPaciente/{id_paciente}")
    Call<Void> actualizarPaciente(
            @Header("Authorization") String token,
            @Path("id_paciente") int pacienteId,
            @Body PacienteRequest pacienteRequest
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

    // En tu AuthService.java
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
    // En tu AuthService.java
    @GET("api/paciente-medicamento/asignaciones/{id_paciente}")
    Call<List<AsignacionMedicamentoResponse>> listarAsignacionesMedicamentos(
            @Header("Authorization") String token,
            @Path("id_paciente") int pacienteId
    );
    // Añadir estos métodos en tu AuthService
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
    // En AuthService.java (alternativa)
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

    @GET("api/membresiaRed/estaUnido/{id_red}")
    Call<ResponseBody> verificarMembresia(
            @Header("Authorization") String token,
            @Path("id_red") int idRed
    );
    @GET("api/membresiaRed/listarRedPertenece")
    Call<List<RedResponse>> listarRedesPertenecientes(
            @Header("Authorization") String token
    );
    // Métodos para el chat
    @GET("api/chat/{id_red}/mensajes")
    Call<List<Mensaje>> obtenerMensajes(
            @Header("Authorization") String token,
            @Path("id_red") int idRed,
            @Query("limite") int limite
    );

    @POST("api/chat/{id_red}/enviar")
    Call<ResponseBody> enviarMensaje(
            @Header("Authorization") String token,
            @Path("id_red") int idRed,
            @Body MensajeRequest mensaje
    );

    @GET("api/chat/obtenerIdRed")
    Call<RedResponse> obtenerIdRedPorNombre(
            @Header("Authorization") String token,
            @Query("nombre") String nombreRed
    );
}
