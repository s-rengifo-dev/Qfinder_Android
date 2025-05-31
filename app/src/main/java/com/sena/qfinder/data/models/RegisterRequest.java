package com.sena.qfinder.data.models;

public class RegisterRequest {
    private String nombre_usuario;
    private String apellido_usuario;
    private String identificacion_usuario;
    private String direccion_usuario;
    private String telefono_usuario;
    private String correo_usuario;
    private String contrasena_usuario;

    // Constructor
    public RegisterRequest(String nombre_usuario, String apellido_usuario, String identificacion_usuario,
                           String direccion_usuario, String telefono_usuario, String correo_usuario,
                           String contrasena_usuario) {
        this.nombre_usuario = nombre_usuario;
        this.apellido_usuario = apellido_usuario;
        this.identificacion_usuario = identificacion_usuario;
        this.direccion_usuario = direccion_usuario;
        this.telefono_usuario = telefono_usuario;
        this.correo_usuario = correo_usuario;
        this.contrasena_usuario = contrasena_usuario;
    }

    // Getters (necesarios para Retrofit)
    public String getNombre_usuario() {
        return nombre_usuario;
    }

    public String getApellido_usuario() {
        return apellido_usuario;
    }

    public String getIdentificacion_usuario() {
        return identificacion_usuario;
    }

    public String getDireccion_usuario() {
        return direccion_usuario;
    }

    public String getTelefono_usuario() {
        return telefono_usuario;
    }

    public String getCorreo_usuario() {
        return correo_usuario;
    }

    public String getContrasena_usuario() {
        return contrasena_usuario;
    }
}