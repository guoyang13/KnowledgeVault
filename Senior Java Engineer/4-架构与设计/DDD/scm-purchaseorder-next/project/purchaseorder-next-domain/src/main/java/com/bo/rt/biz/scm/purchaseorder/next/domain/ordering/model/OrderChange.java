package com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.AbstractAggregateRoot;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Money;
import java.time.LocalDate;

/**
 * 生效后订单变更聚合。变更内容使用明确类型，避免万能 JSON。
 */
public class OrderChange extends AbstractAggregateRoot {

    /** 订单变更聚合标识。 */
    private final String id;

    /** 被修改的采购订单标识。 */
    private final String purchaseOrderId;

    /** 提交变更时所依据的订单版本。 */
    private final long baseOrderVersion;

    /** 类型明确且不可变的变更内容。 */
    private final ChangeDetail detail;

    /** 变更单自身的审批和应用状态。 */
    private ChangeStatus status;

    /** 当前有效审批实例，用于过滤过期回调。 */
    private String approvalInstanceId;

    /**
     * 创建尚未提交审批的类型化订单变更。
     */
    public OrderChange(
            String id,
            String purchaseOrderId,
            long baseOrderVersion,
            ChangeDetail detail
    ) {
        require(id != null && !id.isBlank(), "变更单 ID 不能为空");
        require(purchaseOrderId != null && !purchaseOrderId.isBlank(), "采购订单 ID 不能为空");
        require(baseOrderVersion > 0, "基准订单版本必须大于零");
        require(detail != null, "变更内容不能为空");
        this.id = id;
        this.purchaseOrderId = purchaseOrderId;
        this.baseOrderVersion = baseOrderVersion;
        this.detail = detail;
        this.status = ChangeStatus.DRAFT;
    }

    /** 提交草稿或被驳回的变更单进入审批。 */
    public void submit(String approvalInstanceId) {
        require(status == ChangeStatus.DRAFT || status == ChangeStatus.REJECTED, "当前变更单不能提交");
        require(approvalInstanceId != null && !approvalInstanceId.isBlank(), "审批实例不能为空");
        this.approvalInstanceId = approvalInstanceId;
        status = ChangeStatus.APPROVAL_PENDING;
        nextVersion();
    }

    /** 记录当前审批实例的审批结果。 */
    public void recordApprovalResult(String approvalInstanceId, boolean approved) {
        require(status == ChangeStatus.APPROVAL_PENDING, "变更单不在审批中");
        require(this.approvalInstanceId.equals(approvalInstanceId), "审批实例不匹配");
        status = approved ? ChangeStatus.APPROVED : ChangeStatus.REJECTED;
        nextVersion();
    }

    /** 在变更内容成功写入订单聚合后，将变更单标记为已应用。 */
    public void markApplied() {
        require(status == ChangeStatus.APPROVED, "只有审批通过的变更单才能应用");
        status = ChangeStatus.APPLIED;
        nextVersion();
    }

    /** 返回变更单标识。 */
    public String id() {
        return id;
    }

    /** 返回目标采购订单标识。 */
    public String purchaseOrderId() {
        return purchaseOrderId;
    }

    /** 返回创建变更时的订单基准版本。 */
    public long baseOrderVersion() {
        return baseOrderVersion;
    }

    /** 返回类型化变更内容。 */
    public ChangeDetail detail() {
        return detail;
    }

    /** 返回变更单当前状态。 */
    public ChangeStatus status() {
        return status;
    }

    /** 订单变更内容的封闭类型集合。 */
    public sealed interface ChangeDetail
            permits DeliveryDateChange, PriceAdjustment, TerminationRequest {
    }

    /**
     * 订单行交期变更。
     *
     * @param lineId 目标订单行
     * @param newExpectedArrivalDate 新预计到货日期
     */
    public record DeliveryDateChange(String lineId, LocalDate newExpectedArrivalDate)
            implements ChangeDetail {
    }

    /**
     * 订单行含税单价调整。
     *
     * @param lineId 目标订单行
     * @param newTaxedUnitPrice 新含税单价
     */
    public record PriceAdjustment(String lineId, Money newTaxedUnitPrice)
            implements ChangeDetail {
    }

    /**
     * 生效订单终止申请。
     *
     * @param reason 终止原因
     */
    public record TerminationRequest(String reason) implements ChangeDetail {
    }

    public enum ChangeStatus {
        DRAFT,
        APPROVAL_PENDING,
        APPROVED,
        REJECTED,
        APPLIED
    }
}
