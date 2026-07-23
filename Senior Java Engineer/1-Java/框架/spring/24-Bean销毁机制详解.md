# Bean 销毁机制详解

> 导航：[[00-Spring-Bean加载-学习导航]] · **下篇 16–25** · Bean 销毁
>
> 源码：
> - `spring-beans/.../support/AbstractAutowireCapableBeanFactory.java`（`doCreateBean` 步骤 5）
> - `spring-beans/.../support/AbstractBeanFactory.java`（`registerDisposableBeanIfNecessary`）
> - `spring-beans/.../support/DisposableBeanAdapter.java`
> - `spring-beans/.../support/DefaultSingletonBeanRegistry.java`（`destroySingletons`）
>
> 前置：[[17-Bean加载原理与源码阅读路径]] · [[12-扩展点层-BeanPostProcessor详解]]
>
> 关联：[[18-refresh方法详解]] · [[09-容器层-BeanFactory与Registry详解]] · [[22-Spring-AOP代理创建详解]]

---

## 一句话

`registerDisposableBeanIfNecessary()` 在 Bean **创建成功末尾**登记销毁回调；真正执行在 **`context.close()`**（容器关闭），而非「等 JVM 退出」。目的是在 Context 生命周期结束时 **主动、有序** 释放连接、线程等外部资源。

---

## 一、在 `doCreateBean()` 中的位置

```text
doCreateBean()
├─ ① createBeanInstance()              实例化
├─ ② applyMergedBeanDefinitionPostProcessors()
├─ ③ addSingletonFactory()             三级缓存
├─ ④ populateBean()                     属性注入
├─ ⑤ initializeBean()                  初始化 + AOP → exposedObject（可能是代理）
├─ ⑥ 循环依赖一致性校验
└─ ⑦ registerDisposableBeanIfNecessary(beanName, bean, mbd)  ← ★ 本文
return exposedObject;                   对外返回的可能是代理
```

**关键细节：**

- 传入的是 **`bean`（原始对象）**，不是 `exposedObject`（代理）
- **只登记，不立刻销毁**；登记的是 `DisposableBeanAdapter`，容器关闭时再调 `destroy()`

---

## 二、`registerDisposableBeanIfNecessary` 源码逻辑

```java
protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
    if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
        if (mbd.isSingleton()) {
            registerDisposableBean(beanName, new DisposableBeanAdapter(
                    bean, beanName, mbd, getBeanPostProcessorCache().destructionAware));
        }
        else {
            // 自定义 Scope（request / session 等）
            scope.registerDestructionCallback(beanName, new DisposableBeanAdapter(...));
        }
    }
}
```

### 2.1 要不要注册？

| 条件 | 结果 |
|------|------|
| **Prototype** | ❌ 不注册（Spring 不持有唯一实例，不知道何时不用） |
| **Singleton / 自定义 Scope** | 继续看 `requiresDestruction()` |

### 2.2 `requiresDestruction(bean, mbd)` — 是否需要销毁

满足 **任一** 即需要：

1. **有 destroy 方法** — `DisposableBeanAdapter.hasDestroyMethod(bean, mbd)`
2. **有 DestructionAware BPP** — 如 `@PreDestroy`（`InitDestroyAnnotationBeanPostProcessor`）

```java
protected boolean requiresDestruction(Object bean, RootBeanDefinition mbd) {
    return (bean.getClass() != NullBean.class &&
            (DisposableBeanAdapter.hasDestroyMethod(bean, mbd) ||
             DisposableBeanAdapter.hasApplicableProcessors(bean, destructionAwareBPPs)));
}
```

### 2.3 注册到哪里？

| Scope | 注册位置 | 销毁时机 |
|-------|----------|----------|
| **Singleton** | `DefaultSingletonBeanRegistry.disposableBeans` | `destroySingletons()` |
| **Request / Session 等** | `Scope.registerDestructionCallback()` | Scope 结束 |
| **Prototype** | 不注册 | 调用方负责 |

---

## 三、`DisposableBeanAdapter.destroy()` 执行顺序

容器关闭时 **倒序** 遍历 `disposableBeans`，对每个 adapter 调用 `destroy()`：

