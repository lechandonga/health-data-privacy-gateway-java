package com.github.lechandonga.healthdataprivacygatewayjava.audit;

import java.time.Instant;

/**
 * 审计事件，构成哈希链的一环。
 */
public record AuditEvent(
        long sequence,
        Instant timestamp,
        String actor,
        String action,
        String patientRef,
        String orgId,
        String purpose,
        String outcome,
        String detail,
        String prevHash,
        String hash
) {
    public String payload() {
        return sequence + "|" + timestamp + "|" + actor + "|" + action + "|" + patientRef
                + "|" + orgId + "|" + purpose + "|" + outcome + "|" + detail + "|" + prevHash;
    }
}
