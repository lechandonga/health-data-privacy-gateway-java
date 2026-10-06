package com.github.lechandonga.healthdataprivacygatewayjava.tests;

import com.github.lechandonga.healthdataprivacygatewayjava.domain.PatientRecord;
import com.github.lechandonga.healthdataprivacygatewayjava.repo.PatientRepository;
import com.github.lechandonga.healthdataprivacygatewayjava.tests.support.ApiClient;
import com.github.lechandonga.healthdataprivacygatewayjava.tests.support.BusinessCoverage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@BusinessCoverage("畸形/缺字段记录拒绝（拒绝原因可区分）")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MalformedRecordTest {

    @Autowired
    private TestRestTemplate http;
    @Autowired
    private PatientRepository patients;

    @Test
    @SuppressWarnings("unchecked")
    void invalidIdCardIsRejectedWithDistinctReason() {
        patients.save(new PatientRecord("M001", "孙七", "BAD-ID",
                "13800138111", "sunqi@example.com", "1991-02-02", "男",
                "南京市", "感冒", "休息"));
        ResponseEntity<Map> resp = ApiClient.getRecord(http, "M001", "HOSPITAL_A", "TREATMENT");
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertEquals("MALFORMED_RECORD", resp.getBody().get("reason"));
        List<String> errors = (List<String>) resp.getBody().get("errors");
        assertTrue(errors.contains("INVALID_ID_CARD"), () -> "errors=" + errors);
    }

    @Test
    @SuppressWarnings("unchecked")
    void missingPhoneAndBadEmailReportMultipleReasons() {
        PatientRecord r = new PatientRecord();
        r.setPatientId("M002");
        r.setName("周八");
        r.setIdCard("11010119900307123X");
        r.setPhone(null);
        r.setEmail("not-an-email");
        patients.save(r);
        ResponseEntity<Map> resp = ApiClient.getRecord(http, "M002", "HOSPITAL_A", "TREATMENT");
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        List<String> errors = (List<String>) resp.getBody().get("errors");
        assertTrue(errors.contains("INVALID_PHONE"));
        assertTrue(errors.contains("INVALID_EMAIL"));
        assertFalse(errors.contains("MISSING_NAME"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void missingNameReportsMissingName() {
        PatientRecord r = new PatientRecord();
        r.setPatientId("M003");
        r.setIdCard("11010119900307123X");
        r.setPhone("13800138222");
        patients.save(r);
        ResponseEntity<Map> resp = ApiClient.getRecord(http, "M003", "HOSPITAL_A", "TREATMENT");
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        List<String> errors = (List<String>) resp.getBody().get("errors");
        assertTrue(errors.contains("MISSING_NAME"));
    }

    @Test
    void unknownPatientIsRejectedWithNotFoundReason() {
        ResponseEntity<Map> resp = ApiClient.getRecord(http, "NO_SUCH_PATIENT", "HOSPITAL_A", "TREATMENT");
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("PATIENT_NOT_FOUND", resp.getBody().get("reason"));
    }
}
