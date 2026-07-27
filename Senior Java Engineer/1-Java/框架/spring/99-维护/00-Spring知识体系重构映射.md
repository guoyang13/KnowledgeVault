---
type: maintenance
status: active
topic: Spring Framework
source_version: 6.2.x
created: 2026-07-26
---

# Spring 知识体系重构映射

> 本文是重构过程的权威工作台，记录知识边界、目标结构、旧文档去向和验证结果。  
> 在迁移映射确认前，不直接删除旧文档；完成一组迁移后立即更新本表。

## 1. 当前基线

| 项目 | 当前状态 |
| --- | --- |
| Markdown 文档 | 35 篇 |
| 总行数 | 13,453 行 |
| Obsidian 双链 | 923 处 |
| 真实失效链接 | 0 |
| 歧义链接 | 0 |
| 本地源码分支 | `6.2.x` |
| 本地源码版本 | `6.2.20-SNAPSHOT` |
| 源码仓库状态 | 存在用户本地修改，只读使用，不参与本次重构 |

导航中的 `[[文件名]]` 是语法示例，不属于真实失效链接。

## 2. 知识边界

### 2.1 本目录负责

本目录定位为 **Spring Framework 核心机制学习体系**，覆盖：

- Spring Core 基础抽象中与容器相关的部分。
- IoC 容器、依赖注入与 Bean 元数据。
- BeanDefinition 注册、容器启动、Bean 创建、初始化与销毁。
- BeanFactoryPostProcessor、BeanPostProcessor、Aware、FactoryBean 等扩展机制。
- Spring AOP 代理创建机制。
- Spring 声明式事务的 AOP 接入与事务执行机制。
- 源码调试、调用链案例和机制验证。

### 2.2 边界内已补

- Bean Scope：[[5-生命周期层-Bean作用域与生命周期边界]]
- ApplicationEvent：[[8-Context层-ApplicationEvent事件机制详解]]
- Environment / PropertySource：[[6-Context层-Environment与PropertySource详解]]
- Resource / ResourceLoader：[[7-Context层-Resource与ResourceLoader详解]]
- ConversionService / PropertyEditor / DataBinder：[[11-类型转换-ConversionService与DataBinder边界]]

五篇均以 Spring Framework `6.2.x` 为源码基线，并明确与 Spring Boot、Spring MVC 和外部消息系统的边界。

### 2.3 只建立边界入口

以下主题不在本目录展开，只在导航中说明关系并链接到独立专题：

- Spring Boot：自动配置、启动流程、配置绑定、Actuator。
- Spring MVC / WebFlux：请求处理、参数解析、返回值处理。
- Spring Data、Spring Security、Spring Cloud。
- Java 动态代理与字节码基础：放在 Java 基础专题，Spring AOP 只引用。
- DDD 分层与模块依赖：放在架构与设计专题，Spring 只说明装配角色。

## 3. 目标目录

```text
spring/
├── 00-Spring-Framework核心机制-学习导航.md
├── 00-Spring-Bean加载-学习导航.md
├── 10-入门与速查/
├── 20-结构与元数据/
├── 30-扩展点与生命周期/
├── 40-机制与源码/
├── 50-实践与调试/
├── 90-历史入口与边界/
└── 99-维护/
```

### 目录职责

| 目录 | 只负责什么 |
| --- | --- |
| 根目录 | 新学习导航与旧导航兼容入口 |
| `10` | 配置与组件入门、总览和短速查，不承载大段源码 |
| `20` | BeanDefinition、组件注册、容器结构及 Context 基础抽象 |
| `30` | BFPP、BPP、Aware、FactoryBean、Scope 与生命周期扩展 |
| `40` | `refresh`、Bean 创建、DI、AOP、事务等机制与源码 |
| `50` | 断点、实验、最小 Demo 和调用链验证 |
| `90` | 旧标题重定向与边界外专题入口，不进入学习主线 |
| `99` | 重构映射、变更清单与维护规则 |

## 4. 文档类型

