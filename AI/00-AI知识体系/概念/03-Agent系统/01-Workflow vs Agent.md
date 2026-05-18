---
aliases:
  - Workflow vs Agent
---

# Workflow vs Agent

> Anthropic 在《Building Effective Agents》中提出的关键二分法：**先用 Workflow，必要时再上 Agent**。

## 定义
- **Workflow（工作流）**：人为编排好的固定步骤，LLM 只在指定节点完成任务。结构可控、可预测、便于调试。
- **Agent（智能体）**：LLM 自主决定下一步动作——调哪个工具、是否结束、何时反思。灵活但难调。

## 何时用 Workflow
- 步骤数固定、任务边界清晰。
- 可预测性 / 可解释性要求高（金融、医疗、合规）。
- 失败成本高、需要明确审计。
- 例：客服三段式（识别意图 → 查 KB → 生成回答）。

## 何时用 Agent
- 任务路径不确定，需要根据中间结果分支。
- 工具空间大、组合方式多（编程、研究、运维）。
- 模型能力足够强（差模型上的 Agent 很容易翻车）。
- 例：[[Coding Agent]]、深度研究、Computer Use。

## 常见 Workflow 模式（Anthropic）
1. **Prompt Chaining**：串行多步。
2. **Routing**：分类到不同子流程。
3. **Parallelization**：并行采样 / 分头处理。
4. **Orchestrator–Workers**：中央调度 + 子任务（边界更模糊，开始接近 Agent）。
5. **Evaluator–Optimizer**：生成 + 评分 + 改进的闭环。

## 实践经验
- 90% 的"AI 应用"其实只需要 Workflow，不需要真 Agent。
- 一旦上 Agent，**可观测性、权限模型、错误恢复**就成为头号工程问题。

## 与之相关
- [[ReAct与Agent范式]]
- [[Multi-Agent编排]]
- [[Agent Harness]]
- [[Coding Agent]]

## 延伸阅读
- Anthropic, *Building Effective Agents* (2024)
- LangGraph / OpenAI Swarm 文档对比
