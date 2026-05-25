---
aliases:
  - Agent学习路线与项目阶梯
---

# Agent 学习路线与项目阶梯

> 学 Agent 不应从“堆框架”和“角色扮演多 Agent”开始，而应从最小 agent loop、工具调用、harness、skills、评测和真实项目闭环开始。

## 当前更值得投入的方向

| 优先级 | 方向 | 为什么 |
|---|---|---|
| 1 | Coding Agent | 真实代码库、shell、文件编辑、测试、权限、上下文压缩，是最好的 agent 工程样本 |
| 2 | Agent Harness Engineering | 工具协议、权限、状态、反馈、回放、CI、评测是 agent 能力的重要来源 |
| 3 | Personal Agent | 长运行、本地优先、跨应用、记忆、skills、消息入口，接近个人操作系统 |
| 4 | Skills / MCP / A2A / ACP | skills 复用能力，MCP 连接工具，A2A 连接 agent，ACP 连接宿主应用 |
| 5 | Evaluation & Safety | 没有 eval、trace、权限边界的 agent 只能算 demo |

不建议把主线放在已经模板化的 crew/role-play 框架上。它们可了解历史，但不应成为学习主轴。

## Learning Todo List

### Stage 0：理解什么是 Agent

- 区分 chatbot、workflow、agent、multi-agent。
- 理解基本循环：observe -> think -> act -> observe。
- 明白什么时候不该用 agent：任务稳定、流程可预测、普通脚本能解决时，agent 可能增加不确定性。
- 输出一页短笔记：我的场景为什么需要 agent，而不是普通 workflow？

### Stage 1：构建最小 Agent Loop

- 会用 LLM API 完成普通对话。
- 会让模型输出结构化 JSON。
- 会定义一个工具函数，如 search、calculator、read_file。
- 会解析 tool call / function call。
- 会执行工具，并把工具结果喂回模型。
- 会加最大步数、超时和错误处理。

产出：一个 50-150 行的最小 agent，可以选择工具、执行工具、返回最终答案。

### Stage 2：Tool Use、RAG 与 Memory

- 会做检索增强生成：chunk、embed、retrieve、answer with citations。
- 会把搜索、数据库、文件、浏览器、代码执行接成工具。
- 会区分短期上下文、会话记忆、长期记忆。
- 会处理工具失败、空结果、重复调用、幻觉引用。
- 会让 agent 在回答里给出来源或证据。

产出：一个资料研究助手，输入主题后自动搜索、筛选、总结并输出引用链接。

### Stage 3：研究一个现代 Agent Harness

重点不是框架 API，而是它如何组织：

```text
agent loop
tool registry
permission gate
session store
context compaction
trace / replay
subagent / orchestration
```

产出：一个可调试的 harness demo，包含运行步骤、示例输入输出和失败记录。

### Stage 4：Multi-Agent 是协调，不是魔法

- 理解 planner / executor / reviewer / critic / router 等角色。
- 用 supervisor 或 graph 管理多 agent，而不是让 agent 随便聊天。
- 定义每个 agent 的职责边界、输入输出 schema、停止条件。
- 处理循环、争论、任务漂移、上下文膨胀。
- 判断什么时候单 agent 更好。

产出：一个 research -> write -> review -> revise 的小型多 agent 系统。

### Stage 5：Skills、协议与能力封装

现代 agent 的能力不只来自模型和工具，也来自可复用 skills。一个好的 skill 像小型操作手册：告诉 agent 什么时候使用、怎么使用、需要哪些脚本/资源、如何验证结果。

产出：一个可复用 skill，例如 code-review、research-report、migration-helper、pdf-extraction 或 release-note-writer。

详见 [[AI/00-AI知识体系/概念/03-Agent系统/10-Agent Skills与协议生态|Agent Skills与协议生态]]。

### Stage 6：Browser 与 Computer-Use Agents

- 理解 browser agent 和普通 API tool 的区别。
- 会用 Playwright 或 browser-use 做网页观察和点击。
- 给浏览器操作加安全限制：不登录敏感账号、不越权、不绕过平台规则。
- 处理页面变化、弹窗、加载失败、元素定位失败。
- 记录截图、DOM、动作日志，方便复盘。

产出：一个只操作公开网页的 browser agent，例如打开网页、提取信息、生成摘要。

