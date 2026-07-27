package com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model;

import java.util.Locale;

/**
 * 采购履约目的地。
 *
 * @param type 目的地类型
 * @param code 仓、店或客户目的地编码
 * @param countryCode ISO 3166-1 alpha-2 国家或地区编码
 */
public record Destination(
        DestinationType type,
        String code,
        String countryCode
) {

    /** 校验并规范化采购履约目的地。 */
    public Destination {
        DomainRuleViolation.require(type != null, "目的地类型不能为空");
        DomainRuleViolation.require(code != null && !code.isBlank(), "目的地编码不能为空");
        DomainRuleViolation.require(
                countryCode != null && !countryCode.isBlank(),
                "目的地国家或地区不能为空"
        );
        code = code.trim();
        countryCode = countryCode.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 兼容只表达目的仓的早期模型。
     *
     * <p>该构造器不适用于路线计算，因为它缺少目的地国家或地区。</p>
     */
    public Destination(String warehouseCode) {
        this(DestinationType.WAREHOUSE, warehouseCode, "UNKNOWN");
    }

    /** 返回仓库编码；非仓库目的地不能使用该兼容访问器。 */
    public String warehouseCode() {
        DomainRuleViolation.require(type == DestinationType.WAREHOUSE, "当前目的地不是仓库");
        return code;
    }

    /** 采购订单支持的目的地类型。 */
    public enum DestinationType {
        WAREHOUSE,
        STORE,
        CUSTOMER
    }
}
