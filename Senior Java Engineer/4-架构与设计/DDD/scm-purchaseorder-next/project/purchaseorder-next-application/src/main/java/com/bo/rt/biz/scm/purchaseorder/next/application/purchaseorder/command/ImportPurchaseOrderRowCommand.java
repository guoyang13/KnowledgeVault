package com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.command;

/**
 * 采购订单导入行命令。
 *
 * <p>该对象已经脱离 Excel 注解，但仍保留行号用于错误报告。</p>
 *
 * @param rowIndex Excel 行号
 * @param supplierCode 供应商编码
 * @param skuCode SKU 编码
 * @param quantityText 采购数量原始文本
 * @param purchasePriceText 采购单价原始文本
 * @param destinationWarehouseCode 目的仓编码
 */
public record ImportPurchaseOrderRowCommand(
        int rowIndex,
        String supplierCode,
        String skuCode,
        String quantityText,
        String purchasePriceText,
        String destinationWarehouseCode
) {
}
