# @Configuration 与 @Service 等注解区别

> 导航：[[00-Spring-Bean加载-学习导航]] · **上篇 01–15** · 注解入门 · 前置：[[01-注解入门-配置类与组件类]]

## 一句话区别

| 注解 | 角色 | 核心作用 |
|------|------|----------|
| `@Configuration` | **配置类** | 定义容器：注册 `@Bean`、开启扫描、导入配置 |
| `@Service` / `@Repository` / `@Controller` / `@Component` | **组件类（Stereotype）** | 标记「这个类本身就是要被 Spring 管理的 Bean」 |

---

## 继承关系

两者最终都「属于」`@Component` 体系：

```text
@Component          ← 根 stereotype
  ├── @Service
  ├── @Repository
  ├── @Controller
  └── @Configuration  ← meta-annotated @Component，但 Spring 特殊处理
```

- `@Configuration` 也能被 `@ComponentScan` 扫到
- `@Service` 等是 `@Component` 的 **特化（stereotype）**
- Spring 对 `@Configuration` 的 **处理方式不同**

---

## 语义区别（给人看的）

| 注解 | 语义 | Spring 运行时 |
|------|------|---------------|
| `@Component` | 通用组件 | 与 stereotype 等价 |
| `@Service` | 业务服务层 | 与 `@Component` 几乎相同 |
| `@Repository` | 持久层 | 额外异常翻译（`PersistenceExceptionTranslationPostProcessor`） |
| `@Controller` | Web 控制层 | 与 `@Component` 几乎相同 |
| `@Configuration` | 配置类，组装容器 | **特殊处理** |

---

## Spring 处理机制的核心区别

### @Service 等：扫描 → 注册 → 实例化

```text
@ComponentScan
  → ClassPathBeanDefinitionScanner 发现 @Service
  → registerBeanDefinition()
  → refresh 时 getBean() 创建这个类的实例
```

**结果：类本身 = 一个 Bean。**

默认扫描 stereotype：`@Component` · `@Repository` · `@Service` · `@Controller`

### @Configuration：额外走 ConfigurationClassPostProcessor

```text
@ComponentScan 发现 @Configuration
  → registerBeanDefinition（配置类本身）
  → refresh() 时 ConfigurationClassPostProcessor
      → 解析 @Bean 方法 → 注册更多 BeanDefinition
      → 解析 @Import、@ComponentScan、@PropertySource
      → 默认 proxyBeanMethods=true → CGLIB 代理
```

**结果：配置类本身是一个 Bean，同时它还能「生产」其他 Bean。**

---

## Full 模式 vs Lite 模式

**文件**：`ConfigurationClassUtils.java`

```text
@Configuration + proxyBeanMethods=true（默认）
  → CONFIGURATION_CLASS_FULL → CGLIB 增强，@Bean 互调返回同一单例

@Configuration + proxyBeanMethods=false
  → CONFIGURATION_CLASS_LITE → 不代理，@Bean 方法像普通工厂方法
```

```java
if (config != null && !Boolean.FALSE.equals(config.get("proxyBeanMethods"))) {
    beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_FULL);
}
```

---

## 用法对比

```java
// @Service：这个类本身就是 Bean
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
}

// @Configuration：这是「工厂/装配中心」
@Configuration
@ComponentScan("com.example")
public class AppConfig {
    @Bean
    public DataSource dataSource() { return new HikariDataSource(); }
}
```

---

## 对照表

| 维度 | `@Configuration` | `@Service` 等 |
|------|-------------------|---------------|
| 类本身是否成为 Bean | 是 | 是 |
| 能否定义 `@Bean` | 能（主要用途） | 技术上可以，不推荐 |
| 能否 `@ComponentScan` | 能 | 不能 |
| 是否 CGLIB 增强 | 默认会（Full 模式） | 不会 |
| 典型位置 | `config` 包 | `service` / `dao` / `web` 包 |
| 特殊处理器 | `ConfigurationClassPostProcessor` | 无 |

---

## 结合 Bean 加载源码的两条线

| 注解类型 | 源码阅读路径 |
|----------|-------------|
| `@Service` 等 | `ClassPathBeanDefinitionScanner` → `registerBeanDefinition` → `getBean` → `doCreateBean` |
| `@Configuration` | 同上先注册配置类 → `ConfigurationClassPostProcessor` → 再注册 @Bean 产生的定义 |

详见 [[17-Bean加载原理与源码阅读路径]]。

---

## 常见误区

| 误区 | 正解 |
|------|------|
| `@Service` 和 `@Component` 功能不同 | 对 Spring 几乎一样，只是语义分层 |
| 有 `@Service` 就不需要 `@Configuration` | 不对，职责不同 |
| 在 `@Service` 里写 `@Bean` | 可以但不推荐，应放在 `@Configuration` |

---

## 下一步可深入

- [[22-Spring-AOP代理创建详解]]（`ConfigurationClassEnhancer` CGLIB 增强 vs AOP 代理）
- `@Import` / `@ImportResource` — 配置类组合方式
- `@Enable*` 注解 — 如何通过 `@Import` 导入自动配置

---

## 篇章导航

| 上一篇 | 下一篇 |
|--------|--------|
| [[01-注解入门-配置类与组件类]] | [[03-速查-IoC与DI核心整合速查]] |

---

## 关联

- [[00-Spring-Bean加载-学习导航]]
- [[16-IoC与DI核心概念]]
- [[01-注解入门-配置类与组件类]]
- [[17-Bean加载原理与源码阅读路径]]
- [[06-元数据层-BeanDefinition三兄弟详解]]
- [[100-Q&A/未被使用的类Spring如何处理]]
