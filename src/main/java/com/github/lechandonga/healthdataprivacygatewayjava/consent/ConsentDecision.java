package com.github.lechandonga.healthdataprivacygatewayjava.consent;

public enum ConsentDecision {
    GRANTED,
    CONSENT_MISSING,
    CONSENT_EXPIRED,
    CONSENT_WITHDRAWN,
    ORG_OUT_OF_SCOPE,
    PURPOSE_OUT_OF_SCOPE
}
