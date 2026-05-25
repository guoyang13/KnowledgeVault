---
aliases:
  - Embedding
  - Embeddings
  - embeding
  - 嵌入模型
  - 向量嵌入
  - 向量检索
---

# Embedding 模型与向量检索

> Embedding 是把文本、图片、代码等对象映射成向量，让机器可以用“距离/相似度”做检索、聚类、推荐和去重。RAG 里的召回能力，很大程度取决于 embedding 质量。

## 一句话理解

Embedding 模型做的事是：

```text
文本 / 代码 / 图片
→ 高维向量
→ 用相似度找到语义接近的内容
```

例如：

```text
"如何办理退款？"      → [0.12, -0.03, ...]
"退货退款流程是什么？" → [0.11, -0.02, ...]
"今天天气怎么样？"    → [-0.52, 0.77, ...]
```

前两句语义接近，向量距离也应更近。

## 和 LLM 内部 Token Embedding 的区别

| 类型 | 用途 | 位置 |
|---|---|---|
| **Token Embedding** | LLM 内部把 token ID 变成可计算向量 | 模型内部参数 |
| **Text / RAG Embedding** | 把一段文本变成向量，用于相似度检索 | 模型外部检索系统 |

LLM 内部 embedding 是生成模型的一部分；RAG embedding 是检索系统的一部分。二者都叫 embedding，但服务目标不同。

更基础的表示学习见 [[AI/00-AI知识体系/概念/00-基础与LLM概论/03-表示学习-从词嵌入到上下文|表示学习-从词嵌入到上下文]]。

## 在 RAG 中的位置

```text
文档
→ chunk 切分
→ embedding 模型向量化
→ 存入向量库
→ 用户问题向量化
→ 相似度召回 Top-K
→ rerank
→ 拼入 prompt
→ LLM 回答
```

对应 [[AI/00-AI知识体系/概念/04-RAG进阶/01-RAG基础|RAG基础]] 的 Indexing 和 Retrieval 阶段。

## 常见相似度

| 方法 | 直觉 | 常见用途 |
|---|---|---|
| Cosine similarity | 看方向是否接近 | 文本语义检索最常见 |
| Dot product | 向量点积 | 有些模型/向量库默认 |
| Euclidean distance | 几何距离 | 聚类、传统向量空间 |

实际使用时要看 embedding 模型和向量库推荐的 metric。不要随便混用。

## 选型维度

| 维度 | 关注点 |
|---|---|
| 语言 | 中文、英文、多语种、代码是否表现稳定 |
| 任务 | 问答检索、语义搜索、聚类、分类、去重 |
| 维度 | 向量维度越高通常成本和存储越大 |
| 上下文长度 | 单次可编码文本长度影响 chunk 策略 |
| 成本 | API 价格、本地推理成本、批量建库成本 |
| 延迟 | 在线查询时 embedding 延迟会影响体验 |
| 许可 | 商用授权、私有化部署、数据合规 |

## 常见模型

- **闭源 API**：OpenAI text-embedding 系列、Voyage、Cohere。
- **开源模型**：BGE、E5、GTE、Jina Embeddings、Qwen-Embedding。
- **Reranker**：BGE Reranker、Cohere Rerank、Voyage Rerank 等，常用于召回后精排。

注意：embedding 模型和 reranker 不是一回事。Embedding 负责快速召回，reranker 负责更精细地重排候选片段。

## 常见坑

- **只看向量相似度，不看关键词**：专有名词、编号、SKU、API 名称常需要 BM25 / Hybrid Search。
- **chunk 太大或太小**：太大噪音多，太小丢上下文。
- **查询和文档不对称**：用户问题短，文档片段长，可能需要 query rewriting 或 HyDE。
- **模型换了不重建索引**：embedding 模型变化后，历史向量通常需要重算。
- **没有评测集**：只凭感觉调 Top-K、chunk size、模型，很容易误判。

## 最小评测方法

准备 20-50 个真实问题，每个问题标注应该召回的文档片段。

记录：

```text
Recall@K
MRR
命中片段质量
是否支持最终答案
成本与延迟
```

先让检索评测过关，再调生成 prompt。RAG 的很多问题其实是检索没召回，而不是 LLM 不会回答。

## 与之相关

- [[AI/00-AI知识体系/概念/00-基础与LLM概论/03-表示学习-从词嵌入到上下文|表示学习-从词嵌入到上下文]]
- [[AI/00-AI知识体系/概念/04-RAG进阶/01-RAG基础|RAG基础]]
- [[AI/00-AI知识体系/概念/04-RAG进阶/02-GraphRAG与Agentic RAG|GraphRAG与Agentic RAG]]
- [[AI/00-AI知识体系/概念/03-Agent系统/08-Agent Memory|Agent Memory]]
- [[AI/00-AI知识体系/概念/05-评测/01-Eval Harness|Eval Harness]]

## 学习检查

1. Token embedding 和 RAG embedding 有什么区别？
2. 为什么同一个 embedding 模型换成另一个后通常要重建向量索引？
3. 为什么专有名词检索常常需要 Hybrid Search？
4. Reranker 和 embedding 模型分别解决什么问题？
