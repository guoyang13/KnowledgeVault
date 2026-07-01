package com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model;

import java.math.BigDecimal;

/**
 * 金额值对象。
 *
 * @param amount 金额数值
 * @param currency 币种
 */
public record Money(BigDecimal amount, String currency) {
}
