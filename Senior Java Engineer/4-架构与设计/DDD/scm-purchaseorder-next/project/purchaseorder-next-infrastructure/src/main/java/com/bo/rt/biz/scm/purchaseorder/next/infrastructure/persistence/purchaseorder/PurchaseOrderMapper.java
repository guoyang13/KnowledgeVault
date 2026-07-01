package com.bo.rt.biz.scm.purchaseorder.next.infrastructure.persistence.purchaseorder;

import java.util.Optional;

/**
 * 采购订单持久化 Mapper。
 *
 * <p>后续可接入 MyBatis 或其他持久化框架。</p>
 */
public interface PurchaseOrderMapper {

    /**
     * 保存采购订单数据库对象。
     *
     * @param purchaseOrderDO 采购订单数据库对象
     */
    void save(PurchaseOrderDO purchaseOrderDO);

    /**
     * 根据采购订单号查询数据库对象。
     *
     * @param purchaseOrderCode 采购订单号
     * @return 数据库对象；不存在时返回空
     */
    Optional<PurchaseOrderDO> findByCode(String purchaseOrderCode);
}
