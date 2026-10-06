package com.github.lechandonga.healthdataprivacygatewayjava.access;

import java.util.List;

public class RecordRejectedException extends RuntimeException {
    private final String patientId;
    private final List<String> reasons;

    public RecordRejectedException(String patientId, List<String> reasons) {
        super("record rejected: " + reasons);
        this.patientId = patientId;
        this.reasons = reasons;
    }

    public String getPatientId() { return patientId; }
    public List<String> getReasons() { return reasons; }
}
