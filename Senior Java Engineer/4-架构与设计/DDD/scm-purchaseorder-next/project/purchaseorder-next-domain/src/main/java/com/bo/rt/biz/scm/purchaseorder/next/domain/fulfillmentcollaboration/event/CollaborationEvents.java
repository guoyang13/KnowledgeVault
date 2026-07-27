package com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.event;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;
import java.time.Instant;
import java.util.List;

/**
 * 采购履约协同上下文发布的领域事件。
 *
 * <p>领域事件的前四个字段依次为事件标识、聚合标识、聚合版本和发生时间。</p>
 */
public final class CollaborationEvents {

    /** 工具类不允许实例化。 */
    private CollaborationEvents() {
    }

    /**
     * 要求计划事件携带的不可变要求快照。
     *
     * @param requirementId 要求标识
     * @param type 要求类型
     * @param scope 作用对象类型
     * @param scopeId 作用对象标识
     * @param applicability 适用性结论
     * @param blockingMilestone 阻塞的履约里程碑，不适用时为空
     */
    public record RequirementSnapshot(
            String requirementId,
            String type,
            String scope,
            String scopeId,
            String applicability,
            String blockingMilestone
    ) {
    }

    /**
     * 新版本执行要求计划已激活。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 执行要求计划聚合标识
     * @param aggregateVersion 要求计划事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param purchaseOrderVersion 本计划依据的采购订单版本
     * @param planVersion 要求计划版本
     * @param requirements 本次规则评估的完整要求快照
     */
    public record ExecutionRequirementPlanCreated(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            long purchaseOrderVersion,
            int planVersion,
            List<RequirementSnapshot> requirements
    ) implements DomainEvent {

        /** 固化事件中的要求列表。 */
        public ExecutionRequirementPlanCreated {
            requirements = List.copyOf(requirements);
        }
    }

    /**
     * 一项适用的履约要求已生成执行任务。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 执行任务聚合标识
     * @param aggregateVersion 执行任务事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param requirementId 来源要求标识
     * @param requirementType 要求业务类型
     * @param scopeId 任务作用对象标识
     * @param deadline 任务截止时间
     */
    public record ExecutionTaskCreated(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            String requirementId,
            String requirementType,
            String scopeId,
            Instant deadline
    ) implements DomainEvent {
    }

    /**
     * 执行任务已由负责业务上下文确认完成。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 执行任务聚合标识
     * @param aggregateVersion 执行任务事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param requirementId 来源要求标识
     * @param requirementType 要求业务类型
     * @param scopeId 任务作用对象标识
     */
    public record ExecutionTaskCompleted(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            String requirementId,
            String requirementType,
            String scopeId
    ) implements DomainEvent {
    }

    /**
     * 执行任务已根据有效审批依据被豁免。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 执行任务聚合标识
     * @param aggregateVersion 执行任务事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param requirementId 来源要求标识
     * @param reason 豁免原因
     * @param approvalReference 豁免审批依据
     */
    public record ExecutionTaskWaived(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            String requirementId,
            String reason,
            String approvalReference
    ) implements DomainEvent {
    }

    /**
     * 某履约单元的发货门禁结论发生变化。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 门禁决定来源聚合标识
     * @param aggregateVersion 来源聚合事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param fulfillmentUnitId 被评估的履约单元标识
     * @param dispatchAllowed 是否允许发货
     * @param blockingTaskIds 阻塞发货的执行任务标识
     */
    public record DispatchGateChanged(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            String fulfillmentUnitId,
            boolean dispatchAllowed,
            List<String> blockingTaskIds
    ) implements DomainEvent {

        /** 固化事件中的阻塞任务列表。 */
        public DispatchGateChanged {
            blockingTaskIds = List.copyOf(blockingTaskIds);
        }
    }

    /**
     * 一个新的履约异常处理单已打开。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 履约异常聚合标识
     * @param aggregateVersion 履约异常事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param businessKey 异常来源的稳定业务键
     * @param exceptionType 异常业务类型
     * @param severity 初始严重级别
     * @param relatedTaskId 关联执行任务标识，可以为空
     */
    public record ExecutionExceptionOpened(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            String businessKey,
            String exceptionType,
            String severity,
            String relatedTaskId
    ) implements DomainEvent {
    }

    /**
     * 履约异常已升级到更高严重级别。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 履约异常聚合标识
     * @param aggregateVersion 履约异常事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param businessKey 异常来源的稳定业务键
     * @param severity 升级后的严重级别
     * @param reason 升级原因
     */
    public record ExecutionExceptionEscalated(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            String businessKey,
            String severity,
            String reason
    ) implements DomainEvent {
    }

    /**
     * 履约异常已形成解决方案。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 履约异常聚合标识
     * @param aggregateVersion 履约异常事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param businessKey 异常来源的稳定业务键
     * @param resolution 解决方案或处置结论
     */
    public record ExecutionExceptionResolved(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            String businessKey,
            String resolution
    ) implements DomainEvent {
    }

    /**
     * 跨上下文业务事实已被投影为订单旅程里程碑。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 订单旅程投影标识
     * @param aggregateVersion 旅程投影事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param milestoneType 里程碑类型
     * @param fulfillmentUnitId 对应履约单元标识
     * @param sourceEventId 产生该里程碑的来源事件标识
     */
    public record JourneyMilestoneProjected(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            String milestoneType,
            String fulfillmentUnitId,
            String sourceEventId
    ) implements DomainEvent {
    }
}
