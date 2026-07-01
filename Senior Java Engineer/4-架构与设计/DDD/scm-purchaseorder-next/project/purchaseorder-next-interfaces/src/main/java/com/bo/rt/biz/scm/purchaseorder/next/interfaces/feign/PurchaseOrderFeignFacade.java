package com.bo.rt.biz.scm.purchaseorder.next.interfaces.feign;

import com.bo.rt.biz.scm.purchaseorder.next.api.PurchaseOrderFacade;
import com.bo.rt.biz.scm.purchaseorder.next.api.dto.CreatePurchaseOrderRequest;
import com.bo.rt.biz.scm.purchaseorder.next.api.dto.PurchaseOrderResponse;

/**
 * 采购订单 Feign 入口适配器。
 *
 * <p>负责兼容旧接口路径或暴露新接口契约，并把外部请求转换为应用层命令。</p>
 */
public class PurchaseOrderFeignFacade implements PurchaseOrderFacade {

    /**
     * 创建采购订单草稿。
     *
     * @param request 创建采购订单的外部请求参数
     * @return 创建后的采购订单摘要
     */
    @Override
    public PurchaseOrderResponse createDraft(CreatePurchaseOrderRequest request) {
        throw new UnsupportedOperationException("骨架工程仅表达 Feign 入口边界，待补充协议转换实现。");
    }

    /**
     * 提交采购订单。
     *
     * @param purchaseOrderCode 采购订单号
     * @return 提交后的采购订单摘要
     */
    @Override
    public PurchaseOrderResponse submit(String purchaseOrderCode) {
        throw new UnsupportedOperationException("骨架工程仅表达 Feign 入口边界，待补充协议转换实现。");
    }

    /**
     * 查询采购订单摘要。
     *
     * @param purchaseOrderCode 采购订单号
     * @return 采购订单摘要
     */
    @Override
    public PurchaseOrderResponse getByCode(String purchaseOrderCode) {
        throw new UnsupportedOperationException("骨架工程仅表达 Feign 入口边界，待补充协议转换实现。");
    }
}
