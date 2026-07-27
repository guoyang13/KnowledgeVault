---
type: quick-reference
status: reviewed
topic: Spring Framework terminology
source_version: 6.2.x
aliases:
  - Spring 核心概念中英文术语表
  - 7-术语表-Spring核心概念中英文对照
---

# Spring 核心概念中英文术语表

> [!abstract] 使用规则
> 正文中概念第一次出现时写“中文名（English Term / API）”，之后使用中文简称或 API 名。不要在同一篇中无规则地交替使用 IoC 容器、Container、Bean Factory 等近义表达。

## 1. 容器与依赖

| 中文标准名 | English Term / API | 推荐简称 | 权威入口 |
| --- | --- | --- | --- |
| 控制反转 | Inversion of Control, IoC | IoC | [[1-IoC与DI核心概念]] |
| 依赖注入 | Dependency Injection, DI | DI | [[5-依赖注入实现原理]] |
| Bean 工厂 | Bean Factory / `BeanFactory` | BeanFactory | [[4-容器层-BeanFactory接口体系详解]] |
| 应用上下文 | Application Context / `ApplicationContext` | ApplicationContext | [[5-Context层-ApplicationContext详解]] |
| 依赖描述符 | Dependency Descriptor / `DependencyDescriptor` | DependencyDescriptor | [[Spring注入注解与byType-byName解析逻辑]] |
| 可解析类型 | Resolvable Type / `ResolvableType` | ResolvableType | [[5-依赖注入实现原理]] |
| 对象提供器 | Object Provider / `ObjectProvider<T>` | ObjectProvider | [[getBeanProvider与ObjectProvider有什么用]] |

## 2. 元数据与注册

| 中文标准名 | English Term / API | 推荐简称 | 权威入口 |
| --- | --- | --- | --- |
| Bean 定义 | Bean Definition / `BeanDefinition` | BeanDefinition | [[1-元数据层-BeanDefinition三兄弟详解]] |
| Bean 定义注册表 | Bean Definition Registry / `BeanDefinitionRegistry` | Registry | [[3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]] |
| 组件扫描 | Component Scanning | 组件扫描 | [[2-元数据层-AnnotatedBeanDefinitionReader与组件注册详解]] |
| 配置类 | Configuration Class / `@Configuration` | 配置类 | [[1-注解入门-配置类与组件类]] |
| 组件类 | Component Class / `@Component` | 组件类 | [[1-注解入门-配置类与组件类]] |
| 工厂型 Bean | Factory Bean / `FactoryBean<T>` | FactoryBean | [[4-工厂Bean-FactoryBean接口体系详解]] |
| Bean 工厂方法 | Bean Factory Method / `@Bean` | `@Bean` 方法 | [[1-注解入门-配置类与组件类]] |

`BeanFactory` 与 `FactoryBean` 必须保留 API 大小写，不翻译成同一个“Bean 工厂”：

- `BeanFactory`：容器契约；
- `FactoryBean<T>`：容器中的特殊 Bean，用于生产另一对象。

## 3. 生命周期与扩展

| 中文标准名 | English Term / API | 推荐简称 | 权威入口 |
| --- | --- | --- | --- |
| Bean 工厂后处理器 | BeanFactory Post-Processor / `BeanFactoryPostProcessor` | BFPP | [[1-扩展点层-BeanFactoryPostProcessor详解]] |
| Bean 后处理器 | Bean Post-Processor / `BeanPostProcessor` | BPP | [[2-扩展点层-BeanPostProcessor详解]] |
| 容器感知回调 | Aware Callback / `Aware` | Aware | [[3-生命周期层-Aware体系详解]] |
| Bean 作用域 | Bean Scope / `Scope` | Scope | [[5-生命周期层-Bean作用域与生命周期边界]] |
| 实例化 | Instantiation | 实例化 | [[4-doCreateBean深度解析]] |
| 属性填充 | Property Population | 属性填充 | [[4-doCreateBean深度解析]] |
| 初始化 | Initialization | 初始化 | [[4-doCreateBean深度解析]] |
| 销毁 | Destruction | 销毁 | [[9-Bean 销毁机制详解]] |
| 早期 Bean 引用 | Early Bean Reference | 早期引用 | [[6-循环依赖与三级缓存详解]] |

