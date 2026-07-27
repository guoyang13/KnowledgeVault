package com.bo.rt.biz.scm.purchaseorder.next.application.planning;

import com.bo.rt.biz.scm.purchaseorder.next.application.shared.port.DomainEventPublisher;
import com.bo.rt.biz.scm.purchaseorder.next.application.shared.port.IdentityGenerator;
import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.model.PurchaseRequisition;
import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.model.TransferReservation;
import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.repository.PurchaseRequisitionRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.repository.TransferReservationRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * 采购计划用例。每个方法代表一个本地数据库事务。
 */
public class PlanningApplicationService {

    /** 采购申请聚合仓储。 */
    private final PurchaseRequisitionRepository requisitionRepository;
    /** 转单预占聚合仓储。 */
    private final TransferReservationRepository reservationRepository;
    /** 随本地事务写入领域事件的发布端口。 */
    private final DomainEventPublisher eventPublisher;
    /** 领域对象标识生成器。 */
    private final IdentityGenerator identityGenerator;
    /** 可替换时钟，保证截止时间计算可测试。 */
    private final Clock clock;

    /** 注入采购计划用例依赖。 */
    public PlanningApplicationService(
            PurchaseRequisitionRepository requisitionRepository,
            TransferReservationRepository reservationRepository,
            DomainEventPublisher eventPublisher,
            IdentityGenerator identityGenerator,
            Clock clock
    ) {
        this.requisitionRepository = requisitionRepository;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
        this.identityGenerator = identityGenerator;
        this.clock = clock;
    }

    /**
     * 幂等键为 commandId。数据库还应建立 command_id 唯一索引。
     *
     * @return 已存在或新建的转单预占结果
     */
    public ReservationResult reserve(ReserveRequisitionQuantities command) {
        return reservationRepository.findByCommandId(command.commandId())
                .map(this::toResult)
                .orElseGet(() -> createReservation(command));
    }

    /** 在一个本地事务中创建预占并保存采购申请数量账本。 */
    private ReservationResult createReservation(ReserveRequisitionQuantities command) {
        PurchaseRequisition requisition = requisitionRepository.findById(command.requisitionId())
                .orElseThrow(() -> new IllegalArgumentException("采购申请不存在"));
        TransferReservation reservation = requisition.reserveForOrder(
                identityGenerator.nextId("requisition-reservation"),
                command.commandId(),
                command.quantities(),
                Instant.now(clock).plus(command.ttl())
        );

        // 同一事务保存两个聚合及 Outbox；任一步失败都整体回滚。
        requisitionRepository.save(requisition);
        reservationRepository.save(reservation);
        eventPublisher.publishAll(requisition.pullDomainEvents());
        return toResult(reservation);
    }

    /**
     * 在采购订单创建成功后确认预占并将“已预占量”转为“已下单量”。
     *
     * <p>重复确认同一采购订单直接成功，确认到其他订单则拒绝。</p>
     */
    public void confirmOrderTransfer(String reservationId, String purchaseOrderId) {
        TransferReservation reservation = requireReservation(reservationId);
        if (reservation.status() == TransferReservation.ReservationStatus.CONFIRMED) {
            if (!purchaseOrderId.equals(reservation.purchaseOrderId())) {
                throw new IllegalStateException("预占已被另一采购订单确认");
            }
            return;
        }
        PurchaseRequisition requisition = requisitionRepository.findById(reservation.requisitionId())
                .orElseThrow(() -> new IllegalArgumentException("采购申请不存在"));
        requisition.commitToOrder(reservation, purchaseOrderId);
        requisitionRepository.save(requisition);
        reservationRepository.save(reservation);
        eventPublisher.publishAll(requisition.pullDomainEvents());
    }

    /** 订单创建失败或取消时释放预占数量，使其重新可下单。 */
    public void releaseOrderTransferReservation(String reservationId, String reason) {
        TransferReservation reservation = requireReservation(reservationId);
        if (reservation.status() == TransferReservation.ReservationStatus.RELEASED) {
            return;
        }
        PurchaseRequisition requisition = requisitionRepository.findById(reservation.requisitionId())
                .orElseThrow(() -> new IllegalArgumentException("采购申请不存在"));
        requisition.releaseReservation(reservation, reason);
        requisitionRepository.save(requisition);
        reservationRepository.save(reservation);
        eventPublisher.publishAll(requisition.pullDomainEvents());
    }

    /** 加载转单预占，不存在时终止用例。 */
    private TransferReservation requireReservation(String reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("转单预占不存在"));
    }

    /** 将领域对象转换成不泄漏聚合内部结构的用例结果。 */
    private ReservationResult toResult(TransferReservation reservation) {
        return new ReservationResult(
                reservation.id(),
                reservation.requisitionId(),
                reservation.status().name(),
                reservation.quantities()
        );
    }

    /**
     * 预占采购申请可下单数量的命令。
     *
     * @param commandId 调用方提供的幂等命令标识
     * @param requisitionId 采购申请标识
     * @param quantities 采购申请行标识到预占数量的映射
     * @param ttl 预占有效期
     */
    public record ReserveRequisitionQuantities(
            String commandId,
            String requisitionId,
            Map<String, Quantity> quantities,
            Duration ttl
    ) {

        /** 固化数量映射并校验预占有效期。 */
        public ReserveRequisitionQuantities {
            quantities = Map.copyOf(quantities);
            if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                throw new IllegalArgumentException("预占有效期必须大于零");
            }
        }
    }

    /**
     * 转单预占用例的返回结果。
     *
     * @param reservationId 预占标识
     * @param requisitionId 采购申请标识
     * @param status 预占状态
     * @param quantities 已预占的申请行数量
     */
    public record ReservationResult(
            String reservationId,
            String requisitionId,
            String status,
            Map<String, Quantity> quantities
    ) {

        /** 固化返回结果中的数量映射。 */
        public ReservationResult {
            quantities = Map.copyOf(quantities);
        }
    }
}
