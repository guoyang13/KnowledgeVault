# Context Engineering（上下文工程）

> 2025 年开始替代/扩展 "Prompt Engineering" 的术语：重点不再是写一段聪明 prompt，而是**系统化管理**模型每次看到的全部上下文。

## 为什么不再叫 Prompt Engineering
- 现代 LLM 应用的上下文 = **System + Tools + Memory + RAG + History + Rules**，远不止一段 prompt。
- 决定输出质量的是"喂进去什么"，而不只是"怎么写问句"。
- Agent 场景下，上下文是动态、循环、变化的，需要"工程"而非"工艺"。

## 核心组成
1. **System Prompt**：角色、风格、约束。
2. **Tool Definitions**：可调用工具的 schema 与描述。
3. **Rules / Skills**：规则文件、技能片段、AGENTS.md / .cursor/rules。
4. **Memory**：长期记忆召回（见 [[Agent Memory]]）。
5. **RAG**：当下任务相关的检索片段（见 [[RAG基础]]）。
6. **History**：消息历史、工具调用结果。
7. **当前任务输入**：用户最新消息 + 相关文件。

## 关键问题
- **预算分配**：上下文窗口有限，哪些必须进、哪些可压缩、哪些可省略。
- **顺序**：把"不变的"放前面以命中 [[上下文与KV Cache|Prefix Cache]]。
- **位置效应**：关键信息放头尾，避免 lost-in-the-middle。
- **隔离**：子任务用子上下文，避免主上下文被污染。
- **压缩 / 总结**：长对话自动滚动摘要。
- **可观测**：每一步上下文都应可查看 / diff。

## 常见反模式
- 把整个仓库 / 整个 PDF 直接塞进去。
- 把所有工具一次性 expose（10+ 就开始劣化）。
- 不分系统 / 工具 / 用户消息，全堆成一段 prompt。
- 不清理失败的工具调用结果，让错误持续污染上下文。

## 与之相关
- [[上下文与KV Cache]]
- [[RAG基础]]
- [[Agent Memory]]
- [[Agent Harness]]

## 延伸阅读
- Andrej Karpathy 关于 Context Engineering 的推文/演讲
- LangChain 博客 *The rise of Context Engineering*
- 各 Coding Agent 公开的 system prompt 解析
