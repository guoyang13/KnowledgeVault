---
aliases:
  - Agent系统导航
  - Agent导航
tags: [AI, Agent, 导航]
---

# Agent 系统导航

> 本章回答：怎样让 LLM 不只生成文本，还能观察环境、选择工具、执行动作并验证结果？

↑ [[AI/00-AI学习体系/02-概念库/00-概念库导航|概念库导航]] · [[AI/00-AI学习体系/00-核心索引|核心索引]]

## 最短主线

1. [[AI/00-AI学习体系/02-概念库/03-Agent系统/01-Workflow vs Agent|Workflow vs Agent]]：先决定是否需要 Agent。
2. [[AI/00-AI学习体系/02-概念库/03-Agent系统/03-Tool Use与Function Calling|Tool Use与Function Calling]]：理解模型如何连接真实动作。
3. [[AI/00-AI学习体系/02-概念库/03-Agent系统/02-ReAct与Agent范式|ReAct与Agent范式]]：理解观察、决策、行动循环。
4. [[AI/00-AI学习体系/02-概念库/03-Agent系统/12-AI工作流入门与实践|AI工作流入门与实践]]：认识常见业务模式和选型。
5. [[AI/00-AI学习体系/02-概念库/03-Agent系统/11-AI工作流-Graph与Loop|AI工作流、Graph与Loop]]：深入编排与代码实现。
6. [[AI/00-AI学习体系/02-概念库/03-Agent系统/05-Agent Harness|Agent Harness]]：理解模型外层的工程系统。

## 按能力扩展

| 能力 | 笔记 | 解决的问题 |
|---|---|---|
| 多主体协作 | [[AI/00-AI学习体系/02-概念库/03-Agent系统/04-Multi-Agent编排|Multi-Agent编排]] | 任务分工、Handoff、监督与合并 |
| 工具协议 | [[AI/00-AI学习体系/02-概念库/03-Agent系统/06-MCP协议|MCP协议]] | 标准化连接工具和资源 |
| 浏览器与桌面 | [[AI/00-AI学习体系/02-概念库/03-Agent系统/07-Computer Use与Browser Agent|Computer Use与Browser Agent]] | 操作 GUI 和网页环境 |
| 记忆 | [[AI/00-AI学习体系/02-概念库/03-Agent系统/08-Agent Memory|Agent Memory]] | 跨步骤、跨会话保存信息 |
| 技能复用 | [[AI/00-AI学习体系/02-概念库/03-Agent系统/10-Agent Skills与协议生态|Agent Skills与协议生态]] | 沉淀 SOP、脚本、模板和验收标准 |

## 三篇容易混淆的笔记

| 笔记 | 定位 | 什么时候读 |
|---|---|---|
| [[AI/00-AI学习体系/02-概念库/03-Agent系统/12-AI工作流入门与实践|AI工作流入门与实践]] | 场景、模式、选型和落地原则 | 第一次理解 AI Workflow |
| [[AI/00-AI学习体系/02-概念库/03-Agent系统/11-AI工作流-Graph与Loop|AI工作流、Graph与Loop]] | Graph、Loop、状态与代码实现 | 准备自己实现工作流 |
| [[AI/00-AI学习体系/02-概念库/03-Agent系统/09-Agent学习路线与项目阶梯|Agent学习路线与项目阶梯]] | 从最小 Loop 到真实项目的练习顺序 | 需要制定实践计划 |

`03-外部参考/JavaGuide-Agent专题` 是第三方资料摘录，用于补 Java / Spring AI 视角，不作为主学习路径。

## 读完应该能回答

- Workflow 与 Agent 的控制权分别在哪里？
- Tool Calling 为什么不是工具真正执行？
- Graph、Loop、Harness、Memory 各自负责哪一层？
- 什么场景适合 Multi-Agent，什么场景单 Agent 更可靠？
- Agent 如何处理权限、失败、停止条件和 Prompt Injection？

## 实践入口

[[AI/00-AI学习体系/02-概念库/03-Agent系统/09-Agent学习路线与项目阶梯|Agent学习路线与项目阶梯]]：按阶段构建最小 Agent、RAG/Memory、Harness、Browser、Eval 和真实项目。

