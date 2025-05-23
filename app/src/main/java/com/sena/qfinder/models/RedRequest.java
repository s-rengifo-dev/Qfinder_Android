package com.sena.qfinder.models;

public class RedRequest {
    private String nombre_red;
    private String descripcion_red;

    public RedRequest(String nombre_red, String descripcion_red) {
        this.nombre_red = nombre_red;
        this.descripcion_red = descripcion_red;
    }

    // Getters
    public String getNombre_red() { return nombre_red; }
    public String getDescripcion_red() { return descripcion_red; }
}