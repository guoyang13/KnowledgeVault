package com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.event.CollaborationEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.AbstractAggregateRoot;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 订单执行要求计划。每次重评估生成新版本，旧计划进入 SUPERSEDED。
 */
public class ExecutionRequirementPlan extends AbstractAggregateRoot {

    /** 要求计划标识。 */
    private final String id;
    /** 被评估的采购订单标识。 */
    private final String purchaseOrderId;
    /** 本计划依据的采购订单版本。 */
    private final long purchaseOrderVersion;
    /** 同一采购订单下单调递增的要求计划版本。 */
    private final int planVersion;
    /** 规则评估产生的全部要求快照，包括“不需要执行”的结论。 */
    private final List<ExecutionRequirement> requirements;
    /** 要求计划当前生命周期状态。 */
    private PlanStatus status;

    /** 创建尚未生效的要求计划，并校验同一作用范围内不存在重复要求。 */
    private ExecutionRequirementPlan(
            String id,
            String purchaseOrderId,
            long purchaseOrderVersion,
            int planVersion,
            List<ExecutionRequirement> requirements
    ) {
        require(id != null && !id.isBlank(), "要求计划 ID 不能为空");
        require(purchaseOrderId != null && !purchaseOrderId.isBlank(), "采购订单 ID 不能为空");
        require(purchaseOrderVersion > 0, "采购订单版本必须大于零");
        require(planVersion > 0, "计划版本必须大于零");
        require(requirements != null, "要求列表不能为空");
        Set<String> businessKeys = new HashSet<>();
        requirements.forEach(requirement -> require(
                businessKeys.add(
                        requirement.type() + ":" + requirement.scope() + ":" + requirement.scopeId()
                ),
                "同一作用范围不能重复生成相同要求"
        ));
        this.id = id;
        this.purchaseOrderId = purchaseOrderId;
        this.purchaseOrderVersion = purchaseOrderVersion;
        this.planVersion = planVersion;
        this.requirements = List.copyOf(requirements);
        this.status = PlanStatus.DRAFT;
    }

    /**
     * 根据一个明确的采购订单版本构建要求计划草稿。
     */
    public static ExecutionRequirementPlan build(
            String id,
            String purchaseOrderId,
            long purchaseOrderVersion,
            int planVersion,
            List<ExecutionRequirement> requirements
    ) {
        return new ExecutionRequirementPlan(
                id, purchaseOrderId, purchaseOrderVersion, planVersion, requirements
        );
    }

    /**
     * 激活要求计划并发布完整要求快照，供应用层生成执行任务。
     */
    public void activate() {
        require(status == PlanStatus.DRAFT, "只有草稿要求计划可以激活");
        status = PlanStatus.ACTIVE;
        long eventVersion = nextVersion();
        raise(new CollaborationEvents.ExecutionRequirementPlanCreated(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                purchaseOrderVersion,
                planVersion,
                requirements.stream()
                        .map(requirement -> new CollaborationEvents.RequirementSnapshot(
                                requirement.id(),
                                requirement.type().name(),
                                requirement.scope().name(),
                                requirement.scopeId(),
                                requirement.applicability().name(),
                                requirement.blockingMilestone() == null
                                        ? null
                                        : requirement.blockingMilestone().name()
                        ))
                        .toList()
        ));
    }

    /** 将当前活动计划标记为已被更新版本替代。 */
    public void supersede() {
        require(status == PlanStatus.ACTIVE, "只有活动计划可以被替代");
        status = PlanStatus.SUPERSEDED;
        nextVersion();
    }

    /** 返回需要实际生成执行任务的要求。 */
    public List<ExecutionRequirement> taskRequirements() {
        return requirements.stream().filter(ExecutionRequirement::requiresTask).toList();
    }

    /** 返回要求计划标识。 */
    public String id() {
        return id;
    }

    /** 返回采购订单标识。 */
    public String purchaseOrderId() {
        return purchaseOrderId;
    }

    /** 返回本计划采用的采购订单版本。 */
    public long purchaseOrderVersion() {
        return purchaseOrderVersion;
    }

    /** 返回要求计划版本。 */
    public int planVersion() {
        return planVersion;
    }

    /** 返回本次评估产生的全部要求快照。 */
    public List<ExecutionRequirement> requirements() {
        return requirements;
    }

    /** 返回要求计划状态。 */
    public PlanStatus status() {
        return status;
    }

    /** 要求计划从草稿、活动到被新版替代的状态。 */
    public enum PlanStatus {
        DRAFT,
        ACTIVE,
        SUPERSEDED
    }
}