每个核心概念只设一篇 **主文档**。其他材料必须标明类型，并链接到主文档。

| 类型 | 职责 | 是否允许重复定义 |
| --- | --- | --- |
| `navigation` | 学习路径与主题入口 | 否 |
| `canonical` | 概念的权威主文档 | 仅此一处 |
| `quick-reference` | 对照表、核心图、面试速答 | 否，只摘要并链接 |
| `source-analysis` | 类、方法和调用链证据 | 不重复基础定义 |
| `case-study` | Demo、调试过程和问题推导 | 不重复完整原理 |
| `boundary` | 说明与相邻专题的关系 | 否 |
| `redirect` | 兼容旧标题并指向权威文档 | 否，不保留重复正文 |
| `maintenance` | 映射、进度和验证 | 不进入学习主线 |

## 5. 迁移映射

状态说明：

- `保留重构`：继续作为该主题主文档，但需要统一模板和去重。
- `合并`：有效内容迁入指定主文档，原文件暂保留重定向说明。
- `压缩引用`：保留速查入口，只保存摘要、对照和主文档链接。
- `案例化`：删除重复原理，保留 Demo、调试证据或问题推导。
- `移出边界`：迁入其他知识专题，本目录保留边界入口。
- `待判断`：当前证据不足，不在本轮直接归并。

### 5.1 根导航

| 当前文档 | 处理 | 目标 |
| --- | --- | --- |
| `00-Spring-Bean加载-学习导航` | 已完成 | 新建完整导航；旧标题保留为重定向以兼容外部双链 |

### 5.2 入门与速查

| 当前文档 | 处理 | 目标主文档或角色 |
| --- | --- | --- |
| `1-注解入门-配置类与组件类` | 已完成 | 配置类与组件类权威入门，包含 Full / Lite 边界 |
| `2-注解入门-Configuration与Service等注解区别` | 已删除 | 内容已完整归并到“配置类与组件类”；Vault 内引用已全部迁移，旧标题由主文档 alias 保留 |
| `2-速查-IoC与DI核心整合速查` | 已完成 | 134 行核心心智模型；原编号 3 |
| `3-速查-Spring厨房比喻大全` | 已完成 | 100 行辅助类比表；原编号 4 |
| `4-接口地图-IoC与DI重要接口大全` | 已完成 | 123 行接口导航图；原编号 5 |
| `5-工厂Bean-BeanFactory与FactoryBean的区别` | 已完成 | 85 行 quick-reference；原编号 6 |
| `6-术语表-Spring核心概念中英文对照` | 已完成 | 中英文标准术语与推荐简称；原编号 7 |

### 5.3 结构与元数据

| 当前文档 | 处理 | 目标主文档或角色 |
| --- | --- | --- |
| `1-元数据层-BeanDefinition三兄弟详解` | 已完成 | BeanDefinition 权威主文档；已统一元数据与方法级源码定位 |
| `2-元数据层-AnnotatedBeanDefinitionReader与组件注册详解` | 已完成 | 组件注册权威主文档；已合并“未使用类”FAQ |
| `3-注册层-BeanDefinitionRegistry与DefaultListableBeanFactory详解` | 已完成 | 注册表与默认容器实现权威主文档 |
| `4-容器层-BeanFactory接口体系详解` | 已完成 | BeanFactory 接口体系权威主文档 |
| `5-Context层-ApplicationContext详解` | 已完成 | ApplicationContext 权威主文档；子能力拆分链接 |

### 5.4 扩展点与生命周期

| 当前文档 | 处理 | 目标主文档或角色 |
| --- | --- | --- |
| `1-扩展点层-BeanFactoryPostProcessor详解` | 已完成 | BFPP 权威主文档；源码引用改为方法级定位 |
| `2-扩展点层-BeanPostProcessor详解` | 已完成 | BPP 权威主文档；速查链接与 AOP 锚点已修复 |
| `3-生命周期层-Aware体系详解` | 已完成 | Aware 权威主文档；已标记 `canonical` / Spring 6.2.x |
| `4-工厂Bean-FactoryBean接口体系详解` | 已完成 | FactoryBean 权威主文档；已标记 `canonical` / Spring 6.2.x |

