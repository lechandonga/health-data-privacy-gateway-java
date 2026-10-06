package com.github.lechandonga.healthdataprivacygatewayjava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "hdpg")
public class GatewayProperties {

    private String hmacKey = "local-dev-hmac-key-change-me";
    private boolean maskDirectIdentifiers = true;
    private List<String> directIdentifierFields = List.of("name", "idCard", "phone", "email");
    private boolean generalizeWithoutConsent = true;
    private List<String> quasiIdentifierFields = List.of("birthDate", "address");
    private boolean requireValidRecord = true;
    private boolean auditEnabled = true;
    private String auditQueryPurpose = "AUDIT_QUERY";

    public String getHmacKey() { return hmacKey; }
    public void setHmacKey(String hmacKey) { this.hmacKey = hmacKey; }

    public boolean isMaskDirectIdentifiers() { return maskDirectIdentifiers; }
    public void setMaskDirectIdentifiers(boolean v) { this.maskDirectIdentifiers = v; }

    public List<String> getDirectIdentifierFields() { return directIdentifierFields; }
    public void setDirectIdentifierFields(List<String> v) { this.directIdentifierFields = v; }

    public boolean isGeneralizeWithoutConsent() { return generalizeWithoutConsent; }
    public void setGeneralizeWithoutConsent(boolean v) { this.generalizeWithoutConsent = v; }

    public List<String> getQuasiIdentifierFields() { return quasiIdentifierFields; }
    public void setQuasiIdentifierFields(List<String> v) { this.quasiIdentifierFields = v; }

    public boolean isRequireValidRecord() { return requireValidRecord; }
    public void setRequireValidRecord(boolean v) { this.requireValidRecord = v; }

    public boolean isAuditEnabled() { return auditEnabled; }
    public void setAuditEnabled(boolean v) { this.auditEnabled = v; }

    public String getAuditQueryPurpose() { return auditQueryPurpose; }
    public void setAuditQueryPurpose(String v) { this.auditQueryPurpose = v; }
}
