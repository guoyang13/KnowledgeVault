package com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Destination;
import java.util.Locale;

/**
 * 采购路线值对象。
 *
 * <p>路线保存用于审计的原始事实，路线类型由供应商和目的地是否跨境、是否经过中转节点推导，
 * 不能由接口调用方直接指定。</p>
 *
 * @param supplierRegion 供应商所在地
 * @param deliveryMethod 供应商直送或集货中转
 * @param transitNode 中转节点；直送路线为空
 * @param destination 最终目的地
 * @param policyVersion 生成该路线的规则版本
 */
public record ProcurementRoute(
        SupplierRegion supplierRegion,
        DeliveryMethod deliveryMethod,
        TransitNode transitNode,
        Destination destination,
        String policyVersion
) {

    /** 校验路线事实之间不存在矛盾。 */
    public ProcurementRoute {
        require(supplierRegion != null, "供应商地区不能为空");
        require(deliveryMethod != null, "交付方式不能为空");
        require(destination != null, "目的地不能为空");
        require(policyVersion != null && !policyVersion.isBlank(), "路线策略版本不能为空");
        require(
                !"UNKNOWN".equals(supplierRegion.countryCode()),
                "路线计算必须提供供应商国家或地区"
        );
        require(
                !"UNKNOWN".equals(destination.countryCode()),
                "路线计算必须提供目的地国家或地区"
        );
        if (deliveryMethod == DeliveryMethod.SUPPLIER_DIRECT) {
            require(transitNode == null, "供应商直送路线不能设置中转节点");
        } else {
            require(transitNode != null, "集货中转路线必须设置中转节点");
            require(
                    !"UNKNOWN".equals(transitNode.countryCode()),
                    "路线计算必须提供中转节点国家或地区"
            );
            require(
                    !transitNode.code().equals(destination.code()),
                    "中转节点不能与最终目的地相同"
            );
        }
        policyVersion = policyVersion.trim();
    }

    /** 根据起点、终点和中转方式计算稳定的路线分类。 */
    public RouteType routeType() {
        if (crossBorder()) {
            return requiresTransit()
                    ? RouteType.CROSS_BORDER_TRANSIT
                    : RouteType.CROSS_BORDER_DIRECT;
        }
        return requiresTransit()
                ? RouteType.DOMESTIC_TRANSIT
                : RouteType.DOMESTIC_DIRECT;
    }

    /** 判断供应商所在地与最终目的地是否跨国家或地区。 */
    public boolean crossBorder() {
        return !supplierRegion.countryCode().equals(destination.countryCode());
    }

    /** 判断货物是否必须先经过中转节点。 */
    public boolean requiresTransit() {
        return deliveryMethod == DeliveryMethod.COLLECTION_AND_TRANSFER;
    }

    /**
     * 供应商地区。
     *
     * @param countryCode ISO 3166-1 alpha-2 国家或地区编码
     */
    public record SupplierRegion(String countryCode) {

        /** 校验并规范化供应商国家或地区。 */
        public SupplierRegion {
            require(countryCode != null && !countryCode.isBlank(), "供应商国家或地区不能为空");
            countryCode = countryCode.trim().toUpperCase(Locale.ROOT);
        }

        /** 返回面向现有采购业务的中国大陆或境外分组。 */
        public RegionType regionType() {
            return "CN".equals(countryCode)
                    ? RegionType.CHINA_MAINLAND
                    : RegionType.OVERSEAS;
        }
    }

    /**
     * 中转节点。
     *
     * @param code 中转仓或集货点编码
     * @param countryCode 中转节点所在国家或地区
     */
    public record TransitNode(String code, String countryCode) {

        /** 校验并规范化中转节点。 */
        public TransitNode {
            require(code != null && !code.isBlank(), "中转节点编码不能为空");
            require(
                    countryCode != null && !countryCode.isBlank(),
                    "中转节点国家或地区不能为空"
            );
            code = code.trim();
            countryCode = countryCode.trim().toUpperCase(Locale.ROOT);
        }
    }

    /** 供应商与目的地之间的交付方式。 */
    public enum DeliveryMethod {
        SUPPLIER_DIRECT,
        COLLECTION_AND_TRANSFER
    }

    /** 由路线事实推导出的四类采购路线。 */
    public enum RouteType {
        DOMESTIC_DIRECT,
        DOMESTIC_TRANSIT,
        CROSS_BORDER_DIRECT,
        CROSS_BORDER_TRANSIT
    }

    /** 供应商所在地的业务分组。 */
    public enum RegionType {
        CHINA_MAINLAND,
        OVERSEAS
    }
}
