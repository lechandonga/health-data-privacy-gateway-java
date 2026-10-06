package com.github.lechandonga.healthdataprivacygatewayjava.tests;

import com.github.lechandonga.healthdataprivacygatewayjava.audit.AuditLog;
import com.github.lechandonga.healthdataprivacygatewayjava.domain.PatientRecord;
import com.github.lechandonga.healthdataprivacygatewayjava.repo.PatientRepository;
import com.github.lechandonga.healthdataprivacygatewayjava.tests.support.ApiClient;
import com.github.lechandonga.healthdataprivacygatewayjava.tests.support.BusinessCoverage;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@BusinessCoverage("并发撤回（撤回生效后不得返回可识别数据）")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrentWithdrawalTest {

    @Autowired
    private TestRestTemplate http;
    @Autowired
    private AuditLog auditLog;
    @Autowired
    private PatientRepository patients;

    @RepeatedTest(5)
    void withdrawalRacingWithAccessNeverLeaksIdentifiableData() throws Exception {
        String pid = "CW001";
        patients.save(new PatientRecord(pid, "竞态患者甲", "11010419880715672X",
                "13800138333", "race1@example.com", "1990-03-07", "男",
                "竞争市辖区", "心脏病", "监护治疗"));
        ApiClient.grant(http, pid, "RACE_ORG", "TREATMENT", 3600);

        int threads = 16;
        int perThread = 20;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger allowed = new AtomicInteger();
        AtomicInteger denied = new AtomicInteger();
        AtomicInteger badLeak = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        ResponseEntity<Map> resp = ApiClient.getRecord(http, pid, "RACE_ORG", "TREATMENT");
                        Object decision = resp.getBody().get("decision");
                        if ("ALLOWED".equals(decision)) {
                            allowed.incrementAndGet();
                        } else if ("DENIED".equals(decision)
                                && "CONSENT_WITHDRAWN".equals(resp.getBody().get("reason"))) {
                            denied.incrementAndGet();
                        } else {
                            badLeak.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    badLeak.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        Thread.sleep(5);
        ResponseEntity<Map> withdrawal = ApiClient.withdraw(http, pid, "RACE_ORG", "TREATMENT");
        assertEquals(200, withdrawal.getStatusCode().value());
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(0, badLeak.get(), "出现非 ALLOWED / CONSENT_WITHDRAWN 的异常结果");
        assertEquals(threads * perThread, allowed.get() + denied.get());
        assertTrue(allowed.get() > 0, "竞态窗口内应当至少有一次授权通过，以证明真并发");
        assertTrue(denied.get() > 0, "撤回后应当至少有一次被拒绝");

        for (int i = 0; i < 5; i++) {
            ResponseEntity<Map> after = ApiClient.getRecord(http, pid, "RACE_ORG", "TREATMENT");
            assertEquals("DENIED", after.getBody().get("decision"));
            assertEquals("CONSENT_WITHDRAWN", after.getBody().get("reason"));
            assertEquals(Boolean.FALSE, after.getBody().get("identifiable"));
        }
    }

    @Test
    void noAuditEntryShowsAllowedAfterWithdrawalTimestamp() {
        String pid = "CW002";
        patients.save(new PatientRecord(pid, "竞态患者乙", "31010119900307123X",
                "13800138444", "race2@example.com", "1988-07-15", "女",
                "竞争市新区", "糖尿病", "胰岛素治疗"));
        long grantAt = System.currentTimeMillis();
        ApiClient.grant(http, pid, "RACE_ORG", "TREATMENT", 3600);
        ApiClient.getRecord(http, pid, "RACE_ORG", "TREATMENT");
        ApiClient.withdraw(http, pid, "RACE_ORG", "TREATMENT");
        long withdrawnAt = System.currentTimeMillis();
        ApiClient.getRecord(http, pid, "RACE_ORG", "TREATMENT");

        boolean leak = auditLog.snapshot().stream()
                .filter(e -> "RECORD_ACCESS".equals(e.getEventType()))
                .filter(e -> pid.equals(e.getPatientId()))
                .anyMatch(e -> "ALLOWED".equals(e.getDecision()) && e.getTimestamp() >= withdrawnAt);
        assertFalse(leak, "撤回生效后的审计记录里不允许出现 ALLOWED 访问");
        assertTrue(grantAt <= withdrawnAt);
    }
}
