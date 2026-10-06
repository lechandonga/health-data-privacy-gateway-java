# 医疗数据去标识化与访问网关（本地运行）

基于 Spring Boot 3 / Java 21 的本地医疗数据访问网关，用内存数据模拟患者记录、患者同意和访问请求，
在统一的授权判定路径上输出去标识化结果，并把每次访问、授权变更和审计查询写入带哈希链的审计日志。

## 运行与测试

```bash
mvn spring-boot:run                      # 默认 8080，端口被占用时加 --server.port=18080
mvn test                                 # 全量单测，控制台输出“业务覆盖日志”
mvn test -Dtest=ConsentBoundaryTest      # 只跑某一类业务
```

启动时由 `SampleDataInitializer` 注入 6 条患者记录与若干同意（含过期、已撤回样例）。

## HTTP 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/records/{patientId}?org=&purpose=` | 访问患者记录，按同意状态返回可识别/去标识化视图 |
| POST | `/api/consents` | 授予同意，body：`patientId/organization/purpose/ttlSeconds` |
| DELETE | `/api/consents?patientId=&org=&purpose=` | 撤回同意 |
| GET | `/api/audits?patientId=&org=&purpose=AUDIT_QUERY` | 查询审计（走同一套同意判定） |
| GET | `/api/audits/verify` | 校验审计哈希链完整性 |

判定结果：

- 同意有效：`200`，`decision=ALLOWED`，返回可识别视图。
- 同意缺失/过期/撤回/超机构或用途范围：`403`，`decision=DENIED`，
  `reason` 为 `CONSENT_MISSING / CONSENT_EXPIRED / CONSENT_WITHDRAWN / ORG_OUT_OF_SCOPE / PURPOSE_OUT_OF_SCOPE`，
  响应体内仍带一份去标识化视图（`identifiable=false`）。
- 记录缺字段或格式非法：`422`，`decision=REJECTED`，`errors` 给出逐项可区分原因，不静默放过。
- 患者不存在：`403`，`reason=PATIENT_NOT_FOUND`。

## 去标识化规则

实现在 `DeidentificationService`：

- 直接标识符 `name / idCard / phone / email`：任何授权状态下都不以原值出现在输出中。
  - 授权通过时：业务需要可返回原值（可识别视图）。
  - 未授权时：用 `HMAC-SHA256(密钥, 字段:patientId)` 派生确定性假名，
    同一患者跨请求、跨时间的替换结果稳定一致（密钥不变即不变）。
- 准标识符：未授权时出生日期泛化到年份（如 `1990-03-07 → 1990`），地址抹除，治疗方案抹除；
  诊断等非标识业务字段保留。
- 记录校验（拒绝原因互相区分，可同时返回多个）：
  `MISSING_PATIENT_ID / MISSING_NAME / INVALID_ID_CARD / INVALID_PHONE / INVALID_EMAIL / INVALID_BIRTH_DATE`。

## 同意状态流转

同意维度为 `患者 × 机构 × 用途` 三元组，状态：有效 → 过期 / 已撤回。
`ConsentService` 判定优先级：无该患者任何同意 → `CONSENT_MISSING`；无该机构 → `ORG_OUT_OF_SCOPE`；
无该用途 → `PURPOSE_OUT_OF_SCOPE`；已撤回 → `CONSENT_WITHDRAWN`；超过 `expiresAt` → `CONSENT_EXPIRED`；
否则 `GRANTED`。

并发安全：判定 + 输出视图构造与同意授予/撤回共用同一把生命周期锁（`lifecycleLock`）。
撤回操作与访问请求要么整体先于撤回完成（ALLOWED），要么整体在撤回生效后（CONSENT_WITHDRAWN），
不存在“撤回已生效但响应仍带可识别数据”的交错窗口。并发测试为 16 线程 × 20 次访问与撤回竞速，重复 5 轮。

## 审计完整性

每条审计记录包含：`seq（连续递增）、eventType、patientId、requestingOrg、purpose、
decision、reason、timestamp、outputHash（脱敏输出的 SHA-256）、prevHash、hash`。

- 哈希链：`hash_n = HMAC-SHA256(key, prevHash + "|" + 规范化字段串)`，首条的 `prevHash` 为 `GENESIS`。
- 校验（`/api/audits/verify`）先查全局逆序（`OUT_OF_ORDER`），再查序号缺口（`DELETED`），
  最后逐条重算哈希（字段被改/链断裂为 `TAMPERED`），并返回 `firstBadSeq` 与详情。
- 写入在审计日志对象上加锁串行化，序号由单一计数器分配，并发写入不丢记录、不错序。
- 查询审计必须持有该患者对应机构、用途（默认 `AUDIT_QUERY`）的有效同意，
  无授权返回 `decision=DENIED` 且不返回任何记录；查询动作本身也写审计。

## 配置项（`application.properties`，前缀 `hdpg`）

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `hdpg.hmac-key` | 本地演示密钥 | 稳定假名与审计链 HMAC 密钥，生产用 `HDPG_HMAC_KEY` 环境变量覆盖 |
| `hdpg.mask-direct-identifiers` | `true` | 未授权时直接标识符用确定性假名替换，`false` 时整体抹除 |
| `hdpg.direct-identifier-fields` | name,idCard,phone,email | 直接标识符字段清单 |
| `hdpg.generalize-without-consent` | `true` | 未授权时准标识符泛化，`false` 时抹除 |
| `hdpg.quasi-identifier-fields` | birthDate,address | 准标识符字段清单 |
| `hdpg.require-valid-record` | `true` | 是否对畸形/缺字段记录执行拒绝 |
| `hdpg.audit-enabled` | `true` | 是否写审计 |
| `hdpg.audit-query-purpose` | `AUDIT_QUERY` | 查询审计所需的授权用途 |

## 测试与业务覆盖

`mvn test` 通过 JUnit 5 扩展 `BusinessCoverageLogger`（见 `src/test/resources/META-INF/services`
与 `junit-platform.properties`）按业务模块打印用例清单与逐条通过/失败汇总：

| 测试类 | 覆盖业务 |
| --- | --- |
| `ConsentBoundaryTest`（7 例） | 同意有效/缺失/过期/撤回、机构与用途越界、假名跨请求稳定性 |
| `MalformedRecordTest`（4 例） | 非法证件号、缺手机号、非法邮箱、缺姓名、患者不存在 |
| `ConcurrentWithdrawalTest`（1 重复 ×5 + 1 例） | 撤回与访问并发竞速不泄漏可识别数据、撤回时间点后无 ALLOWED 审计 |
| `AuditIntegrityTest`（5 例） | 12 线程 ×25 次并发写入不丢不乱序、字段篡改/删除/乱序检出、审计查询授权 |
