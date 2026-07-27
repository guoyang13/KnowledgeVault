package com.bo.rt.biz.scm.purchaseorder.next.application.ordering;

import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port.ApprovalWorkflowPort;
import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port.LocationDirectoryPort;
import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port.LocationDirectoryPort.LocationProfile;
import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port.OrderNoPort;
import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port.SupplierProfilePort;
import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port.SupplierProfilePort.SupplierOrderingProfile;
import com.bo.rt.biz.scm.purchaseorder.next.application.shared.port.DomainEventPublisher;
import com.bo.rt.biz.scm.purchaseorder.next.application.shared.port.IdentityGenerator;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.PurchaseOrder;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.PurchaseOrder.DeliveryTerms;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoute;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoute.DeliveryMethod;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoute.SupplierRegion;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoute.TransitNode;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoutePolicy;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoutePolicy.RouteFacts;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.PurchaseOrderLine;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.repository.PurchaseOrderRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.model.PurchaseRequisition;
import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.model.TransferReservation;
import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.repository.PurchaseRequisitionRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.repository.TransferReservationRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Destination;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Money;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 采购订单用例。转单时计划与订单聚合处于同一服务数据库事务。
 */
public class OrderingApplicationService {

    /** 采购订单聚合仓储。 */
    private final PurchaseOrderRepository orderRepository;
    /** 采购申请聚合仓储，用于提交转单数量。 */
    private final PurchaseRequisitionRepository requisitionRepository;
    /** 转单预占仓储，用于保证建单幂等。 */
    private final TransferReservationRepository reservationRepository;
    /** 供应商档案端口，提供准入状态和路线起点国家。 */
    private final SupplierProfilePort supplierProfilePort;
    /** 地点目录端口，解析目的地和可选中转节点。 */
    private final LocationDirectoryPort locationDirectoryPort;
    /** 根据可信地点事实计算采购路线的领域策略。 */
    private final ProcurementRoutePolicy procurementRoutePolicy;
    /** 采购订单号生成端口。 */
    private final OrderNoPort orderNoPort;
    /** 外部审批流程端口。 */
    private final ApprovalWorkflowPort approvalWorkflowPort;
    /** 聚合及实体标识生成器。 */
    private final IdentityGenerator identityGenerator;
    /** 随本地事务写入领域事件的发布端口。 */
    private final DomainEventPublisher eventPublisher;

