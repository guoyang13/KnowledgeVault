package com.bo.rt.biz.scm.purchaseorder.next.infrastructure.acl.supplier;

import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port.SupplierProfilePort;

/**
 * 供应商服务防腐层适配器。
 */
public class SupplierProfileAclAdapter implements SupplierProfilePort {

    /**
     * 判断供应商是否允许创建采购订单。
     *
     * @param supplierCode 供应商编码
     * @return 允许创建时返回 true，否则返回 false
     */
    @Override
    public boolean canCreatePurchaseOrder(String supplierCode) {
        throw new UnsupportedOperationException("骨架工程仅表达供应商防腐层边界，待补充外部调用实现。");
    }
}
