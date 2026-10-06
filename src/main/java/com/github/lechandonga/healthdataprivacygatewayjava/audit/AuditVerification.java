package com.github.lechandonga.healthdataprivacygatewayjava.audit;

public record AuditVerification(String status, long totalEntries, Long firstBadSeq, String detail) {
    public static final String OK = "OK";
    public static final String TAMPERED = "TAMPERED";
    public static final String DELETED = "DELETED";
    public static final String OUT_OF_ORDER = "OUT_OF_ORDER";

    public static AuditVerification ok(long total) {
        return new AuditVerification(OK, total, null, "hash chain intact");
    }

    public static AuditVerification failure(String status, long total, long badSeq, String detail) {
        return new AuditVerification(status, total, badSeq, detail);
    }
}
