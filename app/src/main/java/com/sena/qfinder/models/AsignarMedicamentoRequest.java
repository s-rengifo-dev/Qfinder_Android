package com.sena.qfinder.models;

public class AsignarMedicamentoRequest {
    private int id_paciente;
    private int id_medicamento;
    private String fecha_inicio;
    private String fecha_fin;
    private String dosis;
    private String frecuencia;

    // Constructor
    public AsignarMedicamentoRequest(int id_paciente, int id_medicamento, String fecha_inicio,
                                     String fecha_fin, String dosis, String frecuencia) {
        this.id_paciente = id_paciente;
        this.id_medicamento = id_medicamento;
        this.fecha_inicio = fecha_inicio;
        this.fecha_fin = fecha_fin;
        this.dosis = dosis;
        this.frecuencia = frecuencia;
    }

    // Getters y Setters
    public int getId_paciente() {
        return id_paciente;
    }

    public void setId_paciente(int id_paciente) {
        this.id_paciente = id_paciente;
    }

    public int getId_medicamento() {
        return id_medicamento;
    }

    public void setId_medicamento(int id_medicamento) {
        this.id_medicamento = id_medicamento;
    }

    public String getFecha_inicio() {
        return fecha_inicio;
    }

    public void setFecha_inicio(String fecha_inicio) {
        this.fecha_inicio = fecha_inicio;
    }

    public String getFecha_fin() {
        return fecha_fin;
    }

    public void setFecha_fin(String fecha_fin) {
        this.fecha_fin = fecha_fin;
    }

    public String getDosis() {
        return dosis;
    }

    public void setDosis(String dosis) {
        this.dosis = dosis;
    }

    public String getFrecuencia() {
        return frecuencia;
    }

    public void setFrecuencia(String frecuencia) {
        this.frecuencia = frecuencia;
    }
}