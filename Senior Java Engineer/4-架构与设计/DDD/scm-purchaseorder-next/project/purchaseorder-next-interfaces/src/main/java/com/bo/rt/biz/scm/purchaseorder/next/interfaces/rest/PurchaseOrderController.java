package com.bo.rt.biz.scm.purchaseorder.next.interfaces.rest;

import com.bo.rt.biz.scm.purchaseorder.next.api.dto.CreatePurchaseOrderRequest;
import com.bo.rt.biz.scm.purchaseorder.next.api.dto.PurchaseOrderResponse;

/**
 * 采购订单 REST 入口。
 *
 * <p>该类当前只表达入口位置，后续可按框架规范补充注解和统一响应包装。</p>
 */
public class PurchaseOrderController {

    /**
     * 创建采购订单草稿。
     *
     * @param request 创建采购订单请求
     * @return 采购订单响应
     */
    public PurchaseOrderResponse createDraft(CreatePurchaseOrderRequest request) {
        throw new UnsupportedOperationException("骨架工程仅表达 REST 入口边界，待补充协议转换实现。");
    }
}
