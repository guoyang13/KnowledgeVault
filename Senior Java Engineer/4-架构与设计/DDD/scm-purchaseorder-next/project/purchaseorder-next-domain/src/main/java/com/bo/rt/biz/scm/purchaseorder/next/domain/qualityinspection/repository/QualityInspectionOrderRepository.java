package com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.repository;

import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.model.QualityInspectionOrder;
import java.util.Optional;

/** 到货质检单聚合仓储端口。 */
public interface QualityInspectionOrderRepository {

    /** 保存质检单、行级决定及历史版本。 */
    void save(QualityInspectionOrder inspectionOrder);

    /** 按聚合标识加载质检单。 */
    Optional<QualityInspectionOrder> findById(String inspectionOrderId);

    /** 按履约批次查找唯一质检单。 */
    Optional<QualityInspectionOrder> findByFulfillmentUnitId(String fulfillmentUnitId);
}
