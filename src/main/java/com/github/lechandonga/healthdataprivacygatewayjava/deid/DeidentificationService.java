package com.github.lechandonga.healthdataprivacygatewayjava.deid;

import com.github.lechandonga.healthdataprivacygatewayjava.config.GatewayProperties;
import com.github.lechandonga.healthdataprivacygatewayjava.domain.PatientRecord;
import com.github.lechandonga.healthdataprivacygatewayjava.security.HmacSupport;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class DeidentificationService {

    public static final String MISSING_NAME = "MISSING_NAME";
    public static final String MISSING_PATIENT_ID = "MISSING_PATIENT_ID";
    public static final String INVALID_ID_CARD = "INVALID_ID_CARD";
    public static final String INVALID_PHONE = "INVALID_PHONE";
    public static final String INVALID_EMAIL = "INVALID_EMAIL";
    public static final String INVALID_BIRTH_DATE = "INVALID_BIRTH_DATE";

    private static final Pattern ID_CARD = Pattern.compile("^\\d{17}[\\dXx]$");
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private final GatewayProperties props;
    private final HmacSupport hmac;

    public DeidentificationService(GatewayProperties props, HmacSupport hmac) {
        this.props = props;
        this.hmac = hmac;
    }

    public List<String> validate(PatientRecord r) {
        java.util.ArrayList<String> errors = new java.util.ArrayList<>();
        if (isBlank(r.getPatientId())) errors.add(MISSING_PATIENT_ID);
        if (isBlank(r.getName())) errors.add(MISSING_NAME);
        if (isBlank(r.getIdCard()) || !ID_CARD.matcher(r.getIdCard()).matches()) errors.add(INVALID_ID_CARD);
        if (isBlank(r.getPhone()) || !PHONE.matcher(r.getPhone()).matches()) errors.add(INVALID_PHONE);
        if (!isBlank(r.getEmail()) && !EMAIL.matcher(r.getEmail()).matches()) errors.add(INVALID_EMAIL);
        if (!isBlank(r.getBirthDate()) && !DATE.matcher(r.getBirthDate()).matches()) errors.add(INVALID_BIRTH_DATE);
        return List.copyOf(errors);
    }

    public Map<String, Object> deidentify(PatientRecord r, boolean identifiable) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("patientId", r.getPatientId());

        if (identifiable) {
            view.put("name", r.getName());
            view.put("idCard", r.getIdCard());
            view.put("phone", r.getPhone());
            view.put("email", r.getEmail());
            view.put("birthDate", r.getBirthDate());
            view.put("gender", r.getGender());
            view.put("address", r.getAddress());
            view.put("diagnosis", r.getDiagnosis());
            view.put("treatment", r.getTreatment());
            view.put("identifiable", true);
            return view;
        }

        if (props.isMaskDirectIdentifiers()) {
            view.put("name", hmac.stableToken("name", r.getPatientId()));
            view.put("idCard", hmac.stableToken("idCard", r.getPatientId()));
            view.put("phone", hmac.stableToken("phone", r.getPatientId()));
            String email = r.getEmail();
            view.put("email", email == null || email.isBlank()
                    ? hmac.stableToken("email", r.getPatientId())
                    : hmac.stableToken("email", r.getPatientId()));
        } else {
            view.put("name", "[REDACTED]");
            view.put("idCard", "[REDACTED]");
            view.put("phone", "[REDACTED]");
            view.put("email", "[REDACTED]");
        }

        if (props.isGeneralizeWithoutConsent()) {
            view.put("birthDate", generalizeYear(r.getBirthDate()));
            view.put("gender", r.getGender());
            view.put("address", "[REDACTED]");
        } else {
            view.put("birthDate", "[REDACTED]");
            view.put("gender", "[REDACTED]");
            view.put("address", "[REDACTED]");
        }
        view.put("diagnosis", r.getDiagnosis());
        view.put("treatment", "[REDACTED]");
        view.put("identifiable", false);
        return view;
    }

    private String generalizeYear(String birthDate) {
        if (birthDate == null || birthDate.length() < 4) return "[REDACTED]";
        return birthDate.substring(0, 4);
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}
