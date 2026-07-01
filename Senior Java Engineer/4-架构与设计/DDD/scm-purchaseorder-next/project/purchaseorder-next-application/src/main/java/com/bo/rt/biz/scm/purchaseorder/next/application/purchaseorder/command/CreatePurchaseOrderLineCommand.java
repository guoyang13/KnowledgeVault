package com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.command;

import java.math.BigDecimal;

/**
 * 创建采购订单明细的应用层命令。
 *
 * @param skuCode SKU 编码
 * @param quantity 采购数量
 * @param purchasePrice 采购单价
 */
public record CreatePurchaseOrderLineCommand(
        String skuCode,
        BigDecimal quantity,
        BigDecimal purchasePrice
) {
}
