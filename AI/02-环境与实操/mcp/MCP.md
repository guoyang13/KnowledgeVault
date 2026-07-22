---
aliases:
  - MCP
  - MCP配置
tags:
  - AI
  - MCP
  - 配置
---

# MCP 配置与笔记

> **概念定义**见 [[AI/00-AI学习体系/02-概念库/03-Agent系统/06-MCP协议|MCP协议]]。本页只放**本地配置与使用备忘**。

↑ [[AI/02-环境与实操/00-环境与实操导航|环境与实操导航]]

---

## 本地配置

- 配置文件：`mcp/mcp.json`（与本笔记同目录）
- 用途：Cursor / Codex 等连接 MCP Server（filesystem、git、browser 等）

---

## 三原语速查

| 原语 | 作用 |
|------|------|
| **Tools** | 可执行函数（JSON Schema） |
| **Resources** | 可读取内容（文件、DB、API） |
| **Prompts** | 可参数化 Prompt 模板 |

---

## 与 Skill 的关系

- **Skill**：任务流程知识（何时做、怎么做）
- **MCP**：Agent 与工具/资源的连接协议

详见 [[AI/00-AI学习体系/02-概念库/03-Agent系统/10-Agent Skills与协议生态|Agent Skills与协议生态]]。

---

## 待补充

- [ ] 当前 `mcp.json` 中各 Server 说明
- [ ] 常用调试命令
