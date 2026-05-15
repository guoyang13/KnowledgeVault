---
name: summarize-code-changes-to-rules
description: Summarizes recent code or chat-driven adjustments into updates for current Cursor rules under `.cursor/rules/project-rules` and `.cursor/rules/module-rules`, preserving frontmatter, avoiding duplicate rules, and updating cross-links. Use when the user asks to 总结代码调整、沉淀生成规则、更新 Cursor 规则、把改动写入规则文件、codify patterns after a refactor, or to turn a session’s conventions into persistent rule or skill guidance.
---

# 代码调整沉淀为 Cursor 规则

## 目标

把**本轮对话中已落地的代码调整**、**用户明确补充的长期约定**、或**重复出现的生成模式**，提炼成可复用的短规则，并合并进当前仓库的规则体系：

- 项目级规则：`.cursor/rules/project-rules/`
- 分层规则：`.cursor/rules/module-rules/`

默认做**增量修改**，不要用聊天长文整段覆盖规则全文。

## 输入来源（按优先级）

1. **Git 工作区**：`git diff`、`git status` 中改动的 Java / SQL / 测试 / 规则 / Skill。
2. **用户说明**：用户点名的「必须遵守」「以后都这样做」等约束。
3. **已打开或引用的源码**：命名、目录、注解、事务、DTO/DO/Assembler 结构等具体模式。

若以上不足以还原意图，再向用户确认「要固化的是哪几条」。

## 先判断：写 Rule 还是写 Skill

- **写 Rule**：长期默认约束、命名规范、目录边界、禁止项、契约约定。
- **写 Skill**：步骤型工作流、只有在特定任务下才触发的操作手册。
- **两者都需要**：Rule 写约束，Skill 写步骤并引用对应 Rule。

若某条内容更像「新表全链路怎么做」「如何生成 curl」，优先考虑沉淀为 Skill，而不是塞进 `.mdc`。

## 落盘位置与分工

| 内容类型 | 优先写入 |
|----------|----------|
| 模块职责、依赖方向、包边界、Maven 发布范围 | `project-rules/00-project-architecture.mdc` |
| Java 通用风格、OpenJDK 17、import、异常、日志、测试 | `project-rules/00-project-architecture.mdc` 或按需扩展 |
| Java 注释 / JavaDoc / **必要日志（SLF4J）** 全仓标准 | **`project-rules/03-java-comments-and-javadoc.mdc`** |
| 分支、commit message、PR、push 禁令 | `project-rules/02-git-workflow.mdc` |
| Feign、Request/DTO、路径、Bean Validation、返回值 | `module-rules/java-api-contract.mdc` |
| AppService、Assembler、事务、开放平台请求原样落库 | `module-rules/java-application-layer.mdc` |
| 领域实体、仓储接口、领域服务、枚举 | `module-rules/java-domain-layer.mdc` |
| DO、Mapper、RepositoryImpl、SnowFlake、分页 | `module-rules/java-infrastructure-layer.mdc` |
| Controller、Feign 实现、接口委派 | `module-rules/java-interfaces-layer.mdc` |
| 独立主题且不适合塞入现有规则 | 在 `project-rules/` 或 `module-rules/` 下新建 `*.mdc`，并补交叉引用 |

选择新增文件时：

- **项目级、全仓默认约定** → `project-rules/`
- **某一层 Java 代码的行为约束** → `module-rules/`

## 编写规则时的原则

- **增量合并**：优先扩展现有小节；不要无故新开并列章节。
- **避免重复**：写入前先检查目标规则是否已有同义条目；若只是换个说法，优先修改旧条目而不是再加一条。
- **可执行**：每条规则都应回答“放哪、叫什么、要/不要做什么”。
- **术语统一**：模块名、包名、类名后缀与现有规则保持一致，如 `*AppService`、`*Assembler`、`*RepositoryImpl`。
- **遵守现状**：若仓库代码与旧规则冲突，以**当前已落地且稳定的代码模式**为准，谨慎清理过时规则。
- **frontmatter 保持有效**：维护 `description`、`globs`、`alwaysApply`，修改内容后同步检查 `description` 是否仍准确。

## 建议工作流

