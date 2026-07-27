---
type: canonical
status: reviewed
topic: Spring ApplicationEvent
source_version: 6.2.x
aliases:
  - Spring ApplicationEvent
  - Spring 容器事件
---

# ApplicationEvent 事件机制详解

## 摘要

Spring 应用事件（Application Event）是在同一 `ApplicationContext` 内进行发布者与监听器解耦的机制。发布是同步还是异步、事务提交前还是提交后、异常是否传播，都由多播器、执行器和监听器类型共同决定。

## 1. 核心角色

| API | 中文定位 | 职责 |
| --- | --- | --- |
| `ApplicationEventPublisher` | 事件发布器 | 发布事件对象 |
| `ApplicationListener<E>` | 应用事件监听器 | 以接口方式接收事件 |
| `@EventListener` | 事件监听方法 | 以注解方法接收事件 |
| `ApplicationEventMulticaster` | 事件多播器 | 查找并调用匹配监听器 |
| `PayloadApplicationEvent<T>` | 载荷事件 | 包装普通对象作为事件 |

现代 Spring 允许发布任意对象；非 `ApplicationEvent` 对象会包装为 `PayloadApplicationEvent`。

## 2. 发布主线

```text
publisher.publishEvent(event)
  -> AbstractApplicationContext#publishEvent
  -> ApplicationEventMulticaster#multicastEvent
  -> 按事件类型筛选监听器
  -> 当前线程或配置的 Executor 中调用监听器
```

默认多播器在没有配置任务执行器时同步调用监听器。因此：

- 慢监听器会延长发布调用；
- 监听器异常通常会影响发布者调用；
- 线程上下文和事务上下文默认可以沿同步调用保留。

不要把“事件”天然理解为消息队列式异步通信。

## 3. 三种监听方式

```java
@Component
class OrderCreatedListener {

    @EventListener
    void on(OrderCreated event) {
    }
}
```

| 方式 | 适合场景 |
| --- | --- |
| `ApplicationListener<E>` | 框架扩展、类型明确、需要接口协议 |
| `@EventListener` | 应用代码、一个类监听多个事件 |
| `@TransactionalEventListener` | 需要与事务阶段绑定的副作用 |

`@EventListener` 还可通过 `condition` 使用 Spring Expression Language（SpEL）筛选事件，但复杂业务条件应放在显式代码中，便于测试和重构。

## 4. 事务事件

`@TransactionalEventListener` 可绑定：

- `BEFORE_COMMIT`
- `AFTER_COMMIT`（默认）
- `AFTER_ROLLBACK`
- `AFTER_COMPLETION`

它解决“监听动作应在哪个事务阶段发生”，不自动提供跨进程可靠投递。提交后发送外部消息仍需考虑进程崩溃窗口；高可靠场景通常结合事务消息、Outbox Pattern 或其他持久化机制。

## 5. 父子上下文边界

事件首先在当前上下文发布，并可能向父上下文传播。父上下文事件不会自动反向传播给子上下文。存在父子容器时，要警惕同一监听器被重复注册或事件被多次观察。

## 6. 设计权衡

适合使用：

- 同进程内的一对多通知；
- 框架生命周期扩展；
- 不要求调用方知道所有后续动作的模块协作。

谨慎使用：

- 必须立即返回结果的请求；
- 需要跨进程投递、重试和消费确认；
- 业务主流程被大量隐式监听器切碎；
- 强顺序、幂等和审计要求没有显式设计。

## 7. 常见误区

| 误区 | 修正 |
| --- | --- |
| Spring 事件默认异步 | 默认通常同步执行 |
| `@Async` 后仍自动共享事务 | 切换线程后不应假设原事务上下文存在 |
| `AFTER_COMMIT` 等于可靠消息 | 它只绑定事务阶段，不提供持久化投递保证 |
| 事件完全没有依赖 | 编译依赖降低，但事件契约和时序依赖仍然存在 |

关联：[[5-Context层-ApplicationContext详解]] · [[3-refresh方法详解]] · [[10-Spring事务实现详解]]
