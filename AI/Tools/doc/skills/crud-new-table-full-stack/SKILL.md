---
name: crud-new-table-full-stack
description: >-
  End-to-end workflow for a new MySQL table or aggregate in zta-scm-order-service: DDL → domain +
  infrastructure → api (Feign + Request/DTO) → application (AppService + Assembler) → interfaces
  (Controller). Use when the user asks for 新表全链路、新聚合 CRUD、从建表到对外接口、根据
  init.sql/DDL 生成完整分层代码、头表明细表成套 scaffold、从 SQL 接到 Controller、端到端
  scaffold, or full stack from DDL. Prefer this skill when the task spans multiple layers, not for
  small single-file fixes on existing code.
---

# 新表全链路 CRUD（DDL → 对外接口）

在 **`zta-scm-order-service`** 内为**新表/新聚合**落地标准 CRUD 时，按下列顺序执行；**约束与命名**以仓库 **`.cursor/rules`** 为准，本技能只固定**步骤与交付物清单**。若只是修改已有接口字段、补单个测试、修单点 bug，通常**不要**使用本技能。

## 权威规则（生成与修改时必须遵守）

| 主题 | 规则文件 |
|------|----------|
| 模块与依赖 | `00-project-architecture.mdc` |
| Domain / 枚举 | `java-domain-layer.mdc` |
| DO / Mapper / Repository / SnowFlake | `java-infrastructure-layer.mdc` |
| Feign / Request / DTO | `java-api-contract.mdc` |
| AppService / Assembler / 事务 | `java-application-layer.mdc` |
| Controller | `java-interfaces-layer.mdc` |
| 风格、Java 17、注释、异常、工具类 | 与当前仓库源码风格保持一致 |

## 前置确认（信息不足时先问用户）

1. **业务聚合英文名**（如 `PurchaseOrder`），及 **有界上下文**子包（如 `purchase`，与 `domain` 下现有包一致）。
2. **对外 URL 段**：Feign `path` 与 Controller `@RequestMapping` 须一致，形如 `/zta/scm/order/<kebab-module>`（与 `ReturnOrderApi` 的 `return-order` 风格一致）。
3. **是否只要头表**，还是**头表 + 明细表**一对多（明细会多一套 Request/DTO/仓储方法，参考 `ReturnOrder` / `ReturnOrderItem`）。
4. **DTO 是否含审计展示字段**：若需 `createBy` 等，DTO 继承 `BaseUserNameDTO`（见 `java-api-contract.mdc`）。

---

## 步骤 1：数据库 DDL

1. 在 **`sql/init.sql`** 中新增 `CREATE TABLE`（列注释、必要唯一键、与现有表一致的审计/软删字段习惯）。
2. 表名建议 `t_xxx_yyy`；与 **`BaseDO` / `BaseDomain`** 重复的列不要在业务类重复声明（见技能 **`sql-schema-to-ddd-codegen`**）。

---

## 步骤 2：领域层 + 基础设施层（代码生成）

1. **严格按**项目技能 **`sql-schema-to-ddd-codegen`**（`.cursor/skills/sql-schema-to-ddd-codegen/SKILL.md`）从 DDL 生成：
   - 领域实体、`*DomainPageQuery`（若需要分页列表）、`XxxRepository`、`*RepositoryImpl`
   - `XxxDO`、`XxxMapper`、空 `XxxMapper.xml`
2. **必做**：新 DO 在 **`SnowFlakeIdConfig`** 的 `static` 块注册 `SnowFlakeIdUtil.takePosition(XxxDO.class.getName())`。
3. 枚举列：同步生成 **`common.enums`** 下枚举，DO/领域字段 `@see` 枚举全限定名（见 `java-domain-layer.mdc`）。
4. 仓储接口与实现：至少覆盖 **save / findById / deleteById**；需要列表页则 **pageQuery** + 分页实现模式（`PageUtils.doSelectPage`，见 `java-infrastructure-layer.mdc`）。

---

## 步骤 3：`api` 模块（对外契约）

在 **`com.zta.scm.order.api`** 下按 `java-api-contract.mdc` 增加：

