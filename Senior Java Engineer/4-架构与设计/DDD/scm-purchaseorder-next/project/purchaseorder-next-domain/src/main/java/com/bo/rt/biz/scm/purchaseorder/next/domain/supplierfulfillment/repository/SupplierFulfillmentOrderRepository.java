package com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.repository;

import com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.model.SupplierFulfillmentOrder;
import java.util.Optional;

/** 供应商履约单聚合仓储端口。 */
public interface SupplierFulfillmentOrderRepository {

    /** 保存供应商履约单及承诺数量账。 */
    void save(SupplierFulfillmentOrder fulfillmentOrder);

    /** 按聚合标识加载供应商履约单。 */
    Optional<SupplierFulfillmentOrder> findById(String fulfillmentOrderId);

    /** 按采购订单查找其唯一供应商履约单。 */
    Optional<SupplierFulfillmentOrder> findByPurchaseOrderId(String purchaseOrderId);
}
