package com.bo.rt.biz.scm.purchaseorder.next.infrastructure.query;

import com.bo.rt.biz.scm.purchaseorder.next.application.query.PurchaseOrderExecutionView;
import com.bo.rt.biz.scm.purchaseorder.next.application.query.PurchaseOrderViewStore;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 采购订单查询库端口的内存实现，仅用于场景测试与架构演示。
 */
public class InMemoryPurchaseOrderViewStore implements PurchaseOrderViewStore {

    /** 按采购订单标识保存的执行视图。 */
    private final Map<String, PurchaseOrderExecutionView> byId = new HashMap<>();
    /** 采购订单号到采购订单标识的索引。 */
    private final Map<String, String> idByOrderNo = new HashMap<>();
    /** 子聚合或外部业务引用到采购订单标识的映射。 */
    private final Map<String, String> references = new HashMap<>();
    /** 上游引用尚未建立时暂存的领域事件。 */
    private final Map<String, List<DomainEvent>> deferred = new HashMap<>();
    /** 查询库 Inbox 已处理的事件标识。 */
    private final Set<String> processedEventIds = new HashSet<>();
    /** 每处理一个新事件递增一次的投影水位。 */
    private long watermark;

    /** 判断事件是否已进入查询库 Inbox。 */
    @Override
    public boolean isProcessed(String eventId) {
        return processedEventIds.contains(eventId);
    }

    /** 幂等记录已处理事件并推进投影水位。 */
    @Override
    public void markProcessed(String eventId) {
        if (processedEventIds.add(eventId)) {
            watermark++;
        }
    }

    /** 按采购订单标识查找执行视图。 */
    @Override
    public Optional<PurchaseOrderExecutionView> findByPurchaseOrderId(String purchaseOrderId) {
        return Optional.ofNullable(byId.get(purchaseOrderId));
    }

    /** 按采购订单号索引查找执行视图。 */
    @Override
    public Optional<PurchaseOrderExecutionView> findByOrderNo(String orderNo) {
        return Optional.ofNullable(idByOrderNo.get(orderNo)).map(byId::get);
    }

    /** 保存执行视图并维护订单号索引。 */
    @Override
    public void save(PurchaseOrderExecutionView view) {
        byId.put(view.purchaseOrderId(), view);
        idByOrderNo.put(view.orderNo(), view.purchaseOrderId());
    }

    /** 建立指定类型业务引用到采购订单的映射。 */
    @Override
    public void bindReference(String referenceType, String referenceId, String purchaseOrderId) {
        references.put(referenceType + ":" + referenceId, purchaseOrderId);
    }

    /** 解析指定类型业务引用所属的采购订单。 */
    @Override
    public Optional<String> resolveReference(String referenceType, String referenceId) {
        return Optional.ofNullable(references.get(referenceType + ":" + referenceId));
    }

    /** 按关联键暂存尚不能归属采购订单的事件。 */
    @Override
    public void defer(String correlationId, DomainEvent event) {
        deferred.computeIfAbsent(correlationId, ignored -> new ArrayList<>()).add(event);
    }

    /** 取出并删除关联键下的全部延迟事件。 */
    @Override
    public List<DomainEvent> takeDeferred(String correlationId) {
        List<DomainEvent> events = deferred.remove(correlationId);
        return events == null ? List.of() : List.copyOf(events);
    }

    /** 在内存视图中执行组合筛选、排序和受限分页。 */
    @Override
    public List<PurchaseOrderExecutionView> search(QueryCriteria criteria) {
        int offset = Math.max(0, criteria.offset());
        int limit = criteria.limit() <= 0 ? 20 : Math.min(criteria.limit(), 200);
        return byId.values().stream()
                .filter(view -> {
                    var snapshot = view.snapshot();
                    return matches(criteria.buyerOrganizationId(), snapshot.buyerOrganizationId())
                            && matches(criteria.supplierCode(), snapshot.supplierCode())
                            && matches(criteria.orderStatus(), snapshot.orderStatus())
                            && matches(criteria.overallStage(), snapshot.overallStage());
                })
                .sorted(Comparator.comparing(PurchaseOrderExecutionView::orderNo))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    /** 返回当前投影水位。 */
    @Override
    public long projectionWatermark() {
        return watermark;
    }

    /** 空筛选值视为不限制，否则要求完全匹配。 */
    private boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equals(actual);
    }
}
