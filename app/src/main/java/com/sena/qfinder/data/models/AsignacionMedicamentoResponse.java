package com.sena.qfinder.data.models;

import com.google.gson.annotations.SerializedName;

public class AsignacionMedicamentoResponse {
    @SerializedName("idAsignacion")
    private int idAsignacion;

    @SerializedName("fechaInicio")
    private String fechaInicio;

    @SerializedName("fechaFin")
    private String fechaFin;

    @SerializedName("dosis")
    private String dosis;

    @SerializedName("frecuencia")
    private String frecuencia;

    @SerializedName("medicamento")
    private MedicamentoResponse medicamento;

    @SerializedName("paciente")
    private PacienteResponse paciente;

    // Getters y Setters
    public int getIdAsignacion() {
        return idAsignacion;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public String getFechaFin() {
        return fechaFin;
    }

    public String getDosis() {
        return dosis;
    }

    public String getFrecuencia() {
        return frecuencia;
    }

    public MedicamentoResponse getMedicamento() {
        return medicamento;
    }

    public PacienteResponse getPaciente() {
        return paciente;
    }

    // Setters (opcionales)
    public void setIdAsignacion(int idAsignacion) {
        this.idAsignacion = idAsignacion;
    }

    // ... otros setters si los necesitas
}