### 5.5 机制与源码

| 当前文档 | 处理 | 目标主文档或角色 |
| --- | --- | --- |
| `1-IoC与DI核心概念` | 已完成 | IoC / DI 概念权威主文档 |
| `2-Bean加载原理与源码阅读路径` | 已完成 | Bean 获取与创建总路线权威主文档 |
| `3-refresh方法详解` | 已完成 | 容器启动权威主文档 |
| `4-doCreateBean深度解析` | 已完成 | `doCreateBean` 权威主文档；整体骨架和子方法细节已统一 |
| `4.1-doCreateBean核心子方法深度解析` | 已完成 | 已归并为 26 行重定向入口 |
| `5-依赖注入实现原理` | 已完成 | DI 权威主文档，负责正式定义、生命周期位置和总体源码链 |
| `6-循环依赖与三级缓存详解` | 已完成 | 循环依赖权威主文档；行号地图已改为方法地图 |
| `7-IoC扩展点三部曲对照` | 已完成 | 141 行 quick-reference，只承担 BFPP / BPP / Aware 对照 |
| `8-Spring-AOP代理创建详解` | 已完成 | Spring AOP 代理创建权威主文档；Java 动态代理基础已拆出 |
| `9-Bean 销毁机制详解` | 已完成 | 销毁阶段独立权威子主题，与 Scope 和创建主线互链 |
| `10-Spring事务实现详解` | 已完成 | 声明式事务权威主文档 |

### 5.6 实践与调试

| 当前文档 | 处理 | 目标主文档或角色 |
| --- | --- | --- |
| `1-源码调试与断点指南` | 已完成 | source-analysis / 调试索引；断点改为方法级定位 |
| `2-测试驱动的refresh调用链-Aware与Processor` | 已完成 | 已标记 `case-study`，只负责测试与调用链证据 |

### 5.7 Q&A

| 当前文档 | 处理 | 目标主文档或角色 |
| --- | --- | --- |
| `未被使用的类Spring如何处理` | 已完成 | 独有结论已并入组件注册主文档 FAQ；原文件保留重定向 |
| `Spring依赖注入形式分类与Demo` | 已完成 | 从 447 行收敛为 224 行 case-study，只负责使用选择和示例 |
| `Spring注入注解与byType-byName解析逻辑` | 已完成 | 标记为 source-analysis，只负责候选解析规则 |
| `动态代理是什么` | 已完成 | 已迁入 `Java基础/1010-Java动态代理与运行时代理机制`；原文件保留重定向 |
| `DDD分层-编译时运行时与Spring装配` | 已完成 | 已迁入 DDD 工程落地；Spring 原文件保留机制入口 |
| `Aware体系总结与常见问题` | 已完成 | 已归并为 25 行重定向入口，旧双链继续有效 |
| `getBeanProvider与ObjectProvider有什么用` | 已完成 | ObjectProvider 独立子概念主文档；链接回 DI 主文档 |

## 6. 首批归并顺序

1. Aware：主文档 + Q&A + refresh 调用链案例中的重复原理。
2. FactoryBean：主文档 + BeanFactory/FactoryBean 辨析。
3. `doCreateBean`：整体骨架 + 核心子方法两篇。
4. 依赖注入：主文档 + 注入形式 + 注解解析 + ObjectProvider。
5. 动态代理与 AOP：将 Java 基础和 Spring 机制分界。

每组完成后验证：

- 主文档是否覆盖原有独有知识点。
- 原文件是否保留可追溯入口。
- 新旧双链是否有效。
- 是否仍存在两处完整定义。
- 是否标明 Spring Framework 6.2.x。

## 7. 当前验证记录

### 2026-07-26：初始审计

- 已统计全部 35 篇文档与规模。
- 已确认本地源码基线为 Spring Framework `6.2.x` / `6.2.20-SNAPSHOT`。
- 已确认源码仓库有用户本地修改，本次只读使用。
- 已解析 923 处 Obsidian 双链。
- 未发现真实失效链接或歧义链接。
- 已建立目录边界、文档类型和初始迁移映射。

