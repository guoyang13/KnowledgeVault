# BeanFactoryPostProcessor 详解

> 导航：[[00-Spring-Bean加载-学习导航]] · **上篇 01–15** · 扩展点层 · BFPP
>
> 前置：[[05-接口地图-IoC与DI重要接口大全#二、扩展点接口（IoC 的灵魂）]] · [[10-Context层-ApplicationContext详解]] · [[06-元数据层-BeanDefinition三兄弟详解]]
>
> 关联：[[19-IoC扩展点三部曲对照]] · [[12-扩展点层-BeanPostProcessor详解]] · [[18-refresh方法详解]]
>
> 本地源码：`/Users/guoyang/IdeaProjects/spring/spring-framework`

---

## 一句话

`BeanFactoryPostProcessor`（BFPP）是 Spring 在 **Bean 实例化之前** 修改容器配置的扩展点——操作对象是 `BeanDefinition`（蓝图），不是 Bean 实例。`@Configuration` 解析、`${...}` 占位符替换等核心能力都依赖它。

> 速查版见 [[05-接口地图-IoC与DI重要接口大全#2.1 BeanFactoryPostProcessor — 开伙前改菜谱 ★]]；本篇是完整深入版。

---

## 一、它是什么

```java
@FunctionalInterface
public interface BeanFactoryPostProcessor {

    /**
     * Modify the application context's internal bean factory after its standard
     * initialization. All bean definitions will have been loaded, but no beans
     * will have been instantiated yet.
     */
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException;
}
```

**核心特征：**

- 只有一个方法：`postProcessBeanFactory()`
- 操作 **`BeanDefinition`（元数据）**，不操作 Bean 实例
- 调用时机：**所有定义已加载，但尚未实例化任何 Bean**
- 在 `refresh()` 中，`invokeBeanFactoryPostProcessors()` 早于 `registerBeanPostProcessors()` 和 `preInstantiateSingletons()`

源码关键约束（L31-35）：

> A BFPP may interact with and modify bean **definitions**, but never bean **instances**.  
> Doing so may cause premature bean instantiation, violating the container.

| | BeanFactoryPostProcessor | BeanPostProcessor |
|--|--------------------------|-------------------|
| **操作对象** | BeanDefinition（蓝图） | Bean 实例 |
| **调用时机** | 实例化**之前** | 每个 Bean 创建过程中 |
| **典型用途** | 改定义、注册新定义、解析占位符 | 注入、代理、初始化增强 |
| **代表** | `ConfigurationClassPostProcessor` | `AutowiredAnnotationBeanPostProcessor` |
| **refresh 步骤** | `invokeBeanFactoryPostProcessors()` | `registerBeanPostProcessors()` |

→ BPP 完整版：[[12-扩展点层-BeanPostProcessor详解]]

---

## 二、接口继承体系

```text
BeanFactoryPostProcessor                    ← 根接口，1 个方法
  │
  └── BeanDefinitionRegistryPostProcessor   ← 子接口，可更早注册新定义
        postProcessBeanDefinitionRegistry()  ← 额外方法
        postProcessBeanFactory()             ← 继承（通常 default 空实现或二次处理）
```

### BeanDefinitionRegistryPostProcessor

```java
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {

    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
            throws BeansException;

    @Override
    default void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    }
}
```

| 对比 | BFPP | RegistryPostProcessor |
|------|------|----------------------|
| 改已有定义 | ✅ | ✅ |
| **注册新 BeanDefinition** | ❌（一般不这么做） | ✅ |
| 执行时机 | 第二阶段 | **第一阶段**（更早） |
| 典型代表 | `PropertySourcesPlaceholderConfigurer` | `ConfigurationClassPostProcessor` |

**为什么需要子接口？**

RegistryPostProcessor 可以在常规 BFPP 检测之前**动态注册更多 BeanDefinition**——这些新定义里可能又包含 BFPP Bean，形成「注册 → 发现新 BFPP → 再注册」的扩展链。`ConfigurationClassPostProcessor` 解析 `@Bean` 就是典型场景。

---

## 三、在 refresh() 中的位置

```text
refresh()
├── prepareRefresh()
├── obtainFreshBeanFactory()              // BeanDefinition 已加载（scan / register / XML）
├── prepareBeanFactory()
├── invokeBeanFactoryPostProcessors()     ← BFPP 在这里 ★
├── registerBeanPostProcessors()          ← BPP 注册
├── ... (initMessageSource 等)
└── finishBeanFactoryInitialization()     ← preInstantiateSingletons()，开始实例化
```

**心智模型：**

```text
阶段一：加载 BeanDefinition（scan / register / XML）
阶段二：BFPP 加工定义（改蓝图 / 注册更多定义）    ← 本篇
阶段三：注册 BPP
阶段四：实例化 Bean（getBean / preInstantiateSingletons）
```

---

## 四、invokeBeanFactoryPostProcessors() 完整流程

**文件**：`PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors()`（L68）

这是 BFPP 体系最核心、也最复杂的方法，分 **两大阶段**：

### 阶段 A：BeanDefinitionRegistryPostProcessor

```text
1. 先处理 Context 编程式注册的 RegistryPostProcessor
     → postProcessBeanDefinitionRegistry()

2. 按顺序 getBean 并 invoke 容器中的 RegistryPostProcessor：
     a. PriorityOrdered
     b. Ordered
     c. 普通（无顺序接口）
     d. ★ while 循环：直到没有新的 RegistryPostProcessor 出现

3. 对所有已处理的 RegistryPostProcessor
     → 再调用 postProcessBeanFactory()
```

**while 循环的意义**（L134-150）：

```text
RegistryPostProcessor A 注册了新的 RegistryPostProcessor B
  → 下一轮循环发现并处理 B
  → B 又可能注册 C ...
  → 直到 registry 中不再有未处理的 RegistryPostProcessor
```

### 阶段 B：普通 BeanFactoryPostProcessor

```text
4. 按顺序 getBean 并 invoke 剩余的 BFPP（跳过阶段 A 已处理的）：
     a. PriorityOrdered
     b. Ordered
     c. 普通

5. beanFactory.clearMetadataCache()    // 定义可能被改过，清合并缓存
```

**完整流程图：**

```text
invokeBeanFactoryPostProcessors()
│
├─ [阶段 A] BeanDefinitionRegistryPostProcessor
│     ├─ 编程式注册的 → postProcessBeanDefinitionRegistry()
│     ├─ PriorityOrdered RegistryPostProcessor
│     ├─ Ordered RegistryPostProcessor
│     ├─ while 循环处理剩余 RegistryPostProcessor
│     └─ 全部 → postProcessBeanFactory()
│
├─ [阶段 B] BeanFactoryPostProcessor
│     ├─ PriorityOrdered BFPP
│     ├─ Ordered BFPP
│     └─ 普通 BFPP
│
└─ clearMetadataCache()
```

---

## 五、排序规则

与 BPP 相同（详见 [[12-扩展点层-BeanPostProcessor详解#十一、排序规则]]）：

| 规则 | 说明 |
|------|------|
| `PriorityOrdered` | 最高优先级 |
| `Ordered` | 次优先级 |
| `@Order` 注解 | **对 BFPP 无效** |
| 编程式 `context.addBeanFactoryPostProcessor()` | 在阶段 A 最先处理 Registry 部分 |

---

## 六、BFPP 的注册

### 6.1 三种注册方式

| 方式 | 示例 | 说明 |
|------|------|------|
| **编程式** | `context.addBeanFactoryPostProcessor(...)` | 阶段 A 最先执行 |
| **声明为 Bean** | `@Component` / `@Bean` | 容器自动检测 |
| **框架内置** | `AnnotationConfigUtils.registerAnnotationConfigProcessors()` | 注册 `ConfigurationClassPostProcessor` |

### 6.2 @Configuration 中注册 — 应使用 static

```java
@Configuration
public class AppConfig {

    @Bean
    static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
```

**为什么应使用 static？**（Spring 官方强烈推荐，见 `BeanFactoryPostProcessor.java` L43-47）

BFPP 在 Bean 实例化之前就要生效。非 static `@Bean` 会先实例化 `@Configuration` 配置类；**在 BFPP/BPP 链注册完成之前被创建出来的 Bean**，可能无法被后续 PostProcessor 完整处理。

典型问题：同一配置类上既有非 static BFPP `@Bean`，又有 `@Value` 字段——配置类必须先实例化，但占位符 BFPP 尚未生效（Spring 测试 `ConfigurationClassWithPlaceholderConfigurerBeanTests`）。

**注意：不是「一定会有部分 Bean 无法完整后处理」**——PostProcessor 链注册完成后创建的业务 Bean 通常不受影响。详见 [[12-扩展点层-BeanPostProcessor详解#13.2 @Configuration 中注册 — 应使用 static]] 中的情况对照表。

### 6.3 框架自动注册

`AnnotationConfigUtils.registerAnnotationConfigProcessors()` 注册：

| Bean 名 | 类 | 类型 |
|---------|-----|------|
| `internalConfigurationAnnotationProcessor` | `ConfigurationClassPostProcessor` | RegistryPostProcessor ★ |

---

## 七、框架内置 BFPP 代表

| 类 | 类型 | 核心职责 |
|----|------|----------|
| **`ConfigurationClassPostProcessor`** | RegistryPostProcessor + PriorityOrdered | 解析 `@Configuration` / `@Bean` / `@Import` / `@ComponentScan` ★ |
| `PropertySourcesPlaceholderConfigurer` | BFPP（通过 PropertyResourceConfigurer） | 解析 `${...}` 占位符 |
| `PropertyPlaceholderConfigurer` | BFPP | 旧版占位符解析（≤3.0 XSD） |
| `PropertyOverrideConfigurer` | BFPP | 覆盖已有属性值 |
| `CustomScopeConfigurer` | BFPP + Ordered | 注册自定义 Scope |
| `CustomEditorConfigurer` | BFPP + Ordered | 注册 PropertyEditor |
| `CustomAutowireConfigurer` | BFPP + Ordered | 注册自定义 autowire 模式 |
| `DeprecatedBeanWarner` | BFPP | 警告使用了 `@Deprecated` 的 Bean |
| `AspectJWeavingEnabler` | BFPP + Ordered | 启用 AspectJ 织入 |

---

## 八、ConfigurationClassPostProcessor 深入

Spring 注解驱动最核心的 BFPP，理解它就理解了 `@Configuration` 如何变成 BeanDefinition。

### 8.1 两个回调

```java
// 阶段 A：注册更多 BeanDefinition
@Override
public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    processConfigBeanDefinitions(registry);
}

// 阶段 B：CGLIB 增强配置类
@Override
public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    enhanceConfigurationClasses(beanFactory);           // Full 模式 CGLIB 代理
    beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(...));
}
```

### 8.2 processConfigBeanDefinitions() 做了什么

```text
processConfigBeanDefinitions(registry)
  → 找出所有 @Configuration 候选
  → ConfigurationClassParser.parse()
       ├─ 解析 @ComponentScan → 扫描更多类
       ├─ 解析 @Import → 导入其他配置
       ├─ 解析 @ImportResource → 导入 XML
       └─ 解析 @PropertySource
  → ConfigurationClassBeanDefinitionReader.loadBeanDefinitions()
       └─ 每个 @Bean 方法 → registerBeanDefinition()
```

详见 [[01-注解入门-配置类与组件类]]、[[02-注解入门-Configuration与Service等注解区别]]。

### 8.3 时序示意

```text
@ComponentScan 注册了 AppConfig（@Configuration）
  │
  ▼
ConfigurationClassPostProcessor.postProcessBeanDefinitionRegistry()
  → 发现 AppConfig 上的 @ComponentScan
  → 扫描并 registerBeanDefinition(UserService, UserRepository, ...)
  → 发现 AppConfig 上的 @Bean
  → registerBeanDefinition(dataSource, txManager, ...)
  │
  ▼
ConfigurationClassPostProcessor.postProcessBeanFactory()
  → enhanceConfigurationClasses()   // AppConfig CGLIB 增强
  │
  ▼
（后续 BFPP 如 PropertySourcesPlaceholderConfigurer 替换 ${...}）
  │
  ▼
registerBeanPostProcessors() → preInstantiateSingletons()
```

---

## 九、PropertySourcesPlaceholderConfigurer 深入

第二重要的 BFPP——解析 BeanDefinition 中的 `${...}` 占位符。

### 9.1 做什么

- 遍历所有 `BeanDefinition` 的属性值
- 把 `${jdbc.url}` 替换为 Environment / PropertySource 中的实际值
- 同时支持 `@Value("${...}")` 注解（通过注册 `StringValueResolver`）

### 9.2 示例

```xml
<property name="url" value="${jdbc.url}"/>
```

BFPP 阶段替换为：

```xml
<property name="url" value="jdbc:mysql://localhost:3306/mydb"/>
```

### 9.3 与 @Value 的关系

| 机制 | 处理时机 | 处理方 |
|------|----------|--------|
| XML / `@Bean` 中的 `${...}` | BFPP 阶段 | `PropertySourcesPlaceholderConfigurer` |
| 字段 `@Value("${...}")` | 实例化后 | `AutowiredAnnotationBeanPostProcessor`（BPP） |

---

## 十、BFPP 能做什么 / 不能做什么

### ✅ 可以

- 修改已有 `BeanDefinition` 的属性值、scope、lazy 等
- 注册新的 `BeanDefinition`（RegistryPostProcessor）
- 注册自定义 `Scope`、`PropertyEditor`
- 解析占位符、覆盖属性
- 读取 Environment，按条件修改定义

### ❌ 不可以

- **不要** `beanFactory.getBean()` 获取普通 Bean（会导致过早实例化）
- **不要** 修改 Bean 实例（那是 BPP 的事）
- **不要** 在 BFPP 中依赖尚未注册完成的 BPP

---

## 十一、自定义 BFPP

### 11.1 只改已有定义

```java
@Component
public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        if (beanFactory.containsBeanDefinition("dataSource")) {
            BeanDefinition bd = beanFactory.getBeanDefinition("dataSource");
            bd.getPropertyValues().add("maxPoolSize", 20);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
```

### 11.2 注册新定义（RegistryPostProcessor）

```java
@Component
public class MyRegistryPostProcessor
        implements BeanDefinitionRegistryPostProcessor, Ordered {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        RootBeanDefinition bd = new RootBeanDefinition(MyService.class);
        registry.registerBeanDefinition("myService", bd);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // 可选：二次加工
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
```

### 11.3 @Configuration 中注册

```java
@Configuration
public class AppConfig {

    @Bean
    static MyBeanFactoryPostProcessor myBeanFactoryPostProcessor() {
        return new MyBeanFactoryPostProcessor();
    }
}
```

`@Bean` 方法**应使用 static**，理由见 [[#6.2 @Configuration 中注册 — 应使用 static]]。

---

## 十二、常见误区

| 误区 | 正解 |
|------|------|
| BFPP 可以改 Bean 实例 | 只能改 BeanDefinition；改实例用 BPP |
| BFPP 里可以 `@Autowired` 其他 Bean | 会导致过早实例化，应避免 |
| `@Configuration` 解析在 scan 阶段完成 | scan 只注册配置类本身；`@Bean` 展开在 BFPP 阶段 |
| BFPP 和 BPP 执行顺序可以调换 | 固定：先 BFPP → 再注册 BPP → 再实例化 |
| `@Order` 可以排序 BFPP | 必须 `Ordered` / `PriorityOrdered` |
| RegistryPostProcessor 只执行一次 | while 循环直到没有新的 RegistryPostProcessor |
| 非 static BFPP `@Bean` 一定导致部分 Bean 缺后处理 | 仅**早期被连带创建**的 Bean 有风险；链注册完成后创建的 Bean 通常正常 |

---

## 十三、源码阅读顺序

| 步骤 | 文件 | 关注点 |
|:----:|------|--------|
| 1 | `BeanFactoryPostProcessor.java` | 根接口契约 |
| 2 | `BeanDefinitionRegistryPostProcessor.java` | 子接口 |
| 3 | `PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors()` | 两阶段 + while 循环 |
| 4 | `AbstractApplicationContext.refresh()` | BFPP 在 refresh 中的位置 |
| 5 | `ConfigurationClassPostProcessor.java` | `@Configuration` 解析 ★ |
| 6 | `ConfigurationClassParser.java` | `@Import` / `@ComponentScan` 解析 |
| 7 | `ConfigurationClassBeanDefinitionReader.java` | `@Bean` 方法注册 |
| 8 | `PropertySourcesPlaceholderConfigurer.java` | 占位符替换 |
| 9 | 对比 [[12-扩展点层-BeanPostProcessor详解]] | BFPP vs BPP 分工 |

建议在 `ConfigurationClassPostProcessor.postProcessBeanDefinitionRegistry()` 上打断点，配合 [[25-源码调试与断点指南]]。

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[10-Context层-ApplicationContext详解]] | [[12-扩展点层-BeanPostProcessor详解]] |

---

## 关联

- [[00-Spring-Bean加载-学习导航]]
- [[17-Bean加载原理与源码阅读路径]]
- [[06-元数据层-BeanDefinition三兄弟详解]]
- [[10-Context层-ApplicationContext详解]]
- [[05-接口地图-IoC与DI重要接口大全]]
- [[12-扩展点层-BeanPostProcessor详解]]
- [[13-生命周期层-Aware体系详解]]
- [[01-注解入门-配置类与组件类]]
- [[02-注解入门-Configuration与Service等注解区别]]
- [[25-源码调试与断点指南]]
- [[04-速查-Spring厨房比喻大全]]
- [[19-IoC扩展点三部曲对照]]

---
## 下一步可深入

- [ ] `ConfigurationClassParser` — `@Import` / `@ComponentScan` 解析细节
- [ ] `ConfigurationClassEnhancer` — CGLIB 增强配置类
- [ ] `@Conditional` / `@Profile` — 条件装配在 BFPP 阶段的处理
- [ ] BFPP vs BPP 对照 — 与 [[12-扩展点层-BeanPostProcessor详解]] 联合复习
