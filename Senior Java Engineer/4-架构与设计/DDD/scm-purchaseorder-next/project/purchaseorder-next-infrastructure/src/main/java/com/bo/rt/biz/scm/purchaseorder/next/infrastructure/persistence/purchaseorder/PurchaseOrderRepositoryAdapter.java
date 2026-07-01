package com.bo.rt.biz.scm.purchaseorder.next.infrastructure.persistence.purchaseorder;

import com.bo.rt.biz.scm.purchaseorder.next.domain.purchaseorder.model.PurchaseOrder;
import com.bo.rt.biz.scm.purchaseorder.next.domain.purchaseorder.repository.PurchaseOrderRepository;
import java.util.Optional;

/**
 * 采购订单仓储适配器。
 *
 * <p>负责在领域聚合和数据库对象之间转换，避免数据库模型污染领域层。</p>
 */
public class PurchaseOrderRepositoryAdapter implements PurchaseOrderRepository {

    /**
     * 保存采购订单聚合。
     *
     * @param purchaseOrder 采购订单聚合
     */
    @Override
    public void save(PurchaseOrder purchaseOrder) {
        throw new UnsupportedOperationException("骨架工程仅表达仓储适配边界，待补充持久化实现。");
    }

    /**
     * 根据采购订单号查询聚合。
     *
     * @param purchaseOrderCode 采购订单号
     * @return 采购订单聚合；不存在时返回空
     */
    @Override
    public Optional<PurchaseOrder> findByCode(String purchaseOrderCode) {
        throw new UnsupportedOperationException("骨架工程仅表达仓储适配边界，待补充持久化实现。");
    }
}
