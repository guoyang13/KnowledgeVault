---
type: case-study
status: reviewed
topic: Spring refresh / Aware / Processor
source_version: 6.2.x
---

# 测试驱动的 refresh 调用链 — Aware 与 Processor

> 导航：[[00-Spring-Framework核心机制-学习导航]] · **50 · 实践与调试**
>
> 关联：[[3-refresh方法详解]] · [[3-生命周期层-Aware体系详解]] · [[2-扩展点层-BeanPostProcessor详解]] · [[1-扩展点层-BeanFactoryPostProcessor详解]] · [[1-源码调试与断点指南]]
>
> 本文只负责通过一个测试还原调用顺序；Aware、BFPP、BPP 的正式定义以各自主文档为准。

---

## 场景：一个最小测试

```java
@Test
void autowiringIsEnabledByDefault {
    // 入口：构造即 register + refresh，内部预创建所有单例（含 testBean）
    ApplicationContext context = new AnnotationConfigApplicationContext(AutowiredConfig.class);
    // testBean 已在 refresh 阶段创建；此处 doGetBean 走单例缓存命中
    assertThat(context.getBean(TestBean.class).name).isEqualTo("foo");
}
```

三个业务类：

```java
@Configuration
@Import(NameConfig.class)
static class AutowiredConfig {
    @Autowired String autowiredName;          // ← 唯一注入点
    @Bean TestBean testBean {
        TestBean t = new TestBean;
        t.name = autowiredName;               // ← "foo" 从这里传进去
        return t;
    }
}
static class NameConfig { @Bean String name { return "foo"; } }
class TestBean { String name; }               // 纯 POJO，不实现任何 Aware
```

> **关键前提**：`TestBean` / `AutowiredConfig` / `NameConfig` **都不实现 Aware**。
> 真正用到 Aware / Processor 的，全是 Spring 注册进容器的**基础设施 Bean**。

```text
被处理的对象（TestBean、AutowiredConfig）→ 不实现 Aware
处理别人的基础设施（AAP、CCPP）           → 自己实现 Aware，靠回调拿容器能力
```

---

## 一、涉及的 Processor 与 Aware 总览

| 类型 | 组件 | 角色 | 在本测试的作用 |
|------|------|------|----------------|
| **BFPP** | `ConfigurationClassPostProcessor`（CCPP） | 解析 `@Configuration`/`@Bean`/`@Import` | 解析 `AutowiredConfig`，处理 `@Import(NameConfig)`，注册 `name`、`testBean` |
| **BPP** | `AutowiredAnnotationBeanPostProcessor`（AAP） | 处理 `@Autowired`/`@Value` | 给 `AutowiredConfig.autowiredName` 注入 `"foo"` |
| **BPP** | `CommonAnnotationBeanPostProcessor` | `@PostConstruct`/`@Resource` | 链上存在，本测试无相关注解 → 空转 |
| **BPP** | `ApplicationContextAwareProcessor` | 回调 Context 级 Aware | 对业务类空过；对 CCPP 生效（注入 Environment 等） |
| **BPP** | `ApplicationListenerDetector` | 检测 `ApplicationListener` | 本测试无监听器 → 空转 |
| **Aware** | `BeanFactoryAware` | 注入 `BeanFactory` | **AAP 自己**靠它拿容器，才能 `resolveDependency` ★ |
| **Aware** | `Environment/ResourceLoader/ApplicationStartup/BeanClassLoaderAware` | 注入环境/资源/类加载器 | **CCPP** 靠它们完成解析 |

> **两种 Aware 的分工**（贯穿全文）：
> - **BeanFactory 级 Aware**（`BeanNameAware`/`BeanClassLoaderAware`/`BeanFactoryAware`）→ 由工厂内建方法 `invokeAwareMethods` 处理（阶段 6、11）。
> - **Context 级 Aware**（`EnvironmentAware`/`ApplicationContextAware`/`ResourceLoaderAware` 等）→ 由阶段 3 注册的 `ApplicationContextAwareProcessor` 处理。

---

## 二、按 refresh 阶段逐阶段详解

