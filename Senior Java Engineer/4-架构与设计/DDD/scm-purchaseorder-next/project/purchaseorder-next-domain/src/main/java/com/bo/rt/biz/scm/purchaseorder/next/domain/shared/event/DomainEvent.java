package com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event;

import java.time.Instant;

/**
 * 领域事件统一契约。
 *
 * <p>事件由聚合产生，在同一事务内写入 Outbox。查询投影使用 eventId 做幂等，
 * 使用 aggregateVersion 拒绝旧事件覆盖新状态。</p>
 */
public interface DomainEvent {

    /**
     * 返回事件的全局唯一标识，供 Outbox、Inbox 和投影端做幂等。
     */
    String eventId();

    /**
     * 返回产生该事件的聚合标识。
     */
    String aggregateId();

    /**
     * 返回事件发生后的聚合版本，用于拒绝乱序旧事件。
     */
    long aggregateVersion();

    /**
     * 返回业务事实发生时间，而不是 MQ 实际投递时间。
     */
    Instant occurredAt();

    /**
     * 返回默认事件类型；跨系统发布时可以映射为稳定的契约名称。
     */
    default String eventType() {
        return getClass().getSimpleName();
    }
}
