// En tu paquete models, crea esta clase
package com.sena.qfinder.models;

public class PacienteResponse {
    private int id;
    private String nombre;
    private String apellido;
    private String identificacion;
    private String fecha_nacimiento;
    private String sexo;
    private String diagnostico_principal;
    private boolean es_cuidador_principal;
    private String parentesco;
    private String qrCode;

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }
    public String getFecha_nacimiento() { return fecha_nacimiento; }
    public void setFecha_nacimiento(String fecha_nacimiento) { this.fecha_nacimiento = fecha_nacimiento; }
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
    public String getDiagnostico_principal() { return diagnostico_principal; }
    public void setDiagnostico_principal(String diagnostico_principal) { this.diagnostico_principal = diagnostico_principal; }
    public boolean isEs_cuidador_principal() { return es_cuidador_principal; }
    public void setEs_cuidador_principal(boolean es_cuidador_principal) { this.es_cuidador_principal = es_cuidador_principal; }
    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
}