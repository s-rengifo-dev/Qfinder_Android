package com.sena.qfinder.data.models;

import com.google.gson.annotations.SerializedName;

public class SubscriptionResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("mercado_pago_id")
    private String mercadoPagoId;

    @SerializedName("init_point")
    private String initPoint;

    @SerializedName("status")
    private String status;

    public String getId() {
        return id;
    }

    public String getMercadoPagoId() {
        return mercadoPagoId;
    }

    public String getInitPoint() {
        return initPoint;
    }

    public String getStatus() {
        return status;
    }
}