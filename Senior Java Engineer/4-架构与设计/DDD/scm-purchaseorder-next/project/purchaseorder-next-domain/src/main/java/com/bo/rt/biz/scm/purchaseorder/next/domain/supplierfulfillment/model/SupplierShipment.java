package com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.AbstractAggregateRoot;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.event.SupplierFulfillmentEvents;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 独立发运批次聚合。一张 PO 可以创建多个 SupplierShipment。
 */
public class SupplierShipment extends AbstractAggregateRoot {

    /** 发运批次聚合标识。 */
    private final String id;

    /** 供应商侧或平台生成的发运业务单号。 */
    private final String shipmentNo;

    /** 本批次消耗承诺数量所属的供应商履约单。 */
    private final String fulfillmentOrderId;

    /** 贯穿发运、物流、收货和质检的批次关联标识。 */
    private final String fulfillmentUnitId;

    /** 发起本次发运的供应商编码。 */
    private final String supplierCode;

    /** 本批次包含的订单行、数量和包装信息。 */
    private final List<ShipmentLine> lines;

    /** 发运通知自身的生命周期状态。 */
    private ShipmentStatus status;

    /** 下游仓储确认接收发运信息后返回的业务引用。 */
    private String warehouseReceiptReference;

    /** 校验发运行并创建处于草稿状态的供应商发运批次。 */
    private SupplierShipment(
            String id,
            String shipmentNo,
            String fulfillmentOrderId,
            String fulfillmentUnitId,
            String supplierCode,
            List<ShipmentLine> lines
    ) {
        require(id != null && !id.isBlank(), "发运批次 ID 不能为空");
        require(shipmentNo != null && !shipmentNo.isBlank(), "发运单号不能为空");
        require(fulfillmentOrderId != null && !fulfillmentOrderId.isBlank(), "履约单 ID 不能为空");
        require(fulfillmentUnitId != null && !fulfillmentUnitId.isBlank(), "履约单元 ID 不能为空");
        require(supplierCode != null && !supplierCode.isBlank(), "供应商不能为空");
        require(lines != null && !lines.isEmpty(), "发运批次至少包含一行");
        this.id = id;
        this.shipmentNo = shipmentNo;
        this.fulfillmentOrderId = fulfillmentOrderId;
        this.fulfillmentUnitId = fulfillmentUnitId;
        this.supplierCode = supplierCode;
        this.lines = List.copyOf(lines);
        this.status = ShipmentStatus.DRAFT;
        long eventVersion = nextVersion();
        raise(new SupplierFulfillmentEvents.SupplierShipmentNoticeCreated(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                shipmentNo,
                fulfillmentOrderId,
                fulfillmentUnitId
        ));
    }

    /**
     * 创建尚可修改或取消的供应商发运通知。
     */
    public static SupplierShipment createNotice(
            String id,
            String shipmentNo,
            String fulfillmentOrderId,
            String fulfillmentUnitId,
            String supplierCode,
            List<ShipmentLine> lines
    ) {
        return new SupplierShipment(
                id, shipmentNo, fulfillmentOrderId, fulfillmentUnitId, supplierCode, lines
        );
    }

    /**
     * 标记发运资料已经完整，可以接受发货门禁检查。
     */
    public void markReady() {
        require(status == ShipmentStatus.DRAFT, "只有草稿发运单可以就绪");
        status = ShipmentStatus.READY_TO_DISPATCH;
        long eventVersion = nextVersion();
        raise(new SupplierFulfillmentEvents.SupplierShipmentReady(
                UUID.randomUUID().toString(), id, eventVersion, Instant.now(), shipmentNo
        ));
    }

