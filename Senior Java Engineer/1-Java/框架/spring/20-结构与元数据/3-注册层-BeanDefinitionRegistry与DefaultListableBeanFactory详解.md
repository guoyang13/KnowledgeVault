---
type: canonical
status: reviewed
topic: Spring BeanDefinitionRegistry / DefaultListableBeanFactory
source_version: 6.2.x
---

# BeanDefinitionRegistry 与 DefaultListableBeanFactory 详解

> 导航：[[00-Spring-Framework核心机制-学习导航]] · **20 · 结构与元数据** · 注册层
>
> 前置：[[2-元数据层-AnnotatedBeanDefinitionReader与组件注册详解]] · [[1-元数据层-BeanDefinition三兄弟详解]]
>
> 关联：[[4-容器层-BeanFactory接口体系详解]]（BeanFactory 接口体系）· [[5-工厂Bean-BeanFactory与FactoryBean的区别]] · [[4-工厂Bean-FactoryBean接口体系详解]] · [[5-Context层-ApplicationContext详解]]
>
> 本地源码：`/Users/guoyang/IdeaProjects/spring/spring-framework`
>
> 核心文件：
> - `spring-beans/.../support/BeanDefinitionRegistry.java`
> - `spring-beans/.../support/DefaultListableBeanFactory.java`

---

## 一句话

| 类/接口 | 职责 |
|---------|------|
| `BeanDefinitionRegistry` | 管 **Bean 蓝图** — 注册、移除、查询 `BeanDefinition` |
| `DefaultListableBeanFactory` | **完整工厂** — 既存定义，又能创建 / 注入 / 销毁 Bean |

**Registry 管「定义」，Factory 管「生命周期」。** `DefaultListableBeanFactory` 同时实现了两者。

```text
BeanDefinition（蓝图）
       ↓ registerBeanDefinition
BeanDefinitionRegistry（注册表）
       ↓ getBean
DefaultListableBeanFactory（完整工厂）
       → 实例化 + DI + 初始化
```

---

## 一、整体关系

```text
┌─────────────────────────────────────────────────────────────┐
│  BeanDefinitionRegistry（接口）                              │
│    registerBeanDefinition / removeBeanDefinition / get...    │
│    extends AliasRegistry（别名管理）                          │
├─────────────────────────────────────────────────────────────┤
│  DefaultListableBeanFactory（实现）                          │
│    implements BeanDefinitionRegistry                         │
│    implements ConfigurableListableBeanFactory                │
│    extends AbstractAutowireCapableBeanFactory                │
└─────────────────────────────────────────────────────────────┘
         ↑ 写入定义                          ↑ 创建实例
   XmlBeanDefinitionReader              getBean
   ClassPathBeanDefinitionScanner       resolveDependency
   ConfigurationClassBeanDefinitionReader
```

| 概念 | 含义 |
|------|------|
| `BeanDefinition` | Bean 的**元数据 / 蓝图**（类名、作用域、属性、构造器参数等） |
| `BeanDefinitionRegistry` | **注册表**：管理所有 `BeanDefinition` 的增删查 |
| `DefaultListableBeanFactory` | **工厂 + 注册表**：既存定义，又能创建 Bean |

---

## 二、BeanDefinitionRegistry 详解

### 2.1 定位

`BeanDefinitionRegistry` 是 Spring Bean 工厂包中**唯一封装 Bean 定义注册**的接口。

源码（`BeanDefinitionRegistry.java`）：

```java
/**
 * 这是 Spring Bean 工厂包中唯一封装 Bean 定义 registration 的接口。
 * 标准的 BeanFactory 接口仅覆盖对「已完全配置的工厂实例」的访问。
 *
 * Spring 的 Bean 定义读取器期望在此接口的实现上工作。
 * 已知实现：DefaultListableBeanFactory、GenericApplicationContext。
 */
public interface BeanDefinitionRegistry extends AliasRegistry {
```

**关键理解：**

- 标准 `BeanFactory` 只提供 `getBean`，**不提供注册定义的能力**
- `BeanDefinitionRegistry` 是 Spring 中**唯一专门做「注册」的接口**
- XML / 注解 / 扫描等 Reader 最终都调用 `registerBeanDefinition` 写入容器

### 2.2 继承 AliasRegistry

`BeanDefinitionRegistry extends AliasRegistry`，还负责**别名**管理：

