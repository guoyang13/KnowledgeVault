package com.bo.rt.biz.scm.purchaseorder.next.api.dto;

import java.util.List;

/**
 * 创建采购订单的外部请求对象。
 *
 * @param commandId 幂等命令 ID
 * @param reservationId PR 转单预占 ID
 * @param buyerOrganizationId 采购主体
 * @param destinationCode 仓、店或客户目的地编码
 * @param deliveryMethod 交付方式：SUPPLIER_DIRECT 或 COLLECTION_AND_TRANSFER
 * @param transitNodeCode 中转节点编码；供应商直送时为空
 * @param incoterm 贸易条款
 * @param supplierArrangesTransport 是否由供应商安排运输
 * @param allowOverDelivery 是否允许超量交付
 * @param currency 结算币种
 * @param lines 订单行商业条款，SKU 与数量从预占读取
 */
public record CreatePurchaseOrderRequest(
        String commandId,
        String reservationId,
        String buyerOrganizationId,
        String destinationCode,
        String deliveryMethod,
        String transitNodeCode,
        String incoterm,
        boolean supplierArrangesTransport,
        boolean allowOverDelivery,
        String currency,
        List<CreatePurchaseOrderLineRequest> lines
) {

    /** 固化外部请求中的订单行列表。 */
    public CreatePurchaseOrderRequest {
        lines = List.copyOf(lines);
    }
}