    /**
     * 在所有发货阻塞任务通过后确认实际发运并发布批次事件。
     */
    public void dispatch(boolean dispatchGatePassed) {
        require(status == ShipmentStatus.READY_TO_DISPATCH, "发运批次尚未就绪");
        require(dispatchGatePassed, "仍有阻塞发货的必做任务");
        status = ShipmentStatus.DISPATCHED;
        long eventVersion = nextVersion();
        raise(new SupplierFulfillmentEvents.SupplierShipmentDispatched(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                shipmentNo,
                fulfillmentOrderId,
                fulfillmentUnitId,
                supplierCode,
                lines.stream()
                        .map(line -> new SupplierFulfillmentEvents.ShipmentLineSnapshot(
                                line.purchaseOrderLineId(), line.shippedQuantity()
                        ))
                        .toList()
        ));
    }

    /**
     * 记录下游已经接受发运信息；该动作不等于仓库已经完成收货。
     */
    public void acknowledge(String warehouseReceiptReference) {
        require(status == ShipmentStatus.DISPATCHED, "只有已发运批次可以被下游确认");
        require(
                warehouseReceiptReference != null && !warehouseReceiptReference.isBlank(),
                "仓储收货引用不能为空"
        );
        this.warehouseReceiptReference = warehouseReceiptReference;
        status = ShipmentStatus.ACKNOWLEDGED;
        nextVersion();
    }

    /**
     * 取消尚未实际发出的发运通知。
     */
    public void cancel(String reason) {
        require(
                status == ShipmentStatus.DRAFT || status == ShipmentStatus.READY_TO_DISPATCH,
                "已发运批次不能直接取消"
        );
        require(reason != null && !reason.isBlank(), "取消原因不能为空");
        status = ShipmentStatus.CANCELLED;
        long eventVersion = nextVersion();
        raise(new SupplierFulfillmentEvents.SupplierShipmentCancelled(
                UUID.randomUUID().toString(), id, eventVersion, Instant.now(), shipmentNo, reason
        ));
    }

    /** 返回发运批次聚合标识。 */
    public String id() {
        return id;
    }

    /** 返回发运业务单号。 */
    public String shipmentNo() {
        return shipmentNo;
    }

    /** 返回供应商履约单标识。 */
    public String fulfillmentOrderId() {
        return fulfillmentOrderId;
    }

    /** 返回跨履约环节使用的批次标识。 */
    public String fulfillmentUnitId() {
        return fulfillmentUnitId;
    }

    /** 返回发运供应商编码。 */
    public String supplierCode() {
        return supplierCode;
    }

    /** 返回不可变发运行列表。 */
    public List<ShipmentLine> lines() {
        return lines;
    }

    /** 返回发运批次当前状态。 */
    public ShipmentStatus status() {
        return status;
    }

    /** 返回仓储收货引用；下游尚未确认时为空。 */
    public String warehouseReceiptReference() {
        return warehouseReceiptReference;
    }

    public enum ShipmentStatus {
        DRAFT,
        READY_TO_DISPATCH,
        DISPATCHED,
        ACKNOWLEDGED,
        CANCELLED
    }

    /**
     * 发运批次中的订单行数量。
     *
     * @param purchaseOrderLineId 来源采购订单行
     * @param shippedQuantity 本批次实际发运数量
     * @param packageInfo 包装信息
     */
    public record ShipmentLine(
            String purchaseOrderLineId,
            Quantity shippedQuantity,
            PackageInfo packageInfo
    ) {

        /** 校验并创建正数量发运行。 */
        public ShipmentLine {
            require(
                    purchaseOrderLineId != null && !purchaseOrderLineId.isBlank(),
                    "订单行 ID 不能为空"
            );
            require(shippedQuantity != null && shippedQuantity.isPositive(), "发运数量必须大于零");
            require(packageInfo != null, "包装信息不能为空");
        }
    }

    /**
     * 发运行包装信息。
     *
     * @param cartonCount 箱数
     * @param grossWeight 毛重文本及单位
     * @param volume 体积文本及单位
     */
    public record PackageInfo(int cartonCount, String grossWeight, String volume) {

        /** 校验并创建包装及物流信息。 */
        public PackageInfo {
            require(cartonCount > 0, "箱数必须大于零");
        }
    }
}
