package com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.AbstractAggregateRoot;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.SupplierRef;
import com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.event.SupplierFulfillmentEvents;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 一张生效 PO 对应一个供应商履约单，拥有承诺和累计发运数量。
 */
public class SupplierFulfillmentOrder extends AbstractAggregateRoot {

    /** 供应商履约单聚合标识。 */
    private final String id;

    /** 触发本履约单的生效采购订单。 */
    private final String purchaseOrderId;

    /** 便于供应商门户展示的采购订单号快照。 */
    private final String purchaseOrderNo;

    /** 负责履约的供应商快照。 */
    private final SupplierRef supplier;

    /** 每个采购订单行的承诺、发运和取消数量账。 */
    private final List<CommitmentLine> commitments;

    /** 供应商履约单自身的执行状态。 */
    private FulfillmentStatus status;

    /** 根据生效采购订单快照创建待供应商确认的履约聚合。 */
    private SupplierFulfillmentOrder(
            String id,
            String purchaseOrderId,
            String purchaseOrderNo,
            SupplierRef supplier,
            List<CommitmentLine> commitments
    ) {
        require(id != null && !id.isBlank(), "供应商履约单 ID 不能为空");
        require(purchaseOrderId != null && !purchaseOrderId.isBlank(), "采购订单 ID 不能为空");
        require(purchaseOrderNo != null && !purchaseOrderNo.isBlank(), "采购订单号不能为空");
        require(supplier != null, "供应商不能为空");
        require(commitments != null && !commitments.isEmpty(), "供应商履约单至少包含一行");
        this.id = id;
        this.purchaseOrderId = purchaseOrderId;
        this.purchaseOrderNo = purchaseOrderNo;
        this.supplier = supplier;
        this.commitments = new ArrayList<>(commitments);
        this.status = FulfillmentStatus.WAITING_CONFIRMATION;
        long eventVersion = nextVersion();
        raise(new SupplierFulfillmentEvents.SupplierFulfillmentOrderCreated(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                purchaseOrderNo,
                supplier.supplierCode()
        ));
    }

    /**
     * 根据 PO 生效快照创建唯一供应商履约单。
     */
    public static SupplierFulfillmentOrder create(
            String id,
            String purchaseOrderId,
            String purchaseOrderNo,
            SupplierRef supplier,
            List<CommitmentLine> commitments
    ) {
        return new SupplierFulfillmentOrder(id, purchaseOrderId, purchaseOrderNo, supplier, commitments);
    }

    /**
     * 记录供应商对所有订单行的数量和日期承诺。
     */
    public void confirm(Map<String, CommitmentDecision> decisions) {
        require(status == FulfillmentStatus.WAITING_CONFIRMATION, "履约单不是待确认状态");
        require(decisions != null && !decisions.isEmpty(), "供应商承诺不能为空");
        for (CommitmentLine line : commitments) {
            CommitmentDecision decision = decisions.get(line.purchaseOrderLineId());
            require(decision != null, "每个订单行都必须给出交付承诺");
            line.confirm(decision.committedQuantity(), decision.promisedDate());
        }
        status = FulfillmentStatus.CONFIRMED;
        long eventVersion = nextVersion();
        raise(new SupplierFulfillmentEvents.SupplierCommitmentConfirmed(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                commitmentSnapshots()
        ));
    }

    /**
     * 记录供应商拒绝承接尚未确认的履约单。
     */
    public void reject(String reason) {
        require(status == FulfillmentStatus.WAITING_CONFIRMATION, "履约单不是待确认状态");
        require(reason != null && !reason.isBlank(), "拒绝原因不能为空");
        status = FulfillmentStatus.REJECTED;
        long eventVersion = nextVersion();
        raise(new SupplierFulfillmentEvents.SupplierCommitmentRejected(
                UUID.randomUUID().toString(), id, eventVersion, Instant.now(), purchaseOrderId, reason
        ));
    }

    /**
     * 修改尚未处理完的交付承诺，且新承诺不能小于已发运和已取消数量。
     */
    public void reviseCommitment(
            String purchaseOrderLineId,
            Quantity committedQuantity,
            LocalDate promisedDate
    ) {
        require(
                status == FulfillmentStatus.CONFIRMED || status == FulfillmentStatus.IN_EXECUTION,
                "当前履约状态不能修改承诺"
        );
        CommitmentLine line = requireLine(purchaseOrderLineId);
        line.revise(committedQuantity, promisedDate);
        long eventVersion = nextVersion();
        raise(new SupplierFulfillmentEvents.SupplierCommitmentChanged(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                purchaseOrderLineId,
                committedQuantity,
                promisedDate
        ));
    }

