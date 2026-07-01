package com.bo.rt.biz.scm.purchaseorder.next.api.dto;

/**
 * 采购订单对外响应摘要。
 *
 * @param purchaseOrderCode 采购订单号
 * @param supplierCode 供应商编码
 * @param status 采购订单状态
 */
public record PurchaseOrderResponse(
        String purchaseOrderCode,
        String supplierCode,
        String status
) {
}
