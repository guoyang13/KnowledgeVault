---
aliases:
  - Agent Harness
---

# Agent Harness（Agent 骨架 / Scaffold）

> 包裹 LLM、把它变成 Agent 的那层代码：上下文管理、工具循环、记忆、文件视图、错误恢复、权限控制。

## 核心公式
> **Agent 能力 ≈ 基础模型 × Harness 质量**

同一个模型在不同 harness 下，SWE-bench 等基准分数可相差 10–30 个百分点。

## Harness 要做的事
- **Action Space**：定义模型能用哪些工具（编辑文件、跑命令、搜索、浏览器、子 Agent）。
- **Observation 设计**：错误信息、文件 diff、截图怎么呈现，影响 Agent "看清世界"的能力。
- **Context / Memory 策略**：截断、压缩、子任务隔离、checkpoint、scratchpad。
- **Loop 控制**：步数上限、超时、预算控制、自我反思触发点。
- **Error Recovery**：工具失败、命令报错时的重试与降级。
- **审批 & 权限**：哪些动作需要人类确认（写文件、删除、付钱）。
- **并行 / 子 Agent**：orchestrator + sub-agent，任务分发。

## 典型 Harness 一览
| 类型 | 代表 |
|---|---|
| 学术 Coding Agent | SWE-Agent、Mini-SWE-Agent、Agentless |
| 工业 IDE/CLI Agent | Cursor Agent、Codex CLI、Claude Code、Aider、Gemini CLI、OpenCode |
| 通用软件工程 Agent | Devin、OpenHands、Jules |
| GUI / Browser Agent | Operator、browser-use、Computer Use Reference |
| Personal / Always-on Agent | OpenClaw、Hermes Agent、CyberClaw |
| 通用 Agent 框架 | LangGraph、OpenAI Agents SDK、Pydantic AI、Qwen-Agent、AutoGen、CrewAI |

## Scaffold 这个词
- 在论文里常与 harness 互换使用。
- 也指"为弱模型搭脚手架使其能干活"，对比"模型自身能力"。

## 趋势
- **更长 horizon**：从分钟级到多小时甚至多日级任务。
- **更少 prompt，更多结构**：从堆 prompt 转向结构化状态机 + 工具协议（[[AI/00-AI知识体系/概念/03-Agent系统/06-MCP协议|MCP协议]]）。
- **统一权限模型**：Antigravity / Cursor 等开始把 harness 的"许可层"标准化。
- **可复用 Skills**：把一类任务的流程知识、脚本、模板和验收标准沉淀为 skill，而不是每次临场写 prompt。
- **Trace / Replay / Eval**：harness 不只是执行器，也要能回放、观测和评测 agent 行为。

## 与之相关
- [[AI/00-AI知识体系/概念/03-Agent系统/02-ReAct与Agent范式|ReAct与Agent范式]]
- [[AI/00-AI知识体系/概念/03-Agent系统/01-Workflow vs Agent|Workflow vs Agent]]
- [[AI/00-AI知识体系/概念/03-Agent系统/04-Multi-Agent编排|Multi-Agent编排]]
- [[AI/00-AI知识体系/概念/03-Agent系统/06-MCP协议|MCP协议]]
- [[AI/00-AI知识体系/概念/03-Agent系统/09-Agent学习路线与项目阶梯|Agent学习路线与项目阶梯]]
- [[AI/00-AI知识体系/概念/03-Agent系统/10-Agent Skills与协议生态|Agent Skills与协议生态]]
- [[AI/00-AI知识体系/概念/06-工程生态/01-Coding Agent|Coding Agent]]
- [[AI/00-AI知识体系/概念/05-评测/01-Eval Harness|Eval Harness]]

## 延伸阅读
- SWE-Agent 论文 (Princeton, 2024)
- *Building Effective Agents* (Anthropic)
- Claude Code / Codex / OpenClaw / Hermes Agent / LangGraph 官方文档与源码
