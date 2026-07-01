# scm-purchaseorder-next-service

这是新采购订单服务的结构样板工程，用来表达 DDD/六边形架构下的整体模块边界。

本工程当前只提供骨架，不提供详细业务实现。核心目标是让后续开发可以围绕清晰的边界逐步填充：

- `purchaseorder-next-api`：对外契约与 DTO。
- `purchaseorder-next-application`：应用用例、命令对象、端口编排。
- `purchaseorder-next-domain`：领域模型、聚合、值对象、领域事件、仓储接口。
- `purchaseorder-next-infrastructure`：数据库、外部系统、防腐层、事件发布适配器。
- `purchaseorder-next-interfaces`：REST、Feign、MQ、Excel 等入口适配器。
- `purchaseorder-next-starter`：服务启动模块。
- `purchaseorder-next-test`：架构约束与集成测试承载模块。

详细结构说明见：[docs/PROJECT_STRUCTURE.md](./docs/PROJECT_STRUCTURE.md)
