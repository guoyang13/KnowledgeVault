package com.bo.rt.biz.scm.purchaseorder.next.domain.planning.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.AbstractAggregateRoot;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import java.time.Instant;
import java.util.Map;

/**
 * PR 转 PO 的数量预占聚合。
 *
 * <p>预占成功不代表已经下单。只有 PO 成功持久化后，应用服务才确认预占。</p>
 */
public class TransferReservation extends AbstractAggregateRoot {

    /** 转单预占标识，也是后续确认或释放的令牌。 */
    private final String id;

    /** 被预占数量所属的采购申请。 */
    private final String requisitionId;

    /** 发起预占的幂等命令标识。 */
    private final String commandId;

    /** 按采购申请行记录的预占数量。 */
    private final Map<String, Quantity> quantities;

    /** 预占最晚确认时间，超时后应由补偿任务释放。 */
    private final Instant expiresAt;

    /** 预占生命周期状态。 */
    private ReservationStatus status;

    /** 确认成功后关联的采购订单标识。 */
    private String purchaseOrderId;

    /** 校验数量与有效期并创建转单预占令牌。 */
    private TransferReservation(
            String id,
            String requisitionId,
            String commandId,
            Map<String, Quantity> quantities,
            Instant expiresAt
    ) {
        require(id != null && !id.isBlank(), "预占 ID 不能为空");
        require(requisitionId != null && !requisitionId.isBlank(), "采购申请 ID 不能为空");
        require(commandId != null && !commandId.isBlank(), "命令 ID 不能为空");
        require(quantities != null && !quantities.isEmpty(), "预占数量不能为空");
        quantities.values().forEach(quantity -> require(quantity.isPositive(), "预占数量必须大于零"));
        require(expiresAt != null && expiresAt.isAfter(Instant.now()), "预占过期时间必须晚于当前时间");
        this.id = id;
        this.requisitionId = requisitionId;
        this.commandId = commandId;
        this.quantities = Map.copyOf(quantities);
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.RESERVED;
        nextVersion();
    }

    /**
     * 创建有效期内的 PR 数量预占。
     */
    public static TransferReservation reserve(
            String id,
            String requisitionId,
            String commandId,
            Map<String, Quantity> quantities,
            Instant expiresAt
    ) {
        return new TransferReservation(id, requisitionId, commandId, quantities, expiresAt);
    }

    /**
     * 在 PO 成功保存后确认预占，将令牌绑定到唯一采购订单。
     */
    public void confirm(String purchaseOrderId) {
        require(status == ReservationStatus.RESERVED, "只有预占中的记录可以确认");
        require(!isExpired(), "预占已经过期");
        require(purchaseOrderId != null && !purchaseOrderId.isBlank(), "采购订单 ID 不能为空");
        this.purchaseOrderId = purchaseOrderId;
        status = ReservationStatus.CONFIRMED;
        nextVersion();
    }

    /**
     * 主动释放尚未确认的预占，使数量重新可用于转单。
     */
    public void release() {
        require(status == ReservationStatus.RESERVED, "只有预占中的记录可以释放");
        status = ReservationStatus.RELEASED;
        nextVersion();
    }

    /**
     * 将已到期且未确认的预占标记为过期。
     */
    public void expire() {
        require(status == ReservationStatus.RESERVED, "只有预占中的记录可以过期");
        require(isExpired(), "预占尚未到期");
        status = ReservationStatus.EXPIRED;
        nextVersion();
    }

    /**
     * 判断预占是否已经超过确认截止时间。
     */
    public boolean isExpired() {
        return !expiresAt.isAfter(Instant.now());
    }

    /** 返回预占令牌标识。 */
    public String id() {
        return id;
    }

    /** 返回来源采购申请标识。 */
    public String requisitionId() {
        return requisitionId;
    }

    /** 返回预占请求幂等键。 */
    public String commandId() {
        return commandId;
    }

    /** 返回各采购申请行的不可变预占数量。 */
    public Map<String, Quantity> quantities() {
        return quantities;
    }

    /** 返回预占当前状态。 */
    public ReservationStatus status() {
        return status;
    }

    /** 返回确认后关联的采购订单；未确认时为空。 */
    public String purchaseOrderId() {
        return purchaseOrderId;
    }

    public enum ReservationStatus {
        RESERVED,
        CONFIRMED,
        RELEASED,
        EXPIRED
    }
}