```text
DisposableBeanAdapter.destroy()
│
├─ 1. DestructionAwareBeanPostProcessor.postProcessBeforeDestruction()
│      └─ @PreDestroy（CommonAnnotationBeanPostProcessor / InitDestroyAnnotationBPP）
│
├─ 2. DisposableBean.destroy()           若实现了 DisposableBean 接口
│
└─ 3. 自定义 destroy-method
       ├─ AutoCloseable.close()
       ├─ ExecutorService.shutdown() / close()
       └─ @Bean(destroyMethod="...") 或 XML destroy-method
```

### destroy 方法推断（`inferDestroyMethodsIfNecessary`）

| 情况 | 推断方法 |
|------|----------|
| 实现 `DisposableBean` | `destroy()` |
| 实现 `AutoCloseable` | `close()` |
| 实现 `ExecutorService` | `shutdown()` 或 `close()` |
| 有 public `close()` / `shutdown()` | 自动推断 |
| `@Bean(destroyMethod = "cleanup")` | 指定方法 |
| `@Bean(destroyMethod = "")` | **禁用**推断（如 DataSource 由容器统一管理） |

---

## 四、什么时候真正执行销毁？

```text
ApplicationContext.close()
  或 registerShutdownHook()（JVM 退出钩子）
    → AbstractApplicationContext.doClose()
      → getBeanFactory().destroySingletons()
        → 倒序遍历 disposableBeans
          → destroySingleton(beanName)
            → DisposableBeanAdapter.destroy()
```

与 `refresh()` 异常回滚的关系（[[18-refresh方法详解]]）：

```text
refresh() 失败 → destroyBeans() → destroySingletons()
```

---

## 五、为什么用 `bean` 而不是 `exposedObject`？

```java
exposedObject = initializeBean(...);   // 可能是 AOP 代理
registerDisposableBeanIfNecessary(beanName, bean, mbd);  // 传 raw bean
return exposedObject;
```

| 原因 | 说明 |
|------|------|
| 资源在 target 上 | 连接池、文件句柄在原始对象里，不在代理壳上 |
| `@PreDestroy` 应作用真实对象 | 对 proxy 调 destroy 可能进不了真实逻辑 |
| 与 AOP 分工一致 | 代理负责拦截；销毁负责释放 target 资源 → [[22-Spring-AOP代理创建详解]] |

---

## 六、为什么需要这个机制？

### 6.1 谁创建，谁清理

IoC 容器通过 `doCreateBean()` **替你创建** Bean 并注入依赖。若 Bean 占用外部资源（JDBC 连接池、线程池、文件句柄、MQ Consumer），容器应在生命周期结束时 **统一释放**。

| 不销毁的后果 | |
|-------------|--|
| 连接未 close | 连接泄漏，DB 连接数耗尽 |
| 线程池未 shutdown | 线程挂住，无法优雅退出 |
| 监听器未注销 | 内存泄漏 |

### 6.2 为什么不能只靠 GC？

GC 只回收 **堆内存**，不会自动：

- 调 `close()` / `shutdown()`
- 执行 `@PreDestroy`
- 通知数据库、Redis、MQ 客户端断开

外部系统需要 **显式** 释放。

### 6.3 为什么创建时登记，而不是关闭时再扫？

| 原因 | 说明 |
|------|------|
| **性能** | 创建时已知要不要 destroy，关闭时只遍历 `disposableBeans` |
| **信息完整** | 此时有 `RootBeanDefinition`、raw bean、适用 BPP |
| **销毁顺序** | 登记后可 **倒序** 销毁，减少依赖方用到已销毁 Bean |

### 6.4 为什么 Prototype 不登记？

每次 `getBean()` 都是新实例，Spring 不持有、也不知道何时不用 → **调用方**负责销毁。

---

## 七、常见误解：「启动时创建，JVM 退出才销毁，何必多此一举？」

### 误解 1：启动时所有 Bean 都已创建

**不一定。**

| 情况 | 何时创建 |
|------|----------|
| 默认 Singleton | 启动时 `preInstantiateSingletons()` 大多会创建 |
| `@Lazy` | 第一次 `getBean()` |
| Prototype | 每次 `getBean()` |
| Request / Session | 请求 / 会话到达时 |

运行期间才创建的 Bean 同样占资源，也需要在 **Scope 结束或 Context 关闭** 时释放。

