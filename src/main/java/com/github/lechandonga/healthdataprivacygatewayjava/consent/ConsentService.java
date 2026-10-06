package com.github.lechandonga.healthdataprivacygatewayjava.consent;

import com.github.lechandonga.healthdataprivacygatewayjava.domain.Consent;
import com.github.lechandonga.healthdataprivacygatewayjava.repo.ConsentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConsentService {

    private final ConsentRepository repository;
    private final Object lifecycleLock = new Object();

    public ConsentService(ConsentRepository repository) {
        this.repository = repository;
    }

    public Object lifecycleLock() { return lifecycleLock; }

    public Consent grant(String patientId, String org, String purpose, long grantedAt, long expiresAt) {
        synchronized (lifecycleLock) {
            Consent c = new Consent(patientId, org, purpose, grantedAt, expiresAt);
            repository.save(c);
            return c;
        }
    }

    public Optional<Consent> withdraw(String patientId, String org, String purpose, long at) {
        synchronized (lifecycleLock) {
            return repository.find(patientId, org, purpose).map(c -> {
                c.markWithdrawn(at);
                return c;
            });
        }
    }

    public ConsentDecision decide(String patientId, String org, String purpose, long now) {
        synchronized (lifecycleLock) {
            List<Consent> samePatient = repository.all().stream()
                    .filter(c -> c.getPatientId().equals(patientId))
                    .toList();
            if (samePatient.isEmpty()) {
                return ConsentDecision.CONSENT_MISSING;
            }

            List<Consent> sameOrg = samePatient.stream()
                    .filter(c -> c.getOrganization().equals(org))
                    .toList();
            if (sameOrg.isEmpty()) {
                return ConsentDecision.ORG_OUT_OF_SCOPE;
            }

            List<Consent> samePurpose = sameOrg.stream()
                    .filter(c -> c.getPurpose().equals(purpose))
                    .toList();
            if (samePurpose.isEmpty()) {
                return ConsentDecision.PURPOSE_OUT_OF_SCOPE;
            }

            Consent c = samePurpose.get(0);
            if (c.isWithdrawn()) {
                return ConsentDecision.CONSENT_WITHDRAWN;
            }
            if (now > c.getExpiresAt()) {
                return ConsentDecision.CONSENT_EXPIRED;
            }
            return ConsentDecision.GRANTED;
        }
    }
}
