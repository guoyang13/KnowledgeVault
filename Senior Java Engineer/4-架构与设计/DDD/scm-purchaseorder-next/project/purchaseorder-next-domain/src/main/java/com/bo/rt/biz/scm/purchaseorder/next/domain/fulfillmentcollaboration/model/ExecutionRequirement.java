package com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

/**
 * 一次订单要求判断的不可变快照。
 *
 * @param id 要求标识
 * @param type 要求类型
 * @param scope 要求作用的业务对象类型
 * @param scopeId 要求作用的订单、订单行、发运单或收货批次标识
 * @param applicability 规则判断出的适用性
 * @param decisionReason 得出适用性结论的业务原因
 * @param policyVersion 采用的要求判定规则版本
 * @param blockingMilestone 未完成该要求时阻塞的履约里程碑
 */
public record ExecutionRequirement(
        String id,
        RequirementType type,
        ReferenceScope scope,
        String scopeId,
        Applicability applicability,
        String decisionReason,
        String policyVersion,
        Milestone blockingMilestone
) {

    /** 校验适用性、作用范围和阻塞里程碑之间的一致性。 */
    public ExecutionRequirement {
        require(id != null && !id.isBlank(), "要求 ID 不能为空");
        require(type != null, "要求类型不能为空");
        require(scope != null, "作用范围不能为空");
        require(scopeId != null && !scopeId.isBlank(), "范围对象 ID 不能为空");
        require(applicability != null, "适用性结论不能为空");
        require(decisionReason != null && !decisionReason.isBlank(), "判断原因不能为空");
        require(policyVersion != null && !policyVersion.isBlank(), "规则版本不能为空");
        require(
                applicability == Applicability.NOT_REQUIRED || blockingMilestone != null,
                "必做或条件要求必须声明阻塞里程碑"
        );
    }

    /** 判断该要求是否需要生成可跟踪的执行任务。 */
    public boolean requiresTask() {
        return applicability == Applicability.REQUIRED
                || applicability == Applicability.CONDITIONALLY_REQUIRED;
    }

    /** 可由履约规则产生的任务类型。 */
    public enum RequirementType {
        WAREHOUSE_RESERVATION,
        SAMPLE_IMAGE_UPLOAD,
        QUALIFICATION_DOCUMENT_UPLOAD,
        CONTAINER_LOADING,
        SAMPLE_MATCHING,
        QUALITY_INSPECTION,
        COMMODITY_INSPECTION
    }

    /** 要求可以约束的业务对象层级。 */
    public enum ReferenceScope {
        ORDER,
        ORDER_LINE,
        SHIPMENT,
        RECEIPT_BATCH
    }

    /** 规则对某项履约要求作出的适用性结论。 */
    public enum Applicability {
        REQUIRED,
        NOT_REQUIRED,
        CONDITIONALLY_REQUIRED
    }

    /** 执行任务可能阻塞的采购履约关键节点。 */
    public enum Milestone {
        SUPPLIER_CONFIRMATION,
        SHIPMENT_READY,
        DISPATCH,
        WAREHOUSE_RECEIPT,
        INBOUND,
        SETTLEMENT
    }
}
