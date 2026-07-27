package com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 聚合根基础类，仅处理乐观锁版本与待发布事件。
 */
public abstract class AbstractAggregateRoot {

    /** 当前聚合版本，持久化时作为乐观锁条件。 */
    private long version;

    /** 当前事务中产生、尚未交给 Outbox 的领域事件。 */
    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    /**
     * 返回聚合当前版本。
     */
    public long version() {
        return version;
    }

    /**
     * 在一次有效状态变化后推进聚合版本。
     */
    protected long nextVersion() {
        return ++version;
    }

    /**
     * 登记本次状态变化产生的领域事件，并校验事件版本与聚合一致。
     */
    protected void raise(DomainEvent event) {
        if (event.aggregateVersion() != version) {
            throw new IllegalArgumentException("事件版本必须等于聚合当前版本");
        }
        pendingEvents.add(event);
    }

    /**
     * 取出并清空待发布事件；应用服务保存聚合后将其写入 Outbox。
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return events;
    }

    /**
     * 仓储重建聚合时使用，业务代码不应调用。
     */
    public void restoreVersion(long restoredVersion) {
        if (restoredVersion < 0) {
            throw new IllegalArgumentException("聚合版本不能为负数");
        }
        version = restoredVersion;
    }
}
