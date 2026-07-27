---
type: redirect
status: merged
topic: Spring Aware
source_version: 6.2.x
redirect_to: "[[3-生命周期层-Aware体系详解]]"
merged_on: 2026-07-26
moved_from: 100-Q&A
---

# Aware 体系总结与常见问题

本页内容已经归并到权威主文档：[[3-生命周期层-Aware体系详解]]。

原有问答的对应位置：

| 原问题 | 主文档位置 |
| --- | --- |
| Aware 是什么、为什么需要 | [[3-生命周期层-Aware体系详解#一、设计思想]] |
| `instanceof Aware` 的含义 | [[3-生命周期层-Aware体系详解#3.1 第一层：BeanFactory 级 — `invokeAwareMethods`]] |
| `BeanNameAware` 原理 | [[3-生命周期层-Aware体系详解#7.0 BeanNameAware — 感知注册名]] |
| AAP 为什么使用 `BeanFactoryAware` | [[3-生命周期层-Aware体系详解#六、架构中实现 Aware 的代表性 Bean]] |
| `autowiringIsEnabledByDefault` 测试 | [[2-测试驱动的refresh调用链-Aware与Processor]] |
| 常见误区与调试断点 | [[3-生命周期层-Aware体系详解#十一、常见误区]] |

保留本文件是为了兼容已有双链和历史入口，不再在此维护第二份 Aware 定义。
