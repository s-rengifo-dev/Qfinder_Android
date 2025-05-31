package com.sena.qfinder.data.models;

import java.util.List;

public class RedListResponse {
    private boolean success;
    private String message;
    private List<RedResponse> data;

    // Getters y Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<RedResponse> getData() { return data; }
    public void setData(List<RedResponse> data) { this.data = data; }
}