下面把 `refresh` 拆到方法级，标注在本测试中**谁触发了 Aware / Processor、触发了什么、为什么**。

### 阶段 0：构造函数（`refresh` 之前，"搭舞台"）

```text
new AnnotationConfigApplicationContext(AutowiredConfig.class)
│
├─ this                          // 无参构造
│   ├─ super = GenericApplicationContext
│   │     └─ new DefaultListableBeanFactory      ← 创建空 IoC 容器（此后所有 Bean 都住这里）
│   │
│   ├─ new AnnotatedBeanDefinitionReader(this)
│   │     └─ AnnotationConfigUtils.registerAnnotationConfigProcessors(registry)
│   │           注册 5 个基础设施 BeanDefinition（仅定义，未实例化）：
│   │             • ConfigurationClassPostProcessor      (BFPP)
│   │             • AutowiredAnnotationBeanPostProcessor  (BPP) ★
│   │             • CommonAnnotationBeanPostProcessor     (BPP)
│   │             • EventListenerMethodProcessor
│   │             • DefaultEventListenerFactory
│   │
│   └─ new ClassPathBeanDefinitionScanner(this)    // 本测试用 register 不用 scan，仅备用
│
├─ register(AutowiredConfig.class)
│     └─ 只把 AutowiredConfig 变成一条 BeanDefinition 放进 beanDefinitionMap
│        （此刻容器里还没有 name / testBean / NameConfig 的定义！）
│
└─ refresh   ← 下面逐阶段展开
```

> **要点**：阶段 0 结束时，容器里只有「5 个基础设施 BD + AutowiredConfig 的 BD」。
> `name`、`testBean`、`NameConfig` 都还不存在——它们要等阶段 5 的 BFPP 去"解析生"出来。

### 阶段 3：`prepareBeanFactory(beanFactory)` — 装配 Context 级 Aware 处理器

```java
// AbstractApplicationContext#prepareBeanFactory（源码定位见文末断点 3）
beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
beanFactory.ignoreDependencyInterface(ApplicationStartupAware.class);
beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));
```

这一步做了两件与 Aware 相关的关键事：

1. **注册 `ApplicationContextAwareProcessor`（一个 BPP）**：以后每个 Bean 初始化时，它负责回调 `EnvironmentAware`/`ApplicationContextAware` 等 **Context 级** Aware。
2. **`ignoreDependencyInterface(...)`**：告诉容器"这些 Aware 接口的 setter **不要当成自动装配点**"。
   - 例如 `setEnvironment(Environment)` 长得像一个可注入的 setter，容器若按类型自动装配就会重复注入一遍；但它其实是**回调**专用的，所以这里显式忽略，让它**只走回调、不走 `@Autowired`**。

### 阶段 5：`invokeBeanFactoryPostProcessors(beanFactory)` — BFPP 把 `@Bean` 解析成 BD

这是**第一次真正实例化基础设施 Bean**的地方。

```text
invokeBeanFactoryPostProcessors(beanFactory)
└─ PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(...)
   │
   ├─ 找出 BeanDefinitionRegistryPostProcessor 类型
   │   → getBean("configurationClassPostProcessor")   ★ 实例化 CCPP
   │        └─ doCreateBean → initializeBean
   │             ├─ invokeAwareMethods(ccpp)
   │             │    • ccpp instanceof BeanClassLoaderAware → setBeanClassLoader(...)
   │             │      （BeanFactory 级 Aware：注入类加载器）
   │             └─ applyBeanPostProcessorsBeforeInitialization(ccpp)
   │                  • ApplicationContextAwareProcessor.postProcessBeforeInitialization(ccpp)
   │                       └─ ccpp instanceof EnvironmentAware        → setEnvironment(env)
   │                          ccpp instanceof ResourceLoaderAware     → setResourceLoader(ctx)
   │                          ccpp instanceof ApplicationStartupAware  → setApplicationStartup(...)
   │                       （Context 级 Aware：注入解析所需资源）
   │
   └─ ccpp.postProcessBeanDefinitionRegistry(registry)   ★★ 解析动作
        ├─ 解析 @Configuration AutowiredConfig
        ├─ 处理 @Import(NameConfig)      → 注册 NameConfig 的 BD
        └─ 解析两个 @Bean 方法           → 注册 "name"、"testBean" 的 BD
```

