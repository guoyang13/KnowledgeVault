package com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model;

/**
 * 跨上下文只传业务引用，不共享对方聚合对象。
 *
 * @param type 来源业务类型，例如 WAREHOUSE_RECEIPT
 * @param businessNo 来源系统中的稳定业务编号
 */
public record BusinessReference(String type, String businessNo) {

    /** 校验并创建跨上下文业务引用。 */
    public BusinessReference {
        DomainRuleViolation.require(type != null && !type.isBlank(), "引用类型不能为空");
        DomainRuleViolation.require(businessNo != null && !businessNo.isBlank(), "业务编号不能为空");
    }
}
