package com.github.lechandonga.healthdataprivacygatewayjava.domain;

public class PatientRecord {
    private String patientId;
    private String name;
    private String idCard;
    private String phone;
    private String email;
    private String birthDate;
    private String gender;
    private String address;
    private String diagnosis;
    private String treatment;

    public PatientRecord() {}

    public PatientRecord(String patientId, String name, String idCard, String phone, String email,
                         String birthDate, String gender, String address, String diagnosis, String treatment) {
        this.patientId = patientId;
        this.name = name;
        this.idCard = idCard;
        this.phone = phone;
        this.email = email;
        this.birthDate = birthDate;
        this.gender = gender;
        this.address = address;
        this.diagnosis = diagnosis;
        this.treatment = treatment;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String v) { this.patientId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String v) { this.idCard = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String v) { this.birthDate = v; }
    public String getGender() { return gender; }
    public void setGender(String v) { this.gender = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { this.address = v; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String v) { this.diagnosis = v; }
    public String getTreatment() { return treatment; }
    public void setTreatment(String v) { this.treatment = v; }
}
