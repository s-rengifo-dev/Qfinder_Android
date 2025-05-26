package com.sena.qfinder.models;

import com.google.gson.annotations.SerializedName;

public class NotaEpisodioResponse {

    // Indica si la operación fue exitosa
    @SerializedName("success")
    private boolean success;

    // Mensaje de respuesta del servidor
    @SerializedName("message")
    private String message;

    // Datos devueltos: el episodio guardado
    @SerializedName("data")
    private NotaEpisodio data;

    // --- Getters ---

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public NotaEpisodio getData() {
        return data;
    }

    // --- Setters (opcionalmente necesarios para deserialización) ---

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(NotaEpisodio data) {
        this.data = data;
    }
}
