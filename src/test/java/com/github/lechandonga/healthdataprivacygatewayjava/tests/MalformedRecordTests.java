package com.github.lechandonga.healthdataprivacygatewayjava.tests;

import com.github.lechandonga.healthdataprivacygatewayjava.deid.DeidentificationService;
import com.github.lechandonga.healthdataprivacygatewayjava.deid.RecordValidationException;
import com.github.lechandonga.healthdataprivacygatewayjava.domain.PatientRecord;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 业务覆盖：畸形记录的可区分拒绝（缺字段 / 证件号非法 / 手机号非法 / 记录不存在）。
 */
@SpringBootTest
class MalformedRecordTests {

    private static final Logger log = LoggerFactory.getLogger(MalformedRecordTests.class);

    @Autowired
    DeidentificationService deid;

    @Test
    void missingFieldIsRejected() {
        PatientRecord record = new PatientRecord("P100", null, "110101199001011234",
                "13800000001", LocalDate.now(), "感冒");
        RecordValidationException e = assertThrows(RecordValidationException.class,
                () -> deid.deidentify(record));
        assertEquals(RecordValidationException.Reason.MISSING_FIELD, e.getReason());
        assertEquals("name", e.getField());
        log.info("[畸形记录] 用例=缺字段(name) 原因={} 通过", e.getReason());
    }

    @Test
    void invalidIdCardIsRejected() {
        PatientRecord record = new PatientRecord("P101", "测试", "INVALID-ID",
                "13800000001", LocalDate.now(), "感冒");
        RecordValidationException e = assertThrows(RecordValidationException.class,
                () -> deid.deidentify(record));
        assertEquals(RecordValidationException.Reason.INVALID_ID_CARD_FORMAT, e.getReason());
        log.info("[畸形记录] 用例=证件号非法 原因={} 通过", e.getReason());
    }

    @Test
    void invalidPhoneIsRejected() {
        PatientRecord record = new PatientRecord("P102", "测试", "110101199001011234",
                "12345", LocalDate.now(), "感冒");
        RecordValidationException e = assertThrows(RecordValidationException.class,
                () -> deid.deidentify(record));
        assertEquals(RecordValidationException.Reason.INVALID_PHONE_FORMAT, e.getReason());
        log.info("[畸形记录] 用例=手机号非法 原因={} 通过", e.getReason());
    }

    @Test
    void missingRecordIsRejected() {
        RecordValidationException e = assertThrows(RecordValidationException.class,
                () -> deid.deidentify(null));
        assertEquals(RecordValidationException.Reason.RECORD_NOT_FOUND, e.getReason());
        log.info("[畸形记录] 用例=记录不存在 原因={} 通过", e.getReason());
    }
}
