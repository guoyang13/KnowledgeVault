package com.bo.rt.biz.scm.purchaseorder.next.infrastructure.persistence.memory;

import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model.ExecutionRequirementPlan;
import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model.ExecutionTask;
import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.repository.ExecutionRequirementPlanRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.repository.ExecutionTaskRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.model.PurchaseRequisition;
import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.model.TransferReservation;
import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.repository.PurchaseRequisitionRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.repository.TransferReservationRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.model.QualityInspectionOrder;
import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.repository.QualityInspectionOrderRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.model.ProcurementSettlement;
import com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.model.SettlementRevision;
import com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.repository.ProcurementSettlementRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.model.SupplierFulfillmentOrder;
import com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.model.SupplierShipment;
import com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.repository.SupplierFulfillmentOrderRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.supplierfulfillment.repository.SupplierShipmentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 场景演示使用的仓储。生产代码应分别实现数据库映射与乐观锁。
 */
public final class InMemoryRepositories {

    /** 工具容器不允许实例化。 */
    private InMemoryRepositories() {
    }

    /** 采购申请仓储的线程安全内存实现。 */
    public static final class RequisitionStore implements PurchaseRequisitionRepository {

        /** 采购申请标识到聚合实例的映射。 */
        private final Map<String, PurchaseRequisition> data = new ConcurrentHashMap<>();

        /** 保存或覆盖采购申请聚合。 */
        @Override
        public void save(PurchaseRequisition requisition) {
            data.put(requisition.id(), requisition);
        }

        /** 按采购申请标识查找聚合。 */
        @Override
        public Optional<PurchaseRequisition> findById(String requisitionId) {
            return Optional.ofNullable(data.get(requisitionId));
        }
    }

    /** 转单预占仓储的线程安全内存实现。 */
    public static final class ReservationStore implements TransferReservationRepository {

        /** 预占标识到聚合实例的映射。 */
        private final Map<String, TransferReservation> data = new ConcurrentHashMap<>();
        /** 幂等命令标识到预占标识的唯一索引。 */
        private final Map<String, String> idByCommand = new ConcurrentHashMap<>();

        /** 保存预占并建立幂等命令索引。 */
        @Override
        public void save(TransferReservation reservation) {
            data.put(reservation.id(), reservation);
            idByCommand.putIfAbsent(reservation.commandId(), reservation.id());
        }

        /** 按预占标识查找聚合。 */
        @Override
        public Optional<TransferReservation> findById(String reservationId) {
            return Optional.ofNullable(data.get(reservationId));
        }

        /** 按幂等命令标识查找已创建的预占。 */
        @Override
        public Optional<TransferReservation> findByCommandId(String commandId) {
            return Optional.ofNullable(idByCommand.get(commandId)).map(data::get);
        }
    }

    /** 供应商履约单仓储的线程安全内存实现。 */
    public static final class FulfillmentStore implements SupplierFulfillmentOrderRepository {

        /** 履约单标识到聚合实例的映射。 */
        private final Map<String, SupplierFulfillmentOrder> data = new ConcurrentHashMap<>();
        /** 采购订单标识到其供应商履约单标识的唯一索引。 */
        private final Map<String, String> idByOrder = new ConcurrentHashMap<>();

        /** 保存履约单并维护采购订单索引。 */
        @Override
        public void save(SupplierFulfillmentOrder fulfillmentOrder) {
            data.put(fulfillmentOrder.id(), fulfillmentOrder);
            idByOrder.put(fulfillmentOrder.purchaseOrderId(), fulfillmentOrder.id());
        }

        /** 按供应商履约单标识查找聚合。 */
        @Override
        public Optional<SupplierFulfillmentOrder> findById(String fulfillmentOrderId) {
            return Optional.ofNullable(data.get(fulfillmentOrderId));
        }

        /** 按采购订单标识查找唯一供应商履约单。 */
        @Override
        public Optional<SupplierFulfillmentOrder> findByPurchaseOrderId(String purchaseOrderId) {
            return Optional.ofNullable(idByOrder.get(purchaseOrderId)).map(data::get);
        }
    }

    /** 供应商发运批次仓储的线程安全内存实现。 */
    public static final class ShipmentStore implements SupplierShipmentRepository {

        /** 发运批次标识到聚合实例的映射。 */
        private final Map<String, SupplierShipment> data = new ConcurrentHashMap<>();

        /** 保存或覆盖供应商发运批次。 */
        @Override
        public void save(SupplierShipment shipment) {
            data.put(shipment.id(), shipment);
        }

        /** 按发运批次标识查找聚合。 */
        @Override
        public Optional<SupplierShipment> findById(String shipmentId) {
            return Optional.ofNullable(data.get(shipmentId));
        }
    }

    /** 质量检验单仓储的线程安全内存实现。 */
    public static final class InspectionStore implements QualityInspectionOrderRepository {

        /** 质检单标识到聚合实例的映射。 */
        private final Map<String, QualityInspectionOrder> data = new ConcurrentHashMap<>();
        /** 履约单元标识到质检单标识的唯一索引。 */
        private final Map<String, String> idByUnit = new ConcurrentHashMap<>();

        /** 保存质检单并维护履约单元索引。 */
        @Override
        public void save(QualityInspectionOrder inspectionOrder) {
            data.put(inspectionOrder.id(), inspectionOrder);
            idByUnit.put(inspectionOrder.fulfillmentUnitId(), inspectionOrder.id());
        }

