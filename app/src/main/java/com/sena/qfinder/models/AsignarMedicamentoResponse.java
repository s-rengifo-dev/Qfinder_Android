// AsignarMedicamentoResponse.java
package com.sena.qfinder.models;

public class AsignarMedicamentoResponse {
    private boolean success;
    private PacienteMedicamento data;
    private String message;

    // Getters y Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public PacienteMedicamento getData() {
        return data;
    }

    public void setData(PacienteMedicamento data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}