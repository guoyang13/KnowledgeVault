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
- 例：[[AI/00-AI学习体系/02-概念库/06-工程生态/01-Coding Agent|Coding Agent]]、深度研究、Computer Use。

## 常见 Workflow 模式（Anthropic）
1. **Prompt Chaining**：串行多步。
2. **Routing**：分类到不同子流程。
3. **Parallelization**：并行采样 / 分头处理。
4. **Orchestrator–Workers**：中央调度 + 子任务（边界更模糊，开始接近 Agent）。
5. **Evaluator–Optimizer**：生成 + 评分 + 改进的闭环。

> Graph / Loop 与上述模式的关系，见 [[AI/00-AI学习体系/02-概念库/03-Agent系统/11-AI工作流-Graph与Loop|AI工作流-Graph与Loop]]。

## 核心对比（速查）

| 维度 | AI 工作流 | Agent |
|---|---|---|
| **编排** | 流程图（Graph/节点） | 目标 + 工具 + 循环推理 |
| **谁定步骤** | 人事先画好 | 模型运行时决定 |
| **确定性** | 高 | 中低 |
| **可解释性** | 强（每步有记录） | 弱一些 |
| **成本/延迟** | 相对可控 | 多轮调用，往往更贵更慢 |
| **典型框架** | Dify、n8n、LangGraph（偏编排） | LangGraph（偏 Agent 模式）、Cursor Agent |

## 混合架构（生产常见）

```text
AI 工作流（外壳，可控）
  ├─ 节点1：固定检索
  ├─ 节点2：Agent 子任务（开放查因）
  ├─ 节点3：规则校验
  └─ 节点4：人工审批
```

- **工作流**管：触发、权限、审计、兜底、通知
- **Agent**管：中间「路径不确定、要试几步」的推理

## 选型口诀

| 选 **工作流** | 选 **Agent** |
|---|---|
| 步骤稳定、SOP 明确 | 问题开放、路径每次不同 |
| 要合规/审计 | 探索性分析、研发辅助 |
| 客服分类、文档生成 | 故障排查、多工具协作 |

**经验法则**：能画成流程图 → 优先工作流；只能写目标 → Agent，外面再套工作流约束。

## 实践经验
- 90% 的"AI 应用"其实只需要 Workflow，不需要真 Agent。
- 一旦上 Agent，**可观测性、权限模型、错误恢复**就成为头号工程问题。
- 自评与验收不能单靠 LLM，见 [[AI/00-AI学习体系/02-概念库/05-评测/03-LLM-as-a-Judge|LLM-as-a-Judge]]。

## 与之相关
- [[AI/00-AI学习体系/02-概念库/03-Agent系统/11-AI工作流-Graph与Loop|AI工作流-Graph与Loop]]
- [[AI/00-AI学习体系/02-概念库/03-Agent系统/12-AI工作流入门与实践|AI工作流入门与实践]]
- [[AI/00-AI学习体系/02-概念库/05-评测/03-LLM-as-a-Judge|LLM-as-a-Judge]]
- [[AI/00-AI学习体系/02-概念库/03-Agent系统/02-ReAct与Agent范式|ReAct与Agent范式]]
- [[AI/00-AI学习体系/02-概念库/03-Agent系统/04-Multi-Agent编排|Multi-Agent编排]]
- [[AI/00-AI学习体系/02-概念库/03-Agent系统/05-Agent Harness|Agent Harness]]
- [[AI/00-AI学习体系/02-概念库/06-工程生态/01-Coding Agent|Coding Agent]]

## 延伸阅读
- Anthropic, *Building Effective Agents* (2024)
- LangGraph / OpenAI Swarm 文档对比