### 2026-07-26：Aware 主题归并

- `Aware 体系详解` 已设为该主题唯一权威主文档。
- 原 `Aware 体系总结与常见问题` 的问题均已确认在主文档或调用链案例中有对应内容。
- Q&A 文件已改为重定向映射，从 151 行收敛为 25 行，没有删除历史入口。
- `测试驱动的 refresh 调用链` 已标记为案例文档，并声明不承担正式概念定义。
- 三篇文档均标明 Spring Framework `6.2.x`。

### 2026-07-26：FactoryBean 主题归并

- `FactoryBean 接口体系详解` 已设为该主题唯一权威主文档。
- `BeanFactory 与 FactoryBean 的区别` 从 230 行压缩为 85 行，只保留角色对照、获取规则、生命周期边界和深入入口。
- 速查中原有的 Demo、源码细节和实现类清单均已确认由主文档覆盖。
- 两篇文档均标明 Spring Framework `6.2.x`，原文件名和双链入口保持不变。

### 2026-07-26：doCreateBean 主题归并

- `doCreateBean 深度解析` 已设为 Bean 创建主流水线的权威主文档。
- 原子方法篇中的 BeanWrapper、构造器缓存、属性填充阶段、初始化去重和 Scope 销毁分流已并入主文档对应章节。
- `doCreateBean 核心子方法深度解析` 从 257 行收敛为 26 行章节映射，原文件名继续兼容历史链接。
- 主文档从 261 行调整为 328 行，消除了两篇文档分别维护同一组核心阶段的问题。

### 2026-07-26：依赖注入主题分层

- `依赖注入实现原理` 已设为 DI 唯一权威主文档，标记为 `canonical`。
- `Spring 依赖注入形式分类与 Demo` 从 447 行收敛为 224 行，改为 `case-study`，只保留选择入口与可运行示例。
- `Spring 注入注解与 byType / byName 解析逻辑` 标记为 `source-analysis`，只负责候选解析规则和源码分支。
- `ObjectProvider` 被确认是独立 API 子概念，保留为自己的 `canonical`，不再作为泛化 DI 问答。
- 四篇文档均显式指向各自职责和 DI 主文档，并标明 Spring Framework `6.2.x`。

### 2026-07-26：动态代理与 Spring AOP 边界拆分

- 已在 `1-Java/Java基础` 建立 `Java动态代理与运行时代理机制` 权威主文档，集中解释 JDK 动态代理、CGLIB 子类代理及语言层约束。
- 原 `动态代理是什么` 改为重定向入口，历史双链继续有效。
- `Spring AOP 代理创建详解` 已标记为 `canonical`，只负责 Spring 中代理创建时机、代理策略和拦截器链。
- FactoryBean、DDD 装配和 Spring AOP 文档中的基础概念链接已改指 Java 主文档。
- Java 基础与 Spring 框架不再重复维护“动态代理是什么”的完整定义。

### 2026-07-26：入门速查精炼

- 三篇速查从合计 1481 行收敛为 357 行，均标记为 `quick-reference` / Spring Framework `6.2.x`。
- `IoC 与 DI 核心整合速查` 只保留概念、五个结构角色、一条主链、扩展机制和掌握检查。
- `Spring 厨房比喻速查` 只作为辅助记忆，并显式区分比喻、正式中文名与 English Term / API。
- `IoC 与 DI 重要接口地图` 只负责按职责定位接口，不再复制接口源码和主题正文。
- 全库重新解析 785 处 Spring 目录内双链，真实失效链接为 0，歧义链接为 0。

### 2026-07-26：成熟 Q&A 归档

