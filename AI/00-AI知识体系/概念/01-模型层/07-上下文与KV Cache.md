---
aliases:
  - 上下文与KV Cache
---

# 上下文窗口与 KV Cache

> Context Window 决定模型"看得多远"；KV Cache 决定它"看得多快、多便宜"。

## 关键点
- **Context Window**：模型一次能处理的最大 token 数。2025–2026 主流：128k–1M+，Gemini 已到 1M–2M。
- **Lost in the Middle**：上下文越长，模型对中间部分的注意力越弱，长上下文 ≠ 真正"读懂"。
- **KV Cache**：自回归生成时缓存历史 K/V，避免重算，是推理性能核心。
  - **Prefix Caching / Prompt Cache**：相同前缀共享 KV，显著降本（OpenAI、Anthropic、DeepSeek 都支持）。
  - **PagedAttention（vLLM）**：分页管理 KV，提升吞吐。
  - **MQA / GQA / MLA**：降低 KV 显存的 attention 变体（Llama、Qwen、DeepSeek）。
- **TTFT（Time To First Token）**：首 token 延迟，长上下文场景的关键指标。

## 长上下文工程实践
- 把不变的"系统 + 工具 + 文档"放最前以命中 prefix cache。
- 长文档建议先 RAG 召回再喂入，而不是无脑全塞。
- 关键信息放在头尾，不要埋在中间。

## 与之相关
- [[AI/00-AI知识体系/概念/01-模型层/01-Transformer架构|Transformer架构]]
- [[AI/00-AI知识体系/概念/02-训练与推理/06-推理加速三件套|推理加速三件套]]
- [[AI/00-AI知识体系/概念/04-RAG进阶/01-RAG基础|RAG基础]]
- [[AI/00-AI知识体系/概念/06-工程生态/02-Context Engineering|Context Engineering]]

## 延伸阅读
- *Lost in the Middle*（Liu et al., 2023）
- vLLM PagedAttention 论文
- DeepSeek-V2 MLA 设计
