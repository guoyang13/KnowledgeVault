---
aliases:
  - LLM-as-a-Judge
---

# LLM-as-a-Judge

> 用强模型作为裁判评估回答质量，适合开放式问答、对话、摘要、RAG 和 Agent 任务。

## 关键点
- **评测形式**：pointwise 打分、pairwise 对比、ranking 排序、rubric 分项评分。
- **适用场景**：没有唯一标准答案，但需要比较帮助性、准确性、完整性、引用质量。
- **主要风险**：长度偏好、位置偏好、自家模型偏好、风格偏好、提示词敏感。
- **校准方式**：混入人工标注集、随机交换 A/B 顺序、多裁判投票、固定 rubric。

## 与之相关
- [[AI/00-AI知识体系/概念/05-评测/01-Eval Harness|Eval Harness]]
- [[AI/00-AI知识体系/概念/05-评测/02-Benchmark集合|Benchmark集合]]
- [[AI/00-AI知识体系/概念/04-RAG进阶/01-RAG基础|RAG基础]]
- [[AI/00-AI知识体系/概念/03-Agent系统/05-Agent Harness|Agent Harness]]

## 延伸阅读
- MT-Bench / Chatbot Arena 相关论文
- G-Eval、Prometheus、JudgeLM 等模型裁判研究
