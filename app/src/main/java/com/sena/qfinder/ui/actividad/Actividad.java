package com.sena.qfinder.ui.actividad;

import java.io.Serializable;

public class Actividad implements Serializable {
    private String paciente;
    private String fecha;
    private String hora;
    private String descripcion;
    private String recordarAntes;
    private String repetirCada;

    public Actividad(String paciente, String fecha, String hora, String descripcion,
                     String recordarAntes, String repetirCada) {
        this.paciente = paciente;
        this.fecha = fecha;
        this.hora = hora;
        this.descripcion = descripcion;
        this.recordarAntes = recordarAntes;
        this.repetirCada = repetirCada;
    }

    // Getters y Setters
    public String getPaciente() { return paciente; }
    public void setPaciente(String paciente) { this.paciente = paciente; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getRecordarAntes() { return recordarAntes; }
    public void setRecordarAntes(String recordarAntes) { this.recordarAntes = recordarAntes; }

    public String getRepetirCada() { return repetirCada; }
    public void setRepetirCada(String repetirCada) { this.repetirCada = repetirCada; }
}