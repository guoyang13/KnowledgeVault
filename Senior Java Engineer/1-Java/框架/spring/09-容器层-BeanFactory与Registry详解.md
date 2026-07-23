# BeanFactory 与 Registry 详解

> 导航：[[00-Spring-Bean加载-学习导航]] · **上篇 01–15** · 容器层
>
> 前置：[[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]] · [[05-接口地图-IoC与DI重要接口大全]]
>
> 关联：[[10-Context层-ApplicationContext详解]] · [[14-工厂Bean-BeanFactory与FactoryBean的区别]] · [[03-速查-IoC与DI核心整合速查]]
>
> Registry / DLBF 源码级阅读 → [[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
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
       ↓ registerBeanDefinition()
BeanDefinitionRegistry（注册表）
       ↓ getBean()
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

源码注释（`BeanFactory.java` L23-31）：

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
4. DI 基于 BeanFactory 实现，但推荐「推式」注入，而非主动 `getBean()` 拉取

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
UserService opt = provider.getIfAvailable();
```

#### 查询元信息

| 方法 | 说明 |
|------|------|
| `containsBean(name)` | 是否存在 |
| `isSingleton(name)` / `isPrototype(name)` | 作用域 |
| `isTypeMatch(name, type)` | 类型是否匹配 |
| `getType(name)` | 获取 Bean 类型 |
| `getAliases(name)` | 获取别名 |

#### FactoryBean 前缀

> 完整说明 → [[14-工厂Bean-BeanFactory与FactoryBean的区别]] · 接口体系 → [[15-工厂Bean-FactoryBean接口体系详解]]

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
| `ConfigurableListableBeanFactory` | 预实例化、freezeConfiguration | `refresh()` 阶段 |

> **注意**：`ListableBeanFactory` 的方法**不查父容器**，需用 `BeanFactoryUtils.beanNamesForTypeIncludingAncestors()`。

### 1.4 getBean 核心流程 — doGetBean()

所有 `getBean()` 最终进入 `AbstractBeanFactory.doGetBean()`（L242）：

```text
getBean(name)
  → doGetBean(name, ...)
      ├── transformedBeanName(name)        // 处理 & 前缀、别名
      ├── getSingleton(beanName)           // 1. 查单例缓存
      │     └── 命中 → getObjectForBeanInstance()（处理 FactoryBean）
      ├── 本地无定义 → 委托 parentBeanFactory
      ├── getMergedLocalBeanDefinition()   // 2. 合并定义
      ├── 处理 depends-on 依赖
      └── 按 scope 创建
            ├── singleton → getSingleton(name, () -> createBean(...))
            ├── prototype → createBean(...)
            └── scoped    → Scope.get(...)
                  → createBean()
                    → doCreateBean()
                      ├── createBeanInstance()  [实例化]
                      ├── populateBean()        [依赖注入]
                      └── initializeBean()      [初始化]
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
实例化 createBeanInstance()
  → 属性注入 populateBean()          ← @Autowired 在此
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

触发时机：容器关闭时 `ConfigurableBeanFactory.destroySingletons()` → 逐个单例 Bean 销毁。

→ 完整机制（登记时机、为何需要、Context.close() vs JVM 退出）：[[24-Bean销毁机制详解]]

### 1.6 BeanFactory vs ApplicationContext

| | BeanFactory | ApplicationContext |
|--|-------------|-------------------|
| 定位 | IoC 核心 | 企业级容器 |
| 加载 | **延迟加载**（lazy） | 启动时预实例化非 lazy 单例 |
| 功能 | Bean 生命周期 | + 事件、国际化、Environment、AOP |
| 继承 | — | `ListableBeanFactory` + 更多 |
| 典型实现 | `DefaultListableBeanFactory` | `AnnotationConfigApplicationContext` |

> 完整说明 → [[10-Context层-ApplicationContext详解]]

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
| `getBean("xxx")` | 普通 Bean → 实例本身 | FactoryBean → `getObject()` 产品 |
| 获取工厂本身 | — | `getBean("&xxx")` |

```text
BeanFactory 专题阅读顺序：
  06 Registry + DefaultListableBeanFactory
  → 07 BeanFactory 接口体系（本文）
  → 08 ApplicationContext
  → 09 BeanFactory vs FactoryBean
  → 10 FactoryBean 接口体系
```

详见 [[14-工厂Bean-BeanFactory与FactoryBean的区别]]、[[15-工厂Bean-FactoryBean接口体系详解]]。

---

## 二、BeanDefinitionRegistry — 定义注册表

> 深度阅读：[[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解#二、BeanDefinitionRegistry 详解]]

### 2.1 定位

`BeanDefinitionRegistry` 是 Spring Bean 工厂包中**唯一封装 Bean 定义注册**的接口。

源码注释（L33-38）：

```java
/**
 * This is the only interface in Spring's bean factory packages that encapsulates
 * registration of bean definitions. The standard BeanFactory interfaces
 * only cover access to a fully configured factory instance.
 */
public interface BeanDefinitionRegistry extends AliasRegistry {
```

关键理解：

- 标准 `BeanFactory` 只提供 `getBean()`，**不提供注册定义的能力**
- XML/注解/扫描等 Reader 最终都调用 `registerBeanDefinition()` 写入容器
- 已知实现：`DefaultListableBeanFactory`、`GenericApplicationContext`

### 2.2 继承 AliasRegistry

还负责**别名**管理：

```java
registerAlias("userService", "us");  // us 也能找到 userService
isBeanNameInUse("us");               // 检查名称/别名是否已被占用
```

### 2.3 核心 API

| 方法 | 作用 |
|------|------|
| `registerBeanDefinition(name, def)` | 注册/覆盖 Bean 定义 |
| `removeBeanDefinition(name)` | 移除定义 |
| `getBeanDefinition(name)` | 获取**原始**定义（未合并） |
| `containsBeanDefinition(name)` | 是否存在定义 |
| `getBeanDefinitionNames()` | 所有定义名称 |
| `getBeanDefinitionCount()` | 定义数量 |
| `isBeanDefinitionOverridable(name)` | 是否允许覆盖（默认 true） |
| `isBeanNameInUse(name)` | 名称/别名是否已被使用 |

### 2.4 谁在使用

```text
@ComponentScan / @Configuration
        ↓
ClassPathBeanDefinitionScanner / ConfigurationClassBeanDefinitionReader
        ↓
registerBeanDefinition("userService", GenericBeanDefinition)
        ↓
DefaultListableBeanFactory.beanDefinitionMap
```

---

## 三、DefaultListableBeanFactory — 默认完整实现

> 深度阅读：[[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解#三、DefaultListableBeanFactory 详解]]

### 3.1 定位

```java
/**
 * Spring's default implementation of the ConfigurableListableBeanFactory
 * and BeanDefinitionRegistry interfaces: a full-fledged bean factory
 * based on bean definition metadata, extensible through post-processors.
 */
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
        implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {
```

同时承担：

1. **定义管理**（`BeanDefinitionRegistry`）
2. **Bean 创建/注入/销毁**（继承 `AbstractAutowireCapableBeanFactory`）
3. **按类型列举**（`ListableBeanFactory`）
4. **依赖自动装配**（`AutowireCapableBeanFactory`）
5. **工厂配置**（`ConfigurableBeanFactory`）

典型用法：**先注册所有 Bean 定义，再访问 Bean**。按名称查找是在本地定义表上的低开销操作。

### 3.2 继承链与职责

```text
DefaultListableBeanFactory
  ├── 定义注册、按类型查找、依赖解析（本类新增）
  └── AbstractAutowireCapableBeanFactory
        └── 自动装配（构造器/属性/方法）
      └── AbstractBeanFactory
            └── getBean()、依赖注入、BeanPostProcessor
          └── FactoryBeanRegistrySupport
                └── FactoryBean 处理
              └── DefaultSingletonBeanRegistry
                    └── 单例缓存（三级缓存）
```

| 层次 | 职责 |
|------|------|
| `DefaultSingletonBeanRegistry` | 单例池、`singletonObjects` 三级缓存 |
| `AbstractBeanFactory` | `doGetBean()`、合并定义 |
| `AbstractAutowireCapableBeanFactory` | `@Autowired`、构造器注入 |
| `DefaultListableBeanFactory` | 定义表、类型索引、候选筛选 |

### 3.3 核心数据结构

| 字段 | 用途 |
|------|------|
| `beanDefinitionMap` | **原始定义**存储，`ConcurrentHashMap<name, BeanDefinition>` |
| `beanDefinitionNames` | 保持**注册顺序**（影响 `@DependsOn`、初始化顺序） |
| `mergedBeanDefinitionHolders` | **合并后定义**缓存（处理 parent 继承） |
| `allBeanNamesByType` / `singletonBeanNamesByType` | **按类型查找**缓存，配置冻结后加速 |
| `primaryBeanNamesWithType` | 加速 `@Primary` 冲突检测 |
| `resolvableDependencies` | 特殊依赖（如 `BeanFactory` 自身）的自动注入 |
| `manualSingletonNames` | 手动 `registerSingleton()` 的单例 |
| `frozenBeanDefinitionNames` | 配置冻结时的名称快照 |
| `configurationFrozen` | 是否允许缓存所有 Bean 元数据 |

```java
// DefaultListableBeanFactory.java L216-232
private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);
private final Map<String, BeanDefinitionHolder> mergedBeanDefinitionHolders = new ConcurrentHashMap<>(256);
private volatile List<String> beanDefinitionNames = new ArrayList<>(256);
```

### 3.4 registerBeanDefinition 流程

`DefaultListableBeanFactory.registerBeanDefinition()`（L1296）：

```text
registerBeanDefinition(name, def)
  ├── 1. 校验（AbstractBeanDefinition.validate()）
  ├── 2. 同名定义已存在
  │     ├── 不允许覆盖 → BeanDefinitionOverrideException
  │     └── 允许覆盖 → 替换 + logBeanDefinitionOverriding()
  ├── 3. beanName 本身是别名 → 处理别名冲突 / removeAlias()
  ├── 4. 写入 beanDefinitionMap + beanDefinitionNames
  │     └── Bean 创建已开始 → synchronized 保证迭代稳定
  └── 5. 清理缓存
        ├── 已有实例 → resetBeanDefinition()（清合并缓存、销毁单例）
        └── 配置已冻结 → clearByTypeCache()
```

覆盖策略：`allowBeanDefinitionOverriding` 默认 **true**（Spring Boot 2.1+ 默认 **false**）。

`resetBeanDefinition()` 会：

- 清除合并定义缓存
- 销毁已有单例
- 通知 `MergedBeanDefinitionPostProcessor`
- 递归重置以该 Bean 为 parent 的定义

### 3.5 按类型查找（ListableBeanFactory）

`getBeanNamesForType(UserService.class)` 逻辑：

1. 配置已冻结 → 先查 `allBeanNamesByType` 缓存
2. 遍历 `beanDefinitionNames`，对每个定义：
   - 跳过 abstract
   - 检查 lazy-init / FactoryBean
   - 调用 `isTypeMatch()` 判断类型
3. 同时检查 `manualSingletonNames`

这是 `@Autowired List<UserService>`、`getBeansOfType()` 的基础。

### 3.6 依赖解析 — resolveDependency

`@Autowired` 注入最终调用 `doResolveDependency()`：

```text
Step 1: @Autowired 指定的 shortcut
Step 2: @Value 等预设值
Step 3: 按名称/Qualifier 直接匹配
Step 4: 多 Bean 场景（Collection/Map/Stream/数组）
Step 5: findAutowireCandidates() 找候选
Step 6: determineAutowireCandidate() 选唯一候选
         → @Primary → 名称匹配 → @Priority → defaultCandidate
```

`findAutowireCandidates()` 流程：

1. 查 `resolvableDependencies`（如注入 `ApplicationContext`）
2. 按类型找所有候选，过滤自引用、非 autowire candidate
3. 支持 fallback 和 self-reference 兜底

多 Bean 冲突决策顺序：

```text
@Primary → 名称/Qualifier 匹配 → @Priority → defaultCandidate → NoUniqueBeanDefinitionException
```

### 3.7 预实例化单例 — preInstantiateSingletons

容器 `refresh()` 最后阶段调用（L1111）：

```text
preInstantiateSingletons()
  → 遍历 beanDefinitionNames
    → 非 abstract 且 singleton
      → lazy-init=false → getBean() 立即创建
      → backgroundInit=true → 后台线程异步创建（CompletableFuture）
  → SmartInitializingSingleton.afterSingletonsInstantiated()
```

### 3.8 配置冻结 — freezeConfiguration

```java
freezeConfiguration() {
    this.configurationFrozen = true;
    this.frozenBeanDefinitionNames = ...; // 快照
}
```

冻结后：

- `getBeanDefinitionNames()` 返回快照数组
- 按类型查找结果可被缓存
- 元数据缓存对所有 Bean 生效

对应 `ApplicationContext.refresh()` 中 `finishBeanFactoryInitialization()` 之前。

---

## 四、三者协作 — 完整生命周期

以 Spring Boot 启动为例：

```text
[阶段一：注册定义]
  ComponentScan / @Configuration 解析
    → BeanDefinitionRegistry.registerBeanDefinition()
    → beanDefinitionMap 写入

[阶段二：修改定义]
  invokeBeanFactoryPostProcessors()
    → BeanFactoryPostProcessor 可修改 BeanDefinition

[阶段三：冻结 + 实例化]
  freezeConfiguration()
  preInstantiateSingletons()
    → BeanFactory.getBean()
      → doGetBean() → createBean() → doCreateBean()

[阶段四：运行时]
  按需 getBean()（lazy / prototype）
  @Autowired → resolveDependency()
```

| 阶段 | Registry 角色 | Factory 角色 |
|------|--------------|-------------|
| 解析配置 | 接收 `registerBeanDefinition` | 暂存定义 |
| BFPP | 可被修改定义 | 提供 `getBeanDefinition` |
| 冻结配置 | 名称快照 | 开启缓存 |
| 实例化 | — | `getBean()` 创建对象 |
| 运行时 | 一般不再注册 | 按需创建 lazy/prototype |

---

## 五、关键设计思想

### 5.1 定义与实例分离

- `BeanDefinition` = 蓝图（可修改、可合并、可覆盖）
- Bean 实例 = 运行时对象（缓存在 `singletonObjects`）

### 5.2 先注册、后创建

所有 Reader 先把定义写入 Registry，再统一实例化。`BeanFactoryPostProcessor` 可在创建前修改定义。

### 5.3 合并定义（Merged BeanDefinition）

`ChildBeanDefinition` 可继承 parent 属性。创建 Bean 时用的是**合并后的 `RootBeanDefinition`**，不是原始定义。详见 [[06-元数据层-BeanDefinition三兄弟详解]]。

### 5.4 两级名称索引

- `beanDefinitionMap`：O(1) 按名查找定义
- `allBeanNamesByType`：避免每次按类型遍历全部定义

### 5.5 客户端 vs 框架内部接口

| 视角 | 接口 | 说明 |
|------|------|------|
| **客户端** | `BeanFactory` / `ListableBeanFactory` | 业务代码、`@Autowired` 底层 |
| **框架内部** | `ConfigurableListableBeanFactory` / `BeanDefinitionRegistry` | 容器启动、Reader、Scanner |

---

## 六、代码示例

```java
DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

// 1. 注册定义（Registry 能力）
GenericBeanDefinition def = new GenericBeanDefinition();
def.setBeanClass(UserService.class);
def.setScope(BeanDefinition.SCOPE_SINGLETON);
factory.registerBeanDefinition("userService", def);

// 2. 查询定义
BeanDefinition bd = factory.getBeanDefinition("userService");
assert factory.getBeanDefinitionCount() == 1;

// 3. 创建实例（Factory 能力）
UserService user = factory.getBean("userService", UserService.class);

// 4. 按类型查找（Listable 能力）
String[] names = factory.getBeanNamesForType(UserService.class);

// 5. 自动装配（Autowire 能力）
// @Autowired UserService → factory.resolveDependency(...)
```

---

## 七、日常开发中的接触方式

日常几乎不直接使用 `BeanFactory`，但底层都是它：

```java
// 1. 注入 ApplicationContext（本质是 ListableBeanFactory）
@Autowired
private ApplicationContext context;

// 2. @Autowired 底层
// → AutowireCapableBeanFactory.resolveDependency()
// → DefaultListableBeanFactory.doResolveDependency()

// 3. 按类型获取所有实现
@Autowired
private List<PaymentService> paymentServices;
// → getBeanNamesForType(PaymentService.class)

// 4. 框架内部注册定义
// → BeanDefinitionRegistry.registerBeanDefinition()
// → 之后 getBean() 才能拿到实例
```

---

## 八、概念速查

| 概念 | 一句话 | 源码位置 |
|------|--------|---------|
| `BeanFactory` | IoC 根接口，定义怎么拿 Bean | `factory/BeanFactory.java` |
| `ListableBeanFactory` | 按类型/注解批量列举 | `factory/ListableBeanFactory.java` |
| `AutowireCapableBeanFactory` | 自动装配、`resolveDependency` | `config/AutowireCapableBeanFactory.java` |
| `BeanDefinitionRegistry` | 注册 Bean 定义的唯一接口 | `support/BeanDefinitionRegistry.java` |
| `DefaultListableBeanFactory` | 以上全部的默认实现 | `support/DefaultListableBeanFactory.java` |
| `doGetBean()` | getBean 真正干活的方法 | `support/AbstractBeanFactory.java` L242 |
| `beanDefinitionMap` | 原始定义存储 | `DefaultListableBeanFactory` L217 |
| `registerBeanDefinition()` | 定义注册入口 | `DefaultListableBeanFactory` L1296 |
| `preInstantiateSingletons()` | 预实例化非 lazy 单例 | `DefaultListableBeanFactory` L1111 |
| `resolveDependency()` | `@Autowired` 底层 | `DefaultListableBeanFactory` L1639 |

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]] | [[10-Context层-ApplicationContext详解]] |

> FactoryBean 专题：[[14-工厂Bean-BeanFactory与FactoryBean的区别]] → [[15-工厂Bean-FactoryBean接口体系详解]]
>
> ApplicationContext 专题：[[10-Context层-ApplicationContext详解]]

## 关联

- [[00-Spring-Bean加载-学习导航]]
- [[16-IoC与DI核心概念]]
- [[17-Bean加载原理与源码阅读路径]]
- [[06-元数据层-BeanDefinition三兄弟详解]]
- [[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
- [[14-工厂Bean-BeanFactory与FactoryBean的区别]]
- [[15-工厂Bean-FactoryBean接口体系详解]]
- [[10-Context层-ApplicationContext详解]]
- [[03-速查-IoC与DI核心整合速查]]
- [[04-速查-Spring厨房比喻大全]]
- [[05-接口地图-IoC与DI重要接口大全]]
- [[25-源码调试与断点指南]]
- [[100-Q&A/未被使用的类Spring如何处理]]
- [[20-依赖注入实现原理]]

---
## 下一步可深入

- [ ] 循环依赖 — 三级缓存、`addSingletonFactory` → [[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
- [ ] FactoryBean vs 普通 Bean — `&` 前缀 → [[14-工厂Bean-BeanFactory与FactoryBean的区别]]
- [ ] SmartFactoryBean / AbstractFactoryBean → [[15-工厂Bean-FactoryBean接口体系详解]]
- [ ] `BeanPostProcessor` — 在 `populateBean` / `initializeBean` 中的介入点
- [ ] `@Primary` / `@Qualifier` — `determineAutowireCandidate` 源码
