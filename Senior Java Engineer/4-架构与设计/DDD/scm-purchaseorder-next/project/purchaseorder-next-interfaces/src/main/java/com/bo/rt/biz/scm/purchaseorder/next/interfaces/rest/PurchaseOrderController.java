package com.bo.rt.biz.scm.purchaseorder.next.interfaces.rest;

import com.bo.rt.biz.scm.purchaseorder.next.api.PurchaseOrderFacade;
import com.bo.rt.biz.scm.purchaseorder.next.api.dto.CreatePurchaseOrderRequest;
import com.bo.rt.biz.scm.purchaseorder.next.api.dto.PurchaseOrderResponse;

/**
 * 采购订单 REST 入口。
 *
 * <p>该类当前只表达入口位置，后续可按框架规范补充注解和统一响应包装。</p>
 */
public class PurchaseOrderController {

    /** 采购订单对外契约。 */
    private final PurchaseOrderFacade facade;

    /** 注入采购订单门面。 */
    public PurchaseOrderController(PurchaseOrderFacade facade) {
        this.facade = facade;
    }

    /**
     * 创建采购订单草稿。
     *
     * @param request 创建采购订单请求
     * @return 采购订单响应
     */
    public PurchaseOrderResponse createDraft(CreatePurchaseOrderRequest request) {
        return facade.createDraft(request);
    }
}
