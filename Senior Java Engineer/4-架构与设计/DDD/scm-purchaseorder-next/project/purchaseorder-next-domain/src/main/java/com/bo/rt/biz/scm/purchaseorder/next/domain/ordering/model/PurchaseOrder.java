package com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.event.OrderingEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.AbstractAggregateRoot;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Destination;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Money;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.SupplierRef;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 采购订单聚合，只管理商业承诺，不管理发货、质检、入库和结算状态。
 */
public class PurchaseOrder extends AbstractAggregateRoot {

    /** 采购订单聚合标识。 */
    private final String id;

    /** 对外展示和业务检索使用的采购订单号。 */
    private final String orderNo;

    /** 本订单消耗的 PR 转单预占令牌。 */
    private final String sourceReservationId;

    /** 订单生效时承诺履约的供应商快照。 */
    private final SupplierRef supplier;

    /** 承担采购与应付责任的采购主体。 */
    private final String buyerOrganizationId;

    /** 决定物流、合规和费用策略的采购路线。 */
    private final ProcurementRoute route;

    /** 下单时确认的贸易与运输责任条款。 */
    private final DeliveryTerms deliveryTerms;

    /** 订单包含的商业承诺行。 */
    private final List<PurchaseOrderLine> lines;

    /** 采购订单自身的商业生命周期状态。 */
    private OrderStatus status;

    /** 独立展示的审批流程状态，不与商业状态混用。 */
    private ApprovalStatus approvalStatus;

    /** 当前审批实例标识，用于过滤旧审批回调。 */
    private String approvalInstanceId;

    /** 最近一次提交审批时冻结的订单版本。 */
    private long submittedOrderVersion;

    /** 根据转单预占和商业条款创建采购订单草稿。 */
    private PurchaseOrder(
            String id,
            String orderNo,
            String sourceReservationId,
            SupplierRef supplier,
            String buyerOrganizationId,
            ProcurementRoute route,
            DeliveryTerms deliveryTerms,
            List<PurchaseOrderLine> lines
    ) {
        require(id != null && !id.isBlank(), "采购订单 ID 不能为空");
        require(orderNo != null && !orderNo.isBlank(), "采购订单号不能为空");
        require(supplier != null, "供应商不能为空");
        require(buyerOrganizationId != null && !buyerOrganizationId.isBlank(), "采购主体不能为空");
        require(route != null, "采购路线不能为空");
        require(deliveryTerms != null, "交付条款不能为空");
        require(lines != null && !lines.isEmpty(), "采购订单至少包含一行");
        String currency = lines.get(0).taxedUnitPrice().currency();
        require(
                lines.stream().allMatch(line -> currency.equals(line.taxedUnitPrice().currency())),
                "同一采购订单的结算币种必须一致"
        );
        this.id = id;
        this.orderNo = orderNo;
        this.sourceReservationId = sourceReservationId;
        this.supplier = supplier;
        this.buyerOrganizationId = buyerOrganizationId;
        this.route = route;
        this.deliveryTerms = deliveryTerms;
        this.lines = new ArrayList<>(lines);
        this.status = OrderStatus.DRAFT;
        this.approvalStatus = ApprovalStatus.NOT_STARTED;
        long eventVersion = nextVersion();
        raise(new OrderingEvents.PurchaseOrderDraftCreated(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                orderNo,
                supplier.supplierCode()
        ));
    }

    /**
     * 从已预占的 PR 数量创建订单草稿并发布草稿创建事件。
     */
    public static PurchaseOrder createDraft(
            String id,
            String orderNo,
            String sourceReservationId,
            SupplierRef supplier,
            String buyerOrganizationId,
            ProcurementRoute route,
            DeliveryTerms deliveryTerms,
            List<PurchaseOrderLine> lines
    ) {
        return new PurchaseOrder(
                id,
                orderNo,
                sourceReservationId,
                supplier,
                buyerOrganizationId,
                route,
                deliveryTerms,
                lines
        );
    }

    /**
     * 在草稿或驳回状态执行受控编辑，并通过期望版本防止并发覆盖。
     */
    public void reviseDraft(long expectedVersion, Runnable revision) {
        requireEditable(expectedVersion);
        revision.run();
        nextVersion();
    }

