package com.github.lechandonga.healthdataprivacygatewayjava.access;

import java.util.Map;

public class AccessDeniedException extends RuntimeException {
    private final String reason;
    private final Map<String, Object> view;

    public AccessDeniedException(String reason, Map<String, Object> view) {
        super(reason);
        this.reason = reason;
        this.view = view;
    }

    public String getReason() { return reason; }
    public Map<String, Object> getView() { return view; }
}
