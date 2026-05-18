---
aliases:
  - GraphRAG与Agentic RAG
---

# GraphRAG 与 Agentic RAG

> 朴素 RAG 解决不了"跨文档综合""模糊全局问题"，进阶方向有两条：**用图组织知识** 和 **让 Agent 主动检索**。

## GraphRAG
- **思想**：先用 LLM 把文档抽成实体-关系图，再在图上做检索与摘要。
- **流程**：
  1. 实体抽取 → 关系抽取 → 构建知识图。
  2. 社区检测（Leiden 等）→ 每个社区生成摘要。
  3. 查询时：局部检索（实体邻域） + 全局检索（社区摘要）。
- **强在**：跨文档归纳、"这本书在讲什么"这类全局问题。
- **代表**：**Microsoft GraphRAG**、**LightRAG**、**HippoRAG**、**Neo4j + LLM**。

## Hybrid Search & Rerank（基础但关键）
- **Dense + Sparse**：向量检索 + BM25 / SPLADE，互补长尾。
- **Reranker**：cross-encoder 重排 top-K，是最便宜的提分点之一。

## Agentic RAG
- **思想**：不再"一次检索 + 一次生成"，而是让 Agent **自主决定**：
  - 要不要检索？检索什么？检索几次？
  - 召回质量差时是否重写 query？
  - 答案需要哪些来源支撑？
- **典型流程**：Plan → Retrieve → Read → Re-plan → Synthesize。
- **代表模式**：
  - **Self-RAG**：模型输出特殊 token 触发检索 / 自评。
  - **Corrective RAG**：评分召回质量，差就再搜。
  - **ReAct + RAG**：把"搜索"作为工具调用。
  - **Deep Research**：长 horizon 多轮检索 + 综合（OpenAI Deep Research、Gemini Deep Research）。

## 何时该升级
- 朴素 RAG 已无法回答跨文档 / 全局问题 → 上 GraphRAG。
- 单次召回不够、答案需要多步推理 → 上 Agentic RAG。
- 注意：复杂度和成本都会显著上升，先确认朴素 RAG 真的不够。

## 与之相关
- [[AI/00-AI知识体系/概念/04-RAG进阶/01-RAG基础|RAG基础]]
- [[AI/00-AI知识体系/概念/03-Agent系统/08-Agent Memory|Agent Memory]]
- [[AI/00-AI知识体系/概念/03-Agent系统/02-ReAct与Agent范式|ReAct与Agent范式]]
- [[AI/00-AI知识体系/概念/03-Agent系统/04-Multi-Agent编排|Multi-Agent编排]]

## 延伸阅读
- Microsoft *GraphRAG* 论文与开源实现
- *Self-RAG*（Asai et al., 2023）
- *Corrective RAG*（Yan et al., 2024）
- OpenAI Deep Research 公告与博客
