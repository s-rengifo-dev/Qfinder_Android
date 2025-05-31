package com.sena.qfinder.data.models;

import java.io.Serializable;
public class UsuarioRequest implements Serializable {

    private String nombre_usuario;
    private String apellido_usuario;
    private String direccion_usuario;
    private String telefono_usuario;
    private String correo_usuario;

    // Constructor
    public UsuarioRequest(String nombre_usuario, String apellido_usuario, String direccion_usuario, String telefono_usuario, String correo_usuario) {
        this.nombre_usuario = nombre_usuario;
        this.apellido_usuario = apellido_usuario;
        this.direccion_usuario = direccion_usuario;
        this.telefono_usuario = telefono_usuario;
        this.correo_usuario = correo_usuario;
    }

    // Getters
    public String getNombre_usuario() {
        return nombre_usuario;
    }

    public String getApellido_usuario() {
        return apellido_usuario;
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

    // Setters
    public void setNombre_usuario(String nombre_usuario) {
        this.nombre_usuario = nombre_usuario;
    }

    public void setApellido_usuario(String apellido_usuario) {
        this.apellido_usuario = apellido_usuario;
    }

    public void setDireccion_usuario(String direccion_usuario) {
        this.direccion_usuario = direccion_usuario;
    }

    public void setTelefono_usuario(String telefono_usuario) {
        this.telefono_usuario = telefono_usuario;
    }

    public void setCorreo_usuario(String correo_usuario) {
        this.correo_usuario = correo_usuario;
    }
}



