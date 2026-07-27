---
type: quick-reference
status: reviewed
topic: Spring IoC / DI interface map
source_version: 6.2.x
aliases:
  - IoC 与 DI 重要接口大全
  - 5-接口地图-IoC与DI重要接口大全
---

# IoC 与 DI 重要接口地图

> [!abstract] 阅读方法
> 先按“容器、元数据、创建、扩展、生命周期、依赖消费”定位接口，再进入主文档。这里是地图，不重复接口源码。

## 1. 容器接口族

| API | 中文定位 | 关键能力 |
| --- | --- | --- |
| `BeanFactory` | Bean 工厂根接口 | `getBean`、类型判断、作用域查询 |
| `ListableBeanFactory` | 可枚举 Bean 工厂 | 按类型批量查询 Bean 和定义 |
| `HierarchicalBeanFactory` | 分层 Bean 工厂 | 父子容器关系 |
| `AutowireCapableBeanFactory` | 可自动装配 Bean 工厂 | 创建、填充、初始化外部实例 |
| `ConfigurableBeanFactory` | 可配置 Bean 工厂 | 后处理器、作用域、类型转换、销毁登记 |
| `ConfigurableListableBeanFactory` | 完整内部配置接口 | 合并定义、冻结配置、预实例化单例 |
| `ApplicationContext` | 应用上下文 | 在容器之上组合事件、资源、环境、国际化 |

主要实现：`DefaultListableBeanFactory` 同时承担 Bean 定义注册、依赖解析和常用容器能力。

深入阅读：[[4-容器层-BeanFactory接口体系详解]] · [[5-Context层-ApplicationContext详解]]

## 2. 元数据与注册

| API | 中文定位 | 关键能力 |
| --- | --- | --- |
| `BeanDefinition` | Bean 定义 | 描述类型、作用域、构造参数、属性、初始化与销毁 |
| `AnnotatedBeanDefinition` | 注解 Bean 定义 | 暴露注解元数据 |
| `BeanDefinitionHolder` | Bean 定义持有者 | 组合定义、名称与别名 |
| `BeanDefinitionRegistry` | Bean 定义注册表 | 注册、删除、查询定义 |
| `BeanDefinitionReader` | Bean 定义读取器 | 从特定配置来源读取并注册定义 |
| `AnnotatedBeanDefinitionReader` | 注解定义读取器 | 注册配置类与注解类 |
| `ClassPathBeanDefinitionScanner` | 类路径扫描器 | 扫描候选组件并注册定义 |

深入阅读：[[1-元数据层-BeanDefinition三兄弟详解]] · [[2-元数据层-AnnotatedBeanDefinitionReader与组件注册详解]] · [[3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]]

## 3. 容器扩展点

| API | 处理对象 | 时机 | 常见实现或用途 |
| --- | --- | --- | --- |
| `BeanFactoryPostProcessor` | Bean 定义/工厂 | 普通 Bean 实例化前 | 属性占位符、定义调整 |
| `BeanDefinitionRegistryPostProcessor` | 注册表 | 常规工厂后处理前 | 继续注册 Bean 定义 |
| `BeanPostProcessor` | Bean 实例 | 初始化前后 | 注解处理、代理包装 |
| `InstantiationAwareBeanPostProcessor` | 实例化和属性填充阶段 | 实例化前后、填充前 | 自定义实例化、参与属性注入 |
| `SmartInstantiationAwareBeanPostProcessor` | 类型预测、构造器、早期引用 | 创建期间 | AOP 代理与循环依赖协作 |
| `MergedBeanDefinitionPostProcessor` | 合并后的定义 | 创建实例前 | 缓存注入和生命周期元数据 |

深入阅读：[[1-扩展点层-BeanFactoryPostProcessor详解]] · [[2-扩展点层-BeanPostProcessor详解]] · [[7-IoC扩展点三部曲对照]]

## 4. 生命周期协议

