---
type: quick-reference
status: reviewed
topic: Spring IoC / Dependency Injection
source_version: 6.2.x
aliases:
  - IoC 与 DI 核心整合速查
  - 3-速查-IoC与DI核心整合速查
---

# IoC 与 DI 核心整合速查

> [!abstract] 用途
> 用 10 分钟建立 Spring 容器的整体心智模型。本篇只保留结论和路径；正式定义、源码证据与边界条件进入链接的权威文档。

## 1. 两个基础概念

| 中文名 | 英文名 | 精确定义 | 在 Spring 中的表现 |
| --- | --- | --- | --- |
| 控制反转 | Inversion of Control, **IoC** | 对象创建、组装和生命周期的控制权从业务代码转交给容器 | 容器读取元数据、注册定义、创建并管理 Bean |
| 依赖注入 | Dependency Injection, **DI** | 容器把对象所需的协作者提供给对象 | 构造器、字段、Setter 或方法参数注入 |

**关系**：IoC 是设计原则，DI 是 Spring 实现 IoC 的核心手段之一。IoC 不等于 DI，DI 也不只存在于 Spring。

正式定义与反例：[[1-IoC与DI核心概念]]

## 2. 五个结构角色

| 角色 | 一句话职责 | 不负责什么 |
| --- | --- | --- |
| Bean 定义（`BeanDefinition`） | 描述“创建什么、怎样创建、怎样管理” | 不是最终 Bean 实例 |
| Bean 定义注册表（`BeanDefinitionRegistry`） | 保存 `beanName -> BeanDefinition` | 不承担完整 Bean 创建流程 |
| Bean 工厂（`BeanFactory`） | 按名称或类型取得 Bean，是容器的基础契约 | 不等同于 `FactoryBean` |
| 工厂型 Bean（`FactoryBean<T>`） | 作为一个 Bean，为容器生产另一种对象 | 不是通用容器 |
| 应用上下文（`ApplicationContext`） | 在 `BeanFactory` 上整合事件、资源、环境和国际化等能力 | 不是另一套独立 Bean 工厂 |

深入阅读：