> **为什么 CCPP 需要那些 Aware？** 它要"读懂"配置类：解析 `@ComponentScan`/`@PropertySource` 需要 `Environment`、`ResourceLoader`；加载类名需要 `ClassLoader`。这些能力它**自己没有**，只能通过 Aware 让容器"喂"给它。
>
> **阶段 5 结束时**：容器里终于有了 `name`、`testBean`、`NameConfig` 的 BeanDefinition，但仍**未创建实例**。

### 阶段 6：`registerBeanPostProcessors(beanFactory)` — 实例化 AAP，触发 `BeanFactoryAware` ★★

本测试的**核心 Aware 就在这里发生**。

```text
registerBeanPostProcessors(beanFactory)
└─ PostProcessorRegistrationDelegate.registerBeanPostProcessors(...)
   │
   ├─ getBean("autowiredAnnotationBeanPostProcessor")   ★ 实例化 AAP
   │    └─ doCreateBean → initializeBean
   │         └─ invokeAwareMethods(aap)
   │              • aap instanceof BeanFactoryAware   → TRUE
   │                   └─ aap.setBeanFactory(DefaultListableBeanFactory)   ★★★
   │                        this.beanFactory = clbf;   // AAP 从此持有整个容器
   │
   ├─ getBean("commonAnnotationBeanPostProcessor")      // 也是 BeanFactoryAware，同理注入
   │
   └─ 把这些 BPP 加入 beanFactory 的 beanPostProcessors 列表
      （以后每个业务 Bean 的 populateBean/initializeBean 都会经过它们）
```

关键源码（工厂内建的 BeanFactory 级 Aware 入口，见文末断点 5）：

```java
private void invokeAwareMethods(String beanName, Object bean) {
    if (bean instanceof Aware) {           // AAP 实现了 BeanFactoryAware → 进入
        if (bean instanceof BeanNameAware beanNameAware) { ... }
        if (bean instanceof BeanClassLoaderAware beanClassLoaderAware) { ... }
        if (bean instanceof BeanFactoryAware beanFactoryAware) {
            beanFactoryAware.setBeanFactory(AbstractAutowireCapableBeanFactory.this);  // ★★★
        }
    }
}
```

```java
// AutowiredAnnotationBeanPostProcessor#setBeanFactory（见文末断点 6）
@Override
public void setBeanFactory(BeanFactory beanFactory) {
    if (!(beanFactory instanceof ConfigurableListableBeanFactory clbf)) {
        throw new IllegalArgumentException(...);
    }
    this.beanFactory = clbf;   // ← 拿到容器，后续 resolveDependency 全靠它
    this.metadataReaderFactory = new SimpleMetadataReaderFactory(clbf.getBeanClassLoader);
}
```

> **为什么必须用 `BeanFactoryAware` 而不是 `@Autowired BeanFactory`？**
> AAP 本身就是"执行 `@Autowired` 的那个人"。在它自己被创建的时刻，`@Autowired` 机制还没法为它服务（**自举 / 先有鸡还是先有蛋**问题）。所以只能由容器在 `invokeAwareMethods` 里**直接把自己塞给它**。这是本测试断言成立的**前提条件**。

### 阶段 11：`finishBeanFactoryInitialization` → `preInstantiateSingletons` — 预创建单例，完成注入 ★

```text
finishBeanFactoryInitialization(beanFactory)
└─ beanFactory.preInstantiateSingletons
   └─ 遍历所有非 lazy 单例 beanName，逐个 getBean(name)
      顺序大致：configClass 相关 → nameConfig → autowiredConfig → name → testBean
```

#### ① `getBean("autowiredConfig")` — 触发 `@Autowired` 注入

