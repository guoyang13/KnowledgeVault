---
aliases:
  - JavaGuide Context Engineering
tags:
  - 外部参考
  - JavaGuide
  - Context
source: https://javaguide.cn/ai/agent/context-engineering.html
updated: 2026-07-10
---

# Context Engineering（JavaGuide）

> 原文：[上下文工程](https://javaguide.cn/ai/agent/context-engineering.html) · 约 9445 字
>
> 导航：[[AI/00-AI学习体系/03-外部参考/JavaGuide-Agent专题/00-JavaGuide-Agent专题-导航|JavaGuide Agent 专题导航]]
>
> 本地对照：[[AI/00-AI学习体系/02-概念库/06-工程生态/02-Context Engineering|Context Engineering]]

## 一句话

> 每次调用 LLM 前，决定窗口里放什么、怎么排、何时撤掉 —— **信息供给问题**，非指令措辞问题。

Tobi Lutke：提供足够 context，使任务在模型能力范围内 **plausibly solvable**。

---

## vs Prompt Engineering

| | Prompt Engineering | Context Engineering |
|--|-------------------|---------------------|
| 关注 | 指令怎么写 | 窗口里装什么、结构、时机 |
| 类比 | 告诉厨师怎么做菜 | 准备厨房/食材摆放 |
| 另一类比 | — | LLM 的**内存管理**（LRU、优先级淘汰） |

---

## 管哪些东西

| 块 | 说明 |
|----|------|
| System Prompt | `.cursorrules`、`AGENTS.md` — 角色、约束、执行流 |
| User Prompt | 业务数据 + 指令（易混脏） |
| Memory | 短期滑动窗口；长期=文件/KV/向量/图库 |
| RAG & Tools | 检索片段 + 工具描述 + 调用结果（Observation） |
| Structured Output | Schema、Function Calling 约束进上下文 |
| Token 优化 | 摘要、剔除、Context Caching |

---

## 上下文为何失效

- **能放进去 ≠ 能用好**
- **Context Rot**：越长信噪比越差
- **Lost in the Middle**：中间信息易漏

对策：删噪声、关键约束放显眼位、长文档先切分/检索、分清目标/背景/约束/输出。

**宁愿少一点高信噪比，也别堆「可能有用」的内容。**

---

## 评测指标（不能只靠体感）

| 类型 | 看什么 |
|------|--------|
| 任务成功率 | 完成目标、人工补救、可复现 |
| 工具质量 | 错选/漏调/参数错/重复/危险拦截 |
| 上下文成本 | 输入输出 Token、缓存命中、压缩保留比 |
| 延迟 | TTFT、端到端、p95/p99 |
| 结果质量 | 幻觉率、引用准确率、关键字段遗漏 |

建议：20~50 条真实轨迹，**每次只改一个变量**。

---

## 运行时加载策略

| 策略 | 优点 | 代价 | 适合 |
|------|------|------|------|
| 预检索 | 快、稳定 | 易塞噪声、不灵活 | FAQ、固定文档审阅 |
| Just-in-Time | 干净、证据按需 | 工具调用多、慢 | 代码库、故障排查 |
| 混合 | 兼顾 | 需预算管理器 | 复杂业务 Agent |

**JIT 要点**：元数据也是信息（路径、时间戳、目录结构）；需 glob/grep/tree 等导航工具。

Skills = 渐进式披露思想 → [[AI/00-AI学习体系/03-外部参考/JavaGuide-Agent专题/02-Agent-Skills详解|Agent Skills 详解]]

---

## 长任务：三种撑法

| 技术 | 场景 |
|------|------|
| **Compaction** | 窗口快满时 LLM 摘要历史，保留架构决策/未解 Bug |
| **Structured Note-taking** | 写 `NOTES.md`，重置后读回（to-do、进度） |
| **Sub-agent** | 子 Agent 探索大量上下文，只回 1000~2000 token 摘要给主 Agent |

Compaction 还可：清理旧 tool_result，保留 tool_use 记录。

---

## Context Assembler（每次调用前）

```text
constraints  ← load_system_constraints()
goal         ← extract_current_goal(user_task, session_state)
evidence     ← retrieve_rag(goal, business_context)
memory       ← recall_memory(goal, session_state)
tools        ← select_tools(goal, evidence, memory)
history      ← compact_history(messages)
context      ← rank([...])  → fit_token_budget(context)
output: messages, tool_schema, metadata
```

**关键两步**：`rank`（顺序）、`fit_token_budget`（保留/摘要/引用）。

### 静态规则（System Prompt）

结构化 Markdown：角色、约束、执行流、输出格式。避免过度设计（if-else 堆 Prompt）和过度抽象（一句「有帮助的助手」）。

**Goldilocks zone**：具体到能引导行为，抽象到覆盖常见变化。按 failure case 逐条补规则。

### 工具上下文

描述须回答：**何时该调 / 何时不该调**。一工具一事。

### 动态上下文兜底

| 失败路径 | 兜底 |
|----------|------|
| RAG 无结果 | 关键词检索或向用户澄清 |
| 工具超时 | 超时/重试/熔断/HITL |
| 摘要丢失 | 保留 traceId、证据位置、关键字段 |
| 记忆污染 | 写入校验，标记来源与时间 |
| 多工具冲突 | 优先级、状态机、副作用等级 |

### Token 预算（单次调用内）

| 优先级 | 内容 | 处理 |
|--------|------|------|
| 低 | 早期对话 | AI 摘要 |
| 中 | RAG 背景、旧工具结果 | 裁剪+引用 |
| 高 | System 约束、当前目标、安全边界 | 固定区 |
| 阶段性 | 当前阶段工具 Schema | 按任务加载/卸载 |

---

## 常用工具（按层）

- **编排**：LangChain、LangGraph
- **RAG 数据**：LlamaIndex
- **向量库**：Chroma / Qdrant / Milvus / Pinecone
- **协议**：MCP（工具接入也是副作用入口）
- **记忆产品**：Mem0、Letta、ZEP

---

## 落地原则

1. **高信噪比 > 信息量**（非「窗口大就多塞」）
2. **长任务上下文必变脏** — Compaction + 笔记 + Sub-agent 组合
3. **`do the simplest thing that works`** — 先 System Prompt + 工具边界 → RAG → 压缩预算 → 再记忆层

```text
上下文给对了，中等模型也能做复杂任务
上下文给烂了，贵模型也输出噪声
```

---

## 参考（原文列举）

- Anthropic: Effective context engineering for AI agents
- Context Rot / Lost in the Middle 论文
- MCP 2025-03-26 Specification
- 12 Factor Agents - Own Your Context Window

## 本地延伸阅读

- [[AI/00-AI学习体系/03-外部参考/JavaGuide-Agent专题/04-Prompt-Engineering|Prompt Engineering（JavaGuide）]]
- [[AI/00-AI学习体系/02-概念库/03-Agent系统/08-Agent Memory|Agent Memory]]
- [[AI/00-AI学习体系/02-概念库/04-RAG进阶/01-RAG基础|RAG基础]]
