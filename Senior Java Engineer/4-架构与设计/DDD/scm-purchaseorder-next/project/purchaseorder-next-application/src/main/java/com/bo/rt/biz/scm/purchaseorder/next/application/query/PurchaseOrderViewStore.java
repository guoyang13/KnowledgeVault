package com.bo.rt.biz.scm.purchaseorder.next.application.query;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;
import java.util.List;
import java.util.Optional;

/**
 * 查询库端口。事件 Inbox、引用映射和视图更新应在同一查询库事务中提交。
 */
public interface PurchaseOrderViewStore {

    /** 判断事件是否已经由查询库 Inbox 处理。 */
    boolean isProcessed(String eventId);

    /** 将事件标记为已处理并推进投影水位。 */
    void markProcessed(String eventId);

    /** 按采购订单标识查找详情视图。 */
    Optional<PurchaseOrderExecutionView> findByPurchaseOrderId(String purchaseOrderId);

    /** 按采购订单号查找详情视图。 */
    Optional<PurchaseOrderExecutionView> findByOrderNo(String orderNo);

    /** 保存采购订单执行视图。 */
    void save(PurchaseOrderExecutionView view);

    /** 建立外部聚合或业务引用到采购订单的关联。 */
    void bindReference(String referenceType, String referenceId, String purchaseOrderId);

    /** 解析外部聚合或业务引用所属的采购订单。 */
    Optional<String> resolveReference(String referenceType, String referenceId);

    /** 暂存因上游引用尚未到达而无法投影的事件。 */
    void defer(String correlationId, DomainEvent event);

    /** 取出并删除指定关联键下的延迟事件。 */
    List<DomainEvent> takeDeferred(String correlationId);

    /** 按组合条件搜索采购订单执行视图。 */
    List<PurchaseOrderExecutionView> search(QueryCriteria criteria);

    /** 返回查询库已处理事件的单调水位。 */
    long projectionWatermark();

    /**
     * 采购订单列表查询条件。
     *
     * @param buyerOrganizationId 采购主体筛选条件
     * @param supplierCode 供应商筛选条件
     * @param orderStatus 订单状态筛选条件
     * @param overallStage 总体履约阶段筛选条件
     * @param offset 从零开始的结果偏移量
     * @param limit 最大返回条数
     */
    record QueryCriteria(
            String buyerOrganizationId,
            String supplierCode,
            String orderStatus,
            String overallStage,
            int offset,
            int limit
    ) {
    }
}
