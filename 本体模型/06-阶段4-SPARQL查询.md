---
aliases:
  - SPARQL查询
---

# 阶段 4：SPARQL 查询

## SPARQL 是什么

SPARQL 是 RDF 图的查询语言。你要能把自然语言问题变成图模式匹配。

例子：查询上海仓里的 SKU。

```sparql
PREFIX sc: <https://example.com/supply-chain#>

SELECT ?sku
WHERE {
  ?sku sc:storedIn sc:ShanghaiWarehouse .
}
```

例子：查询缺少安全库存字段的 SKU。

```sparql
PREFIX sc: <https://example.com/supply-chain#>

SELECT ?sku
WHERE {
  ?sku a sc:SKU .
  FILTER NOT EXISTS { ?sku sc:safetyStock ?safetyStock . }
}
```

## 至少练会

- `SELECT`
- `WHERE`
- `FILTER`
- `OPTIONAL`
- `UNION`
- 聚合统计
- 属性路径
- `CONSTRUCT`

## 练习题

1. 查询所有低于安全库存的 SKU。
2. 查询某个供应商供应的所有 SKU。
3. 查询某个订单包含的 SKU 和数量。
4. 查询没有设置安全库存的 SKU。
5. 查询某个仓库服务的所有区域。

## 本阶段交付物

1. 10 个 SPARQL 查询。
2. 每个查询对应一个自然语言业务问题。
3. 至少 2 个包含 `FILTER NOT EXISTS` 或 `OPTIONAL` 的数据质量查询。

## 下一步

- 学习数据质量与约束：[[本体模型/07-阶段5-SHACL规则与数据质量]]