    /**
     * 与发运聚合在同一应用事务中调用，登记已经发生的发运事实。
     */
    public void registerDispatchedShipment(SupplierShipment shipment) {
        require(shipment.fulfillmentOrderId().equals(id), "发运批次不属于当前履约单");
        require(shipment.supplierCode().equals(supplier.supplierCode()), "发运供应商与 PO 供应商不一致");
        require(shipment.status() == SupplierShipment.ShipmentStatus.DISPATCHED, "发运批次尚未发出");
        shipment.lines().forEach(line ->
                requireLine(line.purchaseOrderLineId()).registerShipment(line.shippedQuantity())
        );
        status = FulfillmentStatus.IN_EXECUTION;
        nextVersion();
    }

    /**
     * 明确取消某承诺行尚未发运的剩余数量。
     */
    public void cancelRemaining(String purchaseOrderLineId, Quantity quantity) {
        require(
                status == FulfillmentStatus.CONFIRMED || status == FulfillmentStatus.IN_EXECUTION,
                "当前履约状态不能取消余量"
        );
        requireLine(purchaseOrderLineId).cancel(quantity);
        nextVersion();
    }

    /**
     * 在所有承诺数量均已发运或取消后完成供应商履约。
     */
    public void complete() {
        require(status == FulfillmentStatus.IN_EXECUTION, "只有执行中的履约单可以完成");
        require(commitments.stream().allMatch(CommitmentLine::isFullyFulfilled), "仍有未发运或未取消数量");
        status = FulfillmentStatus.COMPLETED;
        long eventVersion = nextVersion();
        raise(new SupplierFulfillmentEvents.SupplierFulfillmentCompleted(
                UUID.randomUUID().toString(), id, eventVersion, Instant.now(), purchaseOrderId
        ));
    }

    /**
     * 终止尚在确认后执行阶段的剩余供应商履约责任。
     */
    public void terminate() {
        require(
                status == FulfillmentStatus.CONFIRMED || status == FulfillmentStatus.IN_EXECUTION,
                "当前履约状态不能终止"
        );
        status = FulfillmentStatus.TERMINATED;
        nextVersion();
    }

