package com.bo.rt.biz.scm.purchaseorder.next.domain.requisition.model;

/**
 * 采购需求聚合根。
 *
 * <p>用于承接 PR 概念，负责采购需求确认、取消以及转采购订单前的业务规则。</p>
 */
public class PurchaseRequisition {

    private String requisitionCode;

    /**
     * 确认采购需求可以转采购订单。
     *
     * <p>该行为应校验供应商确认、商品明细和需求状态。</p>
     */
    public void confirmReadyToTransfer() {
        throw new UnsupportedOperationException("骨架工程仅表达采购需求行为，待补充业务规则。");
    }

    /**
     * 获取采购需求单号。
     *
     * @return 采购需求单号
     */
    public String requisitionCode() {
        return requisitionCode;
    }
}
