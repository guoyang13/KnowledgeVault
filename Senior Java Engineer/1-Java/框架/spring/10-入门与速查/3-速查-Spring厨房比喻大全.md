---
type: quick-reference
status: reviewed
topic: Spring IoC analogy map
source_version: 6.2.x
aliases:
  - Spring 厨房比喻大全
  - 4-速查-Spring厨房比喻大全
---

# Spring 厨房比喻速查

> [!warning] 使用边界
> 比喻用于第一次记忆，不用于推导源码结论。判断机制时请回到正式术语、处理对象和生命周期时机。

## 1. 总体映射

| 厨房比喻        | 正式中文名       | English Term / API         | 真正职责                   |
| ----------- | ----------- | -------------------------- | ---------------------- |
| 菜谱          | Bean 定义     | `BeanDefinition`           | 保存类型、作用域、依赖、初始化方式等元数据  |
| 菜谱登记册       | Bean 定义注册表  | `BeanDefinitionRegistry`   | 按 Bean 名称注册和读取定义       |
| 后厨管理系统      | Bean 工厂     | `BeanFactory`              | 创建、组装、缓存和取得 Bean       |
| 完整餐厅系统      | 应用上下文       | `ApplicationContext`       | 扩展事件、资源、环境、国际化等能力      |
| 专门制作某类成品的师傅 | 工厂型 Bean    | `FactoryBean<T>`           | 生产并暴露 `T` 类型对象         |
| 开业前改菜谱      | Bean 工厂后处理器 | `BeanFactoryPostProcessor` | 在实例化前修改定义或容器元数据        |
| 上桌前检查/包装    | Bean 后处理器   | `BeanPostProcessor`        | 在初始化前后处理 Bean 实例       |
| 告知内部信息      | 容器感知回调      | `Aware`                    | 把名称、工厂、上下文等基础设施传给 Bean |
| 按需取餐窗口      | 对象提供器       | `ObjectProvider<T>`        | 延迟、可选或多实例地取得依赖         |

## 2. 一道菜的完整流程

```text
读取菜谱
  -> 登记菜谱
  -> 开业前允许修改菜谱
  -> 接单并创建对象
  -> 注入所需协作者
  -> 告知容器基础设施
  -> 初始化前检查
  -> 执行初始化
  -> 初始化后检查或包装
  -> 对外提供最终对象
  -> 关店时执行销毁
```

对应正式链路：

```text
BeanDefinition
  -> BeanDefinitionRegistry
  -> BeanFactoryPostProcessor
  -> instantiateBean
  -> populateBean
  -> invokeAwareMethods
  -> BeanPostProcessor before initialization
  -> initialization callbacks
  -> BeanPostProcessor after initialization
  -> exposed object
  -> destruction callbacks
```

## 3. 三组必须纠正的误解

### `BeanFactory` 不是 `FactoryBean`

- `BeanFactory`：管理整个后厨的容器契约。
- `FactoryBean<T>`：后厨中一个会生产 `T` 的特殊 Bean。
- `getBean("name")` 通常取得产品；`getBean("&name")` 取得 `FactoryBean` 本身。

### `Aware` 不是普通依赖注入

- 依赖注入（Dependency Injection）：提供业务对象需要的协作者。
- 容器感知（Aware Callback）：执行框架协议，传递容器基础设施。
- 业务代码应优先保持对 Spring 的低耦合，确实需要基础设施时再使用 `Aware`。

### `BeanPostProcessor` 不只做“检查”

它可以返回原对象，也可以返回包装对象或代理。Spring AOP 正是其重要应用之一，因此“上桌前包装”比“质检”更接近真实能力。

## 4. 何时离开比喻

遇到以下问题时，直接切换到正式文档：

| 问题 | 深入入口 |
| --- | --- |
| 定义如何读取与合并 | [[1-元数据层-BeanDefinition三兄弟详解]] |
| 容器如何启动 | [[3-refresh方法详解]] |
| Bean 如何创建 | [[4-doCreateBean深度解析]] |
| 依赖如何选择候选 | [[5-依赖注入实现原理]] |
| 三类扩展点的精确时序 | [[7-IoC扩展点三部曲对照]] |
| `FactoryBean` 的产品缓存和生命周期 | [[4-工厂Bean-FactoryBean接口体系详解]] |
| 代理如何生成 | [[8-Spring-AOP代理创建详解]] |

## 5. 记忆检查

只记住四句：

1. `BeanDefinition` 描述对象，`BeanFactory` 管理对象。
2. `BeanFactoryPostProcessor` 改定义，`BeanPostProcessor` 处理实例。
3. DI 提供业务依赖，`Aware` 提供容器基础设施。
4. 容器最终暴露的 Bean 可能是原对象，也可能是代理。
