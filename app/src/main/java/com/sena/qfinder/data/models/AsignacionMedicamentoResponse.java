package com.sena.qfinder.data.models;

import com.google.gson.annotations.SerializedName;

public class AsignacionMedicamentoResponse {
    @SerializedName("id_pac_medicamento")
    private int idAsignacion;

    @SerializedName("fecha_inicio")
    private String fechaInicio;

    @SerializedName("fecha_fin")
    private String fechaFin;

    @SerializedName("dosis")
    private String dosis;

    @SerializedName("frecuencia")
    private String frecuencia;

    @SerializedName("Paciente")
    private PacienteResponse paciente;

    @SerializedName("Medicamento")
    private MedicamentoResponse medicamento;

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

    public PacienteResponse getPaciente() {
        return paciente;
    }

    public MedicamentoResponse getMedicamento() {
        return medicamento;
    }
}