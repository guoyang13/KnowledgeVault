---
aliases:
  - JavaGuide Agent专题
  - JavaGuide Agent
tags:
  - 外部参考
  - JavaGuide
  - Agent
source: https://javaguide.cn/ai/agent/
updated: 2026-07-10
---

# JavaGuide · Agent 专题导航

> 外部参考笔记，整理自 [JavaGuide AI Agent 专题](https://javaguide.cn/ai/agent/)。侧重 **Java 栈（Spring AI Alibaba）** 与 **面试向总结**；与本地概念库互补，非替代。
>
> 抓取日期：2026-07-10。图片/架构图未收录，代码块与表格已保留。

↑ [[AI/00-AI学习体系/03-外部参考/00-外部参考导航|外部参考导航]] · [[AI/00-AI学习体系/02-概念库/03-Agent系统/00-Agent系统导航|Agent系统导航]]

## 阅读顺序

```text
01 核心概念（总览）
  ├→ 04 Prompt Engineering（指令怎么写）
  ├→ 05 Context Engineering（上下文怎么装）
  ├→ 02 Agent Skills（任务 SOP 怎么沉淀）
  └→ 03 Workflow / Graph / Loop（流程怎么编排）
```

| 序号 | 笔记 | 原文 | 约字数 |
|------|------|------|--------|
| 01 | [[AI/00-AI学习体系/03-外部参考/JavaGuide-Agent专题/01-AI-Agent核心概念\|AI Agent 核心概念]] | [agent-basis.html](https://javaguide.cn/ai/agent/agent-basis.html) | 7453 |
| 02 | [[AI/00-AI学习体系/03-外部参考/JavaGuide-Agent专题/02-Agent-Skills详解\|Agent Skills 详解]] | [skills.html](https://javaguide.cn/ai/agent/skills.html) | 8898 |
| 03 | [[AI/00-AI学习体系/03-外部参考/JavaGuide-Agent专题/03-AI工作流-Workflow-Graph-Loop\|Workflow / Graph / Loop]] | [workflow-graph-loop.html](https://javaguide.cn/ai/agent/workflow-graph-loop.html) | 7389 |
| 04 | [[AI/00-AI学习体系/03-外部参考/JavaGuide-Agent专题/04-Prompt-Engineering\|Prompt Engineering]] | [prompt-engineering.html](https://javaguide.cn/ai/agent/prompt-engineering.html) | 6336 |
| 05 | [[AI/00-AI学习体系/03-外部参考/JavaGuide-Agent专题/05-Context-Engineering\|Context Engineering]] | [context-engineering.html](https://javaguide.cn/ai/agent/context-engineering.html) | 9445 |

## 与本地概念库对照

| JavaGuide 主题 | 本地对应笔记 | 差异要点 |
|----------------|--------------|----------|
| Agent 核心概念 | [[AI/00-AI学习体系/02-概念库/03-Agent系统/01-Workflow vs Agent\|Workflow vs Agent]]、[[AI/00-AI学习体系/02-概念库/03-Agent系统/02-ReAct与Agent范式\|ReAct与Agent范式]] | JavaGuide 偏演进时间线与选型表 |
| Skills | [[AI/00-AI学习体系/02-概念库/03-Agent系统/10-Agent Skills与协议生态\|Agent Skills与协议生态]] | JavaGuide 含 SKILL.md 写法与 8 坑 |
| Workflow/Graph/Loop | [[AI/00-AI学习体系/02-概念库/03-Agent系统/11-AI工作流-Graph与Loop\|AI工作流-Graph与Loop]] | 本地含 LangGraph Python；JavaGuide 含 Spring AI Alibaba |
| Prompt | [[AI/00-AI学习体系/02-概念库/00-基础与LLM概论/10-Prompt工程入门与实践\|Prompt工程入门与实践]] | JavaGuide 含企业安全与 Spring AI 结构化输出 |
| Context | [[AI/00-AI学习体系/02-概念库/06-工程生态/02-Context Engineering\|Context Engineering]] | JavaGuide 含 Context Assembler 伪代码与评测指标 |
| MCP / Tools | [[AI/00-AI学习体系/02-概念库/03-Agent系统/06-MCP协议\|MCP协议]]、[[AI/00-AI学习体系/02-概念库/03-Agent系统/03-Tool Use与Function Calling\|Tool Use]] | — |
| Harness | [[AI/00-AI学习体系/02-概念库/03-Agent系统/05-Agent Harness\|Agent Harness]] | JavaGuide 主文仅提及，专题另有独立文 |

## 专题未收录（可继续抓取）

- [万字拆解 MCP 协议](https://javaguide.cn/ai/agent/mcp.html)
- [Harness Engineering 详解](https://javaguide.cn/ai/agent/harness-engineering.html)
- [Loop Engineering 是什么](https://javaguide.cn/ai/agent/loop-engineering.html)
- [AI Agent 面试题总结](https://javaguide.cn/ai/interview-questions/agent-interview-questions.html)

## 一句话心智模型

```text
Prompt     = 这次要做什么（指令）
Context    = 这次能看到什么（信息供给）
Skills     = 这类任务按什么 SOP 做（经验包）
Tools/MCP  = 能调什么外部能力（执行）
Workflow   = 步骤骨架谁控制（图结构）
Agent Loop = 不确定时 LLM 自己转圈（动态决策）
```
