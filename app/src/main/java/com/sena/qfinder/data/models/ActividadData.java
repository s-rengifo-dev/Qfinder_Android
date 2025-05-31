package com.sena.qfinder.data.models;

import com.google.gson.annotations.SerializedName;

public class ActividadData {
    @SerializedName("id_actividad")
    private int idActividad;

    @SerializedName("id_paciente")
    private int idPaciente;

    // Otros campos que devuelva tu API

    public int getIdActividad() { return idActividad; }
    public int getIdPaciente() { return idPaciente; }
}