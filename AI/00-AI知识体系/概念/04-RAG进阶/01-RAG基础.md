---
aliases:
  - RAG基础
---

# RAG 基础（Retrieval-Augmented Generation）

> 用"检索 + 拼到 prompt"的方式给 LLM 注入外部知识。对抗幻觉、降低成本、让答案可引用。

## 三阶段
1. **Indexing（建库）**：文档切分 → 嵌入 → 存入向量库/索引。
2. **Retrieval（检索）**：查询嵌入 → 召回 Top-K → 重排。
3. **Generation（生成）**：把召回片段拼入 prompt，让 LLM 基于此回答。

## 演化阶段
- **Naive RAG**：朴素切片 + 单次稠密检索 + 直接拼接。常见痛点：召回差、上下文乱、引用不准。
- **Advanced RAG**：在三阶段各点引入优化（见下）。
- **Modular RAG**：把流程模块化，可按任务编排（路由、多轮、self-RAG）。

## Advanced RAG 常用技巧
- **Query 优化**：Query Rewriting、HyDE（用 LLM 先伪造答案再检索）、Multi-Query。
- **Chunking 策略**：按语义切、滑窗、父子块（small-to-big）。
- **Hybrid Search**：稠密向量 + BM25 / SPLADE 关键词混合（见 [[GraphRAG与Agentic RAG]]）。
- **Reranker**：Cohere Rerank、BGE Reranker、Voyage Rerank 等 cross-encoder。
- **Citation / Grounding**：强制带引用并验证。
- **Self-RAG / Corrective RAG**：模型自评检索质量，必要时再搜或重写。

## 嵌入模型
- **闭源**：OpenAI text-embedding-3、Voyage、Cohere。
- **开源**：BGE 系（中英顶流）、E5、GTE、Jina、Qwen-Embedding。

## 向量数据库
- 开源：**pgvector**、**Milvus**、**Qdrant**、**Weaviate**、**LanceDB**、**Chroma**。
- 商业：Pinecone、Vespa、Vald。

## 与之相关
- [[GraphRAG与Agentic RAG]]
- [[Agent Memory]]
- [[Context Engineering]]
- [[上下文与KV Cache]]

## 延伸阅读
- *Retrieval-Augmented Generation*（Lewis et al., 2020）
- BGE 系列论文与 leaderboard
- LlamaIndex / LangChain RAG cookbooks
