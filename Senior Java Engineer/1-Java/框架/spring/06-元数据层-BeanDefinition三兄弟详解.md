# BeanDefinition 三兄弟详解

> 导航：[[00-Spring-Bean加载-学习导航]] · **上篇 01–15** · 元数据层 · 前置：[[05-接口地图-IoC与DI重要接口大全]] · [[03-速查-IoC与DI核心整合速查]]
>
> 本地源码（已加中文注释）：`/Users/guoyang/IdeaProjects/spring/spring-framework`
>
> - `spring-beans/.../config/BeanDefinition.java`
> - `spring-beans/.../support/AbstractBeanDefinition.java`
> - `spring-beans/.../support/RootBeanDefinition.java`

---

## 一句话

| 类                          | 角色                             |
| -------------------------- | ------------------------------ |
| **BeanDefinition**         | 接口契约：「创建 Bean 需要哪些信息」          |
| **AbstractBeanDefinition** | 实现层：「这些信息存在哪些字段里，以及如何合并」       |
| **RootBeanDefinition**     | 运行时：「继承已展开、类型已解析、可以拿去实例化的最终定义」 |

**类比**：BeanDefinition = 菜单接口 · AbstractBeanDefinition = 完整菜谱 · RootBeanDefinition = 后厨备好的料

---

## 类层次结构

```text
BeanDefinition (interface)
    └── AbstractBeanDefinition (abstract)
            ├── ScannedGenericBeanDefinition      ← @ComponentScan 扫描 @Service 等
            ├── AnnotatedGenericBeanDefinition      ← @Configuration 配置类本身
            ├── ConfigurationClassBeanDefinition    ← @Bean 方法产生的定义
            ├── GenericBeanDefinition               ← 通用、可设 parent
            ├── RootBeanDefinition                  ← 运行时合并结果 / @Bean 直接注册
            └── ChildBeanDefinition                 ← 旧式 parent 继承（已较少用）
```

> 注解驱动场景下，日常最常遇到的是 **ScannedGenericBeanDefinition**（组件类）和 **RootBeanDefinition / ConfigurationClassBeanDefinition**（`@Bean` 方法）。

---

## 一、BeanDefinition — 接口契约

**文件**：`spring-beans/.../config/BeanDefinition.java`

### 设计意图

接口刻意保持**最小化**，主要目的：

1. 让 `BeanFactoryPostProcessor` 在容器启动早期**内省并修改** Bean 元数据
2. 屏蔽不同来源（注解、`@Bean`、编程式注册）的差异，统一抽象
3. 允许不同实现类承载不同阶段的语义

### 核心属性分类

| 类别 | 代表方法 | 含义 |
|------|----------|------|
| **身份** | `beanClassName` | 要实例化的类 |
| **作用域** | `scope` / `isSingleton()` / `isPrototype()` | singleton 或 prototype |
| **构造** | `constructorArgumentValues` | 构造器参数 |
| **属性注入** | `propertyValues` | setter / 字段注入的值 |
| **工厂方法** | `factoryBeanName` + `factoryMethodName` | 通过工厂方法创建（`@Bean`） |
| **生命周期** | `initMethodName` / `destroyMethodName` | 初始化 / 销毁回调 |
| **依赖关系** | `dependsOn` | 必须先初始化的 Bean |
| **自动装配** | `autowireCandidate` / `primary` / `fallback` | 是否参与 `@Autowired` 候选 |
| **继承** | `parentName` | 父 Bean 定义（注解场景较少用） |
| **元数据** | `role` / `description` / `lazyInit` | 角色提示、描述、延迟初始化 |

### 角色常量

```java
ROLE_APPLICATION   = 0  // 用户业务 Bean（@Service 等）
ROLE_SUPPORT       = 1  // 辅助配置 Bean
ROLE_INFRASTRUCTURE = 2  // 基础设施 Bean（框架内部）
```

### 接口本身不存数据

`BeanDefinition` 只有方法签名，**没有字段**。所有实际数据都在 `AbstractBeanDefinition` 及子类中。

---

## 二、AbstractBeanDefinition — 数据载体

**文件**：`spring-beans/.../support/AbstractBeanDefinition.java`

完整实现了 `BeanDefinition` 接口，并额外提供注解 / 编程式注册阶段需要的属性。

### 相比接口，多了哪些关键字段？

