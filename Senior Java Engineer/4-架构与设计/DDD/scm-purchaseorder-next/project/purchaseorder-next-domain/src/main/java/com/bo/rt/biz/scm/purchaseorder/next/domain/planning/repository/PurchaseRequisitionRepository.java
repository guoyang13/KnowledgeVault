package com.bo.rt.biz.scm.purchaseorder.next.domain.planning.repository;

import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.model.PurchaseRequisition;
import java.util.Optional;

/** 采购申请聚合仓储端口。 */
public interface PurchaseRequisitionRepository {

    /** 保存采购申请及其数量账。 */
    void save(PurchaseRequisition requisition);

    /** 按聚合标识加载采购申请。 */
    Optional<PurchaseRequisition> findById(String requisitionId);
}
