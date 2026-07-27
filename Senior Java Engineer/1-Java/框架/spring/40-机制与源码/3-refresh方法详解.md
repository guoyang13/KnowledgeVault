---
type: canonical
status: reviewed
topic: Spring ApplicationContext refresh
source_version: 6.2.x
---

# refresh 方法详解

> 导航：[[00-Spring-Framework核心机制-学习导航]] · **40 · 机制与源码** · refresh 机制
>
> 源码：`spring-context/.../support/AbstractApplicationContext.java`（`refresh` 及全部子方法）
>
> 前置：[[2-Bean加载原理与源码阅读路径]] · [[5-Context层-ApplicationContext详解]]
>
> 关联：[[7-IoC扩展点三部曲对照]] · [[1-扩展点层-BeanFactoryPostProcessor详解]] · [[2-扩展点层-BeanPostProcessor详解]] · [[5-依赖注入实现原理]] · [[3-生命周期层-Aware体系详解]]
>
> 实战：一个测试贯穿各阶段的 Aware/Processor 调用链见 [[2-测试驱动的refresh调用链-Aware与Processor]]

---

## 一句话

`refresh` 是 **ApplicationContext 容器启动的总编排方法**：从激活容器、配置 BeanFactory、执行扩展点，到预实例化单例、发布启动完成事件，**全部在这一条链里完成**。

```java
new AnnotationConfigApplicationContext(AppConfig.class)
  → register          // 登记 BeanDefinition（构造方法里，refresh 之前）
  → refresh           // ★ 本文
```

---

## 总览：12 个阶段

```text
refresh
│
├─ 【加锁】startupShutdownLock.lock
│
├─ 阶段 1  prepareRefresh                    激活容器、属性源、监听器快照
├─ 阶段 2  obtainFreshBeanFactory             获取/刷新 BeanFactory
├─ 阶段 3  prepareBeanFactory                  配置 ClassLoader、SpEL、Aware、环境 singleton
├─ 阶段 4  postProcessBeanFactory              子类扩展点（默认空）
├─ 阶段 5  invokeBeanFactoryPostProcessors    ★ 解析 @Configuration/@Bean，改 BeanDefinition
├─ 阶段 6  registerBeanPostProcessors         注册 BPP（@Autowired、AOP 等）
├─ 阶段 7  initMessageSource                   国际化
├─ 阶段 8  initApplicationEventMulticaster     事件广播器
├─ 阶段 9  onRefresh                           子类扩展点（如启动 Tomcat）
├─ 阶段 10 registerListeners                   注册监听器、补发早期事件
├─ 阶段 11 finishBeanFactoryInitialization      ★ preInstantiateSingletons → getBean
└─ 阶段 12 finishRefresh                       清缓存、Lifecycle、ContextRefreshedEvent
│
├─ 【异常】destroyBeans → cancelRefresh → throw
└─ 【解锁】startupShutdownLock.unlock
```

### 与 Bean 加载两阶段的对应

| refresh 阶段 | Bean 加载阶段 | 是否创建 Bean 实例 |
|-------------|--------------|-------------------|
| 阶段 1～4 | 准备 | 否 |
| 阶段 5 BFPP | **阶段一**：注册/修改 BeanDefinition | 否 |
| 阶段 6 BPP 注册 | 扩展点就位 | 否 |
| 阶段 7～10 | 基础设施 + 监听器 | 否（个别 getBean 除外） |
| 阶段 11 | **阶段二**：实例化 + DI + 初始化 | **是** |
| 阶段 12 | 启动收尾 | 否 |

---

## 阶段 1：prepareRefresh

**作用**：标记容器激活，校验环境，为事件机制做准备。

```text
prepareRefresh
├─ startupDate = now              记录启动时间
├─ closed = false / active = true     isActive 变为 true
├─ initPropertySources            子类扩展：Web 环境注入 Servlet PropertySource
├─ getEnvironment
│     .validateRequiredProperties 校验 required 属性是否可解析
├─ 快照 applicationListeners        重复 refresh 时恢复监听器列表
└─ earlyApplicationEvents = new Set  暂存 multicaster 就绪前的事件
```

| 方法 | 说明 |
|------|------|
| `initPropertySources` | 模板方法，默认空；`WebApplicationContext` 子类覆盖 |

---

## 阶段 2：obtainFreshBeanFactory

**作用**：拿到 `ConfigurableListableBeanFactory`，供后续所有步骤使用。

```text
obtainFreshBeanFactory
├─ refreshBeanFactory     ← 子类实现
└─ getBeanFactory         返回内部 factory
```

| 实现类 | refreshBeanFactory 行为 |
|--------|--------------------------|
| `GenericApplicationContext` | 校验只 refresh 一次 + 设置 `serializationId`（**不重建** factory） |
| `AbstractRefreshableApplicationContext` | **销毁旧 factory**，加载 XML 定义，创建新 factory |

