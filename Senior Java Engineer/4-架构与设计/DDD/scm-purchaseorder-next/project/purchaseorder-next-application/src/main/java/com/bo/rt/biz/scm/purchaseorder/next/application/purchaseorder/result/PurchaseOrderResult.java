package com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.result;

/**
 * 采购订单应用服务返回结果。
 *
 * @param purchaseOrderCode 采购订单号
 * @param status 当前状态
 */
public record PurchaseOrderResult(
        String purchaseOrderCode,
        String status
) {
}
