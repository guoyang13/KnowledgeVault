---
aliases:
  - MCP协议
---

# MCP（Model Context Protocol）

> Anthropic 发起、目前已成事实标准的 Agent ↔ 工具/资源 通信协议。可把它视为"AI 时代的 USB-C"。

## 解决什么问题
- 每个工具各写一遍接入代码 → 工作量爆炸、生态碎片。
- MCP 把"工具 / 资源 / Prompt 模板"标准化，让任何 MCP-aware 的 Agent 都能即插即用。

## 三类原语
1. **Tools**：可执行的函数（带 JSON Schema），等价于经典 function calling。
2. **Resources**：可读取的内容（文件、数据库表、API 响应），由 URI 标识。
3. **Prompts**：可参数化的 prompt 模板，方便服务端预制最佳实践。

## 架构
- **MCP Server**：暴露 tools / resources / prompts 的进程。
- **MCP Client / Host**：Agent / IDE / 应用，连接多个 server。
- **传输**：stdio（本地子进程）、SSE / HTTP（远程）。
- **能力协商**：连接时握手交换支持的能力。

## 生态
- **官方/常用 Server**：filesystem、git、shell、postgres、puppeteer、Slack、GitHub、各 SaaS。
- **客户端**：Claude Desktop、Cursor、Continue、Cline、OpenAI Agents SDK、众多 IDE。
- **目录**：mcp.so、官方 servers repo、社区 awesome-mcp 列表。

## 与 A2A 的关系
- **MCP**：Agent ↔ 工具/资源（"用东西"）。
- **A2A（Agent-to-Agent，Google 推动）**：Agent ↔ Agent（"和别的 Agent 协作"）。
- 二者互补，不竞争。

## 安全注意
- MCP 工具描述本身可能携带 [[AI/00-AI知识体系/概念/07-安全治理/01-Prompt Injection与Agent安全|prompt injection]]。
- 远程 server 要校验来源、做 sandbox / 审批。

## 与之相关
- [[AI/00-AI知识体系/概念/03-Agent系统/03-Tool Use与Function Calling|Tool Use与Function Calling]]
- [[AI/00-AI知识体系/概念/03-Agent系统/05-Agent Harness|Agent Harness]]
- [[AI/00-AI知识体系/概念/07-安全治理/01-Prompt Injection与Agent安全|Prompt Injection与Agent安全]]
- [[AI/00-AI知识体系/概念/03-Agent系统/07-Computer Use与Browser Agent|Computer Use与Browser Agent]]

## 延伸阅读
- modelcontextprotocol.io 官方文档
- Anthropic MCP 发布博客（2024-11）
- 各客户端 / Server 的实现源码
