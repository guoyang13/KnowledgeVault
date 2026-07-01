package com.bo.rt.biz.scm.purchaseorder.next.infrastructure.persistence.purchaseorder;

/**
 * 采购订单数据库对象。
 *
 * <p>该对象只属于基础设施层，不允许进入领域模型。</p>
 *
 * @param purchaseOrderCode 采购订单号
 * @param supplierCode 供应商编码
 * @param status 订单状态
 */
public record PurchaseOrderDO(
        String purchaseOrderCode,
        String supplierCode,
        String status
) {
}
