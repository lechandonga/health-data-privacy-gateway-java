package com.github.lechandonga.healthdataprivacygatewayjava.tests;

import com.github.lechandonga.healthdataprivacygatewayjava.audit.AuditEntry;
import com.github.lechandonga.healthdataprivacygatewayjava.audit.AuditLog;
import com.github.lechandonga.healthdataprivacygatewayjava.tests.support.ApiClient;
import com.github.lechandonga.healthdataprivacygatewayjava.tests.support.BusinessCoverage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@BusinessCoverage("审计完整性（并发不丢不乱序/篡改/删除/乱序可检出/查询授权）")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditIntegrityTest {

    @Autowired
    private TestRestTemplate http;
    @Autowired
    private AuditLog auditLog;

    @Test
    @SuppressWarnings("unchecked")
    void concurrentWritesNeverLoseOrReorderEntries() throws Exception {
        String pid = "AU001";
        ApiClient.grant(http, pid, "AUDIT_ORG", "TREATMENT", 3600);

        int threads = 12;
        int perThread = 25;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        ApiClient.getRecord(http, pid, "AUDIT_ORG", "TREATMENT");
                    }
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        long expected = threads * perThread;
        List<AuditEntry> all = auditLog.snapshot();
        long count = all.stream()
                .filter(e -> "RECORD_ACCESS".equals(e.getEventType()) && pid.equals(e.getPatientId()))
                .count();
        assertEquals(expected, count, "并发写入不得丢记录");

        List<Long> seqs = all.stream().map(AuditEntry::getSeq).toList();
        for (int i = 1; i < seqs.size(); i++) {
            assertEquals(seqs.get(i - 1) + 1, seqs.get(i), "审计序号必须严格连续递增");
        }

        ResponseEntity<Map> verify = ApiClient.verify(http);
        assertEquals("OK", verify.getBody().get("status"));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void tamperedEntryFieldIsDetected() {
        ApiClient.getRecord(http, "P001", "HOSPITAL_A", "TREATMENT");
        int target = auditLog.size() - 1;
        auditLog.corruptEntryAt(target, e -> e.setReason("FORGED_REASON"));
        ResponseEntity<Map> resp = ApiClient.verify(http);
        assertEquals("TAMPERED", resp.getBody().get("status"));
        assertNotNull(resp.getBody().get("firstBadSeq"));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void deletedEntryIsDetected() {
        ApiClient.getRecord(http, "P001", "HOSPITAL_A", "TREATMENT");
        ApiClient.getRecord(http, "P001", "HOSPITAL_A", "TREATMENT");
        int before = auditLog.size();
        auditLog.deleteEntryAt(before - 2);
        ResponseEntity<Map> resp = ApiClient.verify(http);
        assertEquals("DELETED", resp.getBody().get("status"));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void swappedEntriesAreDetectedAsOutOfOrder() {
        ApiClient.getRecord(http, "P001", "HOSPITAL_A", "TREATMENT");
        ApiClient.getRecord(http, "P001", "HOSPITAL_A", "TREATMENT");
        ApiClient.getRecord(http, "P001", "HOSPITAL_A", "TREATMENT");
        int n = auditLog.size();
        auditLog.swapEntries(n - 3, n - 1);
        ResponseEntity<Map> resp = ApiClient.verify(http);
        assertEquals("OUT_OF_ORDER", resp.getBody().get("status"));
    }

    @Test
    void auditQueryRequiresSameAuthorizationCheck() {
        ResponseEntity<Map> denied =
                ApiClient.queryAudit(http, "P001", "HOSPITAL_X", "AUDIT_QUERY");
        assertEquals(HttpStatus.OK, denied.getStatusCode());
        assertEquals("DENIED", denied.getBody().get("decision"));
        assertEquals("ORG_OUT_OF_SCOPE", denied.getBody().get("reason"));

        ApiClient.grant(http, "P001", "AUDITOR_ORG", "AUDIT_QUERY", 3600);
        ResponseEntity<Map> allowed =
                ApiClient.queryAudit(http, "P001", "AUDITOR_ORG", "AUDIT_QUERY");
        assertEquals("ALLOWED", allowed.getBody().get("decision"));
        List<?> entries = (List<?>) allowed.getBody().get("entries");
        assertNotNull(entries);
    }
}
