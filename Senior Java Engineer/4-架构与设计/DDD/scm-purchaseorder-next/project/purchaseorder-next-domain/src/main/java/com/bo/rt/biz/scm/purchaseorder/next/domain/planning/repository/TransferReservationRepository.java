package com.bo.rt.biz.scm.purchaseorder.next.domain.planning.repository;

import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.model.TransferReservation;
import java.util.Optional;

/** PR 转 PO 预占聚合仓储端口。 */
public interface TransferReservationRepository {

    /** 保存预占令牌及其状态。 */
    void save(TransferReservation reservation);

    /** 按预占标识加载令牌。 */
    Optional<TransferReservation> findById(String reservationId);

    /** 按命令幂等键查找已有预占。 */
    Optional<TransferReservation> findByCommandId(String commandId);
}
