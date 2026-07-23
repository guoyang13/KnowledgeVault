# IoC 与 DI 重要接口大全

> 导航：[[00-Spring-Bean加载-学习导航]] · **上篇 01–15** · 接口地图 · 读源码前总览
>
> 前置：[[03-速查-IoC与DI核心整合速查]] · [[04-速查-Spring厨房比喻大全]] · [[02-注解入门-Configuration与Service等注解区别]]
>
> 关联：[[06-元数据层-BeanDefinition三兄弟详解]] · [[09-容器层-BeanFactory与Registry详解]]
>
> 按本文分层地图顺序阅读上篇：元数据 → 注册 → 容器 → Context → 扩展点 → 生命周期

---

## 一句话

Spring IoC/DI 不只有 `BeanFactory` 和 `BeanDefinition`——还有**容器子接口、扩展点（BFPP/BPP）、生命周期回调、Aware 族、DI 消费端、Context 层注解扩展**等大量接口，构成完整的可扩展容器体系。

---

## 接口分层地图

```text
【元数据层】
  BeanDefinition · AnnotatedBeanDefinition · BeanDefinitionReader

【容器层】
  BeanFactory
    ├── ListableBeanFactory          ← 按类型列举
    ├── HierarchicalBeanFactory      ← 父子容器
    ├── AutowireCapableBeanFactory   ← DI 核心 ★
    ├── ConfigurableBeanFactory      ← 工厂配置
    └── ConfigurableListableBeanFactory ← 完整工厂 ★

【注册层】
  AliasRegistry · BeanDefinitionRegistry · BeanDefinitionRegistryPostProcessor ★

【扩展点层 — IoC 的灵魂】
  BeanFactoryPostProcessor ★         ← 开伙前改定义
  BeanPostProcessor ★                ← 实例化后加工
  InstantiationAwareBeanPostProcessor ← 实例化前后介入

【生命周期层】
  InitializingBean · DisposableBean · SmartInitializingSingleton
  Aware 系列 ★

【DI 消费层】
  ObjectFactory · ObjectProvider ★

【作用域 / 实例化】
  Scope · SingletonBeanRegistry · InstantiationStrategy

【Context 层扩展】
  ConfigurableApplicationContext · ApplicationContextInitializer
  ImportSelector · ImportBeanDefinitionRegistrar · Condition ★
```

★ = 高频重要

> 定位：在 [[03-速查-IoC与DI核心整合速查]] **结构 + 机制整合速查** 之后，按接口分层展开细节  
> 厨房比喻对照 → [[04-速查-Spring厨房比喻大全]]

---

## 一、容器接口族（BeanFactory 子接口）