| AliasRegistry 方法 | 作用 |
|-------------------|------|
| `registerAlias(name, alias)` | 注册别名 |
| `removeAlias(alias)` | 移除别名 |
| `isAlias(name)` | 是否为别名 |
| `getAliases(name)` | 获取别名列表 |

```java
registerAlias("userService", "us");  // 用 us 也能找到 userService
isBeanNameInUse("us");               // 检查名称/别名是否已被占用
```

### 2.3 核心 API 逐方法说明

| 方法 | 作用 | 注意点 |
|------|------|--------|
| `registerBeanDefinition(name, def)` | 注册 / 覆盖 Bean 定义 | 必须支持 `RootBeanDefinition`、`ChildBeanDefinition` |
| `removeBeanDefinition(name)` | 移除定义 | 不存在则抛 `NoSuchBeanDefinitionException` |
| `getBeanDefinition(name)` | 获取**原始**定义（未合并） | 返回永不为 null |
| `containsBeanDefinition(name)` | 是否存在定义 | 仅查定义，不含手动注册的单例 |
| `getBeanDefinitionNames` | 所有定义名称 | 按注册顺序 |
| `getBeanDefinitionCount` | 定义数量 | — |
| `isBeanDefinitionOverridable(name)` | 是否允许覆盖 | 默认 `true`（since 6.1） |
| `isBeanNameInUse(name)` | 名称 / 别名是否已被使用 | 含本地 Bean 和别名 |

### 2.4 覆盖策略 — isBeanDefinitionOverridable

```java
// BeanDefinitionRegistry 默认实现
default boolean isBeanDefinitionOverridable(String beanName) {
    return true;
}

// DefaultListableBeanFactory 实际委托给
public boolean isBeanDefinitionOverridable(String beanName) {
    return isAllowBeanDefinitionOverriding;  // 默认 true
}
```

| 场景 | 行为 |
|------|------|
| `allowBeanDefinitionOverriding = true`（Spring 默认） | 同名定义被新定义替换 |
| `allowBeanDefinitionOverriding = false`（Spring Boot 2.1+ 默认） | 抛 `BeanDefinitionOverrideException` |
| 框架定义覆盖用户定义（role 更低） | 记录 info 日志，允许覆盖 |

### 2.5 谁在使用 Registry

```text
@ComponentScan / @Configuration
        ↓
ClassPathBeanDefinitionScanner
ConfigurationClassBeanDefinitionReader
XmlBeanDefinitionReader
        ↓
registerBeanDefinition("userService", GenericBeanDefinition)
        ↓
DefaultListableBeanFactory.beanDefinitionMap
```

| 调用方 | 模块 | 场景 |
|--------|------|------|
| `ClassPathBeanDefinitionScanner` | spring-context | `@ComponentScan` |
| `ConfigurationClassBeanDefinitionReader` | spring-context | `@Bean` 方法 |
| `XmlBeanDefinitionReader` | spring-beans | XML 配置 |
| `GenericApplicationContext` | spring-context | 对外暴露 Registry 能力 |

---

## 三、DefaultListableBeanFactory 详解

### 3.1 定位

```java
/**
 * ConfigurableListableBeanFactory 和 BeanDefinitionRegistry 的 Spring 默认实现：
 * 基于 Bean 定义元数据的完整 Bean 工厂，可通过后置处理器扩展。
 *
 * 典型用法：先注册所有 Bean 定义，再访问 Bean。
 * 按名称查找是在本地定义表上的低开销操作。
 */
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
        implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {
```

同时承担 **5 类职责**：

1. **定义管理**（`BeanDefinitionRegistry`）
2. **Bean 创建 / 注入 / 销毁**（继承 `AbstractAutowireCapableBeanFactory`）
3. **按类型列举**（`ListableBeanFactory`）
4. **依赖自动装配**（`AutowireCapableBeanFactory`）
5. **工厂配置**（`ConfigurableBeanFactory`）

### 3.2 继承链与职责分工

```text
DefaultListableBeanFactory
  ├── 本类新增：定义注册、按类型查找、依赖解析、候选筛选
  └── AbstractAutowireCapableBeanFactory
        └── 自动装配（@Autowired、构造器/属性/方法注入）
      └── AbstractBeanFactory
            └── getBean、doGetBean、合并定义、BeanPostProcessor
          └── FactoryBeanRegistrySupport
                └── FactoryBean 处理（& 前缀）
              └── DefaultSingletonBeanRegistry
                    └── 单例三级缓存（singletonObjects 等）
```

