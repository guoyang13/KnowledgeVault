package com.bo.rt.biz.scm.purchaseorder.next.domain.planning.repository;

import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.model.ProcurementDemand;
import java.util.Optional;

/** 采购需求聚合仓储端口。 */
public interface ProcurementDemandRepository {

    /** 保存采购需求及其乐观锁版本。 */
    void save(ProcurementDemand demand);

    /** 按聚合标识加载采购需求。 */
    Optional<ProcurementDemand> findById(String demandId);
}
