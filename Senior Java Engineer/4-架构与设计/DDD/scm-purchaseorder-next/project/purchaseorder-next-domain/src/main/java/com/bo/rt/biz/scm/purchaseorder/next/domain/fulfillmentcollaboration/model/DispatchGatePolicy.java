package com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model;

import java.util.List;

/**
 * 发货门禁领域策略：只判断阻塞任务，不替代外部任务自己的业务规则。
 */
public final class DispatchGatePolicy {

    /** 领域策略不持有状态，不允许实例化。 */
    private DispatchGatePolicy() {
    }

    /**
     * 根据订单级及目标履约单元级的未完成任务判断是否允许发货。
     *
     * @param fulfillmentUnitId 待发货的履约单元标识
     * @param tasks 当前采购订单下的活动任务
     * @return 发货结论及全部阻塞任务标识
     */
    public static Decision evaluate(String fulfillmentUnitId, List<ExecutionTask> tasks) {
        List<String> blockingTaskIds = tasks.stream()
                .filter(task -> task.blocks(
                        ExecutionRequirement.Milestone.DISPATCH,
                        fulfillmentUnitId
                ))
                .map(ExecutionTask::id)
                .toList();
        return new Decision(blockingTaskIds.isEmpty(), blockingTaskIds);
    }

    /**
     * 发货门禁评估结果。
     *
     * @param dispatchAllowed 是否允许发货
     * @param blockingTaskIds 仍在阻塞发货的任务标识
     */
    public record Decision(boolean dispatchAllowed, List<String> blockingTaskIds) {

        /** 固化阻塞任务列表，避免评估结果被调用方修改。 */
        public Decision {
            blockingTaskIds = List.copyOf(blockingTaskIds);
        }
    }
}
