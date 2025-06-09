package com.sena.qfinder.data.models;

import com.google.gson.annotations.SerializedName;

public class CitaMedica {

    @SerializedName("id_cita")
    private int id_cita;

    @SerializedName("id_paciente")
    private int id_paciente;

    @SerializedName("fecha_cita")
    private String fecha_cita; // Formato esperado: "yyyy-MM-dd"

    @SerializedName("hora_cita")
    private String hora_cita; // Formato esperado: "HH:mm:ss"

    @SerializedName("fecha_recordatorio")
    private String fecha_recordatorio; // Formato esperado: "yyyy-MM-dd HH:mm:ss"

    @SerializedName("titulo")
    private String titulo_cita;

    @SerializedName("descripcion")
    private String descripcion;

    @SerializedName("estado_cita")
    private String estado;

    @SerializedName("notificado_1h")
    private boolean notificado_1h;

    @SerializedName("notificado_24h")
    private boolean notificado_24h;

    // Constructor vacío
    public CitaMedica() {}

    // Constructor principal
    public CitaMedica(int id_paciente, String fecha_cita, String hora_cita, String titulo_cita, String descripcion,
                      String fecha_recordatorio, String estado) {
        this.id_paciente = id_paciente;
        this.fecha_cita = fecha_cita;
        this.hora_cita = hora_cita;
        this.titulo_cita = titulo_cita;
        this.descripcion = descripcion;
        this.fecha_recordatorio = fecha_recordatorio;
        this.estado = estado;
    }

    // Getters y Setters
    public int getIdCita() {
        return id_cita;
    }

    public void setIdCita(int id_cita) {
        this.id_cita = id_cita;
    }

    public int getIdPaciente() {
        return id_paciente;
    }

    public void setIdPaciente(int id_paciente) {
        this.id_paciente = id_paciente;
    }

    public String getFechaCita() {
        return fecha_cita;
    }

    public void setFechaCita(String fecha_cita) {
        this.fecha_cita = fecha_cita;
    }

    public String getHoraCita() {
        return hora_cita;
    }

    public void setHoraCita(String hora_cita) {
        this.hora_cita = hora_cita;
    }

    public String getFechaRecordatorio() {
        return fecha_recordatorio;
    }

    public void setFechaRecordatorio(String fecha_recordatorio) {
        this.fecha_recordatorio = fecha_recordatorio;
    }

    public String getTituloCita() {
        return titulo_cita;
    }

    public void setTituloCita(String titulo_cita) {
        this.titulo_cita = titulo_cita;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isNotificado1h() {
        return notificado_1h;
    }

    public void setNotificado1h(boolean notificado_1h) {
        this.notificado_1h = notificado_1h;
    }

    public boolean isNotificado24h() {
        return notificado_24h;
    }

    public void setNotificado24h(boolean notificado_24h) {
        this.notificado_24h = notificado_24h;
    }
}