1. **列改动**：文件路径 + 一两句“改了什么”。
2. **抽模式**：归纳 3～10 条可复用约定，优先提取命名、边界、禁止项、默认值、路径习惯、异常行为。
3. **判类型**：先判断是 Rule、Skill，还是 Rule + Skill。
4. **选文件**：按上表映射到对应 `project-rules/*.mdc`、`module-rules/*.mdc` 或 `.cursor/skills/*/SKILL.md`。
5. **查重复**：确认目标规则里是否已有近义条目；必要时合并、删重、改标题。
6. **落规则**：插入新小节或扩展现有小节，保持现有标题层级与语气一致。
7. **补引用**：若改了文件名、目录、规则职责，必须同步更新：
   - 其它 `.mdc` 中的交叉引用
   - `.cursor/skills/*/SKILL.md` 中对规则文件的引用
   - `description` 中提到的旧文件名
8. **自检**：新用户只看规则能否复现相同结构；是否引入重复规则；是否与当前目录结构一致。

## 典型示例

### 示例 1：新增 SQL 表并生成全链路代码

如果本轮改动包含 `sql/init.sql`、`domain` / `infrastructure` / `api` / `application` / `interfaces` 多层新增代码：

- 表结构、DO、Mapper、RepositoryImpl、SnowFlake → `module-rules/java-infrastructure-layer.mdc`
- 实体、仓储接口、枚举 → `module-rules/java-domain-layer.mdc`
- Feign、Request/DTO、路径、Bean Validation → `module-rules/java-api-contract.mdc`
- AppService、Assembler、事务 → `module-rules/java-application-layer.mdc`
- Controller 实现 Feign、仅做委派 → `module-rules/java-interfaces-layer.mdc`
- 若形成稳定的“从 DDL 到接口”的步骤型流程，同时更新对应 Skill（如 CRUD 全链路 Skill）

### 示例 2：统一接口路径风格

如果本轮把接口路径从 camelCase 统一为 kebab-case（如 `returnOrder` → `return-order`）：

- Feign `path` 约定 → `module-rules/java-api-contract.mdc`
- Controller `@RequestMapping` 与 Feign 保持一致 → `module-rules/java-interfaces-layer.mdc`
- 若已有旧示例代码，优先改旧示例，不新增平行规则

### 示例 3：补充通用 Java 编码约定

如果本轮新增的是 OpenJDK 17、注释、JavaDoc、日志、import 顺序、异常处理等横切约定：

- **注释 / JavaDoc / SLF4J 必要日志标准** → `project-rules/03-java-comments-and-javadoc.mdc`（与 `module-rules/java-application-layer.mdc`、`java-infrastructure-layer.mdc` 中 MQ 等条目配合，避免重复大段）
- 其它横切风格 → `project-rules/00-project-architecture.mdc` 或按需新建 `project-rules/*.mdc`
- 若是某一层专属规则（如 Controller 只做委派），写入对应 module rule，不要与全仓注释规则重复矛盾

### 示例 4：新增 Git / 提交规范

如果本轮新增的是分支、commit message、PR、push 禁令等协作约定：

- 写入 `project-rules/02-git-workflow.mdc`
- 若只影响某个 Skill 的使用方式，不要误写到 Java 分层规则中

### 示例 5：发现重复规则

如果同一约定已出现在多个规则文件中：

- 优先保留“职责更贴近”的那一份
- 另一份改成交叉引用，或删除重复条目
- 例如：HTTP 方法归 `java-api-contract.mdc`；Controller 仅保留“实现 Feign + 委派 AppService”

## 建议输出给用户的结果

完成后，优先用下面的格式汇报：

1. 本次识别出的可沉淀约定
2. 每条约定写入了哪个 Rule / Skill
3. 是否清理了重复规则或过期表述
4. 是否同步更新了交叉引用

## 不要做的事

- 不要把一次 PR 的逐文件说明原样贴进规则。
- 不要只在对话里输出“规则建议”，却跳过对 `.cursor/rules` / `.cursor/skills` 的实际更新，除非用户明确只要讨论稿。
- 不要继续引用已经废弃的旧文件名或旧目录结构。
- 不要把步骤型工作流硬塞进 `.mdc`，也不要把长期默认约束误沉淀成 Skill。
- 不要引入与项目 `pom` 模块边界冲突的新分层表述。

## 与用户确认

若要沉淀的内容涉及团队未公开的约定、存在多种合法实现、或会影响多个现有规则文件，在写入前用一句话向用户确认后再落盘。
