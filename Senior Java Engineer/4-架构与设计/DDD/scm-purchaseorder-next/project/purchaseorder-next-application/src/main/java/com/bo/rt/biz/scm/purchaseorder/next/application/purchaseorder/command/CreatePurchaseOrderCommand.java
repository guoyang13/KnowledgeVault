package com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.command;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建采购订单的应用层命令。
 *
 * @param supplierCode 供应商编码
 * @param destinationWarehouseCode 目的仓编码
 * @param expectedArrivalDate 预计到货日期
 * @param lines 采购订单明细命令
 */
public record CreatePurchaseOrderCommand(
        String supplierCode,
        String destinationWarehouseCode,
        LocalDate expectedArrivalDate,
        List<CreatePurchaseOrderLineCommand> lines
) {
}
