---
aliases:
  - Spring AI MCP
  - McpTool
  - Spring AI MCP Client
tags:
  - AI
  - Java
  - Spring
  - MCP
  - Agent
---

# Spring AI 与 MCP

> **6.10 工程与生态 · 本文定位**：Spring AI 如何接入 **Model Context Protocol**——Starter、注解、Client/Server 角色、与 ChatClient 集成。
>
> MCP 协议概念见 [[AI/00-AI学习体系/02-概念库/03-Agent系统/06-MCP协议|MCP协议]]；Spring AI 入门见 [[AI/00-AI学习体系/02-概念库/06-工程生态/04-Spring-AI入门与API|Spring-AI入门与API]]；本地 `@Tool` 见 [[AI/00-AI学习体系/02-概念库/06-工程生态/06-Spring-AI-Advisor与Tools|Spring-AI-Advisor与Tools]]。
>
> 更新时间：2026-07-21 · Spring AI 2.0（MCP Java SDK 2.0，对齐 2025-11-25 MCP 规范）

↑ [[AI/00-AI学习体系/02-概念库/06-工程生态/00-工程生态导航|工程生态导航]] · [[AI/00-AI学习体系/00-核心索引|核心索引]]

官方：[MCP Annotations](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-overview.html) · [MCP Client Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html) · [MCP Server Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)

---

## 摘要

Spring AI **2.0** 将 MCP Java SDK、注解模块、Transport 实现收进主项目，提供 **Boot Starters + 声明式注解**，让 Java 应用既能 **做 MCP Server**（对外暴露 Tool/Resource/Prompt），也能 **做 MCP Client**（连外部 MCP Server，Tool 注入 ChatClient）。

```text
MCP Server 侧：@McpTool / @McpResource / @McpPrompt + starter-mcp-server-*
MCP Client 侧：连接 mcp-servers.json / STDIO → ToolCallbackProvider → ChatClient
```

> **@Tool = 应用内 Tool Calling；@McpTool = 按 MCP 标准对外暴露，供 Cursor / Claude Desktop / 任意 MCP Client 连接。**

---

## 一、架构分层

```text
Spring Boot 应用
    ├── MCP Server：把 Spring 服务能力暴露给外部 AI
    └── MCP Client：连接外部 MCP Server，远程 Tool 给 ChatClient 用

spring-ai-mcp-annotations     注解编程模型
spring-ai-starter-mcp-*       Boot 自动配置
MCP Java SDK（Spring 维护）   协议与 Transport 底层
```

---

## 二、Maven Starter

### MCP Server（对外暴露能力）

| Starter | Transport | 配置 |
|---------|-----------|------|
| `spring-ai-starter-mcp-server` | **STDIO** | `spring.ai.mcp.server.stdio=true` |
| `spring-ai-starter-mcp-server-webmvc` | HTTP | `protocol=STREAMABLE` / `STATELESS` / `SSE`（SSE 2.0 deprecated） |
| `spring-ai-starter-mcp-server-webflux` | 响应式 HTTP | 同上 |

### MCP Client（连接外部 MCP Server）

| Starter | 说明 |
|---------|------|
| `spring-ai-starter-mcp-client` | 标准客户端（JDK HttpClient；STDIO / SSE / Streamable） |
| `spring-ai-starter-mcp-client-webflux` | WebFlux 客户端（**生产推荐** SSE/Streamable 用这套） |

### 注解模块

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-annotations</artifactId>
</dependency>
```

使用任一 MCP Starter 时**自动包含**，通常无需单独引入。

---

## 三、Server 端注解（MCP 三原语）

| 注解 | 作用 | 对应 MCP |
|------|------|----------|
| `@McpTool` | 暴露可调用工具，自动生成 JSON Schema | Tools |
| `@McpToolParam` | 工具参数描述 | Tool 参数 |
| `@McpResource` | URI 模板访问资源 | Resources |
| `@McpPrompt` | 提供 Prompt 模板 | Prompts |
| `@McpComplete` | 参数/URI 自动补全 | Completions |
| `@McpArg` | Prompt 参数 | — |

### 最小 Server 示例

```java
@Component
public class WeatherService {

    @McpTool(description = "查询某地气温（摄氏度）")
    public WeatherResponse getTemperature(
            @McpToolParam(description = "纬度") double latitude,
            @McpToolParam(description = "经度") double longitude) {
        return RestClient.create()
            .get()
            .uri("https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m",
                latitude, longitude)
            .retrieve()
            .body(WeatherResponse.class);
    }

