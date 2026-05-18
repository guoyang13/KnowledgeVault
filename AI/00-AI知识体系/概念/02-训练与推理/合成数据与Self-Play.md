# 合成数据与 Self-Play

> 用强模型 / 模型自己生成训练数据。当下几乎所有前沿模型都重度依赖合成数据。

## 关键点
- **为什么火**：高质量人工数据见顶；模型已强到能生成"教师级别"的数据。
- **典型用法**：
  - **指令数据合成**：让强模型造 SFT 样本（Self-Instruct、Evol-Instruct）。
  - **偏好数据合成**：让强模型当 judge 造 (chosen, rejected) 对（RLAIF）。
  - **推理轨迹合成**：用 [[RLVR]] 选出正确解作为 SFT 数据 → 蒸馏。
  - **能力专项数据**：数学题、代码题、工具调用样本批量生成。
- **Self-Play**：模型与自己博弈/纠错（辩论、debate-style alignment、对抗生成）。
- **数据飞轮**：好模型 → 造更好数据 → 训更好模型，是 R1 / Phi / Llama 3+ 的共同套路。

## 风险
- **Model Collapse**：纯靠合成数据迭代会导致分布坍塌、多样性丢失。
- **Distillation 同质化**：所有小模型都从同一大模型蒸出来，行为高度相似。
- **缓解**：保留真实数据底盘、引入多样性、过滤低质量样本。

## 工具
- **Distilabel**、**Argilla**：合成数据管道。
- **DataDreamer**、**Curator**：研究/工业级合成数据框架。

## 与之相关
- [[训练阶段与对齐方法]]
- [[RLVR]]
- [[推理模型]]
- [[小模型与端侧]]

## 延伸阅读
- Self-Instruct（Wang et al., 2022）
- Phi 系列 *textbook-quality data* 思路
- *The Curse of Recursion*（Model Collapse 论文）
