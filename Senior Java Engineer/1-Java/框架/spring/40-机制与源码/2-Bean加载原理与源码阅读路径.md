---
type: canonical
status: reviewed
topic: Spring Bean retrieval / creation
source_version: 6.2.x
---

# Bean 加载原理与源码阅读路径

> 导航：[[00-Spring-Framework核心机制-学习导航]] · **40 · 机制与源码** · Bean 加载主线 · 前置：[[1-IoC与DI核心概念]]、[[1-注解入门-配置类与组件类]]
>
> 本地源码：`/Users/guoyang/IdeaProjects/spring/spring-framework`

---

## 从哪里开始

学习 Spring 加载 Bean 的原理，建议沿这条主线读源码：

```text
Bean 是什么  →  定义怎么进来  →  容器怎么启动  →  实例怎么创建
```

不要一上来就扎进 `doCreateBean`。先把 **BeanDefinition（蓝图）** 和 **refresh（启动编排）** 搞清楚，后面的调用链会自然串起来。

---

## 核心心智模型

Spring 加载 Bean 分 **两个阶段**：

| 阶段 | 做什么 | 关键词 |
|------|--------|--------|
| **1. 注册 BeanDefinition** | 登记「要创建什么」 | 元数据、蓝图 |
| **2. 实例化 Bean** | 真正 `new` 对象、注入依赖、初始化 | 对象、生命周期 |

```text
阶段一：register / scan / XmlBeanDefinitionReader
           → BeanDefinitionRegistry.registerBeanDefinition

阶段二：refresh → preInstantiateSingletons
           → getBean → doGetBean → createBean → doCreateBean
             → createBeanInstance  [实例化]
             → populateBean        [依赖注入 ← DI]
             → initializeBean      [初始化]
```

---

## 源码阅读路径（8 步）

### 第 1 步：理解 Bean 的「蓝图」

先看 `BeanDefinition` 接口——它描述一个 Bean 的元数据（类名、scope、属性、构造参数等），**此时还没有对象**。

| 文件 | 路径 |
|------|------|
| `BeanDefinition` | `spring-beans/.../config/BeanDefinition.java` |
| `AbstractBeanDefinition` | `spring-beans/.../support/AbstractBeanDefinition.java` |
| `RootBeanDefinition` | `spring-beans/.../support/RootBeanDefinition.java` |

> 三兄弟职责、注解场景下的产生路径、合并流程 → [[1-元数据层-BeanDefinition三兄弟详解]]

源码注释：

```java
/**
 * A BeanDefinition describes a bean instance, which has property values,
 * constructor argument values, and further information supplied by
 * concrete implementations.
 *
 * This is just a minimal interface: The main intention is to allow a
 * BeanFactoryPostProcessor to introspect and modify property values
 * and other bean metadata.
 */
```

常见实现类：

| 实现类                              | 产生场景                     |
| -------------------------------- | ------------------------ |
| `RootBeanDefinition`             | 运行时使用的完整、已合并定义           |
| `ScannedGenericBeanDefinition`   | `@ComponentScan` 扫描产生    |
| `AnnotatedGenericBeanDefinition` | `@Configuration` 等注解注册产生 |

---

### 第 2 步：理解 Bean 存在哪里

| 文件                           | 作用                              |
| ---------------------------- | ------------------------------- |
| `BeanDefinitionRegistry`     | 注册接口：`registerBeanDefinition` |
| `DefaultListableBeanFactory` | 默认实现，既存定义又负责创建 Bean             |

类继承关系：

```text
DefaultListableBeanFactory
  extends AbstractAutowireCapableBeanFactory   (createBean)
  extends AbstractBeanFactory                  (getBean)
  extends DefaultSingletonBeanRegistry         (单例缓存)
  implements BeanDefinitionRegistry            (存定义)
```

读 `DefaultListableBeanFactory` 时重点看：**类注释** + `registerBeanDefinition` + `preInstantiateSingletons`。

