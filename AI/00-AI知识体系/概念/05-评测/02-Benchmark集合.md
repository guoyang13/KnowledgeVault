---
aliases:
  - Benchmark集合
---

# Benchmark 集合（2025–2026 重点）

> 按"能力维度"分类的主流评测集。配合 [[AI/00-AI知识体系/概念/05-评测/01-Eval Harness|Eval Harness]] 使用。

## 通用知识与推理
- **MMLU / MMLU-Pro**：57 学科多选题，老牌通用基准（已接近饱和）。
- **GPQA**：研究生级科学题，目前仍有挑战性。
- **BBH（Big-Bench Hard）**：困难推理任务集合。
- **AGIEval**：高考/公务员/律师等中英人类考试。

## 数学
- **GSM8K**：小学数学题，已饱和。
- **MATH**：竞赛级数学题。
- **AIME / IMO / Putnam**：奥数级，推理模型的主战场。

## 编程
- **HumanEval / HumanEval+**、**MBPP / MBPP+**：函数级代码题。
- **LiveCodeBench**：滚动更新、抗污染。
- **SWE-bench / SWE-bench Verified / Multimodal**：真实 GitHub issue 修复，**Agent 编程能力的金标准**。
- **Terminal-Bench**：终端命令行任务。
- **BigCodeBench**、**ClassEval**：更贴近真实工程。

## Agent / 工具 / GUI
- **τ-Bench**：客服 + 工具调用。
- **WebArena / VisualWebArena**：浏览器 Agent。
- **OSWorld**：跨应用桌面 Agent。
- **Mind2Web**、**WorkArena**：企业场景 Web Agent。
- **GAIA**：通用 Assistant，强调多步骤 + 多工具。

## RAG / 长上下文
- **RGB / CRAG / FRAMES**：RAG 综合评测。
- **NIAH（Needle in a Haystack）**：长上下文召回（已基本饱和）。
- **RULER**、**LongBench**：更难的长上下文。

## 安全 / 对齐
- **HarmBench**、**JailbreakBench**：越狱与有害内容。
- **AgentDojo**：Agent 安全 / prompt injection。
- **Anthropic Sycophancy Eval**、**TruthfulQA**：迎合 / 真实性。

## 多模态
- **MMMU**、**MathVista**、**ChartQA**、**DocVQA**、**MMBench**。
- **Video-MME**、**EgoSchema**（视频）。

## 评测方法
- **LLM-as-a-Judge**：用强模型评弱模型，**MT-Bench / Arena-Hard** 是典型。
- **Chatbot Arena**：人类双盲投票，Elo 排行（最贴近真实使用感）。

## 使用建议
- 看综合：**MMLU-Pro + GPQA + MATH + LiveCodeBench + Arena-Hard**。
- 看 Agent：**SWE-bench Verified + τ-Bench + OSWorld**。
- 看长上下文：**RULER + LongBench**。
- 永远关注**发布时间 vs 模型训练时间**，警惕数据污染。

## 与之相关
- [[AI/00-AI知识体系/概念/05-评测/01-Eval Harness|Eval Harness]]
- [[AI/00-AI知识体系/概念/03-Agent系统/05-Agent Harness|Agent Harness]]
- [[AI/00-AI知识体系/概念/06-工程生态/01-Coding Agent|Coding Agent]]

## 延伸阅读
- Papers With Code Leaderboards
- LMSYS Chatbot Arena
- SWE-bench / τ-Bench 论文与官方网站