| 层次 | 核心职责 | 关键方法 |
|------|---------|---------|
| `DefaultSingletonBeanRegistry` | 单例池、三级缓存 | `getSingleton`、`addSingletonFactory` |
| `AbstractBeanFactory` | getBean 编排 | `doGetBean`、`getMergedLocalBeanDefinition` |
| `AbstractAutowireCapableBeanFactory` | 创建 + 注入 | `createBean`、`doCreateBean`、`populateBean` |
| `DefaultListableBeanFactory` | 定义表 + 类型索引 | `registerBeanDefinition`、`findAutowireCandidates` |

### 3.3 核心数据结构

```java
// DefaultListableBeanFactory.java

/** Bean 定义对象映射，以 Bean 名称为键 */
private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

/** Bean 名称到合并后 BeanDefinitionHolder 的映射 */
private final Map<String, BeanDefinitionHolder> mergedBeanDefinitionHolders = new ConcurrentHashMap<>(256);

/** 带 primary 标记的 Bean 定义名称及其对应类型 */
private final Map<String, Class<?>> primaryBeanNamesWithType = new ConcurrentHashMap<>(16);

/** 单例与非单例 Bean 名称，以依赖类型为键（按类型查找缓存） */
private final Map<Class<?>, String[]> allBeanNamesByType = new ConcurrentHashMap<>(64);
private final Map<Class<?>, String[]> singletonBeanNamesByType = new ConcurrentHashMap<>(64);

/** Bean 定义名称列表，按注册顺序排列 */
private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

/** 手动注册的单例名称，按注册顺序 */
private volatile Set<String> manualSingletonNames = new LinkedHashSet<>(16);

/** 配置冻结时缓存的 Bean 定义名称数组 */
private volatile String[] frozenBeanDefinitionNames;

/** 是否允许缓存所有 Bean 的定义元数据 */
private volatile boolean configurationFrozen;
```

| 字段 | 用途 | 何时写入 / 读取 |
|------|------|----------------|
| `beanDefinitionMap` | **原始定义**存储 | `registerBeanDefinition` 写入；`getBeanDefinition` 读取 |
| `beanDefinitionNames` | 保持**注册顺序** | 影响 `@DependsOn`、初始化顺序、`preInstantiateSingletons` 遍历顺序 |
| `mergedBeanDefinitionHolders` | **合并后定义**缓存 | `getMergedLocalBeanDefinition` 时填充；parent 继承在此合并 |
| `allBeanNamesByType` | 按类型查找缓存 | `freezeConfiguration` 后可缓存；注册/移除定义时清除 |
| `primaryBeanNamesWithType` | `@Primary` 冲突加速 | 注册 primary 定义时写入；`hasPrimaryConflict` 读取 |
| `resolvableDependencies` | 特殊依赖自动注入 | 如 `BeanFactory`、`ResourceLoader` 自身 |
| `manualSingletonNames` | 手动 `registerSingleton` | 不走 BeanDefinition 的直接单例 |
| `allowBeanDefinitionOverriding` | 是否允许定义覆盖 | 默认 null → 视为 true |

### 3.4 registerBeanDefinition — 源码级流程

实现位置：`DefaultListableBeanFactory.registerBeanDefinition`

```text
registerBeanDefinition(beanName, beanDefinition)
  │
  ├─ 1. 校验
  │     AbstractBeanDefinition.validate
  │     失败 → BeanDefinitionStoreException
  │
  ├─ 2. 同名定义已存在？（existingDefinition != null）
  │     ├─ !isBeanDefinitionOverridable(name)
  │     │     → BeanDefinitionOverrideException
  │     └─ 允许覆盖
  │           → logBeanDefinitionOverriding
  │           → beanDefinitionMap.put(name, def)
  │
  ├─ 3. 新定义：beanName 本身是别名？
  │     ├─ 别名指向已有定义且不可覆盖 → BeanDefinitionOverrideException
  │     ├─ 别名指向不存在的定义 → BeanDefinitionStoreException
  │     └─ 允许覆盖 → removeAlias(beanName)
  │
  ├─ 4. 写入存储
  │     beanDefinitionMap.put(name, def)
  │     beanDefinitionNames.add(name)   // 保持注册顺序
  │     │
  │     └── hasBeanCreationStarted == true（Bean 创建已开始）
  │           → synchronized(beanDefinitionMap)
  │           → 复制 beanDefinitionNames 再 add（保证迭代稳定）
  │
  ├─ 5. 清理缓存
  │     frozenBeanDefinitionNames = null
  │     │
  │     ├─ existingDefinition != null 或 containsSingleton(name)
  │     │     → resetBeanDefinition(name)
  │     └─ isConfigurationFrozen
  │           → clearByTypeCache
  │
  └─ 6. primary 标记
        beanDefinition.isPrimary → primaryBeanNamesWithType.put(name, Void.class)
```

