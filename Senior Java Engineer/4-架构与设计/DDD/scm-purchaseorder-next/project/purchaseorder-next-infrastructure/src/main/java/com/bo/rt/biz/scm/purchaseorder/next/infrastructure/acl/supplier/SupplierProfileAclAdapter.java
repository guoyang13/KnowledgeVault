package com.bo.rt.biz.scm.purchaseorder.next.infrastructure.acl.supplier;

import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port.SupplierProfilePort;
import java.util.function.Function;

/**
 * 供应商服务防腐层适配器。
 */
public class SupplierProfileAclAdapter implements SupplierProfilePort {

    /** 远程供应商服务到应用层供应商快照的转换函数。 */
    private final Function<String, SupplierOrderingProfile> remoteProfileQuery;

    /** 注入远程供应商服务调用实现。 */
    public SupplierProfileAclAdapter(
            Function<String, SupplierOrderingProfile> remoteProfileQuery
    ) {
        this.remoteProfileQuery = remoteProfileQuery;
    }

    /**
     * 读取并转换供应商下单与所在地快照。
     *
     * @param supplierCode 供应商编码
     * @return 供应商下单快照
     */
    @Override
    public SupplierOrderingProfile getOrderingProfile(String supplierCode) {
        return remoteProfileQuery.apply(supplierCode);
    }
}
