---
aliases:
  - 语言模型与Next-Token-Prediction
---

# 语言模型与 Next-Token Prediction

> **语言模型（Language Model, LM）** 对文本序列的概率建模；现代 LLM 几乎全部采用 **自回归、下一 token 预测** 作为预训练目标。

## 语言模型在学什么

给定前文 \(x_{<t} = x_1,\ldots,x_{t-1}\)，估计：

\[
P(x_t \mid x_1,\ldots,x_{t-1})
\]

整句概率（链式法则）：

\[
P(x_1,\ldots,x_n) = \prod_{t=1}^{n} P(x_t \mid x_{<t})
\]

**训练**：用大规模语料，在每个位置用**真实**下一 token 作标签，最小化交叉熵。  
**推理**：从 prompt 开始，反复采样 \(x_{t+1}\)，拼回序列，即「生成」。

## 一个极小例子

前文是：

```text
我 今天 想 吃
```

模型不会直接输出“正确答案”，而是输出整个词表上的概率分布，例如：

| 候选 token | 概率 |
|---|---:|
| 饭 | 0.35 |
| 面 | 0.22 |
| 火锅 | 0.18 |
| 苹果 | 0.05 |
| 其他 | 0.20 |

解码策略会从这个分布里选一个 token。选出“火锅”后，新上下文变成“我 今天 想 吃 火锅”，模型再预测下一个 token。

## 从 N-gram 到神经网络 LM（简史脉络）

| 阶段 | 思路 | 局限 |
|------|------|------|
| **N-gram** | 用前 \(n-1\) 个词统计下一词频率 | 稀疏、无法泛化、上下文短 |
| **RNN / LSTM LM** | 隐状态压缩历史 | 长程梯度难、难并行 |
| **Transformer LM** | Self-Attention 直接看全局（窗口内）上下文 | 成为 LLM 主流，见 [[AI/00-AI知识体系/概念/00-基础与LLM概论/06-从RNN-CNN到Attention简史|从 RNN/CNN 到 Attention 简史]] |

## 为什么「预测下一个词」能变「聪明」

- 要预测得好，模型必须隐式学习**语法、事实关联、常识、代码模式**等
- 规模与数据上去后，**下游任务**可通过提示（ICL）或微调（SFT）激发，而不必为每个任务单独建模型
- 这是 **「预训练 + 对齐」** 范式的根基：见 [[AI/00-AI知识体系/概念/02-训练与推理/01-训练阶段与对齐方法|训练阶段与对齐方法]]

## 训练 vs 推理（应用视角）

| | 训练 | 推理（Serving） |
|--|------|-----------------|
| 目标 | 拟合数据分布、降低 loss | 低延迟、高吞吐生成 |
| 计算 | 通常看**整段**并行算（teacher forcing） | **逐 token** 自回归，KV Cache 复用历史 |
| 采样 | 多用 argmax 或固定策略评估 | temperature、top-p、停止条件 |

## 解码参数在控制什么

| 参数 | 直觉 |
|---|---|
| **temperature** | 调整分布的尖锐程度；低温更保守，高温更多样 |
| **top-k** | 只从概率最高的 k 个 token 里选 |
| **top-p** | 只从累计概率达到 p 的候选集合里选 |
| **max tokens** | 最多生成多少 token |
| **stop / EOS** | 生成到指定符号或结束 token 时停止 |

这些参数不改变模型学到的知识，只改变“从概率分布里怎么拿下一个 token”。

## 与「对话模型」的关系

- 裸 LM 只会**续写**；Chat 模型在 SFT / RLHF 后学会遵循 **指令格式**（system / user / assistant）
- API 里的 `messages` 会被模板化成一段 token 序列，仍是 next-token 生成

## 常见误区

- **预测下一个 token 太简单，所以不可能产生推理。** 训练目标简单，不代表学到的内部表示简单；要预测海量复杂文本，模型会学到大量隐含结构。
- **模型每次生成一句话。** 实际上是逐 token 生成；流式输出只是把 token 及时展示出来。
- **概率最高的一定最好。** 创作、代码、问答需要的解码策略不同，贪心选择有时会变死板或陷入重复。

## 学习检查

1. 什么是 teacher forcing？为什么训练时可以直接用真实前文？
2. 为什么 Chat messages 最后仍会变成一段 token 序列？
3. temperature 改变的是模型参数、概率分布，还是外部采样策略？

## 与之相关

- [[AI/00-AI知识体系/概念/00-基础与LLM概论/01-什么是LLM|什么是LLM]]
- [[AI/00-AI知识体系/概念/00-基础与LLM概论/02-神经网络与反向传播|神经网络与反向传播]]
- [[AI/00-AI知识体系/概念/00-基础与LLM概论/05-Tokenizer与分词|Tokenizer与分词]]
- [[AI/00-AI知识体系/概念/01-模型层/01-Transformer架构|Transformer架构]]
- [[AI/00-AI知识体系/概念/01-模型层/07-上下文与KV Cache|上下文与KV Cache]]
- [[AI/00-AI知识体系/概念/02-训练与推理/07-推理时增强-CoT|推理时增强-CoT]]

## 延伸阅读

- Jurafsky & Martin — *Speech and Language Processing*（语言模型章节）
- Radford et al., *Language Models are Unsupervised Multitask Learners*（GPT-2）
- Kaplan et al., *Scaling Laws for Neural Language Models*
