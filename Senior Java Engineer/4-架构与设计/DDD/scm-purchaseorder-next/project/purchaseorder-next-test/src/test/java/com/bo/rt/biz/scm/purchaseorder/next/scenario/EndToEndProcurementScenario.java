package com.bo.rt.biz.scm.purchaseorder.next.scenario;

import com.bo.rt.biz.scm.purchaseorder.next.application.fulfillmentcollaboration.FulfillmentCollaborationApplicationService;
import com.bo.rt.biz.scm.purchaseorder.next.application.fulfillmentcollaboration.FulfillmentCollaborationApplicationService.RequirementDecision;
import com.bo.rt.biz.scm.purchaseorder.next.application.integration.ProcurementEventCoordinator;
import com.bo.rt.biz.scm.purchaseorder.next.application.ordering.OrderingApplicationService;
import com.bo.rt.biz.scm.purchaseorder.next.application.ordering.OrderingApplicationService.CreateOrderFromRequisition;
import com.bo.rt.biz.scm.purchaseorder.next.application.ordering.OrderingApplicationService.LineCommercialTerms;
import com.bo.rt.biz.scm.purchaseorder.next.application.ordering.OrderingApplicationService.OrderResult;
import com.bo.rt.biz.scm.purchaseorder.next.application.planning.PlanningApplicationService;
import com.bo.rt.biz.scm.purchaseorder.next.application.planning.PlanningApplicationService.ReserveRequisitionQuantities;
import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port.LocationDirectoryPort.LocationProfile;
import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port.SupplierProfilePort.SupplierOrderingProfile;
import com.bo.rt.biz.scm.purchaseorder.next.application.qualityinspection.QualityInspectionApplicationService;
import com.bo.rt.biz.scm.purchaseorder.next.application.qualityinspection.QualityInspectionApplicationService.ReceiptAccepted;
import com.bo.rt.biz.scm.purchaseorder.next.application.qualityinspection.QualityInspectionApplicationService.ReceiptLine;
import com.bo.rt.biz.scm.purchaseorder.next.application.qualityinspection.QualityInspectionApplicationService.RecordInspectionLineResult;
import com.bo.rt.biz.scm.purchaseorder.next.application.query.PurchaseOrderExecutionView.DetailSnapshot;
import com.bo.rt.biz.scm.purchaseorder.next.application.query.PurchaseOrderProjector;
import com.bo.rt.biz.scm.purchaseorder.next.application.settlement.SettlementApplicationService;
import com.bo.rt.biz.scm.purchaseorder.next.application.supplierfulfillment.SupplierFulfillmentApplicationService;
import com.bo.rt.biz.scm.purchaseorder.next.application.supplierfulfillment.SupplierFulfillmentApplicationService.CommitmentInput;
import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model.ExecutionRequirement;
import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model.ExecutionTask;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.PurchaseOrder;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoute;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoutePolicy;
import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.model.PurchaseRequisition;
import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.model.PurchaseRequisition.RequisitionLine;
import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.model.QualityInspectionOrder;
import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.model.QualityInspectionOrder.Defect;
import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.model.QualityInspectionOrder.DefectSeverity;
import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.model.QualityInspectionOrder.InspectionDecision;
import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.model.SampleMatchingCase.Evidence;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.BusinessReference;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Money;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.SkuRef;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.SupplierRef;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Destination.DestinationType;
import com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.model.SupplierFulfillmentOrder;
import com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.model.SupplierShipment;
import com.bo.rt.biz.scm.purchaseorder.next.infrastructure.event.OutboxDomainEventPublisher;
import com.bo.rt.biz.scm.purchaseorder.next.infrastructure.id.UuidIdentityGenerator;
import com.bo.rt.biz.scm.purchaseorder.next.infrastructure.persistence.memory.InMemoryRepositories.FulfillmentStore;
import com.bo.rt.biz.scm.purchaseorder.next.infrastructure.persistence.memory.InMemoryRepositories.InspectionStore;
import com.bo.rt.biz.scm.purchaseorder.next.infrastructure.persistence.memory.InMemoryRepositories.RequisitionStore;
import com.bo.rt.biz.scm.purchaseorder.next.infrastructure.persistence.memory.InMemoryRepositories.RequirementPlanStore;
import com.bo.rt.biz.scm.purchaseorder.next.infrastructure.persistence.memory.InMemoryRepositories.ReservationStore;
import com.bo.rt.biz.scm.purchaseorder.next.infrastructure.persistence.memory.InMemoryRepositories.SettlementStore;
import com.bo.rt.biz.scm.purchaseorder.next.infrastructure.persistence.memory.InMemoryRepositories.ShipmentStore;
import com.bo.rt.biz.scm.purchaseorder.next.infrastructure.persistence.memory.InMemoryRepositories.TaskStore;
import com.bo.rt.biz.scm.purchaseorder.next.infrastructure.persistence.purchaseorder.PurchaseOrderRepositoryAdapter;
import com.bo.rt.biz.scm.purchaseorder.next.infrastructure.query.InMemoryPurchaseOrderViewStore;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 可执行伪代码：用内存适配器验证七个上下文的关键事件链。
 */
