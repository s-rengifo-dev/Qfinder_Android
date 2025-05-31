package com.sena.qfinder.data.models;

public class VerificarCodigoRequest {
    private String correo;
    private String codigo;

    public VerificarCodigoRequest(String correo, String codigo) {
        this.correo = correo;
        this.codigo = codigo;
    }

    public String getCorreo() {
        return correo;
    }

    public String getCodigo() {
        return codigo;
    }
}
