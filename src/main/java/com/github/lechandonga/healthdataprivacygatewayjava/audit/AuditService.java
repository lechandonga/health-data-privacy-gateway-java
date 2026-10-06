package com.github.lechandonga.healthdataprivacygatewayjava.audit;

import com.github.lechandonga.healthdataprivacygatewayjava.security.HmacSupport;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private final AuditLog log;
    private final HmacSupport hmac;

    public AuditService(AuditLog log, HmacSupport hmac) {
        this.log = log;
        this.hmac = hmac;
    }

    public AuditEntry record(String eventType, String patientId, String org, String purpose,
                             String decision, String reason, String outputHash, long timestamp) {
        synchronized (log) {
            List<AuditEntry> existing = log.snapshot();
            String prevHash = existing.isEmpty() ? null : existing.get(existing.size() - 1).getHash();
            AuditEntry e = new AuditEntry();
            e.setEventType(eventType);
            e.setPatientId(patientId);
            e.setRequestingOrg(org);
            e.setPurpose(purpose);
            e.setDecision(decision);
            e.setReason(reason);
            e.setTimestamp(timestamp);
            e.setOutputHash(outputHash);
            e.setPrevHash(prevHash);
            AuditEntry appended = log.appendNew(e);
            appended.setHash(hmac.chainHash(appended.canonicalPayload(), prevHash));
            return appended;
        }
    }

    public List<AuditEntry> query(String patientId, String org) {
        return log.snapshot().stream()
                .filter(e -> e.getPatientId() == null || patientId == null || e.getPatientId().equals(patientId))
                .filter(e -> e.getRequestingOrg() == null || org == null || e.getRequestingOrg().equals(org))
                .toList();
    }

    public AuditVerification verify() {
        List<AuditEntry> all = log.snapshot();
        String prevHash = null;
        long expectedSeq = 1;
        for (int i = 1; i < all.size(); i++) {
            if (all.get(i).getSeq() < all.get(i - 1).getSeq()) {
                return AuditVerification.failure(AuditVerification.OUT_OF_ORDER, all.size(),
                        all.get(i).getSeq(), "seq order violation at " + all.get(i).getSeq());
            }
        }
        for (AuditEntry e : all) {
            if (e.getSeq() != expectedSeq) {
                return AuditVerification.failure(AuditVerification.DELETED, all.size(),
                        expectedSeq, "seq gap: expected " + expectedSeq + " but got " + e.getSeq());
            }
            String expectedHash = hmac.chainHash(e.canonicalPayload(), prevHash);
            if (!expectedHash.equals(e.getHash())) {
                return AuditVerification.failure(AuditVerification.TAMPERED, all.size(),
                        e.getSeq(), "hash mismatch at " + e.getSeq());
            }
            prevHash = e.getHash();
            expectedSeq++;
        }
        return AuditVerification.ok(all.size());
    }
}
