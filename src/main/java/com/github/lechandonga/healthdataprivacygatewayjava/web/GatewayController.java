package com.github.lechandonga.healthdataprivacygatewayjava.web;

import com.github.lechandonga.healthdataprivacygatewayjava.access.AccessResponse;
import com.github.lechandonga.healthdataprivacygatewayjava.access.GatewayAccessService;
import com.github.lechandonga.healthdataprivacygatewayjava.audit.AuditEntry;
import com.github.lechandonga.healthdataprivacygatewayjava.audit.AuditService;
import com.github.lechandonga.healthdataprivacygatewayjava.audit.AuditVerification;
import com.github.lechandonga.healthdataprivacygatewayjava.config.GatewayProperties;
import com.github.lechandonga.healthdataprivacygatewayjava.consent.ConsentDecision;
import com.github.lechandonga.healthdataprivacygatewayjava.consent.ConsentService;
import com.github.lechandonga.healthdataprivacygatewayjava.domain.Consent;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class GatewayController {

    private final GatewayAccessService access;
    private final ConsentService consents;
    private final AuditService audit;
    private final GatewayProperties props;

    public GatewayController(GatewayAccessService access, ConsentService consents,
                             AuditService audit, GatewayProperties props) {
        this.access = access;
        this.consents = consents;
        this.audit = audit;
        this.props = props;
    }

    @GetMapping("/api/records/{patientId}")
    public AccessResponse getRecord(@PathVariable String patientId,
                                    @RequestParam String org,
                                    @RequestParam String purpose) {
        return access.accessRecord(patientId, org, purpose, System.currentTimeMillis());
    }

    @PostMapping("/api/consents")
    public Map<String, Object> grant(@RequestBody GrantRequest req) {
        long now = System.currentTimeMillis();
        Consent c = consents.grant(req.getPatientId(), req.getOrganization(), req.getPurpose(),
                now, now + req.getTtlSeconds() * 1000);
        audit.record(AuditEntry.EVT_CONSENT_GRANTED, c.getPatientId(), c.getOrganization(),
                c.getPurpose(), "ALLOWED", "GRANTED", null, now);
        return Map.of("patientId", c.getPatientId(), "organization", c.getOrganization(),
                "purpose", c.getPurpose(), "grantedAt", c.getGrantedAt(),
                "expiresAt", c.getExpiresAt(), "withdrawn", c.isWithdrawn());
    }

    @DeleteMapping("/api/consents")
    public Map<String, Object> withdraw(@RequestParam String patientId,
                                        @RequestParam String org,
                                        @RequestParam String purpose) {
        long now = System.currentTimeMillis();
        Consent c = consents.withdraw(patientId, org, purpose, now)
                .orElseThrow(() -> new IllegalArgumentException("consent not found"));
        audit.record(AuditEntry.EVT_CONSENT_WITHDRAWN, patientId, org, purpose,
                "ALLOWED", "WITHDRAWN", null, now);
        return Map.of("patientId", c.getPatientId(), "organization", c.getOrganization(),
                "purpose", c.getPurpose(), "withdrawn", c.isWithdrawn(),
                "withdrawnAt", c.getWithdrawnAt());
    }

    @GetMapping("/api/audits")
    public Map<String, Object> queryAudit(@RequestParam String patientId,
                                          @RequestParam String org,
                                          @RequestParam(defaultValue = "AUDIT_QUERY") String purpose) {
        long now = System.currentTimeMillis();
        ConsentDecision decision = consents.decide(patientId, org, purpose, now);
        if (decision != ConsentDecision.GRANTED) {
            audit.record(AuditEntry.EVT_AUDIT_QUERY, patientId, org, purpose,
                    "DENIED", decision.name(), null, now);
            return Map.of("decision", "DENIED", "reason", decision.name(), "entries", List.of());
        }
        List<AuditEntry> entries = audit.query(patientId, org);
        AuditEntry self = audit.record(AuditEntry.EVT_AUDIT_QUERY, patientId, org, purpose,
                "ALLOWED", "GRANTED", String.valueOf(entries.size()), now);
        return Map.of("decision", "ALLOWED", "reason", "GRANTED",
                "entries", entries, "auditSeq", self.getSeq());
    }

    @GetMapping("/api/audits/verify")
    public AuditVerification verify(@RequestParam(required = false) String patientId,
                                    @RequestParam(required = false) String org) {
        AuditVerification result = audit.verify();
        audit.record(AuditEntry.EVT_AUDIT_VERIFY, patientId, org,
                props.getAuditQueryPurpose(),
                AuditVerification.OK.equals(result.status()) ? "ALLOWED" : "DENIED",
                result.status(), String.valueOf(result.totalEntries()),
                System.currentTimeMillis());
        return result;
    }
}
