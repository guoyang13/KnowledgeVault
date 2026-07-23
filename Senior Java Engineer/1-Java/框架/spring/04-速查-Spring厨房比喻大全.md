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
```

> 正式定义与对比表 → [[03-速查-IoC与DI核心整合速查]]

---

## 一、基础设施层

| 比喻 | Spring 概念 | 干什么 |
|------|------------|--------|
| 餐厅 | ApplicationContext | 管厨房 + 开业（refresh）+ 环境 + 事件 |
| 厨房 | BeanFactory | 按菜谱做菜、上菜（getBean） |
| 菜谱 | BeanDefinition | 描述一道菜怎么做 |
| 档案柜 | BeanDefinitionRegistry | 菜谱存哪、怎么登记 |

```text
ApplicationContext（餐厅）
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
BFPP            = 开伙前改菜谱
BPP             = 上桌前加料（AOP 在这）

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
| AOP 代理在哪生成？ | BPP 的 `postProcessAfterInitialization` |
| ObjectProvider 和 FactoryBean 区别？ | 前者延迟取已有 Bean；后者专门生产一种产品 |

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
- 下篇：[[16-IoC与DI核心概念]]

---
## 下一步可深入

- [ ] 按比喻对照 [[25-源码调试与断点指南]] 跟栈，看 BFPP / BPP 在 refresh 中的介入点
- [ ] `ConfigurationClassPostProcessor` — 主厨 @Bean 如何被解析注册
- [ ] `AbstractAutoProxyCreator` — 品控员如何生成 AOP 代理
