---
aliases:
  - PRM与ORM
---

# PRM 与 ORM（过程奖励 vs 结果奖励）

> 奖励模型按"评分粒度"分两类，PRM 给中间每一步打分，ORM 只看最终结果。

## ORM（Outcome Reward Model）
- 输入：完整解答；输出：一个标量分数 / 对错。
- 优点：标注便宜，可自动获得（执行器、答案校验）。
- 缺点：信号稀疏，模型可能学到"歪打正着"。

## PRM（Process Reward Model）
- 输入：解题轨迹的每一步；输出：每步对错概率 / 价值。
- 优点：稠密信号、能定位错误步、配合搜索效果显著。
- 缺点：标注昂贵；近年靠**自动 PRM 标注**（蒙特卡洛 rollout 估值）降本。
- 典型工作：**Math-Shepherd**、**OmegaPRM**、**rStar**。

## 用法
1. **Best-of-N / 重排**：生成 N 条解，PRM 选最好的。
2. **Beam Search / MCTS**：把 PRM 当价值函数做搜索。
3. **RL 训练**：作为 [[AI/00-AI知识体系/概念/02-训练与推理/02-RLVR|RLVR]] 的稠密信号补充。

## Verifier 的角色
- **天然 verifier**：代码可运行、数学有答案、Lean 可证。
- **学习型 verifier**：训练一个小判别模型，泛化到无法直接验证的任务。

## 与之相关
- [[AI/00-AI知识体系/概念/02-训练与推理/02-RLVR|RLVR]]
- [[AI/00-AI知识体系/概念/01-模型层/03-推理模型|推理模型]]
- [[AI/00-AI知识体系/概念/02-训练与推理/07-推理时增强-CoT|推理时增强-CoT]]
- [[AI/00-AI知识体系/概念/02-训练与推理/08-Test-time Compute Scaling|Test-time Compute Scaling]]

## 延伸阅读
- *Let's Verify Step by Step*（OpenAI, 2023）
- Math-Shepherd（PRM 自动标注）
- rStar-Math（小模型 + PRM 搜索做奥数）
