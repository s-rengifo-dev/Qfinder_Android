package com.sena.qfinder.data.models;

import com.google.gson.annotations.SerializedName;

public class ActividadResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private ActividadData data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public ActividadData getData() {
        return data;
    }

    // Setters (opcionales pero recomendados)
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(ActividadData data) {
        this.data = data;
    }
}