package com.github.lechandonga.healthdataprivacygatewayjava.deid;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 去标识化配置项。
 */
@ConfigurationProperties(prefix = "gateway.deid")
public class DeidProperties {
    /** HMAC 密钥，决定假名替换结果；同一密钥下同一患者输出稳定。 */
    private String hmacSecret = "local-dev-secret";
    /** 假名前缀，便于识别输出为假名。 */
    private String pseudonymPrefix = "PSEUDO";

    public String getHmacSecret() {
        return hmacSecret;
    }

    public void setHmacSecret(String hmacSecret) {
        this.hmacSecret = hmacSecret;
    }

    public String getPseudonymPrefix() {
        return pseudonymPrefix;
    }

    public void setPseudonymPrefix(String pseudonymPrefix) {
        this.pseudonymPrefix = pseudonymPrefix;
    }
}
