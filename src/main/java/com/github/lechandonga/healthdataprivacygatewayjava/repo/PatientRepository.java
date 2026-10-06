package com.github.lechandonga.healthdataprivacygatewayjava.repo;

import com.github.lechandonga.healthdataprivacygatewayjava.domain.PatientRecord;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PatientRepository {
    private final Map<String, PatientRecord> store = new ConcurrentHashMap<>();

    public void save(PatientRecord r) { store.put(r.getPatientId(), r); }

    public Optional<PatientRecord> findById(String id) { return Optional.ofNullable(store.get(id)); }
}
