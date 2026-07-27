package com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.event;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Money;
import java.time.Instant;

/**
 * 采购结算上下文发布的领域事件。
 *
 * <p>所有事件的前四个字段依次为事件标识、聚合标识、聚合版本和发生时间。</p>
 */
public final class SettlementEvents {

    /** 工具类不允许实例化。 */
    private SettlementEvents() {
    }

    /**
     * 已依据生效采购订单建立结算台账。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购结算聚合标识
     * @param aggregateVersion 采购结算事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param supplierCode 供应商编码
     * @param currency 结算币种
     */
    public record ProcurementSettlementInitialized(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            String supplierCode,
            String currency
    ) implements DomainEvent {
    }

    /**
     * 新业务事实使已有结算版本失效，需要重新计算。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购结算聚合标识
     * @param aggregateVersion 采购结算事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param reason 需要重新计算的原因
     */
    public record SettlementRecalculationRequired(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            String reason
    ) implements DomainEvent {
    }

    /**
     * 已生成一个新的不可变结算计算版本。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购结算聚合标识
     * @param aggregateVersion 采购结算事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param revisionNo 新生成的结算版本号
     * @param totalPayable 本版本应付金额
     */
    public record SettlementCalculated(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            int revisionNo,
            Money totalPayable
    ) implements DomainEvent {
    }

    /**
     * 当前结算版本已由业务方确认。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购结算聚合标识
     * @param aggregateVersion 采购结算事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param revisionNo 已确认的结算版本号
     * @param totalPayable 已确认的应付金额
     */
    public record SettlementConfirmed(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            int revisionNo,
            Money totalPayable
    ) implements DomainEvent {
    }

    /**
     * 当前结算版本已开始提交 AP。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购结算聚合标识
     * @param aggregateVersion 采购结算事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param revisionNo 提交的结算版本号
     * @param idempotencyKey AP 调用幂等键
     */
    public record SettlementSubmittedToAp(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            int revisionNo,
            String idempotencyKey
    ) implements DomainEvent {
    }

    /**
     * AP 已接受当前结算版本并返回应付单据号。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购结算聚合标识
     * @param aggregateVersion 采购结算事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param revisionNo AP 接受的结算版本号
     * @param apDocumentNo AP 应付单据号
     */
    public record SettlementAcceptedByAp(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            int revisionNo,
            String apDocumentNo
    ) implements DomainEvent {
    }

    /**
     * AP 已拒绝当前结算版本。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购结算聚合标识
     * @param aggregateVersion 采购结算事件版本
     * @param occurredAt 事实发生时间
     * @param purchaseOrderId 采购订单标识
     * @param revisionNo AP 拒绝的结算版本号
     * @param reason AP 拒绝原因
     */
    public record SettlementRejectedByAp(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String purchaseOrderId,
            int revisionNo,
            String reason
    ) implements DomainEvent {
    }
}
