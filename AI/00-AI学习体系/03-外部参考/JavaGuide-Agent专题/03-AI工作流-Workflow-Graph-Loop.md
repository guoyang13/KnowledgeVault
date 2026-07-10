---
aliases:
  - JavaGuide Workflow Graph Loop
tags:
  - 外部参考
  - JavaGuide
  - Workflow
  - LangGraph
  - Spring AI
source: https://javaguide.cn/ai/agent/workflow-graph-loop.html
updated: 2026-07-10
---

# AI 工作流：Workflow / Graph / Loop（JavaGuide）

> 原文：[Workflow、Graph 与 Loop](https://javaguide.cn/ai/agent/workflow-graph-loop.html) · 约 7389 字
>
> 导航：[[AI/00-AI学习体系/03-外部参考/JavaGuide-Agent专题/00-JavaGuide-Agent专题-导航|JavaGuide Agent 专题导航]]
>
> 本地对照：[[AI/00-AI学习体系/02-概念库/03-Agent系统/11-AI工作流-Graph与Loop|AI工作流-Graph与Loop]]（含 LangGraph Python 示例）

## 为什么需要 AI 工作流

单轮对话难稳定交付。真实任务需：检索、工具、结构化输出、校验、重试、不满意再一轮。

LLM 三大难题：

1. 下一步不唯一 → **动态决策**
2. 结果不理想 → **自动修正**
3. 中间状态要记录 → **可调试、可恢复**

---

## 传统 vs AI 工作流

| | 传统（Camunda/Temporal） | AI 工作流 |
|--|-------------------------|-----------|
| 假设 | 同输入同节点结果确定 | 输出不确定，需运行时评估 |
| 分支 | 设计时确定（金额>1万走审批） | 「是否达标」本身需 LLM 判断 |
| 传递 | 参数 | 上下文、草稿、评分、错误、轮次 |

---

## 三层关系

```text
Workflow = 目标与过程（「生成→审核→修改直到达标」）
Graph    = 结构与载体（Node + Edge + State）
Loop     = 图上的控制模式（回边迭代修正）
```

### Graph Loop vs Agent Loop

| | Agent Loop | Graph Loop |
|--|-----------|------------|
| 层级 | Agent 顶层 while | Graph 内部回边 |
| 模式 | 推理→行动→观察 | 生成→审核→修改 |
| 关系 | 外层；Graph Loop 可嵌套在节点内 | 内层 |

---

## Graph 三要素

### Node

读 State → 执行 → 写回 State。职责单一：Draft / Review / Revise / Exit。

### Edge

| 类型 | 说明 |
|------|------|
| 顺序边 | 固定下一步 |
| 条件边 | 运行时从预定义候选中选（`addConditionalEdges`） |
| 动态路由 | 候选运行时确定（LangGraph `Send`） |
| 循环边 | 回到前序节点 |
| 并行边 | 一分多 |

条件边 vs 动态路由是**连续谱系**；多数场景条件边够用。

### State 更新策略

| 策略 | 字段示例 | Spring AI | LangGraph |
|------|----------|-----------|-----------|
| Replace | 评分、当前草稿 | `ReplaceStrategy` | 默认 |
| Append | messages | `AppendStrategy` | `Annotated[list, operator.add]` |
| Reducer | 并行写入 | 自定义 | `add_messages` 等 |

并行写 Replace 字段 → LangGraph `INVALID_CONCURRENT_GRAPH_UPDATE`。

常用字段：`input`、`messages`、`current_draft`、`review_score`、`iteration_count`、`next_node`、`output`。

---

## Loop 设计

| 类型 | 类比 | 示例 |
|------|------|------|
| 固定次数 | for | 最多重试 3 次 |
| 条件驱动 | while | 评分 < 80 继续修改 |

**实际两者必并用**：LLM 可能永远不达标，需固定次数兜底。

### 可靠 Loop 三要素

- **继续条件**：为何再来一轮
- **退出条件**：何时足够好
- **安全边界**：最大轮次、超时、Token 预算、熔断

### 嵌套循环

外层=质量迭代；内层=工具重试。计数器与退出条件**独立**。

---

## 框架对照（Spring AI Alibaba vs LangGraph）

| 概念 | Spring AI Alibaba | LangGraph |
|------|-------------------|-----------|
| 状态 | `OverAllState` + `KeyStrategyFactory` | `TypedDict` + reducer |
| 节点 | `NodeAction` | 普通函数 |
| 条件边 | `addConditionalEdges` | `add_conditional_edges` |
| 固定次数循环 | `LoopMode.count(N)` | 自维护计数器 |
| 持久化 | `MemorySaver`/`RedisSaver` | `MemorySaver`/`SqliteSaver` |
| HITL | `interruptBefore` + `updateState` | `interrupt_before` + `update_state` |

> 注：原文导语写「双框架完整代码」，正文以 **Spring AI Alibaba Java 完整示例** 为主；LangGraph 为概念对照。Python 实现见本地 [[AI/00-AI学习体系/02-概念库/03-Agent系统/11-AI工作流-Graph与Loop#十三、Graph 与 Loop 怎么实现（手写 → LangGraph）|第十一章 §13]]。

---

## Spring AI Alibaba 示例：生成→审核→修改

### 1. KeyStrategyFactory

```java
strategies.put("messages", new AppendStrategy());
strategies.put("current_draft", new ReplaceStrategy());
strategies.put("iteration_count", new ReplaceStrategy());
strategies.put("next_node", new ReplaceStrategy());
```

### 2. ReviewNode 路由逻辑

```java
String nextNode = (score >= 80 || count >= 3) ? "exit" : "revise";
```

### 3. Graph 组装

```text
START → draft → review ─┬→ revise → review（Loop）
                        └→ exit → END
```

Loop = `review → revise → review`；安全边界 = `iteration_count >= 3`。

---

## 抽象原则

- **Node**：抽象职责边界（产出什么），非「调了哪个 API」
- **Edge**：抽象流转规则（何时去哪）
- **State**：抽象必须持久记住的信息

高抽象：四个判断节点合并为一个 ReviewNode。

---

## 落地常见坑

| 坑 | 建议 |
|----|------|
| State 太粗/太细 | 按业务语义分块 |
| 循环终止模糊 | 明确轮次、阈值、超时、fallback |
| 只处理成功路径 | 图上明确重试/降级/HITL 边 |
| Token 放大 | Loop 内区分必调 LLM vs 可用代码的节点 |

### 错误四类（Spring AI Alibaba）

| 类型 | 策略 |
|------|------|
| 瞬时错误 | 指数退避重试 |
| LLM 可恢复 | 错误写入 State，循环回去 |
| 用户可修复 | `interruptBefore` 等人输入 |
| 意外错误 | 异常冒泡 |

弹性模式：熔断、舱壁（Semaphore/Resilience4j）、Saga 补偿。

### 工作流特有风险

- **State 污染**：恶意输入改 `next_node` → 路由字段白名单
- **Loop 放大攻击**：永远低分耗 Token → 独立 Token 预算

---

## 面试要点

1. 为何 AI 需要工作流？→ 不确定、需修正、需收敛
2. WF / Graph / Loop 关系？→ 目标 / 结构 / 控制模式
3. Graph Loop vs Agent Loop？→ 内外层，可嵌套
4. 防死循环？→ 继续 + 退出 + 安全边界
5. State 更新？→ Replace / Append / Reducer
6. 中断恢复？→ checkpoint + 持久化 Saver
