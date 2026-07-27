package com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.event;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import java.time.Instant;
import java.util.List;

/**
 * 质量检验上下文发布的对样、质检、接收和拒收事实。
 *
 * <p>事件公共元数据字段遵循 {@code DomainEvent} 契约。</p>
 */
public final class QualityEvents {

    /** 事件容器不允许实例化。 */
    private QualityEvents() {
    }

    /**
     * 可进入采购结算的订单行接收数量。
     *
     * @param purchaseOrderLineId 采购订单行标识
     * @param acceptedQuantity 可结算的质量接收数量
     */
    public record AcceptedLine(
            String purchaseOrderLineId,
            Quantity acceptedQuantity
    ) {
    }

    /**
     * 因质量结论被拒收的订单行数量。
     *
     * @param purchaseOrderLineId 采购订单行标识
     * @param rejectedQuantity 质量拒收数量
     * @param decision 行级质量决定
     */
    public record RejectedLine(
            String purchaseOrderLineId,
            Quantity rejectedQuantity,
            String decision
    ) {
    }

    /**
     * 指定业务范围已经产生对样要求。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 对样单聚合标识
     * @param aggregateVersion 对样单事件版本
     * @param occurredAt 事实发生时间
     * @param referenceType 对样来源业务类型
     * @param referenceId 对样来源业务标识
     */
    public record SampleMatchingRequired(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String referenceType,
            String referenceId
    ) implements DomainEvent {
    }

    /**
     * 对样结论全部通过。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 对样单聚合标识
     * @param aggregateVersion 对样单事件版本
     * @param occurredAt 事实发生时间
     * @param referenceId 对样来源业务标识
     */
    public record SampleMatchingPassed(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String referenceId
    ) implements DomainEvent {
    }

    /**
     * 对样出现异常或样品未到。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 对样单聚合标识
     * @param aggregateVersion 对样单事件版本
     * @param occurredAt 事实发生时间
     * @param referenceId 对样来源业务标识
     * @param conclusion 异常或未到样结论
     */
    public record SampleMatchingAbnormal(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String referenceId,
            String conclusion
    ) implements DomainEvent {
    }

    /**
     * 一个实际收货批次已经生成质检单。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 质检单聚合标识
     * @param aggregateVersion 质检单事件版本
     * @param occurredAt 事实发生时间
     * @param inspectionNo 质检业务单号
     * @param purchaseOrderId 采购订单标识
     * @param fulfillmentUnitId 实际收货履约单元标识
     */
    public record QualityInspectionRequired(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String inspectionNo,
            String purchaseOrderId,
            String fulfillmentUnitId
    ) implements DomainEvent {
    }

    /**
     * 质检已经按指定标准版本开始。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 质检单聚合标识
     * @param aggregateVersion 质检单事件版本
     * @param occurredAt 事实发生时间
     * @param inspectionNo 质检业务单号
     * @param standardVersion 本轮采用的质量标准版本
     */
    public record QualityInspectionStarted(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String inspectionNo,
            String standardVersion
    ) implements DomainEvent {
    }

    /**
     * 订单行存在质量接收数量。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 质检单聚合标识
     * @param aggregateVersion 质检单事件版本
     * @param occurredAt 事实发生时间
     * @param inspectionNo 质检业务单号
     * @param purchaseOrderLineId 采购订单行标识
     * @param acceptedQuantity 本次确认的质量接收数量
     */
    public record InspectionLineAccepted(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String inspectionNo,
            String purchaseOrderLineId,
            Quantity acceptedQuantity
    ) implements DomainEvent {
    }

    /**
     * 订单行存在质量拒收数量。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 质检单聚合标识
     * @param aggregateVersion 质检单事件版本
     * @param occurredAt 事实发生时间
     * @param inspectionNo 质检业务单号
     * @param purchaseOrderLineId 采购订单行标识
     * @param rejectedQuantity 本次确认的质量拒收数量
     */
    public record InspectionLineRejected(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String inspectionNo,
            String purchaseOrderLineId,
            Quantity rejectedQuantity
    ) implements DomainEvent {
    }

    /**
     * 指定履约批次的必检项目已经完成。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 质检单聚合标识
     * @param aggregateVersion 质检单事件版本
     * @param occurredAt 事实发生时间
     * @param inspectionNo 质检业务单号
     * @param purchaseOrderId 采购订单标识
     * @param fulfillmentUnitId 履约单元标识
     */
    public record QualityInspectionCompleted(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String inspectionNo,
            String purchaseOrderId,
            String fulfillmentUnitId
    ) implements DomainEvent {
    }

    /**
     * 已完成质检经授权重新打开。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 质检单聚合标识
     * @param aggregateVersion 质检单事件版本
     * @param occurredAt 事实发生时间
     * @param inspectionNo 质检业务单号
     * @param authorizationReference 重开审批或授权依据
     * @param reason 重开原因
     */
    public record QualityInspectionReopened(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String inspectionNo,
            String authorizationReference,
            String reason
    ) implements DomainEvent {
    }

    /**
     * 结算上下文只消费可结算数量，不需要理解质检内部状态。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 质检单聚合标识
     * @param aggregateVersion 质检单事件版本
     * @param occurredAt 事实发生时间
     * @param inspectionNo 质检业务单号
     * @param purchaseOrderId 采购订单标识
     * @param fulfillmentUnitId 履约单元标识
     * @param lines 可进入结算的订单行接收数量
     */
    public record GoodsAcceptedForSettlement(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String inspectionNo,
            String purchaseOrderId,
            String fulfillmentUnitId,
            List<AcceptedLine> lines
    ) implements DomainEvent {

        /** 固化事件中的合格数量列表。 */
        public GoodsAcceptedForSettlement {
            lines = List.copyOf(lines);
        }
    }

    /**
     * 按订单行发布质量拒收数量及决定。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 质检单聚合标识
     * @param aggregateVersion 质检单事件版本
     * @param occurredAt 事实发生时间
     * @param inspectionNo 质检业务单号
     * @param purchaseOrderId 采购订单标识
     * @param fulfillmentUnitId 履约单元标识
     * @param lines 订单行质量拒收数量及决定
     */
    public record GoodsRejectedByQuality(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String inspectionNo,
            String purchaseOrderId,
            String fulfillmentUnitId,
            List<RejectedLine> lines
    ) implements DomainEvent {

        /** 固化事件中的拒收数量列表。 */
        public GoodsRejectedByQuality {
            lines = List.copyOf(lines);
        }
    }
}
