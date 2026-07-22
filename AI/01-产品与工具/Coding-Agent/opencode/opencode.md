---
aliases:
  - opencode
tags:
  - AI
  - Coding Agent
---

# opencode

> 开源 AI Coding Agent CLI / TUI。官网 [opencode.ai](https://opencode.ai)，仓库 [github.com/anomalyco/opencode](https://github.com/anomalyco/opencode)。概念见 [[AI/00-AI学习体系/02-概念库/06-工程生态/01-Coding Agent|Coding Agent]]。

↑ [[AI/01-产品与工具/00-产品与工具导航|产品与工具导航]]

---

## 形态

- 终端 TUI + CLI，可读写仓库、执行 shell、对接 MCP
- Monorepo（Bun），默认分支 `dev`
- **对比**：[[AI/01-产品与工具/Coding-Agent/Cursor/Cursor|Cursor]]、[[AI/01-产品与工具/Coding-Agent/Codex/Codex|Codex]]

---

## 安装与开发

```bash
# 安装 CLI（npm）
npm i -g opencode-ai

# 本地开发（packages/opencode）
bun install
bun dev          # 启动 TUI
bun typecheck    # 在包目录内做类型检查
```

本地 Bun 路径（本机）：`/Users/guoyang/.bun/bin/bun`

---

## 包结构（Monorepo）

| 包 | 职责 |
|----|------|
| `packages/opencode` | TUI、CLI、V1 instance server |
| `packages/core` | V2 Session 核心（Runner、Execution、持久化） |
| `packages/server` | V2 HTTP API |
| `packages/llm` | Schema-first LLM 核心（Route、Protocol、HTTP） |
| `packages/client` | 客户端 SDK / 生成代码 |
| `packages/schema` / `protocol` | API 契约与事件定义 |

---

## 源码阅读

- [[AI/01-产品与工具/Coding-Agent/opencode/大模型调用链路|大模型调用链路]] — V2 从 `session.prompt` 到 HTTP POST 的完整链路

---

## 观察维度（待补充）

- [x] 大模型调用核心链路 → 见 [[AI/01-产品与工具/Coding-Agent/opencode/大模型调用链路|大模型调用链路]]
- [ ] V1 → V2 Session 迁移边界
- [ ] 与 Cursor/Codex 差异
- [ ] MCP 与 Skills 支持
- [ ] 沙箱与权限模型

---

## 相关

- [[AI/00-AI学习体系/02-概念库/03-Agent系统/05-Agent Harness|Agent Harness]]
- [[AI/00-AI学习体系/02-概念库/03-Agent系统/10-Agent Skills与协议生态|Agent Skills]]
- [[AI/00-AI学习体系/02-概念库/06-工程生态/02-Context Engineering|Context Engineering]]