- “未被使用的类，Spring 如何处理”已并入组件注册权威文档，统一解释未注册、非懒加载单例、`@Lazy`、prototype Scope、条件注册与抽象定义。
- 原问答已收敛为重定向入口，不再维护第二套组件注册与预实例化规则。
- “DDD 分层：编译时 / 运行时与 Spring 装配”已迁入 `4-架构与设计/DDD/实现领域驱动设计/05-工程落地`。
- DDD 新主文档区分编译期依赖、运行期装配和候选解析；Spring 目录只保留 DI、FactoryBean 与 AOP 的机制入口。

### 2026-07-26：入门注解归并与核心主题补齐

- “配置类与组件类”已成为 `@Configuration`、stereotype 注解和 `@Bean` 方法语义的权威入门文档。
- 原“`@Configuration` 与 `@Service` 等注解区别”的 Full / Lite 配置模式已并入主文档；确认无独立内容后删除重定向文件。
- 修正“`@Import` / `@ComponentScan` 只能用于配置类”和“配置类增强等于 Spring AOP”两类过度简化。
- 新增 Bean Scope、ApplicationEvent、Environment / PropertySource、Resource / ResourceLoader、ConversionService / DataBinder 五篇核心主题。
- 新主题均提供中英文术语、机制主线、边界与常见误区，源码基线为 Spring Framework `6.2.x`。
- 全库重新解析 773 处 Spring 目录内双链，真实失效链接为 0，歧义链接为 0。

### 2026-07-26：目录、术语与可维护性收口

- 新建 `Spring Framework 核心机制学习导航`，按“30 分钟模型、2 小时主线、问题式深入”组织阅读路径；旧导航保留重定向。
- 拆除 `100-Q&A`：DI 案例进入实践目录，候选解析与 ObjectProvider 进入机制目录，四个旧标题进入 `90-历史入口与边界`。
- 新建统一中英文术语表，明确 IoC / DI、BeanFactory / FactoryBean、BFPP / BPP、Aware、Scope、AOP 与 Context 基础抽象的推荐写法。
- Spring 目录 45 篇文档均具备 `type`、`status`、`topic`、`source_version` 元数据。
- 全部硬编码源码行号已改为稳定的 `Class#method` 定位；断点顺序和观察目标保留。
- 已删除迁移后为空的 `100-Q&A` 与 `附件` 目录。

### 2026-07-26：入门与速查编号收口

- 删除已完全归并的“`@Configuration` 与 `@Service` 等注解区别”重定向文件。
- 后续文件依次由 `3–7` 调整为 `2–6`，目录编号恢复连续。
- Vault 内所有 Wiki Link 均已迁移到新文件名。
- 五篇重命名文档保留旧文件名 alias，兼容搜索和目录外的历史记忆。

## 8. 最终决策

- 不另建重复的“Bean 生命周期大全”。生命周期总线由 [[2-速查-IoC与DI核心整合速查]] 和 [[4-doCreateBean深度解析]] 承担，Aware、Scope、销毁保留为独立子主题。
- ApplicationContext 保留容器能力总览；Environment、Resource、ApplicationEvent 已拆为独立权威主文档，避免 Context 主文档继续膨胀。
- Spring Boot 使用已有 `1-Java/框架/springboot` 专题，Spring Framework 导航只提供边界链接。
- Java 动态代理归 Java 基础，DDD 组合根归架构与设计；Spring 目录只解释框架如何使用这些机制。
- 保留当前 `10/20/30/40/50/90/99` 分层，不为追求目录形式大规模改名；文档职责由类型元数据和新导航共同约束。

## 9. 最终验证

| 检查项 | 结果 |
| --- | --- |
| Spring 目录 Markdown 文档 | 44 篇 |
| Obsidian 双链 | 827 处 |
| 带章节锚点的双链 | 65 处 |
| 真实失效文件链接 | 0 |
| 歧义文件链接 | 0 |
| 失效章节锚点 | 0 |
| 缺失标准元数据 | 0 |
| 单文档重复标题 | 0 |
| 未闭合代码围栏 | 0 |
| 硬编码源码行号 | 0 |

验证范围包含 Spring 目录内的所有 Markdown，并允许链接到同一 Obsidian Vault 中的 Java 基础、DDD 与 Spring Boot 专题。
