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

可以把它想成一次检索：

- Query：我现在要找什么信息？
- Key：每个位置挂出的可匹配标签。
- Value：一旦匹配上，真正拿走的内容。

注意：Q/K/V 不是人工写死的字段，而是训练出来的向量投影。

## Scaled Dot-Product Attention（单头）

\[
\text{Attention}(Q,K,V) = \text{softmax}\left(\frac{QK^\top}{\sqrt{d_k}}\right) V
\]

- \(QK^\top\)：所有位置两两相似度（注意力分数）
- 除以 \(\sqrt{d_k}\)：防止维度过大时 softmax 过尖
- **softmax**：变成权重（每行和为 1）
- 乘 \(V\)：按权重混合各位置的 value

## 一个简化例子

句子：

```text
小明 把 书 放进 书包，因为 他 要 上学
```

当模型处理“他”这个位置时，一个注意力头可能学会把较高权重放到“小明”；另一个头可能关注“上学”相关的动作结构。多层、多头叠加后，模型逐步形成更丰富的上下文表示。

这不是硬编码规则，而是通过 next-token 训练目标间接学出来的统计结构。

## Multi-Head Attention

- 多套 \(W^Q,W^K,W^V\)，在不同子空间学不同关系（语法、指代、格式…）
- 多头结果拼接后再经 \(W^O\) 投影回 \(d_{\text{model}}\)

## Causal（因果）掩码 —— LLM 必备

Decoder-only 在位置 \(i\) **不能看见** \(j > i\)：

- 训练：与「预测下一 token」一致，不偷看未来
- 推理：生成第 \(t\) 个 token 时，只 attend 到已有前缀

如果训练时允许看未来，模型会作弊：预测当前位置时直接偷看答案。这样训练 loss 很低，但推理时没有未来 token 可看，能力会崩。

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

## 常见误区

- **Attention 权重就是解释。** 它能提供线索，但不能简单等同于模型“为什么这么想”。
- **所有头都学语法关系。** 有些头负责格式、位置、复制、局部模式，甚至可能冗余。
- **长上下文一定更懂全文。** Attention 成本和位置效应都会影响长文本使用，仍需要 RAG、摘要和上下文工程。

## 学习检查

1. Q、K、V 分别承担什么角色？
2. 为什么要除以 \(\sqrt{d_k}\)？
3. Causal mask 为什么是 Decoder-only LLM 的核心约束？
4. KV Cache 缓存的是哪两个东西？为什么能加速推理？

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
