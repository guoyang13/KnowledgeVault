---
name: sql-schema-to-ddd-codegen
description: Generates zta-scm-order-service domain model, repository interface, DO, MyBatis Mapper, empty Mapper.xml, and RepositoryImpl baseline from MySQL DDL (especially sql/init.sql). Use when adding or changing tables in init.sql, syncing persistence layers with schema, or when the user asks to generate infrastructure and domain code from SQL table definitions.
---

# SQL 表结构 → 领域层与基础设施层基础代码

根据 **`sql/init.sql`**（或用户粘贴的等价 `CREATE TABLE` DDL）生成与本工程一致的**领域层 + 基础设施层**起步代码。分层边界以 **`project-rules/00-project-architecture.mdc`** 和 **`module-rules/java-infrastructure-layer.mdc`**、**`module-rules/java-domain-layer.mdc`** 为准；本技能只固化**从表到类的映射与工作流**。

## 前置：读什么

1. 优先读取 **`sql/init.sql`** 中目标表的 `CREATE TABLE`（含列注释、索引、唯一约束）。
2. 若不涉及 init.sql，使用用户提供的 DDL 片段，字段与注释以 DDL 为准。

## 命名与分包

| DDL | Java |
|-----|------|
| 表 `t_xxx_yyy` | 领域/DO 主类名：`XxxYyy`（去掉 `t_` 前缀，蛇形转 PascalCase） |
| 领域实体 | `com.zta.scm.order.domain.<boundedContext>.model.XxxYyy` |
| 仓储接口 | `com.zta.scm.order.domain.<boundedContext>.repository.XxxYyyRepository` |
| DO | `com.zta.scm.order.infrastructure.persistence.entity.XxxYyyDO` |
| Mapper | `com.zta.scm.order.infrastructure.persistence.mapper.XxxYyyMapper` |
| Mapper XML | `infrastructure/src/main/resources/mapper/XxxYyyMapper.xml`（**空 `<mapper>`**，不写业务 SQL） |
| 仓储实现 | `com.zta.scm.order.infrastructure.persistence.repository.XxxYyyRepositoryImpl` |

**有界上下文 `<boundedContext>`**：由表业务含义选定（如采购 `purchase`、销售 `sales`、包裹 `package`），与现有 `domain` 子包风格一致；新聚合勿塞进无关包。

## 类型映射（MySQL → Java）

| SQL | Java |
|-----|------|
| `bigint`（含 unsigned） | `Long` |
| `int` / `integer` | `Integer` |
| `tinyint` | `Integer`（本工程状态/标志多用整型；勿擅自改为 `Boolean` 除非表注释明确布尔且团队已统一） |
| `varchar` / `char` / `text` | `String` |
| `decimal` | `BigDecimal` |
| `datetime` | `java.util.Date` |
| `date` | `java.util.Date` |

## BaseDO / BaseDomain 字段（不要重复声明）

`DO` 继承 **`com.blueorigin.framework.support.dal.BaseDO`**，`领域实体` 继承 **`com.blueorigin.framework.support.domain.BaseDomain`**。下列列若 DDL 中存在，**不要**在子类再写字段，依赖基类即可（与现有 `*DO` / 领域模型一致）：

- `id`, `deleted`, `create_time`, `update_time`, `create_by`, `update_by`, `version`, `sharding`

其余业务列在 DO 与领域实体中**一一对应**（同名 camelCase 属性，便于 `BeanCopierUtil.copy`）。

## DO 约定

- 生成新 `XxxYyyDO` 后，在 **`infrastructure/.../config/SnowFlakeIdConfig.java`** 的 `static` 块中增加：`SnowFlakeIdUtil.takePosition(XxxYyyDO.class.getName());`（与本仓库既有 DO 注册方式一致）。
- `@Data`、`@EqualsAndHashCode(callSuper = true)`
- `@TableName(value = "<实际表名>", autoResultMap = true)` — 表名与 DDL 中一致（可带库前缀若工程惯例如此；以现有 `*DO` 为准）
- 每个非基类字段：`@TableField("<snake_column>")`，Java 属性 **camelCase**
- 字段 JavaDoc：摘自列 `comment`；若该列为枚举语义，须**同时生成** `com.zta.scm.order.common.enums` 下对应 `XxxEnum`（见 `module-rules/java-domain-layer.mdc`「枚举规范」）；DO/领域实体字段 Java 类型仍按列 SQL 类型映射，并写 `@see` 枚举全限定名（勿在 domain 引用 MyBatis）

