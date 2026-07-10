---
aliases:
  - JavaGuide Prompt Engineering
tags:
  - 外部参考
  - JavaGuide
  - Prompt
source: https://javaguide.cn/ai/agent/prompt-engineering.html
updated: 2026-07-10
---

# Prompt Engineering（JavaGuide）

> 原文：[大模型提示词工程](https://javaguide.cn/ai/agent/prompt-engineering.html) · 约 6336 字
>
> 导航：[[AI/00-AI学习体系/03-外部参考/JavaGuide-Agent专题/00-JavaGuide-Agent专题-导航|JavaGuide Agent 专题导航]]
>
> 本地对照：[[AI/00-AI学习体系/02-概念库/00-基础与LLM概论/10-Prompt工程入门与实践|Prompt工程入门与实践]]（索引）

## 核心观点

Prompt 好坏不看长度，看**边界是否清晰**。太长 → 噪声多、幻觉升、延迟增。

Prompt = 缩小模型搜索范围的指令。

---

## 四要素框架

| 要素 | 作用 |
|------|------|
| **Role** | 领域知识与语气 |
| **Task** | 要完成什么动作 |
| **Context** | 任务相关背景 |
| **Format** | 输出格式 |

实践：角色放**开头**，格式放**结尾**（Lost in the Middle）。关键 Prompt 仍需样例实测。

---

## 六种常用技巧

### 1. 角色扮演

越具体越稳：「专注性能优化的 Java 架构师」>「你是 AI」。长对话会稀释角色，复杂任务宜新会话。

### 2. 思维链（CoT）

| 场景 | 建议 |
|------|------|
| 教学 | 可展示步骤 |
| 调试 | 检查点、证据、失败原因 |
| 生产 | 关键依据 + 结论，少冗长推理 |
| reasoning model | 不假设可见完整 reasoning tokens |

Zero-shot：`请给出关键步骤后再回答。`

### 3. 少样本（Few-shot）

1~3 个同类型示例 > 大段文字说明。超过 3 个收益递减。

### 4. 任务分解

- **静态分解**：流程固定，事先规划步骤
- **动态分解**：探索性，按结果决定下一步

简单任务勿过度拆分；某步总错 → 单独调该步。

### 5. 结构化输出

Spring AI `BeanOutputConverter`、原生 structured output（视模型/SDK 验证）。

失败处理：

| 失败类型 | 处理 |
|----------|------|
| Schema 校验失败 | 记日志 + Prompt 版本 + 请求 ID |
| 字段缺失 | 重试并反馈缺失字段 |
| 枚举越界 | `UNKNOWN` 或人工审核 |
| 重试仍失败 | 兜底模板 |

### 6. XML 标签 + 预填充

标签名一致、有语义。预填充 `{` 引导直接输出 JSON。

---

## 复杂场景

### 长文本

- 文档放 Query **之前**
- 多文档用 `<documents>` 结构化
- **先引用再分析**：`<quotes>` → `<diagnosis>`

### 减幻觉

- 允许承认「信息不足」
- 要求逐字引用 + 编号
- Best-of-N 一致性检查
- 迭代验证（上轮输出作下轮输入）

### 一致性

JSON/XML Schema 约束；客服场景用 `<kb>` 限定知识库条目。

### Prompt Chaining

大任务拆多条 Prompt，每步一事，便于定位失败步骤。

---

## 企业级安全

### Prompt Injection vs Jailbreak

| 类型 | 来源 | 目标 |
|------|------|------|
| Prompt Injection | 网页、邮件、文档、**工具返回** | 覆盖应用指令、调错工具 |
| Jailbreak | 用户直接输入 | 绕过模型安全策略 |

### 三层防护

1. **权限控制**：沙箱、最小 API 权限、危险操作额外授权
2. **输入分隔**：System 与 User 分开；`---USER_CONTENT_START---`（不能替代代码层鉴权）
3. **人工审批**：改库、发邮件、转账等 HITL

---

## Prompt 在 Agent 中的位置

单条 Prompt 管不了多轮 Agent → 进入 **Context Engineering** 范畴。

### 提示词路由

按输入类型分配路径（FAQ / 检索 / 分析 / 代码调试）。**低置信度不强行路由**，宁可追问。

### 工具设计原则

- 名称描述 LLM 友好
- 一工具一事，原子性
- 只封装技术逻辑，主观决策不进工具
- 最小权限

→ Context 详见 [[AI/00-AI学习体系/03-外部参考/JavaGuide-Agent专题/05-Context-Engineering|Context Engineering]]

---

## 最小评测流程

| 步骤 | 做法 |
|------|------|
| 准备样例 | 10~30 条，覆盖正常/边缘/异常 |
| 固定变量 | 模型、Temperature、System Prompt |
| 记录指标 | 格式合规率、事实错误、字段缺失、人工修改次数 |
| 单点修改 | 每次只改一个变量 |
| 回归测试 | 保留失败样例定期回放 |

上线 Prompt 常需 **5~10 轮**调整。

---

## 总结

```text
信息密度 > 长度
四要素 + 按场景选技巧 + 生产配评测/Schema/权限/HITL
核心：可迭代、可验证、可回归的流程，非一次写对
```
