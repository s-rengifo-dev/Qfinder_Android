package com.sena.qfinder.models;

import com.google.gson.annotations.SerializedName;

public class PacienteRequest {
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

    @SerializedName("imagen_paciente")
    private String imagenPerfil;

    public PacienteRequest(String nombre, String apellido, String fechaNacimiento,
                           String sexo, String diagnosticoPrincipal, String identificacion, String imagenPerfil) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.fechaNacimiento = fechaNacimiento;
        this.sexo = sexo;
        this.diagnosticoPrincipal = diagnosticoPrincipal;
        this.identificacion = identificacion;
        this.imagenPerfil = imagenPerfil;
    }

    // Setters
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public void setDiagnosticoPrincipal(String diagnosticoPrincipal) {
        this.diagnosticoPrincipal = diagnosticoPrincipal;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public void setImagenPerfil(String imagenPerfil) {
        this.imagenPerfil = imagenPerfil;
    }

}
