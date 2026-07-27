package com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.repository;

import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model.ExecutionTask;
import java.util.List;
import java.util.Optional;

/** 履约执行任务聚合的持久化边界。 */
public interface ExecutionTaskRepository {

    /** 保存执行任务。 */
    void save(ExecutionTask task);

    /** 按任务标识查找执行任务。 */
    Optional<ExecutionTask> findById(String taskId);

    /** 返回采购订单下尚未完成或豁免的活动任务。 */
    List<ExecutionTask> findActiveByPurchaseOrderId(String purchaseOrderId);

    /** 判断同一要求、任务类型和作用对象是否已有活动任务。 */
    boolean existsActiveByBusinessKey(String requirementId, String taskType, String scopeId);
}
