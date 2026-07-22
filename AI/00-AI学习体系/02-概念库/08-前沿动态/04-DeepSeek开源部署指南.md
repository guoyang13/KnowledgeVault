---
aliases:
  - DeepSeek开源
  - DeepSeek部署
  - DeepSeek开源地址
---

# DeepSeek 开源部署指南

> 更新时间：2026-07-08。
> 覆盖：DeepSeek 开源是什么、官方地址、Ollama / vLLM / API 三种用法。

## DeepSeek 开源是什么

DeepSeek 在 Hugging Face / ModelScope / GitHub 公开发布**模型权重**（多为 MIT），可下载、本地部署、微调；**不等于**训练数据和全部训练流程公开。

与官方产品的关系：

| | 开源权重 | 官方 API / Chat |
|---|---|---|
| 地址 | Hugging Face / ModelScope | platform.deepseek.com / chat.deepseek.com |
| 本地部署 | ✅ | ❌ |
| 数据出网 | 可控（内网） | 走公网 |

## 主流开源系列（2026）

| 系列 | 总参数 | 激活参数 | 上下文 | 适合 |
|---|---|---|---|---|
| **V3 / V3-Base** | 671B | 37B | 128K | 通用对话、代码 |
| **R1 / R1-Zero** | 671B | 37B | 128K | 数学、逻辑、推理 |
| **R1-Distill-Qwen/Llama** | 1.5B～70B | 全量 | — | **个人本地**（Ollama） |
| **V4-Flash** | 284B | 13B | **1M** | 长上下文、高效率 |
| **V4-Pro** | 1.6T | 49B | **1M** | 最强能力、部署成本极高 |

架构均为 **MoE**；671B 是总参数量，推理时每次只激活一部分专家。见 [[AI/00-AI学习体系/02-概念库/01-模型层/02-MoE|MoE]]。

## 官方开源地址

### 组织入口

| 平台 | 地址 |
|---|---|
| GitHub | https://github.com/deepseek-ai |
| Hugging Face | https://huggingface.co/deepseek-ai |
| ModelScope（国内） | https://modelscope.cn/organization/deepseek-ai |

### GitHub 仓库

| 模型 | 仓库 |
|---|---|
| DeepSeek-V4 | https://github.com/deepseek-ai/DeepSeek-V4 |
| DeepSeek-V3 | https://github.com/deepseek-ai/DeepSeek-V3 |
| DeepSeek-R1 | https://github.com/deepseek-ai/DeepSeek-R1 |

### Hugging Face 权重（常用）

**V4**

- https://huggingface.co/deepseek-ai/DeepSeek-V4-Pro
- https://huggingface.co/deepseek-ai/DeepSeek-V4-Flash

**V3**

- https://huggingface.co/deepseek-ai/DeepSeek-V3
- https://huggingface.co/deepseek-ai/DeepSeek-V3-Base

**R1**

- https://huggingface.co/deepseek-ai/DeepSeek-R1
- https://huggingface.co/deepseek-ai/DeepSeek-R1-Zero

**R1 蒸馏（本地友好）**

- https://huggingface.co/deepseek-ai/DeepSeek-R1-Distill-Qwen-7B
- https://huggingface.co/deepseek-ai/DeepSeek-R1-Distill-Qwen-32B
- https://huggingface.co/deepseek-ai/DeepSeek-R1-Distill-Llama-8B

### 非开源（在线服务）

- Chat：https://chat.deepseek.com
- API：https://platform.deepseek.com

## 三种部署方式

### 1. Ollama — 个人最简单

详见 [[AI/02-环境与实操/Tools/ollama/Ollama|Ollama]]。

```bash
ollama pull deepseek-r1:7b
ollama run deepseek-r1:7b
```

- 本地 API：`http://localhost:11434`
- 适合：7B～32B 蒸馏版；Mac M 系列友好
- 不适合：V3/R1 全量 671B、V4

### 2. vLLM — 团队/生产

```bash
pip install huggingface_hub vllm

huggingface-cli download deepseek-ai/DeepSeek-R1 --local-dir ./DeepSeek-R1

python -m vllm.entrypoints.openai.api_server \
  --model ./DeepSeek-R1 \
  --tensor-parallel-size 8 \
  --port 8000
```

- 输出 OpenAI 兼容 API：`http://localhost:8000/v1`
- V3/R1 全量通常需 **多卡 A100/H100**（如 8×80GB）
- 见 [[AI/00-AI学习体系/02-概念库/02-训练与推理/09-推理引擎与Serving|推理引擎与Serving]]

### 3. 官方 API — 不想管 GPU

```bash
curl https://api.deepseek.com/chat/completions \
  -H "Authorization: Bearer $DEEPSEEK_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"deepseek-chat","messages":[{"role":"user","content":"你好"}]}'
```

## 选型速查

| 你是谁 | 建议 |
|---|---|
| 个人学习 | Ollama + `deepseek-r1:7b` |
| 小团队内网 | 1～2 张 24GB 卡 + R1-Distill-32B |
| 公司有 GPU 集群 | vLLM + DeepSeek-V3 / R1 |
| 不想运维 | 官方 API |
| 超长文档（1M） | V4 API 或自建 V4（成本极高） |

## R1 vs V3

| | V3 | R1 |
|---|---|---|
| 强项 | 通用对话、代码 | 数学、逻辑、复杂推理 |
| 速度 | 相对快 | 更慢（含思考过程） |
| 本地 | 蒸馏小模型 via Ollama | 同左；全量需集群 |

## 与之相关

- 开放权重概念：[[AI/00-AI学习体系/02-概念库/00-基础与LLM概论/08-开源权重与模型文件|开源权重与模型文件]]
- 权重文件详解：[[AI/00-AI学习体系/02-概念库/08-前沿动态/06-DeepSeek开源权重文件说明|DeepSeek开源权重文件说明]]
- 训练与微调：[[AI/00-AI学习体系/02-概念库/02-训练与推理/10-训练蒸馏与微调实战|训练蒸馏与微调实战]]
- RLVR：[[AI/00-AI学习体系/02-概念库/02-训练与推理/02-RLVR|RLVR]]
- 模型地图：[[AI/00-AI学习体系/02-概念库/08-前沿动态/01-主流模型地图-2026-07|主流模型地图 2026-07]]
- Ollama 详解：[[AI/02-环境与实操/Tools/ollama/Ollama|Ollama]] · [[AI/02-环境与实操/Tools/ollama/Ollama-API|Ollama API]]

## 对话整理入口

本次讨论从「开源权重四大家族」到「权重文件 → 训练 → 蒸馏 → Ollama 部署 → DeepSeek 地址」的完整路径，见 [[AI/00-AI学习体系/02-概念库/08-前沿动态/05-开放权重本地部署学习路径-2026-07|开放权重本地部署学习路径 2026-07]]。
