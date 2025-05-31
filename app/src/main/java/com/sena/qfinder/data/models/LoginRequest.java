package com.sena.qfinder.data.models;

public class LoginRequest {
    private String correo_usuario;
    private String contrasena_usuario;

    //Constructor
    public LoginRequest(String correo_usuario, String contrasena_usuario){
        this.correo_usuario = correo_usuario;
        this.contrasena_usuario = contrasena_usuario;
    }

    public String getCorreo_usuario() {
        return correo_usuario;
    }

    public void setCorreo_usuario(String correo_usuario) {
        this.correo_usuario = correo_usuario;
    }

    public String getContrasena_usuario() {
        return contrasena_usuario;
    }

    public void setContrasena_usuario(String contrasena_usuario) {
        this.contrasena_usuario = contrasena_usuario;
    }
}

