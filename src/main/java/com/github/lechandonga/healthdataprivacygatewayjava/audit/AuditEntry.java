package com.github.lechandonga.healthdataprivacygatewayjava.audit;

public class AuditEntry {
    private long seq;
    private String eventType;
    private String patientId;
    private String requestingOrg;
    private String purpose;
    private String decision;
    private String reason;
    private long timestamp;
    private String outputHash;
    private String prevHash;
    private String hash;

    public AuditEntry() {}

    public long getSeq() { return seq; }
    public void setSeq(long v) { this.seq = v; }
    public String getEventType() { return eventType; }
    public void setEventType(String v) { this.eventType = v; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String v) { this.patientId = v; }
    public String getRequestingOrg() { return requestingOrg; }
    public void setRequestingOrg(String v) { this.requestingOrg = v; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String v) { this.purpose = v; }
    public String getDecision() { return decision; }
    public void setDecision(String v) { this.decision = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long v) { this.timestamp = v; }
    public String getOutputHash() { return outputHash; }
    public void setOutputHash(String v) { this.outputHash = v; }
    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String v) { this.prevHash = v; }
    public String getHash() { return hash; }
    public void setHash(String v) { this.hash = v; }

    public static final String EVT_RECORD_ACCESS = "RECORD_ACCESS";
    public static final String EVT_CONSENT_GRANTED = "CONSENT_GRANTED";
    public static final String EVT_CONSENT_WITHDRAWN = "CONSENT_WITHDRAWN";
    public static final String EVT_AUDIT_QUERY = "AUDIT_QUERY";
    public static final String EVT_AUDIT_VERIFY = "AUDIT_VERIFY";

    public String canonicalPayload() {
        return seq + "|" + nz(eventType) + "|" + nz(patientId) + "|" + nz(requestingOrg) + "|"
                + nz(purpose) + "|" + nz(decision) + "|" + nz(reason) + "|" + timestamp + "|"
                + nz(outputHash) + "|" + nz(prevHash);
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
