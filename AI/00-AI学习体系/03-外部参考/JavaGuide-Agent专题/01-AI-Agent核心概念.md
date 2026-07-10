---
aliases:
  - JavaGuide Agent核心概念
tags:
  - 外部参考
  - JavaGuide
  - Agent
source: https://javaguide.cn/ai/agent/agent-basis.html
updated: 2026-07-10
---

# AI Agent 核心概念（JavaGuide）

> 原文：[AI Agent 核心概念](https://javaguide.cn/ai/agent/agent-basis.html) · 约 7453 字
>
> 导航：[[AI/00-AI学习体系/03-外部参考/JavaGuide-Agent专题/00-JavaGuide-Agent专题-导航|JavaGuide Agent 专题导航]]

## Agent 演进时间线

| 阶段 | 特征 |
|------|------|
| 2022 | 对话为主，Prompt Engineering，**只能说不能做** |
| 2023 中 | Function Calling、RAG、AutoGPT；早期 Agent 易绕圈 |
| 2023 末 | ReAct 普及；Coze/Dify 用 DAG 约束流程 |
| 2024 末 | MCP、Computer Use；Cursor/Claude Code 编程 Agent |
| 2025 | 长任务执行；**Agent Skills** 封装固定流程 |
| 2026 | Skills + Heartbeat 常驻唤醒；**Harness Engineering**（Agent = Model + Harness） |

分水岭：**2023 年中** —— 之前主要「说」，之后逐渐「做」。

---

## 三种范式对比

```text
传统编程：程序员写代码 → 执行结果
Workflow：产品画流程图 → 执行结果
Agent：    用户说意图 → AI 决策 → 动态执行
```

| 类型 | 适合 | 不适合 |
|------|------|--------|
| 传统编程 | 订单扣库存、MQ 消费、支付流转 | — |
| Workflow | 审批流、内容发布、线索分配 | — |
| Agent | 故障排查、意图理解、动态判断 | 高频确定性核心链路 |
| Plan-and-Execute | 超长流程 + 部分动态子任务 | — |

---

## Agent 公式与三层架构

**Agent = LLM + Planning + Memory + Tools**

| 组件 | 职责 |
|------|------|
| Planning | 拆目标、决定下一步（CoT 等） |
| Memory | 短期=上下文历史；长期=向量库/知识图谱 |
| Tools | 查数据、调 API、读写文件；无工具则只能「建议」 |
| Observation | 工具结果写回上下文，驱动下一轮推理 |

### Agent 系统最少三层

1. **LLM Call** — 模型接口、流式、截断、重试
2. **Tools Call** — Function Calling、MCP、Skills
3. **Context Engineering** — Prompt 编排、记忆注入、工具描述组装

→ 详见 [[AI/00-AI学习体系/03-外部参考/JavaGuide-Agent专题/05-Context-Engineering|Context Engineering]]

### Agent Loop

```text
初始化（System Prompt + 工具列表 + 用户请求）
  ↓
循环：读上下文 → LLM 推理 → 调工具？→ 结果写回上下文
  ↓
无 tool_calls → 退出
兜底：max_iterations（10~20）或 Token 阈值
```

工程难点在**上下文管理**，非 while 本身。对照 [[AI/00-AI学习体系/02-概念库/00-基础与LLM概论/09-LLM调用与Agent多轮对话|LLM调用与Agent多轮对话]]。

---

## Tools 注册：Schema + MCP

### Function Calling Schema（数据格式）

JSON Schema 描述 name、description、parameters。`description` 要写清**使用场景**和**禁用场景**。

```json
{
  "type": "function",
  "function": {
    "name": "query_slow_sql",
    "description": "查指定微服务在特定时间段的慢 SQL 日志。服务响应慢、数据库超时时用。若用户问网络或内存问题，别调这个。",
    "parameters": {
      "type": "object",
      "properties": {
        "service_name": { "type": "string", "description": "如 user-service" },
        "time_range": { "type": "string", "description": "HH:MM-HH:MM" }
      },
      "required": ["service_name", "time_range"]
    }
  }
}
```

### Skills（进阶封装）

| 形态 | 特点 | 适用 |
|------|------|------|
| Toolkits（黑盒） | 多工具封装成一个 Schema | 逻辑固定 |
| Agent Skills（白盒） | `SKILL.md` + 延迟加载 | 团队经验、灵活流程 |

→ 详见 [[AI/00-AI学习体系/03-外部参考/JavaGuide-Agent专题/02-Agent-Skills详解|Agent Skills 详解]]

### MCP（通信接入）

- **JSON Schema** = 数据格式；**MCP** = JSON-RPC 2.0 通信协议
- 三原语：Tools / Resources / Prompts
- 对照 [[AI/00-AI学习体系/02-概念库/03-Agent系统/06-MCP协议|MCP协议]]

---

## Agent 核心范式

### ReAct（Reasoning + Acting）

边推理边行动，根据工具反馈修正方向。组件：历史上下文、环境输入、LLM 推理、工具/Skills、观察反馈。

- 优点：减幻觉、可解释
- 代价：延迟高、依赖工具质量

### Plan-and-Execute

先全局计划，再按步执行。适合步骤多、结构清晰的长任务；动态调整弱。可与 ReAct 组合：全局 CoT + 步骤内 ReAct。

### Reflection

- Reflexion：失败后反思存记忆
- Self-Refine：完成后自审迭代
- CRITIC：外部工具验证事实

通常叠加在 ReAct / P&E 上，很少单独用。

### Multi-Agent

| 模式 | 说明 |
|------|------|
| Orchestrator-Subagent | 编排者分发，主流 |
| Peer-to-Peer | 平等对话、辩论评审 |

### A2A 协议

Agent 间用结构化 JSON/XML 交互，非自然语言废话（类比微服务 REST/RPC）。

### Agentic Workflows（吴恩达）

工程组合拳：Reflection + Tool Use + Planning + Multi-agent Collaboration。常见：Planning → ReAct 子任务 → Tools → Reflection。

---

## AI 工作流 vs Agent

| | 纯 Agent | AI 工作流 | Agentic Workflows |
|--|---------|-----------|-------------------|
| 决策者 | LLM | 图结构（人设计） | 全局 Workflow + 局部 Agent |
| 控制 | 模型推理 | 步骤/分支/重试预设 | 混合 |

### Graph 三要素

- **Node**：单职责（生成初稿 / 审核 / 修改）
- **Edge**：顺序边、条件边、循环边
- **State**：草稿、评分、`iteration_count` 等共享上下文

→ 详见 [[AI/00-AI学习体系/03-外部参考/JavaGuide-Agent专题/03-AI工作流-Workflow-Graph-Loop|Workflow Graph Loop]]

### 选型口诀

```text
先把执行路径写出来 → 能写出来用 Workflow，写不出来用 Agent
To B 优先 Workflow 或 Agentic Workflows（可观测、好排查）
```

---

## 范式选型表

| 场景特征 | 推荐 | 代价 |
|----------|------|------|
| 路径可确定，节点需 LLM | AI 工作流（Graph） | 稳定，前期设计成本高 |
| 路径不确定 | ReAct | 灵活，Token 高，调试难 |
| 长任务、步骤多且清晰 | Plan-and-Execute | 不易迷路，调整弱 |
| 输出质量要求高 | + Reflection | 多轮迭代 |
| 多专业角色 | Multi-Agent | 通信/调试成本翻倍 |
| 长任务 + 部分不可预测 | Agentic Workflows | 全局 WF + 局部 ReAct |

---

## Agent 真实挑战

- 长任务失忆；中间位置信息利用差（Lost in the Middle）
- 工具降幻觉但不消灭；Token 成本
- Prompt Injection、越权 → 最小权限、沙箱、人工确认
- 可观测性差：难追溯某步决策原因

---

## 总结（原文提炼）

1. **四块缺一不可**：LLM + Planning + Memory + Tools
2. **别上来就 Multi-Agent**：先最简单方案跑通
3. **工具 description 是隐形关键**：省这里后面双倍还
4. **选型**：路径能写死 → Workflow；写不死 → Agent

## 本地延伸阅读

- [[AI/00-AI学习体系/02-概念库/03-Agent系统/01-Workflow vs Agent|Workflow vs Agent]]
- [[AI/00-AI学习体系/02-概念库/03-Agent系统/02-ReAct与Agent范式|ReAct与Agent范式]]
- [[AI/00-AI学习体系/02-概念库/03-Agent系统/12-AI工作流入门与实践|AI工作流入门与实践]]
