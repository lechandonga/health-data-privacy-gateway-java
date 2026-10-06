package com.github.lechandonga.healthdataprivacygatewayjava.data;

import com.github.lechandonga.healthdataprivacygatewayjava.consent.ConsentService;
import com.github.lechandonga.healthdataprivacygatewayjava.domain.PatientRecord;
import com.github.lechandonga.healthdataprivacygatewayjava.repo.PatientRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SampleDataInitializer implements ApplicationRunner {

    private final PatientRepository patients;
    private final ConsentService consents;

    public SampleDataInitializer(PatientRepository patients, ConsentService consents) {
        this.patients = patients;
        this.consents = consents;
    }

    @Override
    public void run(ApplicationArguments args) {
        patients.save(new PatientRecord("P001", "张伟", "11010119900307123X",
                "13800138001", "zhangwei@example.com", "1990-03-07", "男",
                "北京市海淀区学院路1号", "高血压", "降压药治疗"));
        patients.save(new PatientRecord("P002", "李娜", "31010419880715672X",
                "13900139002", "lina@example.com", "1988-07-15", "女",
                "上海市徐汇区漕溪北路2号", "2型糖尿病", "胰岛素治疗"));
        patients.save(new PatientRecord("P003", "王强", "44010619751203341X",
                "13700137003", "wangqiang@example.com", "1975-12-03", "男",
                "广州市天河区体育西路3号", "哮喘", "吸入剂治疗"));
        patients.save(new PatientRecord("P004", "赵敏", "51010419920101882X",
                "13600136004", "zhaomin@example.com", "1992-01-01", "女",
                "成都市锦江区春熙路4号", "偏头痛", "观察随访"));
        patients.save(new PatientRecord("P005", "陈杰", "33010620010505443X",
                "13500135005", "chenjie@example.com", "2001-05-05", "男",
                "杭州市西湖区文三路5号", "过敏性鼻炎", "抗过敏治疗"));
        patients.save(new PatientRecord("P006", "刘洋", "12010119861122114X",
                "13400134006", "liuyang@example.com", "1986-11-22", "男",
                "天津市和平区南京路6号", "慢性胃炎", "饮食调整"));

        long now = System.currentTimeMillis();
        consents.grant("P001", "HOSPITAL_A", "TREATMENT", now, now + 365L * 24 * 3600 * 1000);
        consents.grant("P002", "HOSPITAL_A", "TREATMENT", now, now + 365L * 24 * 3600 * 1000);
        consents.grant("P002", "INSURER_B", "CLAIM", now, now + 30L * 24 * 3600 * 1000);
        consents.grant("P003", "CLINIC_C", "TREATMENT", now, now - 1000L);
        consents.grant("P005", "HOSPITAL_A", "TREATMENT", now, now + 365L * 24 * 3600 * 1000);
        consents.withdraw("P005", "HOSPITAL_A", "TREATMENT", now);
    }
}
