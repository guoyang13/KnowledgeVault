---
aliases:
  - Tool Use与Function Calling
---

# Tool Use 与 Function Calling

> 让 LLM 按 schema 调用外部工具，是 Agent 系统的最小公共底座，现已是所有主流 LLM 的标配。

## 工作机制
1. 开发者声明工具列表（JSON Schema：name、description、parameters）。
2. 模型决定是否调用、调用哪个、参数是什么。
3. 运行时执行工具，把结果作为新消息回灌。
4. 模型继续推理或再次调用工具。

## 几种形态
- **Function Calling / Tool Use**：经典 OpenAI / Anthropic / Gemini API 形态。
- **Parallel Tool Use**：一次调用多个工具，节省往返延迟。
- **Tool Choice 控制**：`auto / required / none / 指定工具`。
- **Structured Output / JSON Mode**：保证输出严格匹配 schema（也可视为"调用 schema 工具"）。
- **Code as Action（CodeAct）**：把动作改成"写一段代码"，由解释器执行。表达力 > 离散工具。

## 工具设计原则
- **少而精**：工具越多越容易"选错"，10 个以内最佳。
- **清晰描述**：工具描述就是 prompt，要交代用途、边界、副作用。
- **副作用显式**：写操作、删除、付款 → 加审批 / 沙箱 / dry-run。
- **错误信息可读**：让模型能从错误里恢复。

## 协议化趋势
- **[[MCP协议]]**：把"工具/资源/Prompt"标准化，跨厂商复用。
- **A2A**：Agent 之间互相调用。

## 与之相关
- [[ReAct与Agent范式]]
- [[MCP协议]]
- [[Agent Harness]]
- [[Workflow vs Agent]]

## 延伸阅读
- OpenAI / Anthropic / Gemini 的 Tool Use 文档
- *CodeAct: Executable Code Actions Elicit Better LLM Agents*
