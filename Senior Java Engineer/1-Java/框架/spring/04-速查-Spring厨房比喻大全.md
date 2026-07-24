# Spring 厨房比喻大全

> 导航：[[00-Spring-Bean加载-学习导航]] · **上篇 01–15** · 速查 · 厨房比喻
>
> 定位：用「餐厅 / 厨房」比喻理解 Spring IoC 各角色 — 适合建立直觉、面试前速记
>
> 前置：[[03-速查-IoC与DI核心整合速查]]（结构 + 机制整合速查）
>
> 关联：[[14-工厂Bean-BeanFactory与FactoryBean的区别]] · [[15-工厂Bean-FactoryBean接口体系详解]] · [[01-注解入门-配置类与组件类]]

---

## 一句话

Spring IoC 容器 = **一家餐厅**；Bean 的创建与管理 = **厨房运作**。  
**ApplicationContext** = 餐厅经理（管开业、环境、事件）；**BeanFactory** = 厨房。  
FactoryBean 只是其中一种「特殊工种」——**面点师**；厨房里还有主厨、传菜员、改菜谱的行政总厨、品控加料员等。

---

## 厨房总分工

```text
ApplicationContext     = 餐厅经理（管开业、环境、广播、开业流程）
BeanFactory            = 整个厨房
BeanDefinition         = 菜谱
BeanDefinitionRegistry = 菜谱档案柜

普通 Bean（@Service 等）= 普通厨师（自己就是菜，直接上桌）
FactoryBean            = 面点师（本人是员工，上桌的是包子/代理）

@Bean 工厂方法         = 主厨按单现做
ObjectFactory/Provider = 传菜窗口（要时才去厨房取）
Supplier               = 预约单（延迟才开工）
MethodInvokingFactoryBean = 帮工（调用某方法，把结果当菜端出去）

BeanFactoryPostProcessor = 开伙前改菜谱的行政总厨
BeanPostProcessor        = 上桌前品控 / 加料员（AOP 代理在这）

Aware 接口族             = 厨师的「知情权登记表」（声明需要厨房/经理/环境等资源）
invokeAwareMethods       = 厨房后勤进场时直接通知（BeanFactory 级 Aware）
ApplicationContextAwareProcessor = 品控员按登记表逐一通知（Context 级 Aware，仍是 BPP）
```

