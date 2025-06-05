package com.sena.qfinder.data.models;

import com.google.gson.annotations.SerializedName;

public class NotaEpisodio {
    @SerializedName("id_episodio")
    private Integer idEpisodio;

    @SerializedName("id_paciente")
    private Integer idPaciente;

    @SerializedName("tipo")
    private String tipo;

    @SerializedName("fecha_hora_inicio")
    private String fechaHoraInicio;

    @SerializedName("fecha_hora_fin")
    private String fechaHoraFin;

    @SerializedName("titulo")
    private String titulo;

    @SerializedName("descripcion")
    private String descripcion;

    @SerializedName("intervenciones")
    private String intervenciones;

    @SerializedName("registrado_por")
    private Integer registradoPor;

    @SerializedName("registrado_por_role")
    private String registradoPorRole;

    @SerializedName("estado")
    private String estado;

    @SerializedName("origen")
    private String origen;

    @SerializedName("fuente_datos")
    private String fuenteDatos;

    // Getters y Setters actualizados
    public Integer getIdEpisodio() {
        return idEpisodio;
    }

    public void setIdEpisodio(Integer idEpisodio) {
        this.idEpisodio = idEpisodio;
    }

    public Integer getIdPaciente() {
        return idPaciente;
    }

    public void setIdPaciente(Integer idPaciente) {
        this.idPaciente = idPaciente;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(String fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public String getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(String fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getIntervenciones() {
        return intervenciones;
    }

    public void setIntervenciones(String intervenciones) {
        this.intervenciones = intervenciones;
    }

    public Integer getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(Integer registradoPor) {
        this.registradoPor = registradoPor;
    }

    public String getRegistradoPorRole() {
        return registradoPorRole;
    }

    public void setRegistradoPorRole(String registradoPorRole) {
        this.registradoPorRole = registradoPorRole;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getFuenteDatos() {
        return fuenteDatos;
    }

    public void setFuenteDatos(String fuenteDatos) {
        this.fuenteDatos = fuenteDatos;
    }
}