| 分组 | 字段 | 注解场景下的作用 |
|------|------|-----------------|
| **类信息** | `beanClass` | 存 String 类名或已解析的 `Class` |
| **作用域** | `scope`, `lazyInit`, `backgroundInit` | `@Scope`、`@Lazy` |
| **自动装配** | `autowireCandidate`, `primary`, `fallback`, `qualifiers` | `@Primary`、`@Qualifier` |
| **创建方式** | `instanceSupplier`, `factoryBeanName`, `factoryMethodName` | `@Bean` 工厂方法 |
| **注入数据** | `constructorArgumentValues`, `propertyValues` | 构造器 / 属性注入元数据 |
| **生命周期** | `initMethodNames`, `destroyMethodNames` | `@PostConstruct` / `@PreDestroy` 等 |
| **元数据** | `synthetic`, `role`, `resource` | AOP 代理、来源追溯 |

### 注解场景下的关键能力

#### 1. `beanClass` 双重存储

```java
setBeanClassName("com.example.UserService");  // 存 String，延迟加载
setBeanClass(UserService.class);              // 存 Class，已解析
```

组件扫描阶段通常先存类名；后处理或实例化前通过 `resolveBeanClass(classLoader)` 解析。

#### 2. `instanceSupplier` — 现代创建方式

```java
// @Bean 解析后可能设置 Supplier
RootBeanDefinition bd = new RootBeanDefinition(UserService.class, () -> new UserService());
```

#### 3. `qualifiers` — 对应 @Qualifier

```java
addQualifier(new AutowireCandidateQualifier(Qualifier.class, "primary"));
```

扫描 `@Service("primary")` 或 `@Qualifier("xxx")` 时会写入此 Map。

#### 4. `overrideFrom()` — 合并算法（parent 继承时用）

| 属性 | 合并规则 |
|------|----------|
| `beanClass` | 子定义有则覆盖 |
| `scope`, `lazyInit`, `autowireMode` 等 | **总是**取子定义 |
| `constructorArgumentValues`, `propertyValues` | **追加**合并 |
| `factoryMethodName`, `init/destroy` | 子定义有则覆盖 |

> 注解驱动开发中 parent 继承较少见，但 `getMergedBeanDefinition()` 内部仍依赖此逻辑。

#### 5. 为什么是 abstract？

**parent 语义因实现而异**：

| 实现类 | parent 行为 |
|--------|-------------|
| `GenericBeanDefinition` / `ScannedGenericBeanDefinition` | 可自由设置 `parentName` |
| `RootBeanDefinition` | **禁止** parent，`setParentName` 抛异常 |

---

## 三、RootBeanDefinition — 运行时最终版

**文件**：`spring-beans/.../support/RootBeanDefinition.java`

继承 `AbstractBeanDefinition` 的全部属性，增加**运行时专用**的缓存与语义约束。

### "Root" 的含义

- **不是**「根 Bean」或「最重要的 Bean」
- **是** parent 链合并后的**根节点定义**：所有继承已展开，定义自包含
- `getParentName()` 永远返回 `null`

### 相比 AbstractBeanDefinition，多了什么？

#### 1. 运行时解析缓存

| 字段 | 作用 |
|------|------|
| `targetType` / `resolvedTargetType` | 含泛型的目标类型 |
| `factoryMethodToIntrospect` | 已解析的 `@Bean` 工厂方法 |
| `factoryMethodReturnType` | 工厂方法返回类型 |
| `resolvedConstructorOrFactoryMethod` | 已选定的构造器或工厂方法 |
| `resolvedConstructorArguments` | 已解析的构造器参数 |
| `isFactoryBean` | 是否为 FactoryBean（详见 [[14-工厂Bean-BeanFactory与FactoryBean的区别]]） |
| `stale` | 定义过期，需重新合并 |

避免每次 `getBean()` 重复反射，单例 Bean 通常只解析一次。

#### 2. 增强的类型解析

```java
// RootBeanDefinition.getResolvableType() 优先级：
targetType → factoryMethodReturnType → 工厂方法推断 → beanClass
```

对 `@Bean` 返回泛型类型（如 `List<String>`）的精确 `@Autowired` 注入至关重要。

#### 3. 外部管理的生命周期

```java
registerExternallyManagedInitMethod("init");       // @PostConstruct
registerExternallyManagedDestroyMethod("destroy"); // @PreDestroy
registerExternallyManagedConfigMember(field);      // @Autowired 字段
```

标记「由注解/框架管理」的成员，避免与显式配置的 init/destroy 冲突。

#### 4. 装饰定义追溯