    /** 注入采购订单用例依赖。 */
    public OrderingApplicationService(
            PurchaseOrderRepository orderRepository,
            PurchaseRequisitionRepository requisitionRepository,
            TransferReservationRepository reservationRepository,
            SupplierProfilePort supplierProfilePort,
            LocationDirectoryPort locationDirectoryPort,
            ProcurementRoutePolicy procurementRoutePolicy,
            OrderNoPort orderNoPort,
            ApprovalWorkflowPort approvalWorkflowPort,
            IdentityGenerator identityGenerator,
            DomainEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.requisitionRepository = requisitionRepository;
        this.reservationRepository = reservationRepository;
        this.supplierProfilePort = supplierProfilePort;
        this.locationDirectoryPort = locationDirectoryPort;
        this.procurementRoutePolicy = procurementRoutePolicy;
        this.orderNoPort = orderNoPort;
        this.approvalWorkflowPort = approvalWorkflowPort;
        this.identityGenerator = identityGenerator;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 根据有效转单预占创建采购订单草稿，并原子提交采购申请数量账本。
     *
     * <p>预占已确认时返回原订单，实现重复请求幂等。</p>
     */
    public OrderResult createFromRequisition(CreateOrderFromRequisition command) {
        TransferReservation reservation = reservationRepository.findById(command.reservationId())
                .orElseThrow(() -> new IllegalArgumentException("转单预占不存在"));
        if (reservation.status() == TransferReservation.ReservationStatus.CONFIRMED) {
            PurchaseOrder existing = orderRepository.findById(reservation.purchaseOrderId())
                    .orElseThrow(() -> new IllegalStateException("预占已确认但订单不存在"));
            return toResult(existing);
        }
        if (reservation.status() != TransferReservation.ReservationStatus.RESERVED
                || reservation.isExpired()) {
            throw new IllegalStateException("转单预占已经失效");
        }

        PurchaseRequisition requisition = requisitionRepository.findById(reservation.requisitionId())
                .orElseThrow(() -> new IllegalArgumentException("采购申请不存在"));
        ProcurementRoute route = determineRoute(requisition, command);
        Map<String, PurchaseRequisition.RequisitionLine> sourceLines = requisition.lines().stream()
                .collect(java.util.stream.Collectors.toMap(
                        PurchaseRequisition.RequisitionLine::id,
                        line -> line
                ));
        List<PurchaseOrderLine> orderLines = reservation.quantities().entrySet().stream()
                .map(entry -> {
                    PurchaseRequisition.RequisitionLine source = sourceLines.get(entry.getKey());
                    if (source == null) {
                        throw new IllegalArgumentException("预占引用了未知采购申请行");
                    }
                    LineCommercialTerms terms = command.lineTerms().get(entry.getKey());
                    if (terms == null) {
                        throw new IllegalArgumentException("缺少订单行商业条款: " + entry.getKey());
                    }
                    return new PurchaseOrderLine(
                            identityGenerator.nextId("purchase-order-line"),
                            terms.legacyPsoCode(),
                            source.id(),
                            source.sku(),
                            entry.getValue(),
                            terms.taxedUnitPrice(),
                            terms.expectedArrivalDate()
                    );
                })
                .toList();

        PurchaseOrder order = PurchaseOrder.createDraft(
                identityGenerator.nextId("purchase-order"),
                orderNoPort.nextPurchaseOrderCode(),
                reservation.id(),
                requisition.supplier(),
                command.buyerOrganizationId(),
                route,
                command.deliveryTerms(),
                orderLines
        );

        // 单一事务：保存 PO -> 将 reserved 转为 ordered -> 保存 PR/预占 -> 写 Outbox。
        orderRepository.save(order);
        requisition.commitToOrder(reservation, order.id());
        requisitionRepository.save(requisition);
        reservationRepository.save(reservation);
        eventPublisher.publishAll(order.pullDomainEvents());
        eventPublisher.publishAll(requisition.pullDomainEvents());
        return toResult(order);
    }

    /**
     * 提交采购订单审批，并把审批实例绑定到即将提交的订单版本。
     */
    public OrderResult submit(String orderNo, long expectedVersion) {
        PurchaseOrder order = requireOrder(orderNo);
        String approvalInstanceId = approvalWorkflowPort.startPurchaseOrderApproval(
                order.orderNo(),
                expectedVersion + 1
        );
        order.submit(expectedVersion, approvalInstanceId);
        orderRepository.save(order);
        eventPublisher.publishAll(order.pullDomainEvents());
        return toResult(order);
    }

    /**
     * 接收审批结果；聚合会校验审批实例和提交时订单版本，拒绝过期回调。
     */
    public OrderResult recordApprovalResult(
            String orderNo,
            String approvalInstanceId,
            long submittedOrderVersion,
            boolean approved,
            String reason
    ) {
        PurchaseOrder order = requireOrder(orderNo);
        order.recordApprovalResult(
                approvalInstanceId, submittedOrderVersion, approved, reason
        );
        orderRepository.save(order);
        eventPublisher.publishAll(order.pullDomainEvents());
        return toResult(order);
    }

    /** 按采购订单号读取写模型的简要结果。 */
    public OrderResult getByOrderNo(String orderNo) {
        return toResult(requireOrder(orderNo));
    }

    /** 按订单号加载采购订单聚合，不存在时终止用例。 */
    private PurchaseOrder requireOrder(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("采购订单不存在"));
    }

