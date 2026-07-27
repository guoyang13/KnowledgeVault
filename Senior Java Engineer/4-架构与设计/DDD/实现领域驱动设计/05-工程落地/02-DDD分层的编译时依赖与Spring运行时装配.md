---
type: canonical
status: reviewed
topic: DDD dependency inversion / Spring runtime composition
source_version: Spring Framework 6.2.x
aliases:
  - DDD 分层：编译时 / 运行时与 Spring 装配
---

# DDD 分层的编译时依赖与 Spring 运行时装配

> DDD 前置：[[04-第04章-架构]] · [[01-Java落地代码骨架]]
>
> Spring 关联：[[5-依赖注入实现原理]] · [[4-工厂Bean-FactoryBean接口体系详解]] · [[1010-Java动态代理与运行时代理机制]]

## 摘要

领域驱动设计（Domain-Driven Design, **DDD**）中的依赖倒置（Dependency Inversion）包含两个不同问题：

1. **编译期依赖（Compile-time Dependency）**：领域和应用模块只依赖稳定接口，不反向依赖基础设施实现。
2. **运行期装配（Runtime Composition）**：引导层把实现注册进 Spring 容器，容器依据依赖描述和候选解析规则完成注入。

接口负责建立依赖方向，组合根（Composition Root）负责选择具体实现。Spring 是一种运行期装配工具，不负责替代模块边界设计。

## 1. 编译期与运行期的分工

| 维度 | 编译期（Compile Time） | 运行期（Runtime） |
| --- | --- | --- |
| 执行者 | Java 编译器、Maven / Gradle | JVM、Spring IoC Container |
| 核心约束 | 类型可见性与模块依赖图 | Bean 定义、候选选择与生命周期 |
| 领域层知道什么 | 端口接口，例如 `OrderRepository` | 仍只通过接口协作 |
| 实现放在哪里 | 基础设施层（Infrastructure Layer） | 作为 Bean 注册到容器 |
| 接口如何对应实现 | 编译期不选择具体实现 | 组合根配置候选，容器解析并注入 |
| 替换实现的影响 | 领域源码无需修改 | 调整装配配置或候选条件 |

## 2. 编译期：用端口隔离基础设施

```text
domain / application
  └── OrderApplicationService(OrderRepository repository)
        └── 只依赖 OrderRepository 接口

infrastructure
  └── MyBatisOrderRepository implements OrderRepository
        └── 依赖 domain / application 暴露的端口
```

依赖方向是“基础设施实现指向领域端口”，而不是领域指向数据库、消息队列或远程调用实现。

这里没有 Spring 注入过程。编译器只检查：

- 接口和实现的类型关系是否成立；
- 模块是否允许访问相关类型；
- 调用是否符合 Java 类型系统。

## 3. 运行期：在组合根选择实现

引导层（Bootstrap Layer）或专门配置模块充当组合根：

```java
@Configuration
class OrderInfrastructureConfiguration {

    @Bean
    OrderRepository orderRepository(SqlSessionFactory sqlSessionFactory) {
        return new MyBatisOrderRepository(sqlSessionFactory);
    }
}
```

应用服务只声明依赖：

```java
@Service
class OrderApplicationService {

    private final OrderRepository orderRepository;

    OrderApplicationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
```

运行时主线：

```text
读取配置
  -> 注册 OrderRepository 实现的 BeanDefinition
  -> 创建 OrderApplicationService
  -> 为构造器参数建立 DependencyDescriptor
  -> 筛选类型兼容候选
  -> 应用 @Qualifier、@Primary、优先级、依赖名称等规则
  -> 注入最终候选
```

因此，“Spring 按类型注入”只是第一层摘要。准确说法是：**类型兼容是候选前提，随后还会应用限定符与候选消歧规则。**

Spring 主线：[[2-速查-IoC与DI核心整合速查#3. 一条主链]] · [[Spring注入注解与byType-byName解析逻辑]]

## 4. 与 FactoryBean 和动态代理的关系

日常 DDD 装配没有必须使用 `FactoryBean<T>`：

| 场景 | 常用装配手段 | 是否需要业务代码实现 `FactoryBean` |
| --- | --- | --- |
| 存在普通 Java 实现类 | `@Component` / `@Repository` / `@Bean` | 否 |
| 只有接口，框架需生成运行时代理 | 框架提供的 `FactoryBean` 或注册器 | 通常否 |
| 为业务 Bean 增加事务或切面 | Spring AOP 的自动代理创建器 | 否 |
| 复杂第三方对象需接入容器 | `@Bean` 或自定义 `FactoryBean` | 视创建协议而定 |

以 MyBatis Mapper 为例，业务侧只声明接口，框架可以使用 `MapperFactoryBean` 生产代理对象。这里的 `FactoryBean` 是基础设施接入机制，不是 DDD 依赖倒置的必要组成。

## 5. 常见误区

| 误区 | 修正 |
| --- | --- |
| 使用接口就等于完成 DDD 分层 | 还要验证模块依赖方向和领域边界 |
| Spring 在编译期完成注入 | Spring IoC 的依赖解析与赋值发生在运行期 |
| 接口会自动绑定唯一实现 | 组合根必须注册实现；多候选时还要配置消歧规则 |
| DDD Repository 必须使用 `FactoryBean` | 普通实现类直接注册即可 |
| 动态代理就是依赖倒置 | 动态代理是运行时实现技术，依赖倒置是依赖方向原则 |

## 6. 设计检查

1. 领域模块能否在不引入 Spring 和基础设施依赖的情况下编译与测试？
2. Repository、Gateway 等端口是否由需要它们的内层定义？
3. 具体实现的选择是否集中在组合根，而不是散落在领域代码中？
4. 多实现并存时，装配规则是否明确且可测试？
5. 框架代理、`FactoryBean` 等技术细节是否被限制在基础设施边界内？

一句话记忆：**编译期用接口守住依赖方向，运行期由组合根选择实现，再由 Spring 完成对象接线。**
