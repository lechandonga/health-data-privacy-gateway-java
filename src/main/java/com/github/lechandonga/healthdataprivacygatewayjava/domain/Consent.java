package com.github.lechandonga.healthdataprivacygatewayjava.domain;

import java.time.Instant;

/**
 * 患者同意：按 (patientId, orgId, purpose) 限定授权范围。
 */
public record Consent(
        String patientId,
        String orgId,
        String purpose,
        Status status,
        Instant grantedAt,
        Instant expiresAt,
        Instant revokedAt
) {
    public enum Status {ACTIVE, EXPIRED, REVOKED}

    public Consent withStatus(Status status, Instant revokedAt) {
        return new Consent(patientId, orgId, purpose, status, grantedAt, expiresAt, revokedAt);
    }

    public Status effectiveStatus(Instant now) {
        if (status == Status.REVOKED) {
            return Status.REVOKED;
        }
        if (expiresAt != null && !now.isBefore(expiresAt)) {
            return Status.EXPIRED;
        }
        return Status.ACTIVE;
    }
}
