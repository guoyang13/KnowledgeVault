package com.bo.rt.biz.scm.purchaseorder.next.application.query;

import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.event.OrderingEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Money;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 面向采购订单详情页的反规范化读模型。
 */
public class PurchaseOrderExecutionView {

    /** 采购订单标识。 */
    private final String purchaseOrderId;
    /** 采购订单号。 */
    private final String orderNo;
    /** 供应商编码。 */
    private final String supplierCode;
    /** 采购主体标识。 */
    private final String buyerOrganizationId;
    /** 仓、店或客户目的地类型。 */
    private final String destinationType;
    /** 仓、店或客户目的地编码。 */
    private final String destinationCode;
    /** 国内或跨境、直送或中转的采购路线分类。 */
    private final String routeType;
    /** 订单币种。 */
    private final String currency;
    /** 采购订单含税总金额。 */
    private final Money orderAmount;
    /** 采购订单行静态快照。 */
    private final List<LineView> lines;
    /** 各来源聚合已处理的最高版本，用于隔离聚合内乱序与重复事件。 */
    private final Map<String, Long> sourceAggregateVersions = new HashMap<>();
    /** 已发货批次号集合。 */
    private final Set<String> shipmentNos = new HashSet<>();
    /** 已创建的履约执行任务标识集合。 */
    private final Set<String> createdTaskIds = new HashSet<>();
    /** 已完成或豁免的履约执行任务标识集合。 */
    private final Set<String> finishedTaskIds = new HashSet<>();
    /** 按订单行累计的质检合格数量。 */
    private final Map<String, Quantity> acceptedQuantities = new HashMap<>();
    /** 按订单行累计的质检不合格数量。 */
    private final Map<String, Quantity> rejectedQuantities = new HashMap<>();
    /** 采购订单当前状态。 */
    private String orderStatus = "EFFECTIVE";
    /** 采购订单当前审批状态。 */
    private String approvalStatus = "APPROVED";
    /** 供应商履约总体状态。 */
    private String fulfillmentStatus = "WAITING_CONFIRMATION";
    /** 跨上下文事件推导出的最远履约阶段。 */
    private String overallStage = "ORDER_EFFECTIVE";
    /** 质量检验总体状态。 */
    private String qualityStatus = "NOT_STARTED";
    /** 采购结算总体状态。 */
    private String settlementStatus = "WAITING_FOR_BASIS";
    /** 当前结算计算版本号。 */
    private int settlementRevision;
    /** 当前结算版本的应付金额。 */
    private Money settlementPayable;
    /** 要求计划中需要执行的任务数量。 */
    private int plannedRequiredTaskCount;
    /** 尚未解决的履约异常数量。 */
    private int openExceptionCount;
    /** 当前未解决异常中的最高严重级别。 */
    private String highestExceptionSeverity;

    /** 根据采购订单生效事件建立详情读模型的初始快照。 */
    private PurchaseOrderExecutionView(OrderingEvents.PurchaseOrderEffective event) {
        this.purchaseOrderId = event.aggregateId();
        this.orderNo = event.orderNo();
        this.supplierCode = event.supplierCode();
        this.buyerOrganizationId = event.buyerOrganizationId();
        this.destinationType = event.route().destinationType();
        this.destinationCode = event.route().destinationCode();
        this.routeType = event.route().routeType();
        this.currency = event.currency();
        this.lines = event.lines().stream()
                .map(line -> new LineView(
                        line.purchaseOrderLineId(),
                        line.legacyPsoCode(),
                        line.skuCode(),
                        line.orderedQuantity(),
                        line.taxedUnitPrice()
                ))
                .toList();
        this.orderAmount = event.lines().stream()
                .map(line -> line.taxedUnitPrice().multiply(line.orderedQuantity()))
                .reduce(Money.zero(event.currency()), Money::add);
        sourceAggregateVersions.put(event.aggregateId(), event.aggregateVersion());
    }

    /** 从采购订单生效事件创建采购执行视图。 */
    public static PurchaseOrderExecutionView from(OrderingEvents.PurchaseOrderEffective event) {
        return new PurchaseOrderExecutionView(event);
    }

