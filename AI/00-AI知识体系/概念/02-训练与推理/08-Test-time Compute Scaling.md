---
aliases:
  - Test-time Compute Scaling
---

# Test-time Compute Scaling（推理时算力扩展）

> "多花算力换准确率"——和 Pretraining Scaling、Post-training Scaling 并列的第三条 Scaling Law。

## 核心命题
- 给定一个模型，在推理时投入更多 token / 采样 / 搜索，准确率可以持续提升。
- OpenAI o1 报告显示：在数学等任务上，推理 token 与准确率呈对数线性增长。

## 三种"花钱方式"
1. **串行更长**：让模型思考更久（更长 CoT、更高 `reasoning_effort`）。
2. **并行更多**：采样 N 条独立解（Best-of-N、Self-Consistency）。
3. **搜索更广**：树/图搜索 + 价值函数（MCTS-LLM、ToT）。

## 何时奏效
- 任务有"可验证"或可打分的答案（数学、代码、推理、规划）。
- 模型本身已经具备基本能力——Scaling 是"放大器"，不是"创造器"。
- 对纯创意 / 开放任务收益较小。

## 与训练的关系
- **Pretraining Scaling**：参数 + 数据 + FLOPs（已逐渐放缓）。
- **Post-training Scaling**：[[RLVR]] / 偏好数据规模。
- **Test-time Scaling**：推理花钱。
- 三者**乘性叠加**，是当前前沿模型提升的主要驱动力。

## 经济学视角
- 一次回答可能花费几美元（深度推理任务）。
- 适合用在"少量、价值高、可验证"的请求上。
- API 普遍提供 `low / medium / high` 三档 reasoning effort，按需选用。

## 与之相关
- [[推理模型]]
- [[RLVR]]
- [[PRM与ORM]]
- [[推理时增强-CoT]]

## 延伸阅读
- OpenAI o1 system card / 博客
- *Scaling LLM Test-Time Compute Optimally* (DeepMind, 2024)
- Anthropic 关于 Extended Thinking 的实验数据