> 注解路线：`AnnotationConfigApplicationContext` 构造时已 `new DefaultListableBeanFactory`，refresh 时 factory 不变，register/scan 的 BeanDefinition 已在 factory 里。

---

## 阶段 3：prepareBeanFactory

**作用**：给 BeanFactory 装配 Context 级能力，**此阶段仍不创建业务 Bean**。

```text
prepareBeanFactory(beanFactory)
├─ setBeanClassLoader                    Bean 类加载
├─ setBeanExpressionResolver             SpEL（@Value("#{...}")）
├─ addPropertyEditorRegistrar            Resource 类型属性编辑器
├─ addBeanPostProcessor
│     ApplicationContextAwareProcessor     注入 ApplicationContext 等 Aware
├─ ignoreDependencyInterface(...)          Aware 接口不参与 autowire
├─ registerResolvableDependency(...)       可注入 BeanFactory / Context 自身
├─ addBeanPostProcessor
│     ApplicationListenerDetector          检测内部 Bean 是否为 Listener
├─ [可选] LoadTimeWeaverAwareProcessor     存在 loadTimeWeaver bean 时
└─ registerSingleton                     environment / systemProperties / systemEnvironment / applicationStartup
```

> ApplicationContextAware 等回调的完整体系 → [[3-生命周期层-Aware体系详解]]

---

## 阶段 4：postProcessBeanFactory

**作用**：子类扩展点，**默认空实现**。

- 调用时机：`prepareBeanFactory` 之后、BFPP 之前
- BeanDefinition 已加载，**尚未实例化任何 Bean**
- `WebApplicationContext` 等子类可在此注册额外 BPP

---

## 阶段 5：invokeBeanFactoryPostProcessors ★

**作用**：执行所有 **BeanFactoryPostProcessor**，注册/修改 BeanDefinition。

```text
invokeBeanFactoryPostProcessors
└─ PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors
      ├─ 先执行手动 addBeanFactoryPostProcessor 添加的
      ├─ 再按 PriorityOrdered → Ordered → 无序 执行容器中的 BFPP
      └─ ★ ConfigurationClassPostProcessor
            → 解析 @Configuration / @Bean / @Import / @ComponentScan
            → 注册更多 BeanDefinition
```

**此阶段只操作「蓝图」，不 `new` 业务对象。**

→ 详见 [[1-扩展点层-BeanFactoryPostProcessor详解]] · [[7-IoC扩展点三部曲对照]]

---

## 阶段 6：registerBeanPostProcessors

**作用**：实例化并注册所有 **BeanPostProcessor**，供后续 `createBean` 使用。

```text
registerBeanPostProcessors
└─ PostProcessorRegistrationDelegate.registerBeanPostProcessors
      ├─ AutowiredAnnotationBeanPostProcessor    @Autowired / @Value
      ├─ CommonAnnotationBeanPostProcessor       @PostConstruct / @PreDestroy
      ├─ ApplicationListenerDetector
      └─ ... 其他 BPP（AOP 自动代理等）
```

→ 详见 [[2-扩展点层-BeanPostProcessor详解]]

---

## 阶段 7～8：基础设施

### initMessageSource

- 查找名为 `messageSource` 的 Bean；没有则注册 `DelegatingMessageSource`
- 支持父子容器 MessageSource 继承

### initApplicationEventMulticaster

- 查找名为 `applicationEventMulticaster` 的 Bean；没有则注册 `SimpleApplicationEventMulticaster`
- 后续 `publishEvent` 都通过它广播

---

## 阶段 9：onRefresh

**作用**：子类扩展点，**默认空**；在 singleton 实例化**之前**调用。

- `ServletWebServerApplicationContext`：在此创建并启动内嵌 Tomcat/Jetty

---

## 阶段 10：registerListeners

```text
registerListeners
├─ 1. addApplicationListener           静态注册的监听器（非 Bean）
├─ 2. addApplicationListenerBean       ApplicationListener 类型的 Bean 名称
│        ↑ allowEagerInit=true，只查定义，不实例化 FactoryBean
└─ 3. multicastEvent(earlyEvents)        补发 prepareRefresh 阶段缓存的早期事件
```

---

## 阶段 11：finishBeanFactoryInitialization ★

**作用**：预实例化所有**非 lazy 单例**——容器启动时真正 **创建 Bean 实例** 的阶段。

```text
finishBeanFactoryInitialization
├─ prepareSingletonBootstrap           标记进入单例预实例化阶段
├─ [可选] setBootstrapExecutor         后台线程预实例化
├─ [可选] setConversionService         类型转换
├─ addEmbeddedValueResolver            解析 @Value("${key}")（若 BFPP 未注册）
├─ getBean(BeanFactoryInitializer)       提前初始化器
├─ getBean(LoadTimeWeaverAware)          提前织入相关 Bean
├─ setTempClassLoader(null)              清除临时 ClassLoader
├─ freezeConfiguration                 冻结 BeanDefinition，禁止再改
└─ preInstantiateSingletons            ★ 核心
      └─ getBean(name) → doGetBean → doCreateBean
            ├─ createBeanInstance       实例化
            ├─ populateBean             DI ← [[5-依赖注入实现原理]]
            └─ initializeBean           Aware + @PostConstruct + BPP
```