**resetBeanDefinition 做什么**（覆盖 / 移除定义后触发）：

```text
resetBeanDefinition(beanName)
  ├── clearMergedBeanDefinition(beanName)     // 清合并缓存
  ├── destroySingleton(beanName)            // 销毁已有单例
  ├── primaryBeanNamesWithType.remove(name)  // 清 primary 标记
  ├── MergedBeanDefinitionPostProcessor.resetBeanDefinition
  └── 递归：以 beanName 为 parent 的所有定义也 reset
```

### 3.5 按类型查找 — getBeanNamesForType

`getBeanNamesForType(UserService.class)` 核心逻辑（`doGetBeanNamesForType`，）：

```text
getBeanNamesForType(type)
  │
  ├─ 配置已冻结 && allowEagerInit？
  │     → 先查 allBeanNamesByType / singletonBeanNamesByType 缓存
  │
  └─ doGetBeanNamesForType
        ├── 遍历 beanDefinitionNames
        │     ├── 跳过 alias
        │     ├── 跳过 abstract 定义
        │     ├── 检查 lazy-init / FactoryBean
        │     ├── isTypeMatch(beanName, type)
        │     └── FactoryBean 特殊处理（&beanName 前缀）
        │
        └── 遍历 manualSingletonNames（手动注册的单例）
```

用途：

- `@Autowired List<UserService>` 注入
- `getBeansOfType(UserService.class)`
- `findAutowireCandidates` 的前置步骤

### 3.6 依赖解析 — resolveDependency

`@Autowired` 注入最终调用 `doResolveDependency`：

```text
doResolveDependency(descriptor, beanName, autowiredBeanNames, typeConverter)
  │
  ├─ Step 1: shortcut（@Autowired 指定 beanName）
  ├─ Step 2: @Value 等预设值
  ├─ Step 3: 按 dependencyName / Qualifier 直接匹配
  ├─ Step 4a: 多 Bean — Stream / 数组 / Collection / Map
  ├─ Step 4b: findAutowireCandidates 找候选
  ├─ Step 4c: fallback — 自定义 Collection / Map 声明
  │
  └─ Step 5-6: 唯一候选选择
        determineAutowireCandidate
          → @Primary
          → dependencyName / suggestedName 匹配
          → @Priority（数值越小优先级越高）
          → defaultCandidate
          → 直接注册的 resolvableDependency
```

**findAutowireCandidates 三步兜底**：

```text
1. 正常候选：!isSelfReference && isAutowireCandidate
2. fallback 候选：matchesBeanName || hasQualifier
3. 自引用兜底：isSelfReference（Collection 场景除外）
```

### 3.7 预实例化单例 — preInstantiateSingletons

`refresh` 最后阶段调用：

```text
preInstantiateSingletons
  │
  ├─ preInstantiationThread = MAIN
  ├─ mainThreadPrefix = 当前线程前缀（锁策略用）
  │
  ├─ 遍历 beanDefinitionNames 副本
  │     └── 非 abstract && singleton
  │           ├── backgroundInit + bootstrapExecutor
  │           │     → 主线程先创建 depends-on
  │           │     → CompletableFuture 后台 createBean
  │           └── !lazyInit
  │                 → instantiateSingleton → getBean
  │
  └─ SmartInitializingSingleton.afterSingletonsInstantiated
```

### 3.8 配置冻结 — freezeConfiguration

```java
@Override
public void freezeConfiguration {
    clearMetadataCache;
    this.configurationFrozen = true;
    this.frozenBeanDefinitionNames = StringUtils.toStringArray(this.beanDefinitionNames);
}
```

