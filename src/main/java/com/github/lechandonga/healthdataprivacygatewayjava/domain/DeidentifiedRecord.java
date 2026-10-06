package com.github.lechandonga.healthdataprivacygatewayjava.domain;

import java.time.LocalDate;

/**
 * 去标识化后的输出：直接标识符被稳定假名替换。
 */
public record DeidentifiedRecord(
        String patientRef,
        String name,
        String idCard,
        String phone,
        LocalDate birthDate,
        String diagnosis
) {
}
