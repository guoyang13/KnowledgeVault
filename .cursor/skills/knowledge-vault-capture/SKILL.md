---
name: knowledge-vault-capture
description: >-
  Capture conversation and external research into the KnowledgeVault Obsidian
  repo with correct placement, dedup, frontmatter, and navigation updates.
  Use when writing or updating markdown in this repo, when the user asks to
  save/沉淀/记进知识库, or when turning a Q&A into a durable note.
---

# KnowledgeVault 沉淀

## 何时沉淀

**应写：**

- 用户要求保存、写进知识库、记下来
- 新概念/系统设计/专题串讲/踩坑总结，且值得二次打开
- 本地笔记过时，需用外部资料更新

**可不写：**

- 一次性杂问、纯闲聊
- 明显时效信息（价格、版本表）且用户未要求归档——可口头问是否要记

## 写入流程

1. **搜索去重**：Grep/Glob 查是否已有同类笔记 → **合并更新** 优先于新建
2. **选目录**（见下）
3. **读 1～2 篇相邻笔记**，对齐标题层级、语气、双链习惯
4. **起草正文**：定义 + 例子 + 取舍；可独立阅读，少堆裸链接清单
5. **Frontmatter**：`aliases`、`tags`；外部摘录加 `source`、`updated`
6. **补双链**：链到相关概念与上级导航
7. **更新导航**：新增专题或重要页面时，改对应 `00-*导航.md` / `00-核心索引.md`

## 目录速查


| 路径                      | 放什么                             |
| ----------------------- | ------------------------------- |
| `AI/00-AI学习体系/02-概念库/`  | 稳定概念、原理、架构（主笔记）                 |
| `AI/00-AI学习体系/03-外部参考/` | 第三方摘录，不替代概念库                    |
| `AI/01-产品与工具/`          | Cursor、Agent 框架、网关等产品向          |
| `AI/02-环境与实操/`          | 环境、工具链实操                        |
| `AI/04-Prompt模板/`       | 领域 Prompt 模板（非概念课）              |
| `Senior Java Engineer/` | Java、DDIA、DDD、中间件、架构            |
| `本体模型/`                 | 术语口径、本体建模、 competency questions |


**AI 内外关系：** 概念 → `02-概念库`；摘录 → `03-外部参考`（详见 `AI/00-AI学习体系/03-外部参考/00-外部参考导航.md`）。

## Frontmatter 模板

**概念笔记：**

```yaml
---
aliases:
  - 简短别名
tags:
  - 主题标签
---
```

**外部参考摘录：**

```yaml
---
aliases:
  - 资料名称
tags:
  - 外部参考
source: https://example.com/original
updated: YYYY-MM-DD
---
```



## 正文结构（概念类）

```markdown
# 标题

> 一句话：这篇解决什么问题

## 是什么 / 为什么

## 怎么做 / 关键机制

## 例子或对比

## 关联

- [[相关笔记]]
```



## 质量检查

- [ ] 与已有笔记重复了吗？合并而非复制
- [ ] 目录、tags、导航是否正确
- [ ] 外部事实是否标注 source + updated
- [ ] 双链是否指向真实存在的笔记（或同步新建）
- [ ] 用户是否要求 commit（默认不 commit）



## 禁止

- 不为「显得完整」新建与现有笔记高度重复的页面
- 不把时效价格/版本表写进长期概念库（可放外部参考或单独「前沿动态」）
- 不未经同意批量移动目录或改仓库结构

