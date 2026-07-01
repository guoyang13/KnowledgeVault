package com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;

/**
 * 领域事件发布端口。
 *
 * <p>领域层只产生事件，应用层通过该端口交给基础设施层做可靠发布。</p>
 */
public interface DomainEventPublisher {

    /**
     * 发布领域事件。
     *
     * @param event 已产生的领域事件
     */
    void publish(DomainEvent event);
}