- 元数据：[[1-元数据层-BeanDefinition三兄弟详解]]
- 注册：[[3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
- 容器：[[4-容器层-BeanFactory接口体系详解]]
- 工厂型 Bean：[[4-工厂Bean-FactoryBean接口体系详解]]
- 应用上下文：[[5-Context层-ApplicationContext详解]]

## 3. 一条主链

```text
配置来源
  -> 解析为 BeanDefinition
  -> 注册到 BeanDefinitionRegistry
  -> refresh 完成容器准备
  -> getBean 触发或取得 Bean
  -> createBean / doCreateBean
  -> 实例化
  -> 属性填充与依赖解析
  -> Aware 回调
  -> 初始化前 BeanPostProcessor
  -> 初始化回调
  -> 初始化后 BeanPostProcessor
  -> 可用 Bean（可能是代理）
  -> 容器关闭时执行销毁回调
```

关键源码入口：

| 阶段      | 主要入口                                              | 对应文档                           |
| ------- | ------------------------------------------------- | ------------------------------ |
| 容器启动    | `AbstractApplicationContext#refresh`              | [[3-refresh方法详解]]              |
| Bean 获取 | `AbstractBeanFactory#doGetBean`                   | [[2-Bean加载原理与源码阅读路径]]          |
| Bean 创建 | `AbstractAutowireCapableBeanFactory#doCreateBean` | [[4-doCreateBean深度解析]]         |
| 依赖解析    | `DefaultListableBeanFactory#resolveDependency`    | [[5-依赖注入实现原理]]                 |
| 初始化与扩展  | `initializeBean`、`BeanPostProcessor`              | [[2-扩展点层-BeanPostProcessor详解]] |
| 销毁      | `DisposableBeanAdapter#destroy`                   | [[9-Bean 销毁机制详解]]              |

## 4. 三类扩展机制

| 中文名 | API | 介入对象 | 典型时机 | 典型用途 |
| --- | --- | --- | --- | --- |
| Bean 工厂后处理器 | `BeanFactoryPostProcessor` | Bean 定义与容器元数据 | 普通 Bean 大规模实例化前 | 修改属性占位符、调整定义 |
| Bean 后处理器 | `BeanPostProcessor` | Bean 实例 | 初始化前后 | 注解处理、代理包装、回调适配 |
| 容器感知回调 | `Aware` 接口族 | 当前 Bean | 属性填充后、初始化前 | 获得 Bean 名称、工厂、上下文等基础设施 |

要点：

- `BeanFactoryPostProcessor` 处理“菜谱”，`BeanPostProcessor` 处理“成品对象”；比喻只帮助记忆，正式判断看介入对象与时机。
- `Aware` 是容器回调，不是普通业务依赖注入的替代品。
- Spring AOP 的代理通常由特定 `BeanPostProcessor` 在初始化阶段参与创建。

深入阅读：[[1-扩展点层-BeanFactoryPostProcessor详解]] · [[2-扩展点层-BeanPostProcessor详解]] · [[3-生命周期层-Aware体系详解]] · [[7-IoC扩展点三部曲对照]]

## 5. 依赖注入如何选

| 场景 | 首选方式 | 原因 |
| --- | --- | --- |
| 必需依赖 | 构造器注入（Constructor Injection） | 依赖明确、对象可保持有效状态、便于测试 |
| 可选依赖 | Setter 注入（Setter Injection）或 `ObjectProvider<T>` | 可表达“没有也能工作” |
| 多实现选择 | `@Qualifier`，必要时配合 `@Primary` | 消除候选歧义 |
| 延迟或按需获取 | `ObjectProvider<T>` | 避免立即解析，并支持流式/可选获取 |
| 框架基础设施回调 | 对应 `Aware` 接口 | 表达容器协议，而非业务依赖 |

候选解析不是简单的“先按类型、失败再按名称”。更准确的理解是：

1. 先确定依赖类型与限定信息。
2. 从容器中筛选类型兼容的候选。
3. 结合 `@Qualifier`、`@Primary`、优先级和依赖名称等规则缩小范围。
4. 若仍无法得到唯一结果，则抛出缺失或歧义异常。

深入阅读：[[5-依赖注入实现原理]] · [[Spring注入注解与byType-byName解析逻辑]] · [[getBeanProvider与ObjectProvider有什么用]]

## 6. 最易混淆的边界

| 易混组 | 关键区别 |
| --- | --- |
| `BeanFactory` 与 `FactoryBean` | 前者是容器契约；后者是容器中的特殊 Bean |
| `BeanDefinition` 与 Bean | 前者是创建和管理元数据；后者是运行时对象 |
| `BeanFactoryPostProcessor` 与 `BeanPostProcessor` | 前者处理定义；后者处理实例 |
| DI 与 `Aware` | 前者提供业务协作者；后者传递容器基础设施 |
| 实例对象与最终暴露对象 | 初始化后处理器可能返回代理，因此两者未必是同一引用 |
| Java 动态代理与 Spring AOP | 前者是语言/字节码机制；后者在其上组织切点、通知和拦截器链 |

相关入口：[[5-工厂Bean-BeanFactory与FactoryBean的区别]] · [[1010-Java动态代理与运行时代理机制]] · [[8-Spring-AOP代理创建详解]]

## 7. 掌握检查

不看笔记，尝试回答：

1. IoC 与 DI 为什么不是同义词？
2. `BeanDefinition` 在什么时候变成 Bean？
3. `BeanFactoryPostProcessor` 和 `BeanPostProcessor` 分别改变什么？
4. 属性填充、Aware、初始化回调和代理包装的先后关系是什么？
5. 为什么 `getBean("x")` 和 `getBean("&x")` 对 `FactoryBean` 返回不同对象？
6. 多个同类型候选存在时，Spring 如何缩小选择范围？
7. 为什么循环依赖与 AOP 代理会共同涉及三级缓存？

答不完整时按主链回到对应主题：[[6-循环依赖与三级缓存详解]]。
