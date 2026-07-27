---
type: canonical
status: reviewed
topic: Spring FactoryBean
source_version: 6.2.x
aliases:
  - FactoryBean
---

# FactoryBean 接口体系详解

> 导航：[[00-Spring-Framework核心机制-学习导航]] · **30 · 扩展点与生命周期** · 工厂Bean
>
> 快速辨析：[[5-工厂Bean-BeanFactory与FactoryBean的区别]]
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

## 为什么需要 FactoryBean？

**核心问题**：容器里 **注册的类** 和业务 **真正要用的对象**，经常不是同一个东西。Spring 需要标准协议，让 `getBean("xxx")` 稳定拿到 **产品**（`getObject`），而不是把工厂类本身当最终 Bean。

### 官方设计意图（FactoryBean.java Javadoc）

| 要点 | 说明 |
|------|------|
| 角色 | 工厂 Bean **不是**直接暴露的实例；引用时永远是 **`getObject` 的产物** |
| 限制 | **不能当普通 Bean 用**（`NB: A bean that implements this interface cannot be used as a normal bean`） |
| 使用范围 | **框架内大量使用**（AOP `ProxyFactoryBean`、JNDI 等）；自定义 FactoryBean **多见于基础设施** |
| 契约性质 | **编程式契约**；`getObject` / `getObjectType` 可能在 BPP 之前就调用 |
| 生命周期 | 容器只管理 **FactoryBean 本身**；**不自动销毁** `getObject` 产物（需工厂自己 `DisposableBean` 里关） |

since 2003（Rod Johnson），早于注解驱动；XML 时代大量 `<bean class="ProxyFactoryBean">` 即「声明工厂、使用产品」。

### 容器如何兑现（源码）

`AbstractBeanFactory.getObjectForBeanInstance`：

```text
getBean("name")
  → 实例是 FactoryBean？
  → 是 → getObjectFromFactoryBean → factory.getObject
         （singleton 产品 → factoryBeanObjectCache）
  → 否 → 直接返回普通 Bean

getBean("&name")   // FACTORY_BEAN_PREFIX
  → isFactoryDereference → 返回 FactoryBean 本身，不调 getObject
```