`decoratedDefinition` 保存 AOP、Scoped Proxy 等包装前的原始定义。

---

## 四、注解驱动 IoC 中的完整流程

```text
@Configuration / @ComponentScan 启动
    ↓
ClassPathBeanDefinitionScanner.doScan()
    → ScannedGenericBeanDefinition          // @Service、@Repository 等
    ↓
ConfigurationClassPostProcessor
    → AnnotatedGenericBeanDefinition        // @Configuration 类本身
    → ConfigurationClassBeanDefinition      // @Bean 方法（extends RootBeanDefinition）
    ↓
registerBeanDefinition() 写入 Registry
    ↓
BeanFactoryPostProcessor 修改元数据（可选）
    ↓
getMergedBeanDefinition(beanName)
    → RootBeanDefinition（合并 + 缓存）
    ↓
createBean(mbd) → 实例化
```

### 场景 A：组件类 @Service

```java
@Service
public class UserService {
    @Autowired
    private UserRepository repo;
}
```

| 阶段 | BeanDefinition 类型 | 关键属性 |
|------|---------------------|----------|
| 扫描注册 | `ScannedGenericBeanDefinition` | `beanClass=UserService`, `scope=singleton` |
| 合并 | `RootBeanDefinition` | 拷贝 + 缓存 |
| 实例化 | 调用无参或 `@Autowired` 构造器 | `resolvedConstructorOrFactoryMethod` 被填充 |

**产生类**：`ClassPathBeanDefinitionScanner` → `AnnotatedBeanDefinitionReader`

> Reader / Scanner 详解 → [[07-元数据层-AnnotatedBeanDefinitionReader与组件注册详解]]

### 场景 B：@Bean 工厂方法

```java
@Configuration
public class AppConfig {
    @Bean
    public UserService userService(UserDao dao) {
        return new UserService(dao);
    }
}
```

| 阶段 | BeanDefinition 类型 | 关键属性 |
|------|---------------------|----------|
| 解析 @Bean | `ConfigurationClassBeanDefinition` | `factoryBeanName="appConfig"`, `factoryMethodName="userService"` |
| 后处理 | 同上 + 缓存 | `factoryMethodToIntrospect`, `targetType=UserService` |
| 实例化 | 调用 `appConfig.userService(dao)` | 方法参数从容器自动装配 |

**产生类**：`ConfigurationClassBeanDefinitionReader.loadBeanDefinitionsForBeanMethod()`

核心代码逻辑：

```java
// 实例 @Bean 方法
beanDef.setFactoryBeanName(configClass.getBeanName());  // "appConfig"
beanDef.setUniqueFactoryMethodName(methodName);          // "userService"
beanDef.setResolvedFactoryMethod(method);                // 已解析的 Method 对象
```

### 场景 C：@Configuration 配置类本身

```java
@Configuration
public class AppConfig { ... }
```

| 阶段 | BeanDefinition 类型 | 说明 |
|------|---------------------|------|
| 扫描 | `AnnotatedGenericBeanDefinition` | 带完整注解元数据 |
| 后处理 | CGLIB 增强 | `ConfigurationClassEnhancer` 保证 `@Bean` 单例 |

---

## 五、getMergedBeanDefinition() 合并逻辑

**文件**：`spring-beans/.../support/AbstractBeanFactory.java`

```java
if (bd.getParentName() == null) {
    // 无 parent：直接转为 RootBeanDefinition
    mbd = (bd instanceof RootBeanDefinition) ? bd.cloneBeanDefinition() : new RootBeanDefinition(bd);
} else {
    // 有 parent：递归合并
    pbd = getMergedBeanDefinition(parentBeanName);
    mbd = new RootBeanDefinition(pbd);
    mbd.overrideFrom(bd);   // AbstractBeanDefinition 的合并算法
}
cacheMergedBeanDefinition(mbd, beanName);
```

注解场景下大多数 Bean **无 parent**，直接 `new RootBeanDefinition(bd)` 即可。

### stale 机制

BFPP 修改注册表中的原始定义后，已缓存的 `RootBeanDefinition` 会被标记 `stale=true`，下次 `getBean()` 时重新合并。

---

## 六、三兄弟对比总表

