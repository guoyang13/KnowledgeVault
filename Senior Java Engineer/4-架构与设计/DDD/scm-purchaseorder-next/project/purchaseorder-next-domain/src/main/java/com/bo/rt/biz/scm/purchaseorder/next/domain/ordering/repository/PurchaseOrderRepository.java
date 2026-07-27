package com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.repository;

import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.PurchaseOrder;
import java.util.Optional;

/** 采购订单聚合仓储端口。 */
public interface PurchaseOrderRepository {

    /** 保存采购订单、订单行及乐观锁版本。 */
    void save(PurchaseOrder purchaseOrder);

    /** 按聚合标识加载采购订单。 */
    Optional<PurchaseOrder> findById(String purchaseOrderId);

    /** 按业务订单号加载采购订单。 */
    Optional<PurchaseOrder> findByOrderNo(String orderNo);
}
