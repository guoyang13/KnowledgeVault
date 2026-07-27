---
type: quick-reference
status: reviewed
topic: BeanFactory / FactoryBean
source_version: 6.2.x
aliases:
  - 6-工厂Bean-BeanFactory与FactoryBean的区别
---

# BeanFactory 与 FactoryBean 的区别

> 本文只负责快速辨析。FactoryBean 的设计动机、完整调用链、缓存和接口体系见 [[4-工厂Bean-FactoryBean接口体系详解]]。

## 核心区别

| 维度 | `BeanFactory` | `FactoryBean<T>` |
| --- | --- | --- |
| 角色 | IoC 容器的根接口 | 注册在容器中的一种特殊 Bean |
| 管理范围 | 管理整个容器中的 Bean | 负责创建一个产品对象 |
| 典型实现 | `DefaultListableBeanFactory` | `ProxyFactoryBean`、`MapperFactoryBean` |
| 日常业务是否实现 | 否，通常由 Spring 提供 | 很少，多用于框架和基础设施 |

一句话：

```text
BeanFactory = 管理所有 Bean 的容器
FactoryBean = 被容器管理、同时负责生产另一个对象的 Bean
```

## 获取规则

假设名为 `userService` 的 Bean 实现了 `FactoryBean<UserService>`：

| 调用 | 返回 |
| --- | --- |
| `getBean("userService")` | `FactoryBean#getObject` 产生的 `UserService` |
| `getBean("&userService")` | `FactoryBean` 实例本身 |
| `getType("userService")` | 通常是 `getObjectType` 报告的产品类型 |

```text
BeanFactory
  └── userService：FactoryBean 实例 F
        ├── getBean("userService")  → 产品 P
        └── getBean("&userService") → 工厂 F
```

容器分流入口：

```text
AbstractBeanFactory#getObjectForBeanInstance
  → FactoryBeanRegistrySupport#getObjectFromFactoryBean
  → FactoryBean#getObject
```

产品声明为单例时，结果缓存在 `factoryBeanObjectCache`；它与保存普通单例 Bean 的 `singletonObjects` 不是同一个缓存。

## 与 `@Bean` 的区别

| 维度 | `@Bean` 工厂方法 | `FactoryBean` |
| --- | --- | --- |
| 注册结果 | 方法返回值直接成为 Bean | 工厂本身是 Bean，产品来自 `getObject` |
| 常见场景 | 日常业务装配 | 框架集成、动态代理、外部资源查找 |
| 特殊协议 | 无 | `&` 前缀、产品缓存、产品类型推断 |

业务代码只是需要创建一个对象时，优先使用构造器注入或 `@Bean`。框架需要批量注册“工厂 F”，并在运行时产生“产品 P”时，`FactoryBean` 更合适。

## 生命周期边界

Spring 容器管理 `FactoryBean` 本身的生命周期，但不会自动把 `getObject` 返回的任意产品当作独立 Bean 执行完整销毁流程。产品持有外部资源时，应由工厂负责释放。

## 最易混淆的判断

| 说法 | 判断 |
| --- | --- |
| FactoryBean 是 BeanFactory 的实现 | 错，二者角色不同 |
| FactoryBean 只用于 AOP | 错，JNDI、Mapper、复杂对象创建也会使用 |
| `getBean(name)` 返回 FactoryBean 本身 | 错，默认返回产品 |
| `@Service + @Transactional` 一定注册为 FactoryBean | 错，常见路径是 BPP 在初始化后创建代理 |
| FactoryBean 等同于 `@Bean` 方法 | 错，二者的容器协议和生命周期不同 |

## 深入入口

- 设计动机与最小 Demo：[[4-工厂Bean-FactoryBean接口体系详解#为什么需要 FactoryBean？]]
- `FactoryBean`、`SmartFactoryBean` 与抽象基类：[[4-工厂Bean-FactoryBean接口体系详解]]
- 产品缓存：[[4-工厂Bean-FactoryBean接口体系详解#四、FactoryBeanRegistrySupport — 容器支持]]
- 完整调用链：[[4-工厂Bean-FactoryBean接口体系详解#八、完整调用链]]
- 与相关概念对比：[[4-工厂Bean-FactoryBean接口体系详解#九、与相关概念对比]]
