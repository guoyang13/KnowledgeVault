# FactoryBean 接口体系详解

> 导航：[[00-Spring-Bean加载-学习导航]] · **上篇 01–15** · 工厂Bean · 前置：[[14-工厂Bean-BeanFactory与FactoryBean的区别]]
>
> 前置：[[14-工厂Bean-BeanFactory与FactoryBean的区别]]
>
> 本地源码：
> - `spring-beans/.../factory/FactoryBean.java`
> - `spring-beans/.../factory/SmartFactoryBean.java`
> - `spring-beans/.../factory/config/AbstractFactoryBean.java`
> - `spring-beans/.../factory/support/FactoryBeanRegistrySupport.java`

---

## 一句话

FactoryBean 不是单一接口，而是一套 **「工厂 Bean 契约 + 容器支持 + 抽象基类 + 大量实现」** 的体系。

---

## 整体架构

```text
┌─────────────────────────────────────────────────────────────────┐
│  契约层（接口）                                                   │
│    FactoryBean<T>           ← 核心三方法                          │
│      ↑ SmartFactoryBean<T>  ← 扩展：isPrototype / isEagerInit     │
├─────────────────────────────────────────────────────────────────┤
│  抽象实现层                                                       │
│    AbstractFactoryBean<T>   ← 单例/原型模板 + 循环依赖早期代理     │
│      ↑ AbstractServiceLoaderBasedFactoryBean                     │
│      ↑ ListFactoryBean / MapFactoryBean / SetFactoryBean ...      │
├─────────────────────────────────────────────────────────────────┤
│  容器支持层（BeanFactory 继承链）                                  │
│    FactoryBeanRegistrySupport                                     │
│      factoryBeanObjectCache  ← 缓存 getObject() 产物               │
│      getObjectFromFactoryBean()                                   │
│      ↑ AbstractBeanFactory                                        │
│        getObjectForBeanInstance()  ← getBean 时分流               │
├─────────────────────────────────────────────────────────────────┤
│  异常                                                             │
│    FactoryBeanNotInitializedException  ← 未初始化 / 循环依赖       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 一、FactoryBean\<T\> — 核心契约

```java
public interface FactoryBean<T> {
    String OBJECT_TYPE_ATTRIBUTE = "factoryBeanObjectType";

    @Nullable T getObject() throws Exception;
    @Nullable Class<?> getObjectType();
    default boolean isSingleton() { return true; }
}
```

| 方法 | 作用 | 容器如何使用 |
|------|------|-------------|
| `getObject()` | 返回**产品对象** | `getBean("name")` 最终调用 |
| `getObjectType()` | 产品类型 | 按类型查找、`@Autowired`；**应尽量提前返回，避免触发创建** |
| `isSingleton()` | 产品是否单例 | `true` → 结果被 `factoryBeanObjectCache` 缓存 |

源码约束：

- 实现此接口的 Bean **不能当普通 Bean 用**
- 容器只管理 **FactoryBean 本身**的生命周期，**不自动销毁**产品
- **编程式契约**，`getObject()` 可能在 BPP 就绪前被调用

### OBJECT_TYPE_ATTRIBUTE（since 5.2）

FactoryBean 的 class 无法推断产品类型时，在 `BeanDefinition` 上设置：

```java
beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, UserService.class);
```

容器在 `FactoryBeanRegistrySupport.getTypeForFactoryBeanFromAttributes()` 读取，用于**不实例化 FactoryBean 就确定类型**。

---

## 二、SmartFactoryBean\<T\> — 扩展契约

```java
public interface SmartFactoryBean<T> extends FactoryBean<T> {
    default boolean isPrototype() { return false; }
    default boolean isEagerInit() { return false; }
}
```

| 方法 | 含义 | 默认值 |
|------|------|--------|
| `isPrototype()` | 产品是否为**严格原型**（每次独立实例） | `false` |
| `isEagerInit()` | 是否**急切初始化** FactoryBean 及其单例产品 | `false` |

与 `isSingleton()` 的关系：

```text
isSingleton() == false
  → 普通 FactoryBean：产品按需 getObject()
  → SmartFactoryBean + isPrototype()==true：明确为原型

isEagerInit() == true
  → preInstantiateSingletons() 时提前 getBean(name) 创建产品
  → 适用于启动时就绪的单例产品（如 AOP 代理）
