package com.bo.rt.biz.scm.purchaseorder.next.domain.planning.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.planning.event.PlanningEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.AbstractAggregateRoot;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Destination;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.SkuRef;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 采购需求聚合（旧 PRD）。
 */
public class ProcurementDemand extends AbstractAggregateRoot {

    /** 采购需求聚合标识。 */
    private final String id;

    /** 面向业务人员展示和检索的需求单号。 */
    private final String demandNo;

    /** 本需求包含的商品、数量、目的地和要求到货日期快照。 */
    private final List<DemandLine> lines;

    /** 采购需求自身的审批与执行状态。 */
    private DemandStatus status;

    /** 当前有效审批实例，用于拒绝过期审批回调。 */
    private String approvalInstanceId;

    /** 校验需求行并创建采购需求草稿。 */
    private ProcurementDemand(String id, String demandNo, List<DemandLine> lines) {
        require(id != null && !id.isBlank(), "需求 ID 不能为空");
        require(demandNo != null && !demandNo.isBlank(), "需求单号不能为空");
        require(lines != null && !lines.isEmpty(), "采购需求至少包含一行");
        lines.forEach(line -> require(line.requiredQuantity().isPositive(), "需求数量必须大于零"));
        this.id = id;
        this.demandNo = demandNo;
        this.lines = List.copyOf(lines);
        this.status = DemandStatus.DRAFT;
    }

    /**
     * 创建处于草稿状态的采购需求，并校验至少存在一条正数量需求行。
     */
    public static ProcurementDemand draft(String id, String demandNo, List<DemandLine> lines) {
        return new ProcurementDemand(id, demandNo, lines);
    }

    /**
     * 将草稿或驳回需求提交审批，并绑定本轮审批实例。
     */
    public void submit(String approvalInstanceId) {
        require(status == DemandStatus.DRAFT || status == DemandStatus.REJECTED, "当前状态不能提交审批");
        require(approvalInstanceId != null && !approvalInstanceId.isBlank(), "审批实例不能为空");
        this.approvalInstanceId = approvalInstanceId;
        status = DemandStatus.APPROVAL_PENDING;
        nextVersion();
    }

    /**
     * 记录当前审批实例的结果；通过时发布需求已审批事件。
     */
    public void recordApprovalResult(String approvalInstanceId, boolean approved) {
        require(status == DemandStatus.APPROVAL_PENDING, "采购需求不在审批中");
        require(this.approvalInstanceId.equals(approvalInstanceId), "审批结果不属于当前审批实例");
        status = approved ? DemandStatus.APPROVED : DemandStatus.REJECTED;
        long eventVersion = nextVersion();
        if (approved) {
            raise(new PlanningEvents.ProcurementDemandApproved(
                    UUID.randomUUID().toString(), id, eventVersion, Instant.now(), demandNo
            ));
        }
    }

    /**
     * 将审批通过的需求释放给采购计划执行。
     */
    public void releaseForExecution() {
        require(status == DemandStatus.APPROVED, "只有审批通过的需求才能进入执行");
        status = DemandStatus.EXECUTING;
        nextVersion();
    }

    /**
     * 在所有下游采购申请执行完毕后关闭需求执行。
     */
    public void complete() {
        require(status == DemandStatus.EXECUTING, "只有执行中的需求才能完成");
        status = DemandStatus.COMPLETED;
        nextVersion();
    }

    /**
     * 取消尚可撤销的需求，保留已经发生的历史事实。
     */
    public void cancel() {
        require(
                status == DemandStatus.DRAFT
                        || status == DemandStatus.REJECTED
                        || status == DemandStatus.EXECUTING,
                "当前状态不能取消"
        );
        status = DemandStatus.CANCELLED;
        nextVersion();
    }

    /** 返回聚合标识。 */
    public String id() {
        return id;
    }

    /** 返回业务需求单号。 */
    public String demandNo() {
        return demandNo;
    }

    /** 返回当前需求状态。 */
    public DemandStatus status() {
        return status;
    }

    /** 返回不可变需求行快照。 */
    public List<DemandLine> lines() {
        return lines;
    }

    public enum DemandStatus {
        DRAFT,
        APPROVAL_PENDING,
        APPROVED,
        REJECTED,
        EXECUTING,
        COMPLETED,
        CANCELLED
    }

    /**
     * 采购需求行。
     *
     * @param id 需求行标识
     * @param sku 商品引用快照
     * @param requiredQuantity 需求数量
     * @param requiredArrivalDate 要求到货日期
     * @param destination 履约目的地
     */
    public record DemandLine(
            String id,
            SkuRef sku,
            Quantity requiredQuantity,
            LocalDate requiredArrivalDate,
            Destination destination
    ) {

        /** 校验并创建一条完整采购需求。 */
        public DemandLine {
            require(id != null && !id.isBlank(), "需求行 ID 不能为空");
            require(sku != null, "SKU 不能为空");
            require(requiredQuantity != null && requiredQuantity.isPositive(), "需求数量必须大于零");
            require(requiredArrivalDate != null, "要求到货日期不能为空");
            require(destination != null, "目的地不能为空");
        }
    }
}
