# Aware 体系详解

> 导航：[[00-Spring-Bean加载-学习导航]] · **上篇 01–15** · 生命周期层 · Aware 接口体系
>
> 前置：[[05-接口地图-IoC与DI重要接口大全#四、Aware 接口族 — 让 Bean 感知容器 ★]] · [[12-扩展点层-BeanPostProcessor详解]] · [[10-Context层-ApplicationContext详解]]
>
> 关联：[[19-IoC扩展点三部曲对照]] · [[20-依赖注入实现原理]]（Aware vs DI）
>
> 本地源码：`/Users/guoyang/IdeaProjects/spring/spring-framework`

---

## 一句话

`Aware` 是 Spring 的**标记接口体系**——Bean 实现某个 `XxxAware` 子接口，声明「我需要容器里的某个基础设施对象」，容器在特定时机**回调**注入。它与 `@Autowired` 的 DI 不同：Aware 是容器推给 Bean 的框架级回调，不是业务依赖注入。

> 速查版见 [[05-接口地图-IoC与DI重要接口大全#四、Aware 接口族 — 让 Bean 感知容器 ★]]；本篇是完整深入版。

---

## 一、设计思想

### 1.1 Aware 是什么

```java
/**
 * A marker superinterface indicating that a bean is eligible to be notified
 * by the Spring container of a particular framework object through a
 * callback-style method.
 *
 * Note that merely implementing Aware provides no default functionality.
 * Rather, processing must be done explicitly, for example in a BeanPostProcessor.
 */
public interface Aware { }  // 标记接口，本身没有任何方法
```

**关键理解：**

- `Aware` 只是**标记**，实现它本身不会自动注入任何东西
- 每个子接口定义一个 `setXxx(...)` 回调方法
- 容器必须有对应的**处理器**检测并调用，Aware 才生效
- 这是 **Push 式回调**（容器推给你），不是 Bean 主动 `getBean()` 拉取

### 1.2 Aware vs DI

| | Aware 回调 | 构造器 / `@Autowired` 注入 |
|--|-----------|---------------------------|
| **本质** | 容器基础设施回调 | 业务依赖注入 |
| **方向** | 容器 Push 给 Bean | 容器 Push 依赖给 Bean |
| **耦合** | 与 Spring API 强耦合 | 相对解耦（面向接口） |
| **测试** | 需模拟回调 | 直接 new + 传参，更易测 |
| **典型用途** | 获取 BeanFactory、Context、Environment | Service、Repository 等业务依赖 |
| **推荐度** | 框架扩展、基础设施场景 | **日常业务首选** ★ |

Spring 官方态度：`BeanFactoryAware` 的 javadoc 明确说——大多数 Bean 应通过构造器/属性注入协作 Bean，Aware 只用于需要容器基础设施的场景。

→ DI 底层见 [[20-依赖注入实现原理]]

---

## 二、在 Bean 生命周期中的位置

Aware 回调发生在 `initializeBean()` 内，**属性已填充完毕**，但在 `@PostConstruct` 和 BPP `AfterInitialization` **之前**：

```text
doCreateBean()
  ├── createBeanInstance()          new 对象
  ├── populateBean()                @Autowired 注入完成
  └── initializeBean()
        ├── invokeAwareMethods()              ← BeanFactory 级 Aware ★
        ├── BPP.postProcessBeforeInitialization()
        │     ├── ApplicationContextAwareProcessor  ← ApplicationContext 级 Aware ★
        │     ├── ServletContextAwareProcessor      ← Web 级 Aware
        │     └── LoadTimeWeaverAwareProcessor
        ├── invokeInitMethods()               @PostConstruct / init-method
        └── BPP.postProcessAfterInitialization()  AOP 代理
```

**顺序记忆：**

```text
实例化 → 属性注入(DI) → Aware回调 → BPP BeforeInit → @PostConstruct → BPP AfterInit
```

详见 [[12-扩展点层-BeanPostProcessor详解#三、在 Bean 生命周期中的位置]]、[[17-Bean加载原理与源码阅读路径]]。

---

## 三、两套处理机制

Aware 不是统一由一个类处理，而是分**两层**：

### 3.1 第一层：BeanFactory 级 — `invokeAwareMethods()`

**位置**：`AbstractAutowireCapableBeanFactory.initializeBean()` 最开头（L1812）  
**特点**：不经过 BPP，BeanFactory 直接检测并回调

```java
private void invokeAwareMethods(String beanName, Object bean) {
    if (bean instanceof Aware) {
        if (bean instanceof BeanNameAware)
            → setBeanName(beanName)
        if (bean instanceof BeanClassLoaderAware)
            → setBeanClassLoader(getBeanClassLoader())
        if (bean instanceof BeanFactoryAware)
            → setBeanFactory(this)
    }
}
```

| 接口 | 回调方法 | 注入内容 | 模块 |
|------|----------|----------|------|
| `BeanNameAware` | `setBeanName(String)` | 当前 Bean 在容器中的名称 | spring-beans |
| `BeanClassLoaderAware` | `setBeanClassLoader(ClassLoader)` | 容器 ClassLoader | spring-beans |
| `BeanFactoryAware` | `setBeanFactory(BeanFactory)` | 所属 BeanFactory | spring-beans |

> 纯 `BeanFactory` 环境（无 ApplicationContext）只有这一层 Aware。

### 3.2 第二层：ApplicationContext 级 — BPP 处理

**处理器**：`ApplicationContextAwareProcessor`（`prepareBeanFactory()` 中注册）  
**位置**：`postProcessBeforeInitialization()` 内（L83）

```java
public Object postProcessBeforeInitialization(Object bean, String beanName) {
    if (bean instanceof Aware) {
        invokeAwareInterfaces(bean);  // 按固定顺序
    }
    return bean;
}
```

**回调顺序**（源码注释明确写了顺序）：

| 顺序 | 接口 | 回调方法 | 注入内容 |
|:----:|------|----------|----------|
| 1 | `EnvironmentAware` | `setEnvironment()` | Environment（Profile、PropertySource） |
| 2 | `EmbeddedValueResolverAware` | `setEmbeddedValueResolver()` | `${...}` 字符串解析器 |
| 3 | `ResourceLoaderAware` | `setResourceLoader()` | ResourceLoader（Context 本身） |
| 4 | `ApplicationEventPublisherAware` | `setApplicationEventPublisher()` | 事件发布器 |
| 5 | `MessageSourceAware` | `setMessageSource()` | 国际化 MessageSource |
| 6 | `ApplicationStartupAware` | `setApplicationStartup()` | 启动性能度量 |
| 7 | `ApplicationContextAware` | `setApplicationContext()` | ApplicationContext 本身 |

### 3.3 其他模块扩展的 Aware 处理器

| Aware 接口 | 处理器 | 注册时机 | 模块 |
|-----------|--------|----------|------|
| `ServletContextAware` | `ServletContextAwareProcessor` | Web ApplicationContext 启动 | spring-web |
| `ServletConfigAware` | `ServletContextAwareProcessor` | 同上 | spring-web |
| `LoadTimeWeaverAware` | `LoadTimeWeaverAwareProcessor` | 存在 loadTimeWeaver Bean 时 | spring-context |
| `ImportAware` | `ImportAwareBeanPostProcessor` | ConfigurationClassPostProcessor 注册 | spring-context |
| `NotificationPublisherAware` | JMX 导出器处理 | JMX 启用时 | spring-context |
| `SchedulerContextAware` | Quartz 集成 | 使用 Quartz 时 | spring-context-support |

### 3.4 源码逐行：`EnvironmentAware` 回调

`ApplicationContextAwareProcessor` 里典型的一段：

```java
if (bean instanceof EnvironmentAware environmentAware) {
    environmentAware.setEnvironment(this.applicationContext.getEnvironment());
}
```

| 片段 | 含义 |
|------|------|
| `bean` | 当前正在 `initializeBean()` 的 Bean 实例 |
| `instanceof EnvironmentAware environmentAware` | Java 16+ 模式匹配：判断是否实现 `EnvironmentAware`，并绑定到变量 |
| `setEnvironment(...)` | 调用 Bean 的回调，把 Environment 塞进去 |
| `applicationContext.getEnvironment()` | 从容器取 Environment（配置、Profile、PropertySource） |

等价老写法：

```java
if (bean instanceof EnvironmentAware) {
    ((EnvironmentAware) bean).setEnvironment(this.applicationContext.getEnvironment());
}
```

**时序**：在 `@PostConstruct` **之前**执行；若 Bean 同时有 `@Autowired Environment`，那是 `populateBean` 阶段的 DI，与 Aware 是两条通道（见 [[#五、ignoreDependencyInterface 机制]]）。

### 3.5 Demo：Aware 由 BPP 处理

**角色区分：**

| 角色 | 例子 | 关系 |
|------|------|------|
| **Aware 处理器** | `ApplicationContextAwareProcessor` | BPP，给别的 Bean 调 `setXxx()` |
| **实现 Aware 的 Bean** | `FrameworkServlet`、`AutowiredAnnotationBeanPostProcessor` | 自己也要被容器回调 |

**Demo 1 — 你只写接口，Spring 内置 BPP 兑现：**

```java
@Component
public class MyService implements ApplicationContextAware, EnvironmentAware {

    private ApplicationContext ctx;

    @Override
    public void setEnvironment(Environment environment) { /* 先于 Context 回调 */ }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.ctx = applicationContext;
    }

    @PostConstruct
    public void init() {
        // 此处 ctx 已注入
    }
}
```

**Demo 2 — 手写迷你 BPP（理解 `ApplicationContextAwareProcessor` 同款模式）：**

```java
@Component
public class MyAwareProcessor implements BeanPostProcessor {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof ApplicationContextAware aware) {
            aware.setApplicationContext(applicationContext);
        }
        return bean;
    }
}
```

生产环境不要与 `ApplicationContextAwareProcessor` 重复注册；Spring 已在 `prepareBeanFactory()` 中内置。

**对比 `@Autowired`：**

| | `EnvironmentAware` / `ApplicationContextAware` | `@Autowired Environment` / `ApplicationContext` |
|--|------------------------------------------------|------------------------------------------------|
| 阶段 | `initializeBean` → BPP BeforeInit | `populateBean` |
| 机制 | Aware 专用回调 | DI（`AutowiredAnnotationBeanPostProcessor` 也是 BPP） |
| 业务代码 | 不推荐 | **推荐** ★ |

---

## 四、完整 Aware 接口清单

### 4.1 spring-beans 层（BeanFactory 级）

| 接口 | 注入什么 | 典型用途 |
|------|---------|----------|
| `BeanNameAware` | Bean 名称 | 日志、监控、动态注册 |
| `BeanClassLoaderAware` | ClassLoader | 自定义类加载、AOP 织入 |
| `BeanFactoryAware` | BeanFactory | 编程式 `getBean()`、框架扩展 |

### 4.2 spring-context 层（ApplicationContext 级）

| 接口 | 注入什么 | 典型用途 |
|------|---------|----------|
| `EnvironmentAware` | Environment | 读配置、判断 Profile |
| `EmbeddedValueResolverAware` | StringValueResolver | 手动解析 `${...}` 字符串 |
| `ResourceLoaderAware` | ResourceLoader | `getResource("classpath:...")` |
| `ApplicationEventPublisherAware` | 事件发布器 | 发布 ApplicationEvent |
| `MessageSourceAware` | MessageSource | 国际化 `getMessage()` |
| `ApplicationStartupAware` | ApplicationStartup | 启动性能追踪 |
| `ApplicationContextAware` | ApplicationContext | 获取容器本身（最常用） |
| `ImportAware` | 导入方的 AnnotationMetadata | `@Import` 组合配置 |
| `LoadTimeWeaverAware` | LoadTimeWeaver | 类加载时 AOP 织入 |
| `NotificationPublisherAware` | NotificationPublisher | JMX 通知发布 |

### 4.3 spring-web 层

| 接口 | 注入什么 | 典型用途 |
|------|---------|----------|
| `ServletContextAware` | ServletContext | 访问 Servlet 上下文 |
| `ServletConfigAware` | ServletConfig | 访问 Servlet 配置 |

---

## 五、ignoreDependencyInterface 机制

`prepareBeanFactory()` 会把 Aware 接口标记为**不参与自动注入**：

```java
beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
// ... 其他 Aware 接口
```

**为什么要这样？**

- 避免 `@Autowired Environment` 与 `EnvironmentAware.setEnvironment()` **重复注入/冲突**
- Aware 走专用回调通道，普通 `@Autowired` 不会尝试注入 Aware 接口类型
- 但 `registerResolvableDependency()` 注册了部分类型供 `@Autowired` 使用：

```java
beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
beanFactory.registerResolvableDependency(ResourceLoader.class, this);
beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
beanFactory.registerResolvableDependency(ApplicationContext.class, this);
```

所以实际上 **`@Autowired ApplicationContext` 可以工作**，不一定非用 `ApplicationContextAware`。

详见 [[10-Context层-ApplicationContext详解]] 的 `prepareBeanFactory()`。

---

## 六、架构中实现 Aware 的代表性 Bean

Spring 源码里**大量框架 Bean 实现了 Aware**，业务代码却很少用。原因统一：**框架组件需要容器基础设施，且很多 BPP 自身不能靠普通 `@Autowired` 先拿到 BeanFactory（鸡生蛋）**。

> 以下按 IoC 学习优先级排列；非穷举，覆盖注解驱动场景最常碰到的类。

### 6.1 两类角色（复习）

```text
ApplicationContextAwareProcessor (BPP)
    └── 回调其他 Bean 的 setEnvironment / setApplicationContext ...

AutowiredAnnotationBeanPostProcessor (Bean)
    └── implements BeanFactoryAware
    └── 自己被 invokeAwareMethods() 回调 setBeanFactory
    └── 之后才能给别人做 @Autowired
```

### 6.2 spring-beans — IoC / DI 核心

| Bean | 实现的 Aware | 为什么需要 |
|------|-------------|-----------|
| `AutowiredAnnotationBeanPostProcessor` | `BeanFactoryAware` | 执行 `@Autowired` 必须调用 `resolveDependency()`，先要 `ConfigurableListableBeanFactory` |
| `CommonAnnotationBeanPostProcessor` | `BeanFactoryAware` | 处理 `@Resource`、`@PostConstruct`、`@PreDestroy`，要按名/类型查 Bean |
| `PlaceholderConfigurerSupport` | `BeanNameAware`、`BeanFactoryAware` | 解析 `${...}` 占位符 |
| `PropertySourcesPlaceholderConfigurer` | 同上 + `EnvironmentAware` | 读 Environment 中的配置值 |
| `AbstractFactoryBean` | `BeanClassLoaderAware`、`BeanFactoryAware` | FactoryBean 创建产品时要加载类、可能 `getBean()` |

`AutowiredAnnotationBeanPostProcessor.setBeanFactory()` 源码要点：

```java
@Override
public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    this.metadataReaderFactory = new SimpleMetadataReaderFactory(clbf.getBeanClassLoader());
}
```

→ 详见 [[20-依赖注入实现原理]] · [[12-扩展点层-BeanPostProcessor详解]]

### 6.3 spring-aop — 代理创建

| Bean | 实现的 Aware | 为什么需要 |
|------|-------------|-----------|
| `AbstractAutoProxyCreator` | `BeanFactoryAware` | 创建代理时要从容器找 Advisor、TargetSource |
| `AbstractBeanFactoryAwareAdvisingPostProcessor` | `BeanFactoryAware` | 基于 BeanFactory 的自动 advisor 发现 |
| `DefaultAdvisorAutoProxyCreator` | `BeanNameAware` | 需要知道自己的 Bean 名 |

```java
// AbstractAutoProxyCreator
@Override
public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
}
```

→ 详见 [[22-Spring-AOP代理创建详解]]

### 6.4 spring-context — 容器与扩展

| Bean | 实现的 Aware | 为什么需要 |
|------|-------------|-----------|
| `ApplicationObjectSupport` | `ApplicationContextAware` | 框架基类，子类通过 `getApplicationContext()` 访问容器 |
| `DefaultLifecycleProcessor` | `BeanFactoryAware` | 管理 `Lifecycle` Bean 启停，要遍历容器 |
| `AbstractApplicationEventMulticaster` | `BeanClassLoaderAware`、`BeanFactoryAware` | 事件广播：加载 Listener 类、取 Listener Bean |
| `ConfigurationClassPostProcessor.ImportAwareBeanPostProcessor` | （BPP，处理别人的 `ImportAware`） | `@Import` 配置类读取导入方元数据 |
| `MBeanExportConfiguration` | `ImportAware`、`EnvironmentAware`、`BeanFactoryAware` | JMX 导出组合配置 |
| `ExecutorConfigurationSupport` | `BeanNameAware`、`ApplicationContextAware` | 线程池：日志用 Bean 名、关闭时访问 Context |
| `CacheAspectSupport` | `BeanFactoryAware` | 缓存切面可能依赖容器 Bean |

### 6.5 spring-web / spring-webmvc — Web 栈

| Bean | 实现的 Aware | 为什么需要 |
|------|-------------|-----------|
| `FrameworkServlet` | `ApplicationContextAware` | DispatcherServlet 父类，启动时拿 `WebApplicationContext` |
| `HttpServletBean` | `EnvironmentAware` | Servlet Bean 读配置 |
| `WebMvcConfigurationSupport` | `ApplicationContextAware`、`ServletContextAware` | MVC 自动配置核心 |
| `RequestMappingHandlerAdapter` | `BeanFactoryAware` | 执行 `@RequestMapping`、解析参数、找 `HandlerMethodArgumentResolver` |
| `ExceptionHandlerExceptionResolver` | `ApplicationContextAware` | 全局异常处理 |
| `GenericFilterBean` | `BeanNameAware`、`EnvironmentAware` | Filter 也是 Bean，要名字和 Environment |
| `ResourceHttpRequestHandler` | `EmbeddedValueResolverAware` | 静态资源路径解析 `${...}` |

```text
FrameworkServlet implements ApplicationContextAware
    → ApplicationContextAwareProcessor 回调 setApplicationContext
    → initWebApplicationContext() 驱动整个 MVC
```

→ 详见 [[10-Context层-ApplicationContext详解]] · [[18-refresh方法详解]]

### 6.6 spring-tx — 事务

| Bean | 实现的 Aware | 为什么需要 |
|------|-------------|-----------|
| `TransactionAspectSupport` | `BeanFactoryAware` | 事务拦截中可能 `getBean()` |
| `AbstractTransactionManagementConfiguration` | `ImportAware` | `@EnableTransactionManagement` 导入配置 |

→ 详见 [[23-Spring事务实现详解]]

### 6.7 IoC 学习优先记忆表 ★

| Bean | Aware | 一句话 |
|------|-------|--------|
| `AutowiredAnnotationBeanPostProcessor` | `BeanFactoryAware` | 给别人 DI，自己先要工厂 |
| `AbstractAutoProxyCreator` | `BeanFactoryAware` | 做代理时要查 Advisor |
| `PropertySourcesPlaceholderConfigurer` | `EnvironmentAware` | 解析 `${}` |
| `FrameworkServlet` | `ApplicationContextAware` | Servlet 拿 Web 容器 |
| `RequestMappingHandlerAdapter` | `BeanFactoryAware` | MVC 执行器访问容器 |

### 6.8 为什么框架 Bean 用 Aware，业务 Bean 不用？

| 框架 Bean | 业务 Bean |
|-----------|-----------|
| 自身是 BPP / Servlet / FactoryBean 等基础设施 | 依赖 Service、Repository |
| 创建早，有时不能先走普通 DI | 构造器 / `@Autowired` 即可 |
| 要拿 BeanFactory、Context、ClassLoader | 要拿业务接口实现 |
| 与 Spring API 耦合可接受 | 应面向业务接口，便于测试 |

**结论**：Aware 在架构里是**框架内向容器要基础设施**的标准方式；业务代码仍优先构造器注入。

---

## 七、典型 Aware 接口深入

### 7.1 BeanFactoryAware — 感知 BeanFactory

```java
public interface BeanFactoryAware extends Aware {
    void setBeanFactory(BeanFactory beanFactory) throws BeansException;
}
```

- 回调时机：`invokeAwareMethods()`，**最早**的 Aware
- 用途：编程式 `beanFactory.getBean()`、框架级扩展
- 注意：大多数业务代码不应依赖它，应使用 DI

### 7.2 ApplicationContextAware — 感知 ApplicationContext

```java
public interface ApplicationContextAware extends Aware {
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException;
}
```

- 回调时机：`ApplicationContextAwareProcessor`，顺序**最后**（第 7 个）
- 用途：获取容器、发布事件、加载资源
- 便捷基类：`ApplicationObjectSupport` 已实现此接口并提供 `getApplicationContext()`、`getMessage()` 等

```java
// 许多框架类继承此基类
public abstract class ApplicationObjectSupport implements ApplicationContextAware {
    // 保存 context 引用，提供 getApplicationContext() 等便捷方法
}
```

### 7.3 EnvironmentAware — 感知 Environment

```java
@Component
public class MyService implements EnvironmentAware {
    private Environment env;

    @Override
    public void setEnvironment(Environment environment) {
        this.env = environment;
    }

    public void doWork() {
        if (env.acceptsProfiles("prod")) { ... }
        String url = env.getProperty("app.url");
    }
}
```

现代替代写法：`@Autowired Environment env` 或 `@Value("${app.url}")` —— 更简洁。

### 7.4 ImportAware — 感知 @Import 来源

特殊 Aware，由 `ConfigurationClassPostProcessor` 注册的 `ImportAwareBeanPostProcessor` 处理：

```java
public interface ImportAware extends Aware {
    void setImportMetadata(AnnotationMetadata importMetadata);
}
```

用途：被 `@Import` 导入的配置类，可以知道「是谁导入了我」，读取导入方的注解元数据。详见 [[11-扩展点层-BeanFactoryPostProcessor详解#八、ConfigurationClassPostProcessor 深入]]。

---

## 八、自定义 Aware 模式

Spring 自身扩展 Aware 的标准模式：**定义 Aware 接口 + 编写 BPP 兑现回调**。

```java
// 1. 定义 Aware 接口
public interface TraceIdAware extends Aware {
    void setTraceIdGenerator(TraceIdGenerator generator);
}

// 2. 编写 BPP 处理器
@Component
public class TraceIdAwareProcessor implements BeanPostProcessor {
    @Autowired
    private TraceIdGenerator generator;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof TraceIdAware aware) {
            aware.setTraceIdGenerator(generator);
        }
        return bean;
    }
}

// 3. Bean 实现 Aware
@Component
public class MyService implements TraceIdAware {
    private TraceIdGenerator generator;

    @Override
    public void setTraceIdGenerator(TraceIdGenerator generator) {
        this.generator = generator;
    }
}
```

这就是 `ApplicationContextAwareProcessor` 的同款模式。详见 [[12-扩展点层-BeanPostProcessor详解#十三、自定义 BPP]]。

---

## 九、Aware 与循环依赖

源码注释（`doCreateBean()` L591-592）：

> Eagerly cache singletons to be able to resolve circular references  
> even when triggered by lifecycle interfaces like **BeanFactoryAware**.

三级缓存暴露早期引用的原因之一就是：Aware 回调（如 `BeanFactoryAware.setBeanFactory()`）可能触发其他 Bean 的创建，从而引发循环依赖。

---

## 十、怎么选：Aware vs @Autowired

| 场景 | 推荐方式 |
|------|----------|
| 注入 Service / Repository | 构造器注入 ★ |
| 读配置 | `@Value` 或 `@Autowired Environment` |
| 发布事件 | `@Autowired ApplicationEventPublisher` |
| 加载 classpath 资源 | `@Autowired Resource` 或 `@Value("classpath:...")` |
| 框架扩展，需要 BeanFactory | `BeanFactoryAware` |
| 继承 Spring 框架基类 | `ApplicationObjectSupport`（已实现 Aware） |
| 自定义容器基础设施回调 | 自定义 Aware + BPP |

---

## 十一、常见误区

| 误区 | 正解 |
|------|------|
| 实现 `Aware` 就自动注入 | 必须有对应 BPP 或 `invokeAwareMethods()` 处理 |
| `@PostConstruct` 在 Aware 之前 | Aware → BPP BeforeInit → `@PostConstruct` |
| `ApplicationContextAware` 和 `@Autowired ApplicationContext` 完全不同 | 效果类似；`@Autowired` 走 DI，Aware 走回调 |
| 所有 Aware 都由 `invokeAwareMethods()` 处理 | 只有 BeanFactory 级 3 个；其余走 BPP |
| Aware 适合注入业务依赖 | 只适合容器基础设施；业务依赖用构造器注入 |
| 实现多个 Aware 接口顺序任意 | ApplicationContext 级有固定顺序（Environment 最先，Context 最后） |

---

## 十二、与 BFPP / BPP 的关系

```text
BFPP 阶段（改定义）
  → 不涉及 Aware

BPP 阶段（改实例）
  → populateBean()         @Autowired 注入
  → invokeAwareMethods()   BeanFactory 级 Aware（非 BPP，但在 initializeBean 内）
  → BPP BeforeInit         ApplicationContext 级 Aware（ApplicationContextAwareProcessor）
  → @PostConstruct
  → BPP AfterInit          AOP 代理
```

Aware 不是独立的第三种扩展点——它是 **BPP 机制的一种应用模式**（BeanFactory 级 3 个除外，由工厂直接处理）。

| 扩展点 | 操作对象 | 详见 |
|--------|----------|------|
| BFPP | BeanDefinition | [[11-扩展点层-BeanFactoryPostProcessor详解]] |
| BPP | Bean 实例 | [[12-扩展点层-BeanPostProcessor详解]] |
| Aware | Bean 实例（基础设施回调） | 本篇 |

---

## 十三、源码阅读顺序

| 步骤 | 文件 | 关注点 |
|:----:|------|--------|
| 1 | `Aware.java` | 标记接口设计 |
| 2 | `BeanFactoryAware.java` 等子接口 | 各 Aware 契约 |
| 3 | `AbstractAutowireCapableBeanFactory.invokeAwareMethods()` | BeanFactory 级处理 |
| 4 | `ApplicationContextAwareProcessor.java` | ApplicationContext 级处理 ★ |
| 5 | `AbstractApplicationContext.prepareBeanFactory()` | ignoreDependencyInterface |
| 6 | `ServletContextAwareProcessor.java` | Web 级 Aware |
| 7 | `ConfigurationClassPostProcessor.ImportAwareBeanPostProcessor` | ImportAware 特殊处理 |
| 9 | `AutowiredAnnotationBeanPostProcessor.setBeanFactory()` | 框架 Bean 为何也要 Aware ★ |
| 10 | `AbstractAutoProxyCreator.setBeanFactory()` | AOP 与 Aware |
| 11 | 对比 [[12-扩展点层-BeanPostProcessor详解]] | Aware 是 BPP BeforeInit 的典型应用 |

建议在 `invokeAwareMethods()` 和 `ApplicationContextAwareProcessor.postProcessBeforeInitialization()` 上打断点，配合 [[25-源码调试与断点指南]] 跟栈。

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[12-扩展点层-BeanPostProcessor详解]] | [[14-工厂Bean-BeanFactory与FactoryBean的区别]] |

---

## 关联

- [[00-Spring-Bean加载-学习导航]]
- [[17-Bean加载原理与源码阅读路径]]
- [[09-容器层-BeanFactory与Registry详解]]
- [[10-Context层-ApplicationContext详解]]
- [[05-接口地图-IoC与DI重要接口大全]]
- [[20-依赖注入实现原理]]
- [[12-扩展点层-BeanPostProcessor详解]]
- [[11-扩展点层-BeanFactoryPostProcessor详解]]
- [[25-源码调试与断点指南]]
- [[04-速查-Spring厨房比喻大全]]
- [[19-IoC扩展点三部曲对照]]

---
## 下一步可深入

- [ ] 循环依赖 — Aware 回调如何触发三级缓存
- [ ] `ApplicationObjectSupport` — 框架基类的 Aware 封装（见 [[#7.2 ApplicationContextAware — 感知 ApplicationContext]]）
- [ ] 架构中实现 Aware 的 Bean — `AutowiredAnnotationBeanPostProcessor`、`AbstractAutoProxyCreator`（见 [[#六、架构中实现 Aware 的代表性 Bean]]）
- [ ] BFPP + BPP + Aware 三合一对照复习
