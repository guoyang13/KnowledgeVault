package com.bo.rt.biz.scm.purchaseorder.next.application.fulfillmentcollaboration;

import com.bo.rt.biz.scm.purchaseorder.next.application.shared.port.DomainEventPublisher;
import com.bo.rt.biz.scm.purchaseorder.next.application.shared.port.IdentityGenerator;
import com.bo.rt.biz.scm.purchaseorder.next.application.supplierfulfillment.SupplierFulfillmentApplicationService.DispatchGatePort;
import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model.DispatchGatePolicy;
import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model.ExecutionRequirement;
import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model.ExecutionRequirementPlan;
import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model.ExecutionTask;
import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.repository.ExecutionRequirementPlanRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.repository.ExecutionTaskRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.event.OrderingEvents;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 采购履约协同用例。规则端口返回本次订单的判断快照，协同上下文只负责编排。
 */
public class FulfillmentCollaborationApplicationService implements DispatchGatePort {

    /** 执行要求计划聚合仓储。 */
    private final ExecutionRequirementPlanRepository planRepository;
    /** 履约执行任务聚合仓储。 */
    private final ExecutionTaskRepository taskRepository;
    /** 根据订单快照判断履约要求的规则端口。 */
    private final RequirementPolicyPort requirementPolicy;
    /** 要求、计划和任务标识生成器。 */
    private final IdentityGenerator identityGenerator;
    /** 随本地事务写入领域事件的发布端口。 */
    private final DomainEventPublisher eventPublisher;
    /** 可替换时钟，用于稳定计算任务截止时间。 */
    private final Clock clock;

    /** 注入采购履约协同用例依赖。 */
    public FulfillmentCollaborationApplicationService(
            ExecutionRequirementPlanRepository planRepository,
            ExecutionTaskRepository taskRepository,
            RequirementPolicyPort requirementPolicy,
            IdentityGenerator identityGenerator,
            DomainEventPublisher eventPublisher,
            Clock clock
    ) {
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
        this.requirementPolicy = requirementPolicy;
        this.identityGenerator = identityGenerator;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * 采购订单生效后幂等评估履约要求，并为适用要求创建执行任务。
     */
    public void onPurchaseOrderEffective(OrderingEvents.PurchaseOrderEffective event) {
        if (planRepository.findActiveByPurchaseOrderId(event.aggregateId()).isPresent()) {
            return;
        }
        List<RequirementDecision> decisions = requirementPolicy.evaluate(event);
        List<ExecutionRequirement> requirements = decisions.stream()
                .map(decision -> new ExecutionRequirement(
                        identityGenerator.nextId("execution-requirement"),
                        decision.type(),
                        decision.scope(),
                        decision.scopeId(),
                        decision.applicability(),
                        decision.reason(),
                        decision.policyVersion(),
                        decision.blockingMilestone()
                ))
                .toList();
        ExecutionRequirementPlan plan = ExecutionRequirementPlan.build(
                identityGenerator.nextId("execution-requirement-plan"),
                event.aggregateId(),
                event.aggregateVersion(),
                1,
                requirements
        );
        plan.activate();
        planRepository.save(plan);
        eventPublisher.publishAll(plan.pullDomainEvents());

        for (ExecutionRequirement requirement : plan.taskRequirements()) {
            if (taskRepository.existsActiveByBusinessKey(
                    requirement.id(), requirement.type().name(), requirement.scopeId()
            )) {
                continue;
            }
            ExecutionTask task = ExecutionTask.create(
                    identityGenerator.nextId("execution-task"),
                    event.aggregateId(),
                    requirement,
                    Instant.now(clock).plus(deadlineFor(requirement.type()))
            );
            taskRepository.save(task);
            eventPublisher.publishAll(task.pullDomainEvents());
        }
    }

    /** 根据采购订单当前活动任务返回指定履约单元的发货门禁结论。 */
    @Override
    public boolean canDispatch(String purchaseOrderId, String fulfillmentUnitId) {
        return DispatchGatePolicy.evaluate(
                fulfillmentUnitId,
                taskRepository.findActiveByPurchaseOrderId(purchaseOrderId)
        ).dispatchAllowed();
    }

    /** 根据任务类型给出默认完成时限。 */
    private Duration deadlineFor(ExecutionRequirement.RequirementType type) {
        return switch (type) {
            case SAMPLE_MATCHING, SAMPLE_IMAGE_UPLOAD -> Duration.ofDays(2);
            case QUALIFICATION_DOCUMENT_UPLOAD, COMMODITY_INSPECTION -> Duration.ofDays(3);
            case WAREHOUSE_RESERVATION, CONTAINER_LOADING -> Duration.ofDays(1);
            case QUALITY_INSPECTION -> Duration.ofDays(5);
        };
    }

    /** 外部规则实现提供的履约要求评估端口。 */
    public interface RequirementPolicyPort {

        /** 对一个已生效采购订单快照作出完整要求判断。 */
        List<RequirementDecision> evaluate(OrderingEvents.PurchaseOrderEffective order);
    }

    /**
     * 规则端口返回的一项履约要求判断。
     *
     * @param type 要求类型
     * @param scope 作用对象类型
     * @param scopeId 作用对象标识
     * @param applicability 适用性结论
     * @param reason 判断原因
     * @param policyVersion 规则版本
     * @param blockingMilestone 未完成时阻塞的履约里程碑
     */
    public record RequirementDecision(
            ExecutionRequirement.RequirementType type,
            ExecutionRequirement.ReferenceScope scope,
            String scopeId,
            ExecutionRequirement.Applicability applicability,
            String reason,
            String policyVersion,
            ExecutionRequirement.Milestone blockingMilestone
    ) {
    }
}
