package com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.repository;

import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.OrderChange;
import java.util.Optional;

/** 生效后订单变更聚合仓储端口。 */
public interface OrderChangeRepository {

    /** 保存订单变更及其审批状态。 */
    void save(OrderChange orderChange);

    /** 按变更单标识加载订单变更。 */
    Optional<OrderChange> findById(String changeId);
}
