package com.github.lechandonga.healthdataprivacygatewayjava.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class HmacSupport {

    private static final String GENESIS = "GENESIS";

    private final byte[] key;

    public HmacSupport(com.github.lechandonga.healthdataprivacygatewayjava.config.GatewayProperties props) {
        this.key = props.getHmacKey().getBytes(StandardCharsets.UTF_8);
    }

    public String stableToken(String field, String patientId) {
        String h = hmacHex(field + ":" + patientId);
        return switch (field) {
            case "name" -> "患者-" + h.substring(0, 10).toUpperCase();
            case "idCard" -> "ID-" + h.substring(0, 16).toUpperCase();
            case "phone" -> "139" + h.substring(0, 8);
            case "email" -> h.substring(0, 12) + "@pseudonym.local";
            default -> "TOK-" + h.substring(0, 16).toUpperCase();
        };
    }

    public String chainHash(String canonicalPayload, String prevHash) {
        return hmacHex((prevHash == null || prevHash.isBlank()) ? GENESIS : prevHash + "|" + canonicalPayload);
    }

    public String digest(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String hmacHex(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
