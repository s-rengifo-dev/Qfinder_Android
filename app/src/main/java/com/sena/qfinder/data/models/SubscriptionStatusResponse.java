package com.sena.qfinder.data.models;

import java.util.Date;

public class SubscriptionStatusResponse {
    private String id;
    private String status;
    private String type;
    private Date start_date;
    private Date renewal_date;
    private int patient_limit;
    private int caregiver_limit;
    private int used_patients;
    private int used_caregivers;

    // Getters
    public String getId() { return id; }
    public String getStatus() { return status; }
    public String getType() { return type; }
    public Date getStartDate() { return start_date; }
    public Date getRenewalDate() { return renewal_date; }
    public int getPatientLimit() { return patient_limit; }
    public int getCaregiverLimit() { return caregiver_limit; }
    public int getUsedPatients() { return used_patients; }
    public int getUsedCaregivers() { return used_caregivers; }
}