> 正式定义与对比表 → [[03-速查-IoC与DI核心整合速查#Processor 与 Aware 一句话定位]]

---

## Processor 与 Aware 速查总表

与 [[03-速查-IoC与DI核心整合速查#Processor 与 Aware 对照（机制 + 比喻）]] 对照阅读。

| Spring 概念 | 厨房比喻 | 改什么 | 何时 | 典型代表 |
|-------------|---------|--------|------|---------|
| **BFPP** | 开伙前改菜谱的**行政总厨** | BeanDefinition | 实例化**前** | `ConfigurationClassPostProcessor` |
| **BPP** | 上桌前**品控 / 加料员** | Bean 实例 | 实例化**后** | `AutowiredAnnotationBeanPostProcessor`、AOP |
| **Aware 接口族** | 厨师**知情权登记表** | Bean 声明要基础设施 | `initializeBean` | `ApplicationContextAware` 等 |
| **invokeAwareMethods** | **厨房后勤**直接通知 | 回调 BeanFactory 级 Aware | BeforeInit 前 | `BeanFactoryAware` 等 |
| **ApplicationContextAwareProcessor** | **品控员**按表通知 | 回调 Context 级 Aware | BPP BeforeInit | `EnvironmentAware` 等 |

```text
refresh()
  └── BFPP 改菜谱（Definition）
        └── createBean()
              ├── populateBean → BPP（@Autowired 配餐）
              └── initializeBean()
                    ├── 后勤 invokeAwareMethods（BeanFactoryAware…）
                    ├── 品控 BeforeInit → ApplicationContextAwareProcessor
                    ├── @PostConstruct
                    └── 品控 AfterInit → AOP 代理
```

| 对比 | BFPP | BPP | Aware |
|------|------|-----|-------|
| 是独立扩展点？ | ✓ | ✓ | ✗（生命周期回调；Context 级由 BPP 兑现） |
| 业务代码常用？ | 少（框架） | 少（框架） | 少（业务用 @Autowired 代替） |
| 与 DI 关系 | 可改定义影响注入 | 执行 `@Autowired` | 给 Context/Environment，不是业务 Bean |

→ 展开：[[#十、开伙前改菜谱 — BeanFactoryPostProcessor]] · [[#十一、上桌前加料 — BeanPostProcessor]] · [[#十二、知情权登记 — Aware 接口族]]  
→ 深入：[[11-扩展点层-BeanFactoryPostProcessor详解]] · [[12-扩展点层-BeanPostProcessor详解]] · [[13-生命周期层-Aware体系详解]]

---

## 一、基础设施层

| 比喻 | Spring 概念 | 干什么 |
|------|------------|--------|
| 餐厅经理 | ApplicationContext | 管厨房 + 开业（refresh）+ 环境 + 事件 |
| 厨房 | BeanFactory | 按菜谱做菜、上菜（getBean） |
| 菜谱 | BeanDefinition | 描述一道菜怎么做 |
| 档案柜 | BeanDefinitionRegistry | 菜谱存哪、怎么登记 |

> **一家餐厅** = IoC 整体比喻；**经理 ≠ 整栋楼**。Context 是经理，DLBF 是厨房实体。

```text
一家餐厅（Spring IoC 整体）
  ApplicationContext（餐厅经理）
    └── DefaultListableBeanFactory（厨房）
            ├── 档案柜存菜谱（BeanDefinitionRegistry）
            └── 按菜谱做菜（BeanFactory.getBean）
```

→ 详见 [[03-速查-IoC与DI核心整合速查]] · [[10-Context层-ApplicationContext详解]]

---

## 二、普通厨师 — 组件 Bean

```java
@Service
public class UserService { ... }
```

| 特点 | 说明 |
|------|------|
| 比喻 | 普通厨师，**本人就是菜** |
| 注册 | 扫描 → BeanDefinition → Registry |
| 获取 | `getBean("userService")` → 直接拿到 UserService 实例 |

最日常、最常见的 Bean 类型。

→ 详见 [[01-注解入门-配置类与组件类]] · [[02-注解入门-Configuration与Service等注解区别]]

---

## 三、面点师 — FactoryBean

```java
@Component
public class MyProxyFactory implements FactoryBean<UserService> {
    @Override
    public UserService getObject() { return createProxy(); }
    // ...
}
```

| 特点 | 说明 |
|------|------|
| 比喻 | 面点师，**员工 ≠ 产品** |
| 获取产品 | `getBean("myProxy")` → `getObject()` 的返回值 |
| 获取本人 | `getBean("&myProxy")` → FactoryBean 本身 |

```text
BeanFactory  = 整个厨房
FactoryBean  = 厨房里的面点师（做包子的人，不是包子本身）
```

→ 详见 [[14-工厂Bean-BeanFactory与FactoryBean的区别]] · [[15-工厂Bean-FactoryBean接口体系详解]]

---

## 四、各专精面点师 — FactoryBean 实现类

面点师也分工种，各自只做一类「产品」：

| 比喻 | 类 | 端出什么 |
|------|-----|---------|
| 代理面点师 | `ProxyFactoryBean` | AOP 代理对象 |
| 作用域面点师 | `ScopedProxyFactoryBean` | request/session 作用域代理 |
| 外部进货员 | `JndiObjectFactoryBean` | JNDI 远程对象 |
| ORM 面点师 | `LocalContainerEntityManagerFactoryBean` | JPA EntityManagerFactory |
| Mapper 面点师 | `MapperFactoryBean`（MyBatis） | Mapper 接口代理 |
| 集合面点师 | `ListFactoryBean` / `MapFactoryBean` | 集合 Bean |
| 方法调用帮工 | `MethodInvokingFactoryBean` | 某 static/实例方法的返回值 |
| 服务定位厨师 | `ServiceLocatorFactoryBean` | 动态 Service Locator 代理 |
| 延迟取 Bean 包装 | `ObjectFactoryCreatingFactoryBean` | 包装成 ObjectFactory 的传菜口 |

→ 完整列表见 [[15-工厂Bean-FactoryBean接口体系详解#五、常见实现类]]

---

## 五、升级版面点师 — SmartFactoryBean

普通面点师的增强版，能额外告诉厨房：

| 方法 | 比喻 | 默认 |
|------|------|------|
| `isPrototype()` | 产品是「每单现做」还是「统一备货」 | `false`（单例产品） |
| `isEagerInit()` | 开业时是否先把包子蒸好 | `false`（按需） |

```java
public interface SmartFactoryBean<T> extends FactoryBean<T> {
    default boolean isPrototype() { return false; }
    default boolean isEagerInit() { return false; }
}
```

主要供**框架内部**使用；应用层 FactoryBean 一般实现 plain `FactoryBean` 即可。

→ 详见 [[15-工厂Bean-FactoryBean接口体系详解#二、SmartFactoryBean\<T\> — 扩展契约]]

---

## 六、主厨 — @Bean 工厂方法

不是 FactoryBean，但同样是「厨师做菜」：

```java
@Configuration
public class AppConfig {
    @Bean
    public UserService userService(OrderRepository repo) {
        return new UserServiceImpl(repo);  // 主厨现做，直接当 Bean 端出去
    }
}
```

| | FactoryBean（面点师） | @Bean 工厂方法（主厨） |
|--|---------------------|----------------------|
| 形式 | 实现 `FactoryBean` 接口的类 | 配置类里的方法 |
| 注册进容器 | FactoryBean 本身 | 方法**返回值**直接是 Bean |
| 常见场景 | 框架基础设施（代理、JNDI） | 日常业务装配 |
| 日常常见度 | ⭐⭐ | ⭐⭐⭐⭐⭐ |

→ 详见 [[01-注解入门-配置类与组件类]] · [[06-元数据层-BeanDefinition三兄弟详解#ConfigurationClassBeanDefinition]]

---

## 七、传菜窗口 — ObjectFactory / ObjectProvider

不亲自做菜，**需要时才去厨房取**：

```java
@Autowired
private ObjectProvider<UserService> userServiceProvider;

public void doWork() {
    UserService svc = userServiceProvider.getIfAvailable(); // 要时才 getBean
}
```

| | FactoryBean | ObjectProvider |
|--|-------------|----------------|
| 比喻 | 做产品的面点师 | 传菜员 / 取餐口 |
| 产出 | 通过 `getObject()` 造新产品 | 延迟调用 `getBean()` |
| 使用频率 | 框架层 | 现代应用层更常见 |

`ObjectFactoryCreatingFactoryBean` = 把「传菜窗口」本身包装成 Bean 的一种 FactoryBean。

---

## 八、预约单厨师 — Supplier

Java 8+ 可用 `Supplier` 延迟注册 / 创建 Bean：

```java
// Spring 5.0+ GenericApplicationContext.registerBean 等 API
context.registerBean("userService", UserService.class,
    (Supplier<UserService>) () -> new UserServiceImpl());
```

比喻：**先接单，真正下单才进厨房做**。

| | @Bean 主厨 | Supplier |
|--|-----------|----------|
| 时机 | 配置解析时注册定义 | 可更灵活地延迟实例化 |
| 常见度 | ⭐⭐⭐⭐⭐ | ⭐⭐ |

---

## 九、帮工档口 — 静态 / 实例工厂方法

菜谱里写「调用哪个档口的方法来做」：

```xml
<!-- XML 老式写法 -->
<bean id="userService" factory-bean="userServiceFactory" factory-method="create"/>
```

```java
public class UserServiceFactory {
    public static UserService create() { ... }  // 静态工厂
    public UserService createInstance() { ... } // 实例工厂
}
```

BeanDefinition 上对应 `factoryMethodName`、`factoryBeanName` 字段。

→ 详见 [[06-元数据层-BeanDefinition三兄弟详解]]

---

## 十、开伙前改菜谱 — BeanFactoryPostProcessor

不是厨师，但在**实例化之前改 Blueprint**：

```java
@Component
public class MyBFPP implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) {
        BeanDefinition bd = factory.getBeanDefinition("userService");
        bd.getPropertyValues().add("timeout", 3000); // 改菜谱，还没做菜
    }
}
```

| 角色 | 时机 | 比喻 |
|------|------|------|
| **BFPP** | 实例化**之前** | 开伙前改菜谱的行政总厨 |
| **BPP** | 实例化**之后** | 上桌前品控 / 加料员 |

典型 BFPP：

| 类 | 干什么 |
|----|--------|
| `ConfigurationClassPostProcessor` | 解析 `@Configuration`、`@Bean`、`@Import` |
| `PropertySourcesPlaceholderConfigurer` | 解析 `${...}` 占位符 |
| `MapperScannerConfigurer` | MyBatis Mapper 扫描注册 |

---

## 十一、上桌前加料 — BeanPostProcessor

菜做好后、上桌前**再加工**：

```java
@Component
public class MyBPP implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String name) {
        // AOP 代理常在这里生成
        return bean;
    }
}
```

常见「品控员」：

| 类 | 负责 |
|----|------|
| `AutowiredAnnotationBeanPostProcessor` | `@Autowired` / `@Value` |
| `CommonAnnotationBeanPostProcessor` | `@PostConstruct` / `@PreDestroy` |
| `AbstractAutoProxyCreator` | AOP 代理生成 |
| `ApplicationContextAwareProcessor` | ApplicationContext 相关 Aware 回调 |

→ 生命周期完整顺序见 [[09-容器层-BeanFactory与Registry详解#1.5 Bean 生命周期（BeanFactory 定义的标准顺序）]]

---

## 十二、知情权登记 — Aware 接口族

厨师实现 `XxxAware`，等于在「知情权登记表」上打勾：**「请厨房在合适时机告诉我某类基础设施」**。  
容器不会因为你实现了接口就自动注入——必须有**后勤**或**品控员**按表通知（回调 `setXxx()`）。

### 12.1 和 DI（@Autowired）的比喻区别

| | Aware 回调 | `@Autowired` 注入 |
|--|-----------|------------------|
| 比喻 | 厨房/经理**按登记表主动通知**你 | 传菜员把**协作同事**送到你工位 |
| 给什么 | 厨房基础设施（Context、Environment、BeanFactory） | 业务 Bean（Service、Repository） |
| 谁处理 | 后勤 / 品控员（Aware 处理器） | 品控员 `AutowiredAnnotationBeanPostProcessor`（也是 BPP） |
| 日常业务 | 少用 | **首选** ★ |

### 12.2 两套通知机制

```text
做菜流程（initializeBean 内）
  ├── populateBean()           传菜员送协作同事（@Autowired）
  └── initializeBean()
        ├── invokeAwareMethods()           ← 厨房后勤直接通知 ★
        │     BeanNameAware / BeanFactoryAware / BeanClassLoaderAware
        ├── BPP BeforeInit
        │     ApplicationContextAwareProcessor  ← 品控员按表通知 ★
        │     EnvironmentAware、ApplicationContextAware 等
        ├── @PostConstruct
        └── BPP AfterInit（AOP 代理）
```

| 机制 | 比喻 | 典型接口 |
|------|------|----------|
| **invokeAwareMethods()** | 厨房**后勤**进场时直接通知 | `BeanNameAware`、`BeanFactoryAware`、`BeanClassLoaderAware` |
| **ApplicationContextAwareProcessor** | **品控员**上桌前按登记表逐一通知 | `EnvironmentAware`、`ApplicationContextAware`、`ResourceLoaderAware` 等 |

> 品控员（BPP）的一种专项工作 = 处理 Context 级 Aware；不是第三种独立角色。

### 12.3 常见「登记项」速查

| 登记（Aware 接口） | 厨房通知你什么 | 谁通知 |
|-------------------|---------------|--------|
| `BeanNameAware` | 你在档案柜里的编号（beanName） | 后勤 |
| `BeanFactoryAware` | 整个厨房（BeanFactory） | 后勤 |
| `BeanClassLoaderAware` | 厨房统一用的锅具标准（ClassLoader） | 后勤 |
| `EnvironmentAware` | 餐厅环境配置（Profile、properties） | 品控员 |
| `ApplicationContextAware` | 餐厅经理（ApplicationContext） | 品控员 |
| `ResourceLoaderAware` | 进货渠道（ResourceLoader） | 品控员 |

### 12.4 示例

```java
@Service
public class MyService implements ApplicationContextAware {
    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        // 品控员 ApplicationContextAwareProcessor 在上桌前回调
    }
}
```

→ 展开：[[13-生命周期层-Aware体系详解#零、总结速查 ★]] · [[100-Q&A/Aware体系总结与常见问题]]

---

## 全角色对照表

| 比喻 | Spring 角色 | 产出 / 作用 | 日常常见度 |
|------|------------|------------|-----------|
| 餐厅经理 | ApplicationContext | 编排开业 + 环境 + 事件 | ⭐⭐⭐⭐⭐ |
| 厨房 | BeanFactory | getBean、生命周期 | ⭐⭐⭐⭐ |
| 菜谱 | BeanDefinition | 描述 Bean 元数据 | ⭐⭐⭐ |
| 档案柜 | BeanDefinitionRegistry | 存定义 | ⭐⭐⭐ |
| 普通厨师 | `@Service` 等组件 | 本人即 Bean | ⭐⭐⭐⭐⭐ |
| 主厨 | `@Bean` 工厂方法 | 方法返回值 | ⭐⭐⭐⭐⭐ |
| 面点师 | FactoryBean | `getObject()` 产品 | ⭐⭐（框架层） |
| 升级面点师 | SmartFactoryBean | 同上 + 更多控制 | ⭐（框架内部） |
| 传菜窗口 | ObjectProvider | 延迟 getBean | ⭐⭐⭐⭐ |
| 预约单 | Supplier | 延迟实例化 | ⭐⭐ |
| 帮工档口 | factory-method | 指定方法产出 | ⭐⭐ |
| 改菜谱 | BeanFactoryPostProcessor | 改 BeanDefinition | ⭐⭐ |
| 品控加料 | BeanPostProcessor | 改 Bean 实例 | ⭐⭐⭐ |
| 知情权登记 | Aware 接口族 | 声明需要厨房基础设施 | ⭐⭐ |
| 后勤通知 | invokeAwareMethods | BeanFactory 级 Aware 回调 | ⭐⭐ |
| 品控按表通知 | ApplicationContextAwareProcessor | Context 级 Aware 回调 | ⭐⭐ |
| Processor 速查 | BFPP + BPP + Aware | 改菜谱 / 加料 / 知情权（见 [[#Processor 与 Aware 速查总表]]） | ⭐⭐⭐ |

---

## 完整故事线：一道菜从登记到上桌

```text
1. 餐厅经理开业（ApplicationContext.refresh()）

2. 行政总厨改菜谱（BFPP：解析 @Configuration、@Bean）

3. 菜谱入档案柜（BeanDefinition → Registry）

4. 经理下令开伙（finishBeanFactoryInitialization → preInstantiateSingletons）

5. 厨房按菜谱做菜
     ├── 普通厨师 → 直接 new + 注入 + 初始化
     ├── 主厨 @Bean → 调工厂方法，返回值当 Bean
     └── 面点师 FactoryBean → getObject() 才是上桌的菜

5.5 后勤 / 品控按 Aware 登记表通知（BeanFactory 级 + Context 级）

6. 品控加料（BPP：@Autowired、AOP 代理）

7. 上桌（Bean 就绪，可被注入 / getBean）

8. 传菜窗口（ObjectProvider：业务代码要时才取）
```

---

## 记忆口诀（完整版）

```text
Definition      = 菜谱
Registry        = 档案柜
BeanFactory     = 厨房
ApplicationContext = 餐厅经理

普通 Bean       = 厨师本人就是菜
@Bean 方法       = 主厨现做
FactoryBean     = 面点师（员工 ≠ 产品）
ObjectProvider  = 传菜窗口
Supplier        = 预约单
一家餐厅        = Spring IoC 整体（不是 ApplicationContext 别名）
ApplicationContext = 餐厅经理
BFPP            = 开伙前改菜谱
BPP             = 上桌前加料（AOP 在这）
Aware           = 知情权登记表（要厨房/经理通知我）
后勤            = invokeAwareMethods（BeanFactory 级 Aware）
品控按表通知    = ApplicationContextAwareProcessor（Context 级 Aware）

getBean("name")   → 普通 Bean 拿实例；FactoryBean 拿产品
getBean("&name")  → 拿 FactoryBean 本身
```

---

## 常见面试题速答

| 问题 | 答案 |
|------|------|
| FactoryBean 和 @Bean 区别？ | FactoryBean 注册的是工厂 Bean，产品是 `getObject()`；@Bean 注册的是方法返回值本身 |
| 除了 FactoryBean 还有哪些「生产者」？ | @Bean 方法、factory-method、Supplier、ObjectProvider |
| BFPP 和 BPP 区别？ | BFPP 改定义（开伙前）；BPP 改实例（上桌前） |
| Aware 是什么？ | 厨师的「知情权登记表」；后勤/品控按表回调 setXxx() |
| Aware 和 @Autowired 区别？ | Aware 通知厨房基础设施；@Autowired 注入业务协作 Bean |
| ApplicationContextAwareProcessor 是什么？ | 品控员的一种工作：处理 Context 级 Aware 登记 |
| AOP 代理在哪生成？ | BPP 的 `postProcessAfterInitialization` |
| ObjectProvider 和 FactoryBean 区别？ | 前者延迟取已有 Bean；后者专门生产一种产品 |
| ApplicationContext 是整栋餐厅吗？ | 不是；**一家餐厅**=IoC 整体，**Context**=餐厅经理，**BeanFactory**=厨房 |

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[03-速查-IoC与DI核心整合速查]] | [[05-接口地图-IoC与DI重要接口大全]] |

---

## 关联

- [[00-Spring-Bean加载-学习导航]]
- [[03-速查-IoC与DI核心整合速查]]
- [[05-接口地图-IoC与DI重要接口大全]]
- [[01-注解入门-配置类与组件类]]
- [[09-容器层-BeanFactory与Registry详解]]
- [[14-工厂Bean-BeanFactory与FactoryBean的区别]] · [[15-工厂Bean-FactoryBean接口体系详解]]
- [[03-速查-IoC与DI核心整合速查#Processor 与 Aware 一句话定位]]
- [[13-生命周期层-Aware体系详解]] · [[11-扩展点层-BeanFactoryPostProcessor详解]] · [[12-扩展点层-BeanPostProcessor详解]]
- 下篇：[[16-IoC与DI核心概念]]

---
## 下一步可深入

- [ ] 按比喻对照 [[25-源码调试与断点指南]] 跟栈，看 BFPP / BPP 在 refresh 中的介入点
- [ ] `ConfigurationClassPostProcessor` — 主厨 @Bean 如何被解析注册
- [ ] `AbstractAutoProxyCreator` — 品控员如何生成 AOP 代理
- [ ] `ApplicationContextAwareProcessor` — 品控员如何按 Aware 登记表通知
