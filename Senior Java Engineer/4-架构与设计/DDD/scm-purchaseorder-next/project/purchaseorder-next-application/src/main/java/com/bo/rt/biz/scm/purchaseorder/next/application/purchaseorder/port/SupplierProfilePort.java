package com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port;

/**
 * 供应商档案端口。
 *
 * <p>应用层通过该端口取得供应商准入和路线计算所需的可信快照。</p>
 */
public interface SupplierProfilePort {

    /**
     * 读取创建采购订单所需的供应商快照。
     *
     * @param supplierCode 供应商编码
     * @return 供应商下单与所在地快照
     */
    SupplierOrderingProfile getOrderingProfile(String supplierCode);

    /**
     * 供应商下单快照。
     *
     * @param supplierCode 供应商编码
     * @param countryCode 供应商税源地或实际发货主体所在国家/地区
     * @param orderAllowed 当前是否允许创建采购订单
     */
    record SupplierOrderingProfile(
            String supplierCode,
            String countryCode,
            boolean orderAllowed
    ) {
    }
}
