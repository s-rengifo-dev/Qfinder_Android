package com.sena.qfinder.data.models;

import java.util.List;

public class ActividadListResponse {
    private boolean success;
    private String message;
    private List<ActividadGetResponse> data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<ActividadGetResponse> getData() {
        return data;
    }
}