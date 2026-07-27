package com.bo.rt.biz.scm.purchaseorder.next.api.dto;

/**
 * 采购订单对外响应摘要。
 *
 * @param purchaseOrderId 采购订单聚合标识
 * @param purchaseOrderCode 采购订单号
 * @param supplierCode 供应商编码
 * @param status 采购订单状态
 * @param approvalStatus 审批状态
 * @param writeVersion 命令写入版本
 */
public record PurchaseOrderResponse(
        String purchaseOrderId,
        String purchaseOrderCode,
        String supplierCode,
        String status,
        String approvalStatus,
        long writeVersion
) {
}