→ 详见 [[#八、完整调用链]]。

**若没有 FactoryBean**：容器只能「注册 F → 返回 F 的实例」；业务却想要 **产品 P**。容器必须额外约定：**见到 FactoryBean 就调 `getObject` 返回 P**，否则 AOP、MyBatis Mapper、JNDI 等都无法用同一套 `getBean` / 按类型注入接入。

### 进一步理解：普通 Bean 为什么不够？

假设业务要注入的是 **接口 `UserService` 的运行时代理**（AOP / MyBatis Mapper 同理：注册的是工厂类，用的是 **动态生成的实现**）：

```text
期望：getBean("userService")  → UserService 代理（产品 P）
若只注册普通 Bean：
  注册类 = UserServiceProxyFactory（工厂 F）
  getBean("userService")       → 得到 F 本身 ✗
  @Autowired UserService         → 类型不匹配 ✗
```

容器默认规则是：**实例化什么 class，就返回什么对象**。  
FactoryBean 在 `getObjectForBeanInstance` 里加了一条分流：**若实例 implements FactoryBean → 返回 `getObject`，不是工厂本身**。

```text
有 FactoryBean 协议：
  beanDefinition.beanClass = UserServiceProxyFactoryBean（F）
  singletonObjects 里存的是 F 实例
  getBean("userService")
    → getObjectForBeanInstance(F)
    → F.getObject
    → 返回 UserService JDK 代理（P）✓
  @Autowired UserService
    → resolveDependency(UserService.class)
    → getObjectType == UserService.class → 匹配 ✓
```

### Demo：最小 FactoryBean（模拟 AOP / Mapper）

**1. 业务接口与实现**

```java
public interface UserService {
    String hello;
}

public class UserServiceImpl implements UserService {
    public String hello { return "real"; }
}
```

**2. 工厂：注册 F，产出代理 P**

```java
@Component("userService")  // beanName = userService
public class UserServiceProxyFactoryBean implements FactoryBean<UserService> {

    private final UserService target = new UserServiceImpl;

    @Override
    public UserService getObject {
        // 简化：真实 AOP 由 ProxyFactoryBean + Advisor 完成
        return (UserService) Proxy.newProxyInstance(
            UserService.class.getClassLoader,
            new Class<?>[] { UserService.class },
            (proxy, method, args) -> {
                System.out.println("[proxy] before " + method.getName);
                return method.invoke(target, args);
            });
    }

    @Override
    public Class<?> getObjectType {
        return UserService.class;  // 按类型注入靠这个，尽量别在里头 getObject
    }

    @Override
    public boolean isSingleton {
        return true;  // 产品单例 → factoryBeanObjectCache 缓存
    }
}
```

**3. 消费者：只认接口，不知道工厂存在**

```java
@Service
public class OrderAppService {
    private final UserService userService;

    public OrderAppService(UserService userService) {  // 注入的是代理 P，不是 FactoryBean F
        this.userService = userService;
    }

    public void run {
        System.out.println(userService.hello);  // 走代理 → 打印 [proxy] before hello
    }
}
```

**4. 容器里实际发生什么**

```java
// 伪代码：AbstractBeanFactory.getObjectForBeanInstance
Object raw = singletonObjects.get("userService");     // UserServiceProxyFactoryBean 实例 F
if (raw instanceof FactoryBean<?> fb) {
    return getObjectFromFactoryBean(fb, "userService"); // → fb.getObject → 代理 P
}

// 调试时可对比：
ctx.getBean("userService");   // → UserService 代理（产品）
ctx.getBean("&userService");  // → UserServiceProxyFactoryBean（工厂本身）
ctx.getBean("userService").getClass;  // com.sun.proxy.$Proxy...
```

| 调用 | 返回 | 类型 |
|------|------|------|
| `getBean("userService")` | `getObject` 的代理 | `UserService` |
| `getBean("&userService")` | 工厂 Bean 本身 | `UserServiceProxyFactoryBean` |
| `getBean(UserService.class)` | 同上（靠 `getObjectType`） | `UserService` |

**5. 若改用 `@Bean`，等价但不经 FactoryBean 协议**

```java
@Configuration
public class AppConfig {
    @Bean
    public UserService userService {
        UserService target = new UserServiceImpl;
        return (UserService) Proxy.newProxyInstance(/* ... */);
    }
}
```

| | 上面 FactoryBean Demo | `@Bean` 工厂方法 |
|--|----------------------|-----------------|
| 注册进容器 | 工厂类 F | **方法返回值 P 直接就是 Bean** |
| `getBean("userService")` | 容器调 `getObject` | 直接拿 `@Bean` 返回值 |
| 需要 `&` 取工厂？ | 可以 | 不需要 |
| 典型用途 | **框架**统一处理成千上万此类工厂 | **业务**手写一个 Bean |

MyBatis `MapperFactoryBean`、Spring AOP `ProxyFactoryBean` 走的是 **FactoryBean 路线**：扫描注册的是 **FactoryBean 类**，运行时 `getBean(mapperInterface)` 拿到 **JDK 动态代理**——若容器没有「F → `getObject` → P」这条规则，就要为每种集成单独写特殊逻辑。

### Demo 小结

```text
问题：注册 F，要用 P（且 P 的类型 ≠ F）
普通 Bean：getBean → F ✗
FactoryBean：getBean → getObject → P ✓
           @Autowired 接口 ← getObjectType ✓
           框架一套 getObjectForBeanInstance 统一处理 AOP / Mapper / JNDI ...
```

→ 跟栈：`AbstractBeanFactory.getObjectForBeanInstance` · `FactoryBeanRegistrySupport.getObjectFromFactoryBean`（[[1-源码调试与断点指南]]）

### 为什么 AOP、Mapper、JNDI 都注册 F（而不是 P）？

**不是 Spring 故意绕一圈**，而是阶段一写入容器的 **BeanDefinition.beanClass** 在蓝图里往往**只能**写「怎么造 P 的工厂类」，不能直接写产品类。

#### 注册的是什么？

```text
阶段一：BeanDefinition（蓝图）
  beanClass = 某个 Class        ← 创建 Bean 的模板，不是最终业务对象 P
阶段二：getBean 时才造实例 / 调 getObject
```

对 AOP / Mapper / JNDI，蓝图里的 class **天然是工厂 F**，因为 **P 在注册那一刻还不存在或不能 `new` 固定类**。

#### 三个根本原因

| 原因 | 说明 |
|------|------|
| **① P 没有固定实现类** | Mapper 只有接口；AOP 产品是 `$Proxy` / CGLIB 子类，源码里没有 `UserMapperImpl` |
| **② 造 P 要多步装配** | 依赖 `SqlSessionFactory`、Advisor/Pointcut、JNDI 名等；要先注入 F，再 `getObject` |
| **③ 框架要统一集成** | 扫描成百上千 Mapper / 代理；用 **同一套 FactoryBean + getObject** 接入容器，而不是每种集成改 `getBean` |

#### 分别说明

**MyBatis — `MapperFactoryBean`**

```java
@Mapper
public interface UserMapper { User findById(Long id); }  // 无实现类
```

```text
BeanDefinition.beanClass = MapperFactoryBean（F）
属性：mapperInterface = UserMapper.class
getObject → MyBatis 生成的 JDK 动态代理（P）

不能写 beanClass = UserMapperImpl.class  ← 不存在
不能 new UserMapper                     ← 接口不能实例化
```

**AOP — `ProxyFactoryBean`（XML / 编程式）**

```xml
<bean id="userService" class="org.springframework.aop.framework.ProxyFactoryBean">
    <property name="target" ref="userServiceTarget"/>
    <property name="interceptorNames" value="txAdvisor"/>
</bean>
```

```text
F = ProxyFactoryBean（可配置 target、advisor）
P = 实现 UserService 的运行时代理
getBean("userService") → P，不是 F
```

> **注解 AOP 补充**：`@Service` + `@Transactional` 常由 **`AbstractAutoProxyCreator`（BPP）** 在 `AfterInit` 包代理，**不一定**每个 Bean 都是 `ProxyFactoryBean`。但 XML/编程式代理、框架内部仍大量 **注册 F** 的模式。

**JNDI — `JndiObjectFactoryBean`**

```text
F = JndiObjectFactoryBean
属性：jndiName = "java:comp/env/jdbc/MyDS"
getObject → InitialContext.lookup(...) → DataSource（P）

P 在应用服务器 JNDI 里，不在 Spring classpath；只能注册「负责 lookup 的工厂 F」
```

#### 「同一套 getBean / 按类型注入」是什么意思？

若没有 FactoryBean，容器只能 **注册 F → 返回 F**：

```java
@Autowired UserMapper userMapper;   // 期望 P（接口代理）
// 普通规则 → 拿到 MapperFactoryBean ✗
```

有 FactoryBean 后，**一条规则**处理所有集成：

```text
getBean(name) / getBean(Type)
  → getObjectForBeanInstance
  → FactoryBean.getObject → P
  → getObjectType 供 @Autowired 按类型匹配
```

#### 能不能不注册 F，直接注册 P？

**可以，但只适合简单个例**（等价 `@Bean` 返回值直接是 Bean）：

```java
@Bean
UserService userService {
    return (UserService) Proxy.newProxyInstance(...);  // P 直接进容器
}
```

框架层不用这条路的原因：

| 若直接注册 P | 问题 |
|-------------|------|
| Mapper 成百上千 | 无法逐个 `@Bean`；需 **扫描 + 统一注册 F 模板** |
| 代理可配置 advisor | 需 **可配置工厂 Bean**，不是单一工厂方法 |
| JNDI 延迟 lookup | 工厂持 jndiName，`getObject` 时再查 |
| 容器统一规则 | 需要 `getObjectType` + `getObject` 协议 |

**结论**：业务装配优先 `@Bean`；**AOP / MyBatis / JNDI 在框架层注册 F**，是因为 P 动态生成、依赖装配或外部资源，蓝图阶段只能用工厂类描述「如何造 P」。

### 编译期无实现类 vs 运行时动态造

两类情况都会走 **框架注册 F**，但原因略有不同：

| 类型 | 含义 | 典型 |
|------|------|------|
| **编译期无实现类** | 源码里只有 interface，没有 `XxxImpl`，`javac` 无法 `new` | `@Mapper UserMapper` |
| **运行时动态造** | 或有目标类，但 P 须多步装配（代理、JNDI lookup 等），蓝图不能写死 P | `ProxyFactoryBean`、JNDI DataSource |

**MyBatis 完整时间线（UserMapper）**

```text
【编译期】
  源码：UserMapper.java（interface only）
  classpath：UserMapper.class；无 UserMapperImpl、无 $Proxy

【启动 · 阶段一 · 注册蓝图】
  ClassPathMapperScanner
    → register BeanDefinition("userMapper")
    → beanClass = MapperFactoryBean（F）
    → property mapperInterface = UserMapper

【启动 · 阶段二 · 实例化 F】
  createBean("userMapper") → new MapperFactoryBean
    → 注入 SqlSessionFactory 等

【启动 · 阶段三 · 有人要 P】
  OrderService 构造器要 UserMapper
    → resolveDependency(UserMapper.class)
    → getBean → getObjectForBeanInstance → getObject
    → 返回 $Proxy（P）→ 注入
```

与 **DDD 普通 Repository** 对比：`MyBatisOrderRepository implements OrderRepository` 编译期有实现类 → `@Bean` / `@Repository` 直接注册 **P**，不需要 F。见 [[02-DDD分层的编译时依赖与Spring运行时装配]]。

### FactoryBean 抽象的是什么？（不是「增强 Bean」的抽象）

> **FactoryBean = 容器里的「工厂 Bean」协议**——注册 **F**，`getBean` 拿到 **F 生产的 P**。  
> **「增强」只是部分场景下 P 的形态**；Mapper 生成实现、JNDI lookup 与增强无关。

#### 两种「变样」路线，勿混

```text
路线 A：FactoryBean（工厂产 P）
  注册 F → getObject → P
  例：MapperFactoryBean、ProxyFactoryBean、JndiObjectFactoryBean

路线 B：BPP 增强（先造原 Bean，再包一层）
  createBean → 原始 target → postProcessAfterInitialization → 代理替换
  例：@Service + @Transactional（常见，不注册 FactoryBean）
```

| 说法 | 是否成立 |
|------|----------|
| FactoryBean 是「动态生成 / 不好直接 new 的对象」的抽象 | ✓ 更接近 |
| FactoryBean 是「需要 AOP 增强的 Bean」的抽象 | ✗ 太窄；注解 AOP 常走 BPP |
| FactoryBean 是「工厂注册进容器、产品交给 getObject」的抽象 | ✓ 最准确 |

#### P 的形态 ≠ 都叫「增强」

| 场景 | P 是什么 | 算不算增强 |
|------|----------|-----------|
| MyBatis `@Mapper` | 接口 JDK 动态代理 | **生成实现**，非增强已有 Bean |
| `ProxyFactoryBean` | 带 Advisor 的 AOP 代理 | ✓ 增强 target |
| `JndiObjectFactoryBean` | JNDI 查到的 DataSource | ✗ 外部资源 lookup |
| `LocalSessionFactoryBean` | Hibernate SessionFactory | ✗ 复杂对象工厂 |

**记忆**：FactoryBean 抽象 **「造 Bean 的 Bean」**，不是 **「被增强的 Bean」**。AOP 详解见 [[8-Spring-AOP代理创建详解]]；动态代理概念见 [[1010-Java动态代理与运行时代理机制]]。

### 四类典型场景

| 场景 | 注册 | `getBean` 得到 | 代表 |
|------|------|---------------|------|
| **产品类型 ≠ 注册类型** | `ProxyFactoryBean` | 业务接口 **代理** | spring-aop |
| **复杂 / 动态创建** | 自定义 FactoryBean | `Connection` 等，可 prototype | 连接、脚本对象 |
| **框架基础设施** | 各类 `*FactoryBean` | EMF、ObjectMapper、Scope 代理 | spring-orm / spring-web |
| **类型推断** | FactoryBean + `getObjectType` / `OBJECT_TYPE_ATTRIBUTE` | 不实例化工厂即可 `@Autowired` 按类型注入 | since 5.2 |

→ 实现类清单见 [[#六、实现类分类]]

### 和 `@Bean` 工厂方法：现在谁更常用？

| | `FactoryBean` | `@Bean` 方法 |
|--|---------------|-------------|
| 注册 | 工厂 Bean；产品来自 `getObject` | 方法返回值 **就是** Bean |
| 适用 | **框架 / 基础设施** | **日常业务装配** ⭐ |
| 容器特性 | `&`、产品缓存、FactoryBean 类型推断 | 普通 Bean 流程 |

**Spring Boot 时代**：业务代码很少手写 `implements FactoryBean`；但读 AOP、MyBatis `MapperFactoryBean`、JPA 集成时仍绕不开本体系。

### 厨房比喻（与 [[3-速查-Spring厨房比喻大全]] 一致）

```text
普通 Bean     = 厨师本人就是菜
FactoryBean   = 面点师（员工 ≠ 包子）
getBean("bao")  → 包子（getObject 产品）
getBean("&bao") → 面点师本人
@Bean 方法     = 主厨现做，返回值直接上桌（更日常）
```

### 什么时候不必手写 FactoryBean？

满足以下全部时，用 **构造器注入 + `@Bean`** 即可：

- 产品类就是注册类（无代理、无动态生成）
- 创建逻辑可在 `@Configuration` 里写清
- 不需要产品与工厂 lifecycle 分离、不需要 `&` 取工厂

### 选择结论

> FactoryBean 让 IoC 容器统一管理 **「工厂 → 产品」**：`getBean`、按类型注入、singleton 产品缓存、`&` 前缀——框架集成的基础协议；业务层优先 `@Bean`，读源码必须懂 FactoryBean。

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
│      factoryBeanObjectCache  ← 缓存 getObject 产物               │
│      getObjectFromFactoryBean                                   │
│      ↑ AbstractBeanFactory                                        │
│        getObjectForBeanInstance  ← getBean 时分流               │
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

    @Nullable T getObject throws Exception;
    @Nullable Class<?> getObjectType;
    default boolean isSingleton { return true; }
}
```

| 方法 | 作用 | 容器如何使用 |
|------|------|-------------|
| `getObject` | 返回**产品对象** | `getBean("name")` 最终调用 |
| `getObjectType` | 产品类型 | 按类型查找、`@Autowired`；**应尽量提前返回，避免触发创建** |
| `isSingleton` | 产品是否单例 | `true` → 结果被 `factoryBeanObjectCache` 缓存 |

源码约束：

- 实现此接口的 Bean **不能当普通 Bean 用**
- 容器只管理 **FactoryBean 本身**的生命周期，**不自动销毁**产品
- **编程式契约**，`getObject` 可能在 BPP 就绪前被调用

### OBJECT_TYPE_ATTRIBUTE（since 5.2）

FactoryBean 的 class 无法推断产品类型时，在 `BeanDefinition` 上设置：

```java
beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, UserService.class);
```

容器在 `FactoryBeanRegistrySupport.getTypeForFactoryBeanFromAttributes` 读取，用于**不实例化 FactoryBean 就确定类型**。

---

## 二、SmartFactoryBean\<T\> — 扩展契约

```java
public interface SmartFactoryBean<T> extends FactoryBean<T> {
    default boolean isPrototype { return false; }
    default boolean isEagerInit { return false; }
}
```

| 方法 | 含义 | 默认值 |
|------|------|--------|
| `isPrototype` | 产品是否为**严格原型**（每次独立实例） | `false` |
| `isEagerInit` | 是否**急切初始化** FactoryBean 及其单例产品 | `false` |

与 `isSingleton` 的关系：

```text
isSingleton == false
  → 普通 FactoryBean：产品按需 getObject
  → SmartFactoryBean + isPrototype==true：明确为原型

isEagerInit == true
  → preInstantiateSingletons 时提前 getBean(name) 创建产品
  → 适用于启动时就绪的单例产品（如 AOP 代理）
```

`DefaultListableBeanFactory.instantiateSingleton`：

```java
if (isFactoryBean(beanName)) {
    getBean(FACTORY_BEAN_PREFIX + beanName);
    if (bean instanceof SmartFactoryBean<?> sfb && sfb.isEagerInit) {
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
    protected abstract T createInstance throws Exception;
    public abstract Class<?> getObjectType;
}
```

| 能力 | 实现 |
|------|------|
| 单例 / 原型切换 | `setSingleton(boolean)`，默认 `true` |
| 单例 eager 创建 | `afterPropertiesSet` → `createInstance` |
| 原型每次新建 | `getObject` → 每次 `createInstance` |
| 循环依赖早期代理 | `getEarlySingletonInterfaces` + JDK 动态代理 |
| 销毁 | `destroy` → `destroyInstance(singletonInstance)` |

`getObject` 逻辑：

```text
isSingleton == true
  → initialized ? singletonInstance : getEarlySingletonInstance（循环依赖代理）
isSingleton == false
  → 每次 createInstance
```

---

## 四、FactoryBeanRegistrySupport — 容器支持

位于 `AbstractBeanFactory` 与 `DefaultSingletonBeanRegistry` 之间。

### 4.1 两个缓存的区别

| 缓存 | 位置 | 存什么 |
|------|------|--------|
| `singletonObjects` | DefaultSingletonBeanRegistry | FactoryBean **实例本身** |
| `factoryBeanObjectCache` | FactoryBeanRegistrySupport | **`getObject` 的产物** |

### 4.2 getObjectFromFactoryBean 流程

```text
getObjectFromFactoryBean(factory, beanName, shouldPostProcess)
  │
  ├─ factory.isSingleton && containsSingleton(beanName)
  │     → 加锁 + synchronized(factory)
  │     → 查 factoryBeanObjectCache
  │     → 未命中 → factory.getObject
  │     → postProcessObjectFromFactoryBean（可选）
  │     → 写入 factoryBeanObjectCache
  │
  └─ 非单例产品 → getObject → postProcess
```

`doGetObjectFromFactoryBean` 异常映射：

| 异常 | 含义 |
|------|------|
| `FactoryBeanNotInitializedException` | → `BeanCurrentlyInCreationException` |
| `getObject` 返回 null 且正在创建 | → `BeanCurrentlyInCreationException` |
| 其他 | → `BeanCreationException` |

---

## 五、FactoryBeanNotInitializedException

```java
/**
 * FactoryBean 未完全初始化时 getObject 抛出（如循环依赖）。
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
  → 实现 createInstance + getObjectType
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
| false | false | — | 每次 getObject 新建 |
| false | true | — | 明确声明产品为原型 |
| FactoryBean 自身 | — | — | **几乎总是容器 singleton** |

---

## 八、完整调用链

```text
getBean("dataSource")                     // 要产品
  doGetBean
    getSingleton("dataSource")            // FactoryBean 实例
    getObjectForBeanInstance
      getObjectFromFactoryBean
        factoryBeanObjectCache 命中？返回
        否则 factory.getObject
          AbstractFactoryBean → createInstance
          ProxyFactoryBean    → createProxy
        postProcessObjectFromFactoryBean
    返回 Connection

getBean("&dataSource")                    // 要工厂
  getObjectForBeanInstance
    isFactoryDereference(&) → 返回 FactoryBean 实例
```

---

## 九、与相关概念对比

| 概念 | 关系 |
|------|------|
| `BeanFactory` | 容器；管理 FactoryBean 及其产品 |
| `FactoryBean` | 容器中的一种特殊 Bean |
| `@Bean` 方法 | 工厂方法，**不是** FactoryBean；业务装配首选 → [[#为什么需要 FactoryBean？#和 @Bean 工厂方法：现在谁更常用？]] |
| `ObjectFactory<T>` | 延迟获取；`ObjectFactoryCreatingFactoryBean` 包装 getBean |
| `ObjectProvider<T>` | 现代延迟/可选注入，部分替代 FactoryBean |

---

## 十、源码阅读路径

| 优先级 | 文件 | 关注点 |
|:------:|------|--------|
| 1 | `FactoryBean.java` | 三方法契约；**为什么需要**见 [[#为什么需要 FactoryBean？]] |
| 2 | `SmartFactoryBean.java` | isPrototype / isEagerInit |
| 3 | `AbstractFactoryBean.java` | 模板、早期代理 |
| 4 | `FactoryBeanRegistrySupport.java` | 产品缓存 |
| 5 | `AbstractBeanFactory.getObjectForBeanInstance` | getBean 分流 |
| 6 | `ProxyFactoryBean.java` | 典型框架实现 |

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[5-工厂Bean-BeanFactory与FactoryBean的区别]] | [[1-IoC与DI核心概念]] |

---

## 关联

- [[00-Spring-Framework核心机制-学习导航]]
- [[4-容器层-BeanFactory接口体系详解]]
- [[3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]
- [[5-工厂Bean-BeanFactory与FactoryBean的区别]]
- [[5-Context层-ApplicationContext详解]]
- [[2-速查-IoC与DI核心整合速查]]
- [[3-速查-Spring厨房比喻大全]]
- [[4-接口地图-IoC与DI重要接口大全]]

---
## 下一步可深入

- [ ] FactoryBean 循环依赖 vs 普通 Bean 三级缓存
- [ ] `postProcessObjectFromFactoryBean` 对产品做 BPP
- [ ] `ProxyFactoryBean` 创建代理完整流程
