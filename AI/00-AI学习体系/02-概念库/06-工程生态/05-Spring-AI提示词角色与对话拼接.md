---
aliases:
  - Spring AI 提示词
  - Spring AI 多轮对话
  - MessageChatMemoryAdvisor
tags:
  - AI
  - Java
  - Spring
  - Prompt
  - Memory
---

# Spring AI 提示词角色与对话拼接

> **6.5 工程与生态**。Spring AI 如何用 Message 角色表达 system/user/assistant，以及多轮历史、RAG 如何通过 Advisor 自动拼进 Prompt。
>
> 前置：[[AI/00-AI学习体系/02-概念库/06-工程生态/04-Spring-AI入门与API|Spring-AI入门与API]] · [[AI/00-AI学习体系/02-概念库/00-基础与LLM概论/09-LLM调用与Agent多轮对话|LLM调用与Agent多轮对话]]
>
> Tool calling loop 与 `.tools()` 配合见 [[AI/00-AI学习体系/02-概念库/06-工程生态/06-Spring-AI-Advisor与Tools|Spring-AI-Advisor与Tools]]。

↑ [[AI/00-AI学习体系/02-概念库/06-工程生态/00-工程生态导航|工程生态导航]] · [[AI/00-AI学习体系/00-核心索引|核心索引]]

---

## 摘要

Spring AI 对齐 Chat API 的 **Message** 模型；业务侧用 **ChatClient** 设角色，多轮用 **ChatMemory + MessageChatMemoryAdvisor** 自动拼接历史，RAG 用 **QuestionAnswerAdvisor** 增强当前 user 消息。

> **角色用 `.system()` / `.user()` / `AssistantMessage` 表达；多轮用 `MessageChatMemoryAdvisor` + `CONVERSATION_ID`；RAG 叠在 Memory 之后。**

---

## 一、Message 角色一览

| 类型 | 类 | 作用 |
|------|-----|------|
| **System** | `SystemMessage` | 人设、规则、输出格式（优先级高） |
| **User** | `UserMessage` | 用户输入 |
| **Assistant** | `AssistantMessage` | 模型历史回复（含 tool_calls） |
| **Tool** | `ToolResponseMessage` | 工具执行结果（Agent 场景） |

```text
System  →  你怎么 behave
User    →  用户说了什么
Assistant →  你之前答了什么
Tool    →  工具返回了什么
```

最终 Prompt 结构：

```text
SystemMessage
+ (UserMessage ↔ AssistantMessage 交替)
+ 当前 UserMessage
```

---

## 二、ChatClient 里怎么写不同角色

### 2.1 单次请求：system + user

```java
String answer = chatClient.prompt()
    .system("你是 Java 后端专家，只根据上下文回答，不知道就说不知道。")
    .user("解释 G1 的 Mixed GC")
    .call()
    .content();
```

### 2.2 多条 messages / 手动拼历史

```java
chatClient.prompt()
    .system("你是客服助手")
    .messages(
        new UserMessage("Redis 持久化有哪几种？"),
        new AssistantMessage("RDB 和 AOF…"),
        new UserMessage("那 RDB 缺点呢？")   // 当前问题
    )
    .call()
    .content();
```

### 2.3 Builder 默认 system（全局人设）

```java
ChatClient client = ChatClient.builder(chatModel)
    .defaultSystem("你是公司内部知识库助手，回答简洁。")
    .build();

client.prompt().user(question).call().content();
```

### 2.4 模板 + 参数

```java
client.prompt()
    .system(s -> s.text("角色：{role}，语言：{lang}")
        .param("role", "架构师")
        .param("lang", "中文"))
    .user(u -> u.text("问题：{q}").param("q", question))
    .call()
    .content();
```

外部模板文件可用 `PromptTemplate` 或 `@Value` 加载 `.st` 文件。

### 2.5 Few-shot：手写 Assistant

正常多轮 **assistant 由 ChatMemory 注入**；只有 few-shot 样例才手动加 `AssistantMessage`。

---

## 三、多轮对话：ChatMemory + Advisor（推荐）

**不要**每个接口自己 `List<Message>` 维护历史。

```java
ChatClient client = ChatClient.builder(chatModel)
    .defaultSystem("你是助手")
    .defaultAdvisors(
        MessageChatMemoryAdvisor.builder(chatMemory).build()
    )
    .build();

// 同一 conversationId = 同一段对话
String reply = client.prompt()
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
    .user("Redis RDB 是什么？")
    .call()
    .content();

// 下一轮：只传新 user，Advisor 自动拼历史
String reply2 = client.prompt()
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
    .user("它和 AOF 比呢？")
    .call()
    .content();
```

### 拼接流程

```text
第 1 轮：System + User₁ → Model → Assistant₁ → 写入 ChatMemory
第 2 轮：System + User₁ + Assistant₁ + User₂ → Model → Assistant₂ → 写入 …
```

`MessageChatMemoryAdvisor`：**call 前**从 `ChatMemory` 取历史，**call 后**存本轮 user/assistant。

### ChatMemory 实现

