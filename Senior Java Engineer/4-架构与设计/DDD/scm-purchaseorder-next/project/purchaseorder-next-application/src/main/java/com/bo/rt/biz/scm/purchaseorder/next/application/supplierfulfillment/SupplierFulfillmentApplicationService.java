package com.bo.rt.biz.scm.purchaseorder.next.application.supplierfulfillment;

import com.bo.rt.biz.scm.purchaseorder.next.application.shared.port.DomainEventPublisher;
import com.bo.rt.biz.scm.purchaseorder.next.application.shared.port.IdentityGenerator;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.event.OrderingEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.SupplierRef;
import com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.model.SupplierFulfillmentOrder;
import com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.model.SupplierShipment;
import com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.repository.SupplierFulfillmentOrderRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.repository.SupplierShipmentRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 供应商履约用例，协调订单承诺、发运通知和发货动作。
 */
public class SupplierFulfillmentApplicationService {

    /** 供应商履约单聚合仓储。 */
    private final SupplierFulfillmentOrderRepository fulfillmentRepository;
    /** 供应商发运批次聚合仓储。 */
    private final SupplierShipmentRepository shipmentRepository;
    /** 履约协同上下文提供的发货门禁端口。 */
    private final DispatchGatePort dispatchGatePort;
    /** 履约单和发运批次标识生成器。 */
    private final IdentityGenerator identityGenerator;
    /** 随本地事务写入领域事件的发布端口。 */
    private final DomainEventPublisher eventPublisher;

    /** 注入供应商履约用例依赖。 */
    public SupplierFulfillmentApplicationService(
            SupplierFulfillmentOrderRepository fulfillmentRepository,
            SupplierShipmentRepository shipmentRepository,
            DispatchGatePort dispatchGatePort,
            IdentityGenerator identityGenerator,
            DomainEventPublisher eventPublisher
    ) {
        this.fulfillmentRepository = fulfillmentRepository;
        this.shipmentRepository = shipmentRepository;
        this.dispatchGatePort = dispatchGatePort;
        this.identityGenerator = identityGenerator;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Inbox 以 PurchaseOrderEffective.eventId 幂等，仓储再以 purchaseOrderId 唯一兜底。
     */
    public void onPurchaseOrderEffective(OrderingEvents.PurchaseOrderEffective event) {
        if (fulfillmentRepository.findByPurchaseOrderId(event.aggregateId()).isPresent()) {
            return;
        }
        List<SupplierFulfillmentOrder.CommitmentLine> commitments = event.lines().stream()
                .map(line -> new SupplierFulfillmentOrder.CommitmentLine(
                        line.purchaseOrderLineId(),
                        line.orderedQuantity(),
                        Quantity.zero(line.orderedQuantity().unit())
                ))
                .toList();
        SupplierFulfillmentOrder fulfillment = SupplierFulfillmentOrder.create(
                identityGenerator.nextId("supplier-fulfillment-order"),
                event.aggregateId(),
                event.orderNo(),
                new SupplierRef(event.supplierCode()),
                commitments
        );
        fulfillmentRepository.save(fulfillment);
        eventPublisher.publishAll(fulfillment.pullDomainEvents());
    }

    /** 记录供应商对各采购订单行的承诺数量和日期。 */
    public void confirmCommitment(
            String fulfillmentOrderId,
            Map<String, CommitmentInput> commitments
    ) {
        SupplierFulfillmentOrder fulfillment = requireFulfillment(fulfillmentOrderId);
        Map<String, SupplierFulfillmentOrder.CommitmentDecision> decisions = commitments.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new SupplierFulfillmentOrder.CommitmentDecision(
                                entry.getValue().quantity(),
                                entry.getValue().promisedDate()
                        )
                ));
        fulfillment.confirm(decisions);
        fulfillmentRepository.save(fulfillment);
        eventPublisher.publishAll(fulfillment.pullDomainEvents());
    }

    /**
     * 创建发运通知，并校验发运行属于当前履约单且未超过剩余承诺。
     *
     * @return 新建的供应商发运批次标识
     */
    public String createShipmentNotice(
            String fulfillmentOrderId,
            String shipmentNo,
            String fulfillmentUnitId,
            List<SupplierShipment.ShipmentLine> lines
    ) {
        SupplierFulfillmentOrder fulfillment = requireFulfillment(fulfillmentOrderId);
        Map<String, SupplierFulfillmentOrder.CommitmentLine> commitmentByLine =
                fulfillment.commitments().stream().collect(Collectors.toMap(
                        SupplierFulfillmentOrder.CommitmentLine::purchaseOrderLineId,
                        line -> line
                ));
        lines.forEach(line -> {
            SupplierFulfillmentOrder.CommitmentLine commitment =
                    commitmentByLine.get(line.purchaseOrderLineId());
            if (commitment == null
                    || !line.shippedQuantity().lessThanOrEqualTo(commitment.remainingToShip())) {
                throw new IllegalArgumentException("发运行不属于履约单或数量超过剩余承诺");
            }
        });
        SupplierShipment shipment = SupplierShipment.createNotice(
                identityGenerator.nextId("supplier-shipment"),
                shipmentNo,
                fulfillmentOrderId,
                fulfillmentUnitId,
                fulfillment.supplier().supplierCode(),
                lines
        );
        shipmentRepository.save(shipment);
        eventPublisher.publishAll(shipment.pullDomainEvents());
        return shipment.id();
    }

    /** 将发运批次标记为备货完成，等待发货门禁检查。 */
    public void markShipmentReady(String shipmentId) {
        SupplierShipment shipment = requireShipment(shipmentId);
        shipment.markReady();
        shipmentRepository.save(shipment);
        eventPublisher.publishAll(shipment.pullDomainEvents());
    }

    /**
     * 通过协同上下文检查发货门禁后发货，并同步扣减履约单剩余承诺。
     */
    public void dispatchShipment(String shipmentId) {
        SupplierShipment shipment = requireShipment(shipmentId);
        SupplierFulfillmentOrder fulfillment = requireFulfillment(shipment.fulfillmentOrderId());
        boolean allowed = dispatchGatePort.canDispatch(
                fulfillment.purchaseOrderId(),
                shipment.fulfillmentUnitId()
        );
        shipment.dispatch(allowed);
        fulfillment.registerDispatchedShipment(shipment);
        shipmentRepository.save(shipment);
        fulfillmentRepository.save(fulfillment);
        eventPublisher.publishAll(shipment.pullDomainEvents());
        eventPublisher.publishAll(fulfillment.pullDomainEvents());
    }

    /** 加载供应商履约单，不存在时终止用例。 */
    private SupplierFulfillmentOrder requireFulfillment(String id) {
        return fulfillmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("供应商履约单不存在"));
    }

    /** 加载供应商发运批次，不存在时终止用例。 */
    private SupplierShipment requireShipment(String id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("供应商发运批次不存在"));
    }

    /**
     * 一条供应商承诺输入。
     *
     * @param quantity 承诺交付数量
     * @param promisedDate 承诺交付日期
     */
    public record CommitmentInput(Quantity quantity, LocalDate promisedDate) {
    }

    /** 由采购履约协同上下文实现的发货门禁查询端口。 */
    public interface DispatchGatePort {

        /** 判断指定履约单元当前是否允许发货。 */
        boolean canDispatch(String purchaseOrderId, String fulfillmentUnitId);
    }
}
