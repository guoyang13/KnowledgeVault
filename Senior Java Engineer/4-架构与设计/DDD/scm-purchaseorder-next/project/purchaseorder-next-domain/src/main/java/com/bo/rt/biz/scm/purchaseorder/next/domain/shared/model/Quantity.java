package com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 数量值对象。
 *
 * @param value 数量数值
 * @param unit 单位
 */
public record Quantity(BigDecimal value, String unit) {

    /** 以“件”为最小计量单位的统一代码。 */
    public static final String PIECE = "PIECE";

    /** 校验数量非负并规范化单位与数值格式。 */
    public Quantity {
        Objects.requireNonNull(value, "数量不能为空");
        unit = requireText(unit, "数量单位不能为空");
        DomainRuleViolation.require(value.signum() >= 0, "数量不能为负数");
        value = value.stripTrailingZeros();
    }

    /**
     * 创建指定单位的零数量。
     */
    public static Quantity zero(String unit) {
        return new Quantity(BigDecimal.ZERO, unit);
    }

    /**
     * 创建以“件”为单位的整数数量。
     */
    public static Quantity pieces(long value) {
        return new Quantity(BigDecimal.valueOf(value), PIECE);
    }

    /**
     * 将同单位数量相加。
     */
    public Quantity add(Quantity other) {
        requireSameUnit(other);
        return new Quantity(value.add(other.value), unit);
    }

    /**
     * 扣减同单位数量，结果不允许为负数。
     */
    public Quantity subtract(Quantity other) {
        requireSameUnit(other);
        DomainRuleViolation.require(value.compareTo(other.value) >= 0, "数量扣减后不能为负数");
        return new Quantity(value.subtract(other.value), unit);
    }

    /**
     * 判断数量是否大于零。
     */
    public boolean isPositive() {
        return value.signum() > 0;
    }

    /**
     * 判断数量是否为零。
     */
    public boolean isZero() {
        return value.signum() == 0;
    }

    /**
     * 比较两个同单位数量的大小。
     */
    public boolean lessThanOrEqualTo(Quantity other) {
        requireSameUnit(other);
        return value.compareTo(other.value) <= 0;
    }

    /** 校验参与运算的数量单位一致。 */
    private void requireSameUnit(Quantity other) {
        Objects.requireNonNull(other, "待运算数量不能为空");
        DomainRuleViolation.require(unit.equals(other.unit), "数量单位不一致");
    }

    /** 校验必填文本并返回原值。 */
    private static String requireText(String value, String message) {
        DomainRuleViolation.require(value != null && !value.isBlank(), message);
        return value;
    }
}
