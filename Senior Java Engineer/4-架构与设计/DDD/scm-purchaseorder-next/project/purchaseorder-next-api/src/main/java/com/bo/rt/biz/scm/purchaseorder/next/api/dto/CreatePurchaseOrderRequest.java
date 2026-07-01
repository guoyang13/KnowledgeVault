package com.bo.rt.biz.scm.purchaseorder.next.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建采购订单的外部请求对象。
 *
 * @param supplierCode 供应商编码
 * @param destinationWarehouseCode 目的仓编码
 * @param expectedArrivalDate 预计到货日期
 * @param lines 采购订单明细
 */
public record CreatePurchaseOrderRequest(
        String supplierCode,
        String destinationWarehouseCode,
        LocalDate expectedArrivalDate,
        List<CreatePurchaseOrderLineRequest> lines
) {
}
