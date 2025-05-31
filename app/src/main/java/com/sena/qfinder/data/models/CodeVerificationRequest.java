package com.sena.qfinder.data.models;

import com.google.gson.annotations.SerializedName;

public class CodeVerificationRequest {
    @SerializedName("correo_usuario")
    private String correoUsuario;

    @SerializedName("codigo")
    private String codigo;

    public CodeVerificationRequest(String correoUsuario, String codigo) {
        this.correoUsuario = correoUsuario;
        this.codigo = codigo;
    }

    // Añade getters
    public String getCorreoUsuario() {
        return correoUsuario;
    }

    public String getCodigo() {
        return codigo;
    }
}