    /** 从供应商和地点目录读取可信事实，再调用领域策略计算路线。 */
    private ProcurementRoute determineRoute(
            PurchaseRequisition requisition,
            CreateOrderFromRequisition command
    ) {
        String supplierCode = requisition.supplier().supplierCode();
        SupplierOrderingProfile supplier = supplierProfilePort.getOrderingProfile(supplierCode);
        if (supplier == null || !supplierCode.equals(supplier.supplierCode())) {
            throw new IllegalStateException("供应商档案返回结果与采购申请不匹配");
        }
        if (!supplier.orderAllowed()) {
            throw new IllegalStateException("供应商当前不允许创建采购订单");
        }

        LocationProfile destinationLocation = requireLocation(command.destinationCode());
        if (!destinationLocation.destinationEnabled()) {
            throw new IllegalStateException("地点当前不能作为采购目的地");
        }
        Destination destination = new Destination(
                destinationLocation.type(),
                destinationLocation.code(),
                destinationLocation.countryCode()
        );

        TransitNode transitNode = null;
        if (command.transitNodeCode() != null && !command.transitNodeCode().isBlank()) {
            LocationProfile transitLocation = requireLocation(command.transitNodeCode());
            if (!transitLocation.transitEnabled()) {
                throw new IllegalStateException("地点当前不能作为采购中转节点");
            }
            transitNode = new TransitNode(
                    transitLocation.code(),
                    transitLocation.countryCode()
            );
        }

        return procurementRoutePolicy.determine(
                new RouteFacts(
                        new SupplierRegion(supplier.countryCode()),
                        command.deliveryMethod(),
                        transitNode,
                        destination
                ),
                command.deliveryTerms()
        );
    }

    /** 从地点目录读取地点，不存在时以明确业务错误终止建单。 */
    private LocationProfile requireLocation(String locationCode) {
        if (locationCode == null || locationCode.isBlank()) {
            throw new IllegalArgumentException("地点编码不能为空");
        }
        LocationProfile location = locationDirectoryPort.getLocation(locationCode);
        if (location == null) {
            throw new IllegalArgumentException("地点不存在: " + locationCode);
        }
        return location;
    }

    /** 将采购订单聚合转换为稳定的用例返回结构。 */
    private OrderResult toResult(PurchaseOrder order) {
        return new OrderResult(
                order.id(),
                order.orderNo(),
                order.supplier().supplierCode(),
                order.status().name(),
                order.approvalStatus().name(),
                order.version()
        );
    }

    /**
     * 从采购申请转单创建采购订单的命令。
     *
     * @param commandId 调用方命令标识，用于完整实现中的 Inbox 幂等
     * @param reservationId 已成功创建的转单预占标识
     * @param buyerOrganizationId 采购主体标识
     * @param destinationCode 最终目的地编码
     * @param deliveryMethod 供应商直送或集货中转
     * @param transitNodeCode 可选中转节点编码；直送时为空
     * @param deliveryTerms 订单级交付条款
     * @param lineTerms 采购申请行标识到商业条款的映射
     */
    public record CreateOrderFromRequisition(
            String commandId,
            String reservationId,
            String buyerOrganizationId,
            String destinationCode,
            DeliveryMethod deliveryMethod,
            String transitNodeCode,
            DeliveryTerms deliveryTerms,
            Map<String, LineCommercialTerms> lineTerms
    ) {

        /** 固化订单行商业条款映射。 */
        public CreateOrderFromRequisition {
            lineTerms = Map.copyOf(lineTerms);
        }
    }

    /**
     * 从计划转单时补充的采购订单行商业条款。
     *
     * @param legacyPsoCode 兼容旧系统的 PSO 编码
     * @param taxedUnitPrice 含税采购单价
     * @param expectedArrivalDate 期望到货日期
     */
    public record LineCommercialTerms(
            String legacyPsoCode,
            Money taxedUnitPrice,
            LocalDate expectedArrivalDate
    ) {
    }

    /**
     * 采购订单写用例的简要结果。
     *
     * @param purchaseOrderId 采购订单聚合标识
     * @param orderNo 采购订单号
     * @param supplierCode 供应商编码
     * @param status 订单状态
     * @param approvalStatus 审批状态
     * @param writeVersion 聚合写版本
     */
    public record OrderResult(
            String purchaseOrderId,
            String orderNo,
            String supplierCode,
            String status,
            String approvalStatus,
            long writeVersion
    ) {
    }
}
