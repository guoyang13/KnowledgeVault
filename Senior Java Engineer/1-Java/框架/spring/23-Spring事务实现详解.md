# Spring 事务实现详解

> 导航：[[00-Spring-Bean加载-学习导航]] · **下篇 16–25** · 事务机制
>
> 前置：[[22-Spring-AOP代理创建详解]] · [[12-扩展点层-BeanPostProcessor详解]]
>
> 关联：[[17-Bean加载原理与源码阅读路径]] · [[25-源码调试与断点指南]]
>
> 本地源码：`/Users/guoyang/IdeaProjects/spring/spring-framework`
> - `spring-tx/.../interceptor/TransactionInterceptor.java`
> - `spring-tx/.../interceptor/TransactionAspectSupport.java`
> - `spring-tx/.../support/AbstractPlatformTransactionManager.java`
> - `spring-jdbc/.../DataSourceTransactionManager.java`

---

## 一句话

Spring 声明式事务 = **AOP 代理** + **事务 Advisor** + **PlatformTransactionManager**。`@Transactional` 本身不操作数据库；代理拦截方法调用后，由 `TransactionInterceptor` 开启/提交/回滚事务，底层 `DataSourceTransactionManager` 等负责绑定 JDBC Connection。

---

## 一、整体架构（三层分离）

```text
┌─────────────────────────────────────────────────────────────┐
│  声明式层：@Transactional / TransactionAttributeSource       │
│    解析注解 → propagation / isolation / timeout / rollback   │
├─────────────────────────────────────────────────────────────┤
│  拦截层：TransactionInterceptor（AOP MethodInterceptor）     │
│    invokeWithinTransaction → getTransaction / commit / rollback │
├─────────────────────────────────────────────────────────────┤
│  资源层：PlatformTransactionManager                          │
│    DataSourceTransactionManager / JtaTransactionManager ...  │
│    绑定 Connection、传播行为、TransactionSynchronization      │
└─────────────────────────────────────────────────────────────┘
```

| 层次 | 核心类 | 职责 |
|------|--------|------|
| 声明式 | `@Transactional`、`AnnotationTransactionAttributeSource` | 把注解翻译成 `TransactionAttribute` |
| 拦截 | `TransactionInterceptor`、`TransactionAspectSupport` | 方法调用前后管理事务边界 |
| 资源 | `PlatformTransactionManager`、`AbstractPlatformTransactionManager` | 传播行为、挂起/恢复、commit/rollback |
| 线程绑定 | `TransactionSynchronizationManager` | 当前线程的事务资源（Connection 等） |

**设计模式：** Strategy — `TransactionAttributeSource` 决定「要不要事务、什么属性」；`PlatformTransactionManager` 决定「怎么开/关事务」。

---

## 二、启用事务：`@EnableTransactionManagement`

```text
@EnableTransactionManagement
  → TransactionManagementConfigurationSelector（AdviceModeImportSelector）
       → PROXY 模式：
            ① AutoProxyRegistrar
                 → AopConfigUtils.registerAutoProxyCreatorIfNecessary()
                 → 注册 InfrastructureAdvisorAutoProxyCreator
            ② ProxyTransactionManagementConfiguration
                 → 注册 3 个基础设施 Bean
```

### 注册的基础设施 Bean

| Bean | 类型 | 作用 |
|------|------|------|
| `transactionAttributeSource` | `AnnotationTransactionAttributeSource` | 解析 `@Transactional` |
| `transactionInterceptor` | `TransactionInterceptor` | AOP 拦截器，真正开/关事务 |
| `transactionAdvisor` | `BeanFactoryTransactionAttributeSourceAdvisor` | Advisor = Pointcut + Advice |

`transactionAdvisor` 的 BeanDefinition 角色是 `ROLE_INFRASTRUCTURE`，因此只有 `InfrastructureAdvisorAutoProxyCreator` 会把它当作 Advisor 使用——**不会**误代理普通业务 Advisor Bean。

### 与 `@EnableAspectJAutoProxy` 的区别

| | `@EnableTransactionManagement` | `@EnableAspectJAutoProxy` |
|--|-------------------------------|---------------------------|
| AutoProxyCreator | `InfrastructureAdvisorAutoProxyCreator` | `AnnotationAwareAspectJAutoProxyCreator` |
| Advisor 来源 | 仅 `ROLE_INFRASTRUCTURE` 的 Advisor Bean | 容器中所有 Advisor + `@Aspect` |
| 典型用途 | `@Transactional`、缓存等基础设施 | 自定义切面 |

