package com.sena.qfinder.data.models;

import com.google.gson.annotations.SerializedName;

public class NotaEpisodioRequest {
    @SerializedName("id_paciente")
    private int idPaciente;

    @SerializedName("fecha_hora_inicio")
    private String fechaHoraInicio;

    @SerializedName("fecha_hora_fin")
    private String fechaHoraFin;

    @SerializedName("tipo")
    private String tipo; // Cambiado de severidad a tipo

    @SerializedName("titulo")
    private String titulo; // Nuevo campo

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

    // Eliminado el campo 'severidad'
    public NotaEpisodioRequest(int idPaciente, String fechaHoraInicio, String fechaHoraFin,
                               String tipo, String titulo, String descripcion, String intervenciones,
                               int registradoPor, String registradoPorRole, String origen,
                               String fuenteDatos) {
        this.idPaciente = idPaciente;
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = fechaHoraFin;
        this.tipo = tipo;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.intervenciones = intervenciones;
        this.registradoPor = registradoPor;
        this.registradoPorRole = registradoPorRole;
        this.origen = origen;
        this.fuenteDatos = fuenteDatos;
    }

    // Getters...
    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}