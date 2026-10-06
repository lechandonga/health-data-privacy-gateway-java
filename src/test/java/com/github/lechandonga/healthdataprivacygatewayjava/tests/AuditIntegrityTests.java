package com.github.lechandonga.healthdataprivacygatewayjava.tests;

import com.github.lechandonga.healthdataprivacygatewayjava.audit.AuditEvent;
import com.github.lechandonga.healthdataprivacygatewayjava.audit.AuditLogService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业务覆盖：审计链完整性（篡改 / 删除 / 乱序可检测）与并发写入不丢不错序。
 */
class AuditIntegrityTests {

    private static final Logger log = LoggerFactory.getLogger(AuditIntegrityTests.class);

    private AuditLogService newLogWithEvents(int n) {
        AuditLogService audit = new AuditLogService();
        for (int i = 0; i < n; i++) {
            audit.append("tester", "ACCESS_RECORD", "PSEUDO-" + i, "ORG", "RESEARCH", "ALLOWED", "case-" + i);
        }
        return audit;
    }

    @Test
    void intactChainVerifies() {
        AuditLogService audit = newLogWithEvents(5);
        assertTrue(audit.verify().isEmpty());
        log.info("[审计完整性] 用例=完整链校验 事件数=5 结果=无违规 通过");
    }

    @Test
    void tamperedEventIsDetected() {
        AuditLogService audit = newLogWithEvents(5);
        List<AuditEvent> chain = new ArrayList<>(audit.snapshot());
        AuditEvent victim = chain.get(2);
        chain.set(2, new AuditEvent(victim.sequence(), victim.timestamp(), victim.actor(),
                victim.action(), victim.patientRef(), victim.orgId(), victim.purpose(),
                "DENIED", "被篡改的内容", victim.prevHash(), victim.hash()));
        List<String> violations = AuditLogService.verifyChain(chain);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.startsWith("HASH_MISMATCH")));
        log.info("[审计完整性] 用例=内容篡改 检出={} 通过", violations);
    }

    @Test
    void deletedEventIsDetected() {
        AuditLogService audit = newLogWithEvents(5);
        List<AuditEvent> chain = new ArrayList<>(audit.snapshot());
        chain.remove(2);
        List<String> violations = AuditLogService.verifyChain(chain);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.startsWith("SEQ_GAP") || v.startsWith("CHAIN_BREAK")));
        log.info("[审计完整性] 用例=事件删除 检出={} 通过", violations);
    }

    @Test
    void reorderedEventsAreDetected() {
        AuditLogService audit = newLogWithEvents(5);
        List<AuditEvent> chain = new ArrayList<>(audit.snapshot());
        Collections.swap(chain, 1, 3);
        List<String> violations = AuditLogService.verifyChain(chain);
        assertFalse(violations.isEmpty());
        log.info("[审计完整性] 用例=事件乱序 检出={} 通过", violations);
    }

    @Test
    void concurrentAppendsLoseNothingAndStayOrdered() throws Exception {
        AuditLogService audit = new AuditLogService();
        int threads = 8;
        int perThread = 25;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int id = i;
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < perThread; j++) {
                        audit.append("t" + id, "ACCESS_RECORD", "PSEUDO", "ORG", "R", "ALLOWED", "j" + j);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();
        List<AuditEvent> chain = audit.snapshot();
        assertEquals(threads * perThread, chain.size());
        for (int i = 0; i < chain.size(); i++) {
            assertEquals(i + 1, chain.get(i).sequence());
        }
        assertTrue(audit.verify().isEmpty());
        log.info("[审计完整性] 用例=并发写入 线程={} 总事件={} 序号连续且链完整 通过",
                threads, chain.size());
    }
}
