package com.bo.rt.biz.scm.purchaseorder.next.domain.planning.event;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import java.time.Instant;
import java.util.Map;

/**
 * 采购计划上下文发布的业务事实。
 *
 * <p>所有事件的 {@code eventId} 用于消费幂等，{@code aggregateId} 指向事件来源聚合，
 * {@code aggregateVersion} 用于乱序保护，{@code occurredAt} 表示事实发生时间。</p>
 */
public final class PlanningEvents {

    /** 事件容器不允许实例化。 */
    private PlanningEvents() {
    }

    /**
     * 采购需求通过审批，可以生成后续采购申请。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购需求聚合标识
     * @param aggregateVersion 采购需求事件版本
     * @param occurredAt 事实发生时间
     * @param demandNo 采购需求单号
     */
    public record ProcurementDemandApproved(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String demandNo
    ) implements DomainEvent {
    }

    /**
     * 采购申请已经生成并等待供应商确认。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购申请聚合标识
     * @param aggregateVersion 采购申请事件版本
     * @param occurredAt 事实发生时间
     * @param requisitionNo 采购申请单号
     * @param supplierCode 目标供应商编码
     */
    public record PurchaseRequisitionCreated(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String requisitionNo,
            String supplierCode
    ) implements DomainEvent {
    }

    /**
     * 供应商已经给出逐行可供货数量。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购申请聚合标识
     * @param aggregateVersion 采购申请事件版本
     * @param occurredAt 事实发生时间
     * @param requisitionNo 采购申请单号
     */
    public record RequisitionSupplierConfirmed(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String requisitionNo
    ) implements DomainEvent {
    }

    /**
     * 指定申请行数量已经被一个转单命令临时锁定。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购申请聚合标识
     * @param aggregateVersion 采购申请事件版本
     * @param occurredAt 事实发生时间
     * @param requisitionNo 采购申请单号
     * @param reservationId 转单预占标识
     * @param quantities 申请行标识到预占数量的映射
     */
    public record RequisitionQuantityReserved(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String requisitionNo,
            String reservationId,
            Map<String, Quantity> quantities
    ) implements DomainEvent {

        /** 固化事件中的申请行预占数量映射。 */
        public RequisitionQuantityReserved {
            quantities = Map.copyOf(quantities);
        }
    }

    /**
     * 预占数量已经成功形成采购订单。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购申请聚合标识
     * @param aggregateVersion 采购申请事件版本
     * @param occurredAt 事实发生时间
     * @param requisitionNo 采购申请单号
     * @param reservationId 转单预占标识
     * @param purchaseOrderId 形成的采购订单标识
     */
    public record RequisitionQuantityCommittedToOrder(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String requisitionNo,
            String reservationId,
            String purchaseOrderId
    ) implements DomainEvent {
    }

    /**
     * 建单失败或撤销后，预占数量已经恢复为可下单。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购申请聚合标识
     * @param aggregateVersion 采购申请事件版本
     * @param occurredAt 事实发生时间
     * @param requisitionNo 采购申请单号
     * @param reservationId 已释放的转单预占标识
     * @param reason 释放原因
     */
    public record RequisitionReservationReleased(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String requisitionNo,
            String reservationId,
            String reason
    ) implements DomainEvent {
    }

    /**
     * 采购申请全部确认数量已经得到处理。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购申请聚合标识
     * @param aggregateVersion 采购申请事件版本
     * @param occurredAt 事实发生时间
     * @param requisitionNo 采购申请单号
     */
    public record PurchaseRequisitionCompleted(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String requisitionNo
    ) implements DomainEvent {
    }

    /**
     * 采购申请已经按明确原因关闭。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购申请聚合标识
     * @param aggregateVersion 采购申请事件版本
     * @param occurredAt 事实发生时间
     * @param requisitionNo 采购申请单号
     * @param reason 关闭原因
     */
    public record PurchaseRequisitionClosed(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String requisitionNo,
            String reason
    ) implements DomainEvent {
    }
}
