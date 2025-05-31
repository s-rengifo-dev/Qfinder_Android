package com.sena.qfinder.data.models;

public class PerfilUsuarioResponse {
    private String id_usuario;
    private String nombre_usuario;
    private String apellido_usuario;
    private String telefono_usuario;
    private String correo_usuario;
    private String direccion_usuario;
    private String identificacion_usuario;

    // Constructor vacío (importante para Retrofit)
    public PerfilUsuarioResponse() {
    }

    // Getters
    public String getNombre_usuario() {
        return nombre_usuario;
    }

    public String getApellido_usuario() {
        return apellido_usuario;
    }

    public String getTelefono_usuario() {
        return telefono_usuario;
    }

    public String getCorreo_usuario() {
        return correo_usuario;
    }

    public String getDireccion_usuario() {
        return direccion_usuario;
    }

    public String getIdentificacion_usuario() {
        return identificacion_usuario;
    }
    public String getId_usuario() {
        return id_usuario;
    }
}
