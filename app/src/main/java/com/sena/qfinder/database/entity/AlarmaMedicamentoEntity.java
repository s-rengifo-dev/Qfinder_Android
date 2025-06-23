package com.sena.qfinder.database.entity;

public class AlarmaMedicamentoEntity {
    private int id;
    private int idMedicamento;
    private int idPaciente;
    private String nombreMedicamento;
    private String dosis;
    private String frecuencia;
    private String fechaInicio;
    private String horaInicio;
    private String fechaFin;
    private long timestampProximaAlarma;
    private boolean active;
    private long intervaloMillis;

    // Constructor
    public AlarmaMedicamentoEntity(int id, int idMedicamento, int idPaciente,
                                   String nombreMedicamento, String dosis, String frecuencia,
                                   String fechaInicio, String horaInicio, String fechaFin,
                                   long timestampProximaAlarma, boolean active, long intervaloMillis) {
        this.id = id;
        this.idMedicamento = idMedicamento;
        this.idPaciente = idPaciente;
        this.nombreMedicamento = nombreMedicamento;
        this.dosis = dosis;
        this.frecuencia = frecuencia;
        this.fechaInicio = fechaInicio;
        this.horaInicio = horaInicio;
        this.fechaFin = fechaFin;
        this.timestampProximaAlarma = timestampProximaAlarma;
        this.active = active;
        this.intervaloMillis = intervaloMillis;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdMedicamento() {
        return idMedicamento;
    }

    public void setIdMedicamento(int idMedicamento) {
        this.idMedicamento = idMedicamento;
    }

    public int getIdPaciente() {
        return idPaciente;
    }

    public void setIdPaciente(int idPaciente) {
        this.idPaciente = idPaciente;
    }

    public String getNombreMedicamento() {
        return nombreMedicamento;
    }

    public void setNombreMedicamento(String nombreMedicamento) {
        this.nombreMedicamento = nombreMedicamento;
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

    public String getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(String fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(String horaInicio) {
        this.horaInicio = horaInicio;
    }

    public String getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(String fechaFin) {
        this.fechaFin = fechaFin;
    }

    public long getTimestampProximaAlarma() {
        return timestampProximaAlarma;
    }

    public void setTimestampProximaAlarma(long timestampProximaAlarma) {
        this.timestampProximaAlarma = timestampProximaAlarma;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getIntervaloMillis() {
        return intervaloMillis;
    }

    public void setIntervaloMillis(long intervaloMillis) {
        this.intervaloMillis = intervaloMillis;
    }
}