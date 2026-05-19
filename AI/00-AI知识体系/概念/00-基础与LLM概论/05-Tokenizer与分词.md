---
aliases:
  - Tokenizer与分词
---

# Tokenizer 与分词

> LLM 读写的不是「汉字/单词」，而是 **token ID 序列**。**Tokenizer** 负责文本 ↔ ID 的双向映射；选错分词器会导致乱码、成本飙升或微调失效。

## 核心流程

```
原始文本 → 规范化（NFKC、空格）→ 切分（BPE/等）→ token 片段 → ID 序列
生成时反向：ID → 片段 → 拼接为字符串（需注意 Unicode 边界）
```

## 为什么不是直接按字或词切

- **按词切**：英文还勉强可行，中文、代码、URL、emoji、多语种会很麻烦；新词、拼写错误、变量名也会爆词表。
- **按字符切**：词表小，但序列变长，模型需要更多步才能表达一个词或概念。
- **子词 token**：在词表大小和序列长度之间折中，是现代 LLM 的主流选择。

例子：

```text
unbelievable → un / believ / able
HTTPRequestHandler → HTTP / Request / Handler
大语言模型 → 大 / 语言 / 模型 或更细片段
```

实际切法取决于具体 tokenizer，不同模型之间不可混用。

## 常见算法

| 算法 | 特点 | 代表 |
|------|------|------|
| **BPE** | 从字符起不断合并高频对；子词粒度 | GPT-2/3/4、Llama 系 |
| **WordPiece** | 类似 BPE，合并策略略不同 | 早期 BERT |
| **Unigram / SentencePiece** | 语言无关、可直接在 raw 文本上训 | T5、Llama、多语种模型 |

中文常被切成**较短子词**；同样一句话，中文 token 数常多于英文，**计费与上下文占用**都按 token 算。

## 特殊 token

| 类型 | 作用 |
|------|------|
| `<|endoftext|>` / EOS | 序列结束 |
| `<|im_start|>` 等 | Chat 模板边界（模型相关） |
| PAD | 批训练对齐（推理少见） |
| 工具 / 推理专用 | 如 thinking、function call 占位符（因模型而异） |

**重要**：微调与推理必须使用与**基座模型一致**的 tokenizer 和 chat template。

## Chat template 是什么

API 里你传的是：

```json
[
  {"role": "system", "content": "你是助手"},
  {"role": "user", "content": "解释 Transformer"}
]
```

模型实际看到的是某种模板化文本，例如：

```text
<system>你是助手</system>
<user>解释 Transformer</user>
<assistant>
```

不同模型的角色标记、结束符、工具调用格式都可能不同。微调数据如果模板错了，模型会学到错误的对话边界。

## 对应用的影响

1. **成本**：API 按 token 计价；长 prompt 要先 `tiktoken` / 官方 counter 估算
2. **上下文**：「能放多少字」≠ 字符数，需换算 token
3. **RAG 分块**：chunk 边界最好按 token 或句子，避免半个 multibyte 字符
4. **安全**：罕见 Unicode、同形字可干扰分词与审核

## 常见坑

- **同样字数，token 数可能差很多**：代码、表格、JSON、中文混英文通常更费 token。
- **截断可能切坏结构**：长 JSON、Markdown 表格、函数定义被截断后，模型更容易输出坏格式。
- **特殊 token 不能随便造**：除非 tokenizer 词表里有，否则只是普通字符串片段。
- **流式输出不一定按字符边界来**：前端展示时要处理半个 Unicode 片段或半个 Markdown 结构。

## 实践命令（备忘）

```bash
# HuggingFace
from transformers import AutoTokenizer
tok = AutoTokenizer.from_pretrained("meta-llama/Llama-3.1-8B")
len(tok.encode("你好，世界"))
```

## 学习检查

1. 为什么 tokenizer 会影响 API 成本？
2. 为什么微调时 tokenizer 和 chat template 必须与基座模型一致？
3. RAG chunk 为什么最好按 token 或语义边界切？

## 与之相关

- [[AI/00-AI知识体系/概念/00-基础与LLM概论/03-表示学习-从词嵌入到上下文|表示学习-从词嵌入到上下文]]
- [[AI/00-AI知识体系/概念/00-基础与LLM概论/04-语言模型与Next-Token-Prediction|语言模型与 Next-Token Prediction]]
- [[AI/00-AI知识体系/概念/01-模型层/07-上下文与KV Cache|上下文与KV Cache]]
- [[AI/00-AI知识体系/概念/06-工程生态/02-Context Engineering|Context Engineering]]

## 延伸阅读

- Sennrich et al., *Neural Machine Translation of Rare Words with Subword Units*（BPE）
- HuggingFace — *Tokenizer summary*
- 各模型卡中的 **Chat template** 说明（apply_chat_template）
