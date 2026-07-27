package com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Destination.DestinationType;

/**
 * 地点目录端口。
 *
 * <p>统一解析仓库、门店、客户目的地和中转节点，避免由接口调用方声明地点国家和类型。</p>
 */
public interface LocationDirectoryPort {

    /**
     * 读取路线计算使用的地点快照。
     *
     * @param locationCode 仓、店、客户地址或中转节点编码
     * @return 地点类型、国家和可用能力
     */
    LocationProfile getLocation(String locationCode);

    /**
     * 路线计算使用的地点快照。
     *
     * @param code 地点编码
     * @param type 地点类型
     * @param countryCode 地点所在国家或地区
     * @param destinationEnabled 是否允许作为采购目的地
     * @param transitEnabled 是否允许作为中转节点
     */
    record LocationProfile(
            String code,
            DestinationType type,
            String countryCode,
            boolean destinationEnabled,
            boolean transitEnabled
    ) {
    }
}
