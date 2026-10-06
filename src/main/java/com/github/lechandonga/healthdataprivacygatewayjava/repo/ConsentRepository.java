package com.github.lechandonga.healthdataprivacygatewayjava.repo;

import com.github.lechandonga.healthdataprivacygatewayjava.domain.Consent;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ConsentRepository {
    private final Map<String, Consent> store = new ConcurrentHashMap<>();

    public void save(Consent c) { store.put(c.scopeKey(), c); }

    public Optional<Consent> find(String patientId, String org, String purpose) {
        return Optional.ofNullable(store.get(patientId + "|" + org + "|" + purpose));
    }

    public Collection<Consent> all() { return List.copyOf(store.values()); }
}
