package com.github.lechandonga.healthdataprivacygatewayjava.deid;

import com.github.lechandonga.healthdataprivacygatewayjava.domain.DeidentifiedRecord;
import com.github.lechandonga.healthdataprivacygatewayjava.domain.PatientRecord;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * 去标识化服务：
 * - 先校验记录完整性/格式，缺字段或格式非法直接抛出带原因的异常；
 * - 直接标识符（姓名/证件号/联系方式）用 HMAC-SHA256 派生的稳定假名替换，
 *   同一患者 + 同一密钥下跨请求、跨时间输出一致。
 */
@Service
public class DeidentificationService {

    private static final Pattern ID_CARD = Pattern.compile("^\\d{17}[\\dXx]$");
    private static final Pattern PHONE = Pattern.compile("^1\\d{10}$");

    private final DeidProperties properties;

    public DeidentificationService(DeidProperties properties) {
        this.properties = properties;
    }

    public DeidentifiedRecord deidentify(PatientRecord record) {
        validate(record);
        String ref = pseudonym(record.patientId());
        return new DeidentifiedRecord(
                ref,
                pseudonym(record.patientId() + "|name"),
                pseudonym(record.patientId() + "|idCard"),
                pseudonym(record.patientId() + "|phone"),
                record.birthDate(),
                record.diagnosis()
        );
    }

    private void validate(PatientRecord record) {
        if (record == null) {
            throw new RecordValidationException(
                    RecordValidationException.Reason.RECORD_NOT_FOUND, null, "患者记录不存在");
        }
        requireField(record.patientId(), "patientId");
        requireField(record.name(), "name");
        requireField(record.idCard(), "idCard");
        requireField(record.phone(), "phone");
        if (!ID_CARD.matcher(record.idCard()).matches()) {
            throw new RecordValidationException(
                    RecordValidationException.Reason.INVALID_ID_CARD_FORMAT, "idCard",
                    "证件号格式非法: 需为18位身份证号");
        }
        if (!PHONE.matcher(record.phone()).matches()) {
            throw new RecordValidationException(
                    RecordValidationException.Reason.INVALID_PHONE_FORMAT, "phone",
                    "联系方式格式非法: 需为11位手机号");
        }
    }

    private void requireField(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new RecordValidationException(
                    RecordValidationException.Reason.MISSING_FIELD, field, "缺少必填字段: " + field);
        }
    }

    private String pseudonym(String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getHmacSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String hex = HexFormat.of().formatHex(mac.doFinal(key.getBytes(StandardCharsets.UTF_8)));
            return properties.getPseudonymPrefix() + "-" + hex.substring(0, 16);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC 初始化失败", e);
        }
    }
}
