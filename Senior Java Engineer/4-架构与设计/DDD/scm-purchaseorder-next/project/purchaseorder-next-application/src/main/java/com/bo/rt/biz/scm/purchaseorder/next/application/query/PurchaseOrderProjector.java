package com.bo.rt.biz.scm.purchaseorder.next.application.query;

import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.event.CollaborationEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.event.OrderingEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.event.QualityEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.event.SettlementEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;
import com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.event.SupplierFulfillmentEvents;
import java.util.Optional;

/**
 * 将多个上下文的事件投影为单一采购执行视图。
 */
public class PurchaseOrderProjector {

    /** 保存投影、Inbox、水位、引用映射和延迟事件的查询库端口。 */
    private final PurchaseOrderViewStore store;

    /** 注入查询库端口。 */
    public PurchaseOrderProjector(PurchaseOrderViewStore store) {
        this.store = store;
    }

    /**
     * 幂等投影一个受支持的领域事件。
     *
     * <p>依赖映射尚未建立的事件会暂存，待上游创建事件到达后重新投影。</p>
     */
    public void project(DomainEvent event) {
        if (!supports(event) || store.isProcessed(event.eventId())) {
            return;
        }

        if (event instanceof OrderingEvents.PurchaseOrderEffective effective) {
            PurchaseOrderExecutionView view = store.findByPurchaseOrderId(effective.aggregateId())
                    .orElseGet(() -> PurchaseOrderExecutionView.from(effective));
            store.save(view);
            store.markProcessed(event.eventId());
            store.takeDeferred(effective.aggregateId()).forEach(this::project);
            return;
        }

        Optional<String> purchaseOrderId = resolvePurchaseOrderId(event);
        if (purchaseOrderId.isEmpty()) {
            store.defer(correlationId(event), event);
            return;
        }
        PurchaseOrderExecutionView view = store.findByPurchaseOrderId(purchaseOrderId.get())
                .orElse(null);
        if (view == null) {
            store.defer(purchaseOrderId.get(), event);
            return;
        }

        bindReferences(event, purchaseOrderId.get());
        if (view.acceptVersion(event)) {
            apply(view, event);
            store.save(view);
        }
        store.markProcessed(event.eventId());
        if (bindsReference(event)) {
            store.takeDeferred(event.aggregateId()).forEach(this::project);
        }
    }

    /** 将已解析到采购订单的事件内容应用到详情视图。 */
    private void apply(PurchaseOrderExecutionView view, DomainEvent event) {
        if (event instanceof OrderingEvents.PurchaseOrderSubmitted) {
            view.updateOrder("APPROVAL_PENDING", "PROCESSING");
        } else if (event instanceof OrderingEvents.PurchaseOrderRejected) {
            view.updateOrder("REJECTED", "REJECTED");
        } else if (event instanceof OrderingEvents.PurchaseOrderCancelled) {
            view.updateOrder("CANCELLED", null);
        } else if (event instanceof OrderingEvents.PurchaseOrderCompleted) {
            view.updateOrder("COMPLETED", null);
        } else if (event instanceof OrderingEvents.PurchaseOrderTerminated) {
            view.updateOrder("TERMINATED", null);
        } else if (event instanceof SupplierFulfillmentEvents.SupplierFulfillmentOrderCreated) {
            view.updateFulfillment("WAITING_CONFIRMATION");
        } else if (event instanceof SupplierFulfillmentEvents.SupplierCommitmentConfirmed) {
            view.updateFulfillment("CONFIRMED");
        } else if (event instanceof SupplierFulfillmentEvents.SupplierShipmentDispatched dispatched) {
            view.recordShipment(dispatched.shipmentNo());
        } else if (event instanceof SupplierFulfillmentEvents.SupplierFulfillmentCompleted) {
            view.updateFulfillment("COMPLETED");
        } else if (event instanceof QualityEvents.QualityInspectionStarted) {
            view.recordQualityStarted();
        } else if (event instanceof QualityEvents.QualityInspectionCompleted) {
            view.recordQualityCompleted();
        } else if (event instanceof QualityEvents.GoodsAcceptedForSettlement accepted) {
            accepted.lines().forEach(line ->
                    view.addAcceptedQuantity(line.purchaseOrderLineId(), line.acceptedQuantity())
            );
        } else if (event instanceof QualityEvents.GoodsRejectedByQuality rejected) {
            rejected.lines().forEach(line ->
                    view.addRejectedQuantity(line.purchaseOrderLineId(), line.rejectedQuantity())
            );
        } else if (event instanceof SettlementEvents.ProcurementSettlementInitialized) {
            view.updateSettlement("WAITING_FOR_BASIS", 0, null);
        } else if (event instanceof SettlementEvents.SettlementCalculated calculated) {
            view.updateSettlement(
                    "CALCULATED", calculated.revisionNo(), calculated.totalPayable()
            );
        } else if (event instanceof SettlementEvents.SettlementConfirmed confirmed) {
            view.updateSettlement(
                    "CONFIRMED", confirmed.revisionNo(), confirmed.totalPayable()
            );
        } else if (event instanceof SettlementEvents.SettlementRecalculationRequired) {
            view.updateSettlement("RECALCULATION_REQUIRED", 0, null);
        } else if (event instanceof SettlementEvents.SettlementSubmittedToAp submitted) {
            view.updateSettlement("SUBMITTING_TO_AP", submitted.revisionNo(), null);
        } else if (event instanceof SettlementEvents.SettlementAcceptedByAp accepted) {
            view.updateSettlement("AP_ACCEPTED", accepted.revisionNo(), null);
        } else if (event instanceof SettlementEvents.SettlementRejectedByAp rejected) {
            view.updateSettlement("AP_REJECTED", rejected.revisionNo(), null);
        } else if (event instanceof CollaborationEvents.ExecutionRequirementPlanCreated plan) {
            int required = (int) plan.requirements().stream()
                    .filter(item -> !"NOT_REQUIRED".equals(item.applicability()))
                    .count();
            view.registerRequirementPlan(required);
        } else if (event instanceof CollaborationEvents.ExecutionTaskCreated) {
            view.taskCreated(event.aggregateId());
        } else if (event instanceof CollaborationEvents.ExecutionTaskCompleted
                || event instanceof CollaborationEvents.ExecutionTaskWaived) {
            view.taskFinished(event.aggregateId());
        } else if (event instanceof CollaborationEvents.ExecutionExceptionOpened opened) {
            view.exceptionOpened(opened.severity());
        } else if (event instanceof CollaborationEvents.ExecutionExceptionResolved) {
            view.exceptionResolved();
        }
    }

