---
aliases:
  - Multi-Agent编排
---

# Multi-Agent 编排

> 多个 LLM Agent 协作完成任务。各 Agent 可有不同角色 / 工具 / 模型。

## 常见模式
- **Supervisor / Orchestrator–Worker**：一个主 Agent 负责拆任务、派发、汇总；子 Agent 专注执行。
- **Handoff / Swarm**：Agent 之间像团队成员一样"转交"任务（OpenAI Swarm 风格）。
- **Agent Graph**：用有状态图建模（LangGraph 风格），节点 = Agent / 工具，边 = 状态转移。
- **Debate / Society**：多个角色辩论或互评，得出更稳健答案。
- **Pipeline**：固定串行（更偏 [[Workflow vs Agent|Workflow]]）。

## 关键设计点
- **角色定义**：System prompt 描述"你是谁、能用什么工具、什么时候交给谁"。
- **共享状态**：黑板（blackboard）/ scratchpad / 文件系统是常见共享介质。
- **消息协议**：是用自由文本还是结构化消息？后者更可控。
- **终止条件**：很多 multi-agent 系统死循环，需要硬限制（步数、预算）。
- **可观测**：每个 Agent 的 trace 都要能单独查看。

## 工程权衡
- **更强**：分工 + 多视角能解决单 Agent 上下文挤爆的问题。
- **更贵**：token 消耗 N 倍起步。
- **更难调试**：失败原因常常埋在 Agent 间通信里。

## 框架
- **LangGraph**（事实标准的图式 Agent 框架）
- **CrewAI**、**AutoGen**、**OpenAI Swarm**
- **Cursor / Codex / Claude Code 内部** 都有 orchestrator + subagent 概念

## 与之相关
- [[Workflow vs Agent]]
- [[Agent Harness]]
- [[MCP协议]]
- [[Coding Agent]]

## 延伸阅读
- AutoGen / CrewAI / LangGraph 官方文档
- *Generative Agents*（Stanford 25 个小镇 NPC）
- Anthropic *Building Effective Agents* 中的多 Agent 模式部分
