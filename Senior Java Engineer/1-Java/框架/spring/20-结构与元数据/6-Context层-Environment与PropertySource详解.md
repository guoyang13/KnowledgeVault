---
type: canonical
status: reviewed
topic: Spring Environment / PropertySource
source_version: 6.2.x
aliases:
  - Spring Environment
  - Spring 属性源
---

# Environment 与 PropertySource 详解

## 摘要

Spring 环境抽象（Environment Abstraction）统一回答两个问题：

1. 当前激活哪些配置档案（Profiles）？
2. 某个配置属性（Property）应从哪个属性源（Property Source）读取？

`Environment` 提供查询入口，`PropertySources` 保存有顺序的来源集合，`PropertyResolver` 负责按优先级解析键值和占位符。

## 1. 核心类型

| API | 中文定位 | 职责 |
| --- | --- | --- |
| `Environment` | 环境 | Profile 状态 + 属性查询 |
| `ConfigurableEnvironment` | 可配置环境 | 修改 Profile 与属性源集合 |
| `PropertySource<T>` | 属性源 | 把某种底层来源暴露为键值查询 |
| `MutablePropertySources` | 可变属性源集合 | 维护属性源顺序 |
| `PropertyResolver` | 属性解析器 | `getProperty`、类型转换、占位符解析 |
| `EnvironmentCapable` | 环境持有者 | 暴露 `getEnvironment` |

## 2. 属性解析机制

```text
environment.getProperty("app.timeout", Duration.class)
  -> PropertySourcesPropertyResolver
  -> 按 MutablePropertySources 顺序遍历
  -> 第一个包含该 key 的 PropertySource 胜出
  -> ConversionService 转为目标类型
```

关键规则是**顺序决定优先级**，不是把所有来源合并后再随机选择。

常见属性源包括 JVM system properties、操作系统环境变量、Servlet 上下文参数以及应用主动添加的来源。Spring Framework 提供抽象和基础来源；Spring Boot 在其上建立更完整的外部化配置与优先级体系，应放到 Spring Boot 专题理解。

## 3. Profile

Profile 用于条件化注册一组 Bean：

```java
@Configuration
@Profile("prod")
class ProductionConfiguration {
}
```

`@Profile` 本质上是条件注解，配置解析时根据 `Environment` 的 active / default profiles 决定是否注册定义。

Profile 适合表达环境或部署形态差异，不适合替代业务功能开关。复杂业务开关通常需要独立配置模型、动态刷新和审计能力。

## 4. 占位符与表达式的边界

| 语法 | 机制 | 示例 |
| --- | --- | --- |
| 属性占位符 | Placeholder Resolution | `${app.timeout:5s}` |
| Spring 表达式 | Spring Expression Language, SpEL | `#{systemProperties['user.home']}` |

`${...}` 读取属性，`#{...}` 计算表达式。二者可能出现在同一配置场景，但不是同一种语言。

`@PropertySource` 把资源加入环境属性源；`PropertySourcesPlaceholderConfigurer` 参与 Bean 定义和注解中占位符的解析。不要把 `Environment#getProperty`、占位符处理器和 Spring Boot 的绑定机制视为同一个 API。

## 5. 使用与扩展

```java
@Component
class ClientSettings {
    ClientSettings(Environment environment) {
        Duration timeout = environment.getProperty(
            "client.timeout", Duration.class, Duration.ofSeconds(5));
    }
}
```

测试或框架扩展可通过 `ConfigurableEnvironment#getPropertySources` 调整来源。修改顺序时必须明确覆盖关系，并避免在容器已经大量解析配置后再改变基础属性。

## 6. 常见误区

| 误区 | 修正 |
| --- | --- |
| `Environment` 只读取系统环境变量 | 它聚合 Profiles 和多种有序 PropertySource |
| 后加入的属性一定优先 | 取决于使用 `addFirst`、`addLast`、`addBefore` 或 `addAfter` |
| `@PropertySource` 就是 Spring Boot 配置体系 | 它是 Spring Framework 的一个基础入口 |
| Profile 是通用动态开关 | Profile 更适合启动期环境分组 |

关联：[[5-Context层-ApplicationContext详解]] · [[3-refresh方法详解]] · [[1-注解入门-配置类与组件类]]
