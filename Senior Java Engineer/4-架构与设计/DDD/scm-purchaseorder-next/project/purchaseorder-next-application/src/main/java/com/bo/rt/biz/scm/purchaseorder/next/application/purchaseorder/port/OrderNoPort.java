package com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port;

/**
 * 单号生成端口。
 *
 * <p>屏蔽旧单号服务或新单号服务的实现差异。</p>
 */
public interface OrderNoPort {

    /**
     * 生成新的采购订单号。
     *
     * @return 采购订单号
     */
    String nextPurchaseOrderCode();
}
