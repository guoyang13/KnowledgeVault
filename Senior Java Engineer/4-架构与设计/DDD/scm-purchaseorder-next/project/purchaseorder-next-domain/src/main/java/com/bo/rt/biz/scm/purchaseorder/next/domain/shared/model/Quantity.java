package com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model;

import java.math.BigDecimal;

/**
 * 数量值对象。
 *
 * @param value 数量数值
 * @param unit 单位
 */
public record Quantity(BigDecimal value, String unit) {
}
