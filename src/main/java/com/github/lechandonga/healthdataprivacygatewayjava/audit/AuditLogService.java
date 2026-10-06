package com.github.lechandonga.healthdataprivacygatewayjava.audit;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * 追加式审计日志，事件以 SHA-256 哈希链串联：
 * hash(n) = SHA256(payload(n) | hash(n-1))。
 * 篡改、删除、乱序任一历史事件都会破坏链条，verify() 可检测。
 * 追加在内部锁内完成，并发写入不丢记录、序号严格递增。
 */
@Service
public class AuditLogService {

    static final String GENESIS = "GENESIS";

    private final List<AuditEvent> events = new ArrayList<>();
    private final Object lock = new Object();
    private long nextSequence = 1;
    private String lastHash = GENESIS;

    public AuditEvent append(String actor, String action, String patientRef,
                             String orgId, String purpose, String outcome, String detail) {
        synchronized (lock) {
            AuditEvent draft = new AuditEvent(
                    nextSequence, Instant.now(), actor, action, patientRef,
                    orgId, purpose, outcome, detail, lastHash, null);
            String hash = sha256(draft.payload());
            AuditEvent event = new AuditEvent(draft.sequence(), draft.timestamp(), draft.actor(),
                    draft.action(), draft.patientRef(), draft.orgId(), draft.purpose(),
                    draft.outcome(), draft.detail(), draft.prevHash(), hash);
            events.add(event);
            lastHash = hash;
            nextSequence++;
            return event;
        }
    }

    public List<AuditEvent> snapshot() {
        synchronized (lock) {
            return List.copyOf(events);
        }
    }

    /**
     * 校验整条链完整性：序号连续递增、prevHash 衔接、hash 重算一致。
     */
    public List<String> verify() {
        return verifyChain(snapshot());
    }

    /**
     * 校验给定事件链的完整性，供实例校验与外部回放校验共用。
     */
    public static List<String> verifyChain(List<AuditEvent> chain) {
        List<String> violations = new ArrayList<>();
        String expectedPrev = GENESIS;
        long expectedSeq = 1;
        for (AuditEvent e : chain) {
            if (e.sequence() != expectedSeq) {
                violations.add("SEQ_GAP: 期望序号 " + expectedSeq + " 实际 " + e.sequence());
            }
            if (!expectedPrev.equals(e.prevHash())) {
                violations.add("CHAIN_BREAK: 序号 " + e.sequence() + " 的 prevHash 与上一环不一致");
            }
            if (!sha256(e.payload()).equals(e.hash())) {
                violations.add("HASH_MISMATCH: 序号 " + e.sequence() + " 的内容被篡改");
            }
            expectedPrev = e.hash();
            expectedSeq = e.sequence() + 1;
        }
        return violations;
    }

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
