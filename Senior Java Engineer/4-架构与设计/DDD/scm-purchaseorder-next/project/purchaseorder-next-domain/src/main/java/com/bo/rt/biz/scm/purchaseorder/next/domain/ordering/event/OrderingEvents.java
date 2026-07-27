package com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.event;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.event.DomainEvent;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Money;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购订单上下文发布的商业承诺事实。
 *
 * <p>所有事件的 {@code eventId} 用于消费幂等，{@code aggregateId} 指向采购订单或变更聚合，
 * {@code aggregateVersion} 用于乱序保护，{@code occurredAt} 表示事实发生时间。</p>
 */
public final class OrderingEvents {

    /** 事件容器不允许实例化。 */
    private OrderingEvents() {
    }

    /**
     * 随 PO 生效事件发布的订单行商业快照。
     *
     * @param purchaseOrderLineId 新模型订单行标识
     * @param legacyPsoCode 迁移期旧 PSO 编码
     * @param requisitionLineId 来源采购申请行
     * @param skuCode SKU 编码
     * @param orderedQuantity 商业承诺数量
     * @param taxedUnitPrice 含税采购单价
     * @param expectedArrivalDate 预计到货日期
     */
    public record OrderLineSnapshot(
            String purchaseOrderLineId,
            String legacyPsoCode,
            String requisitionLineId,
            String skuCode,
            Quantity orderedQuantity,
            Money taxedUnitPrice,
            LocalDate expectedArrivalDate
    ) {
    }

    /**
     * 随订单生效事件发布的采购路线快照。
     *
     * @param routeType 国内或跨境、直送或中转的路线分类
     * @param supplierCountryCode 供应商所在国家或地区
     * @param supplierRegionType 中国大陆或境外业务分组
     * @param deliveryMethod 供应商直送或集货中转
     * @param transitNodeCode 中转节点编码；直送时为空
     * @param transitCountryCode 中转节点国家或地区；直送时为空
     * @param destinationType 最终目的地类型
     * @param destinationCode 最终目的地编码
     * @param destinationCountryCode 最终目的地国家或地区
     * @param policyVersion 路线规则版本
     */
    public record ProcurementRouteSnapshot(
            String routeType,
            String supplierCountryCode,
            String supplierRegionType,
            String deliveryMethod,
            String transitNodeCode,
            String transitCountryCode,
            String destinationType,
            String destinationCode,
            String destinationCountryCode,
            String policyVersion
    ) {
    }

    /**
     * 采购订单草稿已经创建。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购订单聚合标识
     * @param aggregateVersion 采购订单事件版本
     * @param occurredAt 事实发生时间
     * @param orderNo 采购订单号
     * @param supplierCode 供应商编码
     */
    public record PurchaseOrderDraftCreated(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String orderNo,
            String supplierCode
    ) implements DomainEvent {
    }

    /**
     * 采购订单已经绑定审批实例和提交版本。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购订单聚合标识
     * @param aggregateVersion 采购订单事件版本
     * @param occurredAt 事实发生时间
     * @param orderNo 采购订单号
     * @param approvalInstanceId 审批实例标识
     * @param submittedOrderVersion 提交审批时冻结的订单版本
     */
    public record PurchaseOrderSubmitted(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String orderNo,
            String approvalInstanceId,
            long submittedOrderVersion
    ) implements DomainEvent {
    }

    /**
     * 当前采购订单审批已经撤回。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购订单聚合标识
     * @param aggregateVersion 采购订单事件版本
     * @param occurredAt 事实发生时间
     * @param orderNo 采购订单号
     */
    public record PurchaseOrderApprovalWithdrawn(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String orderNo
    ) implements DomainEvent {
    }

    /**
     * 采购订单审批被驳回。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购订单聚合标识
     * @param aggregateVersion 采购订单事件版本
     * @param occurredAt 事实发生时间
     * @param orderNo 采购订单号
     * @param reason 驳回原因
     */
    public record PurchaseOrderRejected(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String orderNo,
            String reason
    ) implements DomainEvent {
    }

    /**
     * 采购订单商业承诺正式生效，是下游上下文的启动事件。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购订单聚合标识
     * @param aggregateVersion 采购订单事件版本
     * @param occurredAt 事实发生时间
     * @param orderNo 采购订单号
     * @param supplierCode 供应商编码
     * @param buyerOrganizationId 采购主体标识
     * @param route 采购路线事实与计算结论快照
     * @param currency 订单币种
     * @param lines 生效的订单行商业快照
     */
    public record PurchaseOrderEffective(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String orderNo,
            String supplierCode,
            String buyerOrganizationId,
            ProcurementRouteSnapshot route,
            String currency,
            List<OrderLineSnapshot> lines
    ) implements DomainEvent {

        /** 固化事件中的采购订单行快照。 */
        public PurchaseOrderEffective {
            lines = List.copyOf(lines);
        }
    }

    /**
     * 尚未生效的采购订单已经取消。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购订单聚合标识
     * @param aggregateVersion 采购订单事件版本
     * @param occurredAt 事实发生时间
     * @param orderNo 采购订单号
     * @param reason 取消原因
     */
    public record PurchaseOrderCancelled(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String orderNo,
            String reason
    ) implements DomainEvent {
    }

    /**
     * 已审批的订单行交期变更已经生效。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购订单聚合标识
     * @param aggregateVersion 采购订单事件版本
     * @param occurredAt 事实发生时间
     * @param orderNo 采购订单号
     * @param lineId 发生变更的采购订单行标识
     * @param expectedArrivalDate 变更后的预计到货日期
     * @param changeId 已审批的订单变更标识
     */
    public record PurchaseOrderDeliveryDateChanged(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String orderNo,
            String lineId,
            LocalDate expectedArrivalDate,
            String changeId
    ) implements DomainEvent {
    }

    /**
     * 已审批的订单行价格调整已经生效。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购订单聚合标识
     * @param aggregateVersion 采购订单事件版本
     * @param occurredAt 事实发生时间
     * @param orderNo 采购订单号
     * @param lineId 发生调价的采购订单行标识
     * @param taxedUnitPrice 调整后的含税单价
     * @param changeId 已审批的订单变更标识
     */
    public record PurchaseOrderPriceAdjusted(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String orderNo,
            String lineId,
            Money taxedUnitPrice,
            String changeId
    ) implements DomainEvent {
    }

    /**
     * 生效订单已经进入终止处理流程。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购订单聚合标识
     * @param aggregateVersion 采购订单事件版本
     * @param occurredAt 事实发生时间
     * @param orderNo 采购订单号
     * @param changeId 终止变更标识
     */
    public record PurchaseOrderTerminationRequested(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String orderNo,
            String changeId
    ) implements DomainEvent {
    }

    /**
     * 采购订单的剩余商业承诺已经终止。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购订单聚合标识
     * @param aggregateVersion 采购订单事件版本
     * @param occurredAt 事实发生时间
     * @param orderNo 采购订单号
     * @param reason 终止原因
     */
    public record PurchaseOrderTerminated(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String orderNo,
            String reason
    ) implements DomainEvent {
    }

    /**
     * 采购订单的商业与履约责任已经完成。
     *
     * @param eventId 事件唯一标识
     * @param aggregateId 采购订单聚合标识
     * @param aggregateVersion 采购订单事件版本
     * @param occurredAt 事实发生时间
     * @param orderNo 采购订单号
     */
    public record PurchaseOrderCompleted(
            String eventId,
            String aggregateId,
            long aggregateVersion,
            Instant occurredAt,
            String orderNo
    ) implements DomainEvent {
    }
}
