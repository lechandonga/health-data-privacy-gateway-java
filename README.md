# 医疗数据去标识化与访问网关

本地运行的 Spring Boot 服务，用内存数据模拟患者记录、同意状态与访问请求，
对外提供 HTTP 接口，保证脱敏结果、授权判定与审计记录在各访问路径下一致、可解释、可追溯。

## 运行

```bash
mvn spring-boot:run   # 默认端口 8080
mvn test              # 运行单测，日志中含各业务用例覆盖情况
```

## HTTP 接口

| 接口 | 说明 |
| --- | --- |
| `GET /api/records/{patientId}?orgId=&purpose=` | 授权判定 + 去标识化输出（Header `X-Actor` 标识访问者） |
| `POST /api/consents/grant?patientId=&orgId=&purpose=&expiresAt=` | 授予同意（`expiresAt` 可省，ISO-8601） |
| `POST /api/consents/revoke?patientId=&orgId=&purpose=` | 撤回同意 |
| `GET /api/audit` | 查询审计日志（需 Header `X-Access-Role: AUDITOR`） |
| `GET /api/audit/verify` | 校验审计链完整性（同上授权） |

内置演示患者：`P001`、`P002`（合法记录），`P003`（证件号/手机号非法，用于演示拒绝）。

## 去标识化规则

- 直接标识符（姓名 `name`、证件号 `idCard`、联系方式 `phone`）一律替换为假名：
  `PSEUDO-<HMAC-SHA256(patientId|字段名) 前16位hex>`。
- 同一患者 + 同一密钥下，假名跨请求、跨时间稳定一致；更换 `gateway.deid.hmac-secret`
  即整体轮换假名空间。
- 输出前先做记录校验，缺字段或格式非法不静默放过，返回 422 及可区分原因：
  - `MISSING_FIELD`（附字段名）
  - `INVALID_ID_CARD_FORMAT`（须为 18 位身份证号）
  - `INVALID_PHONE_FORMAT`（须为 11 位手机号）
  - `RECORD_NOT_FOUND`

## 同意状态流转

- 同意按 `(patientId, orgId, purpose)` 三元组限定范围，机构与用途任一不匹配即视为无授权。
- 状态机：`ACTIVE` →（到期）`EXPIRED`；`ACTIVE` →（撤回）`REVOKED`。`EXPIRED`/`REVOKED` 不可逆。
- 判定拒绝原因可解释：`CONSENT_MISSING` / `CONSENT_EXPIRED` / `CONSENT_REVOKED`（HTTP 403）。
- 并发安全：访问路径在同一把患者级锁内完成「判定 → 取数 → 脱敏 → 审计」，
  撤回也走同一把锁，因此撤回生效后不会再有可识别数据流出。

## 审计与完整性校验

- 每次访问（含拒绝、记录非法）、同意授予/撤回、审计查询本身都会追加审计事件。
- 事件构成 SHA-256 哈希链：`hash(n) = SHA256(payload(n) | hash(n-1))`，序号从 1 严格递增。
- `GET /api/audit/verify` 重放校验三类违规：
  - `HASH_MISMATCH`：历史事件内容被篡改；
  - `SEQ_GAP`：事件被删除或序号不连续；
  - `CHAIN_BREAK`：事件被乱序或 prevHash 衔接被破坏。
- 追加在内部锁内完成，并发写入不丢记录、不错序。
- 审计查询接口自身也走授权判定：必须携带 `X-Access-Role: AUDITOR`，否则 403 并留痕。

## 配置项（application.properties）

| 配置 | 默认 | 说明 |
| --- | --- | --- |
| `gateway.deid.hmac-secret` | `local-dev-secret` | 假名派生密钥，决定替换结果 |
| `gateway.deid.pseudonym-prefix` | `PSEUDO` | 假名前缀 |
| `gateway.audit.auditor-role` | `AUDITOR` | 查询审计所需角色 |

## 测试覆盖

| 测试类 | 业务场景 |
| --- | --- |
| `ConsentBoundaryTests` | 同意缺失 / 过期 / 撤回 / 超范围机构与用途 / 有效同意放行且假名稳定 |
| `MalformedRecordTests` | 缺字段、证件号非法、手机号非法、记录不存在的可区分拒绝 |
| `ConcurrentRevokeTests` | 16 线程并发访问与撤回竞争，撤回生效后零放行 |
| `AuditIntegrityTests` | 篡改 / 删除 / 乱序可检出；8 线程并发写入不丢不错序 |
| `AuditApiAuthorizationTests` | 审计查询接口的授权判定（无角色 403 / 审计角色放行） |
