---
aliases:
  - SHACL规则与数据质量
---

# 阶段 5：SHACL、规则与数据质量

## SHACL 的作用

企业落地时，SHACL 非常关键。它负责校验 RDF 数据是否符合约束。

例子：每个 SKU 必须有安全库存。

```turtle
@prefix sh: <http://www.w3.org/ns/shacl#> .
@prefix sc: <https://example.com/supply-chain#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

sc:SKUShape a sh:NodeShape ;
  sh:targetClass sc:SKU ;
  sh:property [
    sh:path sc:safetyStock ;
    sh:minCount 1 ;
    sh:datatype xsd:decimal ;
  ] .
```

## 常见校验

- 必填字段
- 数据类型
- 枚举值
- 最小/最大数量
- 引用关系是否存在
- 日期先后关系
- 业务状态是否符合流程

## OWL、SHACL、规则引擎的分工

```text
OWL：表达可推理的领域语义
SHACL：表达数据质量约束和必填规则
业务规则引擎：表达复杂计算、流程和策略
```

## 本阶段交付物

1. 5 个 SHACL shape。
2. 一份校验报告。
3. 至少一个“数据缺失”案例。
4. 至少一个“数据类型错误”案例。
5. 至少一个“业务关系不完整”案例。

## 下一步

- 学习如何让本体长期可维护：[[本体模型/08-阶段6-工程化与治理]]
