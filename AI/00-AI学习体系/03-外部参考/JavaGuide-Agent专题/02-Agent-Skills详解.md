---
aliases:
  - JavaGuide Agent Skills
tags:
  - 外部参考
  - JavaGuide
  - Agent
  - Skills
source: https://javaguide.cn/ai/agent/skills.html
updated: 2026-07-10
---

# Agent Skills 详解（JavaGuide）

> 原文：[Agent Skills 是什么？](https://javaguide.cn/ai/agent/skills.html) · 约 8898 字
>
> 导航：[[AI/00-AI学习体系/03-外部参考/JavaGuide-Agent专题/00-JavaGuide-Agent专题-导航|JavaGuide Agent 专题导航]]
>
> 本地对照：[[AI/00-AI学习体系/02-概念库/03-Agent系统/10-Agent Skills与协议生态|Agent Skills与协议生态]]

## 定义

**Skill = 可被 Agent 发现、按需加载的任务说明**，沉淀某类任务的经验、约束和执行顺序。

类比：把「老员工脑子里的规矩」写进 `SKILL.md`，合适任务时再读。

---

## 与 Prompt / MCP / Function Calling 的分工

```text
Prompt           → 用户这次要做什么
Function Calling → 模型怎么发起工具调用
MCP              → 外部能力怎么接入宿主
Skill            → 这类任务按什么流程、规则、经验执行
```

### 典型执行链路

1. 用户提出任务（Prompt）
2. 宿主放入 Skills **元数据**（name + description）
3. 模型判断命中哪个 Skill（**路由**）
4. 宿主加载完整 `SKILL.md`（**延迟加载**）
5. 按 Skill 流程调工具、读资料、写结果

Skill 是**上下文注入机制**，不是 Function Calling 的替代品。`load_skill()` 是概念而非跨平台统一 API。

---

## SKILL.md 结构

```text
skill-name/
├── SKILL.md          # 主文件，命中时加载
├── scripts/          # 脚本（执行，不必进上下文）
├── references/       # 参考资料（按需加载）
└── assets/           # 模板、静态文件
```

### Frontmatter（元数据）

```yaml
---
name: test-driven-development    # ≤64 字符，小写+连字符，禁保留字
description: Use when implementing any feature or bugfix, before writing implementation code
---
```

**description 最关键**：决定何时触发。写清「做什么 + 什么时候用 + 用户可能说的词」。

| 好的 description | 差的 description |
|------------------|------------------|
| 从 PDF 提取文本…在处理 PDF、表单、文档提取时使用 | 我可以帮助您处理 PDF |
| 分析 git diff 生成提交消息。用户要求写 commit 时使用 | 生成提交消息 |

**name 命名**：优先动名词，如 `reviewing-code`、`processing-pdfs`。

### 正文原则

- 启动只读元数据；命中才读正文 → **省上下文**
- 正文 ≤500 行（Anthropic 建议）；细节拆到 `references/`
- 写**踩坑清单**和**项目私有约定**，不写科普/README
- 每段自问：Agent 真需要吗？是私有知识还是常识？

**渐进式披露示例**：

```markdown
需要做 SOLID 设计检查时，读取 `references/solid-checklist.md`。
```

---

## 自由度把控

| 自由度 | 场景 | 写法 |
|--------|------|------|
| 高 | 代码审查、方案评估 | 给检查方向，不写死步骤 |
| 中 | 有模板可调整 | 模板 + 参数 + 边界 |
| 低 | 迁移、部署、删文件 | 精确命令，明确不可改 |

**原则**：改数据、发请求、部署、删文件 → 收紧；分析、评审、草稿 → 可放开。

---

## 延迟加载三层

| 层 | 内容 | 时机 |
|----|------|------|
| 广告层 | name + description | 启动 |
| 指令层 | SKILL.md 正文 | 路由命中后 |
| 资源层 | references/、scripts/ | 执行时按需 |

背景：Context Rot、Lost in the Middle —— 长上下文信噪比下降。

**文件组织**：主文件做导航，一级引用到子文件，避免 `a → b → c` 深层链。

---

## 工作流与反馈循环

复杂 Skill 需明确：

1. **步骤顺序**
2. **验证节点**（不能一路跑到底就说完成）

### 清单示例（TDD）

- RED → **VERIFY RED（强制，不能跳过）** → GREEN → REFACTOR
- 结尾 Done Checklist：每项可判定，禁写「保证质量」等空话

### 反馈循环

```text
运行 → 验证 → 修复 → 再验证
```

代码审查可拆两轮：设计审查 → 实现审查 → 输出（Critical/Warning/Suggestion）。

### 条件分支

```text
创建新文档？→ 创建工作流
编辑现有文档？→ 编辑工作流
```

分支多时可拆 `workflows/create-document.md` 等。

---

## Skill 路由

Skill 少：靠 description 判断即可。Skill 多：小型检索问题。

| 步骤 | 做法 |
|------|------|
| 粗召回 | 向量化 name/description/triggers，top-5 |
| 精排 | title+description+examples 加权；高风险 Skill 提高阈值 |
| 兜底 | 最高分过低 → **不选任何 Skill** |

**冷启动**：元数据加 `triggers` 字段（用户常说的话）。

调度器四块：注册中心、路由引擎、加载器、上下文装配器（路由与加载解耦）。

---

## 八大常见坑

1. **当 README 写** — Skill 写给 Agent，重可执行性
2. **一个 Skill 太全** — 拆小，边界清晰（如 JVM / Trace / K8s 分开）
3. **给太多选择** — 默认方案 + 例外兜底，别让 Agent 现场选型
4. **术语来回换** — 同一概念固定一个词
5. **让 LLM 做确定性工作** — 格式转换、批量处理用脚本
6. **description 太虚** — 触发词不足
7. **无验证点** — 复杂任务缺测试/检查步骤
8. **第三方 Skill 直接用** — 企业需审正文、脚本、参考文件

---

## 参考仓库

- [anthropics/skills](https://github.com/anthropics/skills)（含 skill-creator）
- [obra/superpowers](https://github.com/obra/superpowers)
- [sanyuan0704/sanyuan-skills](https://github.com/sanyuan0704/sanyuan-skills)
- [skills.sh](https://skills.sh/)

## 总结口诀

```text
description 写准 → 正文别当 README → 主文件别太长
高风险写死步骤 → 复杂任务加验证点 → 第三方必审计
```
