package com.sena.qfinder.data.models;

import com.google.gson.annotations.SerializedName;

public class ActividadRequest {
    @SerializedName("fecha_actividad")
    private String fechaActividad;

    @SerializedName("duracion")
    private int duracion;

    @SerializedName("tipo_actividad")
    private String tipoActividad;

    @SerializedName("intensidad")
    private String intensidad;

    @SerializedName("descripcion")
    private String descripcion;

    @SerializedName("estado")
    private String estado;

    @SerializedName("observaciones")
    private String observaciones;

    public ActividadRequest(String fechaActividad, int duracion, String tipoActividad,
                            String intensidad, String descripcion, String estado, String observaciones) {
        this.fechaActividad = fechaActividad;
        this.duracion = duracion;
        this.tipoActividad = tipoActividad;
        this.intensidad = intensidad;
        this.descripcion = descripcion;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    // Getters
    public String getFechaActividad() { return fechaActividad; }
    public int getDuracion() { return duracion; }
    public String getTipoActividad() { return tipoActividad; }
    public String getIntensidad() { return intensidad; }
    public String getDescripcion() { return descripcion; }
    public String getEstado() { return estado; }
    public String getObservaciones() { return observaciones; }
}