---
aliases:
  - DeepSeek权重文件
  - DeepSeek开源文件
  - DeepSeek safetensors
---

# DeepSeek 开源权重文件说明

> 更新时间：2026-07-08。
> 说明 Hugging Face / ModelScope 下载的 DeepSeek 权重包里有哪些文件、各自作用；GitHub 代码仓库另述。

## 两类「开源 DeepSeek」

| 来源 | 典型内容 | 用途 |
|---|---|---|
| **Hugging Face / ModelScope** | 权重 + config + tokenizer | 推理、微调 |
| **GitHub**（DeepSeek-V3/R1/V4） | 推理脚本、权重文档、转换工具 | 本地部署、格式理解 |
| **Ollama** | 不直接暴露原始文件 | 内部为转换后的量化模型 |

权重本体在 **HF / ModelScope**；GitHub 主要是代码与 `README_WEIGHTS.md`。

---

## 通用文件（所有系列常见）

| 文件 | 作用 |
|---|---|
| **`config.json`** | 模型结构：层数、隐藏维度、MoE 专家数、`model_type` 等；推理框架据此建网络 |
| **`generation_config.json`** | 生成默认参数：max_length、temperature、stop token 等 |
| **`tokenizer.json`** | 分词器主文件：文本 ↔ token ID |
| **`tokenizer_config.json`** | 分词器配置：特殊 token、chat 模板引用 |
| **`tokenizer.model`** | SentencePiece 词表（部分模型有） |
| **`special_tokens_map.json`** | `<\|begin▁of▁sentence\|>` 等特殊 token 映射 |
| **`model-*.safetensors`** | **权重本体**（神经网络参数，体积最大） |
| **`model.safetensors.index.json`** | 分片索引：每个参数在哪个文件、偏移与大小 |
| **`README.md`** | 模型说明、评测、用法、License |
| **`LICENSE` / `LICENSE-MODEL`** | 许可（MIT 为主；蒸馏版还受 Qwen/Llama 约束） |

```text
推理最少需要：config.json + tokenizer* + 全部 model-*.safetensors (+ index.json)
```

---

## 文件总览

```text
开源 DeepSeek 下载包（HF）
├── 配置层
│   ├── config.json              → 网络结构（层数、MoE、MTP）
│   ├── generation_config.json   → 生成默认行为
│   └── quantization_config      → FP8/FP4 量化（如有）
├── 分词层
│   ├── tokenizer.json
│   ├── tokenizer_config.json
│   └── special_tokens_map.json  → 文本 ↔ token
├── 权重层（核心）
│   ├── model.safetensors.index.json
│   └── model-00001~N.safetensors  → 神经网络参数
└── 说明层
    ├── README.md
    └── LICENSE
```

---

## V3 / R1 全量（671B）特有

HF 上 V3/R1 总大小约 **685B 参数** = **主模型 671B + MTP 模块 ~14B**。

### 权重分片

```text
model-00001-of-000XX.safetensors
model-00002-of-000XX.safetensors
...
model.safetensors.index.json
```

| 组件 | 参数量 | 作用 |
|---|---|---|
| **Main Model** | 671B | 61 层 Transformer + Embedding + lm_head；正常推理主体 |
| **MTP Module** | ~14B | Multi-Token Prediction；推测解码加速 |

