package com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoute.DeliveryMethod;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoute.SupplierRegion;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoute.TransitNode;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Destination;

/**
 * 采购路线领域策略。
 *
 * <p>应用层负责从供应商和地点目录取得可信事实，本策略负责校验交付条款并生成不可变路线快照。</p>
 */
public class ProcurementRoutePolicy {

    /** 路线规则发生业务变化时升级该版本，便于解释历史订单。 */
    public static final String POLICY_VERSION = "PROCUREMENT-ROUTE-2026.01";

    /**
     * 根据可信路线事实生成采购路线。
     *
     * @param facts 供应商、交付方式、中转节点和目的地事实
     * @param deliveryTerms 订单贸易与运输责任条款
     * @return 已完成规则校验的采购路线
     */
    public ProcurementRoute determine(RouteFacts facts, PurchaseOrder.DeliveryTerms deliveryTerms) {
        require(facts != null, "路线事实不能为空");
        require(deliveryTerms != null, "交付条款不能为空");
        validateDeliveryTerms(deliveryTerms);
        return new ProcurementRoute(
                facts.supplierRegion(),
                facts.deliveryMethod(),
                facts.transitNode(),
                facts.destination(),
                POLICY_VERSION
        );
    }

    /**
     * DDP 表示供应商承担将货物交到指定目的地的运输责任，不能同时声明供应商不安排运输。
     */
    private void validateDeliveryTerms(PurchaseOrder.DeliveryTerms deliveryTerms) {
        if ("DDP".equalsIgnoreCase(deliveryTerms.incoterm())) {
            require(
                    deliveryTerms.supplierArrangesTransport(),
                    "DDP 条款下必须由供应商安排运输"
            );
        }
    }

    /**
     * 路线计算输入。
     *
     * @param supplierRegion 供应商所在地
     * @param deliveryMethod 供应商直送或集货中转
     * @param transitNode 中转节点；直送时为空
     * @param destination 最终目的地
     */
    public record RouteFacts(
            SupplierRegion supplierRegion,
            DeliveryMethod deliveryMethod,
            TransitNode transitNode,
            Destination destination
    ) {
    }
}
