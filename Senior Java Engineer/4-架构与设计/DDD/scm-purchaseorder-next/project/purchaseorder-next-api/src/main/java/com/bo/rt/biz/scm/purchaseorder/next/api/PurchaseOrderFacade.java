package com.bo.rt.biz.scm.purchaseorder.next.api;

import com.bo.rt.biz.scm.purchaseorder.next.api.dto.CreatePurchaseOrderRequest;
import com.bo.rt.biz.scm.purchaseorder.next.api.dto.PurchaseOrderResponse;

/**
 * 采购订单对外契约。
 *
 * <p>该接口用于承接旧 Feign/API 协议兼容层或未来 v2 协议，不承载领域规则。</p>
 */
public interface PurchaseOrderFacade {

    /**
     * 创建采购订单草稿。
     *
     * @param request 创建采购订单的外部请求参数
     * @return 创建后的采购订单摘要
     */
    PurchaseOrderResponse createDraft(CreatePurchaseOrderRequest request);

    /**
     * 提交采购订单进入审批或后续履约流程。
     *
     * @param purchaseOrderCode 采购订单号
     * @return 提交后的采购订单摘要
     */
    PurchaseOrderResponse submit(String purchaseOrderCode);

    /**
     * 根据采购订单号查询采购订单摘要。
     *
     * @param purchaseOrderCode 采购订单号
     * @return 采购订单摘要；不存在时由接口适配层转换为统一错误
     */
    PurchaseOrderResponse getByCode(String purchaseOrderCode);
}