    /**
     * 提交订单审批，同时冻结本轮审批对应的订单版本。
     */
    public void submit(long expectedVersion, String approvalInstanceId) {
        requireVersion(expectedVersion);
        require(status == OrderStatus.DRAFT || status == OrderStatus.REJECTED, "当前订单不能提交审批");
        require(approvalInstanceId != null && !approvalInstanceId.isBlank(), "审批实例不能为空");
        this.approvalInstanceId = approvalInstanceId;
        status = OrderStatus.APPROVAL_PENDING;
        approvalStatus = ApprovalStatus.PROCESSING;
        submittedOrderVersion = nextVersion();
        raise(new OrderingEvents.PurchaseOrderSubmitted(
                UUID.randomUUID().toString(),
                id,
                submittedOrderVersion,
                Instant.now(),
                orderNo,
                approvalInstanceId,
                submittedOrderVersion
        ));
    }

    /**
     * 撤回正在处理的审批，使订单回到可编辑草稿状态。
     */
    public void withdrawApproval(long expectedVersion) {
        requireVersion(expectedVersion);
        require(status == OrderStatus.APPROVAL_PENDING, "采购订单不在审批中");
        status = OrderStatus.DRAFT;
        approvalStatus = ApprovalStatus.WITHDRAWN;
        long eventVersion = nextVersion();
        raise(new OrderingEvents.PurchaseOrderApprovalWithdrawn(
                UUID.randomUUID().toString(), id, eventVersion, Instant.now(), orderNo
        ));
    }

    /**
     * 记录与审批实例和提交版本都匹配的审批结果。
     *
     * <p>审批通过发布 {@code PurchaseOrderEffective}，作为履约、协同和结算的启动事实。</p>
     */
    public void recordApprovalResult(
            String approvalInstanceId,
            long approvedOrderVersion,
            boolean approved,
            String reason
    ) {
        require(status == OrderStatus.APPROVAL_PENDING, "采购订单不在审批中");
        require(this.approvalInstanceId.equals(approvalInstanceId), "审批实例不匹配");
        require(submittedOrderVersion == approvedOrderVersion, "审批结果对应的订单版本已失效");
        if (approved) {
            status = OrderStatus.EFFECTIVE;
            approvalStatus = ApprovalStatus.APPROVED;
            long eventVersion = nextVersion();
            raise(new OrderingEvents.PurchaseOrderEffective(
                    UUID.randomUUID().toString(),
                    id,
                    eventVersion,
                    Instant.now(),
                    orderNo,
                    supplier.supplierCode(),
                    buyerOrganizationId,
                    routeSnapshot(),
                    currency(),
                    lineSnapshots()
            ));
            return;
        }
        status = OrderStatus.REJECTED;
        approvalStatus = ApprovalStatus.REJECTED;
        long eventVersion = nextVersion();
        raise(new OrderingEvents.PurchaseOrderRejected(
                UUID.randomUUID().toString(), id, eventVersion, Instant.now(), orderNo, reason
        ));
    }

    /**
     * 取消尚未生效的草稿或被驳回订单。
     */
    public void cancel(long expectedVersion, String reason) {
        requireVersion(expectedVersion);
        require(status == OrderStatus.DRAFT || status == OrderStatus.REJECTED, "只有草稿或驳回订单能取消");
        status = OrderStatus.CANCELLED;
        long eventVersion = nextVersion();
        raise(new OrderingEvents.PurchaseOrderCancelled(
                UUID.randomUUID().toString(), id, eventVersion, Instant.now(), orderNo, reason
        ));
    }

    /**
     * 根据已审批终止变更将生效订单推进到终止处理中。
     */
    public void requestTermination(OrderChange change) {
        require(status == OrderStatus.EFFECTIVE, "只有生效订单可以申请终止");
        requireApprovedChange(change, OrderChange.TerminationRequest.class);
        status = OrderStatus.TERMINATION_PENDING;
        long eventVersion = nextVersion();
        raise(new OrderingEvents.PurchaseOrderTerminationRequested(
                UUID.randomUUID().toString(), id, eventVersion, Instant.now(), orderNo, change.id()
        ));
    }

