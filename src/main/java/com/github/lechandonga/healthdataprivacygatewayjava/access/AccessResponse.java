package com.github.lechandonga.healthdataprivacygatewayjava.access;

import java.util.Map;

public record AccessResponse(
        String patientId,
        String requestingOrg,
        String purpose,
        String decision,
        String reason,
        boolean identifiable,
        long decidedAt,
        Map<String, Object> view
) {}
