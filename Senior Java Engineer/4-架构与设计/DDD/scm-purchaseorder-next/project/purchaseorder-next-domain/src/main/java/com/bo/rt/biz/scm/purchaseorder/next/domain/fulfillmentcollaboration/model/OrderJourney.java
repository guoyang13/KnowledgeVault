package com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 可由事件重建的订单旅程投影，不作为采购订单聚合字段。
 */
public class OrderJourney {

    /** 被投影的采购订单标识。 */
    private final String purchaseOrderId;
    /** 按接收顺序记录的履约里程碑事实。 */
    private final List<JourneyMilestone> milestones = new ArrayList<>();
    /** 已处理来源事件标识，用于保证投影幂等。 */
    private final Set<String> processedEventIds = new HashSet<>();
    /** 根据已知最远里程碑推导的订单总体阶段。 */
    private OverallStage stage = OverallStage.ORDER_EFFECTIVE;

    /** 为一个采购订单创建初始旅程投影。 */
    public OrderJourney(String purchaseOrderId) {
        require(purchaseOrderId != null && !purchaseOrderId.isBlank(), "采购订单 ID 不能为空");
        this.purchaseOrderId = purchaseOrderId;
    }

    /**
     * 幂等投影一条来自履约上下文的里程碑事实。
     *
     * <p>晚到的早期事件仍会保留在里程碑列表中，但不会使总体阶段倒退。</p>
     */
    public void project(
            String sourceEventId,
            MilestoneType type,
            String fulfillmentUnitId,
            Instant occurredAt,
            String sourceReference
    ) {
        require(sourceEventId != null && !sourceEventId.isBlank(), "来源事件 ID 不能为空");
        if (!processedEventIds.add(sourceEventId)) {
            return;
        }
        milestones.add(new JourneyMilestone(
                type, fulfillmentUnitId, occurredAt, sourceReference, sourceEventId
        ));
        if (type.stage().ordinal() > stage.ordinal()) {
            stage = type.stage();
        }
    }

    /** 返回采购订单标识。 */
    public String purchaseOrderId() {
        return purchaseOrderId;
    }

    /** 返回当前推导出的订单总体阶段。 */
    public OverallStage stage() {
        return stage;
    }

    /** 返回里程碑快照，调用方不能修改内部列表。 */
    public List<JourneyMilestone> milestones() {
        return List.copyOf(milestones);
    }

    /** 供查询和运营跟踪使用的订单总体履约阶段。 */
    public enum OverallStage {
        ORDER_EFFECTIVE,
        SUPPLIER_CONFIRMED,
        IN_PRODUCTION,
        DISPATCHED,
        RECEIVED,
        QUALITY_INSPECTION,
        INBOUNDED,
        SETTLEMENT,
        COMPLETED
    }

    /** 可以从跨上下文事件投影得到的履约里程碑类型。 */
    public enum MilestoneType {
        ORDER_EFFECTIVE(OverallStage.ORDER_EFFECTIVE),
        SUPPLIER_COMMITMENT_CONFIRMED(OverallStage.SUPPLIER_CONFIRMED),
        SHIPMENT_DISPATCHED(OverallStage.DISPATCHED),
        GOODS_RECEIVED(OverallStage.RECEIVED),
        QUALITY_COMPLETED(OverallStage.QUALITY_INSPECTION),
        GOODS_INBOUNDED(OverallStage.INBOUNDED),
        SETTLEMENT_CONFIRMED(OverallStage.SETTLEMENT),
        ORDER_COMPLETED(OverallStage.COMPLETED);

        /** 该里程碑对应的总体履约阶段。 */
        private final OverallStage stage;

        /** 将具体里程碑映射到总体阶段。 */
        MilestoneType(OverallStage stage) {
            this.stage = stage;
        }

        /** 返回该里程碑对应的总体阶段。 */
        public OverallStage stage() {
            return stage;
        }
    }

    /**
     * 一条可追溯的订单履约里程碑。
     *
     * @param type 里程碑类型
     * @param fulfillmentUnitId 发运单、收货批次等履约单元标识
     * @param occurredAt 业务事实发生时间
     * @param sourceReference 来源业务单据引用
     * @param sourceEventId 来源事件标识，用于幂等
     */
    public record JourneyMilestone(
            MilestoneType type,
            String fulfillmentUnitId,
            Instant occurredAt,
            String sourceReference,
            String sourceEventId
    ) {
    }
}
