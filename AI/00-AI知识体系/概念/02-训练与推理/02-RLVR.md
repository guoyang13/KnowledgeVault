---
aliases:
  - RLVR
---

# RLVR（Reinforcement Learning with Verifiable Rewards）

> 用"可自动验证的奖励信号"做 RL 训练——是推理模型崛起的关键技术。

## 关键点
- **奖励来源**：数学题答案、代码单元测试、形式化证明、游戏胜负——任何"对错可以程序判定"的任务。
- **相比 RLHF**：
  - 不需要人类打标 / Reward Model。
  - 信号干净、规模可大、可避免 reward hacking 的部分模式。
- **GRPO（Group Relative Policy Optimization）**：DeepSeek 提出的轻量 RL 算法，去掉 critic，是 R1 训练核心。
- **典型流程**：
  1. 生成多条候选解。
  2. 用 verifier 判对错（或部分对）。
  3. 用 GRPO / PPO 更新策略。
- **Reward Shaping 注意**：只看最终对错容易学到"投机解法"，常配合 [[AI/00-AI知识体系/概念/02-训练与推理/03-PRM与ORM|PRM与ORM]] 给中间过程评分。

## 应用领域
- 数学（AIME、MATH、奥数）
- 编程（HumanEval、LiveCodeBench、SWE-bench 类）
- 工具使用、Agent 任务（用任务完成度做 verifier）
- 形式化证明（Lean、Coq）

## 代表工作
- **DeepSeek-R1 / R1-Zero**：把 RLVR 推向主流，公开了完整 recipe。
- **AlphaProof / AlphaGeometry**（DeepMind）
- **OpenAI o-series**：未公开细节，但普遍认为 RLVR 是核心。

## 与之相关
- [[AI/00-AI知识体系/概念/01-模型层/03-推理模型|推理模型]]
- [[AI/00-AI知识体系/概念/02-训练与推理/03-PRM与ORM|PRM与ORM]]
- [[AI/00-AI知识体系/概念/02-训练与推理/01-训练阶段与对齐方法|训练阶段与对齐方法]]
- [[AI/00-AI知识体系/概念/02-训练与推理/08-Test-time Compute Scaling|Test-time Compute Scaling]]

## 延伸阅读
- DeepSeek-R1 技术报告
- DeepSeekMath（GRPO 原论文）
