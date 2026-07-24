# Aware 体系总结与常见问题

> 导航：[[00-Spring-Bean加载-学习导航]] · **100-Q&A**
>
> 完整深入：[[13-生命周期层-Aware体系详解]] · 比喻速查：[[04-速查-Spring厨房比喻大全#十二、知情权登记 — Aware 接口族]]

---

## 一句话

**Aware = 容器 Push 给 Bean 的基础设施回调；`@Autowired` = 容器 Push 给 Bean 的业务依赖。**

---

## 总结速查

| 维度 | 说明 |
|------|------|
| 是什么 | 空标记 `Aware` + `setXxx()`；实现本身**不**自动注入 |
| 谁处理 | `invokeAwareMethods`（BeanFactory 级）+ `ApplicationContextAwareProcessor`（Context 级 BPP） |
| 何时 | DI 之后、`@PostConstruct` 之前 |
| 谁用 | 框架 Bean；业务 `@Service` **不用** |

```text
new → @Autowired → invokeAwareMethods → BPP BeforeInit（Aware）→ @PostConstruct → AOP
```

---

## 为什么需要 Aware？

有一类 Bean 初始化时必须拿到**容器本身**（BeanFactory、beanName、ClassLoader），且：

- 不能靠普通 DI（例如 AAP 自己就是处理 `@Autowired` 的，不能 `@Autowired BeanFactory` 自举）
- 不宜 Bean 主动 `getBean()` 拉取

→ Spring 用固定生命周期 **Push `setXxx()`**。

---

## instanceof Aware 是什么？

```java
if (bean instanceof Aware) { ... }
```

**仅当** Bean 类 `implements BeanNameAware` / `BeanFactoryAware` 等子接口时为 true。

| Bean | 结果 |
|------|------|
| `TestBean`（普通 POJO） | false，跳过 |
| `AutowiredAnnotationBeanPostProcessor` | true → `setBeanFactory` |

---

## BeanNameAware 原理

- **谁调**：`invokeAwareMethods(beanName, bean)` → `setBeanName(beanName)`
- **传什么**：`doCreateBean` 的注册名（非业务 id）
- **谁用**：`GenericFilterBean`、`DefaultAdvisorAutoProxyCreator`、`PlaceholderConfigurerSupport` 等框架类
- **业务**：一般不实现

---

## AAP 与 resolveDependency

### setBeanFactory 作用

保存 `ConfigurableListableBeanFactory`，供后续 `resolveDependency` 使用；并创建 `MetadataReaderFactory`。

### 为何不用 @Autowired？

```text
要注入 AAP 的 BeanFactory → 需要 AAP 已就绪 → 需要 AAP 已有 BeanFactory → 循环
```

Aware 由容器在 `initializeBean` 步骤 1 **直接** `setBeanFactory`，不经过 `resolveDependency`。

### resolveDependency 作用

**为一个注入点算出应填入的对象**（`@Autowired` / `@Value` / 构造器参数）：

```text
Step 1 shortcut → Step 2 @Value → Step 3 按名 → Step 4 集合 → Step 5 byType/@Primary → getBean
```

AAP 调用链：

```text
populateBean → resolveFieldValue → beanFactory.resolveDependency(...) → field.set
```

---

## autowiringIsEnabledByDefault 会用 Aware 吗？

```java
new AnnotationConfigApplicationContext(AutowiredConfig.class);
context.getBean(TestBean.class).name;  // "foo"
```

| Bean | Aware？ | 机制 |
|------|--------|------|
| `TestBean` | ✗ | 普通对象 |
| `AutowiredConfig` | ✗ | `@Autowired String autowiredName`（DI） |
| `AutowiredAnnotationBeanPostProcessor` | ✓ | `setBeanFactory` 后帮 Config 做 DI |

测试验证的是**配置类 @Autowired 默认开启**，不是 TestBean 的 Aware。

---

## Spring 实现中谁用 Aware？

| 类别 | 代表 | Aware |
|------|------|-------|
| DI 核心 | `AutowiredAnnotationBeanPostProcessor` | `BeanFactoryAware` |
| AOP | `AbstractAutoProxyCreator` | `BeanFactoryAware` |
| AOP | `DefaultAdvisorAutoProxyCreator` | `BeanNameAware` |
| Web | `GenericFilterBean` | `BeanNameAware`、`EnvironmentAware` |
| Web | `FrameworkServlet` | `ApplicationContextAware` |
| 占位符 | `PlaceholderConfigurerSupport` | `BeanNameAware`、`BeanFactoryAware` |

业务 `OrderService` → 构造器 / `@Autowired`，**不** implements Aware。

---

## 常见误区

见 [[13-生命周期层-Aware体系详解#十一、常见误区]]。

---

## 记忆口诀

```text
@Autowired  = 传菜员送协作同事
Aware       = 后勤/品控按登记表通知厨房基础设施
```

---

## 源码断点

1. `AbstractAutowireCapableBeanFactory.invokeAwareMethods()`
2. `ApplicationContextAwareProcessor.postProcessBeforeInitialization()`
3. `AutowiredAnnotationBeanPostProcessor.setBeanFactory()` / `resolveFieldValue()`
4. `DefaultListableBeanFactory.doResolveDependency()`

→ [[25-源码调试与断点指南]]
