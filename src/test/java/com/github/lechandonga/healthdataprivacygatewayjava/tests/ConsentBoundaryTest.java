package com.github.lechandonga.healthdataprivacygatewayjava.tests;

import com.github.lechandonga.healthdataprivacygatewayjava.tests.support.ApiClient;
import com.github.lechandonga.healthdataprivacygatewayjava.tests.support.BusinessCoverage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@BusinessCoverage("同意判定边界（授权范围/过期/撤回/稳定性）")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsentBoundaryTest {

    @Autowired
    private TestRestTemplate http;

    @Test
    @SuppressWarnings("unchecked")
    void grantedConsentReturnsIdentifiableData() {
        ResponseEntity<Map> resp = ApiClient.getRecord(http, "P001", "HOSPITAL_A", "TREATMENT");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map<String, Object> body = resp.getBody();
        assertEquals("ALLOWED", body.get("decision"));
        assertEquals(true, body.get("identifiable"));
        Map<String, Object> view = (Map<String, Object>) body.get("view");
        assertEquals("张伟", view.get("name"));
        assertEquals("降压药治疗", view.get("treatment"));
    }

    @Test
    void missingConsentDeniesWithDistinctReason() {
        ResponseEntity<Map> resp = ApiClient.getRecord(http, "P004", "HOSPITAL_A", "TREATMENT");
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("CONSENT_MISSING", resp.getBody().get("reason"));
    }

    @Test
    void expiredConsentDeniesAndReturnsOnlyDeidentifiedData() {
        ResponseEntity<Map> resp = ApiClient.getRecord(http, "P003", "CLINIC_C", "TREATMENT");
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("CONSENT_EXPIRED", resp.getBody().get("reason"));
    }

    @Test
    void withdrawnConsentDeniesWithWithdrawnReason() {
        ResponseEntity<Map> resp = ApiClient.getRecord(http, "P005", "HOSPITAL_A", "TREATMENT");
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("CONSENT_WITHDRAWN", resp.getBody().get("reason"));
    }

    @Test
    void differentOrganizationIsOutOfScope() {
        ResponseEntity<Map> resp = ApiClient.getRecord(http, "P001", "HOSPITAL_X", "TREATMENT");
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("ORG_OUT_OF_SCOPE", resp.getBody().get("reason"));
    }

    @Test
    void differentPurposeIsOutOfScope() {
        ResponseEntity<Map> resp = ApiClient.getRecord(http, "P001", "HOSPITAL_A", "MARKETING");
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("PURPOSE_OUT_OF_SCOPE", resp.getBody().get("reason"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deniedAccessReturnsStablePseudonymsAcrossRequestsAndTime() throws Exception {
        ResponseEntity<Map> first = ApiClient.getRecord(http, "P001", "HOSPITAL_X", "RESEARCH");
        ResponseEntity<Map> second = ApiClient.getRecord(http, "P001", "HOSPITAL_X", "RESEARCH");
        assertEquals(HttpStatus.FORBIDDEN, first.getStatusCode());

        Map<String, Object> v1 = (Map<String, Object>) first.getBody().get("view");
        Thread.sleep(20);
        Map<String, Object> v2 = (Map<String, Object>) second.getBody().get("view");
        assertEquals(v1.get("name"), v2.get("name"));
        assertEquals(v1.get("idCard"), v2.get("idCard"));
        assertEquals(v1.get("phone"), v2.get("phone"));
        assertEquals(v1.get("email"), v2.get("email"));
        assertNotEquals("张伟", v1.get("name"));
        assertEquals("1990", v1.get("birthDate"));
        assertEquals("[REDACTED]", v1.get("address"));
        assertEquals("[REDACTED]", v1.get("treatment"));
    }
}
