package com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.command;

import java.util.List;

/**
 * 采购订单导入用例命令。
 *
 * @param importBatchNo 导入批次号
 * @param rows 已完成格式解析的导入行
 */
public record ImportPurchaseOrderCommand(
        String importBatchNo,
        List<ImportPurchaseOrderRowCommand> rows
) {
}