        /** 按质检单标识查找聚合。 */
        @Override
        public Optional<QualityInspectionOrder> findById(String inspectionOrderId) {
            return Optional.ofNullable(data.get(inspectionOrderId));
        }

        /** 按履约单元标识查找其唯一质检单。 */
        @Override
        public Optional<QualityInspectionOrder> findByFulfillmentUnitId(String fulfillmentUnitId) {
            return Optional.ofNullable(idByUnit.get(fulfillmentUnitId)).map(data::get);
        }
    }

    /** 采购结算仓储及版本历史的线程安全内存实现。 */
    public static final class SettlementStore implements ProcurementSettlementRepository {

        /** 结算标识到聚合实例的映射。 */
        private final Map<String, ProcurementSettlement> data = new ConcurrentHashMap<>();
        /** 采购订单标识到结算标识的唯一索引。 */
        private final Map<String, String> idByOrder = new ConcurrentHashMap<>();
        /** 结算标识到不可变计算版本历史的映射。 */
        private final Map<String, List<SettlementRevision>> history = new ConcurrentHashMap<>();

        /** 保存结算聚合，并按版本号幂等追加当前计算版本。 */
        @Override
        public void save(ProcurementSettlement settlement) {
            data.put(settlement.id(), settlement);
            idByOrder.put(settlement.purchaseOrderId(), settlement.id());
            SettlementRevision revision = settlement.currentRevision();
            if (revision != null) {
                List<SettlementRevision> revisions =
                        history.computeIfAbsent(settlement.id(), ignored -> new ArrayList<>());
                boolean exists = revisions.stream()
                        .anyMatch(item -> item.revisionNo() == revision.revisionNo());
                if (!exists) {
                    revisions.add(revision);
                }
            }
        }

        /** 按结算标识查找聚合。 */
        @Override
        public Optional<ProcurementSettlement> findById(String settlementId) {
            return Optional.ofNullable(data.get(settlementId));
        }

        /** 按采购订单标识查找唯一结算聚合。 */
        @Override
        public Optional<ProcurementSettlement> findByPurchaseOrderId(String purchaseOrderId) {
            return Optional.ofNullable(idByOrder.get(purchaseOrderId)).map(data::get);
        }

        /** 返回指定结算的不可变版本历史快照。 */
        @Override
        public List<SettlementRevision> findRevisionHistory(String settlementId) {
            return List.copyOf(history.getOrDefault(settlementId, List.of()));
        }
    }

    /** 执行要求计划仓储的线程安全内存实现。 */
    public static final class RequirementPlanStore implements ExecutionRequirementPlanRepository {

        /** 要求计划标识到聚合实例的映射。 */
        private final Map<String, ExecutionRequirementPlan> data = new ConcurrentHashMap<>();
        /** 采购订单标识到当前活动计划标识的唯一索引。 */
        private final Map<String, String> activeIdByOrder = new ConcurrentHashMap<>();

        /** 保存要求计划并根据状态维护活动计划索引。 */
        @Override
        public void save(ExecutionRequirementPlan plan) {
            data.put(plan.id(), plan);
            if (plan.status() == ExecutionRequirementPlan.PlanStatus.ACTIVE) {
                activeIdByOrder.put(plan.purchaseOrderId(), plan.id());
            } else {
                activeIdByOrder.remove(plan.purchaseOrderId(), plan.id());
            }
        }

        /** 按采购订单标识查找当前活动要求计划。 */
        @Override
        public Optional<ExecutionRequirementPlan> findActiveByPurchaseOrderId(
                String purchaseOrderId
        ) {
            return Optional.ofNullable(activeIdByOrder.get(purchaseOrderId)).map(data::get);
        }
    }

    /** 履约执行任务仓储的线程安全内存实现。 */
    public static final class TaskStore implements ExecutionTaskRepository {

        /** 执行任务标识到聚合实例的映射。 */
        private final Map<String, ExecutionTask> data = new ConcurrentHashMap<>();

        /** 保存或覆盖履约执行任务。 */
        @Override
        public void save(ExecutionTask task) {
            data.put(task.id(), task);
        }

        /** 按执行任务标识查找聚合。 */
        @Override
        public Optional<ExecutionTask> findById(String taskId) {
            return Optional.ofNullable(data.get(taskId));
        }

        /** 返回采购订单下尚未完成或豁免的活动任务。 */
        @Override
        public List<ExecutionTask> findActiveByPurchaseOrderId(String purchaseOrderId) {
            return data.values().stream()
                    .filter(task -> task.purchaseOrderId().equals(purchaseOrderId))
                    .filter(task -> task.status() != ExecutionTask.TaskStatus.COMPLETED
                            && task.status() != ExecutionTask.TaskStatus.WAIVED)
                    .toList();
        }

        /** 判断同一要求、类型和作用对象下是否已有活动任务。 */
        @Override
        public boolean existsActiveByBusinessKey(
                String requirementId,
                String taskType,
                String scopeId
        ) {
            return data.values().stream().anyMatch(task ->
                    task.requirementId().equals(requirementId)
                            && task.requirementType().name().equals(taskType)
                            && task.scopeId().equals(scopeId)
                            && task.status() != ExecutionTask.TaskStatus.COMPLETED
                            && task.status() != ExecutionTask.TaskStatus.WAIVED
            );
        }
    }
}
