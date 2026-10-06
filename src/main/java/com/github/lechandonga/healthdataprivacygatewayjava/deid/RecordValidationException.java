package com.github.lechandonga.healthdataprivacygatewayjava.deid;

/**
 * 记录校验失败，携带可区分的拒绝原因。
 */
public class RecordValidationException extends RuntimeException {

    public enum Reason {
        RECORD_NOT_FOUND,
        MISSING_FIELD,
        INVALID_ID_CARD_FORMAT,
        INVALID_PHONE_FORMAT
    }

    private final Reason reason;
    private final String field;

    public RecordValidationException(Reason reason, String field, String message) {
        super(message);
        this.reason = reason;
        this.field = field;
    }

    public Reason getReason() {
        return reason;
    }

    public String getField() {
        return field;
    }
}