| 冻结后变化 | 说明 |
|-----------|------|
| `getBeanDefinitionNames` | 返回 `frozenBeanDefinitionNames` 快照，不再复制 List |
| `allBeanNamesByType` | 按类型查找结果可被缓存 |
| `isBeanEligibleForMetadataCaching` | 所有 Bean 元数据可缓存 |
| 新注册定义 | 仍可行，但会 `clearByTypeCache` |

对应 `ApplicationContext.refresh` → `finishBeanFactoryInitialization` 之前。

### 3.9 重要配置项

| 配置 | 字段 / 方法 | 默认值 | 作用 |
|------|------------|--------|------|
| 允许定义覆盖 | `allowBeanDefinitionOverriding` | true | 同名定义是否可替换 |
| 允许 eager 类加载 | `allowEagerClassLoading` | true | lazy-init Bean 是否也加载类（按类型查找时） |
| 依赖排序 | `dependencyComparator` | null | List / 数组注入时的 `@Order` 排序 |
| 自动装配候选解析 | `autowireCandidateResolver` | SimpleAutowireCandidateResolver | `@Qualifier` 等解析 |
| 严格锁 | `spring.locking.strict` | 推断 | 6.2+ 预实例化阶段的线程锁策略 |
| 后台初始化 | `bootstrapExecutor` | null | backgroundInit Bean 的异步创建线程池 |

---

## 四、完整生命周期中的协作

以 Spring Boot 启动为例：

```text
AnnotationConfigApplicationContext 启动
  │
  ├─ [阶段一：注册定义]
  │     ComponentScan / @Configuration 解析
  │       → BeanDefinitionRegistry.registerBeanDefinition
  │       → beanDefinitionMap 写入
  │
  ├─ [阶段二：修改定义]
  │     invokeBeanFactoryPostProcessors
  │       → BeanFactoryPostProcessor 可 get/modify BeanDefinition
  │
  ├─ [阶段三：注册后置处理器]
  │     registerBeanPostProcessors
  │
  ├─ [阶段四：冻结 + 实例化]
  │     freezeConfiguration
  │     preInstantiateSingletons
  │       → getBean → doGetBean → createBean → doCreateBean
  │
  └─ [阶段五：运行时]
        按需 getBean（lazy / prototype）
        @Autowired → resolveDependency
```

| 阶段 | Registry 角色 | DefaultListableBeanFactory 角色 |
|------|--------------|----------------------------------|
| 解析配置 | 接收 `registerBeanDefinition` | 写入 `beanDefinitionMap` |
| BFPP 阶段 | 定义可被修改 | 提供 `getBeanDefinition` |
| 冻结配置 | 名称快照 | 开启类型缓存 |
| 实例化 | — | `getBean` 创建对象 |
| 运行时 | 一般不再注册 | 按需创建 lazy / prototype |

---

## 五、与 ApplicationContext 的关系

```text
AnnotationConfigApplicationContext
    └── 内部持有 DefaultListableBeanFactory
            ├── 作为 BeanDefinitionRegistry（注册 @Component 等）
            └── 作为 ConfigurableListableBeanFactory（创建 Bean）
```

ApplicationContext 在 Factory 之上增加了：

- Environment（环境变量、Profile）
- ResourceLoader（资源加载）
- ApplicationEventPublisher（事件发布）
- MessageSource（国际化）
- AOP 自动代理等

但 **Bean 的创建逻辑最终都落在 `DefaultListableBeanFactory`**。

> 完整说明 → [[5-Context层-ApplicationContext详解]]

---

## 六、关键设计思想

### 6.1 定义与实例分离

| | BeanDefinition | Bean 实例 |
|--|---------------|----------|
| 本质 | 蓝图（可修改、可合并、可覆盖） | 运行时对象 |
| 存储 | `beanDefinitionMap` | `singletonObjects`（三级缓存） |
| 时机 | 启动阶段注册 | 首次 `getBean` 或预实例化时创建 |

### 6.2 先注册、后创建

所有 Reader 先把定义写入 Registry，再统一实例化。这样 `BeanFactoryPostProcessor` 有机会在创建前修改定义。

### 6.3 合并定义（Merged BeanDefinition）

