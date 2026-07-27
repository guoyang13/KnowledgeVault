package com.bo.rt.biz.scm.purchaseorder.next.application.integration;

import com.bo.rt.biz.scm.purchaseorder.next.application.fulfillmentcollaboration.FulfillmentCollaborationApplicationService;
import com.bo.rt.biz.scm.purchaseorder.next.application.settlement.SettlementApplicationService;
import com.bo.rt.biz.scm.purchaseorder.next.application.supplierfulfillment.SupplierFulfillmentApplicationService;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.event.OrderingEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.event.QualityEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;

/**
 * 展示跨上下文事件路由。生产实现由 MQ consumer + Inbox 幂等替代这个同步分发器。
 */
public class ProcurementEventCoordinator {

    /** 采购订单生效事件的供应商履约订阅方。 */
    private final SupplierFulfillmentApplicationService supplierFulfillmentService;
    /** 采购订单生效事件的履约协同订阅方。 */
    private final FulfillmentCollaborationApplicationService collaborationService;
    /** 订单和质检事件的采购结算订阅方。 */
    private final SettlementApplicationService settlementService;

    /** 注入各限界上下文的事件订阅用例。 */
    public ProcurementEventCoordinator(
            SupplierFulfillmentApplicationService supplierFulfillmentService,
            FulfillmentCollaborationApplicationService collaborationService,
            SettlementApplicationService settlementService
    ) {
        this.supplierFulfillmentService = supplierFulfillmentService;
        this.collaborationService = collaborationService;
        this.settlementService = settlementService;
    }

    /**
     * 按事件实际类型路由到相应上下文。
     *
     * <p>该同步实现只用于展示；生产环境应由独立 Consumer 在各自事务中处理。</p>
     */
    public void handle(DomainEvent event) {
        if (event instanceof OrderingEvents.PurchaseOrderEffective effective) {
            supplierFulfillmentService.onPurchaseOrderEffective(effective);
            collaborationService.onPurchaseOrderEffective(effective);
            settlementService.onPurchaseOrderEffective(effective);
        } else if (event instanceof OrderingEvents.PurchaseOrderPriceAdjusted adjusted) {
            settlementService.onPriceAdjusted(adjusted);
        } else if (event instanceof QualityEvents.GoodsAcceptedForSettlement accepted) {
            settlementService.onGoodsAccepted(accepted);
        }
    }
}
