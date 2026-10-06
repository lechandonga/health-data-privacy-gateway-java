package com.github.lechandonga.healthdataprivacygatewayjava.api;

import com.github.lechandonga.healthdataprivacygatewayjava.audit.AuditLogService;
import com.github.lechandonga.healthdataprivacygatewayjava.consent.ConsentService;
import com.github.lechandonga.healthdataprivacygatewayjava.deid.DeidentificationService;
import com.github.lechandonga.healthdataprivacygatewayjava.deid.RecordValidationException;
import com.github.lechandonga.healthdataprivacygatewayjava.domain.AccessDecision;
import com.github.lechandonga.healthdataprivacygatewayjava.domain.DeidentifiedRecord;
import com.github.lechandonga.healthdataprivacygatewayjava.domain.PatientRecord;
import com.github.lechandonga.healthdataprivacygatewayjava.domain.PatientRecordStore;
import org.springframework.stereotype.Service;

/**
 * 访问网关编排：患者锁内完成 同意判定 -> 取数 -> 去标识化 -> 审计，
 * 保证撤回并发生效后不会再输出可识别数据；所有路径（含拒绝）均留痕。
 */
@Service
public class GatewayService {

    private final ConsentService consentService;
    private final DeidentificationService deidService;
    private final PatientRecordStore store;
    private final AuditLogService auditLog;

    public GatewayService(ConsentService consentService, DeidentificationService deidService,
                          PatientRecordStore store, AuditLogService auditLog) {
        this.consentService = consentService;
        this.deidService = deidService;
        this.store = store;
        this.auditLog = auditLog;
    }

    public DeidentifiedRecord access(String actor, String patientId, String orgId, String purpose) {
        synchronized (consentService.lockFor(patientId)) {
            AccessDecision decision = consentService.check(patientId, orgId, purpose);
            if (!decision.allowed()) {
                audit(actor, patientId, orgId, purpose, "DENIED", decision.reason());
                throw new AccessDeniedException(decision.reason());
            }
            PatientRecord record = store.find(patientId).orElse(null);
            try {
                DeidentifiedRecord output = deidService.deidentify(record);
                audit(actor, output.patientRef(), orgId, purpose, "ALLOWED", "输出去标识化记录");
                return output;
            } catch (RecordValidationException e) {
                audit(actor, patientId, orgId, purpose, "REJECTED",
                        e.getReason() + (e.getField() != null ? ":" + e.getField() : ""));
                throw e;
            }
        }
    }

    private void audit(String actor, String patientRef, String orgId, String purpose,
                       String outcome, String detail) {
        auditLog.append(actor, "ACCESS_RECORD", patientRef, orgId, purpose, outcome, detail);
    }
}
