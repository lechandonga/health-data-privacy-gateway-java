package com.github.lechandonga.healthdataprivacygatewayjava.api;

import com.github.lechandonga.healthdataprivacygatewayjava.audit.AuditEvent;
import com.github.lechandonga.healthdataprivacygatewayjava.audit.AuditLogService;
import com.github.lechandonga.healthdataprivacygatewayjava.consent.ConsentService;
import com.github.lechandonga.healthdataprivacygatewayjava.domain.DeidentifiedRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GatewayController {

    private final GatewayService gatewayService;
    private final ConsentService consentService;
    private final AuditLogService auditLog;
    private final String auditorRole;

    public GatewayController(GatewayService gatewayService, ConsentService consentService,
                             AuditLogService auditLog,
                             @Value("${gateway.audit.auditor-role:AUDITOR}") String auditorRole) {
        this.gatewayService = gatewayService;
        this.consentService = consentService;
        this.auditLog = auditLog;
        this.auditorRole = auditorRole;
    }

    @GetMapping("/records/{patientId}")
    public DeidentifiedRecord access(@PathVariable String patientId,
                                     @RequestParam String orgId,
                                     @RequestParam String purpose,
                                     @RequestHeader(name = "X-Actor", defaultValue = "anonymous") String actor) {
        return gatewayService.access(actor, patientId, orgId, purpose);
    }

    @PostMapping("/consents/grant")
    public Map<String, Object> grant(@RequestParam String patientId,
                                     @RequestParam String orgId,
                                     @RequestParam String purpose,
                                     @RequestParam(required = false) String expiresAt) {
        consentService.grant(patientId, orgId, purpose,
                expiresAt != null ? Instant.parse(expiresAt) : null);
        auditLog.append("api", "CONSENT_GRANT", patientId, orgId, purpose, "OK", "");
        return Map.of("status", "GRANTED");
    }

    @PostMapping("/consents/revoke")
    public Map<String, Object> revoke(@RequestParam String patientId,
                                      @RequestParam String orgId,
                                      @RequestParam String purpose) {
        consentService.revoke(patientId, orgId, purpose);
        auditLog.append("api", "CONSENT_REVOKE", patientId, orgId, purpose, "OK", "");
        return Map.of("status", "REVOKED");
    }

    @GetMapping("/audit")
    public List<AuditEvent> audit(@RequestHeader(name = "X-Access-Role", defaultValue = "") String role,
                                  @RequestHeader(name = "X-Actor", defaultValue = "anonymous") String actor) {
        requireAuditor(actor, role);
        auditLog.append(actor, "AUDIT_QUERY", "-", "-", "-", "ALLOWED", "查询审计日志");
        return auditLog.snapshot();
    }

    @GetMapping("/audit/verify")
    public Map<String, Object> verify(@RequestHeader(name = "X-Access-Role", defaultValue = "") String role,
                                      @RequestHeader(name = "X-Actor", defaultValue = "anonymous") String actor) {
        requireAuditor(actor, role);
        List<String> violations = auditLog.verify();
        auditLog.append(actor, "AUDIT_VERIFY", "-", "-", "-",
                violations.isEmpty() ? "INTACT" : "VIOLATED", String.join(";", violations));
        return Map.of("intact", violations.isEmpty(), "violations", violations);
    }

    private void requireAuditor(String actor, String role) {
        if (!auditorRole.equals(role)) {
            auditLog.append(actor, "AUDIT_QUERY", "-", "-", "-", "DENIED", "ROLE_REQUIRED:" + auditorRole);
            throw new AccessDeniedException("ROLE_REQUIRED: 需要 " + auditorRole + " 角色");
        }
    }
}
