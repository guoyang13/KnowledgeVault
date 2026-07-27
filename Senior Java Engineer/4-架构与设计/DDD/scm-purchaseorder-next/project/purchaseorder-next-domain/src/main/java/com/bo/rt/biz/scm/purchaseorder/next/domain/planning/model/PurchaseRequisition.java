package com.bo.rt.biz.scm.purchaseorder.next.domain.planning.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.event.PlanningEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.AbstractAggregateRoot;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.SkuRef;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.SupplierRef;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 采购申请聚合（旧 PR），拥有“还可以转多少 PO”的数量事实。
 */
public class PurchaseRequisition extends AbstractAggregateRoot {

    /** 采购申请聚合标识。 */
    private final String id;

    /** 面向采购与供应商展示的申请单号。 */
    private final String requisitionNo;

    /** 本申请选定的供应商快照。 */
    private final SupplierRef supplier;

    /** 拥有申请、确认、预占、下单和取消数量账的申请行。 */
    private final List<RequisitionLine> lines;

    /** 根据各行数量执行进度计算出的申请状态。 */
    private RequisitionStatus status;

    /** 校验申请行并创建待供应商确认的采购申请。 */
    private PurchaseRequisition(
            String id,
            String requisitionNo,
            SupplierRef supplier,
            List<RequisitionLine> lines
    ) {
        require(id != null && !id.isBlank(), "采购申请 ID 不能为空");
        require(requisitionNo != null && !requisitionNo.isBlank(), "采购申请单号不能为空");
        require(supplier != null, "供应商不能为空");
        require(lines != null && !lines.isEmpty(), "采购申请至少包含一行");
        this.id = id;
        this.requisitionNo = requisitionNo;
        this.supplier = supplier;
        this.lines = new ArrayList<>(lines);
        this.status = RequisitionStatus.PENDING_SUPPLIER_CONFIRMATION;
        long eventVersion = nextVersion();
        raise(new PlanningEvents.PurchaseRequisitionCreated(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                requisitionNo,
                supplier.supplierCode()
        ));
    }

    /**
     * 创建待供应商确认的采购申请并发布创建事件。
     */
    public static PurchaseRequisition create(
            String id,
            String requisitionNo,
            SupplierRef supplier,
            List<RequisitionLine> lines
    ) {
        return new PurchaseRequisition(id, requisitionNo, supplier, lines);
    }

    /**
     * 记录供应商逐行确认数量；未提供的行按零确认处理。
     */
    public void confirmBySupplier(Map<String, Quantity> confirmedQuantities) {
        require(
                status == RequisitionStatus.PENDING_SUPPLIER_CONFIRMATION,
                "采购申请不是待供应商确认状态"
        );
        require(confirmedQuantities != null && !confirmedQuantities.isEmpty(), "确认数量不能为空");
        for (RequisitionLine line : lines) {
            Quantity confirmed = confirmedQuantities.getOrDefault(
                    line.id(),
                    Quantity.zero(line.requested().unit())
            );
            line.confirm(confirmed);
        }
        require(lines.stream().anyMatch(line -> line.confirmed().isPositive()), "供应商确认数量不能全部为零");
        status = RequisitionStatus.READY_FOR_ORDER;
        long eventVersion = nextVersion();
        raise(new PlanningEvents.RequisitionSupplierConfirmed(
                UUID.randomUUID().toString(), id, eventVersion, Instant.now(), requisitionNo
        ));
    }

    /**
     * 原子扣减各行可下单量并创建限时转单预占。
     *
     * @return 可供创建 PO 后确认或失败后释放的预占令牌
     */
    public TransferReservation reserveForOrder(
            String reservationId,
            String commandId,
            Map<String, Quantity> quantities,
            Instant expiresAt
    ) {
        require(
                status == RequisitionStatus.READY_FOR_ORDER
                        || status == RequisitionStatus.PARTIALLY_ORDERED,
                "采购申请当前不能转采购订单"
        );
        require(quantities != null && !quantities.isEmpty(), "预占数量不能为空");
        Map<String, Quantity> normalized = new LinkedHashMap<>();
        quantities.forEach((lineId, quantity) -> {
            RequisitionLine line = requireLine(lineId);
            line.reserve(quantity);
            normalized.put(lineId, quantity);
        });
        TransferReservation reservation = TransferReservation.reserve(
                reservationId, id, commandId, normalized, expiresAt
        );
        long eventVersion = nextVersion();
        raise(new PlanningEvents.RequisitionQuantityReserved(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                requisitionNo,
                reservation.id(),
                normalized
        ));
        return reservation;
    }

