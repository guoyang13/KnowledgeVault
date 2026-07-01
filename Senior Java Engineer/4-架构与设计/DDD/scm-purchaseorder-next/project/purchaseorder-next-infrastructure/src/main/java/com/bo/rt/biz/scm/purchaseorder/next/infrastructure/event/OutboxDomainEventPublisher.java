package com.bo.rt.biz.scm.purchaseorder.next.infrastructure.event;

import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port.DomainEventPublisher;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;

/**
 * 基于 Outbox 的领域事件发布适配器。
 *
 * <p>后续应负责事件落库、可靠投递和失败重试。</p>
 */
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    /**
     * 发布领域事件。
     *
     * @param event 已产生的领域事件
     */
    @Override
    public void publish(DomainEvent event) {
        throw new UnsupportedOperationException("骨架工程仅表达事件发布边界，待补充 Outbox 实现。");
    }
}
