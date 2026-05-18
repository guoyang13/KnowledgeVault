---
aliases:
  - 从RNN-CNN到Attention简史
---

# 从 RNN / CNN 到 Attention（简史）

> 不必精通每一种旧架构，但需要知道 **Transformer 解决了什么问题**，以及 LLM 为何几乎清一色 **Decoder-only**。

## 时间线（极简）

```text
N-gram LM → RNN/LSTM LM → Seq2Seq + Attention → Transformer (2017)
    → GPT (Decoder-only LM) → 规模化 LLM → MoE / 长上下文 / 推理模型
```

## RNN / LSTM

- **思路**：隐状态 \(h_t\) 逐步吞噬 \(x_t\)，把历史压进固定维向量
- **优点**：天然序列、参数量相对小
- **缺点**：
  - 长程依赖难（梯度消失/爆炸）
  - **无法高效并行**训练整句（时间步串行）
- **在 LLM 中的地位**：已被 Transformer 取代；理解即可

## CNN 在 NLP 中

- **1D 卷积 + 池化**：抓局部 n-gram 模式（Kim CNN 等）
- **优点**：并行好、局部模式强
- **缺点**：感受野有限；长依赖要叠很多层或空洞卷积
- **在 LLM 中的地位**：非主流；部分工作用卷积替代 FFN 或做局部混合（了解即可）

## Seq2Seq + Attention（2014–2017）

- **Encoder–Decoder**：机器翻译；编码器读源句，解码器生成目标句
- **Attention**：解码每一步**动态加权**看编码器各位置，缓解「整句压成一个向量」瓶颈
- **启示**：**按位置加权聚合信息** 比单一隐状态更有效 → 通向 Self-Attention

## Transformer（2017）

- **Self-Attention**：序列每个位置直接与其它位置交互（在窗口或掩码约束内）
- **并行**：同一层内各位置可并行算（训练吞吐远高于 RNN）
- 详见 [[AI/00-AI知识体系/概念/00-基础与LLM概论/07-注意力机制直觉-QKV|注意力机制直觉-QKV]] 与 [[AI/00-AI知识体系/概念/01-模型层/01-Transformer架构|Transformer架构]]

## 为何 LLM 多用 Decoder-only

| 架构 | 典型用途 | 代表 |
|------|----------|------|
| **Encoder-only** | 理解类（分类、嵌入） | BERT |
| **Encoder–Decoder** | 翻译、摘要（输入输出分明） | T5、BART |
| **Decoder-only** | **开放-ended 生成**、统一用「续写」完成各类任务 | GPT、Llama、Qwen、DeepSeek |

因果掩码（Causal Mask）：位置 \(t\) 只能看见 \(\le t\) 的 token，保证训练与自回归推理一致。  
当前 **Chat LLM 主流 = 堆叠 Decoder 层 + 因果 LM 目标**。

## 之后的新方向（点到为止）

- **长上下文**：稀疏 Attention、[[AI/00-AI知识体系/概念/01-模型层/06-新架构-SSM与Mamba|SSM与Mamba]]、滑动窗口
- **效率**：[[AI/00-AI知识体系/概念/01-模型层/02-MoE|MoE]]、[[AI/00-AI知识体系/概念/02-训练与推理/06-推理加速三件套|推理加速三件套]]
- **能力**：[[AI/00-AI知识体系/概念/01-模型层/03-推理模型|推理模型]]、[[AI/00-AI知识体系/概念/02-训练与推理/02-RLVR|RLVR]]

## 与之相关

- [[AI/00-AI知识体系/概念/00-基础与LLM概论/07-注意力机制直觉-QKV|注意力机制直觉-QKV]]
- [[AI/00-AI知识体系/概念/01-模型层/01-Transformer架构|Transformer架构]]
- [[AI/00-AI知识体系/概念/00-基础与LLM概论/04-语言模型与Next-Token-Prediction|语言模型与 Next-Token Prediction]]

## 延伸阅读

- Hochreiter & Schmidhuber, *LSTM* (1997)
- Bahdanau et al., *Neural Machine Translation by Jointly Learning to Align and Translate* (Attention)
- Vaswani et al., *Attention is All You Need* (2017)
- Radford et al., *Improving Language Understanding by Generative Pre-Training*（GPT-1）
