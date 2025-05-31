package com.sena.qfinder.data.models;

import com.google.gson.annotations.SerializedName;

public class FirebaseTokenResponse {
    private boolean success;

    @SerializedName("firebaseToken")
    private String token;

    private String message;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}