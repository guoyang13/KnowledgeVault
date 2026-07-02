---
aliases:
  - DDIA Java对照
tags:
  - DDIA
  - Java
  - 工程落地
---

# Java 后端与 DDIA 概念对照

> 把书中概念映射到常见 Java/Spring 栈，便于编码与 Code Review 时对照。

## 分层与真相源

```text
Controller / Facade     ← API 边界，DTO 演化（第 4 章）
Application Service     ← 用例编排，事务边界入口
Domain Model            ← 不变量（第 7 章），对应 DDD 聚合
Repository / Port       ← 持久化抽象
Infrastructure          ← MyBatis、Redis、MQ、Feign
```

**原则**：一个 **聚合** 一次 **本地事务**；跨聚合用 **事件**（第 11、12 章）。

## 可靠性 / 可观测

| DDIA 概念 | Java 实践 |
|---|---|
| 容错 | Resilience4j：Retry、CircuitBreaker、RateLimiter |
| p99 延迟 | Micrometer + Prometheus；避免同步日志阻塞 |
| 人为错误 | 权限注解、操作审计表、软删 |
| 快速恢复 | K8s 滚动发布、健康检查、Feature Flag |

## 数据访问

| DDIA 概念 | Java 实践 |
|---|---|
| OLTP 行存 | MySQL InnoDB + MyBatis |
| MVCC / RR | `@Transactional(isolation = REPEATABLE_READ)` |
| 乐观锁 | `UPDATE ... WHERE version = ?` |
| 悲观锁 | `SELECT ... FOR UPDATE` |
| N+1 | MyBatis 懒加载慎用；JOIN 或批量查询 |
| 深分页 | 游标 `WHERE id > ? LIMIT` |

## 复制与缓存

| DDIA 概念 | Java 实践 |
|---|---|
| 读写分离 | ShardingSphere `@DS("master")` 写后读主 |
| 读己之所写 | 同请求 ThreadLocal 标记走主库 |
| Cache-aside | `@Cacheable` + 更新删缓存 |
| 复制 lag | 不在写后立即读从；Canal 更新缓存 |

## 分布式协调

| DDIA 概念 | Java 实践 |
|---|---|
| 本地事务 | `@Transactional` 单数据源 |
| Outbox | 同库 `outbox` 表 + `@Scheduled` / MQ |
| 事务消息 | RocketMQ `TransactionListener` |
| Saga | Seata Saga / 自研状态机 + 补偿接口 |
| TCC | Seata TCC（Try-Confirm-Cancel） |
| 分布式锁 | Redisson；关键写加 **业务版本校验** |
| 幂等 | 网关 Idempotency-Key + DB 唯一索引 |

## 消息与 CDC

| DDIA 概念 | Java 实践 |
|---|---|
| 事件日志 | Kafka `ConsumerGroup` 独立消费 |
| 领域事件 | Spring `ApplicationEvent`（进程内）/ MQ（跨服务） |
| CDC | Canal Client → 更新 ES/Redis |
| Schema 演化 | Kafka + Confluent Schema Registry + Avro |

## 编码与 API 演化

| DDIA 概念 | Java 实践 |
|---|---|
| 向后兼容 API | 只加字段不删；Jackson `ignoreUnknown` |
| RPC 契约 | Protobuf + gRPC / OpenAPI 版本 |
| DB 迁移 | Flyway/Liquibase 与发版流水线绑定 |

## Code Review 对照 DDIA

| 代码味道 | 可能违反 |
|---|---|
| Service 里跨 3 个 Feign 的 `@Transactional` | 第 7、8 章假分布式事务 |
| 写 DB 后同步写 ES，无补偿 | 第 11 章双写 |
| 用 `System.currentTimeMillis()` 排序事件 | 第 8 章时钟 |
| 全局 `@Cacheable` 无 TTL/失效 | 第 5 章一致读 |
| 大事务扫全表 | 第 3、7 章 |

## 与现有 DDD 笔记联动

| DDD | DDIA |
|---|---|
| 限界上下文 | 一上下文一 schema / 一服务一库 |
| 聚合 | 本地事务边界 |
| 领域事件 | Kafka 集成（[[13-第13章-集成限界上下文]]） |
| 防腐层 | ACL 适配外部 RPC/DTO（第 4 章编码边界） |
| 采购 Next 项目 | Outbox、双轨、影子对比 → [[00-新采购服务替换方案总览]] |

## 推荐阅读顺序（Java 后端）

1. 第 1、7 章 → 事务与 `@Transactional`
2. 第 5、11 章 → 读写分离与 MQ
3. 第 9、12 章 → 微服务拆分与 Outbox
4. [[04-架构设计决策清单]] → 落地到当前项目
