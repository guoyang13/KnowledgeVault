package com.bo.rt.biz.scm.purchaseorder.next.domain.purchaseorder.event;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;
import java.time.LocalDateTime;

/**
 * 供应商已确认发货领域事件。
 *
 * @param purchaseOrderCode 采购订单号
 * @param occurredAt 事件发生时间
 */
public record SupplierDeliveryConfirmedEvent(
        String purchaseOrderCode,
        LocalDateTime occurredAt
) implements DomainEvent {
}