→ 详见 [[2-Bean加载原理与源码阅读路径]] · [[2-元数据层-AnnotatedBeanDefinitionReader与组件注册详解#FAQ：类存在但没有被使用，Spring 会怎样处理？]]

---

## 阶段 12：finishRefresh

```text
finishRefresh
├─ resetCommonCaches           清理反射/注解/ResolvableType 缓存
├─ clearResourceCaches         清理 ASM 扫描缓存
├─ initLifecycleProcessor      SmartLifecycle 管理器
├─ lifecycleProcessor.onRefresh  通知 SmartLifecycle 组件启动
└─ publishEvent(ContextRefreshedEvent)  ★ 容器启动完成事件
```

---

## 异常回滚

refresh 过程中抛出 `RuntimeException` / `Error` 时：

```text
catch
├─ destroyBeans        getBeanFactory.destroySingletons  销毁已创建单例
├─ cancelRefresh(ex)     active=false + resetCommonCaches
└─ throw ex              容器启动失败
```

→ 销毁机制详解（登记 vs 执行、DisposableBeanAdapter）：[[9-Bean 销毁机制详解]]

---

## 完整流程图（带中文）

```text
【用户】new AnnotationConfigApplicationContext(AppConfig.class)
         │
         ├─ register(AppConfig)              ← refresh 之前：登记配置类 Blueprint
         │
         └─ refresh
               │
               ├─ 1 prepareRefresh          激活容器
               ├─ 2 obtainFreshBeanFactory  拿到 DefaultListableBeanFactory
               ├─ 3 prepareBeanFactory     装配 Context 能力
               ├─ 4 postProcessBeanFactory  子类扩展
               ├─ 5 invokeBFPP             ★ @Configuration → 更多 BeanDefinition
               ├─ 6 registerBPP            注册 @Autowired 处理器等
               ├─ 7 initMessageSource       国际化
               ├─ 8 initEventMulticaster    事件
               ├─ 9 onRefresh               子类扩展（Web 服务器）
               ├─ 10 registerListeners      监听器
               ├─ 11 finishBeanFactoryInit  ★ getBean 创建所有非 lazy 单例
               └─ 12 finishRefresh          ContextRefreshedEvent
```

---

## 调试断点推荐

| 顺序 | 方法 | 观察什么 |
|:----:|------|---------|
| 1 | `refresh` | 总入口 |
| 2 | `invokeBeanFactoryPostProcessors` | @Bean 如何注册成 BeanDefinition |
| 3 | `registerBeanPostProcessors` | BPP 注册顺序 |
| 4 | `finishBeanFactoryInitialization` | 进入实例化阶段 |
| 5 | `preInstantiateSingletons` | 哪些 Bean 被 eager 创建 |
| 6 | `finishRefresh` | ContextRefreshedEvent |

→ 完整断点清单 [[1-源码调试与断点指南]]

---

## 记忆口诀

- **前 4 步**：搭舞台（激活、拿 factory、配 factory、子类扩展）
- **第 5 步**：改图纸（BFPP，只动 BeanDefinition）
- **第 6 步**：请助手（BPP 注册到位）
- **7～10 步**：装设施（消息、事件、监听器）
- **第 11 步**：正式开业（创建 Bean）
- **第 12 步**：剪彩（发布启动完成事件）

---

## 常见误区

| 误区 | 正解 |
|------|------|
| `register` 会创建 Bean | 只登记 BeanDefinition；实例化在阶段 11 |
| BFPP 阶段会 `@Autowired` | BFPP 只改定义；DI 在 `populateBean`（阶段 11） |
| `refresh` 可无限次调用 | `GenericApplicationContext` 只允许 refresh 一次 |
| 所有 Bean 都在 refresh 时创建 | 只有**非 lazy 单例**；`@Lazy` / prototype 延迟创建 |

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[2-Bean加载原理与源码阅读路径]] | [[7-IoC扩展点三部曲对照]] |

---

## 关联

- [[2-Bean加载原理与源码阅读路径]]
- [[5-Context层-ApplicationContext详解]]
- [[7-IoC扩展点三部曲对照]]
- [[1-扩展点层-BeanFactoryPostProcessor详解]]
- [[2-扩展点层-BeanPostProcessor详解]]
- [[5-依赖注入实现原理]]
- [[3-生命周期层-Aware体系详解]]
- [[2-元数据层-AnnotatedBeanDefinitionReader与组件注册详解#FAQ：类存在但没有被使用，Spring 会怎样处理？]]
- [[1-源码调试与断点指南]]