    @McpResource(uri = "config://{key}", name = "Configuration")
    public String getConfig(String key) {
        return configData.get(key);
    }
}
```

```yaml
# application.yml — WebMVC Streamable HTTP Server
spring:
  ai:
    mcp:
      server:
        protocol: STREAMABLE
        annotation-scanner:
          enabled: true
```

启动后 Cursor / MCP Inspector 等 Client 可连 `http://localhost:8080`。

---

## 四、Client 端注解（双向通信回调）

Client 除调用远程 Tool，还需处理 Server **反向请求**：

| 注解 | 作用 |
|------|------|
| `@McpLogging` | 处理 Server 日志通知 |
| `@McpSampling` | 处理 Sampling（Server 请求 Client 侧 LLM） |
| `@McpElicitation` | 处理 Elicitation（向用户追问） |
| `@McpProgress` | 处理进度通知 |
| `@McpToolListChanged` | Tool 列表变更 |
| `@McpResourceListChanged` | Resource 列表变更 |
| `@McpPromptListChanged` | Prompt 列表变更 |

```java
@Component
public class LoggingHandler {

    @McpLogging(clients = "my-server")
    public void handleLoggingMessage(LoggingMessageNotification notification) {
        log.info("{} - {}", notification.level(), notification.data());
    }
}
```

---

## 五、上下文注入参数

自动注入，**不出现在 Tool JSON Schema** 中：

| 类型 | 用途 |
|------|------|
| `McpSyncRequestContext` | 同步：日志、进度、采样、elicitation、roots |
| `McpAsyncRequestContext` | 异步（Reactor `Mono`）版 |
| `McpTransportContext` | 无状态 Server 轻量传输上下文 |
| `McpMeta` / `MetaProvider` | 请求 `_meta` 元数据 |
| `@McpProgressToken` | 长任务进度 token |

```java
@McpTool(name = "long-task", description = "带进度报告的长任务")
public String longTask(
        McpSyncRequestContext ctx,
        @McpToolParam(description = "任务名") String taskName) {
    ctx.progress(50, "处理中…");
    // ...
    return "done";
}
```

---

## 六、SDK 核心类（编程式）

| 类 | 作用 |
|----|------|
| `McpSyncClient` / `McpAsyncClient` | 同步/异步 MCP 客户端 |
| `McpSyncServer` / `McpAsyncServer` | 同步/异步 MCP 服务端 |
| `McpClientCustomizer` | 自定义 Client 创建 |
| `StdioClientTransport` | STDIO 传输 |
| `WebMvcStreamableServerTransportProvider` | WebMVC Streamable HTTP |
| `WebFluxStreamableServerTransportProvider` | WebFlux Streamable HTTP |

注解 + Starter 覆盖大多数场景；需精细控制 Transport 时用编程式 API。

---

## 七、与 ChatClient 集成（Client 侧关键）

MCP Client Starter 将已连接 MCP Server 的 Tool **转为 Spring AI ToolCallback**：

```java
@Autowired ToolCallbackProvider mcpToolProvider;

@Bean
CommandLineRunner demo(ChatClient.Builder builder, ToolCallbackProvider mcpToolProvider) {
    ChatClient client = builder
        .defaultToolCallbacks(mcpToolProvider)
        .build();
    return args -> System.out.println(
        client.prompt().user("桌面有哪些文件？").call().content());
}
```

| 组件 | 作用 |
|------|------|
| `ToolCallbackProvider` | 所有 MCP Tool 统一入口（自动配置） |
| `SyncMcpToolCallbackProvider` | 同步 MCP Tool 回调 |
| `spring.ai.mcp.client.toolcallback.enabled` | 是否注册 MCP Tool 到 Tool 框架（默认 `true`） |

配合 [[AI/00-AI学习体系/02-概念库/06-工程生态/06-Spring-AI-Advisor与Tools|Spring-AI-Advisor与Tools]] 中的 `ToolCallingAdvisor`，形成完整 Agent loop。

---

## 八、Transport 与配置

### Server 协议（`spring.ai.mcp.server.protocol`）

| 值 | 说明 |
|----|------|
| `STDIO` | `stdio=true`，本地进程通信 |
| `STREAMABLE` | Streamable HTTP（**2.0 默认推荐**） |
| `STATELESS` | 无状态 HTTP，易扩缩容；**不支持** sampling/elicitation |
| `SSE` | 旧版，2.0 deprecated |

### Client 连接 STDIO（Claude Desktop 格式）

```yaml
spring:
  ai:
    mcp:
      client:
        type: SYNC
        stdio:
          connections:
            filesystem:
              command: npx
              args:
                - -y
                - "@modelcontextprotocol/server-filesystem"
                - /Users/me/Desktop
```

或外部 JSON：

```yaml
spring:
  ai:
    mcp:
      client:
        stdio:
          servers-configuration: classpath:mcp-servers.json
```

