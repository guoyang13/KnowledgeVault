package com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port;

/**
 * 供应商档案端口。
 *
 * <p>应用层通过该端口校验供应商是否存在、是否可合作。</p>
 */
public interface SupplierProfilePort {

    /**
     * 判断供应商是否允许创建采购订单。
     *
     * @param supplierCode 供应商编码
     * @return 允许创建时返回 true，否则返回 false
     */
    boolean canCreatePurchaseOrder(String supplierCode);
}
