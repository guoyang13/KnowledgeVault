---
aliases:
  - 供应链本体Mini项目
---

# 供应链本体 Mini 项目

## 目标

构建一个可以回答“缺货风险、订单延迟、供应商影响”的最小供应链本体。

## 第一步：能力问题

```text
哪些 SKU 有缺货风险？
哪些订单可能延迟？
哪些供应商影响了高优先级订单？
哪些仓库服务某个区域？
哪些冷链 SKU 没有匹配冷链承运商？
```

## 第二步：领域模型

```text
类：
Supplier, Product, SKU, Warehouse, Order, OrderLine, Shipment, Carrier

关系：
Supplier supplies SKU
Order hasOrderLine OrderLine
OrderLine orderedSKU SKU
SKU storedIn Warehouse
Shipment delivers Order
Shipment handledBy Carrier

属性：
leadTime, stockQty, safetyStock, promisedDate, deliveryDate, status, priority

规则：
stockQty < safetyStock => ReplenishmentRisk
deliveryDate > promisedDate => LateDelivery
ColdChainSKU must be handledBy ColdChainCarrier
```

## 第三步：RDF 示例

```turtle
@prefix sc: <https://example.com/supply-chain#> .

sc:SupplierA sc:supplies sc:SKU123 .
sc:SKU123 sc:storedIn sc:ShanghaiWarehouse .
sc:Order1001 sc:hasOrderLine sc:OrderLine1001A .
sc:OrderLine1001A sc:orderedSKU sc:SKU123 .
```

## 第四步：查询任务

```text
1. 找出低于安全库存的 SKU
2. 找出晚于承诺交付日期的订单
3. 找出影响高优先级订单的供应商
4. 找出没有设置安全库存的 SKU
5. 找出冷链商品但非冷链承运的运输任务
```

## 第五步：Agent Demo

让 LLM Agent 只能通过本体和工具回答问题：

```text
用户：哪些 SKU 有缺货风险？原因是什么？

Agent：
1. 查询本体定义
2. 生成 SPARQL / SQL
3. 调用查询工具
4. 按规则计算
5. 输出风险、证据和建议
```

## 完成标准

1. 有 10 个能力问题。
2. 有最小类和关系定义。
3. 有至少 30 条示例三元组。
4. 有至少 10 个 SPARQL 查询。
5. 有至少 5 个 SHACL 约束。
6. 有一个 Agent 使用本体完成查询或解释的 demo。

## 相关阶段

- [[本体模型/03-阶段1-领域建模]]
- [[本体模型/04-阶段2-RDF图模型]]
- [[本体模型/06-阶段4-SPARQL查询]]
- [[本体模型/07-阶段5-SHACL规则与数据质量]]
- [[本体模型/09-阶段7-LLM与Agent结合]]
