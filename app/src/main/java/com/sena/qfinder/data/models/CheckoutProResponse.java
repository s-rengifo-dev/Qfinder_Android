package com.sena.qfinder.data.models;

import com.google.gson.annotations.SerializedName;

public class CheckoutProResponse {
    @SerializedName("init_point")
    private String initPoint;

    @SerializedName("sandbox_init_point")
    private String sandboxInitPoint;

    @SerializedName("preference_id")
    private String preferenceId;

    @SerializedName("success")
    private boolean success;

    public String getInitPoint() {
        return initPoint;
    }

    public String getSandboxInitPoint() {
        return sandboxInitPoint;
    }

    public String getPreferenceId() {
        return preferenceId;
    }

    public boolean isSuccess() {
        return success;
    }
}