```text
getBean("autowiredConfig")
└─ doGetBean → getSingleton(工厂lambda) → createBean → doCreateBean
   │
   ├─ createBeanInstance          new AutowiredConfig（@Configuration 被 CGLIB 增强）
   │
   ├─ populateBean   ★ 属性填充阶段
   │    └─ 遍历 BeanPostProcessor，命中 AAP：
   │         AAP.postProcessProperties(pvs, bean, "autowiredConfig")
   │           └─ 找到注入点：字段 @Autowired String autowiredName
   │                └─ InjectedElement.inject
   │                     └─ beanFactory.resolveDependency(
   │                            DependencyDescriptor(autowiredName, String.class), ...)   ★★
   │                          └─ doResolveDependency:
   │                               • 按类型 String 找候选 → 匹配到 @Bean name
   │                               • getBean("name") → 调 NameConfig.name → "foo"
   │                     └─ field.set(config, "foo")   // 反射写入 autowiredName = "foo"
   │              ↑ 这一步能成立，全靠阶段 6 AAP 已通过 setBeanFactory 拿到 beanFactory
   │
   └─ initializeBean
        └─ invokeAwareMethods(config)
             • config instanceof Aware → FALSE（AutowiredConfig 不实现 Aware）→ 整段跳过
```

> 注意：`getBean("name")` 是在解析 `autowiredName` 时**被连带触发**的，此时 `"foo"` 已经产出并缓存。

#### ② `getBean("testBean")` — `@Bean` 工厂方法产出结果

```text
getBean("testBean")
└─ doCreateBean
   └─ createBeanInstance
        └─ 通过 @Bean 工厂方法创建：调用 autowiredConfig.testBean
             TestBean t = new TestBean;
             t.name = autowiredName;   // autowiredName 已是 "foo"
             return t;                 // → TestBean(name="foo")
   └─ initializeBean
        ├─ invokeAwareMethods(testBean)
        │    • testBean instanceof Aware → FALSE → 跳过
        └─ ApplicationContextAwareProcessor.postProcessBeforeInitialization(testBean)
             • testBean instanceof Aware → FALSE → 空过
```

#### 断言：`context.getBean(TestBean.class)`

```text
getBean(TestBean.class)
└─ doGetBean → getSingleton("testBean")
     → 单例缓存命中，直接返回阶段 11 已创建好的 TestBean(name="foo")

assertThat(...name).isEqualTo("foo")   ✅
```

---

## 三、每个组件的具体作用

### 1. `ConfigurationClassPostProcessor`（BFPP）— 让 `@Bean` 生效

- **在哪**：阶段 5 `invokeBeanFactoryPostProcessors`
- **作用**：解析 `AutowiredConfig`，处理 `@Import(NameConfig)`，把 `name`/`testBean` 变成 BD
- **用到的 Aware**：`ResourceLoaderAware / ApplicationStartupAware / BeanClassLoaderAware / EnvironmentAware`——解析配置类需要读环境、加载类等，这些能力靠 Aware 由容器"喂"进来
- **没有它**：`@Configuration`/`@Bean` 不被解析，`testBean`/`name` 根本不存在

```java
public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor,
        ..., ResourceLoaderAware, ApplicationStartupAware, BeanClassLoaderAware, EnvironmentAware {
```

### 2. `AutowiredAnnotationBeanPostProcessor`（BPP）+ `BeanFactoryAware`（本测试主角）

- **Aware 用途**：`setBeanFactory` 在阶段 6 被 `invokeAwareMethods` 回调，AAP 拿到 `ConfigurableListableBeanFactory`
- **为何必须 Aware 而非 `@Autowired`**：AAP 正是执行 `@Autowired` 的那个 BPP，不能靠自己给自己注入（自举循环），只能由容器回调直接塞进去
- **DI 用途**：阶段 11 `populateBean` 时对 `@Autowired String autowiredName` 调 `resolveDependency` → 匹配 `@Bean name` 的 `"foo"`

> **这是断言 `name == "foo"` 的根因**：AAP 靠 `setBeanFactory` 拿到容器，再 `resolveDependency` 注入 `"foo"`，`testBean` 用它赋值。

