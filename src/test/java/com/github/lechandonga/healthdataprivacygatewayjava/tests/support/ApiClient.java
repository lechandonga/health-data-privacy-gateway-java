package com.github.lechandonga.healthdataprivacygatewayjava.tests.support;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public final class ApiClient {

    private ApiClient() {}

    public static ResponseEntity<Map> getRecord(TestRestTemplate http, String pid, String org, String purpose) {
        return http.getForEntity("/api/records/{id}?org={org}&purpose={purpose}",
                Map.class, pid, org, purpose);
    }

    public static ResponseEntity<Map> grant(TestRestTemplate http, String pid, String org,
                                            String purpose, long ttlSeconds) {
        return http.postForEntity("/api/consents",
                Map.of("patientId", pid, "organization", org, "purpose", purpose,
                        "ttlSeconds", ttlSeconds), Map.class);
    }

    public static ResponseEntity<Map> withdraw(TestRestTemplate http, String pid, String org, String purpose) {
        return http.exchange("/api/consents?patientId={id}&org={org}&purpose={purpose}",
                HttpMethod.DELETE, HttpEntity.EMPTY, Map.class, pid, org, purpose);
    }

    @SuppressWarnings("rawtypes")
    public static ResponseEntity<Map> queryAudit(TestRestTemplate http, String pid, String org, String purpose) {
        return http.getForEntity("/api/audits?patientId={id}&org={org}&purpose={purpose}",
                Map.class, pid, org, purpose);
    }

    @SuppressWarnings("rawtypes")
    public static ResponseEntity<Map> verify(TestRestTemplate http) {
        return http.getForEntity("/api/audits/verify", Map.class);
    }
}
