---
type: canonical
status: reviewed
topic: Spring BeanFactory
source_version: 6.2.x
---

# BeanFactory 接口体系详解

> 导航：[[00-Spring-Framework核心机制-学习导航]] · **20 · 结构与元数据** · 容器层
>
> 定位：聚焦 **BeanFactory 接口体系**（继承树、`doGetBean`、ObjectProvider、生命周期、vs ApplicationContext）；`BeanDefinitionRegistry` / `DefaultListableBeanFactory` 结构详见 [[3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]。
>
> 前置：[[3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]] · [[4-接口地图-IoC与DI重要接口大全]]
>
> 关联：[[5-Context层-ApplicationContext详解]] · [[5-工厂Bean-BeanFactory与FactoryBean的区别]] · [[2-速查-IoC与DI核心整合速查]]
>
> Registry / DLBF 源码级阅读 → [[3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
>
> 本地源码：`/Users/guoyang/IdeaProjects/spring/spring-framework`
>
> 核心文件：
> - `spring-beans/.../factory/BeanFactory.java`
> - `spring-beans/.../support/BeanDefinitionRegistry.java`
> - `spring-beans/.../support/DefaultListableBeanFactory.java`

---

## 一句话定位

| 类/接口 | 回答的问题 |
|---------|-----------|
| `BeanDefinition` | Bean **长什么样**？（类、属性、作用域） |
| `BeanDefinitionRegistry` | 蓝图**存哪、怎么注册**？ |
| `BeanFactory` | **怎么根据名称/类型拿到** Bean 实例？ |
| `DefaultListableBeanFactory` | 以上**全部 + 自动装配 + 按类型查找**的默认实现 |

```text
BeanDefinition（蓝图）
       ↓ registerBeanDefinition
BeanDefinitionRegistry（注册表）
       ↓ getBean
BeanFactory（客户端视角：拿实例）
       ↓ 默认实现
DefaultListableBeanFactory（完整工厂）
```

---

## 整体关系图

```text
                    BeanFactory（接口：怎么拿 Bean）
                         │
         ┌───────────────┼───────────────┐
         │               │               │
   ListableBeanFactory   │    BeanDefinitionRegistry
   （按类型列举）         │    （注册 BeanDefinition）
         │               │               │
         └───────┬───────┘               │
                 │                       │
         DefaultListableBeanFactory ←────┘
         （唯一完整默认实现）
                 │
         AbstractAutowireCapableBeanFactory（自动装配）
                 │
         AbstractBeanFactory（doGetBean 核心）
                 │
         DefaultSingletonBeanRegistry（单例三级缓存）
```

---

## 一、BeanFactory — IoC 根接口

### 1.1 定位

`BeanFactory` 是 Spring IoC 的**根接口**，定义访问 Bean 容器的最基本能力。

源码注释（`BeanFactory.java`）：

```java
/**
 * The root interface for accessing a Spring bean container.
 *
 * This is the basic client view of a bean container;
 * further interfaces such as ListableBeanFactory and
 * ConfigurableBeanFactory are available for specific purposes.
 */
```

核心思想：

1. 容器持有 Bean 定义，每个 Bean 有唯一名称
2. 按名称获取实例：单例返回同一对象，原型每次新建
3. 集中配置：对象不再自己读配置文件
4. DI 基于 BeanFactory 实现，但推荐「推式」注入，而非主动 `getBean` 拉取

### 1.2 核心 API

#### 获取 Bean

| 方法 | 说明 |
|------|------|
| `getBean(String name)` | 按名称获取 |
| `getBean(String name, Class<T> type)` | 按名称 + 类型（类型安全） |
| `getBean(String name, Object... args)` | 按名称 + 构造器参数 |
| `getBean(Class<T> type)` | 按类型获取唯一 Bean |
| `getBean(Class<T> type, Object... args)` | 按类型 + 参数 |
| `getBeanProvider(Class/ResolvableType)` | 延迟/可选获取（`ObjectProvider`） |

```java
// 按名称
UserService us = factory.getBean("userService", UserService.class);

// 按类型（要求唯一）
UserService us2 = factory.getBean(UserService.class);

// 延迟/可选
ObjectProvider<UserService> provider = factory.getBeanProvider(UserService.class);
UserService opt = provider.getIfAvailable;
```

#### getBeanProvider 与 ObjectProvider ★

> 子主题：[[getBeanProvider与ObjectProvider有什么用]] · 辅助比喻：[[3-速查-Spring厨房比喻大全]]

**一句话**：`getBeanProvider(type)` 返回 **`ObjectProvider<T>`**——不是立刻给实例，而是给「**按需向容器取 Bean**」的句柄。

| | `getBean(Class<T>)` | `getBeanProvider(Class<T>)` |
|---|---------------------|----------------------------|
| 返回 | 实例 **T** | **`ObjectProvider<T>`** |
| 何时创建 | **调用时**立即 resolve / 创建 | **第一次** `getObject` 等时才 resolve |
| 没有 Bean | `NoSuchBeanDefinitionException` | `getIfAvailable` → `null` |
| 多个 Bean | `NoUniqueBeanDefinitionException` | `stream` 遍历；`getIfUnique` → `null` |

**`ObjectProvider` 常用 API**

```java
ObjectProvider<PaymentGateway> p = factory.getBeanProvider(PaymentGateway.class);

p.getObject;              // 必须有且唯一，否则抛异常（≈ getBean）
p.getIfAvailable;         // 没有 → null
p.getIfUnique;            // 没有或多个且无 @Primary → null（最宽松）
p.ifAvailable(gw -> ...);     // 有才执行
p.stream;                 // 所有候选（≈ @Autowired List<T>）
p.orderedStream;          // 按 @Order 排序
```

**四个典型用途**

| 场景 | 写法 |
|------|------|
| **可选依赖**（容器里可能没有） | `getIfAvailable` / `ifAvailable` |
| **延迟加载**（构造时不创建 heavy Bean） | 构造器注入 `ObjectProvider<T>`，用时再 `getObject` |
| **多个同类型**（策略/插件） | `stream` / `orderedStream` |
| **打破循环** | 注入 provider 而非 T，延迟 `getBean` |

**与 `@Autowired` 的关系**

```java
@Autowired ObjectProvider<UserService> provider;  // 注入的是 provider，不是 UserService
@Autowired UserService userService;             // 立即 resolveDependency → getBean
```

`resolveDependency` 发现类型是 `ObjectProvider` 时返回 `DependencyObjectProvider`，**不立即** `getBean`：

```text
DefaultListableBeanFactory.resolveDependency
  → if (ObjectProvider.class == dependencyType)
  → return new DependencyObjectProvider(...)
```

**勿与 FactoryBean 混淆**

| | `ObjectProvider<T>` | `FactoryBean` |
|---|---------------------|---------------|
| 作用 | 延迟/可选取**容器里已有**的 Bean | 工厂 Bean，`getObject` **生产** P |
| 来源 | `getBeanProvider` / `@Autowired ObjectProvider` | 注册的 `MapperFactoryBean` 等 |

**业务是否需要？** 大多数 **不需要** → [[getBeanProvider与ObjectProvider有什么用]]

→ DI 原理：[[5-依赖注入实现原理]] · 接口地图：[[4-接口地图-IoC与DI重要接口大全#5. 依赖消费与对象生产]]

#### 查询元信息

| 方法 | 说明 |
|------|------|
| `containsBean(name)` | 是否存在 |
| `isSingleton(name)` / `isPrototype(name)` | 作用域 |
| `isTypeMatch(name, type)` | 类型是否匹配 |
| `getType(name)` | 获取 Bean 类型 |
| `getAliases(name)` | 获取别名 |

#### FactoryBean 前缀

> 完整说明 → [[5-工厂Bean-BeanFactory与FactoryBean的区别]] · 接口体系 → [[4-工厂Bean-FactoryBean接口体系详解]]

```java
String FACTORY_BEAN_PREFIX = "&";

// getBean("myFactory")  → FactoryBean 产出的对象
// getBean("&myFactory") → FactoryBean 本身
```

### 1.3 接口继承体系

```text
BeanFactory                              ← 根接口
  ↑
HierarchicalBeanFactory                  ← 父子容器（getParentBeanFactory）
  ↑
ConfigurableBeanFactory                ← 工厂配置 + SingletonBeanRegistry
  ↑
ConfigurableListableBeanFactory          ← 预实例化、ignoreDependency 等

BeanFactory
  ↑
ListableBeanFactory                      ← 按类型/注解批量列举
  ↑
ConfigurableListableBeanFactory

BeanFactory
  ↑
AutowireCapableBeanFactory               ← resolveDependency、autowireBean
  ↑
ConfigurableListableBeanFactory

AliasRegistry                            ← 别名管理（独立分支）
  ↑
BeanDefinitionRegistry                   ← 注册 BeanDefinition
  ↑ (implements)
DefaultListableBeanFactory               ← 实现以上全部
```

| 子接口 | 额外能力 | 典型使用者 |
|--------|---------|-----------|
| `HierarchicalBeanFactory` | 父子容器，`getBean` 找不到时查父工厂 | Web 多层 Context |
| `ListableBeanFactory` | `getBeanNamesForType`、`getBeansOfType` | `@Autowired List<T>` |
| `AutowireCapableBeanFactory` | `resolveDependency`、`autowireBean` | `@Autowired` 底层 |
| `ConfigurableBeanFactory` | 设置 ClassLoader、Scope、BPP | 框架内部 |
| `ConfigurableListableBeanFactory` | 预实例化、freezeConfiguration | `refresh` 阶段 |

> **注意**：`ListableBeanFactory` 的方法**不查父容器**，需用 `BeanFactoryUtils.beanNamesForTypeIncludingAncestors`。

### 1.4 getBean 核心流程 — doGetBean

所有 `getBean` 最终进入 `AbstractBeanFactory.doGetBean`：

```text
getBean(name)
  → doGetBean(name, ...)
      ├── transformedBeanName(name)        // 处理 & 前缀、别名
      ├── getSingleton(beanName)           // 1. 查单例缓存
      │     └── 命中 → getObjectForBeanInstance（处理 FactoryBean）
      ├── 本地无定义 → 委托 parentBeanFactory
      ├── getMergedLocalBeanDefinition   // 2. 合并定义
      ├── 处理 depends-on 依赖
      └── 按 scope 创建
            ├── singleton → getSingleton(name,  -> createBean(...))
            ├── prototype → createBean(...)
            └── scoped    → Scope.get(...)
                  → createBean
                    → doCreateBean
                      ├── createBeanInstance  [实例化]
                      ├── populateBean        [依赖注入]
                      └── initializeBean      [初始化]
```

### 1.5 Bean 生命周期（BeanFactory 定义的标准顺序）

Bean 工厂实现应尽可能支持标准 Bean 生命周期接口。完整顺序定义在 `BeanFactory.java` 类注释中。

#### 初始化阶段（按顺序）

| 顺序 | 回调 | 说明 |
|:----:|------|------|
| 1 | `BeanNameAware.setBeanName` | 注入 Bean 名称 |
| 2 | `BeanClassLoaderAware.setBeanClassLoader` | 注入 Bean 类加载器 |
| 3 | `BeanFactoryAware.setBeanFactory` | 注入所属 BeanFactory |
| 4 | `EnvironmentAware.setEnvironment` | 注入 Environment |
| 5 | `EmbeddedValueResolverAware.setEmbeddedValueResolver` | 注入嵌入式值解析器（`@Value` 占位符） |
| 6 | `ResourceLoaderAware.setResourceLoader` | 注入 ResourceLoader（**仅 ApplicationContext**） |
| 7 | `ApplicationEventPublisherAware.setApplicationEventPublisher` | 注入事件发布器（**仅 ApplicationContext**） |
| 8 | `MessageSourceAware.setMessageSource` | 注入国际化 MessageSource（**仅 ApplicationContext**） |
| 9 | `ApplicationContextAware.setApplicationContext` | 注入 ApplicationContext（**仅 ApplicationContext**） |
| 10 | `ServletContextAware.setServletContext` | 注入 ServletContext（**仅 Web ApplicationContext**） |
| 11 | `BeanPostProcessor.postProcessBeforeInitialization` | BPP 初始化**前**回调 |
| 12 | `InitializingBean.afterPropertiesSet` | 属性设置完成回调 |
| 13 | 自定义 `init-method` | 配置的 init 方法 |
| 14 | `BeanPostProcessor.postProcessAfterInitialization` | BPP 初始化**后**回调（**AOP 代理常在此生成**） |

> **注意**：Aware 回调在属性注入（`populateBean`）之后、初始化逻辑之前执行。步骤 6-10 仅在 ApplicationContext 环境下生效。

```text
实例化 createBeanInstance
  → 属性注入 populateBean          ← @Autowired 在此
  → Aware 回调（步骤 1-10）
  → BPP beforeInit（步骤 11）
  → afterPropertiesSet / init-method（步骤 12-13）
  → BPP afterInit（步骤 14）          ← AOP 代理在此
  → Bean 就绪
```

#### 销毁阶段（按顺序）

| 顺序 | 回调 | 说明 |
|:----:|------|------|
| 1 | `DestructionAwareBeanPostProcessor.postProcessBeforeDestruction` | 销毁前 BPP 回调 |
| 2 | `DisposableBean.destroy` | DisposableBean 销毁回调 |
| 3 | 自定义 `destroy-method` | 配置的 destroy 方法 |

触发时机：容器关闭时 `ConfigurableBeanFactory.destroySingletons` → 逐个单例 Bean 销毁。

→ 完整机制（登记时机、为何需要、Context.close vs JVM 退出）：[[9-Bean 销毁机制详解]]

### 1.6 BeanFactory vs ApplicationContext

| | BeanFactory | ApplicationContext |
|--|-------------|-------------------|
| 定位 | IoC 核心 | 企业级容器 |
| 加载 | **延迟加载**（lazy） | 启动时预实例化非 lazy 单例 |
| 功能 | Bean 生命周期 | + 事件、国际化、Environment、AOP |
| 继承 | — | `ListableBeanFactory` + 更多 |
| 典型实现 | `DefaultListableBeanFactory` | `AnnotationConfigApplicationContext` |

> 完整说明 → [[5-Context层-ApplicationContext详解]]

```text
ApplicationContext.getBean("userService")
  → 内部 DefaultListableBeanFactory.getBean("userService")
  → AbstractBeanFactory.doGetBean(...)
```

### 1.7 FactoryBean 专题（延伸阅读）

**BeanFactory** 与 **FactoryBean** 名字相似、角色完全不同：

| | BeanFactory | FactoryBean |
|--|-------------|-------------|
| 层级 | IoC **容器**接口 | 容器里的**一种特殊 Bean** |
| `getBean("xxx")` | 普通 Bean → 实例本身 | FactoryBean → `getObject` 产品 |
| 获取工厂本身 | — | `getBean("&xxx")` |

```text
BeanFactory 专题阅读顺序：
  08 Registry + DefaultListableBeanFactory
  → 09 BeanFactory 接口体系（本文）
  → 10 ApplicationContext
  → 14 BeanFactory vs FactoryBean
  → 15 FactoryBean 接口体系
```

详见 [[5-工厂Bean-BeanFactory与FactoryBean的区别]]、[[4-工厂Bean-FactoryBean接口体系详解]]。

---

## 二、Registry / DefaultListableBeanFactory（详见 08）

> `BeanDefinitionRegistry` 接口、`DefaultListableBeanFactory` 数据结构、`registerBeanDefinition` 流程、`resolveDependency`、`preInstantiateSingletons`、`freezeConfiguration`，以及「注册 → BFPP → 冻结 → 实例化」协作生命周期，均属**注册层**主题，本文不再重复。
>
> 完整源码级讲解 → [[3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]。

**三者一句话对齐**：

- `BeanFactory` — 「取 Bean」的根接口（**本文第一章主题**）
- `BeanDefinitionRegistry` — 「注册定义」的接口（08 详解）
- `DefaultListableBeanFactory` — 同时实现上述两者的默认落地实现（08 详解）

```text
BeanDefinitionRegistry（注册定义） ┐
                                   ├─→ DefaultListableBeanFactory（默认实现 · 08）
BeanFactory（取 Bean · 本文一章）  ┘
```

设计要点（定义与实例分离、先注册后创建、合并定义、两级名称索引、客户端 vs 框架内部接口）见 [[3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解#六、关键设计思想]] 与 [[1-元数据层-BeanDefinition三兄弟详解]]。

---

## 三、日常开发中的接触方式

日常几乎不直接使用 `BeanFactory`，但底层都是它：

```java
// 1. 注入 ApplicationContext（本质是 ListableBeanFactory）
@Autowired
private ApplicationContext context;

// 2. @Autowired 底层
// → AutowireCapableBeanFactory.resolveDependency
// → DefaultListableBeanFactory.doResolveDependency

// 3. 按类型获取所有实现
@Autowired
private List<PaymentService> paymentServices;
// → getBeanNamesForType(PaymentService.class)

// 4. 框架内部注册定义
// → BeanDefinitionRegistry.registerBeanDefinition
// → 之后 getBean 才能拿到实例
```

---

## 四、概念速查

| 概念 | 一句话 | 源码位置 |
|------|--------|---------|
| `BeanFactory` | IoC 根接口，定义怎么拿 Bean | `factory/BeanFactory.java` |
| `ListableBeanFactory` | 按类型/注解批量列举 | `factory/ListableBeanFactory.java` |
| `AutowireCapableBeanFactory` | 自动装配、`resolveDependency` | `config/AutowireCapableBeanFactory.java` |
| `BeanDefinitionRegistry` | 注册 Bean 定义的唯一接口 | `support/BeanDefinitionRegistry.java` |
| `DefaultListableBeanFactory` | 以上全部的默认实现 | `support/DefaultListableBeanFactory.java` |
| `doGetBean` | getBean 真正干活的方法 | `support/AbstractBeanFactory.java` |
| `beanDefinitionMap` | 原始定义存储 | `DefaultListableBeanFactory` |
| `registerBeanDefinition` | 定义注册入口 | `DefaultListableBeanFactory` |
| `preInstantiateSingletons` | 预实例化非 lazy 单例 | `DefaultListableBeanFactory` |
| `resolveDependency` | `@Autowired` 底层 | `DefaultListableBeanFactory` |

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]] | [[5-Context层-ApplicationContext详解]] |

> FactoryBean 专题：[[5-工厂Bean-BeanFactory与FactoryBean的区别]] → [[4-工厂Bean-FactoryBean接口体系详解]]
>
> ApplicationContext 专题：[[5-Context层-ApplicationContext详解]]

## 关联

- [[00-Spring-Framework核心机制-学习导航]]
- [[1-IoC与DI核心概念]]
- [[2-Bean加载原理与源码阅读路径]]
- [[1-元数据层-BeanDefinition三兄弟详解]]
- [[3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
- [[5-工厂Bean-BeanFactory与FactoryBean的区别]]
- [[4-工厂Bean-FactoryBean接口体系详解]]
- [[5-Context层-ApplicationContext详解]]
- [[2-速查-IoC与DI核心整合速查]]
- [[3-速查-Spring厨房比喻大全]]
- [[4-接口地图-IoC与DI重要接口大全]]
- [[1-源码调试与断点指南]]
- [[2-元数据层-AnnotatedBeanDefinitionReader与组件注册详解#FAQ：类存在但没有被使用，Spring 会怎样处理？]]
- [[5-依赖注入实现原理]]

---
## 下一步可深入

- [ ] 循环依赖 — 三级缓存、`addSingletonFactory` → [[3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
- [ ] FactoryBean vs 普通 Bean — `&` 前缀 → [[5-工厂Bean-BeanFactory与FactoryBean的区别]]
- [ ] SmartFactoryBean / AbstractFactoryBean → [[4-工厂Bean-FactoryBean接口体系详解]]
- [ ] `BeanPostProcessor` — 在 `populateBean` / `initializeBean` 中的介入点
- [ ] `@Primary` / `@Qualifier` — `determineAutowireCandidate` 源码
