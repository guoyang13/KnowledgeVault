package com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.repository;

import com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.model.ProcurementSettlement;
import com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.model.SettlementRevision;
import java.util.List;
import java.util.Optional;

/**
 * 采购结算聚合及其历史计算版本的持久化边界。
 */
public interface ProcurementSettlementRepository {

    /** 保存结算聚合及其当前版本。 */
    void save(ProcurementSettlement settlement);

    /** 按结算标识查找聚合。 */
    Optional<ProcurementSettlement> findById(String settlementId);

    /** 按采购订单标识查找其唯一结算聚合。 */
    Optional<ProcurementSettlement> findByPurchaseOrderId(String purchaseOrderId);

    /** 按版本号顺序返回结算的历史计算版本。 */
    List<SettlementRevision> findRevisionHistory(String settlementId);
}
