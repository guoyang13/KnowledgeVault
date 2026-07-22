---
aliases:
  - Spring AI
  - SpringAI
tags:
  - AI
  - Java
  - Spring
  - 工程
---

# Spring AI 入门与 API

> **6.4 工程与生态**。Spring 官方 Java AI 应用框架：统一 Chat / Embedding / VectorStore / RAG / Tool Calling 抽象，与 Spring Boot 自动配置集成。
>
> **本文定位**：Spring AI **入门与最小可跑**；深度专题见 [[AI/00-AI学习体系/02-概念库/06-工程生态/00-工程生态导航#文档地图|文档地图]]。
>
> 下一步：[[AI/00-AI学习体系/02-概念库/06-工程生态/05-Spring-AI提示词角色与对话拼接|05 · 角色与拼接]] → [[AI/00-AI学习体系/02-概念库/06-工程生态/06-Spring-AI-Advisor与Tools|06 · Advisor与Tools]] → [[AI/00-AI学习体系/02-概念库/06-工程生态/08-Spring-AI-RAG处理与基础设施|08 · RAG基础设施]]
>
> 更新时间：2026-07-21 · Spring AI 2.0（Spring Boot 4.x、Java 21+）

↑ [[AI/00-AI学习体系/02-概念库/06-工程生态/00-工程生态导航|工程生态导航]] · [[AI/00-AI学习体系/00-核心索引|核心索引]]

官方：[Spring AI](https://spring.io/projects/spring-ai) · [Reference](https://docs.spring.io/spring-ai/reference/)

---

## 摘要

**Spring AI** 把大模型、Embedding、向量库、RAG、Tool/Function Calling 做成 **Spring 风格可注入组件**，解决「换厂商改一堆代码、RAG 自己拼、与 Spring 生态不统一」等问题。

```text
Spring Boot  → Web、数据、安全、观测
Spring AI    → LLM、向量检索、Prompt、Advisor、Tool
```

---

## 一、解决什么问题

| 痛点 | Spring AI 做法 |
|------|----------------|
| 各家 Chat API 不一致 | `ChatModel` / `ChatClient` 统一接口 |
| 换 OpenAI → Ollama | 换 dependency + `application.yml`，业务代码少改 |
| RAG 链路自己拼 | `DocumentReader` + `EmbeddingModel` + `VectorStore` + `QuestionAnswerAdvisor` |
| 多轮对话维护历史 | `ChatMemory` + `MessageChatMemoryAdvisor` |
| 工具调用 | `@Tool` + `ToolCallingAdvisor` |
| 可观测 | Micrometer、Tracing 集成 |

---

## 二、架构分层

```text
Controller / Service
       ↓
ChatClient（推荐：Fluent API）
       ↓
Advisors（RAG、Memory、Tool、Moderation…）
       ↓
ChatModel（真正调 LLM）
       ↓
OpenAI / Ollama / Azure / DeepSeek …

并行能力：
  EmbeddingModel  →  文本转向量
  VectorStore     →  存/搜向量（RAG）
  DocumentReader  →  文档入库
```

| 接口 | 作用 | 类比 |
|------|------|------|
| `ChatModel` | 发 `Prompt`，收 `ChatResponse` | `JdbcTemplate` |
| **`ChatClient`** | 链式组装 prompt + advisor + call | `RestClient` / `WebClient` |
| `EmbeddingModel` | `embed()` / `embedForResponse()` | 向量化客户端 |
| `VectorStore` | `add` / `similaritySearch` | 向量 DAO |
| `Advisor` | 请求前后增强 Prompt | Filter / Interceptor |

---

## 三、ChatModel vs ChatClient

**`ChatModel` 是底层调模型接口；`ChatClient` 是上层写业务的流式 API**，内部仍依赖 `ChatModel`。

```text
业务代码 → ChatClient.prompt().user().call().content()
              ↓
         ChatModel.call(Prompt)
              ↓
         OpenAI / Ollama / DeepSeek …
```

| | **ChatModel** | **ChatClient** |
|--|---------------|----------------|
| 层级 | 模型层抽象 | 应用层客户端 |
| 输入 | 自己拼 `Prompt`（`List<Message>` + `ChatOptions`） | 链式 `.user()` / `.system()` |
| 输出 | `ChatResponse` / `Flux<ChatResponse>` | `.content()` / `.entity()` / `.stream()` |
| 能力 | 只负责和 LLM 通信 | 还封装 Advisor、Tool、模板、结构化输出 |

### ChatModel 示例（底层、显式）

```java
@Autowired ChatModel chatModel;

ChatResponse response = chatModel.call(new Prompt(
    List.of(
        new SystemMessage("你是助手"),
        new UserMessage("1+1=?")
    ),
    ChatOptions.builder().temperature(0.3).build()
));
String text = response.getResult().getOutput().getText();
```

### ChatClient 示例（日常推荐）

```java
@Autowired ChatClient chatClient;

String answer = chatClient.prompt()
    .system("你是 Java 后端专家")
    .user("解释 G1 Mixed GC")
    .call()
    .content();
```

### 选型口诀

```text
写 Controller / Service 业务  → ChatClient
需要完全控制 Prompt 结构     → ChatModel
框架封装 Advisor / RAG       → ChatClient + defaultAdvisors
```

---

## 四、依赖与配置（最小可跑）

### Maven（OpenAI 示例）

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

### Ollama 本地

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
</dependency>
```

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: qwen2.5:7b
```

### ChatClient Bean

```java
@Bean
ChatClient chatClient(ChatModel chatModel) {
    return ChatClient.builder(chatModel)
        .defaultSystem("你是助手，回答简洁。")
        .build();
}
```

---

## 五、ChatClient 常用 API

```java
// 同步
String content = chatClient.prompt()
    .user("问题")
    .call()
    .content();

// 流式
Flux<String> stream = chatClient.prompt()
    .user("写一首诗")
    .stream()
    .content();

// 结构化输出（Java record / POJO）
record Answer(String summary, List<String> points) {}
Answer a = chatClient.prompt()
    .user("总结 Redis 持久化")
    .call()
    .entity(Answer.class);

// 模板 + 参数
chatClient.prompt()
    .system(s -> s.text("角色：{role}").param("role", "架构师"))
    .user(u -> u.text("问题：{q}").param("q", question))
    .call()
    .content();
```

---

## 六、Embedding / VectorStore / RAG（概览）

Spring AI RAG 拆成三层，**详细展开见 [[AI/00-AI学习体系/02-概念库/06-工程生态/08-Spring-AI-RAG处理与基础设施|08 · RAG处理与基础设施]]**：

| 组件 | 一句话 |
|------|--------|
| `EmbeddingModel` | 文本 ↔ 向量 |
| `VectorStore` | 存向量、`similaritySearch` 检索 |
| RAG Advisor | 可选；检索 + 拼 Prompt + 调 LLM |

**30 秒最小示例**（Naive RAG）：

```java
@Bean
ChatClient ragClient(ChatModel model, VectorStore vectorStore) {
    return ChatClient.builder(model)
        .defaultSystem("仅根据上下文回答；不知道就说不知道。")
        .defaultAdvisors(
            QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().topK(5).build())
                .build())
        .build();
}

String answer = ragClient.prompt().user("RDB 和 AOF 区别？").call().content();
```

离线索引：`DocumentReader` → `List<Document>` → `vectorStore.add()`（内部自动 embed）。

> 项目实战（手写 pipeline、pgvector、Query Rewrite）见 [[AI/00-AI学习体系/02-概念库/06-工程生态/09-RAG实战-interview-guide全链路|09 · RAG实战全链路]]。

---

## 七、Tool Calling（@Tool）

```java
@Component
public class WeatherTools {
    @Tool(description = "查询城市当前天气")
    public String weather(@ToolParam(description = "城市名") String city) {
        return city + " 晴 25°C";
    }
}

@Bean
ChatClient agentClient(ChatModel model, ToolCallingManager manager, WeatherTools tools) {
    return ChatClient.builder(model)
        .defaultTools(tools)
        .defaultAdvisors(
            ToolCallingAdvisor.builder().toolCallingManager(manager).build()
        )
        .build();
}
```

Agent 多轮消息链（`ToolCallingAdvisor` 自动维护）：

```text
System → User → Assistant（含 tool_calls）→ Tool（工具返回）→ Assistant（最终回答）
```

> **`.tools()` 只声明能力；`ToolCallingAdvisor` 才驱动 loop。** 详见 [[AI/00-AI学习体系/02-概念库/06-工程生态/06-Spring-AI-Advisor与Tools|Spring-AI-Advisor与Tools]]。

---

## 八、与 OpenAI API 的对应

| OpenAI `messages[]` | Spring AI |
|---------------------|-----------|
| `role: system` | `SystemMessage` / `.system()` |
| `role: user` | `UserMessage` / `.user()` |
| `role: assistant` | `AssistantMessage` / ChatMemory 注入 |
| `role: tool` | `ToolResponseMessage` |

协议细节见 [[AI/00-AI学习体系/02-概念库/00-基础与LLM概论/09-LLM调用与Agent多轮对话|LLM调用与Agent多轮对话]]。

---

## 九、选型与对比

| 场景 | 推荐 |
|------|------|
| Java / Spring Boot 团队 | Spring AI |
| Python 快速原型 | LangChain / LlamaIndex |
| 复杂有状态 Agent 图 | LangGraph |
| 已有 Spring 微服务加 AI | Spring AI + 现有 Observability |

Spring AI 优势：**与 Spring Boot 配置、Security、Data、Micrometer 一体**；劣势：生态广度不如 Python 框架。

---

## 十、与之相关

- [[AI/00-AI学习体系/02-概念库/06-工程生态/05-Spring-AI提示词角色与对话拼接|Spring-AI提示词角色与对话拼接]] — Message 角色、ChatMemory、Advisor 顺序
- [[AI/00-AI学习体系/02-概念库/06-工程生态/06-Spring-AI-Advisor与Tools|Spring-AI-Advisor与Tools]] — Tools vs Advisor、ToolCallingAdvisor、栈模型
- [[AI/00-AI学习体系/02-概念库/06-工程生态/08-Spring-AI-RAG处理与基础设施|Spring-AI-RAG处理与基础设施]] — EmbeddingModel、VectorStore、RAG Advisor（**RAG 权威篇**）
- [[AI/00-AI学习体系/02-概念库/06-工程生态/09-RAG实战-interview-guide全链路|RAG实战-interview-guide全链路]] — 项目全链路
- [[AI/00-AI学习体系/02-概念库/06-工程生态/10-Spring-AI与MCP|Spring-AI与MCP]] — MCP Server/Client、@McpTool
- [[AI/00-AI学习体系/02-概念库/04-RAG进阶/01-RAG基础|RAG基础]] — Indexing / Retrieval / Generation 三阶段
- [[AI/00-AI学习体系/02-概念库/04-RAG进阶/03-Embedding模型与向量检索|Embedding模型与向量检索]]
- [[AI/00-AI学习体系/02-概念库/03-Agent系统/03-Tool Use与Function Calling|Tool Use与Function Calling]]
- [[AI/00-AI学习体系/02-概念库/03-Agent系统/08-Agent Memory|Agent Memory]]
- [[AI/00-AI学习体系/02-概念库/06-工程生态/02-Context Engineering|Context Engineering]]
- [[AI/00-AI学习体系/02-概念库/06-工程生态/03-应用层框架|应用层框架]]

## 延伸阅读

- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/)
- [Spring AI GitHub](https://github.com/spring-projects/spring-ai)