> 接口体系、核心数据结构、`registerBeanDefinition` / `resolveDependency` 完整流程 → [[3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
>
> BeanFactory 接口体系、doGetBean 流程 → [[4-容器层-BeanFactory接口体系详解]]
>
> ApplicationContext 详解 → [[5-Context层-ApplicationContext详解]]
>
> BeanFactory vs FactoryBean、FactoryBean 接口体系 → [[5-工厂Bean-BeanFactory与FactoryBean的区别]] · [[4-工厂Bean-FactoryBean接口体系详解]]

---

### 第 3 步：选一个「定义加载」入口

按你熟悉的用法选一条线，**现代项目建议从注解入口入手**。

| 用法 | 入口类 | 模块 |
|------|--------|------|
| `@Configuration` / `@Bean` | `AnnotationConfigApplicationContext` | spring-context |
| `@ComponentScan` | `ClassPathBeanDefinitionScanner` | spring-context |
| XML | `ClassPathXmlApplicationContext` → `XmlBeanDefinitionReader` | spring-beans |

**注解配置加载链：**

```text
AnnotationConfigApplicationContext
  → register(MyConfig.class) / scan("com.example")
  → refresh
  → invokeBeanFactoryPostProcessors
  → ConfigurationClassPostProcessor
  → ConfigurationClassParser + ConfigurationClassBeanDefinitionReader
  → registerBeanDefinition
```

> `register` 与 `scan` 入口、`@Component` 三条注册路径 → [[2-元数据层-AnnotatedBeanDefinitionReader与组件注册详解]]

**组件扫描加载链：**

```text
scan("com.example")
  → ClassPathBeanDefinitionScanner.doScan
  → BeanDefinitionReaderUtils.registerBeanDefinition
```

**XML 加载链：**

```text
ClassPathXmlApplicationContext
  → refreshBeanFactory → loadBeanDefinitions
  → XmlBeanDefinitionReader → DefaultBeanDefinitionDocumentReader
  → registerBeanDefinition
```

> 两种 Context 风格：注解走 `GenericApplicationContext`（构造时即有 BeanFactory）；XML 走 `AbstractRefreshableApplicationContext`（每次 refresh 重建 BeanFactory）。两者最终都汇入同一个 `refresh`。

---

### 第 4 步：容器启动总入口 — `refresh`

**文件**：`spring-context/.../support/AbstractApplicationContext.java`

> **逐步解析（12 阶段、每个子方法）** → [[3-refresh方法详解]]  
> ApplicationContext 完整说明 → [[5-Context层-ApplicationContext详解]]

这是 Spring 容器启动的 **核心方法**（概览）：

```text
refresh
├── prepareRefresh                          // 激活容器，初始化属性源
├── obtainFreshBeanFactory                  // 获取/创建 BeanFactory
├── prepareBeanFactory(beanFactory)           // ClassLoader、SpEL、环境、scope
├── postProcessBeanFactory(beanFactory)       // 子类扩展点
├── invokeBeanFactoryPostProcessors         // 处理 @Configuration、占位符等 ← 关键
├── registerBeanPostProcessors              // 注册 BeanPostProcessor
├── initMessageSource
├── initApplicationEventMulticaster
├── onRefresh                               // 子类扩展点（如 Spring MVC）
├── registerListeners
├── finishBeanFactoryInitialization         // 创建非 lazy 单例 ← 关键
└── finishRefresh                           // 发布 ContextRefreshedEvent
```

**这一步最该盯的两个方法：**

- `invokeBeanFactoryPostProcessors` — 解析 `@Configuration`、`@Bean`、`@Import`
- `finishBeanFactoryInitialization` — 调用 `preInstantiateSingletons`，提前创建所有非 lazy 单例

有序调用 BFPP / BPP 的辅助类：`PostProcessorRegistrationDelegate`。

→ 扩展点分工总览：[[7-IoC扩展点三部曲对照]] · BFPP [[1-扩展点层-BeanFactoryPostProcessor详解]] · BPP [[2-扩展点层-BeanPostProcessor详解]]

---

### 第 5 步：注解配置如何变成 BeanDefinition

如果你走注解路线，这一步不能跳过：

| 文件 | 作用 |
|------|------|
| `ConfigurationClassPostProcessor` | 在 `refresh` 阶段被调用的 BeanFactoryPostProcessor |
| `ConfigurationClassParser` | 解析 `@Configuration`、`@Import`、`@ComponentScan` |
| `ConfigurationClassBeanDefinitionReader` | 把 `@Bean` 方法注册成 BeanDefinition |

```text
refresh
  → invokeBeanFactoryPostProcessors
      → ConfigurationClassPostProcessor.processConfigBeanDefinitions
          → ConfigurationClassParser.parse
          → ConfigurationClassBeanDefinitionReader.loadBeanDefinitions
              → registerBeanDefinition   // 每个 @Bean 方法一条
```

详见 [[1-注解入门-配置类与组件类]]。

---

### 第 6 步：获取 Bean — `getBean`

**文件**：`spring-beans/.../support/AbstractBeanFactory.java`

```text
getBean(name)
  → doGetBean(name, ...)
      ├── getSingleton(beanName)           // 1. 先查单例缓存
      └── getMergedLocalBeanDefinition
          └── createBean(beanName, mbd, args)   // 2. 没命中 → 创建
```

`doGetBean` 是真正干活的方法：

```java
protected <T> T doGetBean(String name, ...) {
    String beanName = transformedBeanName(name);
    // 1. 先查单例缓存
    Object sharedInstance = getSingleton(beanName);
    if (sharedInstance != null && args == null) {
        // 命中缓存，直接返回
    }
    // 2. 没命中 → createBean
}
```

---

### 第 7 步：创建 Bean — `createBean` → `doCreateBean`

**文件**：`spring-beans/.../support/AbstractAutowireCapableBeanFactory.java`

> **逐环深度分析（7 步流水线、createBeanInstance 六分支、图文结合）** → [[4-doCreateBean深度解析]]

```text
createBean(beanName, mbd, args)                    //
├── resolveBeanClass
├── resolveBeforeInstantiation                   // AOP 等可提前返回代理
└── doCreateBean(beanName, mbd, args)              //
      ├── createBeanInstance                     // 实例化
      ├── applyMergedBeanDefinitionPostProcessors
      ├── addSingletonFactory                    // 三级缓存，解决循环依赖
      ├── populateBean                           // 属性注入 / @Autowired ← DI
      ├── initializeBean                         // Aware 回调、init、BPP
      └── registerDisposableBeanIfNecessary      // 销毁登记 → [[9-Bean 销毁机制详解]]
      // 循环依赖：addSingletonFactory 在 populateBean 之前 → [[6-循环依赖与三级缓存详解]]
```

`createBeanInstance` 的决策顺序（ 附近）：

1. `InstanceSupplier`（AOT / 程序化）
2. `@Bean` 工厂方法 → `instantiateUsingFactoryMethod`
3. 构造器自动装配 → `autowireConstructor`
4. 默认无参构造 → `instantiateBean`

---

### 第 8 步：实例化细节 — `instantiateBean`

当使用默认无参构造器时，`createBeanInstance` 会调用 `instantiateBean`：

```text
instantiateBean(beanName, mbd)
  → getInstantiationStrategy.instantiate(mbd, beanName, this)
  → wrap in BeanWrapperImpl
```

> `createBeanInstance` 的完整六条实例化分支（Supplier / 工厂方法 / 缓存 / 构造器推断 / preferred / 无参）、以及 `autowireConstructor` 内部如何逐参 `resolveDependency` → [[4-doCreateBean深度解析]]

---

## 完整调用链（一图看懂）

```text
new AnnotationConfigApplicationContext(AppConfig.class)
  │
  ├─ [阶段一：定义加载]
  │   register / scan / XmlBeanDefinitionReader
  │     → BeanDefinitionRegistry.registerBeanDefinition
  │
  └─ [阶段二：refresh]
        ├─ invokeBeanFactoryPostProcessors
        │     └─ ConfigurationClassPostProcessor → 更多 registerBeanDefinition
        │
        └─ finishBeanFactoryInitialization
              └─ preInstantiateSingletons
                    └─ getBean(name)
                          └─ doGetBean
                                └─ createBean
                                      └─ doCreateBean
                                            ├─ createBeanInstance  [new 对象]
                                            ├─ populateBean        [注入依赖]
                                            └─ initializeBean      [初始化]
```

### 非 lazy 单例的 eager 创建

```text
finishBeanFactoryInitialization
  → DefaultListableBeanFactory.preInstantiateSingletons   //
    → preInstantiateSingleton(beanName, mbd)
      → if (!mbd.isLazyInit) instantiateSingleton(beanName)
            → getBean(beanName)
```

> 注册了但没人用的 Bean 也会走这条路径，详见 [[2-元数据层-AnnotatedBeanDefinitionReader与组件注册详解#FAQ：类存在但没有被使用，Spring 会怎样处理？]]。

---

## 关键源码文件速查

### spring-beans（IoC 容器核心）

| 类 | 路径 | 作用 |
|----|------|------|
| `BeanFactory` | `.../factory/BeanFactory.java` | IoC 容器根接口 |
| `BeanDefinition` | `.../config/BeanDefinition.java` | Bean 元数据 |
| `DefaultListableBeanFactory` | `.../support/DefaultListableBeanFactory.java` | 默认 BeanFactory |
| `DefaultSingletonBeanRegistry` | `.../support/DefaultSingletonBeanRegistry.java` | 单例缓存 |
| `AbstractBeanFactory` | `.../support/AbstractBeanFactory.java` | `doGetBean` |
| `AbstractAutowireCapableBeanFactory` | `.../support/AbstractAutowireCapableBeanFactory.java` | `createBean` / `doCreateBean` |
| `XmlBeanDefinitionReader` | `.../xml/XmlBeanDefinitionReader.java` | 解析 XML |

### spring-context（应用上下文）

| 类 | 路径 | 作用 |
|----|------|------|
| `ApplicationContext` | `.../context/ApplicationContext.java` | 扩展 ListableBeanFactory |
| `AbstractApplicationContext` | `.../support/AbstractApplicationContext.java` | `refresh` 编排 |
| `AnnotationConfigApplicationContext` | `.../annotation/AnnotationConfigApplicationContext.java` | 注解入口 |
| `ClassPathBeanDefinitionScanner` | `.../annotation/ClassPathBeanDefinitionScanner.java` | 组件扫描 |
| `ConfigurationClassPostProcessor` | `.../annotation/ConfigurationClassPostProcessor.java` | 解析配置类 |

---

## 实践建议

1. **先跑一个最小 Demo**，在 IDE 里对 `refresh`、`doGetBean`、`doCreateBean` 打断点，跟一遍调用栈
2. **从 `AnnotationConfigApplicationContext` 入手**，比 XML 更贴近日常开发
3. **模块对应关系**：
   - `spring-beans` — Bean 创建核心
   - `spring-context` — 容器生命周期、`@Configuration` 解析
4. **主流程跟完**再进 [[2-扩展点层-BeanPostProcessor详解]]、循环依赖、`ConfigurationClassEnhancer` 等旁支

详细断点清单和三次跟栈路线见 [[1-源码调试与断点指南]]。

---

## 下一步可深入

- [ ] `ConfigurationClassEnhancer` — `@Configuration` 的 CGLIB 代理原理
- [ ] 循环依赖 — 三级缓存、`addSingletonFactory`
- [x] `BeanPostProcessor` — 在 `populateBean` / `initializeBean` 中的介入点 → [[2-扩展点层-BeanPostProcessor详解]]
- [x] `BeanFactoryPostProcessor` — `invokeBeanFactoryPostProcessors` → [[1-扩展点层-BeanFactoryPostProcessor详解]]
- [ ] `@Conditional` / `@Profile` — 条件装配源码

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[1-IoC与DI核心概念]] | [[3-refresh方法详解]] |

---

## 关联

- [[00-Spring-Framework核心机制-学习导航]]
- [[1-IoC与DI核心概念]]
- [[1-元数据层-BeanDefinition三兄弟详解]]
- [[4-容器层-BeanFactory接口体系详解]]
- [[3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
- [[5-Context层-ApplicationContext详解]]
- [[2-速查-IoC与DI核心整合速查]]
- [[4-接口地图-IoC与DI重要接口大全]]
- [[2-扩展点层-BeanPostProcessor详解]]
- [[1-扩展点层-BeanFactoryPostProcessor详解]]
- [[3-生命周期层-Aware体系详解]]
- [[7-IoC扩展点三部曲对照]]
- [[1-扩展点层-BeanFactoryPostProcessor详解]]
