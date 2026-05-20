---
aliases:
  - RDFS与OWL建模
---

# 阶段 3：RDFS 与 OWL 建模

## RDFS / OWL 的作用

RDFS / OWL 用来定义“这些三元组背后的语义”。

```turtle
@prefix sc: <https://example.com/supply-chain#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .

sc:Supplier a owl:Class .
sc:SKU a owl:Class .
sc:Warehouse a owl:Class .

sc:ColdChainSKU rdfs:subClassOf sc:SKU .

sc:supplies a owl:ObjectProperty ;
  rdfs:domain sc:Supplier ;
  rdfs:range sc:SKU .
```

## 重点概念

| 概念 | 含义 |
|---|---|
| Class | 类，如 Supplier、Order |
| Individual | 实例，如 SupplierA、Order1001 |
| ObjectProperty | 对象关系，如 supplies |
| DataProperty | 数据属性，如 stockQty |
| subclassOf | 类层级 |
| domain / range | 属性适用的主语和宾语范围 |
| restriction | 对类的限制条件 |
| reasoning | 根据显式事实推出隐含事实 |

## 专家提醒：不要把 OWL 当数据库约束

OWL 采用开放世界假设（Open World Assumption）：不知道不等于不存在。

例如你没有写“订单1001有承运商”，OWL 不会默认认为它违规；它只会认为“目前不知道”。如果你要做企业数据质量校验，通常需要 SHACL。

经验法则：

```text
OWL：表达可推理的领域语义
SHACL：表达数据质量约束和必填规则
业务规则引擎：表达复杂计算、流程和策略
```

## 本阶段交付物

1. 最小类层级。
2. ObjectProperty 和 DataProperty 清单。
3. domain / range 定义。
4. 至少 3 条可被推理机推出的隐含事实。

## 下一步

- 学习 RDF 图查询：[[本体模型/06-阶段4-SPARQL查询]]
