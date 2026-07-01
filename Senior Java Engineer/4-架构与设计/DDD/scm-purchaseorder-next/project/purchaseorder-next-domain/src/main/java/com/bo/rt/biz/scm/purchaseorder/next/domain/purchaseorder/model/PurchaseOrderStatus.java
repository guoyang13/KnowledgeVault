package com.bo.rt.biz.scm.purchaseorder.next.domain.purchaseorder.model;

/**
 * 采购订单状态。
 */
public enum PurchaseOrderStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    SUPPLIER_DELIVERED,
    TRANSIT_INBOUNDED,
    DESTINATION_INBOUNDED,
    COMPLETED,
    CANCELLED
}
