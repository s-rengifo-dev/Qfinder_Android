package com.sena.qfinder.data.models;

public class CheckoutProRequest {
    private String userId;
    private String planType;

    public CheckoutProRequest(String userId, String planType) {
        this.userId = userId;
        this.planType = planType;
    }

    public String getUserId() {
        return userId;
    }

    public String getPlanType() {
        return planType;
    }
}