| API / 注解 | 中文名 | 发生位置 |
| --- | --- | --- |
| `Aware` 接口族 | 容器感知回调 | 属性填充后、初始化前 |
| `@PostConstruct` | 构造后回调 | 初始化阶段 |
| `InitializingBean` | 初始化 Bean | `afterPropertiesSet` |
| 自定义 `init-method` | 自定义初始化方法 | 初始化回调末段 |
| `@PreDestroy` | 销毁前回调 | 销毁阶段 |
| `DisposableBean` | 可销毁 Bean | `destroy` |
| 自定义 `destroy-method` | 自定义销毁方法 | 销毁回调末段 |

深入阅读：[[3-生命周期层-Aware体系详解]] · [[4-doCreateBean深度解析]] · [[9-Bean 销毁机制详解]]

## 5. 依赖消费与对象生产

| API | 中文定位 | 适用场景 |
| --- | --- | --- |
| `ObjectFactory<T>` | 对象工厂 | 最小化的延迟获取回调 |
| `ObjectProvider<T>` | 对象提供器 | 可选、延迟、按顺序或流式取得依赖 |
| `Provider<T>` | Jakarta 提供器 | 标准化的延迟依赖获取 |
| `FactoryBean<T>` | 工厂型 Bean | 把复杂第三方对象的生产接入容器 |
| `Supplier<T>` | 对象供应函数 | 以函数注册实例化逻辑 |

深入阅读：[[getBeanProvider与ObjectProvider有什么用]] · [[4-工厂Bean-FactoryBean接口体系详解]]

## 6. 依赖解析核心类型

| API | 作用 |
| --- | --- |
| `DependencyDescriptor` | 描述一个待解析的字段、参数或方法依赖 |
| `ResolvableType` | 保存包含泛型信息的可解析类型 |
| `AutowireCandidateResolver` | 判断候选是否满足限定、懒加载等规则 |
| `DefaultListableBeanFactory#resolveDependency` | 依赖解析总入口 |
| `DefaultListableBeanFactory#doResolveDependency` | 候选查找与单值选择主流程 |

深入阅读：[[5-依赖注入实现原理]] · [[Spring注入注解与byType-byName解析逻辑]]

## 7. 注解驱动的关键处理器

| 实现类 | 主要职责 |
| --- | --- |
| `ConfigurationClassPostProcessor` | 解析配置类、组件扫描、`@Bean` 等定义来源 |
| `AutowiredAnnotationBeanPostProcessor` | 处理 `@Autowired`、`@Value` 等注入元数据 |
| `CommonAnnotationBeanPostProcessor` | 处理常见 Jakarta 注解及相关回调 |
| `ApplicationContextAwareProcessor` | 驱动部分 `ApplicationContextAware` 子接口回调 |
| `AnnotationAwareAspectJAutoProxyCreator` | 根据切面与 Advisor 创建 AOP 代理 |

## 8. 源码阅读优先级

1. **容器主线**：`BeanFactory` → `DefaultListableBeanFactory` → `ApplicationContext`
2. **启动主线**：`AbstractApplicationContext#refresh`
3. **创建主线**：`AbstractBeanFactory#doGetBean` → `AbstractAutowireCapableBeanFactory#doCreateBean`
4. **注入主线**：`populateBean` → `resolveDependency`
5. **扩展主线**：`BeanFactoryPostProcessor` → `BeanPostProcessor` → `Aware`
6. **高级问题**：循环依赖、AOP 代理、声明式事务

对应入口：[[1-源码调试与断点指南]] · [[6-循环依赖与三级缓存详解]] · [[8-Spring-AOP代理创建详解]] · [[10-Spring事务实现详解]]

## 9. 最小掌握集

能够解释下面 12 个名称，已足以覆盖大部分 Spring IoC / DI 主线：

`BeanDefinition`、`BeanDefinitionRegistry`、`BeanFactory`、`ApplicationContext`、`DefaultListableBeanFactory`、`BeanFactoryPostProcessor`、`BeanPostProcessor`、`Aware`、`FactoryBean`、`ObjectProvider`、`DependencyDescriptor`、`ResolvableType`。

不要孤立背接口；始终把它放回“处理什么对象、在什么时机、改变什么结果”三个问题中。
