# DDD 分层：编译时 / 运行时与 Spring 装配

> 导航：[[00-Spring-Bean加载-学习导航]] · **100-Q&A**
>
> DDD 前置：[[04-第04章-架构]] · [[01-Java落地代码骨架]]
>
> Spring 关联：[[20-依赖注入实现原理]] · [[15-工厂Bean-FactoryBean接口体系详解]] · [[100-Q&A/动态代理是什么]]

---

## 一句话

**编译时**靠接口解耦模块依赖；**运行时**靠 bootstrap / infrastructure 把实现类放进容器，Spring 在 `getBean` / 构造器注入时**按类型**解析绑定。

---

## 编译时 vs 运行时

| | 编译时 | 运行时 |
|---|--------|--------|
| **谁在干活** | `javac`、Maven/Gradle 模块依赖 | JVM + Spring 容器 |
| **领域层依赖什么** | 只依赖**接口**（如 `OrderRepository`） | 不关心实现从哪来 |
| **实现类在哪** | **infrastructure** 模块，**不被 domain 引用** | 被 **bootstrap** 注册进容器 |
| **谁绑定接口→实现** | **没人绑定**，只检查类型 | Spring **按类型**找 Bean 并注入 |
| **能否换实现** | 换实现**不必改 domain 源码** | 改 `@Bean` / `@Component` 即可 |

---

## 编译时：接口解耦模块

```text
domain 模块
  └── OrderApplicationService(OrderRepository repo)   ← 只知道接口

infrastructure 模块
  └── MyBatisOrderRepository implements OrderRepository
        ↑
        domain 的 pom **不依赖** infrastructure
```

编译期**没有** Spring，也**没有** `getBean()`；只有 Java 类型系统 + 模块依赖图。

---

## 运行时：bootstrap 装配

```java
// infrastructure / bootstrap
@Configuration
public class OrderInfrastructureConfig {
    @Bean
    OrderRepository orderRepository(SqlSessionFactory sf) {
        return new MyBatisOrderRepository(sf);
    }
}
```

```java
// application — 只声明需要什么
@Service
public class OrderApplicationService {
    public OrderApplicationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
```

```text
启动 → refresh → 注册 BeanDefinition（含实现类）
  → 实例化 OrderApplicationService
  → 构造器要 OrderRepository → getBean(OrderRepository.class)
  → 注入 MyBatisOrderRepository
```

与 Spring **两阶段**对应：注册蓝图（阶段一）→ `getBean` / 注入（阶段二）。见 [[03-速查-IoC与DI核心整合速查#三、完整链路：从配置到对象]]。

---

## 依赖方向示意

```text
interfaces  → application → domain
infrastructure → application/domain 的接口
domain 不依赖 infrastructure
```

领域定义接口，基础设施实现接口 —— **依赖倒置**。见 DDD [[04-第04章-架构#依赖倒置]]。

---

## 和 FactoryBean 有关系吗？需要手写吗？

**没有必然关系。日常 DDD 业务代码不需要 FactoryBean。**

| 场景 | 手段 | 要不要 FactoryBean |
|------|------|-------------------|
| `MyBatisOrderRepository implements OrderRepository` | `@Repository` / `@Bean` | **否** — P 直接进容器 |
| `@Mapper UserMapper`（仅 interface） | 框架注册 `MapperFactoryBean` | **框架内部**，业务不写 F |
| `@Transactional` 注解 AOP | `AbstractAutoProxyCreator`（BPP） | **通常否** |

```text
有普通 Java 实现类？
  ├─ 是 → @Bean / @Component + 接口注入（DDD 常规路径）
  └─ 否（仅 interface / 必须运行时代理）
        → 框架用 FactoryBean（MyBatis 等），你无感
```

详见 [[15-工厂Bean-FactoryBean接口体系详解#编译期无实现类 vs 运行时动态造]]。

---

## 常见误区

| 误区 | 实际 |
|------|------|
| 「用了接口 = 运行时多态」 | 接口解耦的是**模块编译依赖**；**选哪个实现**是 bootstrap / 运行时的事 |
| 「Spring 编译期注入」 | 注入全是容器启动后反射 + 构造器/字段赋值 |
| 「DDD 装配要用 FactoryBean」 | 普通 Repository / Gateway 用 `@Bean` 即可 |
| 「FactoryBean = 增强 Bean 的抽象」 | FactoryBean = **「造 Bean 的 Bean」**；增强只是部分 P 的形态，且注解 AOP 常走 BPP |

---

## 记忆口诀

> **编译时**：我只认识接口，模块之间不绑死。  
> **运行时**：bootstrap 把实现塞进容器，Spring 按类型接线。

FactoryBean 是 infrastructure / **框架层**在「接口没有 Impl、须动态造 P」时接入容器的方式，与 domain 只依赖接口**不矛盾**。