    /** 从事件字段或查询库引用映射解析其所属采购订单。 */
    private Optional<String> resolvePurchaseOrderId(DomainEvent event) {
        if (event instanceof OrderingEvents.PurchaseOrderSubmitted
                || event instanceof OrderingEvents.PurchaseOrderRejected
                || event instanceof OrderingEvents.PurchaseOrderCancelled
                || event instanceof OrderingEvents.PurchaseOrderTerminated
                || event instanceof OrderingEvents.PurchaseOrderCompleted) {
            return Optional.of(event.aggregateId());
        }
        if (event instanceof SupplierFulfillmentEvents.SupplierFulfillmentOrderCreated created) {
            return Optional.of(created.purchaseOrderId());
        }
        if (event instanceof SupplierFulfillmentEvents.SupplierCommitmentConfirmed confirmed) {
            return Optional.of(confirmed.purchaseOrderId());
        }
        if (event instanceof SupplierFulfillmentEvents.SupplierFulfillmentCompleted completed) {
            return Optional.of(completed.purchaseOrderId());
        }
        if (event instanceof SupplierFulfillmentEvents.SupplierShipmentDispatched dispatched) {
            return store.resolveReference("FULFILLMENT_ORDER", dispatched.fulfillmentOrderId());
        }
        if (event instanceof QualityEvents.QualityInspectionRequired required) {
            return Optional.of(required.purchaseOrderId());
        }
        if (event instanceof QualityEvents.QualityInspectionStarted) {
            return store.resolveReference("QUALITY_INSPECTION", event.aggregateId());
        }
        if (event instanceof QualityEvents.QualityInspectionCompleted completed) {
            return Optional.of(completed.purchaseOrderId());
        }
        if (event instanceof QualityEvents.GoodsAcceptedForSettlement accepted) {
            return Optional.of(accepted.purchaseOrderId());
        }
        if (event instanceof QualityEvents.GoodsRejectedByQuality rejected) {
            return Optional.of(rejected.purchaseOrderId());
        }
        if (event instanceof SettlementEvents.ProcurementSettlementInitialized initialized) {
            return Optional.of(initialized.purchaseOrderId());
        }
        if (event instanceof SettlementEvents.SettlementCalculated calculated) {
            return Optional.of(calculated.purchaseOrderId());
        }
        if (event instanceof SettlementEvents.SettlementConfirmed confirmed) {
            return Optional.of(confirmed.purchaseOrderId());
        }
        if (event instanceof SettlementEvents.SettlementRecalculationRequired required) {
            return Optional.of(required.purchaseOrderId());
        }
        if (event instanceof SettlementEvents.SettlementSubmittedToAp submitted) {
            return Optional.of(submitted.purchaseOrderId());
        }
        if (event instanceof SettlementEvents.SettlementAcceptedByAp accepted) {
            return Optional.of(accepted.purchaseOrderId());
        }
        if (event instanceof SettlementEvents.SettlementRejectedByAp rejected) {
            return Optional.of(rejected.purchaseOrderId());
        }
        if (event instanceof CollaborationEvents.ExecutionRequirementPlanCreated plan) {
            return Optional.of(plan.purchaseOrderId());
        }
        if (event instanceof CollaborationEvents.ExecutionTaskCreated task) {
            return Optional.of(task.purchaseOrderId());
        }
        if (event instanceof CollaborationEvents.ExecutionTaskCompleted task) {
            return Optional.of(task.purchaseOrderId());
        }
        if (event instanceof CollaborationEvents.ExecutionTaskWaived task) {
            return Optional.of(task.purchaseOrderId());
        }
        if (event instanceof CollaborationEvents.ExecutionExceptionOpened exception) {
            return Optional.of(exception.purchaseOrderId());
        }
        if (event instanceof CollaborationEvents.ExecutionExceptionResolved exception) {
            return Optional.of(exception.purchaseOrderId());
        }
        return Optional.empty();
    }