    /**
     * 记录终止处理结果；拒绝时恢复生效，批准时保留历史履约事实并终止商业承诺。
     */
    public void recordTerminationResult(OrderChange change, boolean approved) {
        require(status == OrderStatus.TERMINATION_PENDING, "订单不在终止审批中");
        requireApprovedChange(change, OrderChange.TerminationRequest.class);
        if (!approved) {
            status = OrderStatus.EFFECTIVE;
            nextVersion();
            return;
        }
        status = OrderStatus.TERMINATED;
        change.markApplied();
        long eventVersion = nextVersion();
        String reason = ((OrderChange.TerminationRequest) change.detail()).reason();
        raise(new OrderingEvents.PurchaseOrderTerminated(
                UUID.randomUUID().toString(), id, eventVersion, Instant.now(), orderNo, reason
        ));
    }

    /**
     * 将已审批的订单行交期变更应用到订单并发布变更事件。
     */
    public void applyApprovedDeliveryDateChange(OrderChange change) {
        require(status == OrderStatus.EFFECTIVE, "只有生效订单可以变更交期");
        requireApprovedChange(change, OrderChange.DeliveryDateChange.class);
        OrderChange.DeliveryDateChange detail = (OrderChange.DeliveryDateChange) change.detail();
        PurchaseOrderLine line = requireLine(detail.lineId());
        line.changeExpectedArrivalDate(detail.newExpectedArrivalDate());
        change.markApplied();
        long eventVersion = nextVersion();
        raise(new OrderingEvents.PurchaseOrderDeliveryDateChanged(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                orderNo,
                line.id(),
                line.expectedArrivalDate(),
                change.id()
        ));
    }

    /**
     * 将已审批的订单行调价应用到订单并发布调价事件。
     */
    public void applyApprovedPriceAdjustment(OrderChange change) {
        require(status == OrderStatus.EFFECTIVE, "只有生效订单可以调价");
        requireApprovedChange(change, OrderChange.PriceAdjustment.class);
        OrderChange.PriceAdjustment detail = (OrderChange.PriceAdjustment) change.detail();
        PurchaseOrderLine line = requireLine(detail.lineId());
        line.adjustPrice(detail.newTaxedUnitPrice());
        change.markApplied();
        long eventVersion = nextVersion();
        raise(new OrderingEvents.PurchaseOrderPriceAdjusted(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                orderNo,
                line.id(),
                line.taxedUnitPrice(),
                change.id()
        ));
    }

    /**
     * 在所有履约责任完成后标记商业订单完成。
     */
    public void complete() {
        require(status == OrderStatus.EFFECTIVE, "只有生效订单可以完成");
        status = OrderStatus.COMPLETED;
        long eventVersion = nextVersion();
        raise(new OrderingEvents.PurchaseOrderCompleted(
                UUID.randomUUID().toString(), id, eventVersion, Instant.now(), orderNo
        ));
    }

    /** 校验订单版本和当前状态是否允许直接编辑。 */
    private void requireEditable(long expectedVersion) {
        requireVersion(expectedVersion);
        require(status == OrderStatus.DRAFT || status == OrderStatus.REJECTED, "当前订单不能直接编辑");
    }

    /** 使用乐观锁语义拒绝基于旧版本发出的命令。 */
    private void requireVersion(long expectedVersion) {
        require(version() == expectedVersion, "订单已被其他操作修改，请刷新后重试");
    }

    /** 校验变更单归属、状态、基准版本及具体变更类型。 */
    private void requireApprovedChange(OrderChange change, Class<?> expectedType) {
        require(change != null && change.purchaseOrderId().equals(id), "变更单不属于当前订单");
        require(change.status() == OrderChange.ChangeStatus.APPROVED, "变更单尚未审批通过");
        require(change.baseOrderVersion() <= version(), "变更单基准版本晚于当前订单版本");
        require(expectedType.isInstance(change.detail()), "变更类型不匹配");
    }

