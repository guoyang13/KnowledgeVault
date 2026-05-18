---
aliases:
  - 新架构-SSM与Mamba
---

# 新架构：SSM 与 Mamba

> 用状态空间模型替代/补充 Attention，解决 Transformer 在长序列上的 O(n²) 复杂度问题。

## 关键点
- **SSM（State Space Model）**：用线性递归 + 选择性机制建模序列，复杂度 O(n)。
- **Mamba / Mamba-2**：把 SSM 工程化，可与 Transformer 竞争甚至超越某些任务。
- **Hybrid Attention + SSM**：实践证明纯 SSM 在 in-context learning 上不如 Transformer，业界主流做"混合"。
  - **Jamba**（AI21）：Transformer + Mamba 块交替。
  - **Hymba**（NVIDIA）、**Zamba**（Zyphra）、**Samba**（Microsoft）。
- **优势**：超长上下文（百万级）、推理常数显存。
- **限制**：训练生态不如 Transformer 成熟；某些"召回"任务弱于 Attention。

## 相邻概念
- **Linear Attention**、**RWKV**（也是线性复杂度路线，可视为 SSM 的"亲戚"）。
- **Diffusion LM**：用扩散思路生成文本，也是非主流但值得关注的探索方向。

## 与之相关
- [[Transformer架构]]
- [[上下文与KV Cache]]
- [[推理加速三件套]]

## 延伸阅读
- Mamba: Linear-Time Sequence Modeling (Gu & Dao, 2023)
- Jamba (AI21, 2024)
- RWKV 论文及社区
