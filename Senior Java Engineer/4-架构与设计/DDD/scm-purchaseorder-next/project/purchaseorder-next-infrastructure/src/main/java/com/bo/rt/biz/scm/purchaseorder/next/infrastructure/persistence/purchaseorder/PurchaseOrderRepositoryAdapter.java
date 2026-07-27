package com.bo.rt.biz.scm.purchaseorder.next.infrastructure.persistence.purchaseorder;

import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.PurchaseOrder;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.repository.PurchaseOrderRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 采购订单仓储适配器。
 *
 * <p>负责在领域聚合和数据库对象之间转换，避免数据库模型污染领域层。</p>
 */
public class PurchaseOrderRepositoryAdapter implements PurchaseOrderRepository {

    /** 按聚合标识保存的演示数据。 */
    private final Map<String, PurchaseOrder> byId = new ConcurrentHashMap<>();
    /** 采购订单号到聚合标识的唯一索引。 */
    private final Map<String, String> idByOrderNo = new ConcurrentHashMap<>();

    /**
     * 保存采购订单聚合。
     *
     * @param purchaseOrder 采购订单聚合
     */
    @Override
    public void save(PurchaseOrder purchaseOrder) {
        byId.put(purchaseOrder.id(), purchaseOrder);
        idByOrderNo.put(purchaseOrder.orderNo(), purchaseOrder.id());
    }

    /** 按采购订单标识查询聚合。 */
    @Override
    public Optional<PurchaseOrder> findById(String purchaseOrderId) {
        return Optional.ofNullable(byId.get(purchaseOrderId));
    }

    /** 按采购订单号的唯一索引查询聚合。 */
    @Override
    public Optional<PurchaseOrder> findByOrderNo(String orderNo) {
        return Optional.ofNullable(idByOrderNo.get(orderNo)).map(byId::get);
    }
}
