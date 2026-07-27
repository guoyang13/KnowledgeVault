# scm-purchaseorder-next-service

这是采购订单服务第二版设计的**可编译 Java 伪代码**。它不只罗列类名，而是用聚合方法、状态机、不变量、领域事件和应用编排表达七个限界上下文。

对应设计文档：

- [七个限界上下文详细设计总览](../第二版/16-七个限界上下文详细设计总览-第二版.md)
- [采购计划上下文](../第二版/17-采购计划上下文详细设计-第二版.md)
- [采购订单上下文](../第二版/18-采购订单上下文详细设计-第二版.md)
- [供应商履约上下文](../第二版/19-供应商履约上下文详细设计-第二版.md)
- [质量检验上下文](../第二版/20-质量检验上下文详细设计-第二版.md)
- [采购结算上下文](../第二版/21-采购结算上下文详细设计-第二版.md)
- [采购履约协同上下文](../第二版/22-采购履约协同上下文详细设计-第二版.md)
- [采购查询上下文](../第二版/23-采购查询上下文详细设计-第二版.md)

项目内说明：

- [项目结构说明](docs/PROJECT_STRUCTURE.md)
- [采购项目单据流程图](docs/DOCUMENT_FLOWS.md)
- [采购路线实现](docs/PROCUREMENT_ROUTE.md)

## 代码导航

| 限界上下文 | 代码包 | 核心模型 |
|---|---|---|
| 采购计划 | `domain.planning` | `ProcurementDemand`、`PurchaseRequisition`、`TransferReservation` |
| 采购订单 | `domain.ordering` | `PurchaseOrder`、`PurchaseOrderLine`、`OrderChange` |
| 供应商履约 | `domain.supplierfulfillment` | `SupplierFulfillmentOrder`、`SupplierShipment` |
| 质量检验 | `domain.qualityinspection` | `SampleMatchingCase`、`QualityInspectionOrder` |
| 采购结算 | `domain.settlement` | `ProcurementSettlement`、`SettlementRevision` |
| 采购履约协同 | `domain.fulfillmentcollaboration` | `ExecutionRequirementPlan`、`ExecutionTask`、`ExecutionExceptionCase`、`OrderJourney` |
| 采购查询 | `application.query` | `PurchaseOrderProjector`、`PurchaseOrderExecutionView` |

Maven 模块表达六边形架构的依赖层次，包表达限界上下文。七个上下文第一阶段部署在同一个新服务中，并不要求拆成七个微服务。

## 关键设计

1. PR 转 PO 使用两阶段数量转移：`reserveForOrder` 预占，PO 保存成功后 `commitToOrder`，失败或超时则 `releaseReservation`。
2. `PurchaseOrder` 只拥有商业承诺状态，不再保存供应商发货、质检、入库或结算状态。
3. 一张 PO 对应一个 `SupplierFulfillmentOrder`，但可以有多个独立 `SupplierShipment`。
4. 对样和到货质检是两个聚合；到货质检按 `fulfillmentUnitId` 绑定实际批次。
5. 结算只消费“可结算数量”，已确认 `SettlementRevision` 不原地覆盖。
6. 约仓、样图、资质、装柜、对样、质检等先形成要求快照，再生成独立任务；任务阻塞具体里程碑。
7. 查询页消费事件生成反规范化读模型，不在请求时同步拼接多个上下文。
8. 采购路线不由调用方选择；应用层读取供应商和地点事实，由领域策略计算国内/跨境、直送/中转路线。

## 可执行场景

[EndToEndProcurementScenario.java](purchaseorder-next-test/src/test/java/com/bo/rt/biz/scm/purchaseorder/next/scenario/EndToEndProcurementScenario.java) 覆盖：

```text
PR 供应商确认 100 件
→ 预占并转 PO 80 件
→ PO 审批生效
→ 生成并完成发货前样图任务
→ 首批发运 50 件
→ 质检接收 45 件、拒收 5 件
→ 45 件进入结算，确认应付 450 CNY
→ 查询投影汇总订单、履约、质量、任务和结算状态
```

场景同时验证：PO 仍为 `EFFECTIVE`，供应商履约为 `IN_EXECUTION`，质量为 `COMPLETED`，结算为 `CONFIRMED`。这些状态属于不同上下文，不能合并为一个“大订单状态”。

## 生产替换点

`InMemoryRepositories`、`PurchaseOrderRepositoryAdapter` 的内存存储、`InMemoryPurchaseOrderViewStore` 和 `OutboxDomainEventPublisher` 是可运行说明实现。生产落地时需要替换为：

- 带版本条件更新的数据库仓储和唯一索引。
- 同事务写入的 Outbox、消费端 Inbox、失败重试和死信处理。
- 审批、商品、供应商、仓库、物流、资质及 AP 的真实防腐层。
- 查询投影检查点、延迟事件补偿、重放与监控。
- Spring 事务、鉴权、审计、协议校验和统一异常映射。

更完整的目录、事件流和单据状态见 [项目结构说明](docs/PROJECT_STRUCTURE.md) 与
[采购项目单据流程图](docs/DOCUMENT_FLOWS.md)，路线计算与建单接入见
[采购路线实现](docs/PROCUREMENT_ROUTE.md)。
