package com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.result;

/**
 * 采购订单导入结果。
 *
 * @param importBatchNo 导入批次号
 * @param successCount 成功行数
 * @param failureCount 失败行数
 */
public record ImportPurchaseOrderResult(
        String importBatchNo,
        int successCount,
        int failureCount
) {
}
