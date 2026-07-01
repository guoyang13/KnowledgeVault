package com.bo.rt.biz.scm.purchaseorder.next.domain.purchaseorder.repository;

import com.bo.rt.biz.scm.purchaseorder.next.domain.purchaseorder.model.PurchaseOrder;
import java.util.Optional;

/**
 * 采购订单仓储。
 *
 * <p>仓储接口面向聚合，不暴露数据库表、DO 或 Mapper。</p>
 */
public interface PurchaseOrderRepository {

    /**
     * 保存采购订单聚合。
     *
     * @param purchaseOrder 采购订单聚合
     */
    void save(PurchaseOrder purchaseOrder);

    /**
     * 根据采购订单号查询聚合。
     *
     * @param purchaseOrderCode 采购订单号
     * @return 采购订单聚合；不存在时返回空
     */
    Optional<PurchaseOrder> findByCode(String purchaseOrderCode);
}
