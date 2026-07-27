package com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model;

/**
 * 供应商引用值对象。
 *
 * @param supplierCode 供应商编码
 */
public record SupplierRef(String supplierCode) {

    /** 校验并创建供应商引用。 */
    public SupplierRef {
        DomainRuleViolation.require(
                supplierCode != null && !supplierCode.isBlank(),
                "供应商编码不能为空"
        );
    }
}