| 实现 | 说明 |
|------|------|
| `MessageWindowChatMemory` | 内存，保留最近 N 条 |
| JDBC / Redis Repository | 持久化、分布式会话 |

```java
ChatMemory chatMemory = MessageWindowChatMemory.builder()
    .maxMessages(20)   // 窗口大小，防 token 爆
    .build();
```

### Controller 最小示例

```java
@PostMapping("/chat")
public String chat(@RequestParam String sessionId, @RequestBody String msg) {
    return chatClient.prompt()
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
        .user(msg)
        .call()
        .content();
}
```

---

## 四、RAG + 多轮：Advisor 链顺序

> RAG Advisor 原理与 `before()` 逻辑 → [[AI/00-AI学习体系/02-概念库/06-工程生态/08-Spring-AI-RAG处理与基础设施#四、Advisor 编排 — 可选的流程封装|08 §四]]。

```java
.defaultAdvisors(
    MessageChatMemoryAdvisor.builder(chatMemory).build(),      // 先：加历史
    QuestionAnswerAdvisor.builder(vectorStore).build()         // 后：RAG 检索 augment
)
```

Advisor 链：**Memory → RAG → 调模型**；检索时能利用完整对话上下文。

带会话 ID 的完整写法：

```java
client.prompt()
    .advisors(a -> a
        .param(ChatMemory.CONVERSATION_ID, sessionId))
    .user(question)
    .call()
    .content();
```

**典型拼接结果**：

```text
SystemMessage          ← defaultSystem
UserMessage            ← 历史
AssistantMessage
...
UserMessage            ← 当前 user（可能被 RAG 改写/增强，附检索 Document）
```

`QuestionAnswerAdvisor` 把检索文档 augment 进 **当前 user 消息**（或按模板附加），不是单独 system 角色。

---

## 五、底层 ChatModel 手动拼接

适合框架封装或需要完全控制 Prompt 时：

```java
List<Message> messages = new ArrayList<>();
messages.add(new SystemMessage("你是助手"));
messages.add(new UserMessage("上一轮问题"));
messages.add(new AssistantMessage("上一轮回答"));
messages.add(new UserMessage("当前问题"));

ChatResponse response = chatModel.call(new Prompt(messages));
```

业务代码更推荐 **ChatClient + ChatMemory**。

---

## 六、Tool 对话里的角色链

Agent 多轮时消息链（`ToolCallingAdvisor` + `@Tool` 自动维护，**不要手写 Tool 消息**）：

```text
System
User
Assistant（含 tool_calls）
Tool（工具返回）
Assistant（最终自然语言回答）
```

---

## 七、System / User / 上下文从哪来

| 内容 | 放哪 | 示例 |
|------|------|------|
| 固定人设 | `defaultSystem` | 「你是 Java 助手」 |
| 每轮规则 | `.system(...)` | 「用 markdown 表格」 |
| 用户问题 | `.user(...)` | 表单/聊天输入 |
| 检索上下文 | `QuestionAnswerAdvisor` | 自动拼进 prompt |
| 历史对话 | `MessageChatMemoryAdvisor` | 自动拼 User/Assistant |
| Few-shot 样例 | `.messages(...)` | 手写 User+Assistant 对 |

与 [[AI/00-AI学习体系/02-概念库/06-工程生态/02-Context Engineering|Context Engineering]] 的分工：

```text
Prompt Engineering  → 任务规格、角色、输出格式
Context Engineering → 历史、RAG、工具结果如何进入本轮
Spring AI Advisor   → 在 call 前后自动组装 Context
```

---

## 八、PromptTemplate 独立使用

```java
PromptTemplate template = new PromptTemplate("""
    你是{role}。用户问题：{query}
    """);
Prompt prompt = template.create(Map.of(
    "role", "DBA",
    "query", "慢 SQL 怎么查"
));
chatModel.call(prompt);
```

`TemplateRenderer` 负责替换 `{key}`；ChatClient 的 `.user()` / `.system()` 内置同等能力。

---

## 九、注意点

```text
1. System 通常每轮都有；defaultSystem 会自动带上
2. 历史过长 → maxMessages 或摘要 Advisor，否则超 context
3. AssistantMessage 必须是模型真实回复，不要乱编历史
4. 多用户隔离 → CONVERSATION_ID 用 userId / sessionId
5. RAG context 在 Advisor 里拼进 user/system，不必手搓大字符串
6. defaultSystem 与 .system() 会叠加，注意别重复矛盾
```

---

## 十、与之相关

- [[AI/00-AI学习体系/02-概念库/06-工程生态/04-Spring-AI入门与API|Spring-AI入门与API]]
- [[AI/00-AI学习体系/02-概念库/06-工程生态/08-Spring-AI-RAG处理与基础设施|Spring-AI-RAG处理与基础设施]] — RAG Advisor 与 Memory 组合
- [[AI/00-AI学习体系/02-概念库/00-基础与LLM概论/10-Prompt工程入门与实践|Prompt工程入门与实践]]
- [[AI/00-AI学习体系/02-概念库/03-Agent系统/08-Agent Memory|Agent Memory]]
- [[AI/00-AI学习体系/02-概念库/04-RAG进阶/01-RAG基础|RAG基础]]
