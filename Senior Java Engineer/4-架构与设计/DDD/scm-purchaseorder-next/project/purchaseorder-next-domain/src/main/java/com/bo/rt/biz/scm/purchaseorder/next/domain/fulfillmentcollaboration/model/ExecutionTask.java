package com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.event.CollaborationEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.AbstractAggregateRoot;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.BusinessReference;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 单个执行任务聚合。外部表单/单据只保存引用，不复制对方完整模型。
 */
public class ExecutionTask extends AbstractAggregateRoot {

    /** 执行任务标识。 */
    private final String id;
    /** 任务所服务的采购订单标识。 */
    private final String purchaseOrderId;
    /** 产生该任务的要求快照标识。 */
    private final String requirementId;
    /** 任务的业务类型。 */
    private final ExecutionRequirement.RequirementType requirementType;
    /** 任务作用的订单、订单行、发运单或收货批次标识。 */
    private final String scopeId;
    /** 任务未完成时阻塞的履约里程碑。 */
    private final ExecutionRequirement.Milestone blockingMilestone;
    /** 任务期望完成时间，用于逾期检测。 */
    private final Instant deadline;
    /** 任务当前处理状态。 */
    private TaskStatus status;
    /** 当前责任人或责任方。 */
    private String assignee;
    /** 任务实际产出的外部业务单据引用。 */
    private BusinessReference businessReference;
    /** 图片、文件等任务举证材料的引用。 */
    private List<String> evidenceReferences = List.of();
    /** 最近一次提交被驳回的原因。 */
    private String rejectionReason;
    /** 经审批跳过任务时留下的豁免凭据。 */
    private Waiver waiver;

    /** 根据一项适用要求创建待开始任务。 */
    private ExecutionTask(
            String id,
            String purchaseOrderId,
            ExecutionRequirement requirement,
            Instant deadline
    ) {
        require(id != null && !id.isBlank(), "任务 ID 不能为空");
        require(purchaseOrderId != null && !purchaseOrderId.isBlank(), "采购订单 ID 不能为空");
        require(requirement != null && requirement.requiresTask(), "只有适用要求可以生成任务");
        require(deadline != null, "任务截止时间不能为空");
        this.id = id;
        this.purchaseOrderId = purchaseOrderId;
        this.requirementId = requirement.id();
        this.requirementType = requirement.type();
        this.scopeId = requirement.scopeId();
        this.blockingMilestone = requirement.blockingMilestone();
        this.deadline = deadline;
        this.status = TaskStatus.NOT_STARTED;
        long eventVersion = nextVersion();
        raise(new CollaborationEvents.ExecutionTaskCreated(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                requirementId,
                requirementType.name(),
                scopeId,
                deadline
        ));
    }

    /** 创建要求对应的执行任务。 */
    public static ExecutionTask create(
            String id,
            String purchaseOrderId,
            ExecutionRequirement requirement,
            Instant deadline
    ) {
        return new ExecutionTask(id, purchaseOrderId, requirement, deadline);
    }

    /** 分配或重新分配任务责任人。 */
    public void assign(String assignee) {
        require(
                status == TaskStatus.NOT_STARTED
                        || status == TaskStatus.IN_PROGRESS
                        || status == TaskStatus.REJECTED,
                "当前任务不能分配"
        );
        require(assignee != null && !assignee.isBlank(), "负责人不能为空");
        this.assignee = assignee;
        nextVersion();
    }

    /** 在已分配责任人的前提下开始处理任务。 */
    public void start() {
        require(status == TaskStatus.NOT_STARTED || status == TaskStatus.REJECTED, "当前任务不能开始");
        require(assignee != null && !assignee.isBlank(), "任务开始前必须分配负责人");
        status = TaskStatus.IN_PROGRESS;
        nextVersion();
    }

    /**
     * 提交外部业务单据及证据，等待负责上下文确认处理结果。
     */
    public void submit(
            BusinessReference businessReference,
            List<String> evidenceReferences
    ) {
        require(status == TaskStatus.IN_PROGRESS, "只有进行中的任务可以提交");
        require(businessReference != null, "业务单据引用不能为空");
        this.businessReference = businessReference;
        this.evidenceReferences = evidenceReferences == null
                ? List.of()
                : List.copyOf(evidenceReferences);
        status = TaskStatus.SUBMITTED;
        nextVersion();
    }

