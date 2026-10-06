package com.github.lechandonga.healthdataprivacygatewayjava.audit;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AuditLog {
    private final List<AuditEntry> entries = new ArrayList<>();
    private long nextSeq = 1;

    public AuditEntry appendNew(AuditEntry entry) {
        synchronized (entries) {
            entry.setSeq(nextSeq++);
            entries.add(entry);
            return entry;
        }
    }

    public List<AuditEntry> snapshot() {
        synchronized (entries) {
            return List.copyOf(entries);
        }
    }

    public int size() {
        synchronized (entries) {
            return entries.size();
        }
    }

    public void corruptEntryAt(int index, java.util.function.Consumer<AuditEntry> mutation) {
        synchronized (entries) {
            mutation.accept(entries.get(index));
        }
    }

    public void deleteEntryAt(int index) {
        synchronized (entries) {
            entries.remove(index);
        }
    }

    public void swapEntries(int i, int j) {
        synchronized (entries) {
            AuditEntry a = entries.get(i);
            entries.set(i, entries.get(j));
            entries.set(j, a);
        }
    }
}
