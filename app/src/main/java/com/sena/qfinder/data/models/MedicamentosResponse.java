package com.sena.qfinder.data.models;

import java.util.List;

public class MedicamentosResponse {
    private boolean success;
    private List<MedicamentoResponse> data;

    public MedicamentosResponse(){

    }

    // Getters y Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<MedicamentoResponse> getData() {
        return data;
    }

    public void setData(List<MedicamentoResponse> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "MedicamentosResponse{" +
                "success=" + success +
                ", data=" + data +
                '}';
    }
}
