package com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 金额值对象。
 *
 * @param amount 金额数值
 * @param currency 币种
 */
public record Money(BigDecimal amount, String currency) {

    /** 校验币种并将金额统一为两位小数。 */
    public Money {
        Objects.requireNonNull(amount, "金额不能为空");
        DomainRuleViolation.require(currency != null && !currency.isBlank(), "币种不能为空");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 创建指定币种的零金额。
     */
    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /**
     * 将同币种金额相加。
     */
    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    /**
     * 扣减同币种金额。
     */
    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    /**
     * 按数量计算总金额并使用统一金额精度舍入。
     */
    public Money multiply(Quantity quantity) {
        return new Money(amount.multiply(quantity.value()), currency);
    }

    /** 校验参与运算的金额币种一致。 */
    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "待运算金额不能为空");
        DomainRuleViolation.require(currency.equals(other.currency), "币种不一致");
    }
}