### 误解 2：销毁只发生在 JVM 退出

**多数情况下，ApplicationContext 比 JVM 先关闭。**

```text
常见场景（JVM 仍在运行）：
├─ Web 热部署 / 应用重启（Tomcat 卸旧 Context）
├─ Spring Boot 优雅停机（SIGTERM → context.close()）
├─ @SpringBootTest 结束 → context.close()
├─ 微服务滚动发布（旧实例下线）
├─ 手动 new ApplicationContext 用完 close()
└─ @DirtiesContext 刷新测试上下文
```

没有销毁机制：旧 Context 的连接池、线程可能 **一直占用**，新 Context 再起 → 泄漏、端口占用。

### 误解 3：JVM 退出会自动清理干净

| 资源 | JVM 直接退出 |
|------|-------------|
| 堆内存 | 进程结束会回收 |
| JDBC 连接 | 不一定通知 DB 正常断开 |
| 线程池 | 可能硬掐，任务丢失 |
| MQ Consumer | 可能没 ack |

**`context.close()` 时主动 `shutdown()`**，才是优雅释放。

### 心智模型

```text
问题：容器「拥有」一批长期存活的 Bean，占着外部资源
时机：「这个 ApplicationContext 不再用了」—— 不一定是 JVM 退出
做法：创建时登记怎么销毁，关闭时倒序执行
目标：可重启、可优雅停机、测试可隔离
```

---

## 八、示例

```java
@Service
public class ConnectionService implements DisposableBean {

    @PreDestroy
    public void cleanup() {
        System.out.println("1. @PreDestroy");
    }

    @Override
    public void destroy() {
        System.out.println("2. DisposableBean.destroy()");
    }
}
```

容器 `close()` 时输出顺序：`@PreDestroy` → `DisposableBean.destroy()`。

```java
@Bean(destroyMethod = "")
public DataSource dataSource() {
    return new HikariDataSource();  // destroyMethod="" 禁用自动 close 推断，由框架统一管理
}
```

---

## 九、与各 Scope 对照

| Scope | `registerDisposableBeanIfNecessary` | 销毁时机 |
|-------|:-----------------------------------:|----------|
| Singleton | ✅ → `disposableBeans` | `context.close()` |
| Prototype | ❌ | 调用方负责 |
| Request / Session | ✅ → Scope callback | Scope 结束 |
| 有 AOP 代理的 Singleton | ✅ 对 **raw bean** 登记 | `context.close()` |

---

## 十、源码调试断点

| 顺序 | 位置 | 看什么 |
|:----:|------|--------|
| 1 | `AbstractAutowireCapableBeanFactory.doCreateBean()` 末尾 | 何时登记 |
| 2 | `AbstractBeanFactory.registerDisposableBeanIfNecessary()` | 是否 Prototype / requiresDestruction |
| 3 | `DisposableBeanAdapter` 构造 | 推断哪些 destroy 方法 |
| 4 | `DefaultSingletonBeanRegistry.destroySingletons()` | 倒序销毁 |
| 5 | `DisposableBeanAdapter.destroy()` | 实际执行顺序 |

配合 [[25-源码调试与断点指南]]、[[12-扩展点层-BeanPostProcessor详解#DestructionAwareBeanPostProcessor]]。

---

## 十一、常见误区速查

| 误区 | 正解 |
|------|------|
| 创建时就会 destroy | 只 **登记**；`close()` 时才执行 |
| 销毁针对 AOP 代理 | 针对 **raw bean** |
| Prototype 也会自动销毁 | **不会**登记 |
| 只有 JVM 退出才需要 destroy | **Context 关闭**就需要 |
| GC 能替代 destroy | 外部资源必须显式释放 |
| 所有 Bean 都要登记 | 只有 `requiresDestruction` 为 true 的才登记 |

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[23-Spring事务实现详解]] | [[25-源码调试与断点指南]] |

---

## 关联

- [[00-Spring-Bean加载-学习导航]]
- [[17-Bean加载原理与源码阅读路径]]
- [[09-容器层-BeanFactory与Registry详解]]
- [[12-扩展点层-BeanPostProcessor详解]]
- [[18-refresh方法详解]]
- [[22-Spring-AOP代理创建详解]]
