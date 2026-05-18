---
aliases:
  - ReAct与Agent范式
---

# ReAct 与 Agent 基本范式

> Agent 的"心跳"基本都是 **观察 → 思考 → 行动** 的循环，不同范式只是这个循环的不同变体。

## ReAct（Reason + Act）
- **核心**：在一个循环里交替 `Thought → Action → Observation`。
- 是绝大多数 Tool-use Agent 的祖师爷范式。
- 工程实现：LLM 输出结构化动作（工具调用），运行时执行并把结果作为新 observation 回喂。

## Plan-and-Execute
- 先用一次大思考生成完整计划（任务清单），再逐步执行。
- 适合长任务、可预测性更高，但中途难以根据新发现调整。
- 进阶：**ReWOO**（计划 + 工具 + 求解三段式）。

## Reflexion
- 失败后写一段"经验总结"加入下次的上下文。
- 让 Agent 具备**跨 episode 的学习**能力（在不改权重的前提下）。

## Self-Refine / Critic-Improve
- 输出 → 自我批评 → 修正，循环到满意。
- 常用于代码生成、文案润色。

## CodeAct（Code as Action）
- 用"写一段代码并执行"作为通用动作，比离散 function calling 表达力强得多。
- 适合数据处理、复合任务。
- 见 [[Tool Use与Function Calling]]。

## 现代实现注意
- 真实工业 Agent 很少纯 ReAct——往往是 **ReAct + Plan + Reflection + 权限控制 + Context 管理** 的混合体，这就是 [[Agent Harness]] 的工作。
- 推理模型（[[推理模型]]）内置 thinking，Action 步骤更稳，但 harness 设计依然关键。

## 与之相关
- [[Workflow vs Agent]]
- [[Tool Use与Function Calling]]
- [[Agent Harness]]
- [[推理时增强-CoT]]

## 延伸阅读
- *ReAct* (Yao et al., 2022)
- *Reflexion* (Shinn et al., 2023)
- *ReWOO* (Xu et al., 2023)