### Stage 7：Evaluation、Observability 与 Safety

- 准备固定测试集，而不是只看 demo。
- 记录成功率、失败原因、工具调用次数、成本、延迟。
- 通过 trace 判断失败发生在 prompt、工具、检索、模型还是状态管理。
- 给危险工具加人工确认，例如发邮件、删文件、付款、发布内容。
- 了解 prompt injection、data exfiltration、tool abuse 等风险。
- 用回归测试防止 prompt 或工具改动后能力退化。

产出：一个 agent eval 表格，至少包含 20 个任务、期望结果、实际结果、失败分类。

### Stage 8：Ship 一个真实 Agent

- 有明确用户、明确任务、明确成功标准。
- 有日志、trace、错误重试、超时、成本上限。
- 有权限边界和人工确认机制。
- 有部署方式：CLI、Web app、Slack bot、GitHub Action 或后台任务。
- 有 README：怎么运行、怎么配置 key、怎么扩展工具、有哪些限制。

产出：一个别人能 clone 下来跑的 agent 项目。

## Project Ladder

| Level | Project | 学到什么 |
|---|---|---|
| 1 | Calculator Agent | 最小 tool call loop |
| 2 | Web Research Agent | 搜索、筛选、引用、总结 |
| 3 | PDF QA Agent | RAG、chunk、retrieval、citation |
| 4 | Coding Review Agent | 读取 diff、风险排序、测试建议 |
| 5 | Browser Agent | 页面观察、点击、提取、失败恢复 |
| 6 | Nano Coding Agent | shell、文件编辑、权限、session、compact |
| 7 | Agent Gateway | channel、routing、session、memory、heartbeat、delivery |
| 8 | Reusable Skill Pack | SKILL.md、脚本、模板、触发条件、smoke test |
| 9 | Multi-Agent Writer | planner、writer、reviewer 协作 |
| 10 | Personal Agent | 记忆、skills、消息入口、长运行任务 |
| 11 | Production Harness | evals、trace、权限、CI、runner、回放 |

## 现代 Agent 系统地图

| 类型 | 推荐研究对象 | 学习重点 |
|---|---|---|
| Coding Agent 产品 | Claude Code、Codex、Cursor、OpenCode | shell、文件编辑、权限、测试、上下文压缩 |
| From-scratch Harness | learn-claude-code、claw0、hello-agents | agent loop、session、tool registry、trace、gateway |
| Personal Agent | OpenClaw、Hermes Agent、CyberClaw | 长运行、skills、记忆、消息入口、权限边界 |
| Deep Research / RAG | GPT Researcher、Open Deep Research、STORM、RAGFlow | 搜索、抓取、检索、引用、报告生成 |
| Browser / Desktop Agent | browser-use、UI-TARS-desktop | 观察、点击、视觉理解、失败恢复 |
| Production Framework | LangGraph、OpenAI Agents SDK、Pydantic AI、Qwen-Agent | 状态图、结构化输出、工具编排、类型安全 |

## 学习原则

- 先做出能跑的小 agent，再读深层论文。
- 优先做小而可靠的 agent，而不是炫技 demo。
- 工具要有严格 schema。
- 先加 eval，再加更多 agent。
- 每次重要运行都要有 trace。
- 把 multi-agent 当成协调问题。
- 风险动作必须 human-in-the-loop。

## 与之相关

- [[AI/00-AI知识体系/概念/03-Agent系统/01-Workflow vs Agent|Workflow vs Agent]]
- [[AI/00-AI知识体系/概念/03-Agent系统/05-Agent Harness|Agent Harness]]
- [[AI/00-AI知识体系/概念/03-Agent系统/10-Agent Skills与协议生态|Agent Skills与协议生态]]
- [[AI/00-AI知识体系/概念/03-Agent系统/07-Computer Use与Browser Agent|Computer Use与Browser Agent]]
- [[AI/00-AI知识体系/概念/05-评测/01-Eval Harness|Eval Harness]]

## 延伸阅读

- Anthropic: *Building effective agents*
- OpenAI: *A practical guide to building agents*
- Claude Code / Codex / OpenClaw / Hermes Agent 官方文档与源码
- ReAct、Toolformer、Reflexion、AgentBench、SWE-bench、WebArena
