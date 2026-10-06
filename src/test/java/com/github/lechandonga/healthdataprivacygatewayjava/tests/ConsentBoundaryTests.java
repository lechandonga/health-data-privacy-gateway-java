package com.github.lechandonga.healthdataprivacygatewayjava.tests;

import com.github.lechandonga.healthdataprivacygatewayjava.api.AccessDeniedException;
import com.github.lechandonga.healthdataprivacygatewayjava.api.GatewayService;
import com.github.lechandonga.healthdataprivacygatewayjava.consent.ConsentService;
import com.github.lechandonga.healthdataprivacygatewayjava.domain.DeidentifiedRecord;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业务覆盖：同意判定边界（缺失 / 过期 / 撤回 / 超范围机构与用途）。
 */
@SpringBootTest
class ConsentBoundaryTests {

    private static final Logger log = LoggerFactory.getLogger(ConsentBoundaryTests.class);

    @Autowired
    GatewayService gateway;
    @Autowired
    ConsentService consents;

    @Test
    void missingConsentIsDenied() {
        AccessDeniedException e = assertThrows(AccessDeniedException.class,
                () -> gateway.access("tester", "P001", "ORG-X", "RESEARCH"));
        assertTrue(e.getMessage().startsWith("CONSENT_MISSING"));
        log.info("[同意边界] 用例=同意缺失 结果=拒绝({}) 通过", e.getMessage());
    }

    @Test
    void expiredConsentIsDenied() {
        consents.grant("P001", "ORG-A", "TREATMENT", Instant.now().minus(1, ChronoUnit.HOURS));
        AccessDeniedException e = assertThrows(AccessDeniedException.class,
                () -> gateway.access("tester", "P001", "ORG-A", "TREATMENT"));
        assertTrue(e.getMessage().startsWith("CONSENT_EXPIRED"));
        log.info("[同意边界] 用例=同意过期 结果=拒绝({}) 通过", e.getMessage());
    }

    @Test
    void revokedConsentIsDenied() {
        consents.grant("P001", "ORG-B", "TREATMENT", null);
        consents.revoke("P001", "ORG-B", "TREATMENT");
        AccessDeniedException e = assertThrows(AccessDeniedException.class,
                () -> gateway.access("tester", "P001", "ORG-B", "TREATMENT"));
        assertTrue(e.getMessage().startsWith("CONSENT_REVOKED"));
        log.info("[同意边界] 用例=同意撤回 结果=拒绝({}) 通过", e.getMessage());
    }

    @Test
    void outOfScopeOrgAndPurposeAreDenied() {
        consents.grant("P002", "ORG-C", "TREATMENT", null);
        AccessDeniedException wrongPurpose = assertThrows(AccessDeniedException.class,
                () -> gateway.access("tester", "P002", "ORG-C", "MARKETING"));
        AccessDeniedException wrongOrg = assertThrows(AccessDeniedException.class,
                () -> gateway.access("tester", "P002", "ORG-OTHER", "TREATMENT"));
        assertTrue(wrongPurpose.getMessage().startsWith("CONSENT_MISSING"));
        assertTrue(wrongOrg.getMessage().startsWith("CONSENT_MISSING"));
        log.info("[同意边界] 用例=超范围用途/机构 结果=均拒绝 通过");
    }

    @Test
    void activeConsentAllowsAndOutputIsStable() {
        consents.grant("P002", "ORG-D", "RESEARCH", null);
        DeidentifiedRecord first = gateway.access("tester", "P002", "ORG-D", "RESEARCH");
        DeidentifiedRecord second = gateway.access("tester", "P002", "ORG-D", "RESEARCH");
        assertEquals(first, second);
        assertTrue(first.name().startsWith("PSEUDO-"));
        assertTrue(first.idCard().startsWith("PSEUDO-"));
        assertTrue(first.phone().startsWith("PSEUDO-"));
        log.info("[同意边界] 用例=有效同意放行 假名={} 两次输出一致 通过", first.patientRef());
    }
}