已在 [[09-容器层-BeanFactory与Registry详解#1.3 接口继承体系]] 展开，此处作 DI 视角补充：

| 接口 | 职责 | DI / IoC 关联 |
|------|------|--------------|
| `ListableBeanFactory` | `getBeansOfType`、`getBeanNamesForType` | `@Autowired List<T>` 底层 |
| **`AutowireCapableBeanFactory`** | **`resolveDependency()`、`autowireBean()`** | **`@Autowired` 真正干活的地方** ★ |
| `ConfigurableBeanFactory` | 注册 Scope、BPP、ClassLoader | 工厂级配置 |
| `ConfigurableListableBeanFactory` | 预实例化、freeze、ignoreDependency | `preInstantiateSingletons()` |
| `HierarchicalBeanFactory` | 父子容器查找 | Web 多层 Context |
| `SingletonBeanRegistry` | 单例注册表、`registerSingleton` | 三级缓存基础 |

### AutowireCapableBeanFactory — DI 最核心接口

```java
public interface AutowireCapableBeanFactory extends BeanFactory {
    // @Autowired 字段/方法/构造器注入最终都到这里
    @Nullable Object resolveDependency(DependencyDescriptor descriptor,
            @Nullable String requestingBeanName) throws BeansException;

    void autowireBean(Object existingBean) throws BeansException;
    Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck);
    // ...
}
```

源码注释强调：这是 BeanFactory 的**扩展子接口**，正常业务代码应 stick to `BeanFactory` / `ListableBeanFactory`；框架集成代码才直接使用。

**文件**：`spring-beans/.../config/AutowireCapableBeanFactory.java`

---

## 二、扩展点接口（IoC 的灵魂）

> **总览对照** → [[19-IoC扩展点三部曲对照]]  
> Spring IoC 的「可扩展性」主要靠 BFPP + BPP；Aware 是 BPP 机制的应用模式。

### 2.1 BeanFactoryPostProcessor — 开伙前改菜谱 ★

```java
public interface BeanFactoryPostProcessor {
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException;
}
```

| 要点 | 说明 |
|------|------|
| 时机 | Bean **实例化之前** |
| 能改 | `BeanDefinition`（蓝图） |
| 不能改 | Bean 实例（改了会导致过早实例化） |
| 典型实现 | `ConfigurationClassPostProcessor`、`PropertySourcesPlaceholderConfigurer` |

源码关键约束（`BeanFactoryPostProcessor.java` L31-35）：

> A BFPP may interact with and modify bean **definitions**, but never bean **instances**.

→ 厨房比喻：[[04-速查-Spring厨房比喻大全#十、开伙前改菜谱 — BeanFactoryPostProcessor]]

→ 完整深入版：[[11-扩展点层-BeanFactoryPostProcessor详解]]

---

### 2.2 BeanDefinitionRegistryPostProcessor

BFPP 的子类型，能在**更早阶段**注册新定义：

```java
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
            throws BeansException;
}
```

| 对比 | BFPP | RegistryPostProcessor |
|------|------|----------------------|
| 改已有定义 | ✅ | ✅ |
| 注册新定义 | ❌（一般） | ✅ |
| 执行顺序 | 较晚 | 更早 |

---

### 2.3 BeanPostProcessor — 上桌前品控加料 ★

```java
public interface BeanPostProcessor {
    @Nullable default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }
    @Nullable default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}
```

| 要点 | 说明 |
|------|------|
| 时机 | Bean **实例化之后**、初始化前后 |
| 能改 | Bean **实例**（可换代理对象） |
| 典型实现 | `AutowiredAnnotationBeanPostProcessor`（`@Autowired`）、`AbstractAutoProxyCreator`（AOP） |

源码注释（`BeanPostProcessor.java` L26-29）：

> post-processors that populate beans via marker interfaces → `postProcessBeforeInitialization`  
> post-processors that wrap beans with proxies → `postProcessAfterInitialization`

→ 厨房比喻：[[04-速查-Spring厨房比喻大全#十一、上桌前加料 — BeanPostProcessor]]

→ 完整深入版：[[12-扩展点层-BeanPostProcessor详解]]

---

### 2.4 BeanPostProcessor 重要子接口

| 子接口 | 额外能力 |
|--------|---------|
| `InstantiationAwareBeanPostProcessor` | 实例化**前**干预（`postProcessBeforeInstantiation`）、属性注入前后 |
| `SmartInstantiationAwareBeanPostProcessor` | 预测 Bean 类型、循环依赖早期引用 |
| `MergedBeanDefinitionPostProcessor` | 合并 Definition 后缓存类型信息 |
| `DestructionAwareBeanPostProcessor` | 销毁前回调 |

`InstantiationAwareBeanPostProcessor` 是理解**循环依赖三级缓存**的关键扩展点之一。

---

## 三、生命周期回调接口

BeanFactory 定义的标准生命周期（[[09-容器层-BeanFactory与Registry详解#1.5 Bean 生命周期（BeanFactory 定义的标准顺序）]]）对应：

| 接口 / 机制 | 阶段 | 说明 |
|------------|------|------|
| `InitializingBean` | 初始化 | `afterPropertiesSet()` |
| `DisposableBean` | 销毁 | `destroy()` |
| `SmartInitializingSingleton` | 全部单例就绪后 | `afterSingletonsInstantiated()` |
| `@PostConstruct` / `@PreDestroy` | 初始化 / 销毁 | 由 `CommonAnnotationBeanPostProcessor` 处理 |
| init-method / destroy-method | 初始化 / 销毁 | 配置在 BeanDefinition 上 |

---

## 四、Aware 接口族 — 让 Bean 感知容器 ★

```java
public interface Aware { }  // 标记接口，无方法
```

| 接口 | 注入什么 | 生效环境 |
|------|---------|---------|
| `BeanNameAware` | Bean 名称 | BeanFactory |
| `BeanClassLoaderAware` | ClassLoader | BeanFactory |
| `BeanFactoryAware` | BeanFactory 本身 | BeanFactory |
| `ApplicationContextAware` | ApplicationContext | ApplicationContext |
| `EnvironmentAware` | Environment（Profile、配置） | ApplicationContext |
| `ResourceLoaderAware` | 资源加载器 | ApplicationContext |
| `MessageSourceAware` | 国际化 MessageSource | ApplicationContext |
| `ApplicationEventPublisherAware` | 事件发布器 | ApplicationContext |
| `EmbeddedValueResolverAware` | `${...}` 解析器 | ApplicationContext |

**设计建议**：Aware 是「拉式」获取容器资源；日常更推荐**构造器注入**（「推式」DI）。

→ 完整深入版：[[13-生命周期层-Aware体系详解]]（含 BPP 处理 Demo、架构中实现 Aware 的代表性 Bean）

ApplicationContext 独有的 Aware（步骤 6-10）见 [[09-容器层-BeanFactory与Registry详解#1.5]]、[[10-Context层-ApplicationContext详解]]。

---

## 五、DI 消费端接口

| 接口 | 职责 | 使用场景 |
|------|------|---------|
| `ObjectFactory<T>` | 延迟 `getObject()` | 避免循环依赖、延迟获取 |
| **`ObjectProvider<T>`** | 增强版 ObjectFactory | **`getIfAvailable()`、`getIfUnique()`、迭代多个候选** ★ |
| `Provider<T>` | JSR-330 | `@Autowired Provider<T>` |

```java
@Autowired
private ObjectProvider<PaymentService> paymentServices;

paymentServices.forEach(svc -> ...);                    // 所有候选
PaymentService unique = paymentServices.getIfUnique();  // 唯一或 null
PaymentService opt = paymentServices.getIfAvailable();  // 有则取，无则 null
```

比直接 `@Autowired List<T>` 更灵活，是现代 Spring 推荐的**延迟 / 可选注入**方式。

**文件**：`spring-beans/.../factory/ObjectProvider.java`

---

## 六、元数据与注册相关

| 接口 | 职责 |
|------|------|
| `BeanDefinition` | Bean 蓝图契约 ★ → [[06-元数据层-BeanDefinition三兄弟详解]] |
| `AnnotatedBeanDefinition` | 带注解元数据的 Definition（组件扫描产生） |
| `BeanDefinitionReader` | 读取配置 → 注册 Definition（XML 等） |
| `BeanDefinitionRegistry` | 注册表 ★ → [[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]] |
| `AliasRegistry` | 别名管理（Registry 的父接口） |
| `BeanNameGenerator` | 生成 Bean 名称（默认类名首字母小写） |
| `AutowireCandidateResolver` | 判断 Bean 是否可作为自动装配候选（`@Qualifier` 等） |

---

## 七、作用域与实例化

| 接口 | 职责 |
|------|------|
| `Scope` | 自定义作用域（singleton / prototype / request / session） |
| `InstantiationStrategy` | 实例化策略（CGLIB 子类、构造器选择） |
| `TypeConverter` | 类型转换（String → int 等，配合属性注入） |

`Scope` 接口定义：`get()`、`remove()`、`registerDestructionCallback()` — Web 作用域的基础。

---

## 八、Context 层扩展（注解驱动 IoC）

模块 `spring-context`，和 `@Configuration`、`@Conditional`、Spring Boot 自动配置密切相关：

| 接口 | 职责 | 典型用法 |
|------|------|---------|
| `ConfigurableApplicationContext` | 可 refresh / close 的 Context | `SpringApplication.run()` 返回类型 |
| `ApplicationContextInitializer` | Context refresh **之前**初始化 | Spring Boot 扩展点 |
| `ImportSelector` | `@Import` 选择导入哪些配置类 | 自动配置基础 |
| `DeferredImportSelector` | 延迟 Import | Spring Boot `@EnableAutoConfiguration` |
| `ImportBeanDefinitionRegistrar` | `@Import` 直接注册 BeanDefinition | 第三方 starter 注册 Bean |
| **`Condition`** | 条件装配 | **`@Conditional` 底层** ★ |
| `ConfigurationCondition` | 配置阶段条件判断 | `@ConditionalOnClass` 等 |

```java
public interface Condition {
    boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);
}
```

---

## 九、按 IoC vs DI 归类

### IoC（容器管对象）

```text
BeanFactory 体系              → 怎么拿 Bean
BeanDefinition 体系           → 蓝图是什么
BeanDefinitionRegistry        → 蓝图存哪
BeanFactoryPostProcessor      → 启动前改蓝图
BeanPostProcessor             → 创建后改实例
Scope / SingletonBeanRegistry → 生命周期与缓存
ApplicationContext            → 企业级容器编排
```

### DI（注入依赖）

```text
AutowireCapableBeanFactory      → resolveDependency() ★ 最核心
InstantiationAwareBeanPostProcessor → 构造器/属性注入介入
AutowiredAnnotationBeanPostProcessor（BPP 实现）→ @Autowired/@Value
AutowireCandidateResolver       → @Qualifier/@Primary 候选筛选
ObjectProvider / ObjectFactory  → 延迟/可选注入
Aware 系列                      → 回调式获取容器资源（次选）
```

---

## 十、典型 BPP / BFPP 实现速查

| 类 | 类型 | 干什么 |
|----|------|--------|
| `ConfigurationClassPostProcessor` | BFPP + RegistryPostProcessor | 解析 `@Configuration`、`@Bean`、`@Import` ★ |
| `PropertySourcesPlaceholderConfigurer` | BFPP | 解析 `${...}` 占位符 |
| `AutowiredAnnotationBeanPostProcessor` | BPP | 处理 `@Autowired`、`@Value`、`@Inject` ★ |
| `CommonAnnotationBeanPostProcessor` | BPP | 处理 `@PostConstruct`、`@PreDestroy`、`@Resource` |
| `AbstractAutoProxyCreator` | BPP | AOP 代理生成 ★ |
| `ApplicationContextAwareProcessor` | BPP | ApplicationContext 相关 Aware 回调 |

---

## 十一、读源码优先级

| 优先级 | 接口 / 类 | 为什么 |
|:------:|----------|--------|
| ⭐⭐⭐ | `AutowireCapableBeanFactory` | `@Autowired` 底层 |
| ⭐⭐⭐ | `BeanFactoryPostProcessor` | `@Configuration` 解析入口 |
| ⭐⭐⭐ | `BeanPostProcessor` | 初始化扩展、AOP |
| ⭐⭐ | `InstantiationAwareBeanPostProcessor` | 循环依赖、提前代理 |
| ⭐⭐ | `BeanDefinitionRegistryPostProcessor` | 动态注册 Bean |
| ⭐⭐ | `ObjectProvider` | 现代 DI 写法 |
| ⭐⭐ | `Condition` | `@Conditional` 原理 |
| ⭐ | `Aware` 系列 | 生命周期回调 → [[13-生命周期层-Aware体系详解]] |
| ⭐ | `Scope` | 自定义作用域 |

建议跟栈路线 → [[25-源码调试与断点指南]]、[[17-Bean加载原理与源码阅读路径]]

---

## 十二、源码文件速查

| 接口 | 文件路径 | 模块 |
|------|---------|------|
| `AutowireCapableBeanFactory` | `factory/config/AutowireCapableBeanFactory.java` | spring-beans |
| `BeanFactoryPostProcessor` | `factory/config/BeanFactoryPostProcessor.java` | spring-beans |
| `BeanDefinitionRegistryPostProcessor` | `factory/support/BeanDefinitionRegistryPostProcessor.java` | spring-beans |
| `BeanPostProcessor` | `factory/config/BeanPostProcessor.java` | spring-beans |
| `InstantiationAwareBeanPostProcessor` | `factory/config/InstantiationAwareBeanPostProcessor.java` | spring-beans |
| `ObjectProvider` | `factory/ObjectProvider.java` | spring-beans |
| `Scope` | `factory/config/Scope.java` | spring-beans |
| `Condition` | `context/annotation/Condition.java` | spring-context |
| `ImportSelector` | `context/annotation/ImportSelector.java` | spring-context |
| `ImportBeanDefinitionRegistrar` | `context/annotation/ImportBeanDefinitionRegistrar.java` | spring-context |

本地源码：`/Users/guoyang/IdeaProjects/spring/spring-framework`

---

## 常见面试题速答

| 问题 | 答案 |
|------|------|
| `@Autowired` 底层哪个接口？ | `AutowireCapableBeanFactory.resolveDependency()` |
| BFPP 和 BPP 区别？ | BFPP 改 Definition（实例化前）；BPP 改实例（实例化后） |
| AOP 代理在哪生成？ | BPP 的 `postProcessAfterInitialization`（`AbstractAutoProxyCreator`） |
| `@Configuration` 谁解析？ | `ConfigurationClassPostProcessor`（BFPP） |
| ObjectProvider 和 `@Autowired` 区别？ | 前者延迟/可选获取；后者启动时立即解析注入 |
| Aware 和 DI 区别？ | Aware 是容器回调「拉」资源；构造器注入是「推」依赖 |

---

## 记忆口诀

```text
Definition + Registry  = 菜谱 + 档案柜（IoC 存什么）
BeanFactory              = 厨房（IoC 怎么拿）
AutowireCapableBeanFactory = 配餐员（DI 怎么注入）★
BFPP                     = 开伙前改菜谱 ★
BPP                      = 上桌前加料 ★
Aware                    = 员工领工牌（感知容器）
ObjectProvider           = 传菜窗口（延迟/可选注入）
Condition                = 是否上架的条件（@Conditional）
ApplicationContext       = 餐厅经理（编排以上全部）
```

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[04-速查-Spring厨房比喻大全]] | [[06-元数据层-BeanDefinition三兄弟详解]] |

---

## 关联

- [[00-Spring-Bean加载-学习导航]]
- [[03-速查-IoC与DI核心整合速查]] · [[04-速查-Spring厨房比喻大全]]
- [[06-元数据层-BeanDefinition三兄弟详解]]
- [[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
- [[09-容器层-BeanFactory与Registry详解]]
- [[10-Context层-ApplicationContext详解]]
- [[11-扩展点层-BeanFactoryPostProcessor详解]] · [[12-扩展点层-BeanPostProcessor详解]] · [[13-生命周期层-Aware体系详解]]
- 下篇：[[16-IoC与DI核心概念]] · [[17-Bean加载原理与源码阅读路径]] · [[20-依赖注入实现原理]]
- [[25-源码调试与断点指南]] · [[19-IoC扩展点三部曲对照]]

---
## 下一步可深入

- [x] `ConfigurationClassPostProcessor` 源码 — BFPP 如何解析 `@Configuration` → [[11-扩展点层-BeanFactoryPostProcessor详解]]
- [ ] `AutowiredAnnotationBeanPostProcessor` — `@Autowired` 注入全流程
- [ ] `AbstractAutoProxyCreator` — AOP 代理创建时机
- [ ] 循环依赖 — `SmartInstantiationAwareBeanPostProcessor.getEarlyBeanReference()`
