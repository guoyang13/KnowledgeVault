---
type: case-study
status: reviewed
topic: Spring Dependency Injection examples
source_version: 6.2.x
aliases:
  - Spring 依赖注入形式分类与 Demo
---

# Spring 依赖注入形式分类与 Demo

> 本文只负责使用方式和示例。DI 的正式定义、生命周期位置与源码主链见 [[5-依赖注入实现原理]]；候选解析细节见 [[Spring注入注解与byType-byName解析逻辑]]。

## 1. 选择入口

| 需求 | 推荐方式 |
| --- | --- |
| 必选依赖 | 构造器注入 |
| 可选且允许后续设置 | Setter / 方法注入 |
| 配置值 | `@Value` 或配置属性对象 |
| 指定某个实现 | `@Qualifier` |
| 多实现中的默认项 | `@Primary` |
| 注入全部实现 | `List<T>` / `Map<String, T>` |
| 可选、延迟或动态遍历 | `ObjectProvider<T>` |
| JSR-250 按名称语义 | `@Resource` |

字段注入虽然可用，但依赖不显式、不能声明为 `final`，也不利于脱离容器测试，因此不作为默认方案。

## 2. 构造器、字段与 Setter

### 构造器注入

```java
@Service
class OrderService {

    private final OrderRepository repository;

    OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

只有一个构造器时不需要写 `@Autowired`。构造器参数属于 Bean 成立的必要条件，实例创建完成时依赖已经存在。

### 字段注入

```java
@Service
class OrderService {

    @Autowired
    private OrderRepository repository;
}
```

字段注入发生在 `populateBean`，对象先被实例化，再由 `AutowiredAnnotationBeanPostProcessor` 反射写入字段。

### Setter 或任意方法注入

```java
@Service
class ReportService {

    private AuditClient auditClient;

    @Autowired(required = false)
    void setAuditClient(AuditClient auditClient) {
        this.auditClient = auditClient;
    }
}
```

Setter 更适合可选依赖。任意多参数方法也可以标注 `@Autowired`，其参数按同一套依赖解析规则处理。

## 3. `@Autowired`、`@Resource` 与 `@Inject`

| 注解 | 主要语义 | 处理器 |
| --- | --- | --- |
| `@Autowired` | 目标类型必须兼容；源码可按依赖名称快捷匹配，多候选再由 `@Qualifier`、`@Primary` 等消歧 | `AutowiredAnnotationBeanPostProcessor` |
| `@Inject` | 与 `@Autowired` 接近，但没有 `required` 属性 | `AutowiredAnnotationBeanPostProcessor` |
| `@Resource` | 显式名称或字段/Setter 名优先 | `CommonAnnotationBeanPostProcessor` |

```java
@Service
class CheckoutService {

    private final PaymentGateway gateway;

    CheckoutService(@Qualifier("alipayGateway") PaymentGateway gateway) {
        this.gateway = gateway;
    }
}
```

当候选不唯一时，不要依赖字段名“碰巧匹配”；使用 `@Qualifier` 或明确的业务路由策略表达选择意图。

## 4. 多实现注入

```java
interface PaymentGateway {
    boolean supports(String channel);
    void pay;
}

@Service
class PaymentRouter {

    private final List<PaymentGateway> gateways;

    PaymentRouter(List<PaymentGateway> gateways) {
        this.gateways = gateways;
    }

    void pay(String channel) {
        gateways.stream
            .filter(gateway -> gateway.supports(channel))
            .findFirst
            .orElseThrow
            .pay;
    }
}
```

集合注入会收集全部匹配 Bean；需要稳定顺序时使用 `@Order` 或实现 `Ordered`。需要延迟遍历时使用 [[getBeanProvider与ObjectProvider有什么用]]。

## 5. 配置值与延迟依赖

```java
@Service
class PaymentService {

    private final Duration timeout;
    private final ObjectProvider<MetricsRecorder> metrics;

    PaymentService(
            @Value("${payment.timeout:3s}") Duration timeout,
            ObjectProvider<MetricsRecorder> metrics) {
        this.timeout = timeout;
        this.metrics = metrics;
    }

    void pay {
        metrics.ifAvailable(recorder -> recorder.count("payment.request"));
    }
}
```

- `@Value` 由依赖描述符中的 suggested value 分支处理，并经过占位符解析与类型转换。
- `ObjectProvider` 注入的是延迟解析入口，不会在构造器里立即取得目标 Bean。

## 6. XML 与编程式装配

XML 显式属性注入：

```xml
<bean id="orderService" class="example.OrderService">
    <property name="repository" ref="orderRepository"/>
</bean>
```

编程式处理容器外创建的对象：

```java
OrderHandler handler = new OrderHandler;
beanFactory.autowireBean(handler);
```

前者通过 `applyPropertyValues` 写入属性；后者使用 `AutowireCapableBeanFactory` 为已有对象执行注入。常规业务 Bean 仍应交给容器创建。

## 7. 一个完整配置

```java
@Configuration
class PaymentConfiguration {

    @Bean
    @Primary
    PaymentGateway defaultGateway {
        return new DefaultPaymentGateway;
    }

    @Bean("alipayGateway")
    PaymentGateway alipayGateway {
        return new AlipayGateway;
    }

    @Bean
    CheckoutService checkoutService(
            @Qualifier("alipayGateway") PaymentGateway gateway,
            ObjectProvider<MetricsRecorder> metrics) {
        return new CheckoutService(gateway, metrics);
    }
}
```

```java
class CheckoutService {

    private final PaymentGateway gateway;
    private final ObjectProvider<MetricsRecorder> metrics;

    CheckoutService(
            PaymentGateway gateway,
            ObjectProvider<MetricsRecorder> metrics) {
        this.gateway = gateway;
        this.metrics = metrics;
    }
}
```

这个示例同时展示了：

- `@Bean` 方法参数注入；
- `@Qualifier` 明确候选；
- `@Primary` 提供默认候选；
- `ObjectProvider` 表达可选依赖；
- 构造器保持必选依赖显式且可测试。

## 8. 关联

- DI 主文档：[[5-依赖注入实现原理]]
- 候选解析：[[Spring注入注解与byType-byName解析逻辑]]
- 延迟与可选依赖：[[getBeanProvider与ObjectProvider有什么用]]
- 循环依赖：[[6-循环依赖与三级缓存详解]]
