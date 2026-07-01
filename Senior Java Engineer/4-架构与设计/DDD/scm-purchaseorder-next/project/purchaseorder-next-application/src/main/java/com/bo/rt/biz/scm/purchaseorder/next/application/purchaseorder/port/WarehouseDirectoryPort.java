package com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port;

/**
 * 仓库目录端口。
 *
 * <p>用于校验目的仓、中转仓等仓库基础信息。</p>
 */
public interface WarehouseDirectoryPort {

    /**
     * 判断仓库是否可作为采购订单目的仓。
     *
     * @param warehouseCode 仓库编码
     * @return 可作为目的仓时返回 true，否则返回 false
     */
    boolean canUseAsDestination(String warehouseCode);
}