```

`DefaultListableBeanFactory.instantiateSingleton()`（L1273）：

```java
if (isFactoryBean(beanName)) {
    getBean(FACTORY_BEAN_PREFIX + beanName);
    if (bean instanceof SmartFactoryBean<?> sfb && sfb.isEagerInit()) {
        getBean(beanName);  // 急切创建产品
    }
}
```

> SmartFactoryBean 主要供**框架内部**使用；应用层 FactoryBean 一般实现 plain `FactoryBean` 即可。

---

## 三、AbstractFactoryBean\<T\> — 抽象模板

```java
public abstract class AbstractFactoryBean<T>
        implements FactoryBean<T>, BeanClassLoaderAware, BeanFactoryAware,
                   InitializingBean, DisposableBean {
    protected abstract T createInstance() throws Exception;
    public abstract Class<?> getObjectType();
}
```

| 能力 | 实现 |
|------|------|
| 单例 / 原型切换 | `setSingleton(boolean)`，默认 `true` |
| 单例 eager 创建 | `afterPropertiesSet()` → `createInstance()` |
| 原型每次新建 | `getObject()` → 每次 `createInstance()` |
| 循环依赖早期代理 | `getEarlySingletonInterfaces()` + JDK 动态代理 |
| 销毁 | `destroy()` → `destroyInstance(singletonInstance)` |

`getObject()` 逻辑：

```text
isSingleton() == true
  → initialized ? singletonInstance : getEarlySingletonInstance()（循环依赖代理）
isSingleton() == false
  → 每次 createInstance()
```

---

## 四、FactoryBeanRegistrySupport — 容器支持

位于 `AbstractBeanFactory` 与 `DefaultSingletonBeanRegistry` 之间。

### 4.1 两个缓存的区别

| 缓存 | 位置 | 存什么 |
|------|------|--------|
| `singletonObjects` | DefaultSingletonBeanRegistry | FactoryBean **实例本身** |
| `factoryBeanObjectCache` | FactoryBeanRegistrySupport | **`getObject()` 的产物** |

### 4.2 getObjectFromFactoryBean 流程

```text
getObjectFromFactoryBean(factory, beanName, shouldPostProcess)
  │
  ├─ factory.isSingleton() && containsSingleton(beanName)
  │     → 加锁 + synchronized(factory)
  │     → 查 factoryBeanObjectCache
  │     → 未命中 → factory.getObject()
  │     → postProcessObjectFromFactoryBean()（可选）
  │     → 写入 factoryBeanObjectCache
  │
  └─ 非单例产品 → getObject() → postProcess
```

`doGetObjectFromFactoryBean` 异常映射：

| 异常 | 含义 |
|------|------|
| `FactoryBeanNotInitializedException` | → `BeanCurrentlyInCreationException` |
| `getObject()` 返回 null 且正在创建 | → `BeanCurrentlyInCreationException` |
| 其他 | → `BeanCreationException` |

---

## 五、FactoryBeanNotInitializedException

```java
/**
 * FactoryBean 未完全初始化时 getObject() 抛出（如循环依赖）。
 * FactoryBean 的循环依赖不能用普通 Bean 三级缓存解决，
 * 因为必须完全初始化后才能返回产品。
 */
public class FactoryBeanNotInitializedException extends FatalBeanException
```

`AbstractFactoryBean` 折中方案：循环依赖时返回 **EarlySingletonInvocationHandler** 接口代理。

---

## 六、实现类分类

### 6.1 spring-beans 内置（工具型）

| 类 | 继承 | 产出 |
|----|------|------|
| `AbstractFactoryBean<T>` | — | 模板基类 |
| `ListFactoryBean` | AbstractFactoryBean | `List<Object>` |
| `MapFactoryBean` | AbstractFactoryBean | `Map` |
| `SetFactoryBean` | AbstractFactoryBean | `Set` |
| `PropertiesFactoryBean` | PropertiesLoaderSupport + FactoryBean | `Properties` |
| `YamlMapFactoryBean` | YamlProcessor + FactoryBean | `Map` |
| `ObjectFactoryCreatingFactoryBean` | AbstractFactoryBean | `ObjectFactory`（延迟 getBean） |
| `ProviderCreatingFactoryBean` | AbstractFactoryBean | `Provider` |
| `ServiceLocatorFactoryBean` | 直接 implements | Service Locator 动态代理 |
| `MethodInvokingFactoryBean` | MethodInvokingBean + FactoryBean | 方法调用结果 |
| `ServiceLoaderFactoryBean` | AbstractServiceLoaderBasedFactoryBean | SPI 服务 |

### 6.2 框架模块（基础设施型）

| 类 | 模块 | 产出 |
|----|------|------|
| `ProxyFactoryBean` | spring-aop | AOP 代理 |
| `ScopedProxyFactoryBean` | spring-aop | Scope 代理 |
| `JndiObjectFactoryBean` | spring-context | JNDI 对象 |
| `LocalContainerEntityManagerFactoryBean` | spring-orm | EntityManagerFactory |
| `TransactionProxyFactoryBean` | spring-tx | 事务代理 |
| `Jackson2ObjectMapperFactoryBean` | spring-web | ObjectMapper |

### 6.3 三种实现风格

```text
风格 A：继承 AbstractFactoryBean
  → 实现 createInstance() + getObjectType()
  → 例：ListFactoryBean