“实例化”和“初始化”不是同义词：实例化产生对象，初始化发生在依赖填充之后并执行回调与后处理。

## 4. AOP 与事务

| 中文标准名 | English Term / API | 推荐简称 | 权威入口 |
| --- | --- | --- | --- |
| 面向切面编程 | Aspect-Oriented Programming, AOP | AOP | [[8-Spring-AOP代理创建详解]] |
| 连接点 | Join Point | 连接点 | [[8-Spring-AOP代理创建详解]] |
| 切点 | Pointcut | 切点 | [[8-Spring-AOP代理创建详解]] |
| 通知 | Advice | Advice / 通知 | [[8-Spring-AOP代理创建详解]] |
| 顾问 | Advisor | Advisor | [[8-Spring-AOP代理创建详解]] |
| 方法拦截器 | Method Interceptor / `MethodInterceptor` | 拦截器 | [[8-Spring-AOP代理创建详解]] |
| 声明式事务 | Declarative Transaction Management | 声明式事务 | [[10-Spring事务实现详解]] |
| 事务属性 | Transaction Attribute | 事务属性 | [[10-Spring事务实现详解]] |
| 事务管理器 | Transaction Manager / `TransactionManager` | 事务管理器 | [[10-Spring事务实现详解]] |

Java 动态代理（Java Dynamic Proxy）是运行时代理机制；Spring AOP 是在代理机制上组织切点、通知和拦截器链。基础概念见 [[1010-Java动态代理与运行时代理机制]]。

## 5. Context 基础抽象

| 中文标准名 | English Term / API | 推荐简称 | 权威入口 |
| --- | --- | --- | --- |
| 环境 | Environment / `Environment` | Environment | [[6-Context层-Environment与PropertySource详解]] |
| 属性源 | Property Source / `PropertySource` | PropertySource | [[6-Context层-Environment与PropertySource详解]] |
| 配置档案 | Profile | Profile | [[6-Context层-Environment与PropertySource详解]] |
| 资源 | Resource / `Resource` | Resource | [[7-Context层-Resource与ResourceLoader详解]] |
| 资源加载器 | Resource Loader / `ResourceLoader` | ResourceLoader | [[7-Context层-Resource与ResourceLoader详解]] |
| 应用事件 | Application Event / `ApplicationEvent` | 应用事件 | [[8-Context层-ApplicationEvent事件机制详解]] |
| 事件多播器 | Application Event Multicaster | 多播器 | [[8-Context层-ApplicationEvent事件机制详解]] |
| 类型转换服务 | Conversion Service / `ConversionService` | ConversionService | [[11-类型转换-ConversionService与DataBinder边界]] |
| 数据绑定器 | Data Binder / `DataBinder` | DataBinder | [[11-类型转换-ConversionService与DataBinder边界]] |

## 6. 容易误用的表达

| 不推荐 | 推荐 |
| --- | --- |
| Spring 按类型或按名称二选一注入 | 类型兼容筛选后，再应用限定符、Primary、优先级和依赖名称等规则 |
| Spring 单例是 JVM 单例 | 每个 BeanFactory、每个 Bean 名称一个共享实例 |
| BPP 在 Bean 创建后执行 | BPP 可介入实例化、属性填充、初始化前后及销毁阶段，取决于子接口 |
| Aware 就是特殊的 `@Autowired` | Aware 是容器生命周期协议，DI 是依赖提供机制 |
| 三级缓存解决所有循环依赖 | 主要支持部分单例属性注入循环，并受代理和创建时机约束 |
| Spring 事件就是异步消息 | 默认通常是同进程同步多播 |