| 维度 | BeanDefinition | AbstractBeanDefinition | RootBeanDefinition |
|------|----------------|------------------------|-------------------|
| **类型** | interface | abstract class | concrete class |
| **数据存储** | 无字段 | 全部配置字段 | 继承 + 运行时缓存 |
| **parent** | 接口方法 | 子类各自实现 | 禁止，恒为 null |
| **使用阶段** | 全程抽象引用 | 配置注册 | 合并后实例化 |
| **注解典型来源** | — | 扫描 / @Bean 解析 | 合并产生或 @Bean 直接注册 |
| **BFPP 修改** | 通过接口 | 直接改字段 | 一般不改（stale 重合并） |
| **类型解析** | 接口声明 | 仅 `beanClass` | 工厂方法泛型 + 缓存 |
| **谁用它** | BFPP 内省 | 注册阶段 | `createBean()` 实例化 |

---

## 七、注解场景下的实现类速查

| 实现类 | 产生来源 | 典型注解 |
|--------|----------|----------|
| `ScannedGenericBeanDefinition` | 组件扫描 | `@Component`、`@Service`、`@Repository`、`@Controller` |
| `AnnotatedGenericBeanDefinition` | 注解注册 | `@Configuration`（配置类本身） |
| `ConfigurationClassBeanDefinition` | @Bean 解析 | `@Bean` 方法（继承 RootBeanDefinition） |
| `RootBeanDefinition` | 合并 / 编程式 | `getMergedBeanDefinition()` 产出 |

---

## 八、源码阅读建议

### 推荐阅读顺序

```text
1. BeanDefinition.java          → 理解接口契约（有哪些元数据）
2. AbstractBeanDefinition.java → 理解字段存储（数据存在哪）
3. RootBeanDefinition.java      → 理解运行时缓存（合并后多了什么）
4. ConfigurationClassBeanDefinitionReader.java → @Bean 如何生成定义
5. ClassPathBeanDefinitionScanner.java           → 组件扫描如何生成定义
6. AbstractBeanFactory.getMergedBeanDefinition() → 合并时机
```

### 带着问题读

1. `@Service` 扫描后注册的是哪种 BeanDefinition？
2. `@Bean` 方法的 `factoryBeanName` 指向谁？
3. `getBean("userService")` 时用的是注册表里的定义，还是合并后的 `RootBeanDefinition`？
4. `@Primary` 写入 BeanDefinition 的哪个字段？
5. BFPP 修改 `propertyValues` 后，为什么已缓存的合并定义需要 `stale` 重合并？

### 断点位置

| 关注点 | 断点类 / 方法 |
|--------|--------------|
| 组件扫描注册 | `ClassPathBeanDefinitionScanner.doScan()` |
| @Bean 注册 | `ConfigurationClassBeanDefinitionReader.loadBeanDefinitionsForBeanMethod()` |
| 合并定义 | `AbstractBeanFactory.getMergedBeanDefinition()` |
| 实例化入口 | `AbstractAutowireCapableBeanFactory.createBean()` |

详见 [[25-源码调试与断点指南]]。

---

## 记忆口诀

- **BeanDefinition**：接口说「要什么信息」
- **AbstractBeanDefinition**：抽象类存「信息在哪」
- **RootBeanDefinition**：运行时给「可以直接炒的菜」
- **ScannedGenericBeanDefinition**：扫描来的组件
- **ConfigurationClassBeanDefinition**：@Bean 工厂方法

---

## 常见误区

| 误区 | 正解 |
|------|------|
| BeanDefinition 就是 Bean 实例 | BeanDefinition 是蓝图，实例是 `getBean()` 后才有的对象 |
| RootBeanDefinition = 根 Bean | Root = 合并链的根节点，不是业务上的「根依赖」 |
| @Autowired 走 autowireMode | 注解注入走 `AutowiredAnnotationBeanPostProcessor`，与 XML 的 autowireMode 是两套机制 |
| 注册表存的就是 RootBeanDefinition | 注册表存原始定义；`getMergedBeanDefinition()` 才产出 RootBeanDefinition |
| @Bean 和普通类用同一种 Definition | @Bean 用 `ConfigurationClassBeanDefinition`，带 factoryMethod 元数据 |

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[05-接口地图-IoC与DI重要接口大全]] | [[07-元数据层-AnnotatedBeanDefinitionReader与组件注册详解]] |

---

## 关联

- [[00-Spring-Bean加载-学习导航]]
- [[16-IoC与DI核心概念]]
- [[17-Bean加载原理与源码阅读路径]]
- [[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
- [[03-速查-IoC与DI核心整合速查]]
- [[01-注解入门-配置类与组件类]]
- [[02-注解入门-Configuration与Service等注解区别]]
- [[25-源码调试与断点指南]]
