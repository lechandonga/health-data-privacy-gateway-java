package com.github.lechandonga.healthdataprivacygatewayjava.api;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String reason) {
        super(reason);
    }
}