官方文档：[DeepSeek-V3 README_WEIGHTS.md](https://github.com/deepseek-ai/DeepSeek-V3/blob/main/README_WEIGHTS.md)

### 主模型内部参数（节选）

| 参数路径 | 作用 |
|---|---|
| `model.embed_tokens.weight` | 输入词嵌入 |
| `model.layers.0` ~ `model.layers.60` | 61 层 Transformer（MoE + MLA 等） |
| `model.norm.weight` | 最后一层 LayerNorm |
| `lm_head.weight` | 输出词表投影 |

### MTP 模块（`num_nextn_predict_layers: 1`）

| 部分 | 作用 |
|---|---|
| `model.layers.61.self_attn & mlp` | 额外一层 Transformer，结构同主模型 |
| `enorm` / `hnorm` | MTP 用的 RMSNorm |
| `eh_proj` | 维度投影 |
| `embed_tokens` / `shared_head` | **与主模型共享** Embedding 和 lm_head |

加载规则：`num_hidden_layers=61` 加载主模型；`num_nextn_predict_layers=1` 加载 layer 61 为 MTP。

### `config.json` 关键字段（V3）

| 字段 | 作用 |
|---|---|
| `model_type: deepseek_v3` | 框架识别架构类型 |
| `num_hidden_layers: 61` | 主模型 Transformer 层数 |
| `num_nextn_predict_layers: 1` | MTP 模块数量 |
| MoE 相关字段 | 专家总数、每 token 激活专家数、路由配置 |

### FP8 量化（HF FP8 版）

`config.json` 中：

```json
"quantization_config": {
  "quant_method": "fp8",
  "fmt": "e4m3",
  "weight_block_size": [128, 128],
  "activation_scheme": "dynamic"
}
```

| 内容 | 作用 |
|---|---|
| **`quantization_config`** | 声明 FP8 格式与 block 大小 |
| **`weight_scale_inv`**（在 safetensors 内） | 每 128×128 block 的反量化 scale |

反量化：`(128×128 权重块) * weight_scale_inv`。

---

## R1-Distill 小模型（1.5B～70B）

结构比 671B **简单**，通常：

```text
config.json
tokenizer.*
model.safetensors          ← 单文件或少量分片
generation_config.json
README.md / LICENSE
```

| 特点 | 说明 |
|---|---|
| 基座 | Qwen2.5 或 Llama |
| 改动 | DeepSeek **微调过 config 和 tokenizer**，须用官方仓库配置 |
| 许可 | MIT + 底层 Qwen（Apache 2.0）或 Llama License |

R1 README 明确：**Distill 模型须按 DeepSeek 提供的 setting 运行**，不要直接当普通 Qwen/Llama 默认值用。

---

## V4 系列（Flash / Pro）

| 差异 | 说明 |
|---|---|
| 规模 | Flash 284B / Pro 1.6T |
| 上下文 | 1M token |
| 精度 | MoE 专家 FP4 + 其余 FP8 混合 |
| 文件形态 | 仍是分片 `model-*.safetensors` + index + config/tokenizer |

GitHub 通常附带 **`inference/`**：权重转换、交互式 chat demo。

---

## GitHub 仓库文件（非权重）

以 [DeepSeek-V3](https://github.com/deepseek-ai/DeepSeek-V3) 为例：

| 文件/目录 | 作用 |
|---|---|
| **`README.md`** | 模型介绍、HF 下载链接 |
| **`README_WEIGHTS.md`** | 权重结构、MTP、FP8 详解 |
| **`LICENSE` / `LICENSE-MODEL`** | 代码与模型许可 |
| **`inference/`** | 本地推理、转换脚本 |
| 论文 / 配置示例 | 架构与实验说明 |

---

## 按场景需要哪些文件

| 场景 | 最少需要 |
|---|---|
| **Ollama 跑 7B** | 无需手动管理；Ollama 内部已打包 |
| **vLLM / Transformers 推理** | 完整 config + tokenizer + 全部分片 safetensors + index |
| **LoRA 微调** | 同上（完整基座权重） |
| **只研究架构** | `config.json` + `README_WEIGHTS.md` |

---

## 系列与权重特点对照

| 系列 | HF 示例 | 权重特点 |
|---|---|---|
| V3 / V3-Base | `deepseek-ai/DeepSeek-V3` | 671B + MTP，多分片，FP8 |
| R1 / R1-Zero | `deepseek-ai/DeepSeek-R1` | 同 V3 架构 + 推理对齐 |
| R1-Distill-* | `deepseek-ai/DeepSeek-R1-Distill-Qwen-7B` | 小模型，文件少，接近 Qwen/Llama 布局 |
| V4-Flash / Pro | `deepseek-ai/DeepSeek-V4-Flash` | 更大、1M 上下文、FP4/FP8 混合 |

---

## 与之相关

- 通用权重概念：[[AI/00-AI学习体系/02-概念库/00-基础与LLM概论/08-开源权重与模型文件|开源权重与模型文件]]
- 部署方式：[[AI/00-AI学习体系/02-概念库/08-前沿动态/04-DeepSeek开源部署指南|DeepSeek开源部署指南]]
- MoE 架构：[[AI/00-AI学习体系/02-概念库/01-模型层/02-MoE|MoE]]
- 推理 Serving：[[AI/00-AI学习体系/02-概念库/02-训练与推理/09-推理引擎与Serving|推理引擎与Serving]]
- 学习路径：[[AI/00-AI学习体系/02-概念库/08-前沿动态/05-开放权重本地部署学习路径-2026-07|开放权重本地部署学习路径 2026-07]]

## 官方参考

- 权重结构：https://github.com/deepseek-ai/DeepSeek-V3/blob/main/README_WEIGHTS.md
- R1 说明：https://huggingface.co/deepseek-ai/DeepSeek-R1
- V4 说明：https://huggingface.co/deepseek-ai/DeepSeek-V4-Pro

## 自测

1. `config.json` 和 `model-*.safetensors` 分别解决什么问题？
2. V3 的 685B 总大小由哪两部分组成？
3. 为什么 R1-Distill-7B 不能当成普通 Qwen2.5-7B 默认配置直接跑？
