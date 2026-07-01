package com.bo.rt.biz.scm.purchaseorder.next.infrastructure.acl.warehouse;

import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port.WarehouseDirectoryPort;

/**
 * 仓库服务防腐层适配器。
 */
public class WarehouseDirectoryAclAdapter implements WarehouseDirectoryPort {

    /**
     * 判断仓库是否可作为采购订单目的仓。
     *
     * @param warehouseCode 仓库编码
     * @return 可作为目的仓时返回 true，否则返回 false
     */
    @Override
    public boolean canUseAsDestination(String warehouseCode) {
        throw new UnsupportedOperationException("骨架工程仅表达仓库防腐层边界，待补充外部调用实现。");
    }
}
