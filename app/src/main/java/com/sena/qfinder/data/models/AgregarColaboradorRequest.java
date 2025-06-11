package com.sena.qfinder.data.models;

public class AgregarColaboradorRequest {
    private int id_usuario;
    private int id_paciente;

    public AgregarColaboradorRequest(int id_usuario, int id_paciente) {
        this.id_usuario = id_usuario;
        this.id_paciente = id_paciente;
    }

    public int getId_usuario() {
        return id_usuario;
    }

    public int getId_paciente() {
        return id_paciente;
    }
}
