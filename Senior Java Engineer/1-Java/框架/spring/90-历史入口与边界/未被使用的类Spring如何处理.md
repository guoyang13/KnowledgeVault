---
type: redirect
status: merged
topic: Spring component registration
source_version: 6.2.x
redirect_to: "[[2-元数据层-AnnotatedBeanDefinitionReader与组件注册详解#FAQ：类存在但没有被使用，Spring 会怎样处理？]]"
moved_from: 100-Q&A
---

# 未被使用的类，Spring 如何处理？

本问题已归并到组件注册权威文档：

[[2-元数据层-AnnotatedBeanDefinitionReader与组件注册详解#FAQ：类存在但没有被使用，Spring 会怎样处理？]]

核心结论：

- 没有注册为 Bean 的类，Spring 完全忽略。
- 已注册的非懒加载单例即使无人引用，容器启动时通常仍会创建。
- `@Lazy`、prototype Scope、条件注册和抽象定义会改变实例化行为。

创建时机继续阅读：[[2-Bean加载原理与源码阅读路径#非 lazy 单例的 eager 创建]]
