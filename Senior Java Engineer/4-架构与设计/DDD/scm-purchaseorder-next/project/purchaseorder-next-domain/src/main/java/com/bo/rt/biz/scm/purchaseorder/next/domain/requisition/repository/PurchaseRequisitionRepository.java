package com.bo.rt.biz.scm.purchaseorder.next.domain.requisition.repository;

import com.bo.rt.biz.scm.purchaseorder.next.domain.requisition.model.PurchaseRequisition;
import java.util.Optional;

/**
 * 采购需求仓储。
 */
public interface PurchaseRequisitionRepository {

    /**
     * 保存采购需求聚合。
     *
     * @param requisition 采购需求聚合
     */
    void save(PurchaseRequisition requisition);

    /**
     * 根据采购需求单号查询聚合。
     *
     * @param requisitionCode 采购需求单号
     * @return 采购需求聚合；不存在时返回空
     */
    Optional<PurchaseRequisition> findByCode(String requisitionCode);
}
