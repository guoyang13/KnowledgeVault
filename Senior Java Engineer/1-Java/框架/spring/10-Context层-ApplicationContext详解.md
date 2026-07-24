# ApplicationContext 详解

> 导航：[[00-Spring-Bean加载-学习导航]] · **上篇 01–15** · Context层
>
> 前置：[[09-容器层-BeanFactory与Registry详解]] · [[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
>
> 关联：[[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解#五、与 ApplicationContext 的关系]]
>
> 本地源码：
> - `spring-context/.../context/ApplicationContext.java`
> - `spring-context/.../support/AbstractApplicationContext.java`
> - `spring-context/.../annotation/AnnotationConfigApplicationContext.java`

---

## 一句话

**ApplicationContext** 是 Spring 的**应用级 IoC 容器接口**——在 `BeanFactory` 之上，增加了资源加载、事件、国际化、环境配置等企业级能力。

日常开发里注入的 `ApplicationContext`、`SpringApplication.run()` 返回的容器，底层都是它的实现类。

---

## 与 BeanFactory 的关系

| 概念                   | 回答的问题                                       |
| -------------------- | ------------------------------------------- |
| `BeanFactory`        | **怎么拿 Bean**？（IoC 核心）                       |
| `ApplicationContext` | **怎么运行整个 Spring 应用**？（BeanFactory + 更多基础设施） |

源码注释（`ApplicationContext.java` L27-29）：

```java
/**
 * Central interface to provide configuration for an application.
 * This is read-only while the application is running, but may be
 * reloaded if the implementation supports this.
 */
```

```text
BeanFactory     = 厨房（管 Bean 生命周期）
ApplicationContext = 餐厅经理（管厨房 + 环境 + 广播 + 多语言 + 启动流程 refresh）
```

---

## 接口继承体系

`ApplicationContext` 是一个**组合接口**，把多个能力拼在一起：

```text
ApplicationContext
  ├── ListableBeanFactory      ← 按类型列举 Bean、getBean
  ├── HierarchicalBeanFactory  ← 父子容器
  ├── MessageSource            ← 国际化 i18n
  ├── ApplicationEventPublisher← 事件发布
  ├── ResourcePatternResolver  ← 加载 classpath:/file: 等资源
  └── EnvironmentCapable       ← Profile、PropertySource
```

接口声明（`ApplicationContext.java` L58-59）：

```java
public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory,
        HierarchicalBeanFactory, MessageSource, ApplicationEventPublisher,
        ResourcePatternResolver {
```

**结论**：ApplicationContext **首先是一个 BeanFactory**，照样可以 `getBean()`、`@Autowired`。

---

## 比 BeanFactory 多了什么

| 能力                 | 说明                                                | 典型场景                      |
| ------------------ | ------------------------------------------------- | ------------------------- |
| **启动时预实例化**        | `refresh()` 时创建非 lazy 单例                          | 启动即发现配置错误                 |
| **Environment**    | Profile、`application.properties`                  | `@Profile("dev")`         |
| **ResourceLoader** | 加载 `classpath:`、`file:` 资源                        | 读配置文件、模板                  |
| **事件机制**           | `publishEvent()` / `@EventListener`               | 解耦业务通知                    |
| **国际化**            | `MessageSource`                                   | 多语言消息                     |
| **更多 Aware 回调**    | `ApplicationContextAware`、`ResourceLoaderAware` 等 | Bean 感知容器环境               |
| **AOP 自动代理**       | 通过 BPP 在 refresh 流程中启用                            | `@Transactional`、`@Async` |

### 与 BeanFactory 对比

| | BeanFactory | ApplicationContext |
|--|-------------|-------------------|
| 定位 | IoC 核心 | 企业级容器 |
| 加载策略 | **延迟加载**（lazy，用到才创建） | **启动时**预实例化非 lazy 单例 |
| 功能范围 | Bean 生命周期 | + 事件、国际化、Environment、AOP |
| 继承关系 | 根接口 | 继承 `ListableBeanFactory` + 更多 |
| 典型实现 | `DefaultListableBeanFactory` | `AnnotationConfigApplicationContext` |

> 详细对比见 [[09-容器层-BeanFactory与Registry详解#1.6 BeanFactory vs ApplicationContext]]。

---

## 内部结构：Context 包装 Factory

ApplicationContext **不是替代** `DefaultListableBeanFactory`，而是**包装并编排**它：

```text
AnnotationConfigApplicationContext
    └── 内部持有 DefaultListableBeanFactory
            ├── BeanDefinitionRegistry  → 注册 @Component、@Bean 定义
            └── ConfigurableListableBeanFactory → 创建 / 注入 / 销毁 Bean
```

调用链：

```text
context.getBean("userService")
  → DefaultListableBeanFactory.getBean("userService")
  → AbstractBeanFactory.doGetBean(...)
```

**Bean 的创建逻辑最终都落在 `DefaultListableBeanFactory` 上**；ApplicationContext 负责**启动编排**和**附加能力**。

> 详见 [[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解#五、与 ApplicationContext 的关系]]。

### AnnotationConfigApplicationContext 构造「搭舞台」

无参构造在 `refresh()` 之前完成：

```text
AnnotationConfigApplicationContext()
  ├─ GenericApplicationContext() → new DefaultListableBeanFactory()
  ├─ new AnnotatedBeanDefinitionReader(this)   // 注册 BFPP/BPP + 供 register() 使用
  └─ new ClassPathBeanDefinitionScanner(this)  // 供 scan() / @ComponentScan 使用
```

> Reader / Scanner 构造链路、`register` vs `scan`、@Component 注册三条路径 → [[07-元数据层-AnnotatedBeanDefinitionReader与组件注册详解]]

---

## 典型实现类

| 实现 | 用途 |
|------|------|
| `AnnotationConfigApplicationContext` | 注解驱动（`@Configuration`、`@ComponentScan`） |
| `ClassPathXmlApplicationContext` | XML 配置 |
| `GenericApplicationContext` | 通用基类，可手动注册 BeanDefinition |
| `AnnotationConfigServletWebServerApplicationContext` | Spring Boot Web 应用 |

Spring Boot 启动：

```java
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        // 返回 ConfigurableApplicationContext
        ConfigurableApplicationContext ctx = SpringApplication.run(App.class, args);
    }
}
```

### 两种 Context 风格（注解 vs XML）

| 风格 | 基类 | BeanFactory 创建时机 |
|------|------|---------------------|
| 注解 | `GenericApplicationContext` | 构造时即有 BeanFactory |
| XML | `AbstractRefreshableApplicationContext` | 每次 `refresh()` 重建 BeanFactory |

两者最终都汇入同一个 `refresh()` 流程。详见 [[17-Bean加载原理与源码阅读路径#第 3 步：选一个「定义加载」入口]]。

---

## 核心生命周期：`refresh()`

ApplicationContext 的启动核心是 `AbstractApplicationContext.refresh()`（L588）：

```text
refresh()
  ├── prepareRefresh()                          // 激活容器，初始化属性源
  ├── obtainFreshBeanFactory()                  // 获取/创建 BeanFactory
  ├── prepareBeanFactory(beanFactory)           // ClassLoader、SpEL、环境、scope
  ├── postProcessBeanFactory(beanFactory)       // 子类扩展点
  ├── invokeBeanFactoryPostProcessors()         // 处理 @Configuration、占位符 ← 关键
  ├── registerBeanPostProcessors()              // 注册 BeanPostProcessor
  ├── initMessageSource()                       // 国际化
  ├── initApplicationEventMulticaster()         // 事件广播器
  ├── onRefresh()                               // 子类扩展（如 Spring MVC）
  ├── registerListeners()                       // 注册监听器
  ├── finishBeanFactoryInitialization()         // preInstantiateSingletons() ← 关键
  └── finishRefresh()                           // 发布 ContextRefreshedEvent
```

**最该盯的两个方法：**

| 方法 | 作用 |
|------|------|
| `invokeBeanFactoryPostProcessors()` | 解析 `@Configuration`、`@Bean`、`@Import`，修改 BeanDefinition |
| `finishBeanFactoryInitialization()` | 调用 `preInstantiateSingletons()`，预创建所有非 lazy 单例 |

这就是为什么 Spring Boot 启动完成后，大部分 Bean 已经就绪；而纯 `BeanFactory` 默认是 lazy 的。

> **12 阶段逐步解析（每个子方法中文说明）** → [[18-refresh方法详解]]  
> 完整阅读路径见 [[17-Bean加载原理与源码阅读路径#第 4 步：容器启动总入口 — refresh()]]。

---

## ApplicationContext 独有的 Aware 回调

在 Bean 生命周期中，以下 Aware 回调**仅在 ApplicationContext 环境下**生效（`BeanFactory.java` 类注释）：

| 顺序 | 回调 | 说明 |
|:----:|------|------|
| 6 | `ResourceLoaderAware.setResourceLoader` | 注入 ResourceLoader |
| 7 | `ApplicationEventPublisherAware.setApplicationEventPublisher` | 注入事件发布器 |
| 8 | `MessageSourceAware.setMessageSource` | 注入国际化 MessageSource |
| 9 | `ApplicationContextAware.setApplicationContext` | 注入 ApplicationContext |
| 10 | `ServletContextAware.setServletContext` | 注入 ServletContext（**仅 Web**） |

> 完整 14 步初始化顺序见 [[09-容器层-BeanFactory与Registry详解#1.5 Bean 生命周期（BeanFactory 定义的标准顺序）]]。

---

## 日常开发中的接触方式

```java
// 1. 直接注入
@Autowired
private ApplicationContext context;

// 2. Bean 实现 ApplicationContextAware
@Component
public class MyBean implements ApplicationContextAware {
    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        // 容器回调注入
    }
}

// 3. 发布事件
context.publishEvent(new OrderCreatedEvent(order));

// 4. 按 Profile / Environment 读配置
String port = context.getEnvironment().getProperty("server.port");

// 5. 加载资源
Resource resource = context.getResource("classpath:config.xml");
```

---

## 与 Web 的关系

Web 场景下还有 `WebApplicationContext`，额外提供：

- `ServletContext` 访问
- Web 作用域（`request`、`session`）
- **父子容器**：Root Context（Service 层）+ Servlet Context（Controller 层）

```text
Root WebApplicationContext（父）
  └── 共享 Service、Repository 等 Bean

Servlet WebApplicationContext（子）
  └── Controller、Web 相关 Bean
  └── 找不到 Bean 时向父容器查找
```

---

## 概念速查

| 概念 | 一句话 | 源码位置 |
|------|--------|---------|
| `ApplicationContext` | 应用级 IoC 容器接口 | `context/ApplicationContext.java` |
| `ConfigurableApplicationContext` | 可配置/可刷新的 Context | `context/ConfigurableApplicationContext.java` |
| `AbstractApplicationContext` | `refresh()` 编排模板 | `support/AbstractApplicationContext.java` |
| `GenericApplicationContext` | 通用实现，持有 DLBF | `support/GenericApplicationContext.java` |
| `AnnotationConfigApplicationContext` | 注解驱动入口 | `annotation/AnnotationConfigApplicationContext.java` |
| `refresh()` | 容器启动总入口 | `AbstractApplicationContext` L588 |
| `finishBeanFactoryInitialization()` | 预实例化非 lazy 单例 | `AbstractApplicationContext` L939 |

---

## 常见面试题速答

| 问题 | 答案 |
|------|------|
| ApplicationContext 和 BeanFactory 区别？ | Context 是 BeanFactory 的超集，多了事件、国际化、Environment、启动预实例化等 |
| `getBean()` 最终谁干活？ | 内部的 `DefaultListableBeanFactory` |
| Spring Boot 启动做了什么？ | 创建 ApplicationContext → 调用 `refresh()` → 注册定义 → 预实例化单例 |
| 为什么启动慢？ | `refresh()` 要扫描、解析配置、执行 BFPP/BPP、预创建非 lazy 单例 |
| 父子容器怎么查 Bean？ | 子容器先查本地，找不到再查父容器（`getBean` 会向上委托） |

---

## 记忆口诀

```text
BeanFactory        = 管 Bean（拿对象）
ApplicationContext = 管应用（拿对象 + 环境 + 事件 + 资源 + 启动流程）

getBean 走 Factory，refresh 走 Context。
```

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[09-容器层-BeanFactory与Registry详解]] | [[11-扩展点层-BeanFactoryPostProcessor详解]] |

> 上篇继续：[[11-扩展点层-BeanFactoryPostProcessor详解]] → [[12-扩展点层-BeanPostProcessor详解]] → [[13-生命周期层-Aware体系详解]]；下篇机制：[[16-IoC与DI核心概念]] → [[17-Bean加载原理与源码阅读路径]]

---

## 关联

- [[00-Spring-Bean加载-学习导航]]
- [[17-Bean加载原理与源码阅读路径]]
- [[09-容器层-BeanFactory与Registry详解]]
- [[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
- [[03-速查-IoC与DI核心整合速查]]
- [[05-接口地图-IoC与DI重要接口大全]]
- [[25-源码调试与断点指南]]
- [[07-元数据层-AnnotatedBeanDefinitionReader与组件注册详解]]
- [[04-速查-Spring厨房比喻大全]]

---
## 下一步可深入

- [ ] `ConfigurableApplicationContext` vs `ApplicationContext` 区别
- [x] `GenericApplicationContext` 与 `AnnotationConfigApplicationContext` 构造差异 → [[07-元数据层-AnnotatedBeanDefinitionReader与组件注册详解]]
- [ ] Web 父子容器 — `ContextLoaderListener` + `DispatcherServlet`
- [ ] `ContextRefreshedEvent` 与 `@EventListener` 源码
- [ ] Spring Boot `SpringApplication.run()` 如何选择 Context 实现类