`mcp-servers.json` 格式与 Claude Desktop / Cursor 的 `mcp.json` 相同。

### Windows STDIO 注意

`npx`、`npm` 在 Windows 是 `.cmd`，需 `cmd.exe /c` 包装：

```json
{
  "command": "cmd.exe",
  "args": ["/c", "npx", "-y", "@modelcontextprotocol/server-filesystem", "C:\\Users\\me\\Desktop"]
}
```

### 常用配置前缀

```text
spring.ai.mcp.server.*                    Server
spring.ai.mcp.client.*                    Client
spring.ai.mcp.server.type                 SYNC | ASYNC（不可混用）
spring.ai.mcp.client.type                 SYNC | ASYNC
spring.ai.mcp.server.annotation-scanner.enabled
spring.ai.mcp.client.annotation-scanner.enabled
spring.ai.mcp.client.request-timeout      默认 20s
```

---

## 九、Sync / Async Server

| 配置 | 注册的方法 | 底层 |
|------|------------|------|
| `type: SYNC` | 仅同步 `@McpTool` 等 | `McpSyncServer` |
| `type: ASYNC` | 仅返回 `Mono`/`Flux` 的异步方法 | `McpAsyncServer` |

混合同步/异步方法会被**过滤并打 warn 日志**。

---

## 十、@Tool vs @McpTool

| | `@Tool` | `@McpTool` |
|--|---------|------------|
| 协议 | Spring AI Tool Calling | Model Context Protocol |
| 消费者 | 本应用 `ChatClient` | 任意 MCP Client（Cursor、Claude Desktop…） |
| 场景 | 纯 Spring Boot 内部 Agent | 跨 IDE / 跨应用标准互联 |
| 文档 | [[AI/00-AI学习体系/02-概念库/06-工程生态/06-Spring-AI-Advisor与Tools\|06 · Advisor与Tools]] | 本文 |

Spring AI 2.0 起 **MCP 注解是 MCP 场景推荐路径**；imperative `ToolCallback` 仍可用。

---

## 十一、两种典型用法

### 用法 A：做 MCP Server

```text
spring-ai-starter-mcp-server-webmvc
  + @McpTool / @McpResource / @McpPrompt
  + protocol=STREAMABLE
  → 外部 MCP Client 连接 http://localhost:8080
```

### 用法 B：做 MCP Client + Agent

```text
spring-ai-starter-mcp-client-webflux
  + mcp-servers.json / stdio connections
  + ChatClient + ToolCallbackProvider
  → Agent 可调 filesystem、git、postgres 等 MCP Server
```

```text
         ┌─────────────────┐
Cursor ──│ 你的 MCP Server │── @McpTool 暴露 Java 服务
         └─────────────────┘

         ┌─────────────────┐
你的 App ─│ MCP Client      │── ChatClient + 远程 MCP Tools
         └─────────────────┘
              ↓ 连接
         filesystem / git / …
```

---

## 十二、企业能力（2.0）

- **Micrometer / OpenTelemetry**：MCP 交互可观测
- **spring-ai-community/mcp-security**：OAuth 2.0、API Key
- Server 能力可按需开关：Tools、Resources、Prompts、Logging、Progress、Ping

---

## 十三、注意点

```text
1. SYNC / ASYNC Client 与 Server 均不可混用两种方法类型
2. STATELESS 协议牺牲双向能力（sampling、elicitation）换扩缩容
3. 生产 HTTP 传输优先 WebFlux Starter + STREAMABLE
4. MCP Tool 描述可能携带 prompt injection，远程 Server 需校验来源
5. Client 连多个 Server 时，Tool 名可能冲突——可用 toolcallback 前缀配置
```

---

## 十四、与之相关

- [[AI/00-AI学习体系/02-概念库/03-Agent系统/06-MCP协议|MCP协议]] — Tools / Resources / Prompts 三原语
- [[AI/00-AI学习体系/02-概念库/06-工程生态/04-Spring-AI入门与API|Spring-AI入门与API]]
- [[AI/00-AI学习体系/02-概念库/06-工程生态/06-Spring-AI-Advisor与Tools|Spring-AI-Advisor与Tools]]
- [[AI/00-AI学习体系/02-概念库/03-Agent系统/10-Agent Skills与协议生态|Agent Skills与协议生态]]
- [[AI/02-环境与实操/mcp/MCP|MCP 本地配置备忘]]

## 延伸阅读

- [Spring AI MCP Intro Blog](https://spring.io/blog/2025/09/16/spring-ai-mcp-intro-blog)
- [Spring AI 2.0 GA — MCP Integration](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now)
- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
