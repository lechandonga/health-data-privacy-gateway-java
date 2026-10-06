package com.github.lechandonga.healthdataprivacygatewayjava.domain;

import java.time.LocalDate;

/**
 * 本地模拟的患者记录。直接标识符：name / idCard / phone。
 */
public record PatientRecord(
        String patientId,
        String name,
        String idCard,
        String phone,
        LocalDate birthDate,
        String diagnosis
) {
}
