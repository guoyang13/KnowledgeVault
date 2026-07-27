package com.bo.rt.biz.scm.purchaseorder.next.scenario;

import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoute;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoute.DeliveryMethod;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoute.RouteType;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoute.SupplierRegion;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoute.TransitNode;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoutePolicy;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoutePolicy.RouteFacts;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.PurchaseOrder.DeliveryTerms;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Destination;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Destination.DestinationType;

/**
 * 可执行伪代码：验证采购路线的计算规则和关键不变量。
 */
public class ProcurementRoutePolicyScenario {

    private final ProcurementRoutePolicy policy = new ProcurementRoutePolicy();

    /** 直接运行四种路线和非法组合测试。 */
    public static void main(String[] args) {
        new ProcurementRoutePolicyScenario().run();
        System.out.println("Procurement route policy passed.");
    }

    /** 执行采购路线策略场景。 */
    public void run() {
        shouldCalculateDomesticDirect();
        shouldCalculateDomesticTransit();
        shouldCalculateCrossBorderDirect();
        shouldCalculateCrossBorderTransit();
        shouldRejectMissingTransitNode();
        shouldRejectTransitNodeForDirectDelivery();
        shouldRejectContradictoryDdpResponsibility();
    }

    /** 同一国家且不经过中转节点时计算为国内直送。 */
    private void shouldCalculateDomesticDirect() {
        ProcurementRoute route = determine(
                "CN",
                DeliveryMethod.SUPPLIER_DIRECT,
                null,
                destination(DestinationType.WAREHOUSE, "WH-SHANGHAI", "CN"),
                new DeliveryTerms("DDP", true, false)
        );
        check(route.routeType() == RouteType.DOMESTIC_DIRECT, "应计算为国内直送");
        check(!route.crossBorder(), "国内直送不应标记为跨境");
        check(!route.requiresTransit(), "国内直送不应要求中转");
    }

    /** 同一国家且经过集货点时计算为国内中转。 */
    private void shouldCalculateDomesticTransit() {
        ProcurementRoute route = determine(
                "ID",
                DeliveryMethod.COLLECTION_AND_TRANSFER,
                new TransitNode("HUB-JAKARTA", "ID"),
                destination(DestinationType.STORE, "STORE-BALI-01", "ID"),
                new DeliveryTerms("DAP", false, false)
        );
        check(route.routeType() == RouteType.DOMESTIC_TRANSIT, "应计算为国内中转");
        check(route.requiresTransit(), "国内中转必须保留中转节点");
    }

    /** 供应商和目的地位于不同国家且直送时计算为跨境直送。 */
    private void shouldCalculateCrossBorderDirect() {
        ProcurementRoute route = determine(
                "CN",
                DeliveryMethod.SUPPLIER_DIRECT,
                null,
                destination(DestinationType.CUSTOMER, "CUSTOMER-ID-001", "ID"),
                new DeliveryTerms("DDP", true, false)
        );
        check(route.routeType() == RouteType.CROSS_BORDER_DIRECT, "应计算为跨境直送");
        check(route.crossBorder(), "跨境直送应标记为跨境");
    }

    /** 跨国家且经过集货中转时计算为跨境中转。 */
    private void shouldCalculateCrossBorderTransit() {
        ProcurementRoute route = determine(
                "CN",
                DeliveryMethod.COLLECTION_AND_TRANSFER,
                new TransitNode("TRANSIT-SHENZHEN", "CN"),
                destination(DestinationType.WAREHOUSE, "WH-JAKARTA", "ID"),
                new DeliveryTerms("FOB", false, false)
        );
        check(route.routeType() == RouteType.CROSS_BORDER_TRANSIT, "应计算为跨境中转");
        check(
                "TRANSIT-SHENZHEN".equals(route.transitNode().code()),
                "跨境中转应保留中转节点快照"
        );
    }

    /** 集货中转缺少中转节点时必须拒绝创建路线。 */
    private void shouldRejectMissingTransitNode() {
        expectFailure(() -> determine(
                "CN",
                DeliveryMethod.COLLECTION_AND_TRANSFER,
                null,
                destination(DestinationType.WAREHOUSE, "WH-JAKARTA", "ID"),
                new DeliveryTerms("FOB", false, false)
        ), "集货中转路线必须设置中转节点");
    }

    /** 供应商直送却携带中转节点时必须拒绝矛盾输入。 */
    private void shouldRejectTransitNodeForDirectDelivery() {
        expectFailure(() -> determine(
                "CN",
                DeliveryMethod.SUPPLIER_DIRECT,
                new TransitNode("TRANSIT-SHENZHEN", "CN"),
                destination(DestinationType.WAREHOUSE, "WH-JAKARTA", "ID"),
                new DeliveryTerms("DDP", true, false)
        ), "供应商直送路线不能设置中转节点");
    }

    /** DDP 与“供应商不安排运输”同时出现时必须拒绝。 */
    private void shouldRejectContradictoryDdpResponsibility() {
        expectFailure(() -> determine(
                "CN",
                DeliveryMethod.SUPPLIER_DIRECT,
                null,
                destination(DestinationType.WAREHOUSE, "WH-SHANGHAI", "CN"),
                new DeliveryTerms("DDP", false, false)
        ), "DDP 条款下必须由供应商安排运输");
    }

    /** 调用路线策略并返回计算结果。 */
    private ProcurementRoute determine(
            String supplierCountryCode,
            DeliveryMethod deliveryMethod,
            TransitNode transitNode,
            Destination destination,
            DeliveryTerms deliveryTerms
    ) {
        return policy.determine(
                new RouteFacts(
                        new SupplierRegion(supplierCountryCode),
                        deliveryMethod,
                        transitNode,
                        destination
                ),
                deliveryTerms
        );
    }

    /** 创建目的地测试值对象。 */
    private Destination destination(
            DestinationType type,
            String code,
            String countryCode
    ) {
        return new Destination(type, code, countryCode);
    }

    /** 断言代码块必须以给定业务消息失败。 */
    private void expectFailure(Runnable action, String expectedMessage) {
        try {
            action.run();
            throw new AssertionError("预期失败但执行成功: " + expectedMessage);
        } catch (RuntimeException exception) {
            check(
                    expectedMessage.equals(exception.getMessage()),
                    "异常消息不符合预期: " + exception.getMessage()
            );
        }
    }

    /** 最小场景断言。 */
    private void check(boolean expression, String message) {
        if (!expression) {
            throw new AssertionError(message);
        }
    }
}