    /**
     * 不同聚合的事件没有全局顺序，因此按 sourceAggregateId 分别维护版本水位。
     *
     * @return 事件版本高于当前来源聚合水位时返回 {@code true}
     */
    public boolean acceptVersion(DomainEvent event) {
        long current = sourceAggregateVersions.getOrDefault(event.aggregateId(), 0L);
        if (event.aggregateVersion() <= current) {
            return false;
        }
        sourceAggregateVersions.put(event.aggregateId(), event.aggregateVersion());
        return true;
    }

    /** 更新采购订单与审批状态；空值表示该项保持不变。 */
    public void updateOrder(String orderStatus, String approvalStatus) {
        if (orderStatus != null) {
            this.orderStatus = orderStatus;
        }
        if (approvalStatus != null) {
            this.approvalStatus = approvalStatus;
        }
    }

    /** 更新供应商履约总体状态。 */
    public void updateFulfillment(String fulfillmentStatus) {
        this.fulfillmentStatus = fulfillmentStatus;
    }

    /** 幂等记录一个已发货批次并推进总体阶段。 */
    public void recordShipment(String shipmentNo) {
        shipmentNos.add(shipmentNo);
        fulfillmentStatus = "IN_EXECUTION";
        advanceStage("DISPATCHED");
    }

    /** 记录质检开始并推进总体阶段。 */
    public void recordQualityStarted() {
        qualityStatus = "IN_PROGRESS";
        advanceStage("QUALITY_INSPECTION");
    }

    /** 记录质检完成。 */
    public void recordQualityCompleted() {
        qualityStatus = "COMPLETED";
        advanceStage("QUALITY_INSPECTION");
    }

    /** 累加一个订单行的质检合格数量。 */
    public void addAcceptedQuantity(String lineId, Quantity quantity) {
        acceptedQuantities.merge(lineId, quantity, Quantity::add);
    }

    /** 累加一个订单行的质检不合格数量。 */
    public void addRejectedQuantity(String lineId, Quantity quantity) {
        rejectedQuantities.merge(lineId, quantity, Quantity::add);
    }

    /** 更新结算状态和可用的版本、金额信息。 */
    public void updateSettlement(String status, int revisionNo, Money totalPayable) {
        settlementStatus = status;
        if (revisionNo > 0) {
            settlementRevision = revisionNo;
        }
        if (totalPayable != null) {
            settlementPayable = totalPayable;
        }
        if ("CONFIRMED".equals(status) || "AP_ACCEPTED".equals(status) || "SETTLED".equals(status)) {
            advanceStage("SETTLEMENT");
        }
    }

    /** 记录要求计划中必须执行的任务总量。 */
    public void registerRequirementPlan(int requiredCount) {
        plannedRequiredTaskCount = Math.max(plannedRequiredTaskCount, requiredCount);
    }

    /** 幂等登记新建的履约执行任务。 */
    public void taskCreated(String taskId) {
        createdTaskIds.add(taskId);
    }

    /** 幂等登记已完成或豁免的履约执行任务。 */
    public void taskFinished(String taskId) {
        createdTaskIds.add(taskId);
        finishedTaskIds.add(taskId);
    }

    /** 登记一个新异常并更新最高严重级别。 */
    public void exceptionOpened(String severity) {
        openExceptionCount++;
        if (highestExceptionSeverity == null
                || severityRank(severity) > severityRank(highestExceptionSeverity)) {
            highestExceptionSeverity = severity;
        }
    }

    /** 登记一个异常已解决，未关闭数量不会降到零以下。 */
    public void exceptionResolved() {
        openExceptionCount = Math.max(0, openExceptionCount - 1);
    }

    /** 只允许总体履约阶段向前推进。 */
    private void advanceStage(String target) {
        List<String> stages = List.of(
                "ORDER_EFFECTIVE",
                "SUPPLIER_CONFIRMED",
                "DISPATCHED",
                "RECEIVED",
                "QUALITY_INSPECTION",
                "INBOUNDED",
                "SETTLEMENT",
                "COMPLETED"
        );
        if (stages.indexOf(target) > stages.indexOf(overallStage)) {
            overallStage = target;
        }
    }

