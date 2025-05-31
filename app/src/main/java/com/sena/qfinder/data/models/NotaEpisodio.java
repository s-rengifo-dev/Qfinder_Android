package com.sena.qfinder.data.models;

import com.google.gson.annotations.SerializedName;

public class NotaEpisodio {

    @SerializedName("idPaciente")
    private Integer idPaciente;

    @SerializedName("fechaHoraInicio")
    private String fechaHoraInicio;

    @SerializedName("fechaHoraFin")
    private String fechaHoraFin;

    @SerializedName("severidad")
    private String severidad;

    @SerializedName("descripcion")
    private String descripcion;

    @SerializedName("intervenciones")
    private String intervenciones;

    @SerializedName("registradoPor")
    private String registradoPor;

    public Integer getIdPaciente() {
        return idPaciente;
    }

    public void setIdPaciente(Integer idPaciente) {
        this.idPaciente = idPaciente;
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

    public String getSeveridad() {
        return severidad;
    }

    public void setSeveridad(String severidad) {
        this.severidad = severidad;
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

    public String getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(String registradoPor) {
        this.registradoPor = registradoPor;
    }
}