    /**
     * 在 PO 保存成功后将预占数量从 reserved 转入 ordered。
     */
    public void commitToOrder(TransferReservation reservation, String purchaseOrderId) {
        require(reservation.requisitionId().equals(id), "预占不属于当前采购申请");
        require(reservation.status() == TransferReservation.ReservationStatus.RESERVED, "预占状态不可确认");
        require(!reservation.isExpired(), "预占已经过期");
        reservation.quantities().forEach((lineId, quantity) -> requireLine(lineId).commit(quantity));
        reservation.confirm(purchaseOrderId);
        refreshStatus();
        long eventVersion = nextVersion();
        raise(new PlanningEvents.RequisitionQuantityCommittedToOrder(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                requisitionNo,
                reservation.id(),
                purchaseOrderId
        ));
        if (status == RequisitionStatus.COMPLETED) {
            long completedVersion = nextVersion();
            raise(new PlanningEvents.PurchaseRequisitionCompleted(
                    UUID.randomUUID().toString(), id, completedVersion, Instant.now(), requisitionNo
            ));
        }
    }

    /**
     * 在建单失败或撤销时释放预占数量并记录释放原因。
     */
    public void releaseReservation(TransferReservation reservation, String reason) {
        require(reservation.requisitionId().equals(id), "预占不属于当前采购申请");
        require(reservation.status() == TransferReservation.ReservationStatus.RESERVED, "预占状态不可释放");
        reservation.quantities().forEach((lineId, quantity) -> requireLine(lineId).release(quantity));
        reservation.release();
        refreshStatus();
        long eventVersion = nextVersion();
        raise(new PlanningEvents.RequisitionReservationReleased(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                requisitionNo,
                reservation.id(),
                reason
        ));
    }

    /**
     * 明确放弃某申请行的剩余可下单数量。
     */
    public void cancelRemaining(String lineId, Quantity quantity) {
        require(
                status == RequisitionStatus.READY_FOR_ORDER
                        || status == RequisitionStatus.PARTIALLY_ORDERED,
                "当前状态不能放弃剩余数量"
        );
        requireLine(lineId).cancel(quantity);
        refreshStatus();
        nextVersion();
    }

    /**
     * 在所有确认数量均已下单、取消或处理完毕且没有活动预占时关闭申请。
     */
    public void close(String reason) {
        require(lines.stream().allMatch(RequisitionLine::isFullyAccounted), "仍有未处理数量，不能关闭采购申请");
        require(lines.stream().allMatch(line -> line.reserved().isZero()), "仍有预占数量，不能关闭采购申请");
        status = RequisitionStatus.CLOSED;
        long eventVersion = nextVersion();
        raise(new PlanningEvents.PurchaseRequisitionClosed(
                UUID.randomUUID().toString(), id, eventVersion, Instant.now(), requisitionNo, reason
        ));
    }

    /** 查找申请行，不存在时立即拒绝命令。 */
    private RequisitionLine requireLine(String lineId) {
        return lines.stream()
                .filter(line -> line.id().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("采购申请行不存在: " + lineId));
    }

    /** 根据已下单量和剩余可下单量重新计算聚合状态。 */
    private void refreshStatus() {
        boolean hasOrdered = lines.stream().anyMatch(line -> line.ordered().isPositive());
        boolean hasAvailable = lines.stream().anyMatch(line -> line.availableToOrder().isPositive());
        status = !hasAvailable
                ? RequisitionStatus.COMPLETED
                : hasOrdered ? RequisitionStatus.PARTIALLY_ORDERED : RequisitionStatus.READY_FOR_ORDER;
    }

    /** 返回采购申请聚合标识。 */
    public String id() {
        return id;
    }

    /** 返回采购申请业务单号。 */
    public String requisitionNo() {
        return requisitionNo;
    }

    /** 返回申请供应商快照。 */
    public SupplierRef supplier() {
        return supplier;
    }

    /** 返回采购申请行只读副本。 */
    public List<RequisitionLine> lines() {
        return List.copyOf(lines);
    }

    /** 返回当前申请状态。 */
    public RequisitionStatus status() {
        return status;
    }

