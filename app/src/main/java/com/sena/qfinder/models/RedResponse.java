package com.sena.qfinder.models;

public class RedResponse {
    private int id_red;
    private String nombre_red;
    private String descripcion_red;
    private boolean success;
    private String message;

    private boolean unido; // Nuevo campo para indicar si el usuario está unido

    // Getters y Setters
    public boolean isUnido() { return unido; }
    public void setUnido(boolean unido) { this.unido = unido; }
    public int getId_red() { return id_red; }
    public void setId_red(int id_red) { this.id_red = id_red; }
    public String getNombre_red() { return nombre_red; }
    public void setNombre_red(String nombre_red) { this.nombre_red = nombre_red; }
    public String getDescripcion_red() { return descripcion_red; }
    public void setDescripcion_red(String descripcion_red) { this.descripcion_red = descripcion_red; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}