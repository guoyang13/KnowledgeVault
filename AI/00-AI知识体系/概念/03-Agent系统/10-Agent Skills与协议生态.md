---
aliases:
  - Agent Skills与协议生态
---

# Agent Skills 与协议生态

> 现代 Agent 的能力不只来自模型和工具，也来自可复用的 skills、标准化工具协议和宿主应用协议。Skill 负责“怎么做一类任务”，Tool 负责“能调用什么接口”，MCP/A2A/ACP 负责“怎么连接外部世界”。

## Skill、Tool、Prompt、MCP 的区别

| 概念 | 核心作用 | 例子 |
|---|---|---|
| Prompt | 一次性指令 | “请帮我总结这篇文章” |
| Tool | 可调用接口 | search、read_file、run_sql、send_email |
| Skill | 可复用流程知识 | code-review、research-report、pdf-extraction |
| MCP | Agent 与工具/资源的连接协议 | filesystem、git、postgres、browser |
| A2A | Agent 与 Agent 的协作协议 | supervisor 调用外部专业 agent |
| ACP | Agent 与宿主应用的接口 | IDE、终端、编辑器接入 agent |

一句话：

```text
Tool = 做动作的接口
Skill = 什么时候做、怎么做、做到什么算完成
Protocol = Agent 与外部系统如何互联
```

## 一个好 Skill 应该包含什么

最小结构：

```text
skill-name/
  SKILL.md
  scripts/
  references/
  assets/
```

`SKILL.md` 需要说明：

- name：技能名称。
- description：什么时候应该触发。
- workflow：完成任务的步骤。
- constraints：边界、注意事项、安全要求。
- validation：如何验证结果。

## Skill 的质量标准

一个 skill 不是把 prompt 写长，而是降低任务失败率。

| 维度 | 好 Skill | 差 Skill |
|---|---|---|
| 触发条件 | 清楚、可判定 | “所有任务都用我” |
| 步骤 | 短、可执行 | 大段背景知识 |
| 资源 | 按需加载 | 把所有资料塞进上下文 |
| 验证 | 有 smoke test / checklist | 没有验收标准 |
| 安全 | 明确权限边界 | 默认允许危险动作 |

## MCP：连接工具和资源

MCP 负责 Agent 与工具/资源之间的标准连接。

三类原语：

1. **Tools**：可执行函数，带 JSON Schema。
2. **Resources**：可读取内容，如文件、数据库表、API 响应。
3. **Prompts**：可参数化 prompt 模板。

常见 MCP Server：

```text
filesystem
git
shell
postgres
browser / puppeteer
Slack
GitHub
SaaS connectors
```

详见 [[AI/00-AI知识体系/概念/03-Agent系统/06-MCP协议|MCP协议]]。

## A2A：连接 Agent

A2A 的目标是让不同 Agent 能发现、通信和协作。

适合场景：

- 一个主 Agent 调用外部专业 Agent。
- 企业内部多个 Agent 负责不同业务域。
- 跨组织协作时需要标准化任务描述、状态和结果。

需要注意：

- A2A 不是“多个角色聊天”。
- 必须定义职责边界、输入输出 schema、停止条件和权限。
- 多 Agent 成本、延迟和调试复杂度都会上升。

## ACP：连接宿主应用

ACP 关注 Agent 与 IDE、终端、编辑器、宿主应用之间的接口。

它解决的问题是：

```text
Agent 如何被宿主应用发现？
Agent 如何接收上下文？
Agent 如何请求编辑、命令、文件、诊断信息？
Agent 如何把结果返回给宿主？
```

在 coding agent 场景里，ACP 这类协议会影响 IDE、CLI、编辑器和 Agent 的协作边界。

## Skills 与 Agent Harness 的关系

Harness 负责运行系统：

```text
上下文管理
工具循环
权限控制
状态存储
错误恢复
trace / replay
```

Skill 负责某类任务的经验沉淀：

```text
什么时候使用
按什么步骤做
读哪些参考
调用哪些脚本
怎么验收
```

强 Agent 往往是：

```text
强模型 + 高质量 harness + 好工具 + 可复用 skills + 可观测 eval
```

## 最小 Skill 练习

可以先写一个 `research-report` skill：

```text
触发：用户要求调研某个主题并产出报告
步骤：
1. 明确问题边界
2. 搜索权威来源
3. 摘要和交叉验证
4. 输出结构化报告
5. 附来源和不确定性
验收：
- 至少 3 个权威来源
- 明确事实和推断
- 不混入无法验证的结论
```

也可以写一个 `code-review` skill：

```text
触发：用户要求 review 代码、PR 或 diff
步骤：
1. 先看变更范围
2. 优先找 bug、回归、测试缺口
3. 按严重程度排序
4. 给出文件和行号
验收：
- findings first
- 不把风格建议放在 bug 前面
```

## 与之相关

- [[AI/00-AI知识体系/概念/03-Agent系统/05-Agent Harness|Agent Harness]]
- [[AI/00-AI知识体系/概念/03-Agent系统/06-MCP协议|MCP协议]]
- [[AI/00-AI知识体系/概念/03-Agent系统/04-Multi-Agent编排|Multi-Agent编排]]
- [[AI/00-AI知识体系/概念/03-Agent系统/09-Agent学习路线与项目阶梯|Agent学习路线与项目阶梯]]
- [[AI/00-AI知识体系/概念/07-安全治理/01-Prompt Injection与Agent安全|Prompt Injection与Agent安全]]

## 延伸阅读

- Claude Code Skills / Agent Skills 文档
- OpenClaw Skills 文档
- Model Context Protocol 官方文档
- Agent2Agent Protocol specification
- Agent Client Protocol
- SWE-Skills-Bench、Agent Skills analysis
