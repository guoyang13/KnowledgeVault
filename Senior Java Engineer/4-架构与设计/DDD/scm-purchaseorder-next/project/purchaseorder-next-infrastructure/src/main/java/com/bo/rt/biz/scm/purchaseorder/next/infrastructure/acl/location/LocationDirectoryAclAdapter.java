package com.bo.rt.biz.scm.purchaseorder.next.infrastructure.acl.location;

import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port.LocationDirectoryPort;
import java.util.function.Function;

/**
 * 地点目录服务防腐层适配器。
 *
 * <p>远程协议对象在适配器内部转换，应用层只接收路线计算所需的稳定快照。</p>
 */
public class LocationDirectoryAclAdapter implements LocationDirectoryPort {

    /** 远程地点目录到应用层地点快照的转换函数。 */
    private final Function<String, LocationProfile> remoteLocationQuery;

    /** 注入远程地点目录调用实现。 */
    public LocationDirectoryAclAdapter(
            Function<String, LocationProfile> remoteLocationQuery
    ) {
        this.remoteLocationQuery = remoteLocationQuery;
    }

    /** 按地点编码读取仓、店、客户地址或中转节点快照。 */
    @Override
    public LocationProfile getLocation(String locationCode) {
        return remoteLocationQuery.apply(locationCode);
    }
}