| 交付物 | 说明 |
|--------|------|
| `api.client.XxxApi` | `@FeignClient(name = "zta-scm-order-service", contextId = "camelCase唯一", path = "/zta/scm/order/<kebab>")`；**全部 `@PostMapping`** |
| `*SaveRequest` | 创建/更新合一，`id` 空=创建 |
| `*DeleteRequest` | 含 `id` |
| `*GetRequest` | 单条查询（可选但完整 CRUD 建议有） |
| `*PageQueryRequest` | 继承 `PageRequest`，分页列表 |
| `*DTO` | 返回体；校验使用 **`jakarta.validation`** |

**方法签名示例（与现有工程一致）**：

- `HttpResult<XxxDTO> saveXxx(@RequestBody @Validated XxxSaveRequest request)` → `@PostMapping("/save")`
- `HttpResult<Boolean> deleteXxx(@RequestBody @Validated XxxDeleteRequest request)` → `@PostMapping("/delete")`
- `HttpResult<XxxDTO> getXxxById(@RequestBody @Validated XxxGetRequest request)` → `@PostMapping("/get")`（路径名可按模块调整）
- `HttpResult<PageResult<XxxDTO>> pageXxx(@RequestBody XxxPageQueryRequest request)` → `@PostMapping("/page")`

参考实现：**`ReturnOrderApi`**（`api.returnorder.client`）及 `api.returnorder.request` / `api.returnorder.dto` 包。

---

## 步骤 4：`application` 模块（应用服务）

1. **`XxxAppService`**（`application.<boundedContext>.service`）：方法入参为 **api Request**，返回 **api DTO** 或 **`PageResult<DTO>`**；接口上写**完整 JavaDoc**（步骤编号、`@param`、`@return`），见 `module-rules/java-application-layer.mdc`。
2. **`XxxAppServiceImpl`**：`@Service @RequiredArgsConstructor`；`@Override` **不写 JavaDoc**，可用 `// 1) …` 与接口步骤对齐；业务流：**校验 → 幂等/唯一性 → Assembler → `TransactionTemplate.executeWithoutResult` 写库**；**禁止 `@Transactional`**。
3. **`XxxAssembler`**（`application.<boundedContext>.assembler`）：Request ↔ 领域实体 ↔ DTO；禁止在 Assembler 读 `FrameworkContext`（由 AppService 取上下文后传入），见 `module-rules/java-application-layer.mdc`。
4. 注入 **`XxxRepository`**（领域接口），不在应用层依赖 Mapper/DO。

参考：**`ReturnOrderAppService` / `ReturnOrderAppServiceImpl`** 及对应 Assembler。

---

## 步骤 5：`interfaces` 模块（HTTP 入口）

1. **`XxxController`**：`@RestController @RequiredArgsConstructor`，`@RequestMapping` 与 **Feign `path` 完全相同**。
2. **`implements XxxApi`**：方法体内**仅委派** `XxxAppService`，返回 **`HttpResult.success(...)`**，禁止业务与 DTO 装配，见 `java-interfaces-layer.mdc`。

参考：**`ReturnOrderController`**。

---

## 步骤 6：编译与自检

1. 在仓库根目录执行 **`mvn -q -pl domain,infrastructure,api,application,interfaces -am compile`**（或全量 `mvn compile`），修复依赖方向与编译错误。
2. 对照清单：

- [ ] `SnowFlakeIdConfig` 已注册新 DO  
- [ ] 无 `javax.validation`，仅 **`jakarta.validation`**  
- [ ] 对外接口**全部为 POST**，路径以 **`/zta`** 开头  
- [ ] Controller **实现** Feign 接口且 **`@RequestMapping` 与 `path` 一致**  
- [ ] 写操作在 **`TransactionTemplate`** 中，无 **`@Transactional`**  
- [ ] 类/方法注释与当前仓库风格一致（含 `@author`、`@date`）  

3. **单元测试**：默认不生成，除非用户明确要求。

---

## 可选：调试 HTTP

需要 curl 示例时，使用技能 **`generate-rest-curl`**，基于已生成的 `XxxApi` 与 Request 类构造请求。

---

## 参考聚合（本仓库）

| 层次 | 示例类 |
|------|--------|
| Feign + Request + DTO | `ReturnOrderApi`，`api.returnorder.client` / `api.returnorder.request.*` / `api.returnorder.dto.*` |
| Controller | `ReturnOrderController` |
| AppService | `ReturnOrderAppService`，`ReturnOrderAppServiceImpl` |
| 领域 + 仓储 + DO | `sql-schema-to-ddd-codegen` 中的 `EngineeringProject*` 与 `ReturnOrder*` 相关实现 |
