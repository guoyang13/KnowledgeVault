---
type: canonical
status: reviewed
topic: Spring Bean Scope
source_version: 6.2.x
aliases:
  - Spring Bean Scope
  - Bean 作用域
---

# Bean 作用域与生命周期边界

## 摘要

Bean 作用域（Bean Scope）决定容器如何确定实例的身份、缓存与销毁边界。它回答的不是“Bean 能否注入”，而是“同一名称在不同上下文中取得的是哪个实例，以及谁负责结束它的生命周期”。

## 1. 核心作用域

| 中文名 | Scope 名称 | 实例身份 | Spring 是否自动销毁 |
| --- | --- | --- | --- |
| 单例 | `singleton` | 每个 `BeanFactory`、每个 Bean 名称一个共享实例 | 是，容器关闭时 |
| 原型 | `prototype` | 每次显式获取或依赖解析时创建新实例 | 否，创建并初始化后交给调用方 |
| 请求 | `request` | 每个 HTTP 请求一个实例 | 是，请求结束时 |
| 会话 | `session` | 每个 HTTP Session 一个实例 | 是，会话结束时 |
| 应用 | `application` | 每个 `ServletContext` 一个实例 | 是，应用结束时 |
| WebSocket | `websocket` | 每个 WebSocket 会话一个实例 | 是，会话结束时 |

“Spring 单例”不是 JVM 全局单例，也不是每个 Class 一个实例；它的边界是**容器 + Bean 名称**。

## 2. 单例与原型的创建差异

```text
singleton
  -> 先查 singletonObjects
  -> 没有则创建
  -> 完成初始化后放入单例缓存
  -> 后续返回同一实例

prototype
  -> 每次进入 createBean
  -> 创建并初始化新实例
  -> 不放入单例缓存
  -> 容器不跟踪后续销毁
```

非懒加载单例通常在 `refresh` 末段预实例化；原型 Bean 即使定义已注册，也只在实际请求时创建。

## 3. 不同作用域之间的注入

单例直接注入原型时，注入动作只在单例创建期间发生一次，因此它持有的是当时创建的那个原型实例：

```java
@Component
class ReportService {
    private final PrototypeWorker worker;

    ReportService(PrototypeWorker worker) {
        this.worker = worker;
    }
}
```

需要“每次调用取得新实例”时，使用查找方法注入（Lookup Method Injection）、`ObjectProvider<T>` 或作用域代理（Scoped Proxy）：

```java
@Component
class ReportService {
    private final ObjectProvider<PrototypeWorker> workers;

    ReportService(ObjectProvider<PrototypeWorker> workers) {
        this.workers = workers;
    }

    Report create {
        return workers.getObject.create;
    }
}
```

## 4. 作用域代理

`@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)` 注入的是稳定代理，代理在每次方法调用时定位当前作用域中的真实目标。

典型用途：把 request / session Bean 注入生命周期更长的单例。

边界：

- 代理解决“长生命周期对象如何持有短生命周期引用”，不改变目标实例本身的作用域。
- JDK 代理依赖接口；基于类的代理受 `final` 类和方法等限制。
- 代理会影响对象身份判断、调试栈和序列化设计。

## 5. 自定义 Scope

自定义 `Scope` 需要定义：

- `get`：当前上下文中取得或创建对象；
- `remove`：移除对象；
- `registerDestructionCallback`：登记销毁回调；
- `resolveContextualObject`：解析上下文对象；
- `getConversationId`：返回当前会话标识。

随后通过 `ConfigurableBeanFactory#registerScope` 注册。真正困难的不是 API，而是可靠定义上下文开始、结束、跨线程传播和清理。

## 6. 常见误区

| 误区 | 修正 |
| --- | --- |
| prototype Bean 会由容器完整销毁 | 容器只负责创建和初始化，后续清理由使用方负责 |
| `@Lazy` 等于 prototype | `@Lazy` 改变创建时机，Scope 改变实例身份边界 |
| Spring 单例天然线程安全 | 单例只保证共享身份，线程安全取决于对象状态设计 |
| 单例注入 prototype 后每次调用都会换实例 | 直接注入只解析一次，需要 Provider 或作用域代理 |

关联：[[2-Bean加载原理与源码阅读路径]] · [[4-doCreateBean深度解析]] · [[9-Bean 销毁机制详解]] · [[getBeanProvider与ObjectProvider有什么用]]
