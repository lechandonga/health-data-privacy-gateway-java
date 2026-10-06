package com.github.lechandonga.healthdataprivacygatewayjava.tests;

import com.github.lechandonga.healthdataprivacygatewayjava.api.AccessDeniedException;
import com.github.lechandonga.healthdataprivacygatewayjava.api.GatewayService;
import com.github.lechandonga.healthdataprivacygatewayjava.consent.ConsentService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业务覆盖：撤回与并发访问竞争 —— 撤回生效后不得再有任何可识别数据流出。
 */
@SpringBootTest
class ConcurrentRevokeTests {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentRevokeTests.class);

    @Autowired
    GatewayService gateway;
    @Autowired
    ConsentService consents;

    @Test
    void noIdentifiableDataAfterConcurrentRevoke() throws Exception {
        String patient = "P001";
        String org = "ORG-RACE";
        String purpose = "RESEARCH";
        consents.grant(patient, org, purpose, null);
        AtomicBoolean revoked = new AtomicBoolean(false);
        AtomicInteger allowedStartedAfterRevoke = new AtomicInteger(0);
        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger denied = new AtomicInteger(0);
        int threads = 16;
        int rounds = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    for (int r = 0; r < rounds; r++) {
                        try {
                            boolean startedAfterRevoke = revoked.get();
                            gateway.access("race-tester", patient, org, purpose);
                            allowed.incrementAndGet();
                            if (startedAfterRevoke) {
                                allowedStartedAfterRevoke.incrementAndGet();
                            }
                        } catch (AccessDeniedException e) {
                            denied.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
        start.countDown();
        Thread.sleep(5);
        consents.revoke(patient, org, purpose);
        revoked.set(true);
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();
        assertTrue(allowed.get() > 0, "撤回前应有成功访问");
        assertTrue(denied.get() > 0, "撤回后应有拒绝访问");
        assertEquals(0, allowedStartedAfterRevoke.get(), "撤回生效后发起的访问不得放行");
        assertThrows(AccessDeniedException.class, () -> gateway.access("race-tester", patient, org, purpose));
        log.info("[并发撤回] 线程={} 撤回前放行={} 拒绝={} 撤回后放行={} 通过",
                threads, allowed.get(), denied.get(), allowedStartedAfterRevoke.get());
    }
}