    /**
     * 根据负责上下文返回的完成事实结束任务。
     *
     * @param sourceReference 证明任务完成的事件或外部单据引用
     */
    public void complete(String sourceReference) {
        require(status == TaskStatus.SUBMITTED, "只有已提交任务可以完成");
        require(sourceReference != null && !sourceReference.isBlank(), "完成依据不能为空");
        status = TaskStatus.COMPLETED;
        long eventVersion = nextVersion();
        raise(new CollaborationEvents.ExecutionTaskCompleted(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                requirementId,
                requirementType.name(),
                scopeId
        ));
    }

    /** 驳回已提交任务，使责任人能够修正后再次处理。 */
    public void reject(String reason) {
        require(status == TaskStatus.SUBMITTED, "只有已提交任务可以驳回");
        require(reason != null && !reason.isBlank(), "驳回原因不能为空");
        rejectionReason = reason;
        status = TaskStatus.REJECTED;
        nextVersion();
    }

    /** 根据有效审批依据豁免尚未完成的任务。 */
    public void waive(String reason, String operator, String approvalReference) {
        require(
                status == TaskStatus.NOT_STARTED
                        || status == TaskStatus.IN_PROGRESS
                        || status == TaskStatus.REJECTED,
                "当前任务不能豁免"
        );
        require(reason != null && !reason.isBlank(), "豁免原因不能为空");
        require(operator != null && !operator.isBlank(), "豁免操作人不能为空");
        require(
                approvalReference != null && !approvalReference.isBlank(),
                "豁免审批依据不能为空"
        );
        waiver = new Waiver(reason, operator, approvalReference, Instant.now());
        status = TaskStatus.WAIVED;
        long eventVersion = nextVersion();
        raise(new CollaborationEvents.ExecutionTaskWaived(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                requirementId,
                reason,
                approvalReference
        ));
    }

    /**
     * 判断当前任务是否阻塞指定业务对象进入目标里程碑。
     *
     * <p>订单级任务会阻塞订单下所有对象；已完成或已豁免任务不再阻塞。</p>
     */
    public boolean blocks(ExecutionRequirement.Milestone milestone, String targetScopeId) {
        boolean sameScope = scopeId.equals(purchaseOrderId) || scopeId.equals(targetScopeId);
        return sameScope
                && blockingMilestone == milestone
                && status != TaskStatus.COMPLETED
                && status != TaskStatus.WAIVED;
    }

    /** 返回任务标识。 */
    public String id() {
        return id;
    }

    /** 返回采购订单标识。 */
    public String purchaseOrderId() {
        return purchaseOrderId;
    }

    /** 返回来源要求标识。 */
    public String requirementId() {
        return requirementId;
    }

    /** 返回任务业务类型。 */
    public ExecutionRequirement.RequirementType requirementType() {
        return requirementType;
    }

    /** 返回任务作用对象标识。 */
    public String scopeId() {
        return scopeId;
    }

    /** 返回任务截止时间。 */
    public Instant deadline() {
        return deadline;
    }

    /** 返回任务处理状态。 */
    public TaskStatus status() {
        return status;
    }

    /** 返回任务责任人。 */
    public String assignee() {
        return assignee;
    }

    /** 执行任务从创建到完成、驳回或豁免的状态。 */
    public enum TaskStatus {
        NOT_STARTED,
        IN_PROGRESS,
        SUBMITTED,
        COMPLETED,
        REJECTED,
        WAIVED
    }

    /**
     * 经授权跳过执行任务的审计凭据。
     *
     * @param reason 豁免的业务原因
     * @param operator 执行豁免的操作人
     * @param approvalReference 支持豁免的审批单据引用
     * @param waivedAt 豁免发生时间
     */
    public record Waiver(
            String reason,
            String operator,
            String approvalReference,
            Instant waivedAt
    ) {
    }
}
