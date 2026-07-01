package com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port;

/**
 * 审批系统端口。
 *
 * <p>应用层通过该端口发起或查询审批流程，基础设施层负责适配真实审批服务。</p>
 */
public interface ApprovalWorkflowPort {

    /**
     * 为采购订单发起审批流程。
     *
     * @param purchaseOrderCode 采购订单号
     */
    void startPurchaseOrderApproval(String purchaseOrderCode);
}
