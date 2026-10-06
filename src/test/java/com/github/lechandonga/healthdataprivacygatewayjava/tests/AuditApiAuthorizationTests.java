package com.github.lechandonga.healthdataprivacygatewayjava.tests;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 业务覆盖：审计查询接口自身的授权判定（无角色拒绝 / 审计角色放行）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuditApiAuthorizationTests {

    private static final Logger log = LoggerFactory.getLogger(AuditApiAuthorizationTests.class);

    @Autowired
    MockMvc mvc;

    @Test
    void auditQueryWithoutRoleIsDenied() throws Exception {
        mvc.perform(get("/api/audit").header("X-Actor", "intruder"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
        log.info("[审计查询授权] 用例=无角色查询审计 结果=403拒绝 通过");
    }

    @Test
    void auditQueryWithAuditorRoleIsAllowed() throws Exception {
        mvc.perform(get("/api/audit").header("X-Access-Role", "AUDITOR").header("X-Actor", "auditor-1"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/audit/verify").header("X-Access-Role", "AUDITOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intact").value(true));
        log.info("[审计查询授权] 用例=审计角色查询与校验 结果=放行且链完整 通过");
    }
}
