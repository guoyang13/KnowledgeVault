# BeanFactory 与 FactoryBean 的区别

> 导航：[[00-Spring-Bean加载-学习导航]] · **上篇 01–15** · 工厂Bean · 前置：[[09-容器层-BeanFactory与Registry详解]] · [[10-Context层-ApplicationContext详解]]
>
> 关联：[[15-工厂Bean-FactoryBean接口体系详解]] · [[04-速查-Spring厨房比喻大全]]（面点师比喻）
>
> 本地源码：
> - `spring-beans/.../factory/BeanFactory.java`
> - `spring-beans/.../factory/FactoryBean.java`

---

## 一句话

|                 | 是什么                                           | 比喻                        |
| --------------- | --------------------------------------------- | ------------------------- |
| **BeanFactory** | IoC **容器接口**，管理所有 Bean 的定义和生命周期               | 整个**厨房**                  |
| **FactoryBean** | 注册在容器里的**一种特殊 Bean**，本身不是最终产品，而是**生产其他对象的工厂** | 厨房里的**面点师**（做包子的人，不是包子本身） |
|                 |                                               |                           |

```text
BeanFactory（容器）
  ├── userService          → 普通 Bean，getBean 直接拿到 UserService
  └── myProxy（FactoryBean）→ getBean("myProxy") 拿到的是代理对象
                              getBean("&myProxy") 才拿到 FactoryBean 本身
```

> **名字相似，角色完全不同。** 这是 Spring 面试最高频混淆点之一。

---

## 核心对比

| 维度 | BeanFactory | FactoryBean |
|------|-------------|-------------|
| 类型 | **接口**（IoC 容器根） | **接口**（一种 Bean 的实现方式） |
| 层级 | 容器级，管理整个应用 | Bean 级，容器里的一个组件 |
| `getBean("xxx")` 返回 | 普通 Bean 实例 | FactoryBean **产出的对象**（`getObject()` 返回值） |
| 获取自身 | 不适用 | `getBean("&xxx")`（`&` 前缀） |
| 典型实现 | `DefaultListableBeanFactory` | `ProxyFactoryBean`、`JndiObjectFactoryBean` |
| 谁实现 | Spring 框架 | 用户或框架基础设施代码 |

---

## FactoryBean 工作原理

### 接口三方法

```java
public interface FactoryBean<T> {
    T getObject() throws Exception;      // 返回要暴露的产品
    Class<?> getObjectType();            // 产品类型（按类型查找时用）
    default boolean isSingleton() { return true; }  // 产品是否单例
}
```

源码关键约束：

> A bean that implements this interface **cannot be used as a normal bean**.  
> The object exposed is always what **`getObject()` creates**.

### getBean 时的容器处理

`AbstractBeanFactory.getObjectForBeanInstance()`（L1857）：

```text
getBean("myProxy")
  → 从 singletonObjects 取出 FactoryBean 实例
  → 发现是 FactoryBean
  → 调用 getObjectFromFactoryBean() → factory.getObject()
  → 若 isSingleton()==true，结果写入 factoryBeanObjectCache
  → 返回产品对象

getBean("&myProxy")                   ← & 表示「要工厂本身」
  → isFactoryDereference(name) == true
  → 直接返回 FactoryBean 实例，不调用 getObject()
```

### & 前缀

```java
// BeanFactory.java
String FACTORY_BEAN_PREFIX = "&";
```

| 调用 | 结果 |
|------|------|
| `getBean("dataSource")` | FactoryBean 创建的数据源对象 |
| `getBean("&dataSource")` | FactoryBean 本身 |
| `getType("dataSource")` | 产品类型（`getObjectType()`），不是 FactoryBean 的 class |

---

## 典型使用场景