`ChildBeanDefinition` 可继承 parent 的属性。创建 Bean 时用的是**合并后的 `RootBeanDefinition`**，不是 `beanDefinitionMap` 中的原始定义。详见 [[1-元数据层-BeanDefinition三兄弟详解]]。

### 6.4 两级名称索引

- `beanDefinitionMap`：O(1) 按名查找定义
- `allBeanNamesByType`：避免每次按类型遍历全部定义（冻结后生效）

### 6.5 候选筛选链

多 Bean 冲突时的决策顺序：

```text
@Primary → 名称/Qualifier 匹配 → @Priority → defaultCandidate → NoUniqueBeanDefinitionException
```

---

## 七、对比总结

| | BeanDefinitionRegistry | DefaultListableBeanFactory |
|--|------------------------|---------------------------|
| 类型 | 接口 | 具体类 |
| 职责 | 管理 Bean **定义** | 管理定义 + **创建/注入/销毁** Bean |
| 核心操作 | register / remove / get 定义 | getBean / autowire / preInstantiate |
| 核心存储 | 概念上的定义表 | `beanDefinitionMap` + 单例缓存 |
| 使用场景 | Reader、Scanner 写入定义 | 整个 IoC 容器运行时 |
| 典型实现者 | DLBF、GenericApplicationContext | DLBF（唯一完整默认实现） |

---

## 八、代码示例

```java
DefaultListableBeanFactory factory = new DefaultListableBeanFactory;

// ── Registry 能力 ──

GenericBeanDefinition def = new GenericBeanDefinition;
def.setBeanClass(UserService.class);
def.setScope(BeanDefinition.SCOPE_SINGLETON);
factory.registerBeanDefinition("userService", def);

BeanDefinition bd = factory.getBeanDefinition("userService");
assert factory.getBeanDefinitionCount == 1;
assert factory.containsBeanDefinition("userService");

// ── Factory 能力 ──

UserService user = factory.getBean("userService", UserService.class);

// ── Listable 能力 ──

String[] names = factory.getBeanNamesForType(UserService.class);
Map<String, UserService> all = factory.getBeansOfType(UserService.class);

// ── Autowire 能力 ──
// @Autowired UserService → factory.resolveDependency(...)
```

---

## 九、源码阅读建议

| 优先级 | 方法 | 文件位置 | 回答的问题 |
|:------:|------|---------|-----------|
| ⭐⭐⭐ | `registerBeanDefinition` | DLBF | 定义怎么进容器？ |
| ⭐⭐⭐ | `preInstantiateSingletons` | DLBF | 非 lazy 单例何时创建？ |
| ⭐⭐ | `doGetBeanNamesForType` | DLBF | 按类型怎么找 Bean？ |
| ⭐⭐ | `doResolveDependency` | DLBF | @Autowired 怎么解析？ |
| ⭐⭐ | `resetBeanDefinition` | DLBF | 覆盖定义后缓存怎么清？ |
| ⭐ | `freezeConfiguration` | DLBF | 配置冻结做了什么？ |

> BeanFactory 接口体系、doGetBean 完整链路 → [[4-容器层-BeanFactory接口体系详解]]

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[2-元数据层-AnnotatedBeanDefinitionReader与组件注册详解]] | [[4-容器层-BeanFactory接口体系详解]] |

---

## 关联

- [[00-Spring-Framework核心机制-学习导航]]
- [[2-Bean加载原理与源码阅读路径]]
- [[1-元数据层-BeanDefinition三兄弟详解]]
- [[4-容器层-BeanFactory接口体系详解]]
- [[5-工厂Bean-BeanFactory与FactoryBean的区别]]
- [[4-工厂Bean-FactoryBean接口体系详解]]
- [[5-Context层-ApplicationContext详解]]
- [[2-速查-IoC与DI核心整合速查]]
- [[3-速查-Spring厨房比喻大全]]
- [[4-接口地图-IoC与DI重要接口大全]]
- [[1-源码调试与断点指南]]
- [[2-元数据层-AnnotatedBeanDefinitionReader与组件注册详解#FAQ：类存在但没有被使用，Spring 会怎样处理？]]

---
## 下一步可深入

- [ ] `registerBeanDefinition` 与 `getBean` 的完整调用链
- [ ] 按类型查找 + `@Primary` 候选筛选源码
- [ ] 三级单例缓存与循环依赖处理
- [ ] `BeanFactoryPostProcessor` 如何修改已注册的 BeanDefinition
