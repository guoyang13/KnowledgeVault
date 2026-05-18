# Eval Harness（评测脚手架）

> 跑评测集的统一框架：题目怎么喂、答案怎么解析、指标怎么算、结果怎么聚合。

## 为什么需要 harness
- 不同论文/排行榜实现细节差异 → 同一模型分数可差好几个点。
- Harness 提供"标准化跑法"，让结果可比、可复现。
- 区别于 [[Agent Harness]]（那个是包装 LLM 做 Agent；这个是包装跑评测）。

## 主流 Eval Harness
| 工具 | 出品 | 偏向 |
|---|---|---|
| **lm-evaluation-harness** | EleutherAI | 通用 LLM benchmarks，事实标准 |
| **BigCode Evaluation Harness** | BigCode | 代码任务（HumanEval、MBPP、APPS） |
| **Inspect** | UK AISI | Agent / 安全评测，工程友好 |
| **HELM** | Stanford CRFM | 多任务综合评测 + 校准/鲁棒性 |
| **OpenCompass** | 上海 AI Lab | 中英 + 多模态综合平台 |
| **SWE-bench Harness** | Princeton | 真实 GitHub issue 修复，需配合 Agent Harness |
| **τ-Bench Harness** | Sierra/Anthropic | 客服/工具使用评测 |

## 设计要点
- **Prompt 模板**：zero-shot vs few-shot、是否带 system、CoT 与否——这些都影响分数。
- **答案解析**：正则、JSON、log-probability vs 文本匹配。
- **指标**：accuracy、pass@k、BLEU、Elo、LLM-as-Judge 分。
- **可复现**：模型 commit、参数、解析逻辑都要锁定。

## 关键陷阱
- **Contamination（数据污染）**：训练集偷看了测试集，导致虚高分。
- **Prompt Sensitivity**：换个 prompt 模板，分能差 5–10 个点。
- **Cherry-picking 排行榜**：选对自己有利的子集汇报。

## 与之相关
- [[Benchmark集合]]
- [[Agent Harness]]
- [[LLM-as-a-Judge]]（→ 在 [[Benchmark集合]] 内）

## 延伸阅读
- lm-evaluation-harness GitHub 文档
- HELM 项目主页
- OpenCompass 文档（中文友好）