    /** 查找履约承诺行，不存在时拒绝命令。 */
    private CommitmentLine requireLine(String purchaseOrderLineId) {
        return commitments.stream()
                .filter(line -> line.purchaseOrderLineId().equals(purchaseOrderLineId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("履约承诺行不存在: " + purchaseOrderLineId));
    }

    /** 生成供应商确认事件使用的不可变承诺快照。 */
    private List<SupplierFulfillmentEvents.CommitmentSnapshot> commitmentSnapshots() {
        return commitments.stream()
                .map(line -> new SupplierFulfillmentEvents.CommitmentSnapshot(
                        line.purchaseOrderLineId(),
                        line.orderedQuantity(),
                        line.committedQuantity(),
                        line.promisedDate()
                ))
                .toList();
    }

    /** 返回供应商履约单标识。 */
    public String id() {
        return id;
    }

    /** 返回对应采购订单标识。 */
    public String purchaseOrderId() {
        return purchaseOrderId;
    }

    /** 返回采购订单业务单号快照。 */
    public String purchaseOrderNo() {
        return purchaseOrderNo;
    }

    /** 返回履约供应商快照。 */
    public SupplierRef supplier() {
        return supplier;
    }

    /** 返回承诺行只读副本。 */
    public List<CommitmentLine> commitments() {
        return List.copyOf(commitments);
    }

    /** 返回供应商履约当前状态。 */
    public FulfillmentStatus status() {
        return status;
    }

    /**
     * 供应商对单个订单行给出的交付承诺。
     *
     * @param committedQuantity 承诺交付数量
     * @param promisedDate 承诺交付日期
     */
    public record CommitmentDecision(Quantity committedQuantity, LocalDate promisedDate) {
    }

    public enum FulfillmentStatus {
        WAITING_CONFIRMATION,
        CONFIRMED,
        IN_EXECUTION,
        COMPLETED,
        REJECTED,
        TERMINATED
    }

    /** 订单行级供应商承诺和累计处理数量账。 */
    public static final class CommitmentLine {

        /** 被履约的采购订单行标识。 */
        private final String purchaseOrderLineId;

        /** PO 生效时的订单数量。 */
        private final Quantity orderedQuantity;

        /** 规则允许的最大超交数量。 */
        private final Quantity allowedOverDelivery;

        /** 供应商当前有效承诺数量。 */
        private Quantity committedQuantity;

        /** 已由供应商实际发出的累计数量。 */
        private Quantity shippedQuantity;

        /** 已明确取消、不再交付的累计数量。 */
        private Quantity cancelledQuantity;

        /** 供应商当前承诺交付日期。 */
        private LocalDate promisedDate;

        /**
         * 根据 PO 行初始化尚未确认的履约承诺行。
         */
        public CommitmentLine(
                String purchaseOrderLineId,
                Quantity orderedQuantity,
                Quantity allowedOverDelivery
        ) {
            require(purchaseOrderLineId != null && !purchaseOrderLineId.isBlank(), "订单行 ID 不能为空");
            require(orderedQuantity != null && orderedQuantity.isPositive(), "订单数量必须大于零");
            require(allowedOverDelivery != null, "允许超交数量不能为空");
            orderedQuantity.add(allowedOverDelivery);
            this.purchaseOrderLineId = purchaseOrderLineId;
            this.orderedQuantity = orderedQuantity;
            this.allowedOverDelivery = allowedOverDelivery;
            this.committedQuantity = Quantity.zero(orderedQuantity.unit());
            this.shippedQuantity = Quantity.zero(orderedQuantity.unit());
            this.cancelledQuantity = Quantity.zero(orderedQuantity.unit());
        }

        /** 首次写入供应商数量和日期承诺。 */
        private void confirm(Quantity quantity, LocalDate date) {
            require(quantity != null && quantity.isPositive(), "承诺数量必须大于零");
            require(quantity.lessThanOrEqualTo(orderedQuantity.add(allowedOverDelivery)), "承诺数量超过允许范围");
            require(date != null, "承诺交付日期不能为空");
            committedQuantity = quantity;
            promisedDate = date;
        }

        /** 修改承诺，同时保护已经发运和取消的历史数量。 */
        private void revise(Quantity quantity, LocalDate date) {
            require(quantity != null && quantity.isPositive(), "承诺数量必须大于零");
            require(quantity.lessThanOrEqualTo(orderedQuantity.add(allowedOverDelivery)), "承诺数量超过允许范围");
            require(shippedQuantity.add(cancelledQuantity).lessThanOrEqualTo(quantity), "新承诺不能小于已处理数量");
            require(date != null, "承诺交付日期不能为空");
            committedQuantity = quantity;
            promisedDate = date;
        }

        /** 将一次已发运批次数量累计到承诺行。 */
        private void registerShipment(Quantity quantity) {
            require(quantity != null && quantity.isPositive(), "发运数量必须大于零");
            require(quantity.lessThanOrEqualTo(remainingToShip()), "发运数量超过剩余承诺数量");
            shippedQuantity = shippedQuantity.add(quantity);
        }

        /** 将不再交付的剩余数量累计为取消量。 */
        private void cancel(Quantity quantity) {
            require(quantity != null && quantity.isPositive(), "取消数量必须大于零");
            require(quantity.lessThanOrEqualTo(remainingToShip()), "取消数量超过剩余承诺数量");
            cancelledQuantity = cancelledQuantity.add(quantity);
        }

        /** 计算当前承诺下仍需发运的数量。 */
        public Quantity remainingToShip() {
            return committedQuantity.subtract(shippedQuantity).subtract(cancelledQuantity);
        }

        /** 判断本承诺行是否已经全部处理。 */
        private boolean isFullyFulfilled() {
            return remainingToShip().isZero();
        }

        /** 返回采购订单行标识。 */
        public String purchaseOrderLineId() {
            return purchaseOrderLineId;
        }

        /** 返回 PO 原始下单数量。 */
        public Quantity orderedQuantity() {
            return orderedQuantity;
        }

        /** 返回供应商当前承诺数量。 */
        public Quantity committedQuantity() {
            return committedQuantity;
        }

        /** 返回累计已发运数量。 */
        public Quantity shippedQuantity() {
            return shippedQuantity;
        }

        /** 返回累计已取消数量。 */
        public Quantity cancelledQuantity() {
            return cancelledQuantity;
        }

        /** 返回供应商承诺交付日期。 */
        public LocalDate promisedDate() {
            return promisedDate;
        }
    }
}
