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
- **DeepSeek-V2 / V3 / V4 / R1**：细粒度 MoE、低成本推理和 reasoning 路线，性价比标杆。
- **Mixtral 8x7B / 8x22B**：开源 MoE 启蒙。
- **Llama 4 Scout / Maverick**：Meta 明确公开的开放权重多模态 MoE，Scout 17B active / 16 experts，Maverick 17B active / 128 experts。
- **Qwen3 MoE**：Qwen3-30B-A3B、Qwen3-235B-A22B 等开放权重 MoE。
- **Mistral Large / Mixtral / Magistral 相关路线**：Mistral 长期推动开放和企业可部署模型。
- **GPT / Gemini / Claude 等闭源前沿模型**：外部通常推测大量使用稀疏化或路由思想，但未公开完整结构，不应写成确定事实。

## 2026 观察

- MoE 已经从“论文里的稀疏专家”变成开放权重模型的主流扩展方式。
- 关键指标不再只看总参数，而要看**总参数、激活参数、专家数、上下文长度、部署显存、吞吐和通信开销**。
- 对企业部署来说，MoE 的挑战是所有专家参数仍要驻留显存；即使每个 token 只激活少数专家，部署成本也不能只按 active 参数估算。

## 与之相关
- [[AI/00-AI学习体系/02-概念库/01-模型层/01-Transformer架构|Transformer架构]]
- [[AI/00-AI学习体系/02-概念库/01-模型层/03-推理模型|推理模型]]
- [[AI/00-AI学习体系/02-概念库/02-训练与推理/05-高效微调|高效微调]]

## 延伸阅读
- Switch Transformer (Google, 2021)
- DeepSeek-V3 技术报告
- Mixtral of Experts (Mistral)
- [[AI/00-AI学习体系/02-概念库/08-前沿动态/01-主流模型地图-2026-07|主流模型地图 2026-07]]
