package com.sena.qfinder.models;

public class MensajeRequest {
    private String contenido;
    private String idUsuario;
    private String nombreUsuario;

    public MensajeRequest(String contenido, String idUsuario, String nombreUsuario) {
        this.contenido = contenido;
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
    }

    public String getContenido() {
        return contenido;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }
}