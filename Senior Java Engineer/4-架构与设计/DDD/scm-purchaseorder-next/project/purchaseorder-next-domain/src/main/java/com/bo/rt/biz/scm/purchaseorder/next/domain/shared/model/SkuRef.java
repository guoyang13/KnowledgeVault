package com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model;

/**
 * SKU 引用值对象。
 *
 * @param skuCode SKU 编码
 */
public record SkuRef(String skuCode) {

    /** 校验并创建 SKU 引用。 */
    public SkuRef {
        DomainRuleViolation.require(skuCode != null && !skuCode.isBlank(), "SKU 编码不能为空");
    }
}
