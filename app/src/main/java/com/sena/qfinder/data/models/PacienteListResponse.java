package com.sena.qfinder.data.models;

import java.util.List;

public class PacienteListResponse {
    private boolean success;
    private List<PacienteResponse> data;

    // Constructor vacío
    public PacienteListResponse() {
    }

    // Getters y Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<PacienteResponse> getData() {
        return data;
    }

    public void setData(List<PacienteResponse> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "PacienteListResponse{" +
                "success=" + success +
                ", data=" + data +
                '}';
    }
}