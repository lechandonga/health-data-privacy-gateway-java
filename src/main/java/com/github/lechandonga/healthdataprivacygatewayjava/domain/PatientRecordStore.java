package com.github.lechandonga.healthdataprivacygatewayjava.domain;

import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地模拟的患者记录存储，预置演示数据。
 */
@Repository
public class PatientRecordStore {

    private final Map<String, PatientRecord> records = new ConcurrentHashMap<>();

    public PatientRecordStore() {
        save(new PatientRecord("P001", "张三", "110101199001011234", "13800000001",
                LocalDate.of(1990, 1, 1), "高血压"));
        save(new PatientRecord("P002", "李四", "110101198505054321", "13800000002",
                LocalDate.of(1985, 5, 5), "2型糖尿病"));
        save(new PatientRecord("P003", "王五", "INVALID-ID", "12345",
                LocalDate.of(1978, 9, 9), "哮喘"));
    }

    public void save(PatientRecord record) {
        records.put(record.patientId(), record);
    }

    public Optional<PatientRecord> find(String patientId) {
        return Optional.ofNullable(records.get(patientId));
    }
}
