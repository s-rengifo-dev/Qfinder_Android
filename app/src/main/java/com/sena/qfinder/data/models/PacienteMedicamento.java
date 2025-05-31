package com.sena.qfinder.data.models;

public class PacienteMedicamento {
    private int id_pac_medicamento;
    private int id_paciente;
    private int id_medicamento;
    private String fecha_inicio;
    private String fecha_fin;
    private String dosis;
    private String frecuencia;

    // Getters y Setters
    public int getId_pac_medicamento() {
        return id_pac_medicamento;
    }

    public void setId_pac_medicamento(int id_pac_medicamento) {
        this.id_pac_medicamento = id_pac_medicamento;
    }

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