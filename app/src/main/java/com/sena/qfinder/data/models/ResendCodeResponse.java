
package com.sena.qfinder.data.models;

public class ResendCodeResponse {
    private boolean success;
    private String message;
    private String correo;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getCorreo() {
        return correo;
    }
}