两者可共存；事务走 Infrastructure 路径，自定义 `@Aspect` 走 AspectJ 路径。

---

## 三、从 `@Transactional` 到 AOP 代理

### 3.1 注解解析

`AnnotationTransactionAttributeSource` 读取 `@Transactional`，产出 `RuleBasedTransactionAttribute`：

- `propagation` — REQUIRED / REQUIRES_NEW / NESTED ...
- `isolation` — DEFAULT / READ_COMMITTED ...
- `timeout`、`readOnly`
- `rollbackFor` / `noRollbackFor`

解析策略（`AbstractFallbackTransactionAttributeSource`）：

```text
1. 方法上的 @Transactional
2. 类上的 @Transactional
3. 接口方法上的 @Transactional（若允许）
```

### 3.2 Advisor 与切点

`BeanFactoryTransactionAttributeSourceAdvisor`：

- **Advice**：`TransactionInterceptor`
- **Pointcut**：`TransactionAttributeSourcePointcut`
  - `matches(method, targetClass)` → `transactionAttributeSource.hasTransactionAttribute(...)`

只有带 `@Transactional`（或等价元数据）的方法才会匹配 → 对应 Bean 才会被代理。

### 3.3 代理创建时机

与 [[22-Spring-AOP代理创建详解#六、代理在 doCreateBean 中的三个时机]] 相同：

```text
InfrastructureAdvisorAutoProxyCreator
  → wrapIfNecessary()
       → getAdvicesAndAdvisorsForBean()
            → 匹配 transactionAdvisor
       → createProxy()
```

**常规路径：** `postProcessAfterInitialization()` → 初始化完成后创建代理。

---

## 四、方法调用时的事务流程

### 4.1 入口：`TransactionInterceptor.invoke()`

```java
public Object invoke(MethodInvocation invocation) throws Throwable {
    Class<?> targetClass = AopUtils.getTargetClass(invocation.getThis());
    return invokeWithinTransaction(method, targetClass, invocation::proceed);
}
```

外部调用 `userService.createOrder()` 实际进入 **代理对象**，再委托给 `TransactionInterceptor`。

### 4.2 核心模板：`invokeWithinTransaction()`

```text
invokeWithinTransaction(method, targetClass, invocation)
│
├─ 1. tas.getTransactionAttribute(method, targetClass)  → txAttr（null = 非事务方法）
├─ 2. determineTransactionManager(txAttr, targetClass) → PlatformTransactionManager
│
├─ 3. createTransactionIfNecessary(ptm, txAttr, joinpointId)
│       └─ ptm.getTransaction(txAttr)  → TransactionStatus
│       └─ txInfo.bindToThread()         → 压入 ThreadLocal 栈
│
├─ 4. try {
│       retVal = invocation.proceed()    → 执行目标方法（及后续拦截器）
│     } catch (ex) {
│       completeTransactionAfterThrowing(txInfo, ex)  → 按 rollback 规则 rollback
│       throw ex
│     } finally {
│       cleanupTransactionInfo(txInfo)   → 弹出 ThreadLocal 栈
│     }
│
└─ 5. commitTransactionAfterReturning(txInfo)  → ptm.commit(status)
```

**关键：** 事务边界包在 `proceed()` 外面——目标方法及其内部调用的其他 `@Transactional` 方法共享/传播同一事务上下文（由传播行为决定）。

### 4.3 回滚规则

`completeTransactionAfterThrowing()`：

```text
if (txAttr.rollbackOn(ex))  → transactionManager.rollback(status)
else                        → 不 rollback（异常被「吞掉」时仍可能 commit）
```

默认：`RuntimeException` 和 `Error` 回滚；受检异常（checked exception）**不回滚**（除非配置 `rollbackFor`）。

Spring 6.2+：`@EnableTransactionManagement(rollbackOn = ALL_EXCEPTIONS)` 可改为所有异常都回滚。

---

## 五、PlatformTransactionManager 与传播行为

### 5.1 接口

```java
public interface PlatformTransactionManager {
    TransactionStatus getTransaction(TransactionDefinition definition);
    void commit(TransactionStatus status);
    void rollback(TransactionStatus status);
}
```

`TransactionDefinition` = `@Transactional` 解析出的属性；`TransactionStatus` = 当前事务状态（是否新事务、是否 rollback-only、savepoint 等）。

### 5.2 `AbstractPlatformTransactionManager.getTransaction()` 决策树

```text
getTransaction(definition)
│
├─ 当前线程已有事务？
│   ├─ PROPAGATION_NEVER      → 抛异常
│   ├─ PROPAGATION_NOT_SUPPORTED → 挂起当前事务，空事务执行
│   ├─ PROPAGATION_REQUIRES_NEW  → 挂起当前，开新事务
│   ├─ PROPAGATION_NESTED     → 在当前事务内建 Savepoint（JDBC）
│   └─ REQUIRED / SUPPORTS / MANDATORY → 加入现有事务（MANDATORY 无事务则报错）
│
└─ 当前无线程事务？
    ├─ PROPAGATION_MANDATORY  → 抛异常
    ├─ REQUIRED / REQUIRES_NEW / NESTED → 创建新事务
    └─ SUPPORTS / NOT_SUPPORTED / NEVER → 空事务（可能仅激活同步）
```

### 5.3 传播行为速查

| 传播行为 | 有现有事务 | 无现有事务 |
|----------|-----------|-----------|
| **REQUIRED**（默认） | 加入 | 新建 |
| **REQUIRES_NEW** | 挂起旧的，新建 | 新建 |
| **NESTED** | Savepoint 嵌套 | 新建 |
| **SUPPORTS** | 加入 | 非事务执行 |
| **NOT_SUPPORTED** | 挂起，非事务执行 | 非事务执行 |
| **MANDATORY** | 加入 | 抛异常 |
| **NEVER** | 抛异常 | 非事务执行 |

### 5.4 commit / rollback

`commit()` 前检查：

- `status.isRollbackOnly()` → 改走 rollback（内层标记 rollback-only，外层 commit 时会 `UnexpectedRollbackException`）
- 触发 `TransactionSynchronization.beforeCommit()` / `afterCommit()`
- 调用子类 `doCommit()`

`rollback()` → `TransactionSynchronization.afterCompletion(STATUS_ROLLED_BACK)` → `doRollback()`

---

## 六、JDBC 事务：`DataSourceTransactionManager`

最常用实现，继承 `AbstractPlatformTransactionManager`：

```text
doBegin()
  → DataSourceUtils.getConnection(dataSource)
  → connection.setAutoCommit(false)
  → TransactionSynchronizationManager.bindResource(dataSource, connectionHolder)

doCommit() / doRollback()
  → connection.commit() / connection.rollback()
  → 释放 Connection 回池

doCleanupAfterCompletion()
  → TransactionSynchronizationManager.unbindResource(dataSource)
```

### Connection 从哪来？

业务代码应通过 `DataSourceUtils.getConnection(dataSource)` 或 `JdbcTemplate` 获取 Connection——它们会从 `TransactionSynchronizationManager` 取**当前线程已绑定**的 Connection，保证同一事务内复用同一连接。

```text
线程 A 调用 @Transactional 方法
  → TM 绑定 Connection 到 ThreadLocal
  → JdbcTemplate 执行 SQL
       → DataSourceUtils.getConnection()
            → 返回 ThreadLocal 里的 Connection（非新连接）
  → 方法结束 commit
```

---

## 七、TransactionSynchronizationManager（线程上下文）

每个线程维护：

| ThreadLocal | 内容 |
|-------------|------|
| `resources` | `Map<key, ConnectionHolder>` — 绑定的 JDBC / Hibernate 等资源 |
| `synchronizations` | 事务完成回调列表 |
| `currentTransactionName` / `currentTransactionReadOnly` / ... | 当前事务元数据 |

由 `AbstractPlatformTransactionManager` 在 `getTransaction()` / `commit()` / `rollback()` 时 `initSynchronization()` / `clearSynchronization()`。

**典型用途：**

- ORM：`SessionFactoryUtils` 绑定 Hibernate Session
- `@TransactionalEventListener(phase = AFTER_COMMIT)` — 事务提交后发事件
- 自定义 `TransactionSynchronization` — beforeCommit / afterCompletion

---

## 八、完整调用链（一次 `@Transactional` 方法调用）

```text
客户端
  → userService.createOrder()          // 代理对象
       → JdkDynamicAopProxy.invoke()
            → ReflectiveMethodInvocation.proceed()
                 → TransactionInterceptor.invoke()
                      → TransactionAspectSupport.invokeWithinTransaction()
                           ├─ AnnotationTransactionAttributeSource.getTransactionAttribute()
                           ├─ DataSourceTransactionManager.getTransaction()
                           │    └─ doBegin() → bind Connection
                           ├─ target.createOrder()    // 原始 Bean 方法
                           │    └─ jdbcTemplate.update() → 同一 Connection
                           └─ DataSourceTransactionManager.commit()
                                └─ doCommit() → unbind Connection
```

与 Bean 生命周期的衔接见 [[22-Spring-AOP代理创建详解]]：代理在 `initializeBean()` 的 `postProcessAfterInitialization` 创建；事务拦截在**运行时**每次方法调用时触发。

---

## 九、编程式事务（对比）

| 方式 | 入口 | 适用 |
|------|------|------|
| 声明式 | `@Transactional` + AOP | 绝大多数业务 |
| `TransactionTemplate` | `transactionTemplate.execute(status -> {...})` | 细粒度控制、非 public 方法 |
| `PlatformTransactionManager` 直接调用 | `getTransaction` / `commit` / `rollback` | 框架内部、底层库 |

声明式本质是 `TransactionInterceptor` 帮你调用了 `getTransaction` / `commit` / `rollback`。

---

## 十、两种 AdviceMode

`@EnableTransactionManagement(mode = ...)`：

| 模式 | 机制 | 说明 |
|------|------|------|
| **PROXY**（默认） | Spring AOP 代理 + `TransactionInterceptor` | 需代理；同类自调用不生效 |
| **ASPECTJ** | AspectJ 编译期/加载期织入 | 可拦截 self-invocation；需 AspectJ 环境 |

---

## 十一、常见失效场景

| 场景 | 原因 |
|------|------|
| 同类 `this.method()` 自调用 | 绕过了代理，拦截器未执行 |
| 方法非 public（默认） | `AnnotationTransactionAttributeSource` 默认只处理 public |
| 异常被 catch 未抛出 | 拦截器认为成功，走 commit |
| 受检异常未配置 `rollbackFor` | 默认不回滚 |
| 未被 Spring 管理（手动 `new`） | 无代理 |
| 未启用 `@EnableTransactionManagement` | 无 Advisor、无 Infrastructure APC |

**解决自调用：** 注入自身代理、`AopContext.currentProxy()`、拆分到另一个 Bean、或 AspectJ 模式。

---

## 十二、源码调试断点

| 顺序 | 类 · 方法 | 看什么 |
|:----:|-----------|--------|
| 1 | `TransactionInterceptor.invoke()` | 是否进入事务拦截 |
| 2 | `TransactionAspectSupport.invokeWithinTransaction()` | txAttr、TM |
| 3 | `AbstractPlatformTransactionManager.getTransaction()` | 传播行为决策 |
| 4 | `DataSourceTransactionManager.doBegin()` | Connection 绑定 |
| 5 | `TransactionAspectSupport.commitTransactionAfterReturning()` | commit 时机 |
| 6 | `TransactionAspectSupport.completeTransactionAfterThrowing()` | rollback 规则 |
| 7 | `InfrastructureAdvisorAutoProxyCreator` + `wrapIfNecessary()` | 代理是否创建 |

配合 [[25-源码调试与断点指南]]、[[22-Spring-AOP代理创建详解#十、源码调试断点推荐]]。

---

## 十三、常见误区

| 误区 | 正解 |
|------|------|
| `@Transactional` 直接操作数据库 | 只是元数据；真正开连接的是 `PlatformTransactionManager` |
| 所有 `@Service` 都有事务 | 只有带 `@Transactional` 且被代理的方法才有 |
| 事务在实例化时开启 | 在**方法调用时**由拦截器开启 |
| `@Transactional` 与 `@EnableAspectJAutoProxy` 是一回事 | 前者用 Infrastructure APC，后者管自定义切面 |
| REQUIRED 和 NESTED 一样 | NESTED 用 Savepoint，内层 rollback 不一定回滚外层 |

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[22-Spring-AOP代理创建详解]] | [[24-Bean销毁机制详解]] |

---

## 关联

- [[00-Spring-Bean加载-学习导航]]
- [[22-Spring-AOP代理创建详解]]
- [[12-扩展点层-BeanPostProcessor详解]]
- [[17-Bean加载原理与源码阅读路径]]
- [[25-源码调试与断点指南]]

## 后续阅读

- [ ] `AbstractPlatformTransactionManager.handleExistingTransaction()` — 传播行为完整分支
- [ ] `DataSourceUtils` — Connection 与 ThreadLocal 绑定细节
- [ ] `@TransactionalEventListener` — 事务事件
- [ ] Spring 6.x 虚拟线程下的事务传播