风格 B：直接 implements FactoryBean + Aware + InitializingBean
  → 例：ProxyFactoryBean、ServiceLocatorFactoryBean

风格 C：最小 implements FactoryBean
  → 例：MethodInvokingFactoryBean
```

---

## 七、isSingleton / isPrototype / isEagerInit 组合

| isSingleton（产品） | isPrototype（Smart） | isEagerInit（Smart） | 行为 |
|:------------------:|:-------------------:|:-------------------:|------|
| true | false | false | 产品单例，首次 getBean 时创建（默认） |
| true | false | true | 产品单例，启动 preInstantiate 时创建 |
| false | false | — | 每次 getObject() 新建 |
| false | true | — | 明确声明产品为原型 |
| FactoryBean 自身 | — | — | **几乎总是容器 singleton** |

---

## 八、完整调用链

```text
getBean("dataSource")                     // 要产品
  doGetBean()
    getSingleton("dataSource")            // FactoryBean 实例
    getObjectForBeanInstance()
      getObjectFromFactoryBean()
        factoryBeanObjectCache 命中？返回
        否则 factory.getObject()
          AbstractFactoryBean → createInstance()
          ProxyFactoryBean    → createProxy()
        postProcessObjectFromFactoryBean()
    返回 Connection

getBean("&dataSource")                    // 要工厂
  getObjectForBeanInstance()
    isFactoryDereference(&) → 返回 FactoryBean 实例
```

---

## 九、与相关概念对比

| 概念 | 关系 |
|------|------|
| `BeanFactory` | 容器；管理 FactoryBean 及其产品 |
| `FactoryBean` | 容器中的一种特殊 Bean |
| `@Bean` 方法 | 工厂方法，**不是** FactoryBean |
| `ObjectFactory<T>` | 延迟获取；`ObjectFactoryCreatingFactoryBean` 包装 getBean |
| `ObjectProvider<T>` | 现代延迟/可选注入，部分替代 FactoryBean |

---

## 十、源码阅读路径

| 优先级 | 文件 | 关注点 |
|:------:|------|--------|
| 1 | `FactoryBean.java` | 三方法契约 |
| 2 | `SmartFactoryBean.java` | isPrototype / isEagerInit |
| 3 | `AbstractFactoryBean.java` | 模板、早期代理 |
| 4 | `FactoryBeanRegistrySupport.java` | 产品缓存 |
| 5 | `AbstractBeanFactory.getObjectForBeanInstance()` | getBean 分流 |
| 6 | `ProxyFactoryBean.java` | 典型框架实现 |

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[14-工厂Bean-BeanFactory与FactoryBean的区别]] | [[16-IoC与DI核心概念]] |

---

## 关联

- [[00-Spring-Bean加载-学习导航]]
- [[09-容器层-BeanFactory与Registry详解]]
- [[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
- [[14-工厂Bean-BeanFactory与FactoryBean的区别]]
- [[10-Context层-ApplicationContext详解]]
- [[03-速查-IoC与DI核心整合速查]]
- [[04-速查-Spring厨房比喻大全]]
- [[05-接口地图-IoC与DI重要接口大全]]

---
## 下一步可深入

- [ ] FactoryBean 循环依赖 vs 普通 Bean 三级缓存
- [ ] `postProcessObjectFromFactoryBean` 对产品做 BPP
- [ ] `ProxyFactoryBean` 创建代理完整流程
