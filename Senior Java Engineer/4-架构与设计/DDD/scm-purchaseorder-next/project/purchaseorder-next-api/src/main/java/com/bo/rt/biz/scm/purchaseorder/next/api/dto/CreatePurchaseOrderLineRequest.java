package com.bo.rt.biz.scm.purchaseorder.next.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建采购订单明细的外部请求对象。
 *
 * @param requisitionLineId 来源采购申请行 ID
 * @param legacyPsoCode 迁移期旧 PSO 编码
 * @param taxedUnitPrice 含税采购单价
 * @param expectedArrivalDate 预计到货日期
 */
public record CreatePurchaseOrderLineRequest(
        String requisitionLineId,
        String legacyPsoCode,
        BigDecimal taxedUnitPrice,
        LocalDate expectedArrivalDate
) {
}
