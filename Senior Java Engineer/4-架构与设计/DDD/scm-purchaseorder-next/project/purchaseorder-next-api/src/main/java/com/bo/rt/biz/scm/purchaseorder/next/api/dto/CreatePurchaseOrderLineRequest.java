package com.bo.rt.biz.scm.purchaseorder.next.api.dto;

import java.math.BigDecimal;

/**
 * 创建采购订单明细的外部请求对象。
 *
 * @param skuCode SKU 编码
 * @param quantity 采购数量
 * @param purchasePrice 采购单价
 */
public record CreatePurchaseOrderLineRequest(
        String skuCode,
        BigDecimal quantity,
        BigDecimal purchasePrice
) {
}
