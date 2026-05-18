---
aliases:
  - Agent Memory
---

# Agent Memory（Agent 记忆系统）

> 让 Agent 跨会话 / 跨任务保持上下文：用户偏好、过往结论、长期目标、错误教训。

## 记忆分层
- **Working Memory**：当前对话的上下文窗口，受 [[AI/00-AI知识体系/概念/01-模型层/07-上下文与KV Cache|context window]] 限制。
- **Episodic Memory（情景）**：过去发生过的具体事件（"上次我们改过这个文件"）。
- **Semantic Memory（语义/知识）**：抽象事实和偏好（"用户喜欢 TypeScript"）。
- **Procedural Memory（程序性）**：技能与流程（"修这种 bug 通常用这个套路"）。

## 实现方式
- **向量检索**：把对话/事件嵌入向量库，按相似度召回。
- **结构化存储**：键值对 / 关系表，存偏好、事实、任务状态。
- **图记忆**：实体 + 关系，便于推理（Zep、Mem0 等）。
- **Memory Consolidation**：定期"睡眠整理"——压缩冗余、提炼总结、过期遗忘。

## 框架与服务
- **Mem0**：开源记忆层，集成各主流框架。
- **Letta（前 MemGPT）**：长记忆 + 反思机制，模拟"操作系统"管理记忆。
- **Zep**：图 + 向量混合，强调时序与实体关系。
- **OpenAI ChatGPT Memory / Claude Memory**：产品级长记忆。

## 工程要点
- **写入策略**：不是所有消息都该记，否则噪音很大；常用 LLM 判断"是否值得记"。
- **更新与遗忘**：新事实要能覆盖旧事实；过期信息要被淘汰。
- **隐私 & 可控**：用户必须能查看 / 编辑 / 删除记忆。
- **避免污染**：恶意输入可能注入"虚假记忆"，参见 [[AI/00-AI知识体系/概念/07-安全治理/01-Prompt Injection与Agent安全|Prompt Injection与Agent安全]]。

## 与之相关
- [[AI/00-AI知识体系/概念/04-RAG进阶/01-RAG基础|RAG基础]]
- [[AI/00-AI知识体系/概念/06-工程生态/02-Context Engineering|Context Engineering]]
- [[AI/00-AI知识体系/概念/03-Agent系统/05-Agent Harness|Agent Harness]]
- [[AI/00-AI知识体系/概念/01-模型层/07-上下文与KV Cache|上下文与KV Cache]]

## 延伸阅读
- MemGPT / Letta 论文（Berkeley, 2023）
- Mem0、Zep 官方文档
- *Generative Agents*（带反思 + 记忆的 NPC 实验）
