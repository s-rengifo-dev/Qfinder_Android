package com.sena.qfinder.database.entity;

public class AlarmaEntity {
    private int id;
    private String titulo;
    private String descripcion;
    private String fecha;
    private String hora;
    private long timestamp;
    private boolean active;

    public AlarmaEntity(int id, String titulo, String descripcion,
                        String fecha, String hora, long timestamp, boolean active) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.hora = hora;
        this.timestamp = timestamp;
        this.active = active;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}