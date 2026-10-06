package com.github.lechandonga.healthdataprivacygatewayjava.domain;

public class Consent {
    private final String patientId;
    private final String organization;
    private final String purpose;
    private final long grantedAt;
    private final long expiresAt;
    private volatile boolean withdrawn;
    private volatile long withdrawnAt;

    public Consent(String patientId, String organization, String purpose, long grantedAt, long expiresAt) {
        this.patientId = patientId;
        this.organization = organization;
        this.purpose = purpose;
        this.grantedAt = grantedAt;
        this.expiresAt = expiresAt;
    }

    public String getPatientId() { return patientId; }
    public String getOrganization() { return organization; }
    public String getPurpose() { return purpose; }
    public long getGrantedAt() { return grantedAt; }
    public long getExpiresAt() { return expiresAt; }
    public boolean isWithdrawn() { return withdrawn; }
    public long getWithdrawnAt() { return withdrawnAt; }

    public void markWithdrawn(long at) {
        this.withdrawn = true;
        this.withdrawnAt = at;
    }

    public String scopeKey() {
        return patientId + "|" + organization + "|" + purpose;
    }
}
