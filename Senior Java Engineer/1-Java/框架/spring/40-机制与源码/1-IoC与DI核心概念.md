---
type: canonical
status: reviewed
topic: Spring IoC / Dependency Injection
source_version: 6.2.x
---

# IoC 与 DI 核心概念

> 导航：[[00-Spring-Framework核心机制-学习导航]] · **40 · 机制与源码** · IoC/DI 机制 · 推荐首读下篇
>
> 速查整合（思想 + 结构五概念 + 机制）→ [[2-速查-IoC与DI核心整合速查]] · 本文展开机制与源码视角

---

## 一句话

| 概念 | 含义 |
|------|------|
| **IoC（Inversion of Control，控制反转）** | 设计思想：对象的创建、组装、生命周期由**容器**管理，而不是业务代码自己 `new` |
| **DI（Dependency Injection，依赖注入）** | 实现手段：容器把依赖**注入**到对象中（构造器、Setter、字段等） |

**IoC 是「谁控制对象」，DI 是「怎么把依赖给对象」。**

---

## 传统方式 vs IoC/DI

### 传统写法（主动控制）

```java
public class UserService {
    private UserRepository repo = new UserRepository; // 自己创建依赖
}
```

问题：强耦合、难测试、配置分散。

### IoC + DI（控制反转）

```java
public class UserService {
    private final UserRepository repo;

    public UserService(UserRepository repo) {  // 容器注入
        this.repo = repo;
    }
}
```

对象不再自己 `new` 依赖，而是由 **Spring 容器**负责创建并注入。

---

## IoC 与 DI 的关系

```text
IoC（思想）
  └── 控制反转：创建和管理对象的责任交给容器
        └── DI（实现）
              ├── 构造器注入
              ├── Setter 注入
              └── 接口回调（BeanFactoryAware 等）
```

IoC 是大原则；DI 是 Spring 实现 IoC 的主要方式。Spring 更推荐 DI 这种「推式（push）配置」，而不是业务代码里主动 `getBean` 的「拉式（pull）配置」。

---

## DI 的三种常见方式

| 方式 | 示例 | 推荐度 |
|------|------|--------|
| **构造器注入** | `public Foo(Bar bar)` | ⭐ **Spring 官方推荐**，依赖不可变、便于测试 |
| **Setter 注入** | `setBar(Bar bar)` | 可选依赖 |
| **字段注入** | `@Autowired private Bar bar` | 简洁，但不利于单元测试 |

→ 完整对比（@Resource、@Inject、字段注入为何不推荐、`getBean(Class)` 链路）：[[5-依赖注入实现原理]]

---

## 在 Spring 源码中的体现

### 1. IoC 容器 — `BeanFactory`

**模块**：`spring-beans` · **包**：`org.springframework.beans.factory`

`package-info.java`：

> The core package implementing Spring's lightweight Inversion of Control (IoC) container.

`BeanFactory` 核心要点：中央注册表、集中管理配置、DI 通过子接口实现。

> 接口体系、`BeanDefinitionRegistry`、`DefaultListableBeanFactory` 详解 → [[3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]] · [[4-容器层-BeanFactory接口体系详解]]
>
> `BeanFactory` vs `FactoryBean`、FactoryBean 接口体系 → [[5-工厂Bean-BeanFactory与FactoryBean的区别]] · [[4-工厂Bean-FactoryBean接口体系详解]]
>
> ApplicationContext → [[5-Context层-ApplicationContext详解]]
>
> 五概念 + 机制整合速查 → [[2-速查-IoC与DI核心整合速查]]
>
> 厨房比喻（面点师 / 主厨 / 传菜口等）→ [[3-速查-Spring厨房比喻大全]]
>
> IoC/DI 重要接口（BFPP / BPP / Aware / ObjectProvider 等）→ [[4-接口地图-IoC与DI重要接口大全]]

### 2. Bean 元数据 — `BeanDefinition`

**文件**：`spring-beans/.../config/BeanDefinition.java`

| 实现类 | 作用 |
|--------|------|
| `BeanDefinition` | 接口契约，定义 Bean 元数据读写 |
| `AbstractBeanDefinition` | 抽象实现，承载全部配置字段 |
| `RootBeanDefinition` | 运行时合并后的完整定义 |
| `ScannedGenericBeanDefinition` | 组件扫描产生的定义（`@Service` 等） |
| `ConfigurationClassBeanDefinition` | `@Bean` 方法产生的定义 |

> 三兄弟详解见 [[1-元数据层-BeanDefinition三兄弟详解]]。
>
> DI 实现原理（populateBean / resolveDependency）→ [[5-依赖注入实现原理]]

### 3. DI 实现 — `AbstractAutowireCapableBeanFactory`

| 方法 | 作用 |
|------|------|
| `populateBean` | 属性填充（`@Autowired`、`@Value` 等） |
| `autowireConstructor` | 构造器自动装配 |
| `resolveDependency` | 解析单个依赖，找到匹配的 Bean |

---

## 整体流程

```text
配置源（XML / @Configuration / 组件扫描）
    ↓
BeanDefinition 注册到 BeanFactory
    ↓
getBean 触发创建
    ↓
createBeanInstance  →  populateBean  →  initializeBean
   [实例化]              [DI]               [初始化]
    ↓
就绪的 Bean 实例
```

详见 [[2-Bean加载原理与源码阅读路径]] 中的 `doCreateBean` 调用链。

→ DI 完整实现原理 → [[5-依赖注入实现原理]]

---

## 与 Bean 加载两阶段的关系

| 阶段 | IoC/DI 视角 | 关键词 |
|------|-------------|--------|
| **阶段一：注册 BeanDefinition** | 容器知道「要创建什么、依赖谁」 | 元数据、蓝图 |
| **阶段二：实例化 Bean** | 容器创建对象并完成 DI | 对象、生命周期 |

---

## 记忆口诀

- **IoC**：对象的命交给容器管
- **DI**：容器把依赖塞给你
- **BeanDefinition**：做菜的菜谱
- **BeanFactory**：厨房

---

## 常见误区

| 误区 | 正解 |
|------|------|
| IoC 和 DI 是一回事 | IoC 是思想，DI 是实现方式之一 |
| IoC 只有 DI 一种实现 | Service Locator（`getBean`）也算 IoC，但 Spring 更推荐 DI |
| 加了 `@Autowired` 就是 IoC | DI 是 IoC 的实现；IoC 还包括生命周期、作用域等 |
| `@Component` 就是 IoC | `@Component` 只是让类被扫描注册；IoC 是容器整体机制 |

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[4-工厂Bean-FactoryBean接口体系详解]] | [[2-Bean加载原理与源码阅读路径]] |

---

## 关联

- [[00-Spring-Framework核心机制-学习导航]]
- [[1-注解入门-配置类与组件类]]
- [[2-Bean加载原理与源码阅读路径]]
- [[1-元数据层-BeanDefinition三兄弟详解]]
- [[4-容器层-BeanFactory接口体系详解]]
- [[3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
- [[5-Context层-ApplicationContext详解]]
- [[2-速查-IoC与DI核心整合速查]]
- [[3-速查-Spring厨房比喻大全]]
- [[4-接口地图-IoC与DI重要接口大全]]
- [[5-依赖注入实现原理]]
- [[2-元数据层-AnnotatedBeanDefinitionReader与组件注册详解#FAQ：类存在但没有被使用，Spring 会怎样处理？]]
- [[手写源码专题/Spring/02-手写实现IoC模块功能/01-创建对象代码演变及IoC思路分析]]
