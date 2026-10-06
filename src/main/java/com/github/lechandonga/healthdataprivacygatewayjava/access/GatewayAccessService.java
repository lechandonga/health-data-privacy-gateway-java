package com.github.lechandonga.healthdataprivacygatewayjava.access;

import com.github.lechandonga.healthdataprivacygatewayjava.audit.AuditEntry;
import com.github.lechandonga.healthdataprivacygatewayjava.audit.AuditService;
import com.github.lechandonga.healthdataprivacygatewayjava.config.GatewayProperties;
import com.github.lechandonga.healthdataprivacygatewayjava.consent.ConsentDecision;
import com.github.lechandonga.healthdataprivacygatewayjava.consent.ConsentService;
import com.github.lechandonga.healthdataprivacygatewayjava.deid.DeidentificationService;
import com.github.lechandonga.healthdataprivacygatewayjava.domain.PatientRecord;
import com.github.lechandonga.healthdataprivacygatewayjava.repo.PatientRepository;
import com.github.lechandonga.healthdataprivacygatewayjava.security.HmacSupport;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GatewayAccessService {

    private final PatientRepository patients;
    private final ConsentService consents;
    private final DeidentificationService deid;
    private final AuditService audit;
    private final HmacSupport hmac;
    private final GatewayProperties props;

    public GatewayAccessService(PatientRepository patients, ConsentService consents,
                                DeidentificationService deid, AuditService audit,
                                HmacSupport hmac, GatewayProperties props) {
        this.patients = patients;
        this.consents = consents;
        this.deid = deid;
        this.audit = audit;
        this.hmac = hmac;
        this.props = props;
    }

    public AccessResponse accessRecord(String patientId, String org, String purpose, long now) {
        PatientRecord record = patients.findById(patientId).orElse(null);

        if (record == null) {
            if (props.isAuditEnabled()) {
                audit.record(AuditEntry.EVT_RECORD_ACCESS, patientId, org, purpose,
                        "DENIED", "PATIENT_NOT_FOUND", null, now);
            }
            throw new AccessDeniedException("PATIENT_NOT_FOUND", Map.of("patientId", patientId));
        }

        List<String> validationErrors = props.isRequireValidRecord() ? deid.validate(record) : List.of();
        if (!validationErrors.isEmpty()) {
            if (props.isAuditEnabled()) {
                audit.record(AuditEntry.EVT_RECORD_ACCESS, patientId, org, purpose,
                        "REJECTED", "MALFORMED_RECORD:" + String.join(",", validationErrors), null, now);
            }
            throw new RecordRejectedException(patientId, validationErrors);
        }

        synchronized (consents.lifecycleLock()) {
            ConsentDecision decision = consents.decide(patientId, org, purpose, now);
            boolean identifiable = decision == ConsentDecision.GRANTED;
            Map<String, Object> view = deid.deidentify(record, identifiable);

            if (props.isAuditEnabled()) {
                audit.record(AuditEntry.EVT_RECORD_ACCESS, patientId, org, purpose,
                        identifiable ? "ALLOWED" : "DENIED",
                        identifiable ? "GRANTED" : decision.name(),
                        hmac.digest(view.toString()), now);
            }

            if (!identifiable) {
                throw new AccessDeniedException(decision.name(), view);
            }

            return new AccessResponse(patientId, org, purpose,
                    "ALLOWED", "GRANTED", true, now, view);
        }
    }
}