    /** 将异常级别转换为便于比较的顺序值。 */
    private int severityRank(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    /** 生成供查询接口返回的不可变详情快照。 */
    public DetailSnapshot snapshot() {
        int requiredTaskCount = Math.max(plannedRequiredTaskCount, createdTaskIds.size());
        int completedTaskCount = finishedTaskIds.size();
        return new DetailSnapshot(
                purchaseOrderId,
                orderNo,
                supplierCode,
                buyerOrganizationId,
                destinationType,
                destinationCode,
                routeType,
                currency,
                orderAmount,
                lines,
                orderStatus,
                approvalStatus,
                fulfillmentStatus,
                overallStage,
                qualityStatus,
                settlementStatus,
                settlementRevision,
                settlementPayable,
                requiredTaskCount,
                completedTaskCount,
                Math.max(0, requiredTaskCount - completedTaskCount),
                openExceptionCount,
                highestExceptionSeverity,
                new ArrayList<>(shipmentNos),
                Map.copyOf(acceptedQuantities),
                Map.copyOf(rejectedQuantities)
        );
    }

    /** 返回采购订单标识。 */
    public String purchaseOrderId() {
        return purchaseOrderId;
    }

    /** 返回采购订单号。 */
    public String orderNo() {
        return orderNo;
    }

    /**
     * 采购订单行查询快照。
     *
     * @param purchaseOrderLineId 采购订单行标识
     * @param legacyPsoCode 兼容旧系统的 PSO 编码
     * @param skuCode 商品 SKU 编码
     * @param orderedQuantity 下单数量
     * @param taxedUnitPrice 含税采购单价
     */
    public record LineView(
            String purchaseOrderLineId,
            String legacyPsoCode,
            String skuCode,
            Quantity orderedQuantity,
            Money taxedUnitPrice
    ) {
    }

    /**
     * 采购订单详情页使用的完整不可变快照。
     *
     * @param purchaseOrderId 采购订单标识
     * @param orderNo 采购订单号
     * @param supplierCode 供应商编码
     * @param buyerOrganizationId 采购主体标识
     * @param destinationType 目的地类型
     * @param destinationCode 目的地编码
     * @param routeType 采购路线分类
     * @param currency 币种
     * @param orderAmount 订单含税总金额
     * @param lines 订单行快照
     * @param orderStatus 订单状态
     * @param approvalStatus 审批状态
     * @param fulfillmentStatus 供应商履约状态
     * @param overallStage 总体履约阶段
     * @param qualityStatus 质量检验状态
     * @param settlementStatus 采购结算状态
     * @param settlementRevision 当前结算版本
     * @param settlementPayable 当前应付金额
     * @param requiredTaskCount 应执行任务数
     * @param completedTaskCount 已完成或豁免任务数
     * @param blockingTaskCount 尚未完成的阻塞任务数
     * @param openExceptionCount 未解决异常数
     * @param highestExceptionSeverity 最高异常严重级别
     * @param shipmentNos 已发货批次号
     * @param acceptedQuantities 各订单行累计合格数量
     * @param rejectedQuantities 各订单行累计不合格数量
     */
    public record DetailSnapshot(
            String purchaseOrderId,
            String orderNo,
            String supplierCode,
            String buyerOrganizationId,
            String destinationType,
            String destinationCode,
            String routeType,
            String currency,
            Money orderAmount,
            List<LineView> lines,
            String orderStatus,
            String approvalStatus,
            String fulfillmentStatus,
            String overallStage,
            String qualityStatus,
            String settlementStatus,
            int settlementRevision,
            Money settlementPayable,
            int requiredTaskCount,
            int completedTaskCount,
            int blockingTaskCount,
            int openExceptionCount,
            String highestExceptionSeverity,
            List<String> shipmentNos,
            Map<String, Quantity> acceptedQuantities,
            Map<String, Quantity> rejectedQuantities
    ) {

        /** 固化详情中的集合和映射。 */
        public DetailSnapshot {
            lines = List.copyOf(lines);
            shipmentNos = List.copyOf(shipmentNos);
            acceptedQuantities = Map.copyOf(acceptedQuantities);
            rejectedQuantities = Map.copyOf(rejectedQuantities);
        }
    }
}
