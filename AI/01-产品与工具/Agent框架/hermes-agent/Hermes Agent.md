---
title: Hermes Agent — 会自我进化的 AI Agent 框架
tags: [AI, Agent, 开源, Nous Research]
created: 2026-04-20
---

# Hermes Agent

> **Hermes Agent** 是由 [Nous Research](https://nousresearch.com) 开发的开源自主 AI Agent 框架。
> 核心理念：AI 不应该是无状态的工具，而应该是一个**会成长的伙伴**。

---

## 一、核心特别之处

### 1. 内置学习循环（最核心的差异化能力）

普通 AI 助手每次对话都是"从零开始"，而 Hermes 遵循一个闭环：

```
执行任务 → 成功了吗？→ 提取经验 → 生成"技能文档" → 下次遇到类似任务直接复用
```

- **自主创建技能**：成功完成复杂任务后，自动生成 **Skill Document**（结构化 Markdown），作为可复用的操作手册
- **持续精炼**：随着使用次数增多，不断优化技能文档，**用得越久越强**

### 2. 三层持久记忆

| 记忆层 | 作用 |
|:---|:---|
| **会话记忆 (Session)** | 维持当前对话的连贯性 |
| **持久记忆 (Persistent)** | 跨会话存储用户信息、偏好、历史经验 |
| **程序记忆 (Procedural/Skills)** | 存储自己生成的可复用工作流和方法 |

能**记住几周甚至几个月前的上下文**，而不是每次对话都失忆。

### 3. 用户建模

Hermes 会构建持久的**用户画像**——包括决策历史、任务模式和偏好。越用越懂你，不需要每次重复交代习惯。

### 4. 极高的灵活性和可移植性

| 特性 | 说明 |
|:---|:---|
| **模型无关** | 支持 200+ 种 AI 模型（OpenRouter、本地 Ollama、任何 OpenAI 兼容接口），一条命令切换 |
| **基础设施无关** | 可以跑在 $5 的 VPS、GPU 集群、甚至 Serverless 上 |
| **多平台连接** | 通过 Telegram、Discord、Slack、WhatsApp、Signal、SMS 等与 Agent 交互 |

### 5. 丰富的工具集

- **40+ 内置工具**：网页搜索、浏览器自动化、文件操作、终端/Shell 访问
- **MCP 协议支持**：可通过 Model Context Protocol 安全连接外部服务器，扩展能力
- **安全机制**：命令审批流程、危险命令拦截、沙箱隔离执行

---

## 二、安装

### 前提条件

| 项目 | 要求 |
|:---|:---|
| **系统** | Linux / macOS / Windows (WSL2) / Android (Termux) |
| **软件** | Git（安装脚本会自动处理 Python、Node.js 等） |
| **内存** | 最低 4 GB，推荐 8 GB+ |
| **模型** | 需要 API Key（OpenRouter / OpenAI / Anthropic 等）或本地 Ollama，且模型需支持 ≥64K tokens 上下文 |

### 一键安装

```bash
curl -fsSL https://raw.githubusercontent.com/NousResearch/hermes-agent/main/scripts/install.sh | bash
```

安装完成后重载 shell：

```bash
source ~/.zshrc   # macOS zsh
# 或
source ~/.bashrc  # Linux bash
```

### 初始配置

```bash
hermes setup    # 交互式配置向导（LLM 提供商 + 消息网关，一次搞定）
hermes model    # 单独切换模型提供商/模型
hermes doctor   # 环境诊断，排查问题
```

**本地模型示例（Ollama）**：选择 "Custom endpoint"，填入 `http://localhost:11434/v1`

---

## 三、使用

### CLI 常用命令

| 命令 | 说明 |
|:---|:---|
| `hermes` | 启动终端交互界面，开始对话 |
| `hermes model` | 切换 LLM 模型 |
| `hermes tools` | 配置启用的工具 |
| `hermes config set` | 设置单个配置项 |
| `hermes update` | 更新到最新版 |
| `hermes doctor` | 诊断问题 |

### 消息网关（Telegram / Discord / Slack / WhatsApp / Signal / Email）

```bash
hermes gateway setup   # 配置消息平台
hermes gateway start   # 启动网关
```

配置后就可以通过 Telegram 等平台远程和 Agent 对话，而 Agent 在云端工作。

### 会话中斜杠命令

| 命令 | 功能 |
|:---|:---|
| `/new` / `/reset` | 新建 / 重置对话 |
| `/model [provider:model]` | 会话中切换模型 |
| `/personality [name]` | 切换人设 |
| `/retry` / `/undo` | 重试 / 撤销上条回复 |
| `/skills` | 查看已学会的技能 |
| `/compress` | 压缩上下文以节省 token |
| `/usage` | 查看 token 用量 |
| `/insights [--days N]` | 查看使用洞察 |
| `Ctrl+C` (CLI) / `/stop` (消息) | 停止当前任务 |

### 技能系统

Agent 成功完成复杂任务后，会自动生成 **Skill Document**，之后可以用 `/技能名` 直接调用。查看所有技能：

```bash
/skills
```

---

## 四、与其他 Agent 框架对比

| 特性 | Hermes Agent | AutoGPT | LangChain |
|:---|:---|:---|:---|
| 自我学习/进化 | ✅ 核心特性 | ❌ | ❌ |
| 持久记忆 | ✅ 三层记忆 | 部分支持 | 需手动实现 |
| 用户建模 | ✅ 自动 | ❌ | ❌ |
| 模型可切换 | ✅ 200+ | 有限 | ✅ |
| 开源 | ✅ | ✅ | ✅ |
| 上手难度 | 中等 | 较高 | 需要编程 |

---

## 五、总结

Hermes Agent 的核心卖点是**学习循环**——别的 Agent 每次都从零开始解决问题，Hermes 会把经验积累下来，越用越聪明。

对于个人开发者来说特别有吸引力：一台便宜的服务器 + 一个开源模型，就能拥有一个随着时间不断进化的私人 AI 助手。

---

## 参考

- [Nous Research 官方](https://nousresearch.com)
- [Hermes Agent GitHub](https://github.com/NousResearch/hermes-agent)
- [Hermes Agent 官网](https://hermesagent.agency)
