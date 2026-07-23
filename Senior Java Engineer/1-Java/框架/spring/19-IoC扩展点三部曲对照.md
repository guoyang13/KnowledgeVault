# IoC 扩展点三部曲对照

> 导航：[[00-Spring-Bean加载-学习导航]] · **下篇 16–25** · 扩展点总览（接口详见上篇 11–13）
>
> refresh 逐步解析：[[18-refresh方法详解]]
>
> 深入版：[[11-扩展点层-BeanFactoryPostProcessor详解]] · [[12-扩展点层-BeanPostProcessor详解]] · [[13-生命周期层-Aware体系详解]] · [[20-依赖注入实现原理]]
>
> 速查：[[05-接口地图-IoC与DI重要接口大全#二、扩展点接口（IoC 的灵魂）]]

---

## 一句话

Spring IoC 的三大扩展机制按 `refresh()` 时序排列：**BFPP 改定义 → BPP 改实例 → Aware 推基础设施**；DI（`@Autowired`）发生在 BPP 参与的 `populateBean()` 阶段。

---

## refresh() 时序对照

```text
refresh()
├── invokeBeanFactoryPostProcessors()     ← BFPP（上篇 11）：改 BeanDefinition
├── registerBeanPostProcessors()          ← BPP（上篇 12）：注册到工厂
└── finishBeanFactoryInitialization()
      └── getBean() → doCreateBean()
            ├── populateBean()            ← DI（下篇 21）：BPP.postProcessProperties()
            └── initializeBean()
                  ├── invokeAwareMethods()           ← Aware（上篇 13，BeanFactory 级）
                  ├── BPP BeforeInit                 ← Aware（Context 级，上篇 12）
                  ├── @PostConstruct
                  └── BPP AfterInit                  ← AOP 代理（下篇 23）
```

---

## 三者一页纸对比

| 维度 | BFPP | BPP | Aware |
|--|------|-----|-------|
| **操作对象** | BeanDefinition | Bean 实例 | Bean 实例 |
| **refresh 步骤** | `invokeBeanFactoryPostProcessors()` | `registerBeanPostProcessors()` + 创建过程 | `initializeBean()` 内 |
| **核心接口** | `BeanFactoryPostProcessor` | `BeanPostProcessor` | `XxxAware` 标记体系 |
| **谁处理** | 容器直接 invoke | BPP 链逐条调用 | `invokeAwareMethods()` + 专用 BPP |
| **典型代表** | `ConfigurationClassPostProcessor` | `AutowiredAnnotationBeanPostProcessor` | `ApplicationContextAwareProcessor` |
| **能注册新定义？** | RegistryPostProcessor 可以 | 否 | 否 |
| **能换代理对象？** | 否 | 能（AfterInit） | 否 |
| **深入** | [[11-扩展点层-BeanFactoryPostProcessor详解]] | [[12-扩展点层-BeanPostProcessor详解]] | [[13-生命周期层-Aware体系详解]] |

---

## DI 与三者的关系

DI 不是第四套独立扩展点，而是 **BPP 机制的具体应用**：

```text
@Autowired / @Value
  → AutowiredAnnotationBeanPostProcessor（BPP）
  → populateBean() → postProcessProperties()
```

详见 [[20-依赖注入实现原理]]。

---

## 推荐阅读顺序（按 refresh 时序）

```text
20 总览（下篇）→ 11 BFPP → 12 BPP → 13 Aware（上篇）→ 21 DI → 23 AOP → 24 事务
```

| 顺序 | 笔记 | 在 refresh 中的位置 |
|:----:|------|-------------------|
| 0 | [[19-IoC扩展点三部曲对照]] | 总览入口 |
| 1 | [[11-扩展点层-BeanFactoryPostProcessor详解]] | 实例化前，改定义 |
| 2 | [[12-扩展点层-BeanPostProcessor详解]] | 注册 BPP，创建过程中介入 |
| 3 | [[20-依赖注入实现原理]] | populateBean，BPP 执行注入 |
| 4 | [[13-生命周期层-Aware体系详解]] | initializeBean，基础设施回调 |
| 5 | [[22-Spring-AOP代理创建详解]] | BPP AfterInit / 三级缓存，代理替换 |
| 6 | [[23-Spring事务实现详解]] | `@Transactional` + TransactionInterceptor |

> 三部曲接口详见上篇 [[11-扩展点层-BeanFactoryPostProcessor详解]] · [[12-扩展点层-BeanPostProcessor详解]] · [[13-生命周期层-Aware体系详解]]；下篇 16–21、22–24 与 `refresh()` 时序对齐，线性通读即可。

---

## @Configuration 注册 PostProcessor：应使用 static

BFPP 和 BPP 在 `@Configuration` 中用 `@Bean` 注册时，**应使用 static 方法**（Spring 官方强烈推荐）。

| 表述 | 说明 |
|------|------|
| 不是语法硬性要求 | 非 static 也能启动 |
| 风险 | 过早实例化配置类及其依赖 |
| 谁可能受影响 | **仅在 PostProcessor 链注册完成之前被创建的 Bean** |
| 不是 | 「一定会有部分 Bean 无法完整后处理」 |

→ 详见 [[12-扩展点层-BeanPostProcessor详解#13.2 @Configuration 中注册 — 应使用 static]] · [[11-扩展点层-BeanFactoryPostProcessor详解#6.2 @Configuration 中注册 — 应使用 static]]

---

## 调试断点建议

| 扩展点 | 断点位置 |
|--------|----------|
| BFPP | `ConfigurationClassPostProcessor.postProcessBeanDefinitionRegistry()` |
| BPP 注册 | `PostProcessorRegistrationDelegate.registerBeanPostProcessors()` |
| DI | `AbstractAutowireCapableBeanFactory.populateBean()` |
| Aware | `invokeAwareMethods()` · `ApplicationContextAwareProcessor.postProcessBeforeInitialization()` |
| BPP AfterInit | `AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsAfterInitialization()` |

→ 完整清单 [[25-源码调试与断点指南]]

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[18-refresh方法详解]] | [[20-依赖注入实现原理]] |

---

## 关联

- [[00-Spring-Bean加载-学习导航]]
- [[17-Bean加载原理与源码阅读路径]]
- [[10-Context层-ApplicationContext详解]]
- [[05-接口地图-IoC与DI重要接口大全]]
- [[20-依赖注入实现原理]]
- [[12-扩展点层-BeanPostProcessor详解]]
- [[11-扩展点层-BeanFactoryPostProcessor详解]]
- [[13-生命周期层-Aware体系详解]]
- [[22-Spring-AOP代理创建详解]]
- [[25-源码调试与断点指南]]
