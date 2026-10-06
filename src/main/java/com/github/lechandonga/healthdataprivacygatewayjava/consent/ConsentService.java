package com.github.lechandonga.healthdataprivacygatewayjava.consent;

import com.github.lechandonga.healthdataprivacygatewayjava.domain.AccessDecision;
import com.github.lechandonga.healthdataprivacygatewayjava.domain.Consent;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 同意状态管理：按 (patientId, orgId, purpose) 三元组限定授权范围。
 * 判定与访问执行在同一把患者级锁内完成，保证撤回与访问并发时，
 * 撤回生效后不会再有可识别数据流出。
 */
@Service
public class ConsentService {

    private final Map<String, Consent> consents = new ConcurrentHashMap<>();
    private final Map<String, Object> patientLocks = new ConcurrentHashMap<>();
    private final Clock clock;

    public ConsentService() {
        this(Clock.systemUTC());
    }

    public ConsentService(Clock clock) {
        this.clock = clock;
    }

    private static String key(String patientId, String orgId, String purpose) {
        return patientId + "|" + orgId + "|" + purpose;
    }

    public Object lockFor(String patientId) {
        return patientLocks.computeIfAbsent(patientId, k -> new Object());
    }

    public Consent grant(String patientId, String orgId, String purpose, Instant expiresAt) {
        Consent consent = new Consent(patientId, orgId, purpose, Consent.Status.ACTIVE,
                Instant.now(clock), expiresAt, null);
        synchronized (lockFor(patientId)) {
            consents.put(key(patientId, orgId, purpose), consent);
        }
        return consent;
    }

    public void revoke(String patientId, String orgId, String purpose) {
        synchronized (lockFor(patientId)) {
            Consent existing = consents.get(key(patientId, orgId, purpose));
            if (existing != null && existing.status() != Consent.Status.REVOKED) {
                consents.put(key(patientId, orgId, purpose),
                        existing.withStatus(Consent.Status.REVOKED, Instant.now(clock)));
            }
        }
    }

    /**
     * 调用方必须已持有 {@link #lockFor(String)} 的患者锁，
     * 以保证判定结果在数据输出前不会被撤回操作改变。
     */
    public AccessDecision check(String patientId, String orgId, String purpose) {
        Consent consent = consents.get(key(patientId, orgId, purpose));
        if (consent == null) {
            return AccessDecision.deny("CONSENT_MISSING: 无匹配的同意记录");
        }
        return switch (consent.effectiveStatus(Instant.now(clock))) {
            case ACTIVE -> AccessDecision.allow();
            case EXPIRED -> AccessDecision.deny("CONSENT_EXPIRED: 同意已过期");
            case REVOKED -> AccessDecision.deny("CONSENT_REVOKED: 同意已撤回");
        };
    }
}
