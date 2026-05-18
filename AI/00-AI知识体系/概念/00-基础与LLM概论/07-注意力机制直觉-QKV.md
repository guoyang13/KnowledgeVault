---
aliases:
  - 注意力机制直觉-QKV
---

# 注意力机制直觉：Q / K / V

> **Attention** 让序列中每个位置根据与其它位置的相关性，**加权汇总**信息。读懂 Q/K/V，再读 [[AI/00-AI知识体系/概念/01-模型层/01-Transformer架构|Transformer架构]] 会轻松很多。

## 一句话直觉

对每个位置 \(i\)，问一句：**「我应该从哪些位置 \(j\) 多拿一点信息？」**  
用 **Query** 与所有 **Key** 的相似度当权重，对 **Value** 做加权平均，得到新表示。

## Q / K / V 是什么

由同一层的 hidden state \(X\) 经三个线性变换得到：

\[
Q = X W^Q,\quad K = X W^K,\quad V = X W^V
\]

| 符号 | 口语理解 |
|------|----------|
| **Query** | 当前位置「在找什么」 |
| **Key** | 各位置「能提供什么标签/索引」 |
| **Value** | 各位置「实际要传递的内容」 |

## Scaled Dot-Product Attention（单头）

\[
\text{Attention}(Q,K,V) = \text{softmax}\left(\frac{QK^\top}{\sqrt{d_k}}\right) V
\]

- \(QK^\top\)：所有位置两两相似度（注意力分数）
- 除以 \(\sqrt{d_k}\)：防止维度过大时 softmax 过尖
- **softmax**：变成权重（每行和为 1）
- 乘 \(V\)：按权重混合各位置的 value

## Multi-Head Attention

- 多套 \(W^Q,W^K,W^V\)，在不同子空间学不同关系（语法、指代、格式…）
- 多头结果拼接后再经 \(W^O\) 投影回 \(d_{\text{model}}\)

## Causal（因果）掩码 —— LLM 必备

Decoder-only 在位置 \(i\) **不能看见** \(j > i\)：

- 训练：与「预测下一 token」一致，不偷看未来
- 推理：生成第 \(t\) 个 token 时，只 attend 到已有前缀

## 与 FFN、残差、LayerNorm 的关系（一层 Transformer Block）

```text
x → LayerNorm → Multi-Head Attention → 残差加回 x
  → LayerNorm → FFN (通常 SwiGLU) → 残差加回
```

堆叠 \(L\) 层（如 32～80+）即深度 LLM。**FFN** 常占参数量大头；**Attention** 负责 token 间信息路由。

## 复杂度与工程

- 标准 Attention 对序列长度 \(n\) 为 **\(O(n^2)\)**（内存与算力）
- 催生：FlashAttention、GQA/MLA、滑动窗口、[[AI/00-AI知识体系/概念/01-模型层/06-新架构-SSM与Mamba|SSM]] 等
- 推理时用 **KV Cache** 缓存历史 K/V：见 [[AI/00-AI知识体系/概念/01-模型层/07-上下文与KV Cache|上下文与KV Cache]]

## 与之相关

- 架构总览：[[AI/00-AI知识体系/概念/01-模型层/01-Transformer架构|Transformer架构]]
- 简史：[[AI/00-AI知识体系/概念/00-基础与LLM概论/06-从RNN-CNN到Attention简史|从 RNN/CNN 到 Attention 简史]]
- 表示：[[AI/00-AI知识体系/概念/00-基础与LLM概论/03-表示学习-从词嵌入到上下文|表示学习-从词嵌入到上下文]]
- 位置编码：RoPE 等在 [[AI/00-AI知识体系/概念/01-模型层/01-Transformer架构|Transformer架构]] 中展开

## 延伸阅读

- Vaswani et al., *Attention is All You Need* — 原图与公式
- Jay Alammar — *The Illustrated Transformer*
- Lilian Weng — *Attention? Attention!*
- Dao et al., *FlashAttention*（工程实现）
