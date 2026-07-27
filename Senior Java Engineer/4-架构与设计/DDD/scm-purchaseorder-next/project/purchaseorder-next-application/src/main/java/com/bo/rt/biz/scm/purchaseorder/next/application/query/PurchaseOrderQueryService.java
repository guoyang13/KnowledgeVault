package com.bo.rt.biz.scm.purchaseorder.next.application.query;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 查询用例只访问读模型。读己之写（Read Your Writes）通过投影水位等待实现。
 */
public class PurchaseOrderQueryService {

    /** 采购订单查询库端口。 */
    private final PurchaseOrderViewStore store;

    /** 注入查询库端口。 */
    public PurchaseOrderQueryService(PurchaseOrderViewStore store) {
        this.store = store;
    }

    /**
     * 按订单号获取详情，并在限定时间内等待投影达到调用方要求的最低水位。
     */
    public PurchaseOrderExecutionView.DetailSnapshot getByOrderNo(
            String orderNo,
            long minimumWatermark,
            Duration waitTimeout
    ) {
        waitForWatermark(minimumWatermark, waitTimeout);
        return store.findByOrderNo(orderNo)
                .map(PurchaseOrderExecutionView::snapshot)
                .orElseThrow(() -> new IllegalArgumentException("采购订单读模型不存在"));
    }

    /** 按组合条件分页搜索采购订单执行视图。 */
    public List<PurchaseOrderExecutionView.DetailSnapshot> search(
            PurchaseOrderViewStore.QueryCriteria criteria
    ) {
        return store.search(criteria).stream()
                .map(PurchaseOrderExecutionView::snapshot)
                .toList();
    }

    /** 在超时前等待查询库投影水位追上写请求返回的水位。 */
    private void waitForWatermark(long minimumWatermark, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (store.projectionWatermark() < minimumWatermark && Instant.now().isBefore(deadline)) {
            Thread.onSpinWait();
        }
    }
}
