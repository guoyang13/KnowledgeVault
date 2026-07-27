---
type: canonical
status: reviewed
topic: Spring ObjectProvider
source_version: 6.2.x
---

# getBeanProvider 与 ObjectProvider 有什么用？

> 导航：[[00-Spring-Framework核心机制-学习导航]] · **40 · 机制与源码** · 依赖消费 API
>
> 关联：[[4-容器层-BeanFactory接口体系详解]] · [[5-依赖注入实现原理]] · [[4-接口地图-IoC与DI重要接口大全#5. 依赖消费与对象生产]]
>
> 本文是 ObjectProvider 子概念的主文档；依赖注入的正式定义和总体流程以 [[5-依赖注入实现原理]] 为准。

---

## 一句话

> **`getBean(type)` = 现在就要 Bean，没有/不唯一就抛异常**  
> **`getBeanProvider(type)` = 先拿「取 Bean 的遥控器」，用时再 `getObject` / `getIfAvailable` / `stream`**

---

## 是什么

```java
ObjectProvider<UserService> provider = beanFactory.getBeanProvider(UserService.class);
// 返回 ObjectProvider<T>，不是 UserService 实例
```

- 定义：`BeanFactory.getBeanProvider(Class/ResolvableType)` → `ObjectProvider<T>`
- 源码：`spring-beans/.../factory/ObjectProvider.java`
- 每次 `getObject` 等调用都会**重新向容器解析**（反映容器当前状态）

---

## 和 getBean 对比

| | `getBean(Class<T>)` | `getBeanProvider(Class<T>)` |
|---|---------------------|----------------------------|
| 返回 | 实例 T | `ObjectProvider<T>` |
| 何时创建 | 调用时立即 | 第一次 `getObject` 时 |
| 没有 Bean | 异常 | `getIfAvailable` → null |
| 多个 Bean | 异常 | `stream` / `getIfUnique` |

---

## ObjectProvider 常用 API

```java
provider.getObject;           // 必须有且唯一，否则抛异常
provider.getIfAvailable;      // 没有 → null
provider.getIfUnique;         // 没有或多个且无唯一 winner → null
provider.ifAvailable(x -> ...); // 有才执行
provider.getIfAvailable( -> defaultImpl);  // 带默认值
provider.stream;              // 所有匹配 Bean
provider.orderedStream;       // 按 @Order 排序
```

---

## 两种用法入口

### 方式 A：依赖注入（业务代码常用）

```java
@Service
public class OrderService {
    private final ObjectProvider<PaymentClient> paymentClient;

    public OrderService(ObjectProvider<PaymentClient> paymentClient) {
        this.paymentClient = paymentClient;
    }

    public void checkout {
        paymentClient.ifAvailable(PaymentClient::pay);
    }
}
```

### 方式 B：编程式从容器取（框架代码常用）

```java
context.getBeanProvider(WebFilter.class).orderedStream.toList;
```

`ApplicationContext.getBeanProvider` 委托给内部 `BeanFactory.getBeanProvider`。

---

## Spring 源码里真实用在哪

| 类 | 用法 | 场景 |
|----|------|------|
| `WebHttpHandlerBuilder` | `getBeanProvider(WebFilter.class).orderedStream` | 收集**所有** WebFilter 组装链 |
| `RouterFunctionMapping` | `getBeanProvider(RouterFunction.class).orderedStream` | 合并所有路由函数 |
| `WebHttpHandlerBuilder` | `ifUnique(ObservationRegistry)` / `ifAvailable(Convention)` | **可选**观测组件 |
| `AbstractAsyncConfiguration` | `@Autowired ObjectProvider<AsyncConfigurer>` + `stream` | **0 或 1** 个自定义异步配置 |
| `MethodValidationPostProcessor` | `validatorProvider::getObject` | **延迟**取 Validator |

**WebFlux 收集过滤器：**

```java
List<WebFilter> webFilters = context
    .getBeanProvider(WebFilter.class)
    .orderedStream
    .toList;
```

**可选观测：**

```java
context.getBeanProvider(ObservationRegistry.class).ifUnique(builder::observationRegistry);
context.getBeanProvider(ServerRequestObservationConvention.class).ifAvailable(builder::observationConvention);
```

→ 本地源码：`spring-web/.../WebHttpHandlerBuilder.java` · `spring-webmvc/.../RouterFunctionMapping.java`

---

## 实际业务需要吗？ ★

**大多数业务代码不需要。** 普通 `@Service` / `@Repository` 用 **构造器 + `@Autowired`** 即可（约 90%+ 场景）。

### 不需要 ObjectProvider 时

| 场景 | 够用写法 |
|------|----------|
| 注入一个 Service / Repository | 构造器注入 |
| 多实现 + `@Primary` | `@Autowired PaymentGateway gw` |
| 指定实现 | `@Qualifier("alipay")` |
| 要全部实现 | `@Autowired List<PaymentGateway>`（往往比 `stream` 更简单） |

### 业务里值得用的少数场景

| 场景 | 为什么 |
|------|--------|
| **可选依赖** | 没配 Metrics/审计也要能启动 → `ifAvailable` |
| **0 或 1 个** | 有自定义就用，没有走默认 → `getIfUnique` / 自己 `stream` 判断 |
| **用时再创建** | 依赖很重或很少用 → 构造器只存 provider |
| **Prototype 进 Singleton** | 每次要新实例 → `provider.getObject` |
| **打破构造器循环** | 延迟 `getBean`（更推荐改设计） |

### 业务示例

```java
// 可选 Metrics：没配 Prometheus 也能跑
@Service
public class OrderService {
    private final ObjectProvider<MetricsRecorder> metrics;

    public OrderService(ObjectProvider<MetricsRecorder> metrics) {
        this.metrics = metrics;
    }

    public void createOrder {
        // ...
        metrics.ifAvailable(m -> m.count("order.created"));
    }
}
```

```java
// 运行时按 channel 选支付网关
@Service
public class CheckoutService {
    private final ObjectProvider<PaymentGateway> gateways;

    public void pay(String channel) {
        gateways.stream
            .filter(g -> g.supports(channel))
            .findFirst
            .orElseThrow
            .pay;
    }
}
```

### 和常见替代

| 需求 | 更简单 | ObjectProvider |
|------|--------|----------------|
| 可选一个 | `@Autowired(required = false)` | `getIfAvailable` |
| 懒加载 | `@Lazy @Autowired T` | `ObjectProvider<T>` |
| 全部实现 | `@Autowired List<T>` | `orderedStream` |
| 必填唯一 | 构造器注入 | **不必用** |

### 谁用得多

| 谁 | 频率 |
|----|------|
| Spring 框架 / Boot 自动配置 | **经常用** |
| 普通 CRUD / 领域服务 | **很少** |
| 平台 / SDK / 插件化封装 | 稍多 |

**结论**：知道它是干什么的即可；**默认不用**；读源码、写框架扩展时会常见。

---

## 四个典型用途（机制层）

### 1. 可选依赖

```java
@Service
public class ReportService {
    private final ObjectProvider<MetricsExporter> exporter;

    public ReportService(ObjectProvider<MetricsExporter> exporter) {
        this.exporter = exporter;
    }

    public void report {
        exporter.ifAvailable(MetricsExporter::flush);  // 没配也不报错
    }
}
```

≈ `@Autowired(required = false)`，但更灵活。

### 2. 延迟加载

构造器只保存 provider，heavy Bean 在第一次业务调用时再 `getObject`。

### 3. 多个同类型实现

```java
providers.orderedStream.forEach(PaymentGateway::init);
```

≈ `@Autowired List<PaymentGateway>`，编程式、可延迟。

### 4. 打破循环依赖

注入 `ObjectProvider<SomeBean>` 而非 `SomeBean`，构造阶段不立即 `getBean`。

---

## 与 @Autowired 的关系

```java
@Autowired ObjectProvider<UserService> provider;  // resolveDependency 特殊分支
@Autowired UserService userService;             // 立即 resolve → getBean
```

`DefaultListableBeanFactory.resolveDependency`：

```text
if (ObjectProvider.class == descriptor.getDependencyType)
  → return new DependencyObjectProvider(descriptor, beanName)
  → 不立即 getBean(UserService.class)
```

---

## 与 ObjectFactory / FactoryBean 区别

| | `ObjectProvider<T>` | `ObjectFactory<T>` | `FactoryBean` |
|---|---------------------|--------------------|---------------|
| 是什么 | 注入点用的延迟取 Bean 工具 | 更底层延迟工厂 | 一种 Bean，生产 P |
| 取什么 | 容器里**已有**的 Bean | 同上 | 工厂 **getObject** 产物 |

厨房比喻：ObjectProvider = **传菜窗口**（要时才取）；FactoryBean = **面点师**（专门做产品）。

→ [[3-速查-Spring厨房比喻大全#1. 总体映射]]

---

## 何时用

| 场景 | 推荐 |
|------|------|
| 普通必填依赖 | 构造器 / `@Autowired` |
| 可选组件 | `ObjectProvider` + `getIfAvailable` |
| 多实现遍历 | `stream` / `orderedStream` |
| 延迟到用时再取 | `ObjectProvider` |
| 编程式从工厂取 | `beanFactory.getBeanProvider(T.class)` |

---

## 源码断点

- `DefaultListableBeanFactory.getBeanProvider`
- `DefaultListableBeanFactory.resolveDependency` — `ObjectProvider` 分支
- `DependencyObjectProvider.getObject`

→ [[1-源码调试与断点指南]]
