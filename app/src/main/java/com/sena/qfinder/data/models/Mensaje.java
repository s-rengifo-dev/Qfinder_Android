package com.sena.qfinder.data.models;

public class Mensaje {
    private String id;
    private String nombreUsuario;
    private String contenido;
    private String hora;
    private String comunidad;
    private long fecha_envio;
    private String idUsuario;
    private String estado; // Nuevo campo: "enviado", "pendiente", "error"

    public Mensaje() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }
    public String getComunidad() { return comunidad; }
    public void setComunidad(String comunidad) { this.comunidad = comunidad; }
    public long getFecha_envio() { return fecha_envio; }
    public void setFecha_envio(long fecha_envio) { this.fecha_envio = fecha_envio; }
    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}