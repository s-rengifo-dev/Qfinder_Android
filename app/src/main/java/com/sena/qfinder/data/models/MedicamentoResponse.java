package com.sena.qfinder.data.models;

import com.google.gson.annotations.SerializedName;

public class MedicamentoResponse {

    @SerializedName("id_medicamento")  // Asegúrate de usar esto si el nombre del JSON no coincide con el nombre del campo
    private int id_medicamento;
    private String message;
    private String nombre;
    private String tipo;
    private String descripcion;

    public String getMessage() {
        return message;
    }

    public MedicamentoResponse(String nombre, String tipo, String descripcion) {
        //this.message = message;
        this.nombre = nombre;
        this.tipo = tipo;
        this.descripcion = descripcion;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getId_medicamento() {
        return id_medicamento;
    }

    public void setId_medicamento(int id_medicamento) {
        this.id_medicamento = id_medicamento;
    }

    @Override
    public String toString() {
        return "MedicamentoResponse{" +
                "message='" + message + '\'' +
                ", nombre='" + nombre + '\'' +
                ", tipo='" + tipo + '\'' +
                ", descripcion='" + descripcion + '\'' +
                '}';
    }
}