public class EndToEndProcurementScenario {

    public static void main(String[] args) {
        new EndToEndProcurementScenario().run();
    }

    public void run() {
        var ids = new UuidIdentityGenerator();
        var outbox = new OutboxDomainEventPublisher();
        var requisitions = new RequisitionStore();
        var reservations = new ReservationStore();
        var orders = new PurchaseOrderRepositoryAdapter();
        var fulfillments = new FulfillmentStore();
        var shipments = new ShipmentStore();
        var inspections = new InspectionStore();
        var settlements = new SettlementStore();
        var plans = new RequirementPlanStore();
        var tasks = new TaskStore();
        var views = new InMemoryPurchaseOrderViewStore();
        var projector = new PurchaseOrderProjector(views);

        var planning = new PlanningApplicationService(
                requisitions, reservations, outbox, ids, Clock.systemUTC()
        );
        var ordering = new OrderingApplicationService(
                orders,
                requisitions,
                reservations,
                supplierCode -> new SupplierOrderingProfile(supplierCode, "CN", true),
                locationCode -> switch (locationCode) {
                    case "WH-SHANGHAI" -> new LocationProfile(
                            locationCode,
                            DestinationType.WAREHOUSE,
                            "CN",
                            true,
                            false
                    );
                    default -> null;
                },
                new ProcurementRoutePolicy(),
                () -> "PO-20260727-001",
                (orderNo, version) -> "APR-" + orderNo + "-" + version,
                ids,
                outbox
        );
        var collaboration = new FulfillmentCollaborationApplicationService(
                plans,
                tasks,
                event -> {
                    boolean containerRequired = "CROSS_BORDER_TRANSIT"
                            .equals(event.route().routeType());
                    return List.of(
                            new RequirementDecision(
                                    ExecutionRequirement.RequirementType.SAMPLE_IMAGE_UPLOAD,
                                    ExecutionRequirement.ReferenceScope.ORDER,
                                    event.aggregateId(),
                                    ExecutionRequirement.Applicability.REQUIRED,
                                    "该供应商首次采购，发货前必须上传商品样图",
                                    "REQ-POLICY-2026.07",
                                    ExecutionRequirement.Milestone.DISPATCH
                            ),
                            new RequirementDecision(
                                    ExecutionRequirement.RequirementType.CONTAINER_LOADING,
                                    ExecutionRequirement.ReferenceScope.ORDER,
                                    event.aggregateId(),
                                    containerRequired
                                            ? ExecutionRequirement.Applicability.REQUIRED
                                            : ExecutionRequirement.Applicability.NOT_REQUIRED,
                                    "采购路线为 " + event.route().routeType(),
                                    "REQ-POLICY-2026.07",
                                    containerRequired
                                            ? ExecutionRequirement.Milestone.DISPATCH
                                            : null
                            )
                    );
                },
                ids,
                outbox,
                Clock.systemUTC()
        );
        var supplierFulfillment = new SupplierFulfillmentApplicationService(
                fulfillments, shipments, collaboration, ids, outbox
        );
        var quality = new QualityInspectionApplicationService(inspections, ids, outbox);
        var settlement = new SettlementApplicationService(settlements, ids, outbox);
        var coordinator = new ProcurementEventCoordinator(
                supplierFulfillment, collaboration, settlement
        );

        String requisitionId = "PR-ID-001";
        String requisitionLineId = "PR-LINE-001";
        PurchaseRequisition requisition = PurchaseRequisition.create(
                requisitionId,
                "PR-20260727-001",
                new SupplierRef("SUPPLIER-001"),
                List.of(new RequisitionLine(
                        requisitionLineId,
                        "DEMAND-LINE-001",
                        new SkuRef("SKU-RED-CUP"),
                        Quantity.pieces(100)
                ))
        );
        requisition.confirmBySupplier(Map.of(requisitionLineId, Quantity.pieces(100)));
        requisitions.save(requisition);
        outbox.publishAll(requisition.pullDomainEvents());

        var reservation = planning.reserve(new ReserveRequisitionQuantities(
                "CMD-TRANSFER-001",
                requisitionId,
                Map.of(requisitionLineId, Quantity.pieces(80)),
                Duration.ofMinutes(15)
        ));
        OrderResult draft = ordering.createFromRequisition(new CreateOrderFromRequisition(
                "CMD-CREATE-PO-001",
                reservation.reservationId(),
                "BUYER-ORG-CN",
                "WH-SHANGHAI",
                ProcurementRoute.DeliveryMethod.SUPPLIER_DIRECT,
                null,
                new PurchaseOrder.DeliveryTerms("DDP", true, false),
                Map.of(requisitionLineId, new LineCommercialTerms(
                        "LEGACY-PSO-001",
                        new Money(new BigDecimal("10.00"), "CNY"),
                        LocalDate.of(2026, 8, 15)
                ))
        ));
        check(
                orders.findById(draft.purchaseOrderId()).orElseThrow().route().routeType()
                        == ProcurementRoute.RouteType.DOMESTIC_DIRECT,
                "采购路线应根据供应商和目的地国家计算为国内直送"
        );
        dispatchOutbox(outbox, coordinator, projector);

        OrderResult submitted = ordering.submit(draft.orderNo(), draft.writeVersion());
        String approvalId = "APR-" + draft.orderNo() + "-" + submitted.writeVersion();
        OrderResult effective = ordering.recordApprovalResult(
                draft.orderNo(),
                approvalId,
                submitted.writeVersion(),
                true,
                null
        );
        dispatchOutbox(outbox, coordinator, projector);

        completeBlockingTasks(tasks, outbox, effective.purchaseOrderId());
        dispatchOutbox(outbox, coordinator, projector);

        SupplierFulfillmentOrder fulfillment = fulfillments
                .findByPurchaseOrderId(effective.purchaseOrderId())
                .orElseThrow();
        String orderLineId = fulfillment.commitments().get(0).purchaseOrderLineId();
        supplierFulfillment.confirmCommitment(
                fulfillment.id(),
                Map.of(orderLineId, new CommitmentInput(
                        Quantity.pieces(80),
                        LocalDate.of(2026, 8, 10)
                ))
        );
        String shipmentId = supplierFulfillment.createShipmentNotice(
                fulfillment.id(),
                "SHIP-20260808-001",
                "FULFILLMENT-UNIT-001",
                List.of(new SupplierShipment.ShipmentLine(
                        orderLineId,
                        Quantity.pieces(50),
                        new SupplierShipment.PackageInfo(5, "52kg", "0.8m3")
                ))
        );
        supplierFulfillment.markShipmentReady(shipmentId);
        supplierFulfillment.dispatchShipment(shipmentId);
        dispatchOutbox(outbox, coordinator, projector);

        String inspectionId = quality.createInspectionFromReceipt(new ReceiptAccepted(
                "QI-20260812-001",
                effective.purchaseOrderId(),
                "FULFILLMENT-UNIT-001",
                "WAREHOUSE_RECEIPT",
                "RECEIPT-20260812-001",
                QualityInspectionOrder.InspectionMode.FULL,
                List.of(new ReceiptLine(orderLineId, Quantity.pieces(50), true))
        ));
        quality.start(inspectionId, "QUALITY-STANDARD-V3");
        quality.recordLineResult(new RecordInspectionLineResult(
                inspectionId,
                orderLineId,
                Quantity.pieces(50),
                Quantity.pieces(45),
                Quantity.pieces(5),
                InspectionDecision.PARTIALLY_ACCEPTED,
                List.of(new Defect(
                        "SURFACE-SCRATCH",
                        DefectSeverity.MAJOR,
                        Quantity.pieces(5),
                        List.of(new Evidence("oss://quality/scratch-001.jpg", "杯体划痕"))
                )),
                null
        ));
        quality.finish(inspectionId);
        dispatchOutbox(outbox, coordinator, projector);

        settlement.recalculateAndConfirm(
                effective.purchaseOrderId(),
                new BigDecimal("0.13"),
                "TAX-CN-2026",
                "FEE-DOMESTIC-V2"
        );
        dispatchOutbox(outbox, coordinator, projector);

        DetailSnapshot detail = views.findByOrderNo(effective.orderNo())
                .orElseThrow()
                .snapshot();
        check("EFFECTIVE".equals(detail.orderStatus()), "PO 应保持商业生效状态");
        check("IN_EXECUTION".equals(detail.fulfillmentStatus()), "供应商履约应处于执行中");
        check("COMPLETED".equals(detail.qualityStatus()), "本批次质检应已完成");
        check("CONFIRMED".equals(detail.settlementStatus()), "结算版本应已确认");
        check(
                Quantity.pieces(45).equals(detail.acceptedQuantities().get(orderLineId)),
                "只有质检接收的 45 件可以结算"
        );
        check(detail.blockingTaskCount() == 0, "发货阻塞任务应已完成");

        System.out.println("Scenario passed: " + detail);
    }

    private void completeBlockingTasks(
            TaskStore tasks,
            OutboxDomainEventPublisher outbox,
            String purchaseOrderId
    ) {
        for (ExecutionTask task : tasks.findActiveByPurchaseOrderId(purchaseOrderId)) {
            task.assign("supplier-user-001");
            task.start();
            task.submit(
                    new BusinessReference("SAMPLE_IMAGE", "SAMPLE-IMG-001"),
                    List.of("oss://supplier/sample-001.jpg")
            );
            task.complete("QUALITY-REVIEW-PASSED");
            tasks.save(task);
            outbox.publishAll(task.pullDomainEvents());
        }
    }

    private void dispatchOutbox(
            OutboxDomainEventPublisher outbox,
            ProcurementEventCoordinator coordinator,
            PurchaseOrderProjector projector
    ) {
        List<DomainEvent> events;
        while (!(events = outbox.drain()).isEmpty()) {
            for (DomainEvent event : events) {
                projector.project(event);
                coordinator.handle(event);
            }
        }
    }

    private void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
