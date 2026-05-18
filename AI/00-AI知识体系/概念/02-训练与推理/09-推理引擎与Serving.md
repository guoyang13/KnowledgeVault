---
aliases:
  - 推理引擎与Serving
---

# 推理引擎与 Serving

> 把模型稳定、高吞吐、低延迟地对外提供服务的系统层，核心是调度、KV Cache 管理和硬件利用率。

## 关键点
- **推理引擎**：vLLM、SGLang、TensorRT-LLM、TGI、llama.cpp、MLX。
- **调度能力**：continuous batching、chunked prefill、prefix cache、speculative decoding。
- **部署指标**：TTFT、TPOT、吞吐、并发、显存占用、成本/百万 token。
- **生产关注**：限流、队列、熔断、模型热切换、可观测、灰度发布。

## 与之相关
- [[AI/00-AI知识体系/概念/02-训练与推理/06-推理加速三件套|推理加速三件套]]
- [[AI/00-AI知识体系/概念/01-模型层/07-上下文与KV Cache|上下文与KV Cache]]
- [[AI/00-AI知识体系/概念/01-模型层/08-Model Routing|Model Routing]]
- [[AI/00-AI知识体系/概念/05-评测/01-Eval Harness|Eval Harness]]

## 延伸阅读
- vLLM / SGLang / TensorRT-LLM 官方文档
- PagedAttention 与 continuous batching 相关论文
