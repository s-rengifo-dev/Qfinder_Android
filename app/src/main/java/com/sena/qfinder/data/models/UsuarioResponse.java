package com.sena.qfinder.data.models;

import com.google.gson.annotations.SerializedName;

public class UsuarioResponse {

    @SerializedName("id_usuario")
    private int id;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("apellido")
    private String apellido;

    @SerializedName("correo")
    private String correo;

    public UsuarioResponse(int id, String nombre, String apellido, String correo) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
    }

    public int getId() {
        return id;
    }

    public String getNombre() { return nombre; }

    public String getApellido() {
        return apellido;
    }

    public String getCorreo() {
        return correo;
    }
}
