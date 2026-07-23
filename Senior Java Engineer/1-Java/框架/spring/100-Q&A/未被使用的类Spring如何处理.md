# 未被使用的类 Spring 如何处理

> 导航：[[00-Spring-Bean加载-学习导航]] · **100-Q&A** · 容器行为 · 前置：[[15-工厂Bean-FactoryBean接口体系详解]] · [[07-元数据层-AnnotatedBeanDefinitionReader与组件注册详解]]

---

## 先分清两种「没有被使用」

| 情况 | 含义 | Spring 是否感知 |
|------|------|----------------|
| **情况一** | 类在 classpath 里，但**没有注册成 Bean** | 否，完全忽略 |
| **情况二** | 类**已注册成 Bean**，但没有被注入 / 调用 | 是，默认仍会创建 |

Spring **不会做死代码分析**：只认「有没有注册成 Bean」，不认「有没有被业务代码用到」。

---

## 情况一：类根本没有注册成 Bean

```java
public class UnusedHelper {
    public void help() { }
}
```

### Spring 的处理：完全不管

- 组件扫描只处理 stereotype 注解或 `@Bean` 显式注册的类
- 不匹配 filter 的类直接跳过，容器里没有 `BeanDefinition`
- 手动 `new UnusedHelper()` 与 Spring 无关

### 源码依据

**文件**：`spring-context/.../ClassPathScanningCandidateComponentProvider.java`

```text
isCandidateComponent(metadataReader)
  ├── 匹配 include filter → 候选
  └── 不匹配 → trace："Ignored because not matching any filter"
```

默认 include filter（`ClassPathBeanDefinitionScanner.registerDefaultFilters()`）：

`@Component` · `@Repository` · `@Service` · `@Controller`

---

## 情况二：已是 Bean，但没人注入 / 调用

```java
@Service
public class OrphanService {  // 注册了，但没有别的 Bean 依赖它
}
```

### Spring 的处理：默认照样创建

容器启动时**预实例化所有非 lazy 单例 Bean**，与是否被引用无关。

### 源码依据

**文件**：`spring-beans/.../DefaultListableBeanFactory.java`

```text
finishBeanFactoryInitialization()
  → preInstantiateSingletons()
      → preInstantiateSingleton(beanName, mbd)
          → if (!mbd.isLazyInit()) instantiateSingleton(beanName)
                → getBean() → doCreateBean()
```

详见 [[17-Bean加载原理与源码阅读路径#非 lazy 单例的 eager 创建]]。

---

## 不同配置下的行为对照

| 配置 | 没人用时 Spring 的行为 |
|------|------------------------|
| 默认单例（无 `@Lazy`） | **启动时创建**，实例留在容器里 |
| `@Lazy` / `lazy-init="true"` | **不创建**，直到第一次 `getBean()` |
| `@Scope("prototype")` | 只有调用 `getBean()` 时才创建 |
| `@Conditional` 条件不满足 | 连 `BeanDefinition` 都不会注册 |
| 抽象 Bean（`abstract="true"`） | 只作为模板，不会被实例化 |

---

## 整体决策流程

```text
类在 classpath 中
    ├─ 是否注册为 Bean？
    │     ├─ 否 → Spring 完全忽略
    │     └─ 是 ↓
    ├─ 是否 lazy-init？
    │     ├─ 是 → 只存 BeanDefinition，首次 getBean 才创建
    │     └─ 否 ↓
    ├─ 启动时创建实例（preInstantiateSingletons）
    └─ 有没有被注入/调用？
          ├─ 没有 → 实例仍在容器中，无额外处理
          └─ 有   → 正常使用
```

---

## 容易混淆的点

| 点 | 说明 |
|----|------|
| 「没被使用」≠「不会创建」 | 非 lazy 单例启动时走完整生命周期，即使无人 `@Autowired` |
| `@PostConstruct` 仍会执行 | Bean 被创建就会触发，可能造成多余初始化 |
| 手动 `new` | Spring 不管理，不注入、不参与生命周期 |
| 依赖链间接使用 | A 注入 B，则 B 因 A 被创建而「被用到」 |
| `@Bean` 产出的孤儿 | 与 `@Service` 注册的孤儿 Bean 行为相同 |

---

## 实际建议

| 场景 | 建议 |
|------|------|
| 不需要 Spring 管理的工具类 | 保持普通类，不加注解 |
| 注册了但暂时不用 | 加 `@Lazy` |
| 确认容器里有哪些 Bean | `context.getBeanDefinitionNames()` |
| 排查启动慢 | 检查大量非 lazy 单例被 eager 创建 |

```java
String[] names = context.getBeanDefinitionNames();
Arrays.sort(names);
for (String name : names) System.out.println(name);
```

---

## 一句话总结

> 没注册的类直接忽略；注册了的默认单例 Bean，启动时就会创建，除非显式设为 lazy。

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[15-工厂Bean-FactoryBean接口体系详解]] | [[16-IoC与DI核心概念]] |

---

## 关联

- [[00-Spring-Bean加载-学习导航]]
- [[16-IoC与DI核心概念]]
- [[17-Bean加载原理与源码阅读路径]]
- [[25-源码调试与断点指南]]
- [[20-依赖注入实现原理]]
