# Transformer 架构

> 当前所有主流 LLM 的底层架构，核心是 Self-Attention + Decoder-only 设计。

## 关键点
- **Self-Attention**：每个 token 与序列中所有 token 计算相关性，捕捉长距离依赖。
- **Decoder-only**：相比早期 Encoder-Decoder 结构，GPT 系采用单一 Decoder 堆叠 + 因果掩码。
- **Position Encoding**：从绝对位置 → 相对位置 → **RoPE（旋转位置编码）**，RoPE 是当前事实标准。
- **Norm 位置**：Pre-LN（训练更稳）已基本取代 Post-LN。
- **激活函数**：从 ReLU/GeLU → **SwiGLU**（LLaMA 系普及）。
- **复杂度问题**：标准 Attention 是 O(n²)，是长上下文的主要瓶颈，催生了 FlashAttention、SSM、Sliding Window 等优化。

## 代表模型
- GPT 系、Claude、Gemini、Llama、Qwen、DeepSeek、Mistral 等几乎全部。

## 与之相关
- [[MoE]]
- [[新架构-SSM与Mamba]]
- [[上下文与KV Cache]]
- [[推理加速三件套]]

## 延伸阅读
- Vaswani et al., *Attention is All You Need* (2017)
- Su et al., *RoFormer* (RoPE 原论文)
- Llama / Qwen 等开源模型技术报告