> 为什么 AOP、Mapper、JNDI 都注册 **F 而不是 P**？见 [[15-工厂Bean-FactoryBean接口体系详解#为什么 AOP、Mapper、JNDI 都注册 F（而不是 P）？]] · FactoryBean 是否「增强抽象」？见 [[15-工厂Bean-FactoryBean接口体系详解#FactoryBean 抽象的是什么？（不是「增强 Bean」的抽象）]]

### 1. AOP 代理 — ProxyFactoryBean

```text
注册：ProxyFactoryBean（FactoryBean）
getBean("userService")  → JDK/CGLIB 代理对象
getBean("&userService") → ProxyFactoryBean 本身
```

### 2. 复杂对象创建

```java
@Component
public class ConnectionFactoryBean implements FactoryBean<Connection> {
    @Override
    public Connection getObject() throws Exception {
        return DriverManager.getConnection(url, user, password);
    }
    @Override
    public Class<?> getObjectType() { return Connection.class; }
    @Override
    public boolean isSingleton() { return false; }
}
```

### 3. 框架内部

| FactoryBean | 产出 |
|-------------|------|
| `ProxyFactoryBean` | AOP 代理 |
| `JndiObjectFactoryBean` | JNDI 对象 |
| `LocalContainerEntityManagerFactoryBean` | JPA EntityManagerFactory |
| `MapperFactoryBean`（MyBatis-Spring） | Mapper 代理 |

---

## 生命周期差异（重要）

源码（`FactoryBean.java` L46-50）：

> 容器只管理 **FactoryBean 实例**的生命周期，**不**自动销毁 `getObject()` 产出的对象。

| | FactoryBean 本身 | FactoryBean 产出的对象 |
|--|-----------------|----------------------|
| 创建 | 容器管理 | `getObject()` 创建 |
| 销毁 | 容器 `destroy-method` | **不会自动销毁** |
| 正确做法 | FactoryBean 实现 `DisposableBean`，在 `destroy()` 里关闭产品 | — |

---

## 与 @Bean 工厂方法的区别

```java
@Configuration
public class AppConfig {
    @Bean
    public UserService userService() {
        return new UserServiceImpl();  // 工厂方法，不是 FactoryBean
    }
}
```

| | `@Bean` 工厂方法 | `FactoryBean` |
|--|----------------|---------------|
| 形式 | 配置类方法 | 实现 `FactoryBean` 接口的类 |
| 注册结果 | 方法返回值**直接**作为 Bean | 注册 FactoryBean，产品由 `getObject()` 提供 |
| 使用频率 | 日常开发常用 | 框架 / 基础设施常用 |

---

## 与 BeanFactory 的关系图

```text
                    BeanFactory（IoC 容器）
                           │
           ┌───────────────┼───────────────┐
           │               │               │
      普通 Bean        FactoryBean      其他 Bean
    UserService      ProxyFactoryBean   OrderService
           │               │
    getBean 直接返回    getBean → getObject() 的产品
           │          getBean("&") → FactoryBean 自身
           ▼               ▼
      UserService       Proxy 对象
```

---

## 常见面试题速答

| 问题 | 答案 |
|------|------|
| `getBean("xxx")` 拿到什么？ | 普通 Bean → 实例本身；FactoryBean → `getObject()` 返回值 |
| 如何拿 FactoryBean 本身？ | `getBean("&xxx")` |
| 按类型注入时怎么处理？ | 看 `getObjectType()`，不是 FactoryBean 的 class |
| 产品单例会缓存吗？ | `isSingleton()==true` → `factoryBeanObjectCache` 缓存 `getObject()` 结果 |

---

## 记忆口诀

```text
BeanFactory  = 容器（管所有 Bean）
FactoryBean  = 工厂 Bean（管一个产品）

getBean("name")   → 要产品
getBean("&name")  → 要工厂
```

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[13-生命周期层-Aware体系详解]] | [[15-工厂Bean-FactoryBean接口体系详解]] |

---

## 关联

- [[00-Spring-Bean加载-学习导航]]
- [[09-容器层-BeanFactory与Registry详解]]
- [[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
- [[15-工厂Bean-FactoryBean接口体系详解]]
- [[04-速查-Spring厨房比喻大全]]
- [[05-接口地图-IoC与DI重要接口大全]]

---
## 下一步可深入

- [ ] `factoryBeanObjectCache` vs `singletonObjects` 两个缓存的区别 → [[15-工厂Bean-FactoryBean接口体系详解#四、FactoryBeanRegistrySupport — 容器支持]]
- [ ] FactoryBean 循环依赖 vs 普通 Bean 三级缓存 → [[15-工厂Bean-FactoryBean接口体系详解]]
- [ ] `SmartFactoryBean.isEagerInit()` 与启动时预创建代理 → [[15-工厂Bean-FactoryBean接口体系详解#二、SmartFactoryBean\<T\> — 扩展契约]]
