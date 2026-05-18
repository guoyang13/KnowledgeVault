---
aliases:
  - 推理时增强-CoT
---

# 推理时增强（CoT / ToT / Best-of-N / Self-Consistency / Reflection）

> 不改模型权重，只在推理阶段用 prompt 技巧 + 多次采样 + 搜索，提升回答质量。

## 单次增强
- **CoT（Chain-of-Thought）**：让模型"逐步思考"再回答。可显式 prompt，也可由 [[AI/00-AI知识体系/概念/01-模型层/03-推理模型|推理模型]] 内置。
- **Zero-shot CoT**：仅加一句 "Let's think step by step"。
- **Self-Ask**：模型自问自答，逐层分解。
- **Plan-and-Solve**：先给计划，再按计划执行。

## 多次采样
- **Self-Consistency**：采样 N 条 CoT，对答案投票（适合有标准答案的任务）。
- **Best-of-N**：用 verifier / reward model 从 N 条中选最好。

## 搜索式
- **ToT（Tree of Thought）**：把思考过程展成树，节点级搜索。
- **GoT（Graph of Thought）**：更一般的图结构。
- **MCTS-LLM**：用 [[AI/00-AI知识体系/概念/02-训练与推理/03-PRM与ORM|PRM与ORM]] 当价值函数做蒙特卡洛树搜索。

## 自我修正
- **Reflection / Self-Critique**：模型先答 → 再批评 → 再改。
- **Reflexion**（Agent 语境）：失败后写"经验笔记"再试。
- **Debate**：多个模型/角色辩论得出更稳健答案。

## 实践经验
- 简单任务别用 CoT，反而变慢、变差。
- 数学/代码：CoT + Self-Consistency + Verifier 几乎总是值得。
- 现代 [[AI/00-AI知识体系/概念/01-模型层/03-推理模型|推理模型]] 已把这些"训练进去"，开发者只需调 `reasoning_effort` 即可。

## 与之相关
- [[AI/00-AI知识体系/概念/01-模型层/03-推理模型|推理模型]]
- [[AI/00-AI知识体系/概念/02-训练与推理/08-Test-time Compute Scaling|Test-time Compute Scaling]]
- [[AI/00-AI知识体系/概念/02-训练与推理/03-PRM与ORM|PRM与ORM]]
- [[AI/00-AI知识体系/概念/03-Agent系统/02-ReAct与Agent范式|ReAct与Agent范式]]

## 延伸阅读
- *Chain-of-Thought Prompting*（Wei et al., 2022）
- *Self-Consistency Improves CoT*（Wang et al., 2022）
- *Tree of Thoughts*（Yao et al., 2023）
- *Reflexion*（Shinn et al., 2023）
