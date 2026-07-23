# BeanPostProcessor 详解

> 导航：[[00-Spring-Bean加载-学习导航]] · **上篇 01–15** · 扩展点层 · BPP
>
> 前置：[[11-扩展点层-BeanFactoryPostProcessor详解]] · [[09-容器层-BeanFactory与Registry详解]] · [[05-接口地图-IoC与DI重要接口大全#二、扩展点接口（IoC 的灵魂）]]
>
> 关联：[[19-IoC扩展点三部曲对照]] · [[20-依赖注入实现原理]] · [[22-Spring-AOP代理创建详解]]
>
> 本地源码：`/Users/guoyang/IdeaProjects/spring/spring-framework`

---

## 一句话

`BeanPostProcessor`（BPP）是 Spring IoC 容器里最重要的**扩展点**——允许在 Bean 创建生命周期的多个节点插入自定义逻辑。`@Autowired` 注入、AOP 代理、`@PostConstruct` 处理等，底层都依赖 BPP 实现。

> 速查版见 [[05-接口地图-IoC与DI重要接口大全#2.3 BeanPostProcessor — 上桌前品控加料 ★]]；本篇是完整深入版。

---

## 一、它是什么

```java
/**
 * Factory hook that allows for custom modification of new bean instances —
 * for example, checking for marker interfaces or wrapping beans with proxies.
 *
 * Typically, post-processors that populate beans via marker interfaces
 * or the like will implement postProcessBeforeInitialization,
 * while post-processors that wrap beans with proxies will normally
 * implement postProcessAfterInitialization.
 */
public interface BeanPostProcessor {
    default Object postProcessBeforeInitialization(Object bean, String beanName) { return bean; }
    default Object postProcessAfterInitialization(Object bean, String beanName) { return bean; }
}
```

**核心特征：**

- 针对**每个 Bean 实例**生效（不是针对 BeanDefinition）
- 可以**修改** Bean，也可以**替换**为代理对象
- 多个 BPP 组成**责任链**，按注册顺序依次调用
- 与 `BeanFactoryPostProcessor`（BFPP）不同：BFPP 改的是**定义**，BPP 改的是**实例**

| | BeanFactoryPostProcessor | BeanPostProcessor |
|--|--------------------------|-------------------|
| **操作对象** | BeanDefinition（元数据） | Bean 实例 |
| **调用时机** | 所有定义加载完毕，实例化之前 | 每个 Bean 创建过程中 |
| **典型用途** | 修改属性值、注册额外定义 | 注入、代理、初始化增强 |
| **代表** | `ConfigurationClassPostProcessor` | `AutowiredAnnotationBeanPostProcessor` |
| **refresh 步骤** | `invokeBeanFactoryPostProcessors()` | `registerBeanPostProcessors()` |

---

## 二、接口继承体系

```text
BeanPostProcessor                              ← 根接口，2 个方法
  │
  ├── InstantiationAwareBeanPostProcessor     ← 实例化阶段（3 个方法）
  │     └── SmartInstantiationAwareBeanPostProcessor  ← 构造器推断、早期引用（+4 个方法）
  │
  ├── MergedBeanDefinitionPostProcessor       ← 合并定义后处理（1 个方法）
  │
  └── DestructionAwareBeanPostProcessor       ← 销毁阶段（1-2 个方法）
```

### 各接口方法一览

| 接口 | 方法 | 阶段 |
|------|------|------|
| **SmartInstantiationAwareBeanPostProcessor** | `determineCandidateConstructors()` | 实例化前：选择构造器 |
| | `predictBeanType()` / `determineBeanType()` | 类型推断 |
| | `getEarlyBeanReference()` | 循环依赖：暴露早期引用 |
| **InstantiationAwareBeanPostProcessor** | `postProcessBeforeInstantiation()` | 实例化前：可跳过正常实例化 |
| | `postProcessAfterInstantiation()` | 实例化后、属性填充前 |
| | `postProcessProperties()` | 属性填充前：处理属性值 |
| **MergedBeanDefinitionPostProcessor** | `postProcessMergedBeanDefinition()` | 实例化后：缓存元数据到定义 |
| **BeanPostProcessor** | `postProcessBeforeInitialization()` | 初始化前 |
| | `postProcessAfterInitialization()` | 初始化后 |
| **DestructionAwareBeanPostProcessor** | `postProcessBeforeDestruction()` | 销毁前 |

Spring 官方建议：**应用层自定义 BPP 优先实现根接口 `BeanPostProcessor`**；`InstantiationAwareBeanPostProcessor` 等子接口主要供框架内部使用。

---

## 三、在 Bean 生命周期中的位置

结合 `doCreateBean()` 的完整时序（详见 [[17-Bean加载原理与源码阅读路径]]）：

```text
createBean()
  │
  ├─ resolveBeforeInstantiation()                         ← BPP 介入点 ①
  │     └─ postProcessBeforeInstantiation()
  │           若返回非 null → 跳过实例化，直接 postProcessAfterInitialization() → 返回
  │
  └─ doCreateBean()
        │
        ├─ createBeanInstance()                         new 对象
        │
        ├─ applyMergedBeanDefinitionPostProcessors()    ← BPP 介入点 ②
        │     └─ postProcessMergedBeanDefinition()
        │
        ├─ addSingletonFactory()                        ← BPP 介入点 ③
        │     └─ getEarlyBeanReference()                循环依赖早期引用
        │
        ├─ populateBean()                               ← BPP 介入点 ④
        │     ├─ postProcessAfterInstantiation()
        │     └─ postProcessProperties()                @Autowired 在这里
        │
        └─ initializeBean()                             ← BPP 介入点 ⑤⑥
              ├─ invokeAwareMethods()                   （Aware，非 BPP）
              ├─ postProcessBeforeInitialization()      所有 BPP 链式调用
              ├─ invokeInitMethods()                    @PostConstruct / init-method
              └─ postProcessAfterInitialization()       AOP 代理通常在这里

销毁时：
  registerDisposableBeanIfNecessary()  ← doCreateBean 末尾登记
  DisposableBeanAdapter.destroy()      ← context.close() 时执行
    └─ postProcessBeforeDestruction()                   ← BPP 介入点 ⑦（@PreDestroy）
```

→ 销毁登记、`requiresDestruction`、为何 Context.close() 就需要销毁：[[24-Bean销毁机制详解]]

**一张图记顺序：**

```text
实例化前 → 实例化 → 合并定义处理 → 早期引用 → 属性注入 → BeforeInit → init → AfterInit → [使用中] → 销毁
   ①          —           ②            ③         ④          ⑤       —      ⑥                    ⑦
```

与 [[20-依赖注入实现原理]] 的关系：`postProcessProperties()`（介入点 ④）是 `@Autowired` 字段/方法注入的入口，在 `invokeInitMethods()` 之前完成。

---

## 四、根接口两个方法的细节

### 4.1 postProcessBeforeInitialization

- **时机**：属性已填充完毕，但在 `@PostConstruct`、`InitializingBean.afterPropertiesSet()`、自定义 `init-method` **之前**
- **典型用途**：Aware 回调（`ApplicationContextAwareProcessor`）、校验、预处理
- **此时 Bean 状态**：对象已 `new` 出来，依赖已注入，但尚未执行初始化逻辑

### 4.2 postProcessAfterInitialization

- **时机**：上述所有初始化回调 **之后**
- **典型用途**：**AOP 代理**、包装 Bean、注册到外部系统
- **特殊说明**：若 `postProcessBeforeInstantiation()` 短路了正常实例化，**仍会**调用 `postProcessAfterInitialization()`

### 4.3 链式调用规则

```java
// AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization()
Object result = existingBean;
for (BeanPostProcessor processor : getBeanPostProcessors()) {
    Object current = processor.postProcessBeforeInitialization(result, beanName);
    if (current == null) {
        return result;  // 返回 null → 中断后续 BPP，但不回退到 null
    }
    result = current;   // 下一个 BPP 拿到的是上一个的返回值
}
return result;
```

要点：

- 每个 BPP 的输入是**上一个 BPP 的输出**
- 返回 `null` 会**中断链**，但容器保留上一个非 null 结果
- AOP 代理就是在链末尾把原始对象替换成代理对象

---

## 五、InstantiationAwareBeanPostProcessor 深入

这是 `@Autowired` 和 AOP 真正干活的地方。

### 5.1 postProcessBeforeInstantiation — 实例化短路

```text
// 若返回非 null，跳过后续 createBeanInstance / populateBean / initializeBean
// 只执行 postProcessAfterInitialization，然后直接返回
Object bean = resolveBeforeInstantiation(beanName, mbd);
if (bean != null) {
    return bean;  // 短路
}
```

`AbstractAutoProxyCreator`（AOP）在某些场景下利用此机制。

### 5.2 postProcessAfterInstantiation

- 实例已创建，属性**尚未**填充
- 返回 `false` 会**跳过后续属性填充**（包括 `@Autowired`）
- 正常应返回 `true`

### 5.3 postProcessProperties — @Autowired 的真正入口

```text
populateBean()
  → postProcessAfterInstantiation()     // 通常返回 true
  → autowireByName / autowireByType     // XML 时代的注入方式
  → postProcessProperties()             // AutowiredAnnotationBeanPostProcessor 在这里
  → applyPropertyValues()               // 把 PropertyValues 应用到 Bean
```

`AutowiredAnnotationBeanPostProcessor.postProcessProperties()` 扫描 `@Autowired`、`@Value`、`@Inject`，完成字段/方法注入。详见 [[20-依赖注入实现原理]]。

---

## 六、SmartInstantiationAwareBeanPostProcessor

在 `InstantiationAwareBeanPostProcessor` 基础上增加了 4 个方法：

| 方法 | 作用 | 谁在用 |
|------|------|--------|
| `determineCandidateConstructors()` | 推断该用哪个构造器 | `AutowiredAnnotationBeanPostProcessor`（`@Autowired` 构造器） |
| `predictBeanType()` | 预测 Bean 最终类型 | AOP 代理类型推断 |
| `determineBeanType()` | 确定 Bean 类型（6.0+） | AOP |
| `getEarlyBeanReference()` | 循环依赖时暴露早期引用 | AOP + 三级缓存 |

循环依赖场景：

```text
addSingletonFactory(beanName, () -> getEarlyBeanReference(...))
  → SmartInstantiationAwareBPP.getEarlyBeanReference()
  → 若需要 AOP，这里就可能提前生成代理
```

---

## 七、MergedBeanDefinitionPostProcessor

在实例化**之后**、属性填充**之前**，对合并后的 `RootBeanDefinition` 做后处理：

```java
void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition,
                                     Class<?> beanType, String beanName);
```

典型用途：`AutowiredAnnotationBeanPostProcessor` 在此缓存 `@Autowired` 字段/方法的 `InjectionMetadata`，避免每次创建 Bean 都重新反射扫描。

---

## 八、DestructionAwareBeanPostProcessor

Bean 销毁时调用：

```java
void postProcessBeforeDestruction(Object bean, String beanName);
default boolean requiresDestruction(Object bean) { return true; }
```

`CommonAnnotationBeanPostProcessor` 通过它处理 `@PreDestroy`。

---

## 九、BeanPostProcessorCache — 性能优化

容器不会每次都遍历全部 BPP 判断类型，而是维护一个缓存（`AbstractBeanFactory` 内部）：

```java
static class BeanPostProcessorCache {
    final List<InstantiationAwareBeanPostProcessor> instantiationAware = new ArrayList<>();
    final List<SmartInstantiationAwareBeanPostProcessor> smartInstantiationAware = new ArrayList<>();
    final List<DestructionAwareBeanPostProcessor> destructionAware = new ArrayList<>();
    final List<MergedBeanDefinitionPostProcessor> mergedDefinition = new ArrayList<>();
}
```

BPP 列表变化时会 `resetBeanPostProcessorCache()`。各阶段只遍历对应子类型的列表，避免全量扫描。

---

## 十、BPP 的注册

### 10.1 注册时机（refresh 流程中）

```text
prepareBeanFactory()              // 直接 addBeanPostProcessor（内置 BPP）
  → invokeBeanFactoryPostProcessors()
  → registerBeanPostProcessors()  // 实例化并注册所有 BPP Bean
  → finishBeanFactoryInitialization()
```

详见 [[10-Context层-ApplicationContext详解]] 的 `refresh()` 步骤。

### 10.2 三种注册方式

| 方式 | 示例 |
|------|------|
| **编程式** | `beanFactory.addBeanPostProcessor(new MyBpp())` |
| **声明为 Bean** | `@Component` 或 `@Bean` 注册 BPP 类 |
| **框架内置** | `prepareBeanFactory()` / `AnnotationConfigUtils` 自动注册 |

### 10.3 框架自动注册的 BPP

**prepareBeanFactory 直接 add：**

| BPP | 职责 |
|-----|------|
| `ApplicationContextAwareProcessor` | Aware 回调 |
| `ApplicationListenerDetector` | 检测 ApplicationListener |
| `LoadTimeWeaverAwareProcessor` | 类加载织入（条件注册） |

**AnnotationConfigUtils 注册为 BeanDefinition：**

| Bean 名 | 类 |
|---------|-----|
| `internalAutowiredAnnotationProcessor` | `AutowiredAnnotationBeanPostProcessor` |
| `internalCommonAnnotationProcessor` | `CommonAnnotationBeanPostProcessor` |
| `internalPersistenceAnnotationProcessor` | `PersistenceAnnotationBeanPostProcessor` |

---

## 十一、排序规则

`PostProcessorRegistrationDelegate.registerBeanPostProcessors()` 的注册顺序：

```text
1. BeanPostProcessorChecker（检测警告）
2. PriorityOrdered 的 BPP          ← AutowiredAnnotationBeanPostProcessor 在这里
3. Ordered 的 BPP
4. 普通 BPP（无顺序接口）
5. MergedBeanDefinitionPostProcessor 重新注册到链尾
6. ApplicationListenerDetector 再次注册到链尾
```

**关键规则：**

| 规则 | 说明 |
|------|------|
| 实现 `PriorityOrdered` | 最高优先级 |
| 实现 `Ordered` | 次优先级 |
| `@Order` 注解 | **对 BPP 无效** |
| 同优先级 | 注册顺序决定调用顺序 |
| MergedBeanDefinitionPostProcessor | 始终移到链尾（但在 ApplicationListenerDetector 之前） |

---

## 十二、框架内置 BPP 代表

| 类 | 实现接口 | 核心职责 |
|----|---------|----------|
| `AutowiredAnnotationBeanPostProcessor` | Smart + Merged + PriorityOrdered | `@Autowired` / `@Value` / 构造器注入 |
| `CommonAnnotationBeanPostProcessor` | InstantiationAware + DestructionAware | `@PostConstruct` / `@PreDestroy` / `@Resource` |
| `AbstractAutoProxyCreator` | SmartInstantiationAware | AOP 自动代理 |
| `ApplicationContextAwareProcessor` | BeanPostProcessor | ApplicationContext 级 Aware |
| `PersistenceAnnotationBeanPostProcessor` | — | JPA `@PersistenceContext` |
| `EventListenerMethodProcessor` | — | `@EventListener` 方法注册 |

### AutowiredAnnotationBeanPostProcessor 参与的回调

```text
determineCandidateConstructors()         → 构造器 @Autowired
postProcessMergedBeanDefinition()        → 缓存 InjectionMetadata
postProcessProperties()                → 字段/Setter @Autowired、@Value
```

### AbstractAutoProxyCreator 参与的回调

```text
postProcessBeforeInstantiation()         → 可能短路实例化（自定义 TargetSource）
postProcessAfterInitialization()         → 常规 AOP 代理生成
getEarlyBeanReference()                  → 循环依赖 + AOP
predictBeanType()                        → 代理类型预测
```

→ 代理创建条件、`wrapIfNecessary()`、JDK/CGLIB 决策、常见注解是否触发代理：详见 [[22-Spring-AOP代理创建详解]]

---

## 十三、自定义 BPP

### 13.1 最简示例

```java
@Component
public class LoggingBeanPostProcessor implements BeanPostProcessor, Ordered {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof UserService) {
            System.out.println("Before init: " + beanName);
        }
        return bean;  // 必须返回 bean（或包装后的对象）
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
```

### 13.2 @Configuration 中注册 — 应使用 static

```java
@Configuration
public class AppConfig {

    @Bean
    static LoggingBeanPostProcessor loggingBeanPostProcessor() {
        return new LoggingBeanPostProcessor();
    }
}
```

**为什么应使用 static？**（Spring 官方强烈推荐，见 `BeanPostProcessor.java` L37-40）

BPP 在 `registerBeanPostProcessors()` 阶段被创建。非 static `@Bean` 会先实例化 `@Configuration` 配置类及其依赖；**在 BPP 链注册完成之前被创建出来的 Bean**，只能被「已注册的那部分 BPP」处理，可能收不到后续 BPP（例如 AOP 代理）：

```text
Bean 'xxx' is not eligible for getting processed by all BeanPostProcessors
(for example: not eligible for auto-proxying).
```

`BeanPostProcessorChecker` 就是专门检测并 warn 这个情况的。

**注意：不是「一定会有部分 Bean 无法完整后处理」。**

| 情况 | 是否可能缺少后处理 |
|------|-------------------|
| BPP 全部注册**之后**才创建的 Bean | ❌ 通常不受影响 |
| 非 static 导致**过早**实例化配置类 / 其 `@Autowired` 依赖 / 同配置类其他 `@Bean` | ✅ 可能受影响 |
| 配置类极简（仅一个非 static BPP `@Bean`，无其他依赖） | 可能不触发 warn，但不推荐赌 |

→ BFPP 同理，见 [[11-扩展点层-BeanFactoryPostProcessor详解#6.2 @Configuration 中注册 — 应使用 static]]

### 13.3 返回代理对象

```java
@Override
public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (needsProxy(bean)) {
        return Proxy.newProxyInstance(
            bean.getClass().getClassLoader(),
            bean.getClass().getInterfaces(),
            (proxy, method, args) -> method.invoke(bean, args));
    }
    return bean;
}
```

AOP 的 `AbstractAutoProxyCreator` 就是这个模式的框架级实现。

---

## 十四、常见误区

| 误区 | 正解 |
|------|------|
| `@PostConstruct` 由 BPP 在 BeforeInit 处理 | `@PostConstruct` 在 BeforeInit **之后**的 `invokeInitMethods()` 中执行；BPP 只是注册了处理它的 `CommonAnnotationBeanPostProcessor` |
| AOP 代理在实例化时生成 | 绝大多数在 `postProcessAfterInitialization()` |
| BPP 可以用 `@Order` 排序 | 不行，必须 `Ordered` / `PriorityOrdered` |
| 所有 Bean 都经过完整 BPP 链 | BPP 自身、infrastructure Bean 可能例外；非 static `@Bean` 注册 BPP 时，**仅早期被连带创建的 Bean** 有风险，不是全体 |
| 返回 null 表示「不处理」 | 返回 null 会**中断后续 BPP 链** |
| `@Autowired` 在 BeforeInit 注入 | 在 `populateBean()` → `postProcessProperties()`，比 BeforeInit 更早 |

---

## 十五、源码阅读顺序

| 步骤 | 文件 | 关注点 |
|:----:|------|--------|
| 1 | `BeanPostProcessor.java` | 根接口契约 |
| 2 | `InstantiationAwareBeanPostProcessor.java` | 实例化阶段扩展 |
| 3 | `SmartInstantiationAwareBeanPostProcessor.java` | 构造器推断、早期引用 |
| 4 | `AbstractAutowireCapableBeanFactory.doCreateBean()` | BPP 各介入点 |
| 5 | `AbstractAutowireCapableBeanFactory.populateBean()` | `@Autowired` 注入 |
| 6 | `AbstractAutowireCapableBeanFactory.initializeBean()` | Before/After 调用 |
| 7 | `PostProcessorRegistrationDelegate.registerBeanPostProcessors()` | 注册与排序 |
| 8 | `AutowiredAnnotationBeanPostProcessor.java` | 最复杂的内置 BPP |
| 9 | `AbstractAutoProxyCreator.java` | AOP 如何利用 BPP |

建议在 `populateBean()` 和 `initializeBean()` 上打断点，观察 BPP 链的实际调用顺序。配合 [[25-源码调试与断点指南]] 跟栈。

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[11-扩展点层-BeanFactoryPostProcessor详解]] | [[13-生命周期层-Aware体系详解]] |

---

## 关联

- [[00-Spring-Bean加载-学习导航]]
- [[17-Bean加载原理与源码阅读路径]]
- [[09-容器层-BeanFactory与Registry详解]]
- [[10-Context层-ApplicationContext详解]]
- [[05-接口地图-IoC与DI重要接口大全]]
- [[20-依赖注入实现原理]]
- [[25-源码调试与断点指南]]
- [[04-速查-Spring厨房比喻大全]]
- [[11-扩展点层-BeanFactoryPostProcessor详解]]
- [[13-生命周期层-Aware体系详解]]
- [[19-IoC扩展点三部曲对照]]
- [[22-Spring-AOP代理创建详解]]

---
## 下一步可深入

- [x] `AbstractAutoProxyCreator` — AOP 代理创建时机 → [[22-Spring-AOP代理创建详解]]
- [ ] 循环依赖 — `SmartInstantiationAwareBeanPostProcessor.getEarlyBeanReference()`（与 [[22-Spring-AOP代理创建详解#六、代理在 doCreateBean 中的三个时机]] 交叉）
- [ ] `AutowiredAnnotationBeanPostProcessor` — 与 [[20-依赖注入实现原理]] 对照精读
- [x] Aware 体系 → [[13-生命周期层-Aware体系详解]]
