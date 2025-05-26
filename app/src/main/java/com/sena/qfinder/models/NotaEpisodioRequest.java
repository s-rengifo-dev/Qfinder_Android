package com.sena.qfinder.models;

import com.google.gson.annotations.SerializedName;

public class NotaEpisodioRequest {

    @SerializedName("id_paciente")
    private int idPaciente;

    @SerializedName("fecha_hora_inicio")
    private String fechaHoraInicio;

    @SerializedName("fecha_hora_fin")
    private String fechaHoraFin;

    @SerializedName("severidad")
    private String severidad;

    @SerializedName("descripcion")
    private String descripcion;

    @SerializedName("intervenciones")
    private String intervenciones;

    @SerializedName("registrado_por")
    private int registradoPor;

    @SerializedName("registrado_por_role")
    private String registradoPorRole;

    @SerializedName("origen")
    private String origen;

    @SerializedName("fuente_datos")
    private String fuenteDatos;

    @SerializedName("tipo")
    private String tipo;

    public NotaEpisodioRequest(int idPaciente, String fechaHoraInicio, String fechaHoraFin,
                               String severidad, String descripcion, String intervenciones,
                               int registradoPor, String registradoPorRole, String origen,
                               String fuenteDatos, String tipo) {
        this.idPaciente = idPaciente;
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = fechaHoraFin;
        this.severidad = severidad;
        this.descripcion = descripcion;
        this.intervenciones = intervenciones;
        this.registradoPor = registradoPor;
        this.registradoPorRole = registradoPorRole;
        this.origen = origen;
        this.fuenteDatos = fuenteDatos;
        this.tipo = tipo;
    }

    // Getters opcionales aquí...
}
