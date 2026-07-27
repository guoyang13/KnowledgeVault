package com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.repository;

import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model.ExecutionRequirementPlan;
import java.util.Optional;

/** 执行要求计划聚合的持久化边界。 */
public interface ExecutionRequirementPlanRepository {

    /** 保存要求计划及其状态。 */
    void save(ExecutionRequirementPlan plan);

    /** 查找采购订单当前唯一的活动要求计划。 */
    Optional<ExecutionRequirementPlan> findActiveByPurchaseOrderId(String purchaseOrderId);
}
