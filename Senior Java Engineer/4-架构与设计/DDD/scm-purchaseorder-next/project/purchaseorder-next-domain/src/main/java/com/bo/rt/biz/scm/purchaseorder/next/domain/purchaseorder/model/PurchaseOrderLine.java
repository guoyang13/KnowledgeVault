package com.bo.rt.biz.scm.purchaseorder.next.domain.purchaseorder.model;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Money;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.SkuRef;

/**
 * 采购订单明细实体。
 *
 * @param lineNo 明细行号
 * @param sku SKU 引用
 * @param quantity 采购数量
 * @param purchasePrice 采购单价
 */
public record PurchaseOrderLine(
        String lineNo,
        SkuRef sku,
        Quantity quantity,
        Money purchasePrice
) {
}