## 领域实体约定

- `@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor`、`@EqualsAndHashCode(callSuper = true)`
- 仅业务字段 + JavaDoc；**无** MyBatis 注解
- 枚举语义：`@see` 指向 `common.enums` 中枚举；若由 DDL 新识别出的枚举字段，须与 DO **一并生成**该枚举类（不得只留占位注释）；领域字段类型与 DO 一致（存库类型 + `@see`）

## 仓储接口（最小集 + 扩展）

为新聚合生成接口，至少包含：

- `XxxYyy save(XxxYyy entity)`  
- `Optional<XxxYyy> findById(Long id)`  
- `boolean deleteById(Long id)`  

按 DDL **补充只读查询**（方法名表达语义）：

- **唯一键 / 联合唯一**：如 `uk_po_no(po_no)` → `Optional<XxxYyy> findByPoNo(String poNo)`  
- 明细表常见：`List<XxxYyy> findByParentNo(String parentNo)`（列名按业务改为 `poNo` / `soNo` / `packageNo` 等）

**分页**：若用户需要列表页，增加 `PageResult<XxxYyy> pageQuery(XxxYyyDomainPageQuery query)`，并在 `domain...model` 下新增 `XxxYyyDomainPageQuery`，分页实现使用 **`PageUtils.doSelectPage`**（见 `module-rules/java-infrastructure-layer.mdc`）。只做“基础代码”时，可仅加接口方法 + 实现类中空 `throw new UnsupportedOperationException` 或待补充的 wrapper，但**优先**一次给齐与现有 `EngineeringProject` 同级的分页骨架。

## 仓储实现约定

- `@Repository`，`class XxxYyyRepositoryImpl extends ServiceImpl<XxxYyyMapper, XxxYyyDO> implements XxxYyyRepository`
- `save`：`convertToDO` → `saveOrUpdate` → `convertToDomain`
- `findById`：`getById` + `Optional`
- `deleteById`：`removeById`
- 唯一键查询：`lambdaQuery().eq(XxxYyyDO::getField, ...).one()`
- `convertToDO` / `convertToDomain`：`BeanCopierUtil.copy`，空指针防护与现有仓储一致
- **批量按 id 查询**：若接口提供 `findByIds`，用 `Lists.partition` + `PageConstants.PAGE_SIZE` 分批 `listByIds`（与 `EngineeringProjectRepositoryImpl` 一致）

## Mapper

- `interface XxxYyyMapper extends BaseMapper<XxxYyyDO>` + `@Mapper`
- 配套 XML：`namespace` 为 Mapper 全限定名，**体为空**

## 生成后自检清单

- [ ] 模块：`domain` 仅接口与模型；`infrastructure` 仅 DO、Mapper、Impl  
- [ ] 新 DO 已在 `SnowFlakeIdConfig` 中追加 `SnowFlakeIdUtil.takePosition(...)`  
- [ ] DO/领域类未重复 BaseDO/BaseDomain 字段  
- [ ] 依赖方向未反转（infra 实现 domain 仓储接口）  
- [ ] 未在 Mapper.xml 写业务 SQL  
- [ ] DDL 含枚举语义列时，已生成 `common.enums` 下对应枚举，且 DO/领域字段已 `@see` 该枚举  
- [ ] 类/方法 JavaDoc、`@author` / `@date` 与当前仓库现有代码风格一致  
- [ ] **默认不生成测试类**（除非用户明确要求）

## 参考实现（本仓库）

生成风格对齐现有工程示例（命名与转换方式以这些文件为准）：

- `domain/.../project/model/EngineeringProject.java`
- `domain/.../project/repository/EngineeringProjectRepository.java`
- `infrastructure/.../persistence/entity/PurchaseOrderDO.java`
- `infrastructure/.../persistence/mapper/PurchaseOrderMapper.java`
- `infrastructure/.../persistence/repository/PurchaseOrderRepositoryImpl.java`

## `sql/init.sql` 当前表与建议 boundedContext

| 表名 | 建议 `boundedContext` |
|------|------------------------|
| `t_purchase_order`, `t_purchase_order_item` | `purchase` |
| `t_sales_order`, `t_sales_order_item` | `sales` |
| `t_package`, `t_package_item` | `package` |

订单头/明细：可共用同一上下文包，实体名分别为 `PurchaseOrder` / `PurchaseOrderItem` 等。
