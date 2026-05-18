---
aliases:
  - Coding Agent
---

# Coding Agent

> 当前 AI Agent 落地最成熟、最火热的方向。按形态可分四类。

## 四种形态
1. **IDE 内 Agent**：在编辑器里实时协作。
   - **Cursor**、**Windsurf**、**Antigravity**（Google）、**GitHub Copilot Agent**。
   - 优势：低延迟、强可视化、习惯成本低。
2. **CLI Agent**：终端里跑。
   - **Codex CLI**（OpenAI）、**Claude Code**（Anthropic）、**Aider**、**Gemini CLI**、**Cursor CLI**、**opencode**。
   - 优势：脚本化、CI 友好、易嵌入自定义工作流。
3. **Cloud / Background Agent**：远端沙箱跑长任务。
   - **Cursor Cloud Agents / Background Agents**、**Codex Cloud**、**Devin**、**Jules**（Google）、**Replit Agent**。
   - 优势：并行、不占本地、隔离风险。
4. **PR / CI Agent**：以 review / 修 bug 为主。
   - **Bugbot**（Cursor）、**CodeRabbit**、**Greptile**、**Diamond**。

## 决定能力的两件事
- **底层模型**：Claude Sonnet 4+、GPT-5、Gemini 3 是当前第一梯队。
- **[[Agent Harness]] 质量**：同模型 + 不同 harness，SWE-bench 能差 10–30 个点。

## 工程关键
- **Context 管理**：仓库太大塞不下 → repo map、grep / glob、按需读文件。
- **diff-based 编辑**：让模型输出 diff 而非整文件，更稳。
- **多步循环**：编辑 → 编译/测试 → 看错误 → 修。
- **子 Agent / 并行任务**：例如同时改前后端。
- **审批模型**：写文件 / 跑命令 / 删除 / 推远端 → 分级权限。
- **[[MCP协议]]**：标准化外部工具接入。

## 评测
- **SWE-bench Verified**：真实 GitHub issue。
- **Terminal-Bench**、**LiveCodeBench**、**HumanEval+**。
- 见 [[Benchmark集合]]。

## 与之相关
- [[Agent Harness]]
- [[MCP协议]]
- [[Context Engineering]]
- [[Multi-Agent编排]]
- [[Benchmark集合]]

## 延伸阅读
- SWE-bench 论文与官方排行榜
- Cursor / Claude Code / Codex CLI 官方文档
- Anthropic *Building Effective Agents* 中的 Coding Agent 章节
