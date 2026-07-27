package com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.repository;

import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.model.SampleMatchingCase;
import java.util.Optional;

/** 对样聚合仓储端口。 */
public interface SampleMatchingCaseRepository {

    /** 保存对样单及证据引用。 */
    void save(SampleMatchingCase sampleCase);

    /** 按聚合标识加载对样单。 */
    Optional<SampleMatchingCase> findById(String sampleCaseId);
}
