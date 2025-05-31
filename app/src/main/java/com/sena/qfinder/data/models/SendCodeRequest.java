package com.sena.qfinder.data.models;

public class SendCodeRequest {
    private String correo_usuario;

    public SendCodeRequest(String correo_usuario) {
        this.correo_usuario = correo_usuario;
    }

    public String getCorreo_usuario() {
        return correo_usuario;
    }

    public void setCorreo_usuario(String correo_usuario) {
        this.correo_usuario = correo_usuario;
    }
}
