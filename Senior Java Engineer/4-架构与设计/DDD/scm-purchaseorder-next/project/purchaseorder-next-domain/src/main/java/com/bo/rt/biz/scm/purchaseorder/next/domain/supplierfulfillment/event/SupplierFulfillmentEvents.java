package com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.event;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 供应商履约上下文发布的承诺和发运事实。
 *
 * <p>事件公共元数据字段遵循 {@code DomainEvent} 契约。</p>
 */
public final class SupplierFulfillmentEvents {

    /** 事件容器不允许实例化。 */
    private SupplierFulfillmentEvents() {
    }

    /**
     * 供应商确认事件中的订单行承诺快照。
     *
     * @param purchaseOrderLineId 采购订单行标识
     * @param orderedQuantity 订单要求数量
     * @param committedQuantity 供应商承诺数量
     * @param promisedDate 供应商承诺交付日期
     */
    public record CommitmentSnapshot(
            String purchaseOrderLineId,
            Quantity orderedQuantity,
            Quantity committedQuantity,
            LocalDate promisedDate
    ) {
    }

    /**
     * 发运事件中的订单行数量快照。
     *
     * @param purchaseOrderLineId 采购订单行标识
     * @param shippedQuantity 本批实际发运数量
     */
    public record ShipmentLineSnapshot(
            String purchaseOrderLineId,
            Quantity shippedQuantity
    ) {
    }

    /**
     * PO 生效后已经创建唯一供应商履约单。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 供应商履约单聚合标识
     * @param aggregateVersion 供应商履约单事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param purchaseOrderNo 采购订单号
     * @param supplierCode 供应商编码
     */
    public record SupplierFulfillmentOrderCreated(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            String purchaseOrderNo,
            String supplierCode
    ) implements DomainEvent {
    }

    /**
     * 供应商已经确认全部订单行的数量和交期。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 供应商履约单聚合标识
     * @param aggregateVersion 供应商履约单事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param commitments 逐订单行供应商承诺快照
     */
    public record SupplierCommitmentConfirmed(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            List<CommitmentSnapshot> commitments
    ) implements DomainEvent {

        /** 固化事件中的供应商承诺列表。 */
        public SupplierCommitmentConfirmed {
            commitments = List.copyOf(commitments);
        }
    }

    /**
     * 供应商拒绝了承接本采购订单。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 供应商履约单聚合标识
     * @param aggregateVersion 供应商履约单事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param reason 拒绝原因
     */
    public record SupplierCommitmentRejected(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            String reason
    ) implements DomainEvent {
    }

    /**
     * 供应商的行级交付承诺已经变更。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 供应商履约单聚合标识
     * @param aggregateVersion 供应商履约单事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param purchaseOrderLineId 采购订单行标识
     * @param committedQuantity 更新后的承诺数量
     * @param promisedDate 更新后的承诺交付日期
     */
    public record SupplierCommitmentChanged(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            String purchaseOrderLineId,
            Quantity committedQuantity,
            LocalDate promisedDate
    ) implements DomainEvent {
    }

    /**
     * 新的供应商发运通知已经创建。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 供应商发运批次聚合标识
     * @param aggregateVersion 发运批次事件版本
     * @param occurredAt 事实发生时间
     * @param shipmentNo 发运批次号
     * @param fulfillmentOrderId 供应商履约单标识
     * @param fulfillmentUnitId 履约单元标识
     */
    public record SupplierShipmentNoticeCreated(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String shipmentNo,
            String fulfillmentOrderId,
            String fulfillmentUnitId
    ) implements DomainEvent {
    }

    /**
     * 发运资料已完整，正在等待发货门禁。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 供应商发运批次聚合标识
     * @param aggregateVersion 发运批次事件版本
     * @param occurredAt 事实发生时间
     * @param shipmentNo 发运批次号
     */
    public record SupplierShipmentReady(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String shipmentNo
    ) implements DomainEvent {
    }

    /**
     * 指定履约单元已经由供应商实际发出。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 供应商发运批次聚合标识
     * @param aggregateVersion 发运批次事件版本
     * @param occurredAt 事实发生时间
     * @param shipmentNo 发运批次号
     * @param fulfillmentOrderId 供应商履约单标识
     * @param fulfillmentUnitId 履约单元标识
     * @param supplierCode 供应商编码
     * @param lines 本批发运行数量快照
     */
    public record SupplierShipmentDispatched(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String shipmentNo,
            String fulfillmentOrderId,
            String fulfillmentUnitId,
            String supplierCode,
            List<ShipmentLineSnapshot> lines
    ) implements DomainEvent {

        /** 固化事件中的发运行列表。 */
        public SupplierShipmentDispatched {
            lines = List.copyOf(lines);
        }
    }

    /**
     * 尚未发出的供应商发运通知已经取消。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 供应商发运批次聚合标识
     * @param aggregateVersion 发运批次事件版本
     * @param occurredAt 事实发生时间
     * @param shipmentNo 发运批次号
     * @param reason 取消原因
     */
    public record SupplierShipmentCancelled(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String shipmentNo,
            String reason
    ) implements DomainEvent {
    }

    /**
     * 供应商履约承诺已经全部完成。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 供应商履约单聚合标识
     * @param aggregateVersion 供应商履约单事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     */
    public record SupplierFulfillmentCompleted(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId
    ) implements DomainEvent {
    }
}
