---
aliases:
  - Computer Use与Browser Agent
---

# Computer Use 与 Browser Agent

> 让 AI 直接操作真实计算机界面（截屏 + 鼠标键盘）或浏览器，能干"任何人类能在屏幕前做的事"。

## 两条技术路线
1. **像素 / 视觉路线（Computer Use）**
   - 输入：屏幕截图。
   - 输出：点击坐标、键入文本、滚动等通用动作。
   - 代表：**Anthropic Computer Use**、**OpenAI Operator**、**Gemini Computer Use**。
2. **DOM / 结构化路线（Browser Agent）**
   - 解析 HTML / Accessibility Tree，对元素 ref 操作。
   - 更准确、更快，但只能在浏览器内。
   - 代表：**browser-use**、**Playwright + LLM**、**Stagehand**、Cursor 的 cursor-ide-browser。

## 关键能力
- **多模态视觉理解**：尤其是 GUI 元素识别、表格/图表阅读。
- **长任务记忆**：步骤间保持任务目标。
- **错误恢复**：弹窗、加载、登录墙等异常处理。

## 主要难点
- **可靠性**：现实 UI 千变万化，成功率仍远低于人。
- **延迟与成本**：每一步都要截图 + 推理。
- **安全**：登录态、支付、个人数据 → 权限模型、审批、沙箱必须有。
- **Captcha / 反爬**：道德与合规边界。

## 评测
- **OSWorld**、**WebArena**、**VisualWebArena**、**Mind2Web**：主流 GUI/Web Agent benchmark。
- **τ-Bench**：偏工具调用 + 客服流程。

## 与之相关
- [[AI/00-AI知识体系/概念/01-模型层/04-多模态模型|多模态模型]]
- [[AI/00-AI知识体系/概念/03-Agent系统/05-Agent Harness|Agent Harness]]
- [[AI/00-AI知识体系/概念/07-安全治理/01-Prompt Injection与Agent安全|Prompt Injection与Agent安全]]
- [[AI/00-AI知识体系/概念/03-Agent系统/06-MCP协议|MCP协议]]

## 延伸阅读
- Anthropic Computer Use 博客与 reference 实现
- OpenAI Operator 公告
- WebArena / OSWorld 论文
