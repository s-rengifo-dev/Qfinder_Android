package com.sena.qfinder.data.models;

public class CambiarPasswordRequest {
    private String correo;
    private String nuevaContrasena;

    public CambiarPasswordRequest(String correo, String nuevaContrasena) {
        this.correo = correo;
        this.nuevaContrasena = nuevaContrasena;
    }

    // Getters y setters
    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getNuevaContrasena() {
        return nuevaContrasena;
    }

    public void setNuevaContrasena(String nuevaContrasena) {
        this.nuevaContrasena = nuevaContrasena;
    }
}