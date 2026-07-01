package com.bo.rt.biz.scm.purchaseorder.next.domain.purchaseorder.event;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;
import java.time.LocalDateTime;

/**
 * 采购订单已提交领域事件。
 *
 * @param purchaseOrderCode 采购订单号
 * @param occurredAt 事件发生时间
 */
public record PurchaseOrderSubmittedEvent(
        String purchaseOrderCode,
        LocalDateTime occurredAt
) implements DomainEvent {
}
