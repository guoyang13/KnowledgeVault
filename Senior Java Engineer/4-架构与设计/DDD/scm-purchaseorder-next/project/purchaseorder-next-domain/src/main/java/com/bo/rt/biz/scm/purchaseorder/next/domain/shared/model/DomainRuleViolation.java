package com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model;

/**
 * 领域不变量被破坏时抛出的异常。
 */
public class DomainRuleViolation extends RuntimeException {

    /** Java 序列化版本，仅用于异常跨边界传递时保持兼容。 */
    private static final long serialVersionUID = 1L;

    /**
     * 使用明确的业务规则说明创建异常。
     */
    public DomainRuleViolation(String message) {
        super(message);
    }

    /**
     * 断言领域条件成立，不成立时抛出领域规则异常。
     */
    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new DomainRuleViolation(message);
        }
    }
}