### 3. `ApplicationContextAwareProcessor`（BPP）— 本测试对业务类空转

- **在哪**：阶段 3 注册
- **作用**：对每个 Bean 检查 `instanceof Aware` 后回调 `setEnvironment`/`setApplicationContext` 等
- **本测试**：业务类都不是 Context 级 Aware → 空过；但对 CCPP 生效（注入 Environment 等）

```java
public Object postProcessBeforeInitialization(Object bean, String beanName) {
    if (bean instanceof Aware) { invokeAwareInterfaces(bean); }
    return bean;
}
```

### 4. `invokeAwareMethods`（非 BPP，工厂内建）— BeanFactory 级 Aware 入口

每个 Bean 的 `initializeBean` 第一步都会走它；本测试里只对 AAP / CCPP 等基础设施 Bean 命中 `BeanFactoryAware` 等，对业务 Bean 全部跳过。

---

## 四、一图总览：谁在哪个阶段、干了什么

| refresh 阶段 | 组件 | Aware / 机制 | 本测试作用 |
|---|---|---|---|
| 阶段 0 构造 | 注册 5 个基础设施 BD | — | 定义 CCPP、AAP 等，未实例化 |
| 阶段 3 prepareBeanFactory | `ApplicationContextAwareProcessor` | 注册（Context 级 Aware 处理器） | 备用；业务类无 Context Aware |
| 阶段 5 invokeBFPP | `ConfigurationClassPostProcessor` | `Environment/ResourceLoader/ClassLoaderAware` | 解析 `@Bean`，生出 name/testBean 的 BD |
| **阶段 6 registerBPP** | **`AutowiredAnnotationBeanPostProcessor`** | **`BeanFactoryAware.setBeanFactory`** ★★★ | AAP 拿到容器，为注入做准备 |
| **阶段 11 preInstantiate** | **AAP.resolveDependency** | — | 给 `autowiredName` 注入 `"foo"` |
| 阶段 11 preInstantiate | `@Bean testBean` | — | 用 `autowiredName` 造出 `TestBean(name="foo")` |

**因果链一句话**：

```text
阶段 5 让 @Bean 定义存在
  → 阶段 6 靠 BeanFactoryAware 让 AAP 拿到容器
    → 阶段 11 AAP 靠 resolveDependency 注入 "foo"
      → testBean 用它赋值
        → 断言 name == "foo" 成立
```

---

## 五、常见误区

| 误区 | 纠正 |
|------|------|
| 「这个测试验证 TestBean 的 Aware」 | TestBean 不实现任何 Aware；验证的是**配置类 @Autowired 默认开启** |
| 「AAP 用 `@Autowired BeanFactory` 拿容器」 | 会自举循环；必须靠 `BeanFactoryAware` 回调 |
| 「`@Bean` 在构造阶段就创建」 | 构造阶段只注册 AutowiredConfig 的 BD；`@Bean` 要到阶段 5 才被解析成 BD、阶段 11 才实例化 |
| 「Context 级 Aware 也走自动装配」 | 阶段 3 已 `ignoreDependencyInterface`，只走回调 |

---

## 源码断点

1. `AbstractApplicationContext.refresh` — 看 12 阶段骨架 → [[3-refresh方法详解]]
2. `AnnotationConfigUtils.registerAnnotationConfigProcessors` — 阶段 0 注册基础设施
3. `AbstractApplicationContext.prepareBeanFactory` — 阶段 3 注册 Aware 处理器 + `ignoreDependencyInterface`
4. `ConfigurationClassPostProcessor.postProcessBeanDefinitionRegistry` — 阶段 5 解析 @Bean
5. `AbstractAutowireCapableBeanFactory.invokeAwareMethods` — 阶段 6/11 BeanFactory 级 Aware
6. `AutowiredAnnotationBeanPostProcessor.setBeanFactory` / `postProcessProperties` — 阶段 6/11
7. `DefaultListableBeanFactory.doResolveDependency` — 阶段 11 注入 "foo"

→ [[1-源码调试与断点指南]]
