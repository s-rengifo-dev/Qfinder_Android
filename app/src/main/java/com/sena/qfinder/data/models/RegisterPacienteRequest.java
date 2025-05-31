package com.sena.qfinder.data.models;

import com.google.gson.annotations.SerializedName;

public class RegisterPacienteRequest {

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("apellido")
    private String apellido;

    @SerializedName("fecha_nacimiento")
    private String fechaNacimiento;


    private String sexo;

    @SerializedName("diagnostico_principal")
    private String diagnosticoPrincipal;

    @SerializedName("identificacion")
    private String identificacion;

    public RegisterPacienteRequest(String nombre, String apellido, String fechaNacimiento,
                                   String sexo, String diagnosticoPrincipal, String identificacion) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.fechaNacimiento = fechaNacimiento;
        this.sexo = sexo;
        this.diagnosticoPrincipal = diagnosticoPrincipal;
        this.identificacion = identificacion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public String getSexo() {
        return sexo;
    }

    public String getDiagnosticoPrincipal() {
        return diagnosticoPrincipal;
    }

    public String getIdentificacion() {
        return identificacion;
    }
}
