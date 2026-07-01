package com.bo.rt.biz.scm.purchaseorder.next.interfaces.file.excel;

/**
 * 采购订单导入 Excel 行对象。
 *
 * <p>该对象只服务于文件解析和行级错误提示，不允许进入领域层。</p>
 *
 * @param rowIndex 行号
 * @param supplierCode 供应商编码
 * @param skuCode SKU 编码
 * @param quantityText 采购数量原始文本
 * @param purchasePriceText 采购单价原始文本
 * @param destinationWarehouseCode 目的仓编码
 */
public record PurchaseOrderImportExcelRow(
        int rowIndex,
        String supplierCode,
        String skuCode,
        String quantityText,
        String purchasePriceText,
        String destinationWarehouseCode
) {
}
