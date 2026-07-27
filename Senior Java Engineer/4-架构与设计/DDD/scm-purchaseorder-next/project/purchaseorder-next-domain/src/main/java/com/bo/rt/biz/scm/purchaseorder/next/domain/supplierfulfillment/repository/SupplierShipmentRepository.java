package com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.repository;

import com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.model.SupplierShipment;
import java.util.Optional;

/** 供应商发运批次聚合仓储端口。 */
public interface SupplierShipmentRepository {

    /** 保存发运通知及其批次状态。 */
    void save(SupplierShipment shipment);

    /** 按聚合标识加载发运批次。 */
    Optional<SupplierShipment> findById(String shipmentId);
}