    public enum RequisitionStatus {
        PENDING_SUPPLIER_CONFIRMATION,
        READY_FOR_ORDER,
        PARTIALLY_ORDERED,
        COMPLETED,
        CLOSED
    }

    /**
     * 采购申请行及其数量账。
     */
    public static final class RequisitionLine {

        /** 采购申请行标识，是转单数量映射的稳定键。 */
        private final String id;

        /** 产生本申请行的需求行标识。 */
        private final String sourceDemandLineId;

        /** 申请时冻结的商品引用。 */
        private final SkuRef sku;

        /** 最初向供应商申请的数量。 */
        private final Quantity requested;

        /** 供应商明确可供货的数量。 */
        private Quantity confirmed;

        /** 已锁定给建单流程、尚未确认成单的数量。 */
        private Quantity reserved;

        /** 已成功转入采购订单的数量。 */
        private Quantity ordered;

        /** 已明确放弃、不再转单的数量。 */
        private Quantity cancelled;

        /**
         * 创建数量账全部为零的采购申请行。
         */
        public RequisitionLine(String id, String sourceDemandLineId, SkuRef sku, Quantity requested) {
            require(id != null && !id.isBlank(), "采购申请行 ID 不能为空");
            require(sourceDemandLineId != null && !sourceDemandLineId.isBlank(), "来源需求行不能为空");
            require(sku != null, "SKU 不能为空");
            require(requested != null && requested.isPositive(), "申请数量必须大于零");
            this.id = id;
            this.sourceDemandLineId = sourceDemandLineId;
            this.sku = sku;
            this.requested = requested;
            this.confirmed = Quantity.zero(requested.unit());
            this.reserved = Quantity.zero(requested.unit());
            this.ordered = Quantity.zero(requested.unit());
            this.cancelled = Quantity.zero(requested.unit());
        }

        /** 写入供应商确认量并限制其不超过申请量。 */
        private void confirm(Quantity quantity) {
            require(quantity.lessThanOrEqualTo(requested), "供应商确认数量不能超过申请数量");
            confirmed = quantity;
        }

        /** 将可下单量转入活动预占量。 */
        private void reserve(Quantity quantity) {
            require(quantity.isPositive(), "预占数量必须大于零");
            require(quantity.lessThanOrEqualTo(availableToOrder()), "预占数量超过可下单数量");
            reserved = reserved.add(quantity);
        }

        /** 将已预占量转入已下单量。 */
        private void commit(Quantity quantity) {
            require(quantity.lessThanOrEqualTo(reserved), "确认数量超过已预占数量");
            reserved = reserved.subtract(quantity);
            ordered = ordered.add(quantity);
        }

        /** 释放活动预占但不改变已下单事实。 */
        private void release(Quantity quantity) {
            require(quantity.lessThanOrEqualTo(reserved), "释放数量超过已预占数量");
            reserved = reserved.subtract(quantity);
        }

        /** 将剩余可下单量明确记为放弃。 */
        private void cancel(Quantity quantity) {
            require(quantity.isPositive(), "取消数量必须大于零");
            require(quantity.lessThanOrEqualTo(availableToOrder()), "取消数量超过可处理数量");
            cancelled = cancelled.add(quantity);
        }

        /**
         * 计算仍可用于新 PO 预占的数量。
         */
        public Quantity availableToOrder() {
            return confirmed.subtract(reserved).subtract(ordered).subtract(cancelled);
        }

        /** 判断供应商确认量是否已经全部得到处理。 */
        private boolean isFullyAccounted() {
            return availableToOrder().isZero();
        }

        /** 返回申请行标识。 */
        public String id() {
            return id;
        }

        /** 返回来源需求行标识。 */
        public String sourceDemandLineId() {
            return sourceDemandLineId;
        }

        /** 返回商品引用快照。 */
        public SkuRef sku() {
            return sku;
        }

        /** 返回原始申请数量。 */
        public Quantity requested() {
            return requested;
        }

        /** 返回供应商确认数量。 */
        public Quantity confirmed() {
            return confirmed;
        }

        /** 返回当前活动预占数量。 */
        public Quantity reserved() {
            return reserved;
        }

        /** 返回已转采购订单数量。 */
        public Quantity ordered() {
            return ordered;
        }

        /** 返回已放弃数量。 */
        public Quantity cancelled() {
            return cancelled;
        }
    }
}