    /** 在创建类事件到达时建立子聚合到采购订单的关联映射。 */
    private void bindReferences(DomainEvent event, String purchaseOrderId) {
        if (event instanceof SupplierFulfillmentEvents.SupplierFulfillmentOrderCreated) {
            store.bindReference("FULFILLMENT_ORDER", event.aggregateId(), purchaseOrderId);
        } else if (event instanceof QualityEvents.QualityInspectionRequired) {
            store.bindReference("QUALITY_INSPECTION", event.aggregateId(), purchaseOrderId);
        } else if (event instanceof SettlementEvents.ProcurementSettlementInitialized) {
            store.bindReference("SETTLEMENT", event.aggregateId(), purchaseOrderId);
        }
    }

    /** 判断事件投影后是否建立了可唤醒延迟事件的引用映射。 */
    private boolean bindsReference(DomainEvent event) {
        return event instanceof SupplierFulfillmentEvents.SupplierFulfillmentOrderCreated
                || event instanceof QualityEvents.QualityInspectionRequired
                || event instanceof SettlementEvents.ProcurementSettlementInitialized;
    }

    /** 返回无法立即解析采购订单时使用的延迟关联键。 */
    private String correlationId(DomainEvent event) {
        if (event instanceof SupplierFulfillmentEvents.SupplierShipmentDispatched dispatched) {
            return dispatched.fulfillmentOrderId();
        }
        return event.aggregateId();
    }

    /** 判断当前投影器是否消费该事件类型。 */
    private boolean supports(DomainEvent event) {
        return event instanceof OrderingEvents.PurchaseOrderEffective
                || event instanceof OrderingEvents.PurchaseOrderSubmitted
                || event instanceof OrderingEvents.PurchaseOrderRejected
                || event instanceof OrderingEvents.PurchaseOrderCancelled
                || event instanceof OrderingEvents.PurchaseOrderTerminated
                || event instanceof OrderingEvents.PurchaseOrderCompleted
                || event instanceof SupplierFulfillmentEvents.SupplierFulfillmentOrderCreated
                || event instanceof SupplierFulfillmentEvents.SupplierCommitmentConfirmed
                || event instanceof SupplierFulfillmentEvents.SupplierShipmentDispatched
                || event instanceof SupplierFulfillmentEvents.SupplierFulfillmentCompleted
                || event instanceof QualityEvents.QualityInspectionRequired
                || event instanceof QualityEvents.QualityInspectionStarted
                || event instanceof QualityEvents.QualityInspectionCompleted
                || event instanceof QualityEvents.GoodsAcceptedForSettlement
                || event instanceof QualityEvents.GoodsRejectedByQuality
                || event instanceof SettlementEvents.ProcurementSettlementInitialized
                || event instanceof SettlementEvents.SettlementRecalculationRequired
                || event instanceof SettlementEvents.SettlementCalculated
                || event instanceof SettlementEvents.SettlementConfirmed
                || event instanceof SettlementEvents.SettlementSubmittedToAp
                || event instanceof SettlementEvents.SettlementAcceptedByAp
                || event instanceof SettlementEvents.SettlementRejectedByAp
                || event instanceof CollaborationEvents.ExecutionRequirementPlanCreated
                || event instanceof CollaborationEvents.ExecutionTaskCreated
                || event instanceof CollaborationEvents.ExecutionTaskCompleted
                || event instanceof CollaborationEvents.ExecutionTaskWaived
                || event instanceof CollaborationEvents.ExecutionExceptionOpened
                || event instanceof CollaborationEvents.ExecutionExceptionResolved;
    }
}
