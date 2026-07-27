package com.bo.rt.biz.scm.purchaseorder.next.infrastructure.event;

import com.bo.rt.biz.scm.purchaseorder.next.application.shared.port.DomainEventPublisher;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Outbox 的领域事件发布适配器。
 *
 * <p>后续应负责事件落库、可靠投递和失败重试。</p>
 */
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    /** 演示用事务事件缓冲区，生产实现应替换为 Outbox 数据表。 */
    private final List<DomainEvent> transactionalOutbox = new ArrayList<>();

    /**
     * 发布领域事件。
     *
     * @param event 已产生的领域事件
     */
    @Override
    public void publish(DomainEvent event) {
        transactionalOutbox.add(event);
    }

    /**
     * 演示事件中继读取未投递记录。真实实现应使用 outbox 表、锁批次和重试状态。
     *
     * @return 本批待投递事件的不可变快照
     */
    public List<DomainEvent> drain() {
        List<DomainEvent> events = List.copyOf(transactionalOutbox);
        transactionalOutbox.clear();
        return events;
    }
}
