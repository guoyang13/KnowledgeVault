package com.bo.rt.biz.scm.purchaseorder.next.application.shared.port;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;
import java.util.List;

/**
 * 事件在业务事务内写入 Outbox，事务提交后再投递 MQ。
 */
public interface DomainEventPublisher {

    /** 将一条领域事件追加到当前业务事务的 Outbox。 */
    void publish(DomainEvent event);

    /** 依次将聚合产生的领域事件追加到当前业务事务。 */
    default void publishAll(List<? extends DomainEvent> events) {
        events.forEach(this::publish);
    }
}
