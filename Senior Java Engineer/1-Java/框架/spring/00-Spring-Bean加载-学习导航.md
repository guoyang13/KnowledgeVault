# Spring IoC 容器专题 — 学习导航

> 基于 Spring Framework 源码（`spring-beans` + `spring-context`）整理。  
> 与 [[手写源码专题/Spring/01-Spring源码阅读前言/01-Spring源码阅读的目的和方法|Spring 源码阅读方法]] 互补。
>
> 本地源码：`/Users/guoyang/IdeaProjects/spring/spring-framework`

---

## 文档结构（上篇 / 下篇）

```text
┌─────────────────────────────────────────────────────────────────┐
│  上篇 · 01–15  接口、类与 Bean 元数据（文件名含分层标签）          │
│    注解入门 → 速查（03–04）→ 接口地图 → 元数据 → 注册 → 容器     │
│    → Context → 扩展点 → 生命周期 → 工厂Bean              │
├─────────────────────────────────────────────────────────────────┤
│  下篇 · 16–25  IoC / DI / AOP / 事务 机制与源码（再跟流程）     │
│  100-Q&A  补充专题（不进主序列编号）                              │
└─────────────────────────────────────────────────────────────────┘
```

**推荐通读顺序 = 编号 01 → 25**

---

## 编号索引

### 上篇（01–15）

| 编号 | 分层 | 笔记 | 内容 |
|:----:|:----:|------|------|
| 01 | 注解入门 | [[01-注解入门-配置类与组件类]] | 配置类 vs 组件类 |
| 02 | 注解入门 | [[02-注解入门-Configuration与Service等注解区别]] | 注解处理差异 |
| 03 | 速查 | [[03-速查-IoC与DI核心整合速查]] | **结构 + 机制整合速查** |
| 04 | 速查 | [[04-速查-Spring厨房比喻大全]] | 厨房比喻速查 |
| 05 | 接口地图 | [[05-接口地图-IoC与DI重要接口大全]] | 接口分层总览 |
| 06 | 元数据层 | [[06-元数据层-BeanDefinition三兄弟详解]] | BeanDefinition |
| 07 | 元数据层 | [[07-元数据层-AnnotatedBeanDefinitionReader与组件注册详解]] | 组件注册 |
| 08 | 注册层 | [[08-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解]] | Registry + DLBF |
| 09 | 容器层 | [[09-容器层-BeanFactory与Registry详解]] | BeanFactory |
| 10 | Context层 | [[10-Context层-ApplicationContext详解]] | ApplicationContext |
| 11 | 扩展点层 | [[11-扩展点层-BeanFactoryPostProcessor详解]] | BFPP |
| 12 | 扩展点层 | [[12-扩展点层-BeanPostProcessor详解]] | BPP |
| 13 | 生命周期层 | [[13-生命周期层-Aware体系详解]] | Aware |
| 14 | 工厂Bean | [[14-工厂Bean-BeanFactory与FactoryBean的区别]] | 区别 |
| 15 | 工厂Bean | [[15-工厂Bean-FactoryBean接口体系详解]] | FactoryBean |

### 下篇（16–25）

| 编号 | 笔记 | 内容 |
|:----:|------|------|
| 16 | [[16-IoC与DI核心概念]] | IoC / DI 机制 |
| 17 | [[17-Bean加载原理与源码阅读路径]] | 加载路径 |
| 18 | [[18-refresh方法详解]] | refresh() |
| 19 | [[19-IoC扩展点三部曲对照]] | 扩展点对照（上篇 11–13） |
| 20 | [[20-依赖注入实现原理]] | @Autowired 源码 |
| 21 | [[21-循环依赖与三级缓存详解]] | 三级缓存 |
| 22 | [[22-Spring-AOP代理创建详解]] | AOP 代理 |
| 23 | [[23-Spring事务实现详解]] | 事务 |
| 24 | [[24-Bean销毁机制详解]] | 销毁 |
| 25 | [[25-源码调试与断点指南]] | 断点 |

### 100-Q&A（补充）

| 编号 | 笔记 | 内容 |
|:----:|------|------|
| — | [[100-Q&A/未被使用的类Spring如何处理]] | 未注册 / 孤儿 Bean 行为 |
| — | [[100-Q&A/Spring依赖注入形式分类与Demo]] | DI 形式分类 + Demo |
| — | [[100-Q&A/Spring注入注解与byType-byName解析逻辑]] | 注解解析 + byName/byType |
| — | [[100-Q&A/动态代理是什么]] | JDK / CGLIB 与 Spring 对应 |
| — | [[100-Q&A/DDD分层-编译时运行时与Spring装配]] | 编译时解耦 vs 运行时装配 + FactoryBean 边界 |
| — | [[100-Q&A/Aware体系总结与常见问题]] | Aware 总结、BeanNameAware、AAP、resolveDependency |

---

## 推荐路径

### 路径 A：完整通读

```text
01 → 02 → 03 → 04 → 05 → … → 15 → 16 → … → 25
```

### 路径 B：时间紧

```text
03 → 04 → 05 → 11 → 12 → 13 → 16 → 19 → 22 → 23
```

### 路径 C：下篇机制

```text
16 → 17 → 18 → 19 → 20 → 21 → 22 → 23 → 24 → 25
```

---

## 核心心智模型（下篇）

```text
阶段一：注册 BeanDefinition          ← 上篇 06、07
阶段二：BFPP                         ← 上篇 11
阶段三：BPP                          ← 上篇 12
阶段四：getBean()
  → populateBean()                   ← 20
  → 三级缓存                          ← 21
  → Aware                            ← 上篇 13
  → AOP                              ← 22
  → 事务                              ← 23
  → 销毁                              ← 24
```

---

## 概念速查

| 概念 | 详见 |
|------|------|
| 整合速查 | [[03-速查-IoC与DI核心整合速查]] · [[04-速查-Spring厨房比喻大全]] |
| 接口地图 | [[05-接口地图-IoC与DI重要接口大全]] |
| BeanDefinition | [[06-元数据层-BeanDefinition三兄弟详解]] |
| BFPP / BPP / Aware | [[11-扩展点层-BeanFactoryPostProcessor详解]] · [[12-扩展点层-BeanPostProcessor详解]] · [[13-生命周期层-Aware体系详解]] |
| 孤儿 Bean / 未扫描类 | [[100-Q&A/未被使用的类Spring如何处理]] |
| DI 形式 / 注解解析 | [[100-Q&A/Spring依赖注入形式分类与Demo]] · [[100-Q&A/Spring注入注解与byType-byName解析逻辑]] |

---

## 源码仓库

| 模块 | 核心类 |
|------|--------|
| `spring-beans` | `BeanFactory`、`DefaultListableBeanFactory`、`BeanDefinition` |
| `spring-context` | `ApplicationContext`、`refresh()`、`ConfigurationClassPostProcessor` |
| `spring-aop` | `AbstractAutoProxyCreator`、`DefaultAopProxyFactory` |
| `spring-tx` | `TransactionInterceptor`、`PlatformTransactionManager` |

---

## 关联笔记（专题外）

- [[手写源码专题/Spring/02-手写实现IoC模块功能/01-创建对象代码演变及IoC思路分析]]
- [[1001-ClassLoader类加载机制]]
