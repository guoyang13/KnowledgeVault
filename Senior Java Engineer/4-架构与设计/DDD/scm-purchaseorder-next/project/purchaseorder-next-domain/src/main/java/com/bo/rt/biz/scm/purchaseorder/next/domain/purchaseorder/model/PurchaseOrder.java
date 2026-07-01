package com.bo.rt.biz.scm.purchaseorder.next.domain.purchaseorder.model;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Destination;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.SupplierRef;
import java.util.List;

/**
 * 采购订单聚合根。
 *
 * <p>该聚合负责维护采购订单生命周期和强一致性业务规则，不依赖数据库、Excel、Feign 或 MQ。</p>
 */
public class PurchaseOrder {

    private String purchaseOrderCode;
    private SupplierRef supplier;
    private Destination destination;
    private PurchaseOrderStatus status;
    private List<PurchaseSubOrder> subOrders;

    /**
     * 提交采购订单。
     *
     * <p>该行为应校验当前状态是否允许提交，并产生采购订单已提交领域事件。</p>
     */
    public void submit() {
        throw new UnsupportedOperationException("骨架工程仅表达领域行为，待补充状态流转规则。");
    }

    /**
     * 审批通过采购订单。
     *
     * <p>该行为应校验审批前置状态，并产生采购订单已审批领域事件。</p>
     */
    public void approve() {
        throw new UnsupportedOperationException("骨架工程仅表达领域行为，待补充状态流转规则。");
    }

    /**
     * 取消采购订单。
     *
     * <p>该行为应拒绝取消已完成入库或其他不可逆状态的订单。</p>
     */
    public void cancel() {
        throw new UnsupportedOperationException("骨架工程仅表达领域行为，待补充状态流转规则。");
    }

    /**
     * 确认供应商发货。
     *
     * <p>该行为应校验订单已经审批通过，并产生供应商已发货领域事件。</p>
     */
    public void confirmSupplierDelivery() {
        throw new UnsupportedOperationException("骨架工程仅表达领域行为，待补充履约规则。");
    }

    /**
     * 确认目的仓入库。
     *
     * <p>该行为应校验收货链路完整性，并产生目的仓已入库领域事件。</p>
     */
    public void confirmDestinationInbound() {
        throw new UnsupportedOperationException("骨架工程仅表达领域行为，待补充入库规则。");
    }

    /**
     * 获取采购订单号。
     *
     * @return 采购订单号
     */
    public String purchaseOrderCode() {
        return purchaseOrderCode;
    }

    /**
     * 获取采购订单状态。
     *
     * @return 采购订单状态
     */
    public PurchaseOrderStatus status() {
        return status;
    }
}