    /** 查找目标订单行，不存在时拒绝变更命令。 */
    private PurchaseOrderLine requireLine(String lineId) {
        return lines.stream()
                .filter(line -> line.id().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("订单行不存在: " + lineId));
    }

    /** 生成跨上下文事件使用的不可变订单行快照。 */
    private List<OrderingEvents.OrderLineSnapshot> lineSnapshots() {
        return lines.stream()
                .map(line -> new OrderingEvents.OrderLineSnapshot(
                        line.id(),
                        line.legacyPsoCode(),
                        line.requisitionLineId(),
                        line.sku().skuCode(),
                        line.orderedQuantity(),
                        line.taxedUnitPrice(),
                        line.expectedArrivalDate()
                ))
                .toList();
    }

    /** 生成供下游上下文判断物流、合规和协同要求的采购路线快照。 */
    private OrderingEvents.ProcurementRouteSnapshot routeSnapshot() {
        ProcurementRoute.TransitNode transitNode = route.transitNode();
        return new OrderingEvents.ProcurementRouteSnapshot(
                route.routeType().name(),
                route.supplierRegion().countryCode(),
                route.supplierRegion().regionType().name(),
                route.deliveryMethod().name(),
                transitNode == null ? null : transitNode.code(),
                transitNode == null ? null : transitNode.countryCode(),
                route.destination().type().name(),
                route.destination().code(),
                route.destination().countryCode(),
                route.policyVersion()
        );
    }

    /** 汇总所有订单行的当前含税金额。 */
    public Money totalAmount() {
        return lines.stream()
                .map(PurchaseOrderLine::total)
                .reduce(Money.zero(currency()), Money::add);
    }

    /** 返回订单统一结算币种。 */
    public String currency() {
        return lines.get(0).taxedUnitPrice().currency();
    }

    /** 返回采购订单聚合标识。 */
    public String id() {
        return id;
    }

    /** 返回采购订单业务单号。 */
    public String orderNo() {
        return orderNo;
    }

    /** 返回来源 PR 转单预占标识。 */
    public String sourceReservationId() {
        return sourceReservationId;
    }

    /** 返回供应商快照。 */
    public SupplierRef supplier() {
        return supplier;
    }

    /** 返回采购主体标识。 */
    public String buyerOrganizationId() {
        return buyerOrganizationId;
    }

    /** 返回履约目的地。 */
    public Destination destination() {
        return route.destination();
    }

    /** 返回采购路线。 */
    public ProcurementRoute route() {
        return route;
    }

    /** 返回贸易与运输责任条款。 */
    public DeliveryTerms deliveryTerms() {
        return deliveryTerms;
    }

    /** 返回订单行只读副本。 */
    public List<PurchaseOrderLine> lines() {
        return List.copyOf(lines);
    }

    /** 返回订单商业状态。 */
    public OrderStatus status() {
        return status;
    }

    /** 返回独立审批状态。 */
    public ApprovalStatus approvalStatus() {
        return approvalStatus;
    }

    /** 返回最近一次提交审批时的订单版本。 */
    public long submittedOrderVersion() {
        return submittedOrderVersion;
    }

    public enum OrderStatus {
        DRAFT,
        APPROVAL_PENDING,
        REJECTED,
        EFFECTIVE,
        CANCELLED,
        TERMINATION_PENDING,
        TERMINATED,
        COMPLETED
    }

    public enum ApprovalStatus {
        NOT_STARTED,
        PROCESSING,
        APPROVED,
        REJECTED,
        WITHDRAWN
    }

    /**
     * 采购订单交付条款。
     *
     * @param incoterm 国际贸易术语或内部交付条款代码
     * @param supplierArrangesTransport 是否由供应商安排运输
     * @param allowOverDelivery 是否允许供应商在规则范围内超交
     */
    public record DeliveryTerms(
            String incoterm,
            boolean supplierArrangesTransport,
            boolean allowOverDelivery
    ) {

        /** 校验并创建订单级交付条款。 */
        public DeliveryTerms {
            require(incoterm != null && !incoterm.isBlank(), "贸易条款不能为空");
        }
    }
}
