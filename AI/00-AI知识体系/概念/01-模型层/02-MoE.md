---
aliases:
  - MoE
---

# MoE（Mixture of Experts）

> 稀疏专家网络：模型有海量参数，但每个 token 只激活其中一小部分，做到"激活参数 ≪ 总参数"。

## 关键点
- **结构**：FFN 被替换为 N 个"专家 FFN" + 一个 **Router/Gate** 网络，每 token 选 top-k 专家。
- **优势**：同等推理成本下容量大幅提升；同等容量下推理便宜。
- **挑战**：
  - **负载均衡**：避免少数专家被打爆（用辅助 loss、Expert Choice 路由等）。
  - **通信开销**：分布式训练时 All-to-All 是瓶颈。
  - **显存**：所有专家都要驻留显存。
- **Fine-grained Expert**（DeepSeek-V2/V3）：把专家切得更细、共享专家 + 路由专家。
- **Shared Expert**：一部分专家所有 token 都过，作为"通识"。

## 代表模型
- **DeepSeek-V2 / V3 / R1**：细粒度 MoE + MLA，性价比标杆。
- **Mixtral 8x7B / 8x22B**：开源 MoE 启蒙。
- **Qwen 系 MoE**、**GLM-4 MoE**、**Grok**。
- **GPT-4 / GPT-5 / Gemini 1.5+** 普遍被认为是 MoE（未公开细节）。

## 与之相关
- [[Transformer架构]]
- [[推理模型]]
- [[高效微调]]

## 延伸阅读
- Switch Transformer (Google, 2021)
- DeepSeek-V3 技术报告
- Mixtral of Experts (Mistral)
