package com.bo.rt.biz.scm.purchaseorder.next.domain.purchaseorder.model;

import java.util.List;

/**
 * 采购子单实体。
 *
 * <p>用于承接旧 PSO 概念，后续可以按国家、仓库、供应商履约批次继续细化。</p>
 */
public class PurchaseSubOrder {

    private String purchaseSubOrderCode;
    private List<PurchaseOrderLine> lines;

    /**
     * 获取采购子单号。
     *
     * @return 采购子单号
     */
    public String purchaseSubOrderCode() {
        return purchaseSubOrderCode;
    }

    /**
     * 获取采购子单明细。
     *
     * @return 采购子单明细列表
     */
    public List<PurchaseOrderLine> lines() {
        return lines;
    }
}
