package com.github.lechandonga.healthdataprivacygatewayjava.domain;

/**
 * 授权判定结果，reason 用于可解释的拒绝。
 */
public record AccessDecision(boolean allowed, String reason) {
    public static AccessDecision allow() {
        return new AccessDecision(true, "ALLOWED");
    }

    public static AccessDecision deny(String reason) {
        return new AccessDecision(false, reason);
    }
}
