package com.github.lechandonga.healthdataprivacygatewayjava.web;

public class GrantRequest {
    private String patientId;
    private String organization;
    private String purpose;
    private long ttlSeconds = 3600;

    public String getPatientId() { return patientId; }
    public void setPatientId(String v) { this.patientId = v; }
    public String getOrganization() { return organization; }
    public void setOrganization(String v) { this.organization = v; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String v) { this.purpose = v; }
    public long getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(long v) { this.ttlSeconds = v; }
}
